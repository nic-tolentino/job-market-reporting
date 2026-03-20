#!/usr/bin/env python3
"""
Tier 1 data quality report: field coverage matrix per source type.

Usage:
    python3 scripts/quality/tier1_coverage_report.py <file.json> [<file2.json> ...]
    python3 scripts/quality/tier1_coverage_report.py crawl-results/*.json
    python3 scripts/quality/tier1_coverage_report.py output.json --source bamboohr

Input: One or more JSON files. Each file is either:
  - A JSON array of CrawlResponse objects (batch crawl output)
  - A single CrawlResponse object
  - A JSON array of NormalizedJob objects

Output: Field coverage table, extraction stats table, and a validation summary.
"""

import json
import sys
import argparse
import re
from collections import defaultdict
from typing import Any

FIELDS = [
    "title",
    "location",
    "employmentType",
    "workModel",
    "seniorityLevel",
    "department",
    "applyUrl",
    "postedAt",
    "descriptionText",
    "salaryMin",
]

FIELD_LABELS = {
    "title":          "title",
    "location":       "location",
    "employmentType": "empType",
    "workModel":      "workModel",
    "seniorityLevel": "seniority",
    "department":     "dept",
    "applyUrl":       "applyUrl",
    "postedAt":       "postedAt",
    "descriptionText": "desc",
    "salaryMin":      "salary",
}


def detect_source_type(company_id: str, seed_url: str | None, ats_provider: str | None) -> str:
    """Classify a crawl result into a source type bucket."""
    if ats_provider and ats_provider not in ("", "null", "None"):
        return ats_provider.upper()
    if not seed_url:
        return "unknown"
    url = seed_url.lower()
    if "bamboohr.com" in url:
        return "BambooHR"
    if "myworkdayjobs.com" in url or "workday.com" in url:
        return "Workday"
    if "greenhouse.io" in url:
        return "Greenhouse"
    if "lever.co" in url:
        return "Lever"
    if "ashbyhq.com" in url:
        return "Ashby"
    if "smartrecruiters.com" in url:
        return "SmartRecruiters"
    if "teamtailor.com" in url:
        return "TeamTailor"
    if "recruitee.com" in url:
        return "Recruitee"
    if "personio.com" in url or "jobs.personio" in url:
        return "Personio"
    if "breezy.hr" in url:
        return "Breezy"
    if "join.com" in url:
        return "Join.com"
    if "deel.com" in url:
        return "Deel"
    if "ats.rippling.com" in url:
        return "Rippling"
    if "pinpointhq.com" in url:
        return "Pinpoint"
    if "successfactors" in url or "sapsf.com" in url:
        return "SuccessFactors"
    if "icims.com" in url:
        return "iCIMS"
    if "taleo.net" in url:
        return "Taleo"
    if "pageuppeople.com" in url:
        return "PageUp"
    if "jobadder.com" in url:
        return "JobAdder"
    if "employmenthero.com" in url:
        return "EmploymentHero"
    return "CustomPage"


def load_jobs_from_file(path: str) -> list[dict]:
    """Load raw job objects from a JSON file, handling CrawlResponse or plain arrays."""
    with open(path) as f:
        data = json.load(f)

    if isinstance(data, dict):
        # Single CrawlResponse
        return _jobs_from_response(data)

    if isinstance(data, list) and len(data) > 0:
        first = data[0]
        if isinstance(first, dict) and "jobs" in first and "crawlMeta" in first:
            # List of CrawlResponse objects
            all_jobs = []
            for response in data:
                all_jobs.extend(_jobs_from_response(response))
            return all_jobs
        else:
            # Assume list of NormalizedJob objects
            return [_tag_job(j, None, None) for j in data]

    return []


def _jobs_from_response(response: dict) -> list[dict]:
    """Extract jobs from a CrawlResponse, tagging with source metadata."""
    meta = response.get("crawlMeta", {})
    ats_provider = meta.get("detectedAtsProvider")
    # Try to infer seed URL from listingPageUrls
    listing_urls = meta.get("listingPageUrls", [])
    seed_url = listing_urls[0] if listing_urls else None
    company_id = response.get("companyId", "")
    jobs = response.get("jobs", [])
    return [_tag_job(j, company_id, seed_url, ats_provider) for j in jobs]


def _tag_job(job: dict, company_id: str | None, seed_url: str | None, ats_provider: str | None = None) -> dict:
    """Attach _source_type metadata to a job dict."""
    source_type = detect_source_type(company_id or "", seed_url, ats_provider)
    return {**job, "_source_type": source_type, "_company_id": company_id or ""}


def load_extraction_stats(paths: list[str]) -> list[dict]:
    """Load extractionStats per company from CrawlResponse files."""
    rows = []
    for path in paths:
        with open(path) as f:
            data = json.load(f)
        if isinstance(data, dict):
            responses = [data]
        elif isinstance(data, list):
            responses = data if data and "crawlMeta" in data[0] else []
        else:
            responses = []

        for r in responses:
            stats = r.get("extractionStats")
            if not stats:
                continue
            meta = r.get("crawlMeta", {})
            rows.append({
                "companyId": r.get("companyId", ""),
                "jobsRaw": stats.get("jobsRaw", 0),
                "jobsValid": stats.get("jobsValid", 0),
                "jobsTech": stats.get("jobsTech", 0),
                "detailPagesAttempted": stats.get("detailPagesAttempted", 0),
                "detailPagesEnriched": stats.get("detailPagesEnriched", 0),
                "descriptionCoverage": stats.get("descriptionCoverage", 0),
                "status": meta.get("status", ""),
            })
    return rows


def field_present(job: dict, field: str) -> bool:
    """Return True if the field is non-null and non-empty."""
    v = job.get(field)
    if v is None:
        return False
    if isinstance(v, str) and v.strip() == "":
        return False
    return True


def print_coverage_table(jobs: list[dict], filter_source: str | None):
    """Print per-source-type field coverage matrix."""
    # Group by source type
    by_source: dict[str, list[dict]] = defaultdict(list)
    for job in jobs:
        src = job.get("_source_type", "unknown")
        by_source[src].append(job)

    if filter_source:
        key = filter_source.lower()
        by_source = {k: v for k, v in by_source.items() if k.lower() == key}
        if not by_source:
            print(f"No jobs found for source type '{filter_source}'")
            return

    # Compute coverage per source per field
    results = {}
    for source, source_jobs in sorted(by_source.items()):
        n = len(source_jobs)
        results[source] = {
            "n": n,
            "fields": {
                f: int(100 * sum(field_present(j, f) for j in source_jobs) / n)
                for f in FIELDS
            }
        }

    # Print header
    col_w = 22
    field_w = 8
    header = f"{'Source type':<{col_w}} {'n':>5}  "
    header += "  ".join(f"{FIELD_LABELS[f]:>{field_w}}" for f in FIELDS)
    sep = "-" * len(header)
    print(sep)
    print(header)
    print(sep)

    for source, data in results.items():
        n = data["n"]
        row = f"{source:<{col_w}} {n:>5}  "
        parts = []
        for f in FIELDS:
            pct = data["fields"][f]
            # Colour-code: ≥80% plain, 40-79% tilde, <40% asterisk
            marker = " " if pct >= 80 else ("~" if pct >= 40 else "!")
            parts.append(f"{pct:>{field_w-1}}%{marker}")
        row += "  ".join(parts)
        print(row)

    print(sep)
    print("  ! = <40%  ~ = 40–79%  (blank) = ≥80%")
    print()

    # Per-source detail when there are issues
    for source, data in results.items():
        gaps = [f for f in FIELDS if data["fields"][f] < 40]
        if gaps:
            print(f"  {source}: low coverage fields — {', '.join(gaps)}")


def print_stats_table(stats: list[dict]):
    """Print extraction stats (raw vs valid vs tech) per company."""
    if not stats:
        return
    print()
    print("Extraction stats (raw → valid → tech):")
    print("-" * 80)
    problems = [s for s in stats if s["jobsRaw"] > 0 and s["jobsRaw"] != s["jobsValid"]]
    if not problems:
        print("  All companies: jobsRaw == jobsValid (no validation rejections)")
        return
    print(f"  {'Company':<40} {'raw':>5} {'valid':>6} {'tech':>5} {'desc%':>6}")
    for s in sorted(problems, key=lambda x: x["jobsRaw"] - x["jobsValid"], reverse=True):
        rejected = s["jobsRaw"] - s["jobsValid"]
        desc_pct = int(s["descriptionCoverage"] * 100)
        flag = "  !" if rejected > 5 else "   "
        print(f"{flag} {s['companyId']:<40} {s['jobsRaw']:>5} {s['jobsValid']:>6} {s['jobsTech']:>5} {desc_pct:>5}%")


def main():
    parser = argparse.ArgumentParser(description="Tier 1 field coverage report")
    parser.add_argument("files", nargs="+", help="Crawl result JSON files")
    parser.add_argument("--source", help="Filter to a single source type (e.g. BambooHR)")
    args = parser.parse_args()

    all_jobs = []
    for path in args.files:
        try:
            jobs = load_jobs_from_file(path)
            all_jobs.extend(jobs)
        except Exception as e:
            print(f"Warning: could not load {path}: {e}", file=sys.stderr)

    if not all_jobs:
        print("No jobs found in input files.")
        sys.exit(1)

    print(f"\nTotal jobs analysed: {len(all_jobs)}")
    print()
    print_coverage_table(all_jobs, args.source)

    stats = load_extraction_stats(args.files)
    print_stats_table(stats)
    print()


if __name__ == "__main__":
    main()
