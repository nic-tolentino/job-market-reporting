#!/usr/bin/env python3
"""
Method A: Probe Greenhouse / Lever / Ashby APIs with slug candidates derived from
company manifest IDs, names, and alternateNames.

For every company without an ATS config, generates slug candidates and probes
all three free-API providers concurrently. Uses per-provider semaphores and a
small inter-request delay to stay within reasonable rate limits.

Usage:
    python3 scripts/ats/probe_free_ats.py
    python3 scripts/ats/probe_free_ats.py --output /tmp/probe_hits.csv
    python3 scripts/ats/probe_free_ats.py --apply-to-manifests
    python3 scripts/ats/probe_free_ats.py --apply-to-manifests --dry-run
    python3 scripts/ats/probe_free_ats.py --providers GREENHOUSE,LEVER
    python3 scripts/ats/probe_free_ats.py --concurrency 2 --delay 0.25

Requirements:
    pip install aiohttp
"""

import asyncio
import json
import re
import csv
import argparse
import sys
from pathlib import Path
from typing import Optional
from urllib.parse import quote

try:
    import aiohttp
except ImportError:
    print("Error: aiohttp not found. Run: pip install aiohttp")
    sys.exit(1)

# ── Config ────────────────────────────────────────────────────────────────────

MANIFEST_DIR  = Path(__file__).parent.parent.parent / "data" / "companies"
DEFAULT_OUTPUT = Path(__file__).parent / "ats_probe_hits.csv"

API_ENDPOINTS = {
    "GREENHOUSE": "https://boards-api.greenhouse.io/v1/boards/{slug}/jobs",
    "LEVER":      "https://api.lever.co/v0/postings/{slug}",
    "ASHBY":      "https://api.ashbyhq.com/posting-api/job-board/{slug}",
}

# Max simultaneous open connections per provider
DEFAULT_CONCURRENCY = {
    "GREENHOUSE": 5,
    "LEVER":      3,
    "ASHBY":      3,
}

REQUEST_DELAY = 0.15   # seconds between requests inside each semaphore slot
TIMEOUT       = 10     # per-request timeout in seconds

# Candidate priority order (lower index = higher priority for deduplication)
SLUG_SOURCES_PRIORITY = ["manifest_id", "name_slug", "alt_slug", "raw_name", "raw_alt"]

# ── Slug generation ──────────────────────────────────────────────────────────

def slugify(s: str) -> str:
    """Lowercase kebab-case: strip special chars, spaces → hyphens."""
    s = s.lower().strip()
    s = re.sub(r"[^\w\s-]", "", s)   # remove punctuation except hyphens
    s = re.sub(r"[\s_]+", "-", s)    # spaces/underscores → hyphens
    s = re.sub(r"-+", "-", s)        # collapse runs
    return s.strip("-")


def get_candidates(company: dict) -> list[tuple[str, str]]:
    """
    Returns [(slug, source_label), ...] — deduplicated, highest confidence first.

    Slugified variants are probed for all providers.
    Raw (un-slugified) variants are also generated for Ashby, which accepts
    names with spaces and mixed case (e.g. "Checkbox Technology", "leonardo.ai").
    """
    seen: set[str] = set()
    results: list[tuple[str, str]] = []

    def add(slug: str, label: str) -> None:
        slug = slug.strip()
        if slug and slug not in seen:
            seen.add(slug)
            results.append((slug, label))

    # Highest confidence: manifest id is already a clean slug
    add(company["id"], "manifest_id")

    # Slugified name
    add(slugify(company["name"]), "name_slug")

    # Slugified alternateNames
    for alt in company.get("alternateNames", []):
        add(slugify(alt), "alt_slug")

    # Raw un-slugified variants (useful for Ashby)
    add(company["name"], "raw_name")
    for alt in company.get("alternateNames", []):
        add(alt, "raw_alt")

    return results


# ── Probing ──────────────────────────────────────────────────────────────────

async def probe_one(
    session:   "aiohttp.ClientSession",
    semaphore: asyncio.Semaphore,
    provider:  str,
    slug:      str,
    delay:     float = REQUEST_DELAY,
) -> Optional[int]:
    """
    Returns job count (>= 0) if slug is a valid board, else None.

    Ashby quirk: the API returns 404 when the board exists but has 0 open jobs.
    In that case we verify the board page (jobs.ashbyhq.com/{slug}) before
    accepting it as a valid (empty) hit.
    """
    encoded = quote(slug, safe="-_.")
    url = API_ENDPOINTS[provider].format(slug=encoded)

    async with semaphore:
        await asyncio.sleep(delay)
        try:
            async with session.get(
                url,
                timeout=aiohttp.ClientTimeout(total=TIMEOUT),
                allow_redirects=True,
            ) as resp:
                if resp.status == 200:
                    try:
                        body = await resp.json(content_type=None)
                    except Exception:
                        body = {}

                    if provider == "GREENHOUSE":
                        return len(body.get("jobs", []))
                    elif provider == "LEVER":
                        return len(body) if isinstance(body, list) else 0
                    elif provider == "ASHBY":
                        postings = body.get("jobPostings", body.get("jobs", []))
                        return len(postings) if isinstance(postings, list) else 0

                # Note: Ashby's board page (jobs.ashbyhq.com) is a SPA that returns
                # HTTP 200 for any URL, so we cannot use it to verify empty boards.
                # Only accept Ashby hits when the API itself returns 200 (>= 0 jobs).

        except (asyncio.TimeoutError, aiohttp.ClientError):
            pass  # network issue — treat as miss

    return None


async def run_probes(
    companies:       list[dict],
    providers:       list[str],
    cli_concurrency: Optional[int],
    delay:           float = REQUEST_DELAY,
) -> list[dict]:
    """
    For every company × provider × slug candidate, fires a probe.
    Returns a flat list of all hit dicts (may include multiple slugs per
    company×provider — caller deduplicates with priority ordering).
    """
    semaphores = {
        p: asyncio.Semaphore(cli_concurrency or DEFAULT_CONCURRENCY[p])
        for p in providers
    }

    # Build probe queue
    probe_queue: list[tuple[dict, str, str, str]] = []
    for company in companies:
        candidates = get_candidates(company)
        for provider in providers:
            for slug, label in candidates:
                # Multi-word slugs are only valid for Ashby
                if provider in ("GREENHOUSE", "LEVER") and " " in slug:
                    continue
                probe_queue.append((company, provider, slug, label))

    total = len(probe_queue)
    print(f"  {len(companies)} companies  ×  {len(providers)} providers  =  {total:,} probes queued")
    print(f"  Providers: {', '.join(providers)}")
    print()

    all_hits: list[dict] = []
    done_count = 0
    lock = asyncio.Lock()

    connector = aiohttp.TCPConnector(limit=30)
    headers   = {"User-Agent": "job-market-research/1.0 (ATS discovery; contact via github.com)"}

    async with aiohttp.ClientSession(connector=connector, headers=headers) as session:

        async def run_one(company: dict, provider: str, slug: str, label: str) -> None:
            nonlocal done_count
            count = await probe_one(session, semaphores[provider], provider, slug, delay=delay)
            async with lock:
                done_count += 1
                if done_count % 200 == 0 or done_count == total:
                    pct = done_count / total * 100
                    hits_so_far = len(all_hits)
                    print(f"  [{done_count:>{len(str(total))}}/{total}]  {pct:4.0f}%  hits so far: {hits_so_far}", end="\r")
                if count is not None:
                    all_hits.append({
                        "companyId":   company["id"],
                        "companyName": company["name"],
                        "provider":    provider,
                        "identifier":  slug,
                        "jobCount":    count,
                        "slugSource":  label,
                        "_path":       company["path"],
                        "_priority":   _source_priority(label),
                    })

        await asyncio.gather(*[run_one(*args) for args in probe_queue])

    print(f"\n\n  Done. {len(all_hits)} raw hits found.")
    return all_hits


def _source_priority(label: str) -> int:
    """Lower number = higher priority for deduplication."""
    for i, prefix in enumerate(SLUG_SOURCES_PRIORITY):
        if label == prefix or label.startswith(prefix):
            return i
    return len(SLUG_SOURCES_PRIORITY)


def deduplicate_hits(hits: list[dict]) -> list[dict]:
    """
    For each (companyId, provider) pair, keep the highest-priority slug.
    Priority: manifest_id > name_slug > alt_slug > raw_name > raw_alt
    """
    best: dict[tuple[str, str], dict] = {}
    for h in hits:
        key = (h["companyId"], h["provider"])
        if key not in best or h["_priority"] < best[key]["_priority"]:
            best[key] = h
    return sorted(best.values(), key=lambda h: (h["companyId"], h["provider"]))


# ── Manifest loading ──────────────────────────────────────────────────────────

def load_unidentified_companies() -> list[dict]:
    """Load all manifest companies that do not yet have an ATS config."""
    companies = []
    for path in MANIFEST_DIR.rglob("*.json"):
        if path.name == "schema.json":
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            if "ats" not in data:
                companies.append({
                    "id":             data["id"],
                    "name":           data.get("name", ""),
                    "alternateNames": data.get("alternateNames", []),
                    "path":           path,
                })
        except Exception:
            pass
    return sorted(companies, key=lambda c: c["id"])


# ── Output ────────────────────────────────────────────────────────────────────

CSV_FIELDS = ["companyId", "companyName", "provider", "identifier", "jobCount", "slugSource"]


def write_csv(hits: list[dict], output_path: Path) -> None:
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
        writer.writeheader()
        for h in hits:
            writer.writerow({k: h[k] for k in CSV_FIELDS})
    print(f"Wrote {len(hits)} hits → {output_path}")


def print_summary(hits: list[dict]) -> None:
    from collections import Counter
    print(f"\n{'═'*60}")
    print(f"  Method A probe results (deduplicated)")
    print(f"{'═'*60}")
    print(f"  Unique company+provider hits: {len(hits)}")
    print(f"  Unique companies with ≥1 hit: {len(set(h['companyId'] for h in hits))}")
    print()
    by_provider = Counter(h["provider"] for h in hits)
    for provider, count in by_provider.most_common():
        print(f"    {provider:<14} {count}")
    print()
    by_source = Counter(h["slugSource"] for h in hits)
    print(f"  Slug sources:")
    for source, count in by_source.most_common():
        print(f"    {source:<30} {count}")
    print()
    print(f"  Sample hits:")
    for h in hits[:20]:
        print(f"    {h['companyId']:<35} {h['provider']:<12} {h['identifier']}  ({h['jobCount']} jobs)")
    if len(hits) > 20:
        print(f"    … and {len(hits) - 20} more (see CSV)")


def apply_to_manifests(hits: list[dict], dry_run: bool = False) -> None:
    label = "DRY RUN — would write" if dry_run else "Writing"
    print(f"\n{label} {len(hits)} ATS configs to manifests...")
    written = 0
    for h in hits:
        path: Path = h["_path"]
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            if "ats" in data:
                continue  # safety: never overwrite existing config

            data["ats"] = {"identifier": h["identifier"], "provider": h["provider"]}

            if not dry_run:
                path.write_text(
                    json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
                    encoding="utf-8",
                )
            action = "WOULD WRITE" if dry_run else "WROTE"
            print(f"  {action} {h['companyId']:<35} → {h['provider']}/{h['identifier']}")
            written += 1
        except Exception as e:
            print(f"  ERROR {h['companyId']}: {e}")

    print(f"\n{'Would write' if dry_run else 'Wrote'} {written} manifest files.")
    if not dry_run:
        print("Run validate_ats_configs.py to verify the new entries against live APIs.")


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT),
                        help="Output CSV path")
    parser.add_argument("--providers", default="GREENHOUSE,LEVER,ASHBY",
                        help="Comma-separated list of providers to probe (default: all three)")
    parser.add_argument("--concurrency", type=int, default=None,
                        help="Override per-provider concurrency (default: GH=5, LV=3, AS=3)")
    parser.add_argument("--delay", type=float, default=None,
                        help=f"Seconds between requests per slot (default: {REQUEST_DELAY})")
    parser.add_argument("--apply-to-manifests", action="store_true",
                        help="Write confirmed hits to manifest files")
    parser.add_argument("--dry-run", action="store_true",
                        help="With --apply-to-manifests: preview without writing")
    args = parser.parse_args()

    providers = [p.strip().upper() for p in args.providers.split(",")]
    invalid = [p for p in providers if p not in API_ENDPOINTS]
    if invalid:
        print(f"Unknown providers: {invalid}. Valid: {list(API_ENDPOINTS)}")
        sys.exit(1)

    delay = args.delay if args.delay is not None else REQUEST_DELAY

    companies = load_unidentified_companies()
    print(f"Loaded {len(companies)} companies without ATS config\n")

    raw_hits  = asyncio.run(run_probes(companies, providers, args.concurrency, delay=delay))
    hits      = deduplicate_hits(raw_hits)

    print_summary(hits)
    write_csv(hits, Path(args.output))

    if args.apply_to_manifests:
        apply_to_manifests(hits, dry_run=args.dry_run)
    else:
        print(f"\nReview the CSV, then re-run with --apply-to-manifests to write confirmed entries.")
        print(f"Or add --dry-run to preview first.")


if __name__ == "__main__":
    main()
