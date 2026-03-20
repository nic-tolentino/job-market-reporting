#!/usr/bin/env python3
"""
Method B: Extract Greenhouse / Lever / Ashby identifiers from raw_jobs.applyUrls in BigQuery.

For every company that doesn't already have an ATS config in the manifest, this script
queries all applyUrls we've ever seen for that company, runs regex extraction against them,
and outputs a CSV of candidates for review.

Usage:
    python3 scripts/ats/extract_ats_from_apply_urls.py
    python3 scripts/ats/extract_ats_from_apply_urls.py --output /tmp/ats_candidates.csv
    python3 scripts/ats/extract_ats_from_apply_urls.py --apply-to-manifests  # write confirmed hits directly

Output CSV columns:
    companyId, manifestId, provider, identifier, jobCount, sampleUrl, alreadyInManifest

Requirements:
    pip install google-cloud-bigquery
"""

import json
import re
import sys
import csv
import argparse
from pathlib import Path
from typing import Optional

try:
    from google.cloud import bigquery
except ImportError:
    print("Error: google-cloud-bigquery not found. Run: pip install google-cloud-bigquery")
    sys.exit(1)

# ── Config ────────────────────────────────────────────────────────────────────

PROJECT_ID   = "tech-market-insights"
DATASET_ID   = "techmarket"
JOBS_TABLE   = f"`{PROJECT_ID}.{DATASET_ID}.raw_jobs`"

MANIFEST_DIR = Path(__file__).parent.parent.parent / "data" / "companies"

DEFAULT_OUTPUT = Path(__file__).parent.parent.parent / "scripts" / "ats" / "ats_candidates_from_apply_urls.csv"

# Regex patterns: each yields the slug as group 1
ATS_PATTERNS = {
    "GREENHOUSE": [
        r"boards\.greenhouse\.io/([^/?#\s]+)",
        r"boards-api\.greenhouse\.io/v1/boards/([^/?#\s]+)",
    ],
    "LEVER": [
        r"jobs\.lever\.co/([^/?#\s]+)",
    ],
    "ASHBY": [
        r"jobs\.ashbyhq\.com/([^/?#\s]+)",
    ],
}

# ── Manifest loading ──────────────────────────────────────────────────────────

def load_manifest_companies() -> dict[str, dict]:
    """
    Returns a dict keyed by companyId (manifest `id` field).
    Only loads companies that do NOT already have an ATS config.
    """
    companies: dict[str, dict] = {}
    for path in MANIFEST_DIR.rglob("*.json"):
        if path.name == "schema.json":
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            companies[data["id"]] = {
                "id": data["id"],
                "name": data.get("name", ""),
                "has_ats": "ats" in data,
                "existing_ats": data.get("ats"),
                "path": path,
            }
        except Exception:
            pass
    return companies


# ── BigQuery query ────────────────────────────────────────────────────────────

QUERY = f"""
SELECT
    companyId,
    url,
    COUNT(*) AS occurrences
FROM
    {JOBS_TABLE},
    UNNEST(applyUrls) AS url
WHERE
    url IS NOT NULL
    AND url != ''
    AND (
        url LIKE '%greenhouse.io%'
        OR url LIKE '%lever.co%'
        OR url LIKE '%ashbyhq.com%'
    )
GROUP BY
    companyId, url
ORDER BY
    companyId, occurrences DESC
"""


def fetch_apply_urls() -> list[dict]:
    """Run the BigQuery query and return rows as a list of dicts."""
    print("Querying BigQuery for Greenhouse / Lever / Ashby apply URLs...")
    client = bigquery.Client()
    rows = list(client.query(QUERY).result())
    print(f"  → {len(rows)} URL rows returned\n")
    return [dict(r) for r in rows]


# ── Extraction ────────────────────────────────────────────────────────────────

def extract_slug(url: str) -> Optional[tuple[str, str]]:
    """
    Returns (provider, slug) if the URL matches a known ATS pattern, else None.
    Strips trailing path segments so we only keep the slug, not a job ID.
    URL-decodes the slug and trims whitespace.
    """
    from urllib.parse import unquote
    for provider, patterns in ATS_PATTERNS.items():
        for pattern in patterns:
            m = re.search(pattern, url, re.IGNORECASE)
            if m:
                slug = m.group(1).rstrip("/")
                # For Greenhouse the URL may be /boards/{slug}/jobs/{id} — take first segment only
                slug = slug.split("/")[0]
                # URL-decode (e.g. "Checkbox%20Technology" → "Checkbox Technology") and trim
                slug = unquote(slug).strip()
                if not slug:
                    return None
                # Reject Greenhouse embed widget path — "embed" is not a board token
                if provider == "GREENHOUSE" and slug.lower() == "embed":
                    return None
                return provider, slug
    return None


def process_rows(rows: list[dict], companies: dict[str, dict]) -> list[dict]:
    """
    Group rows by companyId, extract slugs, and collate results.
    Returns a list of candidate dicts.
    """
    # Build a map: companyId → {(provider, slug) → (occurrences, sample_url)}
    found: dict[str, dict[tuple, dict]] = {}

    for row in rows:
        company_id = row["companyId"]
        url        = row["url"]
        occ        = row["occurrences"]

        result = extract_slug(url)
        if result is None:
            continue

        provider, slug = result
        key = (provider, slug)

        if company_id not in found:
            found[company_id] = {}

        if key not in found[company_id] or found[company_id][key]["occurrences"] < occ:
            found[company_id][key] = {"occurrences": occ, "sample_url": url}

    # Flatten into candidate rows
    candidates = []
    for company_id, hits in found.items():
        manifest = companies.get(company_id, {})
        for (provider, slug), meta in hits.items():
            candidates.append({
                "companyId":        company_id,
                "companyName":      manifest.get("name", ""),
                "provider":         provider,
                "identifier":       slug,
                "occurrences":      meta["occurrences"],
                "sampleUrl":        meta["sample_url"],
                "alreadyInManifest": "YES" if manifest.get("has_ats") else "NO",
                "existingProvider": manifest.get("existing_ats", {}).get("provider", "") if manifest.get("existing_ats") else "",
                "existingId":       manifest.get("existing_ats", {}).get("identifier", "") if manifest.get("existing_ats") else "",
                "_path":            manifest.get("path"),
            })

    # Sort: new discoveries first, then by company
    candidates.sort(key=lambda c: (c["alreadyInManifest"], c["companyId"]))
    return candidates


# ── Output ────────────────────────────────────────────────────────────────────

def write_csv(candidates: list[dict], output_path: Path) -> None:
    fieldnames = [
        "companyId", "companyName", "provider", "identifier",
        "occurrences", "alreadyInManifest", "existingProvider", "existingId", "sampleUrl",
    ]
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for c in candidates:
            writer.writerow({k: c[k] for k in fieldnames})
    print(f"\nWrote {len(candidates)} candidates → {output_path}")


def print_summary(candidates: list[dict]) -> None:
    new   = [c for c in candidates if c["alreadyInManifest"] == "NO"]
    known = [c for c in candidates if c["alreadyInManifest"] == "YES"]

    print(f"\n{'═'*60}")
    print(f"  ATS candidates extracted from apply URLs")
    print(f"{'═'*60}")
    print(f"  New (not yet in manifest):  {len(new)}")
    print(f"  Already in manifest:        {len(known)}")
    print(f"  Total:                      {len(candidates)}")

    if new:
        print(f"\n  New candidates by provider:")
        from collections import Counter
        for provider, count in Counter(c["provider"] for c in new).most_common():
            print(f"    {provider:<16} {count}")

        print(f"\n  Sample new candidates:")
        for c in new[:15]:
            print(f"    {c['companyId']:<30} {c['provider']:<12} {c['identifier']}")
        if len(new) > 15:
            print(f"    ... and {len(new) - 15} more (see CSV)")


# ── Apply to manifests ────────────────────────────────────────────────────────

def apply_to_manifests(candidates: list[dict], dry_run: bool = False) -> None:
    """
    Write confirmed ATS configs to manifest files.
    Only writes for candidates where alreadyInManifest == NO.
    """
    new = [c for c in candidates if c["alreadyInManifest"] == "NO" and c.get("_path")]

    print(f"\n{'─'*60}")
    if dry_run:
        print(f"  DRY RUN — would write {len(new)} ATS configs to manifests")
    else:
        print(f"  Writing {len(new)} ATS configs to manifests...")

    written = 0
    for c in new:
        path: Path = c["_path"]
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            if "ats" in data:
                print(f"  SKIP {c['companyId']} — already has ATS (shouldn't happen)")
                continue

            data["ats"] = {
                "identifier": c["identifier"],
                "provider":   c["provider"],
            }

            if not dry_run:
                # Write with 2-space indent, sorted keys to match existing manifest style
                path.write_text(
                    json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
                    encoding="utf-8",
                )
            print(f"  {'WOULD WRITE' if dry_run else 'WROTE'} {c['companyId']} → {c['provider']} / {c['identifier']}")
            written += 1
        except Exception as e:
            print(f"  ERROR {c['companyId']}: {e}")

    print(f"\n  {'Would write' if dry_run else 'Wrote'} {written} manifest files.")
    if not dry_run:
        print("  Run validate_ats_configs.py to verify the new entries against live APIs.")


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="Output CSV path")
    parser.add_argument("--apply-to-manifests", action="store_true",
                        help="Write confirmed new ATS configs directly to manifest files")
    parser.add_argument("--dry-run", action="store_true",
                        help="With --apply-to-manifests: print what would be written without writing")
    args = parser.parse_args()

    companies  = load_manifest_companies()
    print(f"Loaded {len(companies)} companies from manifest "
          f"({sum(1 for c in companies.values() if c['has_ats'])} already have ATS config)\n")

    rows       = fetch_apply_urls()
    candidates = process_rows(rows, companies)

    print_summary(candidates)
    write_csv(candidates, Path(args.output))

    if args.apply_to_manifests:
        apply_to_manifests(candidates, dry_run=args.dry_run)
    else:
        print(f"\nReview the CSV, then re-run with --apply-to-manifests to write confirmed entries.")
        print(f"Or run with --apply-to-manifests --dry-run to preview first.")


if __name__ == "__main__":
    main()
