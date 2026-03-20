#!/usr/bin/env python3
"""
Derive career listing page URLs (crawler seeds) from apply URLs stored in BigQuery.

For every company that lacks a supported ATS config AND has no live seeds, this
script queries all applyUrls we've ever seen for that company, then extracts the
*listing page* URL by stripping job-specific IDs — giving the AI crawler a valid
starting point.

Unlike extract_ats_from_apply_urls.py (which targets GH/LV/AS for ATS configs),
this script targets every other provider to produce seeds. If a supported ATS
provider (GH/LV/AS/SR/TT/WL) appears in an apply URL for a company that doesn't
have that ATS configured yet, it flags the company for the ATS config scripts
rather than adding a seed.

Usage:
    python3 scripts/ats/discover_seeds_from_apply_urls.py
    python3 scripts/ats/discover_seeds_from_apply_urls.py --apply
    python3 scripts/ats/discover_seeds_from_apply_urls.py --company-id canva

Output:
    scripts/ats/seed_candidates_from_apply_urls.csv

Then verify discovered seeds:
    python3 scripts/ats/verify_seeds.py

Requirements:
    pip install google-cloud-bigquery
"""

import argparse
import csv
import json
import re
import sys
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse, unquote

try:
    from google.cloud import bigquery
except ImportError:
    print("Error: google-cloud-bigquery not found. Run: pip install google-cloud-bigquery")
    sys.exit(1)

# ── Config ─────────────────────────────────────────────────────────────────────

PROJECT_ID   = "tech-market-insights"
DATASET_ID   = "techmarket"
JOBS_TABLE   = f"`{PROJECT_ID}.{DATASET_ID}.raw_jobs`"
MANIFEST_DIR = Path(__file__).parent.parent.parent / "data" / "companies"
DEFAULT_OUTPUT = Path(__file__).parent / "seed_candidates_from_apply_urls.csv"

SUPPORTED_ATS = {"GREENHOUSE", "LEVER", "ASHBY", "SMARTRECRUITERS", "TEAMTAILOR", "WORKABLE"}

# ── Listing page derivation rules ──────────────────────────────────────────────
#
# Each entry: provider_label → list of (regex, derive_fn)
#   regex:     matches the apply URL; should capture the part needed to build the listing URL
#   derive_fn: takes the re.Match and returns the listing page URL string
#
# Order matters — more specific patterns first.
# "ATS_CONFIG" entries flag the company for ATS config scripts, not seeds.
#

def _workday_listing(m: re.Match) -> str:
    """Strip everything from /job/ onward, leaving the board root."""
    base = m.group(1).rstrip("/")
    # Remove /job/... or /searchpage/... suffix if it crept in via group 1
    base = re.sub(r"/(?:job|searchpage)/.*$", "", base)
    return base

SEED_RULES: list[tuple[str, str, callable]] = [
    # ── Supported ATS (flag for ATS config, don't add seed) ─────────────────
    ("ATS_CONFIG:GREENHOUSE", r"boards\.greenhouse\.io/([^/?#\s\"']+)", None),
    ("ATS_CONFIG:GREENHOUSE", r"job-boards\.greenhouse\.io/([^/?#\s\"']+)", None),
    ("ATS_CONFIG:LEVER",      r"jobs\.lever\.co/([^/?#\s\"']+)",            None),
    ("ATS_CONFIG:ASHBY",      r"jobs\.ashbyhq\.com/([^/?#\s\"']+)",         None),
    ("ATS_CONFIG:WORKABLE",   r"apply\.workable\.com/([^/?#\s\"']+)",        None),
    ("ATS_CONFIG:SMARTRECRUITERS", r"careers\.smartrecruiters\.com/([^/?#\s\"']+)", None),
    ("ATS_CONFIG:SMARTRECRUITERS", r"jobs\.smartrecruiters\.com/([^/?#\s\"']+)", None),
    ("ATS_CONFIG:TEAMTAILOR", r"jobs\.teamtailor\.com/companies/([^/?#\s\"']+)", None),
    ("ATS_CONFIG:TEAMTAILOR", r"([a-zA-Z0-9-]+)\.teamtailor\.com",          None),

    # ── Workday ──────────────────────────────────────────────────────────────
    # Apply: https://tenant.wdN.myworkdayjobs.com/BoardPath/job/Location/Title_ID
    # Seed:  https://tenant.wdN.myworkdayjobs.com/BoardPath
    ("WORKDAY",
     r"(https?://[a-zA-Z0-9_-]+\.wd\d+\.myworkdayjobs\.com/[^/?#\s\"']+)",
     _workday_listing),

    # ── BambooHR ─────────────────────────────────────────────────────────────
    # Apply: https://subdomain.bamboohr.com/careers/123  or  /jobs/123
    # Seed:  https://subdomain.bamboohr.com/careers
    ("BAMBOOHR",
     r"https?://([a-zA-Z0-9_-]+)\.bamboohr\.com",
     lambda m: f"https://{m.group(1)}.bamboohr.com/careers"),

    # ── SAP SuccessFactors ───────────────────────────────────────────────────
    # Apply: various — typically https://tenant.successfactors.com/career?...
    # Seed:  https://tenant.successfactors.com/career
    ("SUCCESSFACTORS",
     r"https?://([a-zA-Z0-9_.-]+\.(?:successfactors\.com|hcm\.onesource\.sap\.com))",
     lambda m: f"https://{m.group(1)}/career"),

    # ── Recruitee ────────────────────────────────────────────────────────────
    # Apply: https://subdomain.recruitee.com/o/job-title-id
    # Seed:  https://subdomain.recruitee.com
    ("RECRUITEE",
     r"https?://([a-zA-Z0-9_-]+)\.recruitee\.com",
     lambda m: f"https://{m.group(1)}.recruitee.com"),

    # ── Breezy HR ────────────────────────────────────────────────────────────
    # Apply: https://subdomain.breezy.hr/p/job-slug-HASH
    # Seed:  https://subdomain.breezy.hr
    ("BREEZY",
     r"https?://([a-zA-Z0-9_-]+)\.breezy\.hr",
     lambda m: f"https://{m.group(1)}.breezy.hr"),

    # ── Join.com ─────────────────────────────────────────────────────────────
    # Apply: https://join.com/companies/slug/1234-job-title
    # Seed:  https://join.com/companies/slug
    ("JOIN",
     r"https?://join\.com/companies/([a-zA-Z0-9_-]+)",
     lambda m: f"https://join.com/companies/{m.group(1)}"),

    # ── Employment Hero ──────────────────────────────────────────────────────
    # Apply: https://company.employmenthero.com/jobs/12345 (company-specific subdomain)
    # Seed:  https://company.employmenthero.com/jobs
    # NOTE: jobs.employmenthero.com is the *generic* shared platform — skip it.
    #       Only emit a seed when the subdomain is company-specific.
    ("EMPLOYMENT_HERO",
     r"https?://(?!jobs\.)([a-zA-Z0-9_-]+)\.employmenthero\.com",
     lambda m: f"https://{m.group(1)}.employmenthero.com/jobs"),

    # ── PageUp ───────────────────────────────────────────────────────────────
    # Apply: https://company.pageuppeople.com/...
    # Seed:  https://company.pageuppeople.com
    ("PAGEUP",
     r"https?://([a-zA-Z0-9_-]+)\.pageuppeople\.com",
     lambda m: f"https://{m.group(1)}.pageuppeople.com"),

    # ── Oracle Taleo ─────────────────────────────────────────────────────────
    # Apply: https://company.taleo.net/careersection/N/jobdetail.ftl?job=ID
    # Seed:  https://company.taleo.net/careersection/N/jobsearch.ftl
    ("TALEO",
     r"https?://([a-zA-Z0-9_-]+)\.taleo\.net/careersection/([^/]+)/",
     lambda m: f"https://{m.group(1)}.taleo.net/careersection/{m.group(2)}/jobsearch.ftl"),

    # ── iCIMS ────────────────────────────────────────────────────────────────
    # Apply: https://careers.icims.com/jobs/NNNNN/...
    # Seed:  https://careers.icims.com/jobs/search
    ("ICIMS",
     r"(https?://[a-zA-Z0-9_.-]+\.icims\.com)",
     lambda m: f"{m.group(1)}/jobs/search"),

    # ── Snaphire ─────────────────────────────────────────────────────────────
    ("SNAPHIRE",
     r"https?://([a-zA-Z0-9_-]+)\.snaphire\.com",
     lambda m: f"https://{m.group(1)}.snaphire.com"),

    # ── Personio ─────────────────────────────────────────────────────────────
    # Apply: https://company.jobs.personio.com/job/ID or https://company.personio.de/...
    # Seed:  https://company.jobs.personio.com
    ("PERSONIO",
     r"https?://([a-zA-Z0-9_-]+)\.(?:jobs\.personio\.com|personio\.de)",
     lambda m: f"https://{m.group(1)}.jobs.personio.com"),

    # ── JobAdder ─────────────────────────────────────────────────────────────
    # NOTE: apply.jobadder.com/{code} URLs are application *forms*, not listing
    # pages. There is no reliable public listing URL derivable from apply URLs.
    # JobAdder is intentionally excluded — use path probing or Gemini instead.

    # ── Elmo Talent (elmotalent.com.au) ──────────────────────────────────────
    ("ELMO",
     r"https?://([a-zA-Z0-9_-]+)\.elmotalent\.com\.au",
     lambda m: f"https://{m.group(1)}.elmotalent.com.au"),

    # ── Comeet ───────────────────────────────────────────────────────────────
    ("COMEET",
     r"https?://www\.comeet\.com/jobs/([a-zA-Z0-9_-]+)",
     lambda m: f"https://www.comeet.com/jobs/{m.group(1)}"),

    # ── Factorial ────────────────────────────────────────────────────────────
    # Apply: https://company.factorialhr.com/job_posting/123-title
    # Seed:  https://company.factorialhr.com  (root shows all open roles)
    ("FACTORIAL",
     r"https?://([a-zA-Z0-9_-]+)\.factorialhr\.com",
     lambda m: f"https://{m.group(1)}.factorialhr.com"),

    # ── Zoho Recruit ─────────────────────────────────────────────────────────
    ("ZOHO",
     r"https?://([a-zA-Z0-9_.-]+)\.zohorecruit\.com",
     lambda m: f"https://{m.group(1)}.zohorecruit.com/jobs/Careers"),
]


# ── Manifest helpers ───────────────────────────────────────────────────────────

def load_target_companies() -> dict[str, dict]:
    """
    Return manifest data for companies that:
      - Don't have a supported ATS config, AND
      - Have no live (non-dead) seeds
    """
    targets: dict[str, dict] = {}
    for path in MANIFEST_DIR.rglob("*.json"):
        if path.name == "schema.json":
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue

        ats = data.get("ats") or {}
        provider = ats.get("provider", "")
        if provider in SUPPORTED_ATS:
            continue

        seeds = data.get("crawler", {}).get("seeds", [])
        live_seeds = [s for s in seeds if s.get("status") != "dead"]
        if live_seeds:
            continue

        targets[data["id"]] = {"path": path, "name": data.get("name", ""), "data": data}

    return targets


# ── BigQuery ───────────────────────────────────────────────────────────────────

def fetch_apply_urls(company_ids: list[str]) -> dict[str, list[str]]:
    """Return {companyId: [url, ...]} with all distinct apply URLs, ordered by frequency."""
    if not company_ids:
        return {}

    print(f"  Querying BigQuery for {len(company_ids)} companies...")
    client = bigquery.Client()

    # BigQuery can't handle >5000 items in UNNEST inline literal — batch if needed
    results: dict[str, list[str]] = {}
    BATCH = 500
    for start in range(0, len(company_ids), BATCH):
        batch = company_ids[start : start + BATCH]
        id_list = ", ".join(f'"{cid}"' for cid in batch)
        query = f"""
            SELECT
                companyId,
                url,
                COUNT(*) AS n
            FROM {JOBS_TABLE},
                UNNEST(applyUrls) AS url
            WHERE companyId IN ({id_list})
              AND url IS NOT NULL AND url != ''
            GROUP BY companyId, url
            ORDER BY companyId, n DESC
        """
        for row in client.query(query).result():
            results.setdefault(row.companyId, []).append(row.url)

    print(f"  → apply URLs found for {len(results)} companies")
    return results


# ── Derivation ─────────────────────────────────────────────────────────────────

def derive_seed(url: str) -> Optional[tuple[str, str]]:
    """
    Returns (provider_label, listing_page_url) or None.
    provider_label is 'ATS_CONFIG:PROVIDER' for supported providers.
    """
    for label, pattern, derive_fn in SEED_RULES:
        m = re.search(pattern, url, re.IGNORECASE)
        if not m:
            continue
        if derive_fn is None:
            # ATS_CONFIG entry — return the slug as the URL placeholder
            slug = m.group(1).split("/")[0].rstrip("/")
            return label, slug
        listing_url = derive_fn(m)
        if listing_url:
            return label, listing_url
    return None


def process(company_ids: list[str], apply_urls_map: dict[str, list[str]], companies: dict[str, dict]) -> list[dict]:
    candidates: list[dict] = []
    seen: set[tuple] = set()  # (company_id, url)

    for cid in company_ids:
        urls = apply_urls_map.get(cid, [])
        if not urls:
            continue
        name = companies[cid]["name"]
        for url in urls:
            result = derive_seed(url)
            if not result:
                continue
            label, derived = result
            key = (cid, derived.lower())
            if key in seen:
                continue
            seen.add(key)
            candidates.append({
                "companyId":    cid,
                "companyName":  name,
                "provider":     label,
                "seedUrl":      derived,
                "sampleApplyUrl": url,
                "_path":        companies[cid]["path"],
                "_data":        companies[cid]["data"],
            })

    return candidates


# ── Output ─────────────────────────────────────────────────────────────────────

def print_summary(candidates: list[dict]) -> None:
    seeds      = [c for c in candidates if not c["provider"].startswith("ATS_CONFIG")]
    ats_flags  = [c for c in candidates if c["provider"].startswith("ATS_CONFIG")]

    from collections import Counter
    print(f"\n{'═'*62}")
    print(f"  Seed candidates from apply URLs")
    print(f"{'═'*62}")
    print(f"  New seeds:             {len(seeds)}")
    print(f"  ATS config candidates: {len(ats_flags)}")
    print(f"    (run extract_ats_from_apply_urls.py to process these)")

    if seeds:
        print(f"\n  Seeds by provider:")
        for provider, count in Counter(c["provider"] for c in seeds).most_common():
            print(f"    {provider:<22} {count}")

    if ats_flags:
        print(f"\n  ATS config flags by provider:")
        for provider, count in Counter(c["provider"] for c in ats_flags).most_common():
            print(f"    {provider:<22} {count}")


def write_csv(candidates: list[dict], path: Path) -> None:
    fieldnames = ["companyId", "companyName", "provider", "seedUrl", "sampleApplyUrl"]
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        for c in candidates:
            w.writerow({k: c[k] for k in fieldnames})
    print(f"\n  Wrote {len(candidates)} rows → {path}")


def apply_to_manifests(candidates: list[dict], dry_run: bool = False) -> None:
    seeds = [c for c in candidates if not c["provider"].startswith("ATS_CONFIG")]
    print(f"\n{'─'*62}")
    label = "DRY RUN — would write" if dry_run else "Writing"
    print(f"  {label} {len(seeds)} seeds to manifests...")

    written = 0
    for c in seeds:
        path: Path = c["_path"]
        data: dict = c["_data"]

        # Re-read from disk in case a previous iteration already wrote to it
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception as e:
            print(f"  ERROR reading {path}: {e}")
            continue

        crawler = data.setdefault("crawler", {})
        existing_seeds: list[dict] = crawler.setdefault("seeds", [])

        # Skip if this URL is already present (any status)
        if any(s.get("url", "").lower() == c["seedUrl"].lower() for s in existing_seeds):
            continue

        new_seed = {
            "category": "careers",
            "source":   "apply-url-mining",
            "status":   "pending",
            "url":      c["seedUrl"],
        }
        existing_seeds.append(new_seed)

        if not dry_run:
            try:
                path.write_text(
                    json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
                    encoding="utf-8",
                )
            except Exception as e:
                print(f"  ERROR writing {path}: {e}")
                continue

        verb = "WOULD ADD" if dry_run else "ADDED"
        print(f"  {verb} [{c['provider']}] {c['companyId']}: {c['seedUrl']}")
        written += 1

    print(f"\n  {'Would add' if dry_run else 'Added'} {written} seeds.")
    if not dry_run and written:
        print("  Next: run python3 scripts/ats/verify_seeds.py to check each seed.")


# ── Main ───────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="Output CSV path")
    parser.add_argument("--apply", action="store_true", help="Write discovered seeds to manifest files")
    parser.add_argument("--dry-run", action="store_true", help="With --apply: preview without writing")
    parser.add_argument("--company-id", help="Run for a single company ID only")
    args = parser.parse_args()

    print("Loading manifests...")
    companies = load_target_companies()
    print(f"  → {len(companies)} companies need seeds\n")

    if args.company_id:
        if args.company_id not in companies:
            print(f"Company '{args.company_id}' not found or already has live seeds / supported ATS.")
            sys.exit(1)
        target_ids = [args.company_id]
    else:
        target_ids = list(companies.keys())

    apply_urls_map = fetch_apply_urls(target_ids)
    candidates = process(target_ids, apply_urls_map, companies)

    print_summary(candidates)
    write_csv(candidates, Path(args.output))

    if args.apply or args.dry_run:
        apply_to_manifests(candidates, dry_run=args.dry_run)
    else:
        print("\n  Review the CSV, then re-run with --apply to write seeds to manifests.")
        print("  Or use --dry-run first to preview.\n")


if __name__ == "__main__":
    main()
