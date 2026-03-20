#!/usr/bin/env python3
"""
Stage 6 (on-demand): Use Gemini with Google Search grounding to find career
page URLs for companies that remain unseeded after all prior pipeline stages.

For each qualifying company (no supported ATS, no live seeds), asks Gemini:
  "What is the career listings page URL for {name} ({website})?"

Gemini fetches live Google Search results to ground its answer. Returned URLs
are HTTP-verified before being written to manifests.

Target population:
  - Companies with no ats.provider or provider in {CUSTOM, WORKDAY, BAMBOOHR...}
  - AND no seeds, or all seeds are dead

Usage:
    # Preview companies that would be queried (no API calls)
    python3 scripts/ats/discover_seeds_gemini.py

    # Query Gemini and print results without writing to manifests
    python3 scripts/ats/discover_seeds_gemini.py --query

    # Query Gemini and write verified seeds to manifests
    python3 scripts/ats/discover_seeds_gemini.py --query --apply

    # Restrict to specific companies
    python3 scripts/ats/discover_seeds_gemini.py --query --apply --company-ids foo bar

    # Adjust concurrency and batch size
    python3 scripts/ats/discover_seeds_gemini.py --query --apply --concurrency 5 --limit 50

Requirements:
    pip install google-genai aiohttp
    gcloud auth application-default login   (Vertex AI credentials)

Note: Google Search grounding is only available in certain Vertex AI regions.
The script uses us-central1 by default (override with --location).
"""

import argparse
import asyncio
import json
import re
import sys
import time
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse

try:
    from google import genai
    from google.genai import types as genai_types
except ImportError:
    print("Error: google-genai not found. Run: pip install google-genai")
    sys.exit(1)

try:
    import aiohttp
except ImportError:
    print("Error: aiohttp not found. Run: pip install aiohttp")
    sys.exit(1)

# ── Config ──────────────────────────────────────────────────────────────────

MANIFEST_DIR = Path(__file__).parent.parent.parent / "data" / "companies"

GCP_PROJECT   = "tech-market-insights"
# Search grounding requires a region that supports it; us-central1 is safest.
GCP_LOCATION  = "us-central1"
GEMINI_MODEL  = "gemini-2.5-flash"

# ATS providers we integrate with directly — companies with these need no seed
INTEGRATED_ATS = {"GREENHOUSE", "LEVER", "ASHBY", "SMARTRECRUITERS", "TEAMTAILOR", "WORKABLE"}

# Seed statuses that count as "live" — company is already covered
LIVE_STATUSES = {"active", "empty", "blocked", "pending", "unknown"}

CONCURRENCY  = 3    # Gemini calls in flight at once (stay within quota)
VERIFY_TIMEOUT = 12  # seconds for HTTP seed verification
READ_BYTES   = 40960

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

# ── Company loading ──────────────────────────────────────────────────────────

def load_target_companies(
    only_ids: Optional[set] = None,
    limit: Optional[int] = None,
) -> list[dict]:
    """
    Returns companies with no integrated ATS and no live seeds.
    Each entry: {path, id, name, website, existing_seeds}
    """
    targets = []
    for f in sorted(MANIFEST_DIR.rglob("*.json")):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except Exception:
            continue
        if "name" not in data:
            continue

        cid     = data.get("id", f.stem)
        name    = data.get("name", "")
        website = data.get("website", "")

        if only_ids and cid not in only_ids:
            continue
        if not website:
            continue

        # Skip if already on an integrated ATS
        provider = data.get("ats", {}).get("provider", "")
        if provider in INTEGRATED_ATS:
            continue

        # Skip if any live seed exists
        seeds = data.get("crawler", {}).get("seeds", [])
        live  = [s for s in seeds if s.get("status", "") in LIVE_STATUSES]
        if live:
            continue

        targets.append({
            "path":    f,
            "id":      cid,
            "name":    name,
            "website": website,
            "seeds":   seeds,  # existing (all dead or none)
        })

        if limit and len(targets) >= limit:
            break

    return targets


# ── Gemini query ─────────────────────────────────────────────────────────────

def build_client(location: str = GCP_LOCATION) -> genai.Client:
    """
    Initialise the Gemini client.

    Priority:
      1. GOOGLE_API_KEY env var  → Gemini Developer API (free tier: 1500 req/day)
      2. Otherwise              → Vertex AI with Application Default Credentials
                                  (production; same creds as BQ/Cloud Tasks)

    To use the free tier during development:
        export GOOGLE_API_KEY=<key from https://aistudio.google.com/apikey>
        python3 scripts/ats/discover_seeds_gemini.py --query --apply
    """
    import os
    api_key = os.environ.get("GOOGLE_API_KEY")
    if api_key:
        return genai.Client(api_key=api_key)
    return genai.Client(
        vertexai=True,
        project=GCP_PROJECT,
        location=location,
    )


def _search_tool() -> genai_types.Tool:
    """
    Returns the Google Search grounding tool.

    google_search_retrieval (the old forced-search API with dynamic_threshold=0.0)
    is no longer supported by either the Developer API or Vertex AI — both return
    400 INVALID_ARGUMENT. We use google_search for both auth modes.

    Note: google_search uses dynamic retrieval — the model may answer from training
    data without searching, especially for well-known companies. The HTTP
    verification step downstream catches and discards stale/hallucinated URLs.
    """
    return genai_types.Tool(google_search=genai_types.GoogleSearch())


def query_gemini(client: genai.Client, name: str, website: str) -> tuple[Optional[str], bool]:
    """
    Ask Gemini (with Google Search grounding) for the career listings page URL.

    Returns (url_or_None, search_was_used).

    Key design choices:
    - dynamic_threshold=0.0 forces a live web search on every call (default dynamic
      mode skips search when the model is confident from training data, which causes
      it to return stale/hallucinated URLs for well-known companies).
    - Prompt asks for the *job listing* page specifically, not the marketing careers
      landing page, and requests the underlying ATS URL where applicable.
    - Returns the source URL from grounding chunks when available (more reliable
      than the model's text output).
    """
    prompt = (
        f'Search the web now for the current job listings page for "{name}".\n'
        f'Their website is {website}.\n'
        f"\n"
        f"I need the URL of the page that lists ALL open job positions — the actual "
        f"job search/listing page, not the marketing careers landing page.\n"
        f"\n"
        f"Many companies host their jobs on a third-party ATS or job board platform. "
        f"If you find one of these, return the platform URL directly rather than the "
        f"company's own careers page:\n"
        f"  • Workday:        https://company.wd3.myworkdayjobs.com/Careers\n"
        f"  • Greenhouse:     https://boards.greenhouse.io/company  OR  https://company.greenhouse.io/jobs\n"
        f"  • Lever:          https://jobs.lever.co/company\n"
        f"  • Ashby:          https://jobs.ashbyhq.com/company\n"
        f"  • SmartRecruiters:https://careers.smartrecruiters.com/Company\n"
        f"  • TeamTailor:     https://company.teamtailor.com/jobs\n"
        f"  • BambooHR:       https://company.bamboohr.com/careers\n"
        f"  • Personio:       https://company.jobs.personio.com\n"
        f"  • Deel job boards:https://jobs.deel.com/job-boards/company\n"
        f"  • Rippling:       https://ats.rippling.com/company/postings\n"
        f"  • Pinpoint:       https://company.pinpointhq.com/jobs\n"
        f"  • Recruitee:      https://company.recruitee.com\n"
        f"  • Jobvite:        https://jobs.jobvite.com/company\n"
        f"  • iCIMS:          https://careers-company.icims.com/jobs\n"
        f"  • Breezy:         https://company.breezy.hr\n"
        f"\n"
        f"Return ONLY the URL on a single line. "
        f"If you cannot find a current job listings page, return UNKNOWN."
    )

    try:
        response = client.models.generate_content(
            model=GEMINI_MODEL,
            contents=prompt,
            config=genai_types.GenerateContentConfig(
                tools=[_search_tool()],
                temperature=0,
            ),
        )
    except Exception as e:
        print(f"  ERROR (Gemini) {name}: {e}", file=sys.stderr)
        return None, False

    # Check whether search grounding was actually used
    search_used = False
    grounding_url = None
    try:
        gm = response.candidates[0].grounding_metadata if response.candidates else None
        if gm and gm.grounding_chunks:
            search_used = True
            # Prefer the first grounding chunk URL as the source of truth —
            # it's the actual web page Gemini found, more reliable than model text
            for chunk in gm.grounding_chunks:
                if chunk.web and chunk.web.uri:
                    grounding_url = chunk.web.uri
                    break
    except Exception:
        pass

    text = (response.text or "").strip()

    if not text or "UNKNOWN" in text.upper():
        return None, search_used

    # Extract URL: prefer model's text output (it's been processed/cleaned),
    # but fall back to first grounding chunk if text has no URL
    urls = re.findall(r"https?://[^\s\"'<>\])\}\|]+", text)
    url  = urls[0].rstrip(".,;)") if urls else None

    if not url and grounding_url:
        # Strip Vertex AI redirect wrapper to get the real URL
        real = re.search(r"https?://(?!vertexaisearch)[^\s\"'<>]+", grounding_url)
        url  = real.group(0).rstrip(".,;)") if real else None

    if not url:
        return None, search_used

    parsed = urlparse(url)
    if not parsed.netloc:
        return None, search_used

    return url, search_used


# ── HTTP verification ─────────────────────────────────────────────────────────

async def verify_url(
    session: aiohttp.ClientSession,
    sem: asyncio.Semaphore,
    url: str,
) -> str:
    """Returns seed status: active, empty, dead, blocked, or unknown."""
    async with sem:
        try:
            async with session.get(
                url,
                headers=HEADERS,
                timeout=aiohttp.ClientTimeout(total=VERIFY_TIMEOUT),
                allow_redirects=True,
                ssl=False,
            ) as resp:
                final_url  = str(resp.url)
                status     = resp.status

                if status in (404, 410, 400):
                    return "dead"
                if status == 403:
                    return "blocked"

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


# ── Manifest write ────────────────────────────────────────────────────────────

def write_seed(path: Path, url: str, status: str) -> None:
    try:
        data  = json.loads(path.read_text(encoding="utf-8"))
        seeds = data.setdefault("crawler", {}).setdefault("seeds", [])

        # Don't duplicate an existing URL
        existing_urls = {s.get("url") for s in seeds}
        if url in existing_urls:
            return

        seeds.append({
            "category": "careers",
            "source":   "gemini-search",
            "status":   status,
            "url":      url,
        })
        path.write_text(
            json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
            encoding="utf-8",
        )
    except Exception as e:
        print(f"  WRITE ERROR {path.stem}: {e}", file=sys.stderr)


# ── Main logic ────────────────────────────────────────────────────────────────

async def process_company(
    client: genai.Client,
    company: dict,
    http_sem: asyncio.Semaphore,
    session: aiohttp.ClientSession,
    gemini_sem: asyncio.Semaphore,
    apply: bool,
    idx: int,
    total: int,
) -> dict:
    """Query Gemini, verify URL, optionally write to manifest. Returns result dict."""
    name    = company["name"]
    website = company["website"]
    cid     = company["id"]

    async with gemini_sem:
        # Run the synchronous Gemini call in a thread pool with a hard timeout.
        # Without a timeout, a hung API call blocks the thread indefinitely.
        loop = asyncio.get_event_loop()
        try:
            url, search_used = await asyncio.wait_for(
                loop.run_in_executor(None, query_gemini, client, name, website),
                timeout=60,
            )
        except asyncio.TimeoutError:
            print(f"  [{idx:4}/{total}] ⏱  TIMEOUT    {cid:<35}  (Gemini call exceeded 60s)", flush=True)
            return {"company": cid, "url": None, "status": "timeout", "searched": False}

    search_icon = "🔍" if search_used else "🧠"  # 🔍 = searched web, 🧠 = training data only

    if not url:
        print(f"  [{idx:4}/{total}] {search_icon} ❓  NO_RESULT  {cid:<35}  (Gemini returned nothing)", flush=True)
        return {"company": cid, "url": None, "status": "no_result", "searched": search_used}

    # Verify the URL via HTTP
    status = await verify_url(session, http_sem, url)

    icon = {"active": "✅", "empty": "⬜", "dead": "❌", "blocked": "🔒", "unknown": "❓"}.get(status, "?")
    apply_marker = " → written" if apply and status != "dead" else ""
    print(f"  [{idx:4}/{total}] {search_icon} {icon} {status:<8}  {cid:<35}  {url[:55]}{apply_marker}", flush=True)

    if apply and status != "dead":
        write_seed(company["path"], url, status)

    return {"company": cid, "url": url, "status": status, "searched": search_used}


async def run(
    companies: list[dict],
    apply: bool,
    gemini_concurrency: int,
    location: str = GCP_LOCATION,
) -> list[dict]:
    client     = build_client(location)
    gemini_sem = asyncio.Semaphore(gemini_concurrency)
    http_sem   = asyncio.Semaphore(gemini_concurrency * 3)

    connector = aiohttp.TCPConnector(ssl=False, limit=gemini_concurrency * 6)
    results   = []

    async with aiohttp.ClientSession(connector=connector) as session:
        tasks = [
            process_company(
                client, company, http_sem, session, gemini_sem, apply, i, len(companies)
            )
            for i, company in enumerate(companies, 1)
        ]
        for coro in asyncio.as_completed(tasks):
            result = await coro
            results.append(result)

    return results


# ── CLI ───────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--query", action="store_true",
        help="Actually call Gemini (without this flag, just lists target companies)",
    )
    parser.add_argument(
        "--apply", action="store_true",
        help="Write verified seeds to manifests (requires --query)",
    )
    parser.add_argument(
        "--company-ids", nargs="+", metavar="ID",
        help="Restrict to these company IDs",
    )
    parser.add_argument(
        "--limit", type=int, default=None, metavar="N",
        help="Max number of companies to process",
    )
    parser.add_argument(
        "--concurrency", type=int, default=CONCURRENCY,
        help=f"Gemini calls in flight at once (default: {CONCURRENCY})",
    )
    parser.add_argument(
        "--location", default=GCP_LOCATION,
        help=f"Vertex AI region (default: {GCP_LOCATION})",
    )
    args = parser.parse_args()

    if args.apply and not args.query:
        parser.error("--apply requires --query")

    only_ids = set(args.company_ids) if args.company_ids else None

    print("Loading target companies...")
    companies = load_target_companies(only_ids=only_ids, limit=args.limit)
    print(f"Found {len(companies)} companies with no ATS and no live seeds.\n")

    if not companies:
        print("Nothing to do.")
        sys.exit(0)

    if not args.query:
        # Preview mode — just list the companies
        print(f"{'ID':<40} {'Name':<35} Website")
        print("─" * 100)
        for c in companies:
            dead_seeds = len(c["seeds"])
            note = f"  ({dead_seeds} dead seeds)" if dead_seeds else ""
            print(f"  {c['id']:<38} {c['name']:<35} {c['website']}{note}")
        print(f"\nRun with --query to call Gemini for these {len(companies)} companies.")
        sys.exit(0)

    location = args.location

    import os
    backend = "Gemini Developer API (free tier)" if os.environ.get("GOOGLE_API_KEY") else f"Vertex AI ({location})"
    print(f"Querying Gemini ({GEMINI_MODEL}) via {backend}...")
    print(f"Concurrency: {args.concurrency} | Apply: {args.apply}\n")

    t0      = time.time()
    results = asyncio.run(run(companies, args.apply, args.concurrency, location))
    elapsed = time.time() - t0

    # Summary
    from collections import Counter
    status_counts  = Counter(r["status"] for r in results)
    searched_count = sum(1 for r in results if r.get("searched"))
    trained_count  = len(results) - searched_count

    print(f"\n{'═' * 62}")
    print(f"  Gemini seed discovery results")
    print(f"{'═' * 62}")
    for status, n in status_counts.most_common():
        icon = {"active": "✅", "empty": "⬜", "dead": "❌",
                "blocked": "🔒", "unknown": "❓", "no_result": "–"}.get(status, "?")
        print(f"  {icon} {status:<12} {n:>4}")
    print(f"{'─' * 62}")
    written = sum(1 for r in results if r["url"] and r["status"] != "dead" and args.apply)
    print(f"  Total queried:  {len(results):>4}")
    print(f"  URLs found:     {sum(1 for r in results if r['url']):>4}")
    print(f"  Written:        {written:>4}")
    print(f"  Elapsed:        {elapsed:.0f}s")
    print(f"{'─' * 62}")
    print(f"  🔍 Web searched: {searched_count:>4}  (grounding chunks present)")
    print(f"  🧠 Training only:{trained_count:>4}  (no web search performed — higher hallucination risk)")
    if args.apply and written:
        print(f"\nNext: python3 scripts/ats/verify_seeds.py --statuses pending")


if __name__ == "__main__":
    main()
