#!/usr/bin/env python3
"""
Method D: Domain-slug based ATS discovery.

For companies with a known website but no ATS config, derives a slug from the
bare domain name (e.g. "atlassian" from "atlassian.com") and probes five providers:

  Free-API (job data fetchable):
    - GREENHOUSE  boards-api.greenhouse.io/v1/boards/{slug}/jobs
    - LEVER       api.lever.co/v0/postings/{slug}
    - ASHBY       api.ashbyhq.com/posting-api/job-board/{slug}

  Non-free (ATS identification only — jobs not fetchable via our pipeline):
    - SMARTRECRUITERS  api.smartrecruiters.com/v1/companies/{slug}/postings
    - TEAMTAILOR       {slug}.teamtailor.com  (verified via response body markers)

Results are written to a CSV for review, and optionally applied to manifests.

Usage:
    python3 scripts/ats/probe_vanity_domains.py
    python3 scripts/ats/probe_vanity_domains.py --apply-to-manifests
    python3 scripts/ats/probe_vanity_domains.py --providers GH LV AS SR TT
    python3 scripts/ats/probe_vanity_domains.py --delay 0.3 --concurrency 8

Output CSV columns:
    companyId, companyName, domain, provider, identifier, jobCount, method

Requirements:
    pip install aiohttp
"""

import argparse
import asyncio
import csv
import json
import re
import sys
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse

try:
    import aiohttp
except ImportError:
    print("Error: aiohttp not found. Run: pip install aiohttp")
    sys.exit(1)

# ── Config ────────────────────────────────────────────────────────────────────

MANIFEST_DIR = Path(__file__).parent.parent.parent / "data" / "companies"
DEFAULT_OUTPUT = Path(__file__).parent / "vanity_domain_hits.csv"

USER_AGENT = "TechMarket/1.0 (+https://techmarket.dev)"
DEFAULT_CONCURRENCY = 6
DEFAULT_DELAY = 0.2  # seconds between requests per provider slot

# Providers enabled by default (short codes used in --providers flag)
ALL_PROVIDERS = ["GH", "LV", "AS", "SR", "TT"]

# SmartRecruiters: the postings API returns 200 + {"totalFound":0,"content":[]} for ANY
# slug — it cannot be used as an existence check.  Instead, use the board redirect:
#   https://jobs.smartrecruiters.com/{slug}
#     → careers.smartrecruiters.com/{slug}   (real board)
#     → jobs.smartrecruiters.com/            (fake / not found — root redirect)
# Once the board is confirmed via redirect, the postings API is used for job count only.
# Note: SR slugs sometimes include a numeric suffix (e.g. "LendiGroup1") that
# can't be derived from the domain alone — the domain-slug probe catches the
# simple cases only.
SR_BOARD = "https://jobs.smartrecruiters.com/{slug}"
SR_API   = "https://api.smartrecruiters.com/v1/companies/{slug}/postings?status=PUBLISHED&limit=10"

# TeamTailor: career boards live at {slug}.teamtailor.com.
# We verify a real TT board by checking for teamtailor.com references in the
# HTML response.  The public jobs feed returns JSON we can count.
TT_BOARD   = "https://{slug}.teamtailor.com"
TT_FEED    = "https://{slug}.teamtailor.com/feed/jobs.json"
TT_MARKERS = ("teamtailor.com", "teamtailor-careers")

PROBE_TEMPLATES = {
    "GREENHOUSE": ["https://boards-api.greenhouse.io/v1/boards/{slug}/jobs"],
    "LEVER":      ["https://api.lever.co/v0/postings/{slug}"],
    "ASHBY":      ["https://api.ashbyhq.com/posting-api/job-board/{slug}"],
}


# ── Manifest loading ──────────────────────────────────────────────────────────

def load_companies_without_ats(only_ids: Optional[set] = None) -> list[dict]:
    """
    Load companies that have a website but no ATS config.
    Returns list of dicts with id, name, website, path.
    If only_ids is provided, restrict to those company IDs.
    """
    companies = []
    for f in MANIFEST_DIR.rglob("*.json"):
        if f.name == "schema.json":
            continue
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
            if "ats" in data:
                continue  # already configured
            if only_ids and data["id"] not in only_ids:
                continue
            website = data.get("website")
            if not website:
                continue
            companies.append({
                "id":      data["id"],
                "name":    data.get("name", ""),
                "website": website,
                "path":    f,
            })
        except Exception:
            pass
    return companies


def extract_domain_slug(website: str) -> Optional[str]:
    """
    Given a URL like "https://www.atlassian.com" return "atlassian".
    Strips www., common ccTLDs, and multi-part TLDs.
    """
    try:
        host = urlparse(website).hostname or ""
    except Exception:
        return None

    # Remove www. or similar prefixes
    host = re.sub(r"^www\d*\.", "", host)

    # Split on dots and take the first label (the effective SLD)
    parts = host.split(".")
    if not parts:
        return None

    slug = parts[0]
    # Discard short/generic slugs
    if len(slug) < 3:
        return None
    return slug.lower()


# ── HTTP probing ──────────────────────────────────────────────────────────────

async def probe_greenhouse(session: aiohttp.ClientSession, slug: str, delay: float) -> Optional[int]:
    """Returns job count if the Greenhouse board exists, else None."""
    url = PROBE_TEMPLATES["GREENHOUSE"][0].format(slug=slug)
    try:
        await asyncio.sleep(delay)
        async with session.get(url, timeout=aiohttp.ClientTimeout(total=8)) as resp:
            if resp.status == 200:
                body = await resp.json(content_type=None)
                return len(body.get("jobs", []))
    except Exception:
        pass
    return None


async def probe_lever(session: aiohttp.ClientSession, slug: str, delay: float) -> Optional[int]:
    """Returns posting count if the Lever board exists, else None."""
    url = PROBE_TEMPLATES["LEVER"][0].format(slug=slug)
    try:
        await asyncio.sleep(delay)
        async with session.get(url, timeout=aiohttp.ClientTimeout(total=8)) as resp:
            if resp.status == 200:
                body = await resp.json(content_type=None)
                if isinstance(body, list):
                    return len(body)
                if isinstance(body, dict) and "postings" in body:
                    return len(body["postings"])
    except Exception:
        pass
    return None


async def probe_ashby(session: aiohttp.ClientSession, slug: str, delay: float) -> Optional[int]:
    """Returns posting count if the Ashby job board exists and returns data, else None."""
    url = PROBE_TEMPLATES["ASHBY"][0].format(slug=slug)
    try:
        await asyncio.sleep(delay)
        async with session.get(url, timeout=aiohttp.ClientTimeout(total=8)) as resp:
            if resp.status == 200:
                body = await resp.json(content_type=None)
                # Ashby SPA returns 200 for any URL — only accept if jobPostings key exists
                if isinstance(body, dict) and "jobPostings" in body:
                    return len(body["jobPostings"])
    except Exception:
        pass
    return None


async def probe_smartrecruiters(
    session: aiohttp.ClientSession, slug: str, delay: float
) -> tuple[Optional[int], Optional[str]]:
    """
    Returns (job_count, matched_slug) if the SmartRecruiters board exists, else (None, None).

    Discriminates real boards via redirect:
      jobs.smartrecruiters.com/{slug}  →  careers.smartrecruiters.com/{actual_slug}  (real)
      jobs.smartrecruiters.com/{slug}  →  jobs.smartrecruiters.com/                  (fake)

    Once confirmed, the postings API is used for job count only.
    The actual slug is extracted from the redirect URL so we capture the correct casing.
    """
    for candidate in _sr_candidates(slug):
        board_url = SR_BOARD.format(slug=candidate)
        try:
            await asyncio.sleep(delay)
            async with session.get(
                board_url, timeout=aiohttp.ClientTimeout(total=10), allow_redirects=True
            ) as resp:
                final_url = str(resp.url)
                if "careers.smartrecruiters.com" not in final_url:
                    continue  # root or other redirect — not a real board

                # Extract the actual slug from the redirect path (may differ in casing)
                path_slug = final_url.rstrip("/").split("/")[-1]
                actual_slug = path_slug if path_slug else candidate

                # Board confirmed — fetch job count via postings API
                try:
                    api_url = SR_API.format(slug=actual_slug)
                    async with session.get(api_url, timeout=aiohttp.ClientTimeout(total=8)) as api_resp:
                        if api_resp.status == 200:
                            body = await api_resp.json(content_type=None)
                            count = body.get("totalFound", len(body.get("content", [])))
                            return count, actual_slug
                except Exception:
                    pass

                return 0, actual_slug  # board confirmed but count unavailable
        except Exception:
            pass

    return None, None


def _sr_candidates(slug: str) -> list[str]:
    """SmartRecruiters slug variants to try, in priority order."""
    candidates = [slug, slug.capitalize(), slug.upper()]
    # Also try title-case of each word (e.g. "covergenius" → "CoverGenius")
    titled = "".join(w.capitalize() for w in re.split(r"[-_]", slug))
    if titled not in candidates:
        candidates.append(titled)
    # Deduplicate while preserving order
    seen: set[str] = set()
    return [c for c in candidates if not (c in seen or seen.add(c))]  # type: ignore[func-returns-value]


async def probe_teamtailor(session: aiohttp.ClientSession, slug: str, delay: float) -> Optional[int]:
    """
    Returns job count if {slug}.teamtailor.com is a real TeamTailor board, else None.
    Verification step: response body must contain a TeamTailor-specific marker so we
    don't accidentally match a company that happens to own that subdomain independently.
    Falls back to 0 if the feed endpoint is unavailable but the board page is confirmed.
    """
    board_url = TT_BOARD.format(slug=slug)
    feed_url  = TT_FEED.format(slug=slug)
    try:
        await asyncio.sleep(delay)
        async with session.get(board_url, timeout=aiohttp.ClientTimeout(total=10)) as resp:
            if resp.status != 200:
                return None
            text = await resp.text(errors="replace")
            if not any(marker in text for marker in TT_MARKERS):
                return None  # not a TeamTailor board
    except Exception:
        return None

    # Board confirmed — try to count jobs from the JSON feed
    try:
        async with session.get(feed_url, timeout=aiohttp.ClientTimeout(total=8)) as resp:
            if resp.status == 200:
                body = await resp.json(content_type=None)
                if isinstance(body, list):
                    return len(body)
                if isinstance(body, dict):
                    jobs = body.get("jobs", body.get("data", []))
                    return len(jobs)
    except Exception:
        pass

    return 0  # board exists but feed unavailable; count unknown


async def probe_company(
    session: aiohttp.ClientSession,
    company: dict,
    semaphores: dict[str, asyncio.Semaphore],
    delay: float,
    enabled: set[str],
) -> list[dict]:
    """
    Probe enabled providers for a single company.
    Returns a list of hit dicts (may be empty).
    """
    slug = extract_domain_slug(company["website"])
    if not slug:
        return []

    hits = []

    def _hit(provider: str, identifier: str, count: int) -> dict:
        return {
            "companyId":   company["id"],
            "companyName": company["name"],
            "domain":      company["website"],
            "provider":    provider,
            "identifier":  identifier,
            "jobCount":    count,
            "method":      "domain-slug",
            "_path":       company["path"],
        }

    if "GH" in enabled:
        async with semaphores["GH"]:
            count = await probe_greenhouse(session, slug, delay)
            if count is not None:
                hits.append(_hit("GREENHOUSE", slug, count))

    if "LV" in enabled:
        async with semaphores["LV"]:
            count = await probe_lever(session, slug, delay)
            if count is not None:
                hits.append(_hit("LEVER", slug, count))

    if "AS" in enabled:
        async with semaphores["AS"]:
            count = await probe_ashby(session, slug, delay)
            if count is not None:
                hits.append(_hit("ASHBY", slug, count))

    if "SR" in enabled:
        async with semaphores["SR"]:
            count, matched_slug = await probe_smartrecruiters(session, slug, delay)
            if count is not None:
                hits.append(_hit("SMARTRECRUITERS", matched_slug, count))

    if "TT" in enabled:
        async with semaphores["TT"]:
            count = await probe_teamtailor(session, slug, delay)
            if count is not None:
                hits.append(_hit("TEAMTAILOR", slug, count))

    return hits


# ── Orchestration ─────────────────────────────────────────────────────────────

async def run_probes(companies: list[dict], concurrency: int, delay: float, enabled: set[str]) -> list[dict]:
    # TeamTailor needs a lower concurrency — it fetches a full HTML page per probe
    tt_concurrency = max(1, concurrency // 2)
    semaphores = {
        "GH": asyncio.Semaphore(concurrency),
        "LV": asyncio.Semaphore(concurrency),
        "AS": asyncio.Semaphore(concurrency),
        "SR": asyncio.Semaphore(concurrency),
        "TT": asyncio.Semaphore(tt_concurrency),
    }

    headers = {"User-Agent": USER_AGENT}
    connector = aiohttp.TCPConnector(limit=concurrency * len(enabled))

    all_hits: list[dict] = []

    async with aiohttp.ClientSession(headers=headers, connector=connector) as session:
        tasks = [
            probe_company(session, company, semaphores, delay, enabled)
            for company in companies
        ]
        for i, coro in enumerate(asyncio.as_completed(tasks), 1):
            results = await coro
            if results:
                for hit in results:
                    print(f"  HIT  {hit['companyId']:<35} {hit['provider']:<16} "
                          f"{hit['identifier']:<30} ({hit['jobCount']} jobs)")
                all_hits.extend(results)
            if i % 50 == 0:
                print(f"  ... {i}/{len(tasks)} companies probed, {len(all_hits)} hits so far")

    return all_hits


# ── Output ────────────────────────────────────────────────────────────────────

def write_csv(hits: list[dict], output_path: Path) -> None:
    fieldnames = ["companyId", "companyName", "domain", "provider", "identifier", "jobCount", "method"]
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for hit in hits:
            writer.writerow({k: hit[k] for k in fieldnames})
    print(f"\nWrote {len(hits)} hits → {output_path}")


def apply_to_manifests(hits: list[dict], dry_run: bool = False) -> None:
    """
    Write confirmed ATS configs to manifest files.
    Where multiple providers hit for the same company, picks the one with the most jobs.
    """
    # Group by company, pick best provider per company
    best: dict[str, dict] = {}
    for hit in hits:
        cid = hit["companyId"]
        if cid not in best or hit["jobCount"] > best[cid]["jobCount"]:
            best[cid] = hit

    print(f"\n{'─'*60}")
    tag = "DRY RUN — would write" if dry_run else "Writing"
    print(f"  {tag} {len(best)} ATS configs to manifests...")

    written = 0
    for cid, hit in sorted(best.items()):
        path: Path = hit["_path"]
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            if "ats" in data:
                print(f"  SKIP {cid} — already has ATS")
                continue

            data["ats"] = {
                "identifier": hit["identifier"],
                "provider":   hit["provider"],
            }

            if not dry_run:
                path.write_text(
                    json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
                    encoding="utf-8",
                )

            verb = "WOULD WRITE" if dry_run else "WROTE"
            print(f"  {verb} {cid:<40} {hit['provider']}/{hit['identifier']} ({hit['jobCount']} jobs)")
            written += 1
        except Exception as e:
            print(f"  ERROR {cid}: {e}")

    print(f"\n  {'Would write' if dry_run else 'Wrote'} {written} manifest files.")
    if not dry_run:
        print("  Run validate_ats_configs.py to verify the new entries against live APIs.")


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--output",      default=str(DEFAULT_OUTPUT), help="Output CSV path")
    parser.add_argument("--concurrency", type=int,   default=DEFAULT_CONCURRENCY, help="Semaphore size per provider")
    parser.add_argument("--delay",       type=float, default=DEFAULT_DELAY,       help="Seconds delay per request slot")
    parser.add_argument("--providers",   nargs="+",  default=ALL_PROVIDERS,
                        metavar="P",
                        help=f"Providers to probe (default: all). Choices: {ALL_PROVIDERS}. "
                             "E.g. --providers SR TT   to only probe the two new providers.")
    parser.add_argument("--apply-to-manifests", action="store_true",
                        help="Write confirmed new ATS configs directly to manifest files")
    parser.add_argument("--dry-run", action="store_true",
                        help="With --apply-to-manifests: preview without writing")
    parser.add_argument("--company-ids", nargs="+", metavar="ID",
                        help="Restrict to these company IDs only (space-separated)")
    args = parser.parse_args()

    enabled = set(p.upper() for p in args.providers)
    unknown = enabled - set(ALL_PROVIDERS)
    if unknown:
        parser.error(f"Unknown provider(s): {unknown}. Valid: {ALL_PROVIDERS}")

    provider_names = {"GH": "GREENHOUSE", "LV": "LEVER", "AS": "ASHBY",
                      "SR": "SMARTRECRUITERS", "TT": "TEAMTAILOR"}
    active = " / ".join(provider_names[p] for p in ALL_PROVIDERS if p in enabled)

    only_ids = set(args.company_ids) if args.company_ids else None
    companies = load_companies_without_ats(only_ids=only_ids)
    print(f"Companies without ATS config (with website): {len(companies)}")
    print(f"Probing {active} using domain-slug method...\n")

    hits = asyncio.run(run_probes(companies, args.concurrency, args.delay, enabled))

    print(f"\n{'═'*60}")
    print(f"  Domain-slug probe results")
    print(f"{'═'*60}")
    print(f"  Total hits: {len(hits)}")
    from collections import Counter
    for provider, count in Counter(h["provider"] for h in hits).most_common():
        print(f"    {provider:<18} {count}")

    write_csv(hits, Path(args.output))

    if args.apply_to_manifests:
        apply_to_manifests(hits, dry_run=args.dry_run)
    else:
        print(f"\nReview the CSV, then re-run with --apply-to-manifests to write confirmed entries.")


if __name__ == "__main__":
    main()
