#!/usr/bin/env python3
"""
Stage 5: HTTP-check crawler seeds and write a status back to each manifest.

For every seed that matches the --statuses filter, fetches the URL, scans the
first 40 KB of HTML for job-indicator keywords, and writes one of:

  active   — ≥3 job-indicator hits (ready to crawl)
  empty    — page loads, no jobs visible (SPA or quiet period)
  dead     — 404 / DNS failure / redirect to unrelated domain
  blocked  — 403 or very thin HTML (anti-bot / JS-only SPA)
  unknown  — timeout or other transient error

By default, re-checks seeds with status in: none, unknown, empty, blocked.
Skips 'active' (already confirmed good) and 'dead' (already confirmed gone).

Usage:
    python3 scripts/ats/verify_seeds.py                      # all non-active/dead
    python3 scripts/ats/verify_seeds.py --statuses none unknown
    python3 scripts/ats/verify_seeds.py --company-id canva
    python3 scripts/ats/verify_seeds.py --all                # including active+dead

Requirements:
    pip install aiohttp
"""

import argparse
import asyncio
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse

try:
    import aiohttp
except ImportError:
    print("Error: aiohttp not found. Run: pip install aiohttp")
    sys.exit(1)

# ── Config ─────────────────────────────────────────────────────────────────────

MANIFEST_DIR = Path(__file__).parent.parent.parent / "data" / "companies"
CONCURRENCY  = 12
DELAY        = 0.1    # seconds between requests per slot
TIMEOUT      = 12     # seconds per request
READ_BYTES   = 40960  # 40 KB

DEFAULT_STATUSES = {"none", "unknown", "empty", "blocked"}

# Statuses that are manually set and should never be re-verified
TERMINAL_STATUSES = {"linkedin", "excluded"}

HEADERS = {
    "User-Agent": "Mozilla/5.0 (compatible; TechMarket-SeedVerify/1.0)",
    "Accept":     "text/html,application/xhtml+xml",
}

JOB_INDICATORS = re.compile(
    r"job[s\-_ ]|position[s\-_]|vacanc|apply[\s\-]now|we.?re hiring|"
    r"JobPosting|\"title\"\s*:|open role|current opening|career opportunit|"
    r"join our team|work with us|browse jobs|view jobs|see all jobs",
    re.IGNORECASE,
)

# ── Seed loading ───────────────────────────────────────────────────────────────

def load_seeds(
    target_statuses: set[str],
    only_company: Optional[str] = None,
) -> list[dict]:
    """
    Returns a flat list of seed records to check:
      {path, company_id, company_name, seed_index, url, current_status, source}
    """
    seeds = []
    for f in sorted(MANIFEST_DIR.rglob("*.json")):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except Exception:
            continue
        if "name" not in data:
            continue
        cid = data.get("id", f.stem)
        if only_company and cid != only_company:
            continue

        for i, s in enumerate(data.get("crawler", {}).get("seeds", [])):
            status = s.get("status", "none")
            if status not in target_statuses:
                continue
            url = s.get("url", "")
            if not url:
                continue
            seeds.append({
                "path":           f,
                "company_id":     cid,
                "company_name":   data.get("name", ""),
                "seed_index":     i,
                "url":            url,
                "current_status": status,
                "source":         s.get("source", ""),
            })
    return seeds


# ── HTTP + classification ──────────────────────────────────────────────────────

async def fetch_and_classify(
    session: aiohttp.ClientSession,
    sem: asyncio.Semaphore,
    url: str,
) -> str:
    async with sem:
        await asyncio.sleep(DELAY)
        try:
            async with session.get(
                url,
                headers=HEADERS,
                timeout=aiohttp.ClientTimeout(total=TIMEOUT),
                allow_redirects=True,
                ssl=False,
            ) as resp:
                final_url = str(resp.url)
                status    = resp.status

                if status in (404, 410, 400):
                    return "dead"
                if status == 403:
                    return "blocked"

                # Off-domain redirect → dead
                orig_host  = urlparse(url).netloc.lower()
                final_host = urlparse(final_url).netloc.lower()
                if (orig_host and final_host
                        and orig_host not in final_host
                        and final_host not in orig_host):
                    return "dead"

                raw  = await resp.content.read(READ_BYTES)
                html = raw.decode("utf-8", errors="replace")

                if len(html.strip()) < 300:
                    return "blocked"

                hits = len(JOB_INDICATORS.findall(html))
                return "active" if hits >= 3 else "empty"

        except (aiohttp.ClientConnectorError, aiohttp.ClientResponseError):
            return "dead"
        except asyncio.TimeoutError:
            return "unknown"
        except Exception:
            return "unknown"


# ── Write back ─────────────────────────────────────────────────────────────────

def write_status(seed: dict, new_status: str) -> None:
    path: Path = seed["path"]
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        seeds_list = data.get("crawler", {}).get("seeds", [])
        idx = seed["seed_index"]
        if idx < len(seeds_list):
            seeds_list[idx]["status"] = new_status
            path.write_text(
                json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
                encoding="utf-8",
            )
    except Exception as e:
        print(f"  WRITE ERROR {seed['company_id']}: {e}", file=sys.stderr)


# ── Main ───────────────────────────────────────────────────────────────────────

async def run(seeds: list[dict], concurrency: int) -> Counter:
    sem       = asyncio.Semaphore(concurrency)
    connector = aiohttp.TCPConnector(ssl=False, limit=concurrency * 2)

    results = Counter()
    changed = 0

    async with aiohttp.ClientSession(connector=connector) as session:
        tasks = [
            (seed, fetch_and_classify(session, sem, seed["url"]))
            for seed in seeds
        ]

        total = len(tasks)
        for i, (seed, coro) in enumerate(tasks, 1):
            new_status = await coro
            results[new_status] += 1

            old = seed["current_status"]
            icon = {"active": "✅", "empty": "⬜", "dead": "❌",
                    "blocked": "🔒", "unknown": "❓"}.get(new_status, "?")
            changed_marker = " ←" if new_status != old else ""
            print(
                f"  [{i:4}/{total}] {icon} {new_status:<8}"
                f"  {seed['company_id']:<35}"
                f"  {seed['url'][:60]}"
                f"{changed_marker}"
            )

            write_status(seed, new_status)
            if new_status != old:
                changed += 1

    return results, changed


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--statuses", nargs="+",
        default=sorted(DEFAULT_STATUSES),
        metavar="S",
        help=f"Seed statuses to re-check (default: {sorted(DEFAULT_STATUSES)})",
    )
    parser.add_argument(
        "--all", action="store_true",
        help="Re-check every seed regardless of status",
    )
    parser.add_argument(
        "--company-id",
        metavar="ID",
        help="Restrict to seeds for a single company",
    )
    parser.add_argument(
        "--concurrency", type=int, default=CONCURRENCY,
        help=f"Parallel requests (default: {CONCURRENCY})",
    )
    args = parser.parse_args()

    target_statuses = (
        {"active", "dead", "none", "unknown", "empty", "blocked", "pending"}
        if args.all
        else set(args.statuses)
    )
    # Never re-verify manually-set terminal statuses even with --all
    target_statuses -= TERMINAL_STATUSES

    print(f"Loading seeds with status in {sorted(target_statuses)}...")
    seeds = load_seeds(target_statuses, only_company=args.company_id)

    if not seeds:
        print("No seeds to verify.")
        sys.exit(0)

    print(f"Verifying {len(seeds)} seeds (concurrency={args.concurrency})...\n")
    results, changed = asyncio.run(run(seeds, args.concurrency))

    print(f"\n{'═'*62}")
    print(f"  Seed verification results")
    print(f"{'═'*62}")
    for status, n in results.most_common():
        icon = {"active": "✅", "empty": "⬜", "dead": "❌",
                "blocked": "🔒", "unknown": "❓"}.get(status, "?")
        print(f"  {icon} {status:<10} {n:>4}")
    print(f"{'─'*62}")
    print(f"  Total checked:  {len(seeds):>4}")
    print(f"  Status changed: {changed:>4}")


if __name__ == "__main__":
    main()
