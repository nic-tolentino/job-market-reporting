#!/usr/bin/env python3
"""
Step 2+3 of the seed discovery pipeline: probe common career page paths and
fall back to homepage link extraction for companies that have no seeds and no
supported ATS config.

For each company with a website:
  1. HTTP GET against common career paths (/careers, /jobs, /work-with-us, ...)
  2. Follow redirects — final URL may reveal the ATS platform directly
  3. Scan response HTML for ATS embed signatures
  4. Count job-indicator keywords to assess whether the page has open roles
  5. If nothing found on probed paths: fetch homepage and scan <a href> tags
     for career-related links, then probe those

Outcomes written to manifests:
  - ATS config (provider + identifier) if a supported ATS is detected
  - Seed URL with status=active/empty/blocked if a careers page is found
  - Seed URL with status=unknown for timeouts on pages that looked promising

Usage:
    python3 scripts/ats/probe_career_paths.py               # dry-run by default
    python3 scripts/ats/probe_career_paths.py --apply       # write to manifests
    python3 scripts/ats/probe_career_paths.py --company-id canva --apply
    python3 scripts/ats/probe_career_paths.py --limit 50 --apply

Requirements:
    pip install aiohttp
"""

import argparse
import asyncio
import json
import re
import sys
from datetime import date
from pathlib import Path
from typing import Optional
from urllib.parse import urljoin, urlparse, unquote

try:
    import aiohttp
except ImportError:
    print("Error: aiohttp not found. Run: pip install aiohttp")
    sys.exit(1)

# ── Config ─────────────────────────────────────────────────────────────────────

MANIFEST_DIR  = Path(__file__).parent.parent.parent / "data" / "companies"
CONCURRENCY   = 10
REQUEST_DELAY = 0.1   # seconds per slot
PAGE_TIMEOUT  = 12    # seconds per request
READ_BYTES    = 40960 # 40 KB of HTML to scan

SUPPORTED_ATS = {"GREENHOUSE", "LEVER", "ASHBY", "SMARTRECRUITERS", "TEAMTAILOR", "WORKABLE"}
FREE_API_ATS  = {"GREENHOUSE", "LEVER", "ASHBY"}   # can write ats config + fetch jobs
TODAY         = date.today().isoformat()

CAREER_PATHS = [
    "/careers",
    "/jobs",
    "/about/careers",
    "/about-us/careers",
    "/company/careers",
    "/work-with-us",
    "/join-us",
    "/join-the-team",
    "/opportunities",
    "/vacancies",
    "/careers/",
    "/jobs/",
]

HEADERS = {
    "User-Agent": "Mozilla/5.0 (compatible; TechMarket-SeedDiscovery/1.0)",
    "Accept": "text/html,application/xhtml+xml",
}

# ── ATS detection patterns ─────────────────────────────────────────────────────
# Reuses the same patterns as scan_careers_pages.py — (provider, [(regex, group_idx)])
# group_idx = -1 means presence-only (no slug captured)

ATS_PATTERNS: dict[str, list[tuple[str, int]]] = {
    "GREENHOUSE": [
        (r"boards\.greenhouse\.io/embed/job_board(?:\.js)?\?for=([^&\"'\s]+)", 0),
        (r"boards\.greenhouse\.io/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
        (r"job-boards\.greenhouse\.io/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
    ],
    "LEVER": [
        (r"jobs\.lever\.co/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
    ],
    "ASHBY": [
        (r"jobs\.ashbyhq\.com/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
        (r'data-teamName=["\']([^"\']+)["\']', 0),
    ],
    "WORKABLE": [
        (r"apply\.workable\.com/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
        (r"cdn\.workable\.com", -1),
    ],
    "SMARTRECRUITERS": [
        (r"careers\.smartrecruiters\.com/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
        (r'data-company-id=["\']([^"\']+)["\']', 0),
    ],
    "TEAMTAILOR": [
        (r"career\.teamtailor\.com", -1),
        (r"teamtailor\.com", -1),
    ],
    "WORKDAY": [
        (r"([\w-]+\.wd\d+\.myworkdayjobs\.com/[\w-]+)", 0),
        (r"myworkdayjobs\.com", -1),
    ],
    "BAMBOOHR": [
        (r"([\w-]+)\.bamboohr\.com", 0),
    ],
    "SUCCESSFACTORS": [
        (r"successfactors\.com", -1),
        (r"jobs\.sap\.com", -1),
    ],
    "RECRUITEE": [
        (r"([\w-]+)\.recruitee\.com", 0),
    ],
    "BREEZY": [
        (r"([\w-]+)\.breezy\.hr", 0),
    ],
    "JOIN": [
        (r"join\.com/companies/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
    ],
    "PERSONIO": [
        (r"([\w-]+)\.jobs\.personio\.(?:de|com)", 0),
        (r"jobs\.personio\.com", -1),
    ],
    "FACTORIAL": [
        (r"([\w-]+)\.factorialhr\.com", 0),
    ],
    "EMPLOYMENT_HERO": [
        (r"(?!jobs\.)([a-zA-Z0-9_-]+)\.employmenthero\.com", 0),
    ],
}

JOB_INDICATORS = re.compile(
    r"job[s\-_]|position[s\-_]|vacanc|apply[\s\-]now|we.?re hiring|"
    r"JobPosting|\"title\"\s*:|open roles|current openings|career opportunities",
    re.IGNORECASE,
)

CAREER_LINK_RE = re.compile(
    r'href=["\']([^"\']*(?:/careers|/jobs|/work-with-us|/join-us|/join-the-team|/opportunities|/vacancies|/hiring)[^"\']*)["\']',
    re.IGNORECASE,
)


# ── ATS scan helpers ───────────────────────────────────────────────────────────

def scan_for_ats(html: str, final_url: str) -> list[tuple[str, Optional[str]]]:
    """Return list of (provider, slug_or_None) found in the HTML or final URL."""
    combined = final_url + "\n" + html
    found: dict[str, Optional[str]] = {}
    for provider, patterns in ATS_PATTERNS.items():
        for pattern, group_idx in patterns:
            if provider in found:
                break
            m = re.search(pattern, combined, re.IGNORECASE)
            if not m:
                continue
            if group_idx == -1:
                found[provider] = None
            else:
                try:
                    slug = m.group(group_idx + 1).rstrip("/").split("/")[0]
                    slug = unquote(slug).strip()
                    if provider == "GREENHOUSE" and slug.lower() == "embed":
                        continue
                    if slug:
                        found[provider] = slug
                except IndexError:
                    found[provider] = None
    return list(found.items())


def derive_seed_from_ats(provider: str, slug: Optional[str], final_url: str) -> Optional[str]:
    """Turn an ATS detection into a usable listing page seed URL."""
    if provider == "GREENHOUSE":
        return f"https://boards.greenhouse.io/{slug}" if slug else None
    if provider == "LEVER":
        return f"https://jobs.lever.co/{slug}" if slug else None
    if provider == "ASHBY":
        return f"https://jobs.ashbyhq.com/{slug}" if slug else None
    if provider == "WORKABLE":
        return f"https://apply.workable.com/{slug}" if slug else None
    if provider == "SMARTRECRUITERS":
        return f"https://careers.smartrecruiters.com/{slug}" if slug else None
    if provider == "TEAMTAILOR":
        return f"https://{slug}.teamtailor.com" if slug else None
    if provider == "WORKDAY":
        if slug:
            return f"https://{slug}"
        # Try to extract from final URL
        m = re.search(r"(https?://[\w-]+\.wd\d+\.myworkdayjobs\.com/[\w-]+)", final_url, re.I)
        return m.group(1) if m else None
    if provider == "BAMBOOHR":
        return f"https://{slug}.bamboohr.com/careers" if slug else None
    if provider == "RECRUITEE":
        return f"https://{slug}.recruitee.com" if slug else None
    if provider == "BREEZY":
        return f"https://{slug}.breezy.hr" if slug else None
    if provider == "JOIN":
        return f"https://join.com/companies/{slug}" if slug else None
    if provider == "PERSONIO":
        return f"https://{slug}.jobs.personio.com" if slug else None
    if provider == "FACTORIAL":
        return f"https://{slug}.factorialhr.com" if slug else None
    if provider == "EMPLOYMENT_HERO":
        return f"https://{slug}.employmenthero.com/jobs" if slug else None
    return None


def extract_career_links(html: str, base_url: str) -> list[str]:
    """Find on-site and ATS links that look like career pages."""
    links = set()
    base = urlparse(base_url)

    for m in CAREER_LINK_RE.finditer(html):
        href = m.group(1)
        if href.startswith("http"):
            parsed = urlparse(href)
            # Accept same-domain or known ATS domains
            if parsed.netloc == base.netloc or any(
                d in parsed.netloc for d in (
                    "greenhouse.io", "lever.co", "ashbyhq.com",
                    "workable.com", "smartrecruiters.com", "teamtailor.com",
                    "bamboohr.com", "recruitee.com", "breezy.hr",
                )
            ):
                links.add(href)
        elif href.startswith("/"):
            links.add(f"{base.scheme}://{base.netloc}{href}")

    return sorted(links)


# ── HTTP helpers ───────────────────────────────────────────────────────────────

async def fetch(session: aiohttp.ClientSession, url: str) -> Optional[tuple[str, str, int]]:
    """
    Returns (html, final_url, status) or None on error.
    Reads up to READ_BYTES of the response body.
    """
    try:
        async with session.get(
            url,
            headers=HEADERS,
            timeout=aiohttp.ClientTimeout(total=PAGE_TIMEOUT),
            allow_redirects=True,
            ssl=False,
        ) as resp:
            final_url = str(resp.url)
            raw = await resp.content.read(READ_BYTES)
            html = raw.decode("utf-8", errors="replace")
            return html, final_url, resp.status
    except Exception:
        return None


def classify(html: str, final_url: str, orig_url: str, status: int) -> str:
    """Classify a fetched page as active/empty/dead/blocked."""
    if status in (404, 410, 400):
        return "dead"
    if status == 403:
        return "blocked"
    # Check for off-domain redirect (e.g. error page on different host)
    orig_host = urlparse(orig_url).netloc.lower()
    final_host = urlparse(final_url).netloc.lower()
    if orig_host and final_host and orig_host not in final_host and final_host not in orig_host:
        return "dead"
    if len(html) < 300:
        return "blocked"
    hits = len(JOB_INDICATORS.findall(html))
    return "active" if hits >= 3 else "empty"


# ── Main probe logic ───────────────────────────────────────────────────────────

async def probe_company(
    session: aiohttp.ClientSession,
    sem: asyncio.Semaphore,
    company: dict,
) -> Optional[dict]:
    """
    Probe a single company. Returns a result dict or None if nothing found.
    """
    website = company["website"].rstrip("/")
    cid = company["id"]

    async with sem:
        # ── Step 2: probe common paths ─────────────────────────────────────────
        for path in CAREER_PATHS:
            await asyncio.sleep(REQUEST_DELAY)
            url = website + path
            result = await fetch(session, url)
            if result is None:
                continue
            html, final_url, status = result

            if status in (404, 410):
                continue  # try next path

            # ATS detection on final URL + HTML
            ats_hits = scan_for_ats(html, final_url)
            if ats_hits:
                provider, slug = ats_hits[0]  # take best match
                return {
                    "type": "ats",
                    "provider": provider,
                    "slug": slug,
                    "source_url": url,
                    "final_url": final_url,
                    **company,
                }

            page_status = classify(html, final_url, url, status)
            if page_status in ("active", "empty"):
                return {
                    "type": "seed",
                    "seed_url": final_url,
                    "seed_status": page_status,
                    "source": "path-probe",
                    **company,
                }

        # ── Step 3: homepage link extraction ──────────────────────────────────
        await asyncio.sleep(REQUEST_DELAY)
        result = await fetch(session, website)
        if result is None:
            return None
        html, final_url, status = result
        if status not in (200, 301, 302):
            return None

        # First check if homepage itself reveals ATS
        ats_hits = scan_for_ats(html, final_url)
        if ats_hits:
            provider, slug = ats_hits[0]
            return {
                "type": "ats",
                "provider": provider,
                "slug": slug,
                "source_url": website,
                "final_url": final_url,
                **company,
            }

        # Extract career links and probe the most promising one
        links = extract_career_links(html, website)
        for link in links[:3]:  # try up to 3 candidate links
            await asyncio.sleep(REQUEST_DELAY)
            result2 = await fetch(session, link)
            if result2 is None:
                continue
            html2, final_url2, status2 = result2

            ats_hits2 = scan_for_ats(html2, final_url2)
            if ats_hits2:
                provider, slug = ats_hits2[0]
                return {
                    "type": "ats",
                    "provider": provider,
                    "slug": slug,
                    "source_url": link,
                    "final_url": final_url2,
                    **company,
                }

            page_status = classify(html2, final_url2, link, status2)
            if page_status in ("active", "empty"):
                return {
                    "type": "seed",
                    "seed_url": final_url2,
                    "seed_status": page_status,
                    "source": "homepage-scan",
                    **company,
                }

    return None


# ── Manifest helpers ───────────────────────────────────────────────────────────

def load_targets(only_id: Optional[str] = None, limit: Optional[int] = None) -> list[dict]:
    """Companies with website, no supported ATS, and no active/non-dead seeds."""
    targets = []
    for f in sorted(MANIFEST_DIR.rglob("*.json")):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except Exception:
            continue
        if "name" not in data:
            continue

        if only_id and data.get("id") != only_id:
            continue

        provider = (data.get("ats") or {}).get("provider", "")
        if provider in SUPPORTED_ATS:
            continue

        seeds = data.get("crawler", {}).get("seeds", [])
        if any(s.get("status") in ("active", "empty", "blocked", "unknown") for s in seeds):
            continue  # already has live seeds

        website = data.get("website", "")
        if not website:
            continue

        targets.append({"id": data["id"], "name": data.get("name", ""), "website": website, "path": f})

        if limit and len(targets) >= limit:
            break

    return targets


def apply_result(result: dict, dry_run: bool) -> str:
    """Write the result back to the manifest. Returns a status string."""
    path: Path = result["path"]
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as e:
        return f"ERROR reading: {e}"

    if result["type"] == "ats":
        provider = result["provider"]
        slug = result.get("slug")

        if "ats" in data:
            return "SKIP (already has ats)"

        if provider in SUPPORTED_ATS and slug:
            data["ats"] = {"identifier": slug, "provider": provider}
            if not dry_run:
                path.write_text(json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n")
            return f"{'WOULD WRITE' if dry_run else 'WROTE'} ats={provider}/{slug}"
        else:
            # Non-supported ATS — write as seed instead
            seed_url = derive_seed_from_ats(provider, slug, result.get("final_url", ""))
            if not seed_url:
                return f"SKIP (no seed derivable for {provider})"
            return _write_seed(data, path, seed_url, "path-probe", "active", dry_run)

    else:  # seed
        return _write_seed(
            data, path,
            result["seed_url"],
            result["source"],
            result["seed_status"],
            dry_run,
        )


def _write_seed(data, path, url, source, status, dry_run):
    crawler = data.setdefault("crawler", {})
    seeds = crawler.setdefault("seeds", [])
    if any(s.get("url", "").lower() == url.lower() for s in seeds):
        return "SKIP (seed already present)"
    seeds.append({"category": "careers", "source": source, "status": status, "url": url})
    if not dry_run:
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n")
    return f"{'WOULD ADD' if dry_run else 'ADDED'} seed [{status}] {url[:60]}"


# ── Runner ─────────────────────────────────────────────────────────────────────

async def run(targets: list[dict], dry_run: bool) -> None:
    sem = asyncio.Semaphore(CONCURRENCY)
    connector = aiohttp.TCPConnector(ssl=False, limit=CONCURRENCY * 2)
    async with aiohttp.ClientSession(connector=connector) as session:
        tasks = [probe_company(session, sem, t) for t in targets]
        results_raw = await asyncio.gather(*tasks, return_exceptions=True)

    found = 0
    ats_written = 0
    seeds_written = 0

    for i, (target, raw) in enumerate(zip(targets, results_raw), 1):
        cid = target["id"]
        if isinstance(raw, Exception) or raw is None:
            print(f"  [{i:4}/{len(targets)}] ⬜ {cid}")
            continue

        status_str = apply_result(raw, dry_run)
        rtype = raw["type"]
        provider = raw.get("provider", "")
        icon = "✅" if "WROTE" in status_str or "WOULD" in status_str or "ADDED" in status_str else "⬜"
        print(f"  [{i:4}/{len(targets)}] {icon} {cid:<38} {rtype}:{provider or raw.get('seed_status','')}  {status_str}")

        if "SKIP" not in status_str:
            found += 1
            if rtype == "ats":
                ats_written += 1
            else:
                seeds_written += 1

    print(f"\n{'═'*62}")
    print(f"  Results")
    print(f"{'═'*62}")
    print(f"  Companies probed:   {len(targets)}")
    print(f"  ATS configs found:  {ats_written}")
    print(f"  Seeds found:        {seeds_written}")
    print(f"  No result:          {len(targets) - found}")
    if dry_run:
        print(f"\n  DRY RUN — re-run with --apply to write to manifests.")
    else:
        print(f"\n  Written to manifests. Next: python3 scripts/ats/validate_ats_configs.py")


# ── Main ───────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--apply", action="store_true", help="Write results to manifests (default: dry-run)")
    parser.add_argument("--company-id", help="Run for a single company ID only")
    parser.add_argument("--limit", type=int, help="Cap number of companies to probe")
    parser.add_argument("--concurrency", type=int, default=CONCURRENCY, help=f"Parallel requests (default: {CONCURRENCY})")
    args = parser.parse_args()

    targets = load_targets(only_id=args.company_id, limit=args.limit)
    dry_run = not args.apply

    if not targets:
        print("No companies to probe (all have seeds, supported ATS, or no website).")
        sys.exit(0)

    print(f"Probing {len(targets)} companies (paths + homepage scan)...")
    print(f"Mode: {'DRY RUN' if dry_run else 'APPLY'}\n")

    asyncio.run(run(targets, dry_run))


if __name__ == "__main__":
    main()
