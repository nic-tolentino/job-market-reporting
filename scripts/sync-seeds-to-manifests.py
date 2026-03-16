#!/usr/bin/env python3
"""
sync-seeds-to-manifests.py

Reads confirmed-active crawler seeds from BigQuery and proposes additions to
the company JSON manifest files in data/companies/.

Usage:
    # Preview proposed changes (dry-run — default)
    python3 scripts/sync-seeds-to-manifests.py

    # Write changes to manifest files on disk
    python3 scripts/sync-seeds-to-manifests.py --apply

    # Filter to a specific status (default: ACTIVE)
    python3 scripts/sync-seeds-to-manifests.py --status ACTIVE --min-jobs 1

After running with --apply, review with:
    git diff data/companies/

Dependencies:
    pip install google-cloud-bigquery
"""

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Optional, Union, List, Dict

# ──────────────────────────────────────────────────────────────────────────────
# Configuration
# ──────────────────────────────────────────────────────────────────────────────

REPO_ROOT = Path(__file__).resolve().parent.parent
COMPANIES_DIR = REPO_ROOT / "data" / "companies"
BQ_PROJECT = os.environ.get("GCP_PROJECT_ID", "tech-market-insights")
BQ_DATASET = os.environ.get("BQ_DATASET", "techmarket")
BQ_TABLE = f"{BQ_PROJECT}.{BQ_DATASET}.crawler_seeds"

# ──────────────────────────────────────────────────────────────────────────────
# BigQuery query
# ──────────────────────────────────────────────────────────────────────────────

def fetch_active_seeds(status: str, min_jobs: int) -> list[dict]:
    """Query BigQuery for seeds that meet the promotion criteria."""
    try:
        from google.cloud import bigquery
    except ImportError:
        print("ERROR: google-cloud-bigquery is not installed.", file=sys.stderr)
        print("Install it with:  pip install google-cloud-bigquery", file=sys.stderr)
        sys.exit(1)

    client = bigquery.Client(project=BQ_PROJECT)

    query = f"""
        SELECT
            company_id   AS companyId,
            url,
            category,
            status,
            last_known_job_count  AS lastKnownJobCount,
            last_crawled_at       AS lastCrawledAt,
            ats_provider          AS atsProvider
        FROM `{BQ_TABLE}`
        WHERE status = @status
          AND (last_known_job_count IS NULL OR last_known_job_count >= @minJobs)
        ORDER BY company_id, url
    """

    job_config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("status", "STRING", status),
            bigquery.ScalarQueryParameter("minJobs", "INT64", min_jobs),
        ]
    )

    rows = client.query(query, job_config=job_config).result()
    return [dict(row) for row in rows]


# ──────────────────────────────────────────────────────────────────────────────
# Manifest helpers
# ──────────────────────────────────────────────────────────────────────────────

def find_manifest(company_id: str) -> Optional[Path]:
    """Return the path to a company's JSON manifest, or None if not found."""
    # First check nested structure: data/companies/{first_char}/{id}.json
    first_char = company_id[0].lower()
    nested_candidate = COMPANIES_DIR / first_char / f"{company_id}.json"
    if nested_candidate.exists():
        return nested_candidate

    # Fallback to legacy root structure: data/companies/{id}.json
    root_candidate = COMPANIES_DIR / f"{company_id}.json"
    return root_candidate if root_candidate.exists() else None


def load_manifest(path: Path) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_manifest(path: Path, data: dict) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def seed_already_present(manifest: dict, url: str) -> bool:
    """Check if a seed URL is already in the manifest's crawler.seeds list."""
    seeds = manifest.get("crawler", {}).get("seeds", [])
    return any(s.get("url") == url for s in seeds)


def add_seed(manifest: dict, url: str, category: str) -> dict:
    """Return a copy of the manifest with the new seed appended."""
    manifest = json.loads(json.dumps(manifest))  # deep copy
    if "crawler" not in manifest:
        manifest["crawler"] = {}
    if "seeds" not in manifest["crawler"]:
        manifest["crawler"]["seeds"] = []
    manifest["crawler"]["seeds"].append({"url": url, "category": category or "general"})
    return manifest


# ──────────────────────────────────────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Sync active crawler seeds from BigQuery into company manifest JSON files."
    )
    parser.add_argument(
        "--apply", action="store_true",
        help="Write changes to manifest files on disk (default: dry-run)"
    )
    parser.add_argument(
        "--status", default="ACTIVE",
        help="Seed status to promote (default: ACTIVE)"
    )
    parser.add_argument(
        "--min-jobs", type=int, default=1, dest="min_jobs",
        help="Minimum last_known_job_count to promote (default: 1)"
    )
    args = parser.parse_args()

    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"=== Seed-to-Manifest Sync ({mode}) ===")
    print(f"Querying {BQ_TABLE} for status={args.status}, min_jobs={args.min_jobs}...")
    print()

    seeds = fetch_active_seeds(args.status, args.min_jobs)
    print(f"Found {len(seeds)} active seeds in BigQuery.")
    print()

    added = 0
    skipped_already_present = 0
    skipped_no_manifest = 0

    # Group by companyId for cleaner output
    from itertools import groupby
    seeds.sort(key=lambda s: s["companyId"])

    for company_id, group in groupby(seeds, key=lambda s: s["companyId"]):
        group_seeds = list(group)
        manifest_path = find_manifest(company_id)

        if manifest_path is None:
            for seed in group_seeds:
                print(f"  SKIP  {company_id}  — manifest not found ({seed['url']})")
                skipped_no_manifest += 1
            continue

        manifest = load_manifest(manifest_path)
        updated = False
        updated_manifest = manifest

        for seed in group_seeds:
            url = seed["url"]
            category = seed.get("category") or "general"

            if seed_already_present(manifest, url):
                skipped_already_present += 1
                continue

            job_count = seed.get("lastKnownJobCount", "?")
            print(f"  ADD   {company_id}  +  {url}  (category={category}, jobs={job_count})")
            updated_manifest = add_seed(updated_manifest, url, category)
            added += 1
            updated = True

        if updated and args.apply:
            write_manifest(manifest_path, updated_manifest)
            print(f"        → wrote {manifest_path.relative_to(REPO_ROOT)}")

    print()
    print(f"Summary:")
    print(f"  To add:              {added}")
    print(f"  Already present:     {skipped_already_present}")
    print(f"  No manifest found:   {skipped_no_manifest}")

    if added > 0 and not args.apply:
        print()
        print("Run with --apply to write these changes to disk, then:")
        print("  git diff data/companies/")
        print("  git add data/companies/ && git commit -m 'chore: sync crawler seeds from BigQuery'")
    elif added > 0 and args.apply:
        print()
        print("Changes written. Review with:  git diff data/companies/")


if __name__ == "__main__":
    main()
