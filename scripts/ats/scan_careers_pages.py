#!/usr/bin/env python3
"""
Method C: Scan careers pages for ATS embed patterns and robots.txt restrictions.

For every company with crawl seeds but no ATS config, fetches the seed URL and
searches the raw HTML for known ATS signatures. Also checks robots.txt for each
company's domain to flag crawl restrictions.

What it writes to manifests:
  - `ats` config if a free-API provider (Greenhouse/Lever/Ashby) is detected
  - `crawler.robots` with `{ "checked_at", "blocked", "disallow_patterns", "crawl_delay" }`
  - `crawler.discovery` note for non-free ATSs detected (Workday, Workable, etc.)

Usage:
    python3 scripts/ats/scan_careers_pages.py
    python3 scripts/ats/scan_careers_pages.py --output /tmp/scan_results.csv
    python3 scripts/ats/scan_careers_pages.py --dry-run        # preview, no writes
    python3 scripts/ats/scan_careers_pages.py --concurrency 5

Requirements:
    pip install aiohttp
"""

import asyncio
import json
import re
import csv
import sys
import argparse
from datetime import date
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse, urljoin, unquote, quote

try:
    import aiohttp
except ImportError:
    print("Error: aiohttp not found. Run: pip install aiohttp")
    sys.exit(1)

# ── Config ────────────────────────────────────────────────────────────────────

MANIFEST_DIR  = Path(__file__).parent.parent.parent / "data" / "companies"
DEFAULT_OUTPUT = Path(__file__).parent / "careers_scan_results.csv"

TODAY = date.today().isoformat()

CONCURRENCY    = 8    # simultaneous page fetches
REQUEST_DELAY  = 0.1  # seconds between fetches per slot
PAGE_TIMEOUT   = 15   # seconds per page request
ROBOTS_TIMEOUT = 8    # seconds for robots.txt fetch

# ── ATS detection patterns ────────────────────────────────────────────────────
#
# Each entry:  PROVIDER → list of (regex, slug_group_index)
# Slug group 0 means the first capture group in the pattern.
#
# "FREE_API" providers: we write an `ats` config to the manifest.
# Others: we note the detected ATS in crawler.discovery.
#
FREE_API_PROVIDERS = {"GREENHOUSE", "LEVER", "ASHBY"}

ATS_PATTERNS: dict[str, list[tuple[str, int]]] = {
    # ── Free API ──────────────────────────────────────────────────────────────
    "GREENHOUSE": [
        # Embed widget  <script src="...job_board.js?for=SLUG">
        (r"boards\.greenhouse\.io/embed/job_board(?:\.js)?\?for=([^&\"'\s]+)", 0),
        # Direct board link  href="https://boards.greenhouse.io/SLUG"
        (r"boards\.greenhouse\.io/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
        # New board URL pattern
        (r"job-boards\.greenhouse\.io/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
    ],
    "LEVER": [
        # Embed or link  jobs.lever.co/SLUG
        (r"jobs\.lever\.co/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
    ],
    "ASHBY": [
        # Embed or link  jobs.ashbyhq.com/SLUG
        (r"jobs\.ashbyhq\.com/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
        # Widget attribute  data-teamName="SLUG"
        (r'data-teamName=["\']([^"\']+)["\']', 0),
    ],
    # ── Tracked (non-free) ────────────────────────────────────────────────────
    "WORKDAY": [
        # myworkdayjobs.com/en-US/SLUG  or  COMPANY.wd1.myworkdayjobs.com
        (r"([\w-]+)\.(?:wd\d+\.)?myworkdayjobs\.com", 0),
    ],
    "WORKABLE": [
        # apply.workable.com/SLUG
        (r"apply\.workable\.com/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
        # Workable widget script
        (r"cdn\.workable\.com", -1),   # -1 = no slug capture, just presence
    ],
    "SMARTRECRUITERS": [
        # careers.smartrecruiters.com/SLUG
        (r"careers\.smartrecruiters\.com/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
        # Widget data attribute
        (r'data-company-id=["\']([^"\']+)["\']', 0),
    ],
    "BAMBOOHR": [
        # COMPANY.bamboohr.com
        (r"([\w-]+)\.bamboohr\.com", 0),
    ],
    "TEAMTAILOR": [
        # career.teamtailor.com or TeamTailor CDN/script
        (r"career\.teamtailor\.com", -1),
        (r"teamtailor\.com", -1),
    ],
    "SUCCESSFACTORS": [
        # jobs.sap.com or successfactors.com career pages
        (r"successfactors\.com", -1),
        (r"jobs\.sap\.com", -1),
    ],
    "PERSONIO": [
        # COMPANY.jobs.personio.de  or  jobs.personio.com/job/
        (r"([\w-]+)\.jobs\.personio\.(?:de|com)", 0),
        (r"jobs\.personio\.com", -1),
    ],
    "BREEZY": [
        # COMPANY.breezy.hr
        (r"([\w-]+)\.breezy\.hr", 0),
    ],
    "RECRUITEE": [
        # COMPANY.recruitee.com
        (r"([\w-]+)\.recruitee\.com", 0),
    ],
    "FACTORIAL": [
        (r"factorialhr\.com/jobs", -1),
        (r"factorialhr\.es/jobs", -1),
    ],
    "JOBADDER": [
        (r"jobadder\.com", -1),
        (r"app\.jobadder\.com", -1),
    ],
    "SNAPHIRE": [
        (r"snaphire\.com", -1),
    ],
    "JOIN": [
        # join.com/companies/SLUG
        (r"join\.com/companies/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
    ],
    "COMEET": [
        (r"comeet\.com/jobs/([^\"'\s/?#]+)(?:[\"'\s/?#]|$)", 0),
    ],
    "PINPOINT": [
        (r"([\w-]+)\.pinpointhq\.com", 0),
    ],
    "JAZZ": [
        (r"([\w-]+)\.applytojob\.com", 0),
        (r"app\.jazz\.co/apply/([^\"'\s/?#]+)", 0),
    ],
    "MYRECRUITMENTPLUS": [
        (r"myrecruitmentplus\.com", -1),
    ],
    "ZOHO": [
        (r"zoho\.com/recruit", -1),
        (r"jobs\.zoho\.com", -1),
    ],
}


def scan_html_for_ats(html: str) -> list[tuple[str, Optional[str]]]:
    """
    Returns list of (provider, identifier_or_None) found in the HTML.
    Deduplicates: returns at most one entry per provider.
    """
    found: dict[str, Optional[str]] = {}

    for provider, patterns in ATS_PATTERNS.items():
        for pattern, group_idx in patterns:
            if provider in found:
                break
            m = re.search(pattern, html, re.IGNORECASE)
            if not m:
                continue
            if group_idx == -1:
                # Presence-only match — no slug extractable
                found[provider] = None
            else:
                slug = m.group(group_idx + 1).rstrip("/").split("/")[0]
                slug = unquote(slug).strip()
                # Reject Greenhouse embed path
                if provider == "GREENHOUSE" and slug.lower() == "embed":
                    continue
                if slug:
                    found[provider] = slug

    return list(found.items())


def extract_careers_links(html: str, base_url: str) -> list[str]:
    """
    Find links in the page that look like ATS job board URLs or
    on-site careers subpages — potential new seeds.
    """
    links = set()

    # ATS-hosted board URLs (these make excellent seeds or reveal ATS directly)
    ats_link_patterns = [
        r'href=["\']((https?://)?(?:boards|jobs)\.greenhouse\.io/[^"\']+)["\']',
        r'href=["\']((https?://)?jobs\.lever\.co/[^"\']+)["\']',
        r'href=["\']((https?://)?jobs\.ashbyhq\.com/[^"\']+)["\']',
        r'href=["\']((https?://)?apply\.workable\.com/[^"\']+)["\']',
        r'href=["\']((https?://)?careers\.smartrecruiters\.com/[^"\']+)["\']',
    ]
    for pat in ats_link_patterns:
        for m in re.finditer(pat, html, re.IGNORECASE):
            url = m.group(1)
            if not url.startswith("http"):
                url = "https://" + url
            links.add(url)

    # On-site /jobs or /careers subpages
    onsite_pattern = r'href=["\']([^"\']*(?:/careers|/jobs|/work-with-us|/join-us)[^"\']*)["\']'
    base_parsed = urlparse(base_url)
    for m in re.finditer(onsite_pattern, html, re.IGNORECASE):
        href = m.group(1)
        if href.startswith("http"):
            parsed = urlparse(href)
            if parsed.netloc == base_parsed.netloc:
                links.add(href)
        elif href.startswith("/"):
            links.add(f"{base_parsed.scheme}://{base_parsed.netloc}{href}")

    return sorted(links)


# ── robots.txt ────────────────────────────────────────────────────────────────

def parse_robots_txt(content: str, seed_path: str) -> dict:
    """
    Returns:
      blocked (bool): True if the seed path is disallowed for all crawlers
      disallow_patterns (list[str]): all Disallow: values for User-agent: *
      crawl_delay (int|None): Crawl-delay value if present
    """
    disallow_patterns: list[str] = []
    crawl_delay: Optional[int] = None
    in_star_agent = False

    for line in content.splitlines():
        line = line.strip()
        if line.lower().startswith("user-agent:"):
            agent = line.split(":", 1)[1].strip()
            in_star_agent = (agent == "*")
        elif in_star_agent:
            if line.lower().startswith("disallow:"):
                path = line.split(":", 1)[1].strip()
                if path:
                    disallow_patterns.append(path)
            elif line.lower().startswith("crawl-delay:"):
                try:
                    crawl_delay = int(line.split(":", 1)[1].strip())
                except ValueError:
                    pass

    def is_blocked(path: str) -> bool:
        for pattern in disallow_patterns:
            if pattern == "/":
                return True
            if path.startswith(pattern):
                return True
        return False

    blocked = is_blocked(seed_path) or is_blocked("/careers") or is_blocked("/jobs")

    return {
        "blocked": blocked,
        "disallow_patterns": disallow_patterns[:20],  # cap to avoid huge lists
        "crawl_delay": crawl_delay,
    }


# ── Async fetching ────────────────────────────────────────────────────────────

async def fetch_robots(
    session: aiohttp.ClientSession,
    base_url: str,
) -> Optional[str]:
    robots_url = f"{base_url.rstrip('/')}/robots.txt"
    # Strip path from base_url to get origin
    parsed = urlparse(base_url)
    robots_url = f"{parsed.scheme}://{parsed.netloc}/robots.txt"
    try:
        async with session.get(
            robots_url,
            timeout=aiohttp.ClientTimeout(total=ROBOTS_TIMEOUT),
            allow_redirects=True,
        ) as resp:
            if resp.status == 200:
                return await resp.text(errors="replace")
    except Exception:
        pass
    return None


async def fetch_page(
    session: aiohttp.ClientSession,
    semaphore: asyncio.Semaphore,
    url: str,
) -> Optional[str]:
    async with semaphore:
        await asyncio.sleep(REQUEST_DELAY)
        try:
            async with session.get(
                url,
                timeout=aiohttp.ClientTimeout(total=PAGE_TIMEOUT),
                allow_redirects=True,
            ) as resp:
                if resp.status == 200:
                    return await resp.text(errors="replace")
        except Exception:
            pass
    return None


# ── Manifest loading ──────────────────────────────────────────────────────────

def load_target_companies() -> list[dict]:
    """Companies with seeds but no ATS config."""
    companies = []
    for path in sorted(MANIFEST_DIR.rglob("*.json")):
        if path.name == "schema.json":
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            seeds = data.get("crawler", {}).get("seeds", [])
            if seeds and "ats" not in data:
                companies.append({
                    "id":      data["id"],
                    "name":    data.get("name", ""),
                    "seeds":   [s["url"] for s in seeds if s.get("url")],
                    "website": data.get("website", ""),
                    "path":    path,
                })
        except Exception:
            pass
    return companies


# ── Main scan loop ────────────────────────────────────────────────────────────

async def scan_all(
    companies: list[dict],
    concurrency: int,
    dry_run: bool,
) -> list[dict]:
    semaphore = asyncio.Semaphore(concurrency)
    results: list[dict] = []
    total = len(companies)
    done = 0
    lock = asyncio.Lock()

    connector = aiohttp.TCPConnector(limit=concurrency + 4, ssl=False)
    headers = {
        "User-Agent": "Mozilla/5.0 (compatible; job-market-research/1.0; ATS discovery)",
        "Accept": "text/html,application/xhtml+xml,*/*;q=0.9",
        "Accept-Language": "en-US,en;q=0.9",
    }

    async with aiohttp.ClientSession(connector=connector, headers=headers) as session:

        async def scan_one(company: dict) -> None:
            nonlocal done
            cid   = company["id"]
            seeds = company["seeds"]
            seed_url = seeds[0]  # use first seed

            parsed_seed = urlparse(seed_url)
            seed_path   = parsed_seed.path or "/"

            # ── robots.txt ────────────────────────────────────────────────────
            robots_info: Optional[dict] = None
            robots_raw = await fetch_robots(session, seed_url)
            if robots_raw is not None:
                robots_info = parse_robots_txt(robots_raw, seed_path)
                robots_info["checked_at"] = TODAY

            # ── Page scan ─────────────────────────────────────────────────────
            html = await fetch_page(session, semaphore, seed_url)

            ats_hits: list[tuple[str, Optional[str]]] = []
            new_links: list[str] = []

            if html:
                ats_hits  = scan_html_for_ats(html)
                new_links = extract_careers_links(html, seed_url)

            result = {
                "companyId":   cid,
                "companyName": company["name"],
                "seedUrl":     seed_url,
                "robotsBlocked": robots_info.get("blocked") if robots_info else None,
                "robotsDelay":   robots_info.get("crawl_delay") if robots_info else None,
                "atsFound":    ", ".join(
                    f"{p}/{s}" if s else p
                    for p, s in ats_hits
                ) if ats_hits else "",
                "newLinks":    "; ".join(new_links[:5]),
                "_robots_info": robots_info,
                "_ats_hits":    ats_hits,
                "_new_links":   new_links,
                "_path":        company["path"],
            }

            # ── Write to manifest ─────────────────────────────────────────────
            if not dry_run:
                _write_manifest(company["path"], robots_info, ats_hits, new_links)

            async with lock:
                done += 1
                results.append(result)
                if done % 20 == 0 or done == total:
                    pct = done / total * 100
                    ats_count = sum(1 for r in results if r["atsFound"])
                    print(f"  [{done:>{len(str(total))}}/{total}]  {pct:4.0f}%  ATS found: {ats_count}", end="\r")

        await asyncio.gather(*[scan_one(c) for c in companies])

    print(f"\n")
    return results


def _write_manifest(
    path: Path,
    robots_info: Optional[dict],
    ats_hits: list[tuple[str, Optional[str]]],
    new_links: list[str],
) -> None:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return

    changed = False

    # ── robots ────────────────────────────────────────────────────────────────
    if robots_info:
        if "crawler" not in data:
            data["crawler"] = {"seeds": []}
        data["crawler"]["robots"] = {
            "blocked":           robots_info["blocked"],
            "checked_at":        robots_info["checked_at"],
            "crawl_delay":       robots_info.get("crawl_delay"),
            "disallow_patterns": robots_info.get("disallow_patterns", []),
        }
        changed = True

    # ── ATS config (free providers only) ──────────────────────────────────────
    if "ats" not in data:
        for provider, identifier in ats_hits:
            if provider in FREE_API_PROVIDERS and identifier:
                data["ats"] = {"identifier": identifier, "provider": provider}
                changed = True
                break  # first free-API hit wins

    # ── discovery note (non-free ATSs) ────────────────────────────────────────
    non_free = [
        (p, s) for p, s in ats_hits
        if p not in FREE_API_PROVIDERS
        and "ats" not in data  # don't overwrite a just-written ats
    ]
    # Also note if a free-API ATS was found but ats is already set
    if non_free and "ats" not in data:
        provider, identifier = non_free[0]
        slug_str = f" ({identifier})" if identifier else ""
        note = f"Careers page scan detected {provider}{slug_str} — requires paid/custom integration"
        if "crawler" not in data:
            data["crawler"] = {"seeds": []}
        existing_discovery = data["crawler"].get("discovery", {})
        data["crawler"]["discovery"] = {
            **existing_discovery,
            "lastAttemptedAt": TODAY,
            "notes": note,
            "status": "NO_MATCH",
        }
        changed = True

    # ── new seeds from discovered ATS-hosted links ─────────────────────────────
    if new_links and "ats" not in data:
        # Only add ATS-hosted links as seeds (they're high-quality)
        ats_hosted = [
            l for l in new_links
            if any(domain in l for domain in [
                "greenhouse.io", "lever.co", "ashbyhq.com",
                "workable.com", "smartrecruiters.com",
            ])
        ]
        if ats_hosted:
            existing_seeds = data.get("crawler", {}).get("seeds", [])
            existing_urls  = {s["url"] for s in existing_seeds}
            for link in ats_hosted[:3]:  # cap at 3 new seeds
                if link not in existing_urls:
                    existing_seeds.append({
                        "category": "discovered",
                        "status":   "active",
                        "url":      link,
                    })
                    existing_urls.add(link)
                    changed = True
            if "crawler" not in data:
                data["crawler"] = {}
            data["crawler"]["seeds"] = existing_seeds

    if changed:
        path.write_text(
            json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
            encoding="utf-8",
        )


# ── Output ────────────────────────────────────────────────────────────────────

CSV_FIELDS = [
    "companyId", "companyName", "seedUrl",
    "robotsBlocked", "robotsDelay", "atsFound", "newLinks",
]


def write_csv(results: list[dict], output_path: Path) -> None:
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
        writer.writeheader()
        for r in sorted(results, key=lambda x: x["companyId"]):
            writer.writerow({k: r.get(k, "") for k in CSV_FIELDS})
    print(f"Wrote {len(results)} rows → {output_path}")


def print_summary(results: list[dict]) -> None:
    from collections import Counter

    ats_found   = [r for r in results if r["atsFound"]]
    robots_blocked = [r for r in results if r["robotsBlocked"]]
    free_api    = [r for r in results if any(
        p in FREE_API_PROVIDERS
        for p in r["atsFound"].split(", ") if r["atsFound"]
    )]
    has_new     = [r for r in results if r["newLinks"]]

    print(f"\n{'═'*60}")
    print(f"  Careers page scan results")
    print(f"{'═'*60}")
    print(f"  Companies scanned:          {len(results)}")
    print(f"  ATS detected:               {len(ats_found)}")
    print(f"    of which free-API:        {len(free_api)}")
    print(f"  robots.txt blocked:         {len(robots_blocked)}")
    print(f"  New links discovered:       {len(has_new)}")
    print()

    all_providers = Counter()
    for r in results:
        for entry in r["atsFound"].split(", "):
            if entry:
                provider = entry.split("/")[0]
                all_providers[provider] += 1
    if all_providers:
        print("  ATS breakdown:")
        for provider, count in all_providers.most_common():
            tag = " ✓ free API" if provider in FREE_API_PROVIDERS else ""
            print(f"    {provider:<20} {count}{tag}")
        print()

    if robots_blocked:
        print(f"  Blocked companies (sample):")
        for r in robots_blocked[:10]:
            delay = f"  crawl-delay={r['robotsDelay']}" if r["robotsDelay"] else ""
            print(f"    {r['companyId']:<40}{delay}")
        if len(robots_blocked) > 10:
            print(f"    … and {len(robots_blocked) - 10} more (see CSV)")


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--concurrency", type=int, default=CONCURRENCY,
                        help=f"Simultaneous page fetches (default: {CONCURRENCY})")
    parser.add_argument("--dry-run", action="store_true",
                        help="Scan without writing to manifests")
    args = parser.parse_args()

    companies = load_target_companies()
    print(f"Loaded {len(companies)} companies with seeds but no ATS\n")

    if args.dry_run:
        print("  DRY RUN — manifests will not be modified\n")

    results = asyncio.run(scan_all(companies, args.concurrency, args.dry_run))

    print_summary(results)
    write_csv(results, Path(args.output))

    if args.dry_run:
        print("\nRe-run without --dry-run to write results to manifests.")


if __name__ == "__main__":
    main()
