#!/usr/bin/env python3
"""
Parse the autechjobs MySQL dump and extract value for our pipeline.

Modes:
  (default)             Print a cross-reference report — matched companies, new ATS candidates,
                        unmatched companies, and seed injection candidates.
  --apply-to-manifests  Write validated ATS configs to matched manifest files.
  --inject-seeds        Add jobs_page URLs as crawler seeds to matched manifests.
  --export-jsonl        Export historical job rows as JSONL for BigQuery ingestion.

Input:
  data/third-party/autechjobs/Cloud_SQL_Export_YYYY-MM-DD.sql
  (or specify with --sql-file)

Usage:
    python3 scripts/ats/parse_autechjobs.py
    python3 scripts/ats/parse_autechjobs.py --apply-to-manifests --dry-run
    python3 scripts/ats/parse_autechjobs.py --inject-seeds --dry-run
    python3 scripts/ats/parse_autechjobs.py --export-jsonl --output data/third-party/autechjobs/jobs.jsonl

Requirements:
    pip install aiohttp   (only needed for --apply-to-manifests live validation)
"""

import argparse
import json
import re
import sys
import unicodedata
from collections import defaultdict
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse

# ── Paths ─────────────────────────────────────────────────────────────────────

REPO_ROOT    = Path(__file__).parent.parent.parent
MANIFEST_DIR = REPO_ROOT / "data" / "companies"
THIRD_PARTY  = REPO_ROOT / "data" / "third-party" / "autechjobs"
DEFAULT_SQL  = THIRD_PARTY / "Cloud_SQL_Export_2026-03-17.sql"
FALLBACK_SQL = REPO_ROOT / "data" / "Cloud_SQL_Export_2026-03-17.sql"  # original location

# Greenhouse slugs that are shared embed widgets, not real board tokens
GH_EMBED_SLUGS = {"embed"}

# Known company name → manifest ID overrides for names that don't fuzzy-match
MANUAL_ID_MAP: dict[str, str] = {
    "afterpaytouch":  "afterpay",
    "reagroup":       "rea-group",
    "acloudguru":     "a-cloud-guru",
    "arqgroup":       "arq-group",
    "paloit":         "palo-it",
    "paloit australia": "palo-it",
}


# ── Normalisation helpers ──────────────────────────────────────────────────────

def norm(s: str) -> str:
    """Fold to ASCII lowercase alphanum — used for fuzzy name matching."""
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]", "", s.lower())


# ── SQL parsing ───────────────────────────────────────────────────────────────

_COMPANY_ROW = re.compile(
    r"\((\d+),'((?:[^'\\]|\\.)*)',"       # id, name
    r"((?:NULL|'(?:[^'\\]|\\.)*')),"       # description
    r"(\d+),"                              # visa_sponsor
    r"'([^']+)','([^']+)',"               # created_at, updated_at
    r"(NULL|'[^']*'),(NULL|'[^']*'),(NULL|'[^']*')\)",  # website, logo, jobs_page
    re.DOTALL,
)


def _unquote(v: str) -> Optional[str]:
    if v == "NULL":
        return None
    return v[1:-1].replace("\\'", "'").replace("\\\\", "\\")


def parse_companies(sql: str) -> dict[int, dict]:
    """Parse the `company` INSERT block and return {id: company_dict}."""
    block_m = re.search(r"INSERT INTO `company` VALUES (.*?);\n", sql, re.DOTALL)
    if not block_m:
        raise ValueError("Could not find `company` INSERT block in SQL dump")
    block = block_m.group(1)
    companies = {}
    for m in _COMPANY_ROW.finditer(block):
        cid = int(m.group(1))
        companies[cid] = {
            "id":           cid,
            "name":         m.group(2).replace("\\'", "'"),
            "description":  _unquote(m.group(3)),
            "visa_sponsor": m.group(4) == "1",
            "website":      _unquote(m.group(7)),
            "jobs_page":    _unquote(m.group(9)),
        }
    return companies


def parse_job_ats_slugs(sql: str) -> dict[int, dict[str, set]]:
    """
    Scan all `job` INSERT blocks and build a map of:
        company_id → {provider → set of slugs}
    Only Greenhouse, Lever, and Ashby (free-API) slugs are extracted.
    """
    company_ats: dict[int, dict[str, set]] = defaultdict(lambda: defaultdict(set))
    for block in re.findall(r"INSERT INTO `job` VALUES (.*?);$", sql, re.DOTALL | re.MULTILINE):
        for cid_s, url in re.findall(r"\(\d+,(\d+),'(https?://[^']+)'", block):
            cid = int(cid_s)
            if "boards.greenhouse.io" in url:
                m = re.search(r"boards\.greenhouse\.io/([^/?#]+)", url)
                if m and m.group(1).lower() not in GH_EMBED_SLUGS:
                    company_ats[cid]["GREENHOUSE"].add(m.group(1))
            elif "jobs.lever.co" in url:
                m = re.search(r"jobs\.lever\.co/([^/?#]+)", url)
                if m:
                    company_ats[cid]["LEVER"].add(m.group(1))
            elif "jobs.ashbyhq.com" in url:
                m = re.search(r"jobs\.ashbyhq\.com/([^/?#]+)", url)
                if m:
                    company_ats[cid]["ASHBY"].add(m.group(1))
    return company_ats


def parse_all_jobs(sql: str):
    """
    Generator yielding raw job dicts for --export-jsonl mode.
    Fields: id, company_id, url, position, city, job_type, has_expired, posted_at, expired_at.
    """
    # We extract only the fields safe to export (no description to avoid PII risk)
    pattern = re.compile(
        r"\((\d+),(\d+),'((?:[^'\\]|\\.)*)',"  # id, company_id, url
        r"'((?:[^'\\]|\\.)*)',"                 # position
        r"((?:NULL|'(?:[^'\\]|\\.)*')),"        # department
        r"(?:NULL|'(?:[^'\\]|\\.)*'),"          # description (skipped)
        r"(?:NULL|'(?:[^'\\]|\\.)*'),"          # summary (skipped)
        r"((?:NULL|'[^']*')),"                  # city
        r"((?:NULL|'[^']*')),"                  # job_type
        r"(?:NULL|[\d.]+),(?:NULL|[\d.]+),"     # computer_percent, business_percent
        r"(?:NULL|'(?:[^'\\]|\\.)*'),"          # classifier_data (skipped)
        r"(\d),"                                # has_expired
        r"(\d+),"                               # scrape_count
        r"((?:NULL|'[^']+')),"                  # posted_at
        r"'[^']+','[^']+','?[^']*'?\)",         # created/updated/expired
    )
    for block in re.findall(r"INSERT INTO `job` VALUES (.*?);$", sql, re.DOTALL | re.MULTILINE):
        for m in pattern.finditer(block):
            yield {
                "jobId":        int(m.group(1)),
                "companyId":    int(m.group(2)),
                "url":          m.group(3).replace("\\'", "'"),
                "title":        m.group(4).replace("\\'", "'"),
                "department":   _unquote(m.group(5)),
                "city":         _unquote(m.group(6)),
                "jobType":      _unquote(m.group(7)),
                "hasExpired":   m.group(8) == "1",
                "scrapeCount":  int(m.group(9)),
                "postedAt":     _unquote(m.group(10)),
            }


# ── Manifest loading ───────────────────────────────────────────────────────────

def load_manifests() -> dict[str, dict]:
    """Returns {manifest_id: {name, path, data}} for all company manifests."""
    manifests = {}
    for f in MANIFEST_DIR.rglob("*.json"):
        if f.name == "schema.json":
            continue
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
            manifests[data["id"]] = {"name": data.get("name", ""), "path": f, "data": data}
        except Exception:
            pass
    return manifests


def build_name_lookup(manifests: dict[str, dict]) -> dict[str, str]:
    """
    Returns {normalised_name: manifest_id} covering:
      - manifest name
      - alternateNames array entries
      - manual overrides
    """
    lookup: dict[str, str] = {}
    for mid, info in manifests.items():
        lookup[norm(info["name"])] = mid
        for alt in info["data"].get("alternateNames", []):
            lookup[norm(alt)] = mid
    for sql_norm, mid in MANUAL_ID_MAP.items():
        if mid in manifests:
            lookup[sql_norm] = mid
    return lookup


def find_manifest(name: str, lookup: dict[str, str], manifests: dict[str, dict]) -> Optional[str]:
    """
    Try progressively looser matches. Returns manifest_id or None.
    """
    n = norm(name)
    if n in lookup:
        return lookup[n]
    # Strip common legal/descriptor suffixes from the end
    # Note: "ai" is intentionally excluded — it would turn "openai" → "open" etc.
    for suffix in [
        "group", "limited", "ltd", "inc", "pty", "au", "australia",
        "technologies", "technology", "solutions", "software", "systems",
        "digital", "global", "international",
    ]:
        stripped = re.sub(rf"{suffix}$", "", n)
        if stripped and len(stripped) > 3 and stripped in lookup:
            return lookup[stripped]
    # Substring containment (both directions, min length guard)
    for k, mid in lookup.items():
        if len(n) > 5 and len(k) > 5:
            if n in k or k in n:
                # Extra guard: don't match "open" → "openai" etc.
                ratio = min(len(n), len(k)) / max(len(n), len(k))
                if ratio > 0.75 and mid in manifests:
                    return mid
    return None


# ── Live ATS validation ────────────────────────────────────────────────────────

def validate_ats_slug_sync(provider: str, slug: str) -> Optional[int]:
    """
    Synchronously check whether a slug returns jobs from the live API.
    Returns job count (0 is valid for Greenhouse/Lever) or None on 404/error.
    """
    import urllib.request
    headers = {"User-Agent": "TechMarket/1.0"}

    def get(url: str) -> Optional[dict]:
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=6) as r:
                return json.loads(r.read())
        except Exception:
            return None

    if provider == "GREENHOUSE":
        data = get(f"https://boards-api.greenhouse.io/v1/boards/{slug}/jobs")
        if data and isinstance(data, dict):
            return len(data.get("jobs", []))
    elif provider == "LEVER":
        data = get(f"https://api.lever.co/v0/postings/{slug}?mode=json&limit=1")
        if data is not None and isinstance(data, list):
            return len(data)
    elif provider == "ASHBY":
        data = get(f"https://api.ashbyhq.com/posting-api/job-board/{slug}")
        if data and isinstance(data, dict) and "jobPostings" in data:
            return len(data["jobPostings"])
    return None


# ── Report ────────────────────────────────────────────────────────────────────

def run_report(
    sql_companies: dict[int, dict],
    company_ats: dict[int, dict[str, set]],
    manifests: dict[str, dict],
    lookup: dict[str, str],
) -> None:
    """Print a comprehensive cross-reference report."""
    matched_with_new_ats = []
    matched_already_have_ats = []
    unmatched = []

    for cid, c in sorted(sql_companies.items(), key=lambda x: x[1]["name"]):
        mid = find_manifest(c["name"], lookup, manifests)
        if not mid:
            unmatched.append(c)
            continue

        our = manifests[mid]
        ats = company_ats.get(cid, {})
        if "ats" in our["data"]:
            matched_already_have_ats.append({"sql": c, "manifest_id": mid})
        else:
            for provider, slugs in ats.items():
                if slugs:
                    slug = sorted(slugs)[0]
                    matched_with_new_ats.append({
                        "sql": c, "manifest_id": mid, "manifest_name": our["name"],
                        "provider": provider, "identifier": slug,
                    })

    print(f"\n{'═' * 64}")
    print(f"  autechjobs cross-reference report")
    print(f"{'═' * 64}")
    print(f"  SQL companies:              {len(sql_companies)}")
    print(f"  Matched to our manifests:   {len(matched_with_new_ats) + len(matched_already_have_ats)}")
    print(f"    Already have ATS:         {len(matched_already_have_ats)}")
    print(f"    New ATS candidates:       {len(matched_with_new_ats)}")
    print(f"  Not in our manifests:       {len(unmatched)}")

    if matched_with_new_ats:
        print(f"\n  New ATS candidates to apply:")
        seen = set()
        for r in matched_with_new_ats:
            key = (r["manifest_id"], r["provider"])
            if key not in seen:
                seen.add(key)
                print(f"    {r['manifest_id']:<40} {r['provider']:<12} {r['identifier']}")

    print(f"\n  Unmatched SQL companies (potential additions to our dataset):")
    for c in unmatched:
        jp = f"  jobs_page: {c['jobs_page']}" if c.get("jobs_page") else ""
        ats_str = ""
        ats = company_ats.get(c["id"], {})
        for prov, slugs in ats.items():
            if slugs:
                ats_str += f"  [{prov}: {sorted(slugs)[0]}]"
        print(f"    [{c['id']:3d}] {c['name']:<35}{ats_str}")


# ── Apply ATS configs ─────────────────────────────────────────────────────────

def apply_ats_configs(
    sql_companies: dict[int, dict],
    company_ats: dict[int, dict[str, set]],
    manifests: dict[str, dict],
    lookup: dict[str, str],
    dry_run: bool,
    validate: bool,
) -> None:
    candidates = []
    seen: set[tuple[str, str]] = set()

    for cid, c in sql_companies.items():
        mid = find_manifest(c["name"], lookup, manifests)
        if not mid:
            continue
        our = manifests[mid]
        if "ats" in our["data"]:
            continue
        for provider, slugs in company_ats.get(cid, {}).items():
            if not slugs:
                continue
            # Prefer the slug that appeared most (take first alphabetically as tiebreak)
            slug = sorted(slugs)[0]
            key = (mid, provider)
            if key not in seen:
                seen.add(key)
                candidates.append({
                    "manifest_id": mid, "manifest_name": our["name"],
                    "provider": provider, "identifier": slug,
                    "path": our["path"],
                })

    print(f"\n{'─' * 64}")
    tag = "DRY RUN" if dry_run else "Applying"
    print(f"  {tag}: {len(candidates)} ATS config(s) from autechjobs data\n")

    written = 0
    for c in candidates:
        if validate:
            count = validate_ats_slug_sync(c["provider"], c["identifier"])
            if count is None:
                print(f"  SKIP (404/stale) {c['manifest_id']}: {c['provider']}/{c['identifier']}")
                continue
            print(f"  VALID  {c['manifest_id']:<40} {c['provider']}/{c['identifier']}  ({count} jobs)")
        else:
            print(f"  WRITE  {c['manifest_id']:<40} {c['provider']}/{c['identifier']}")

        if not dry_run:
            path: Path = c["path"]
            data = json.loads(path.read_text(encoding="utf-8"))
            if "ats" not in data:
                data["ats"] = {"identifier": c["identifier"], "provider": c["provider"]}
                path.write_text(
                    json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
                    encoding="utf-8",
                )
                written += 1

    if not dry_run:
        print(f"\n  Wrote {written} manifest files.")


# ── Inject seeds ──────────────────────────────────────────────────────────────

def inject_seeds(
    sql_companies: dict[int, dict],
    manifests: dict[str, dict],
    lookup: dict[str, str],
    dry_run: bool,
) -> None:
    """
    Add jobs_page URL as a crawler seed to matched manifests that don't already have it.
    """
    print(f"\n{'─' * 64}")
    tag = "DRY RUN" if dry_run else "Injecting"
    print(f"  {tag}: careers page seeds from autechjobs data\n")

    written = 0
    skipped = 0
    for cid, c in sorted(sql_companies.items(), key=lambda x: x[1]["name"]):
        jobs_page = c.get("jobs_page")
        if not jobs_page:
            continue

        mid = find_manifest(c["name"], lookup, manifests)
        if not mid:
            continue

        our = manifests[mid]
        existing_seeds = our["data"].get("crawler", {}).get("seeds", [])
        existing_urls = {s.get("url") for s in existing_seeds}

        if jobs_page in existing_urls:
            skipped += 1
            continue

        print(f"  {'WOULD ADD' if dry_run else 'ADDING'} {mid:<40} {jobs_page}")
        if not dry_run:
            path: Path = our["path"]
            data = json.loads(path.read_text(encoding="utf-8"))
            if "crawler" not in data:
                data["crawler"] = {}
            if "seeds" not in data["crawler"]:
                data["crawler"]["seeds"] = []
            # Only add if not already present (re-check after reload)
            urls = {s.get("url") for s in data["crawler"]["seeds"]}
            if jobs_page not in urls:
                data["crawler"]["seeds"].append({
                    "category": "careers",
                    "source":   "autechjobs",
                    "url":      jobs_page,
                })
                path.write_text(
                    json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
                    encoding="utf-8",
                )
                written += 1

    print(f"\n  {'Would write' if dry_run else 'Wrote'} {written} manifest files. {skipped} already had this seed.")


# ── Export JSONL ──────────────────────────────────────────────────────────────

def export_jsonl(
    sql: str,
    sql_companies: dict[int, dict],
    manifests: dict[str, dict],
    lookup: dict[str, str],
    output_path: Path,
) -> None:
    """
    Export job rows to JSONL for BigQuery loading.
    Enriches each row with companyName and resolvedManifestId where a match exists.
    Skips rows without a posted_at date (incomplete historical records).
    """
    # Build company_id → manifest_id map
    cid_to_mid = {}
    for cid, c in sql_companies.items():
        mid = find_manifest(c["name"], lookup, manifests)
        cid_to_mid[cid] = mid

    output_path.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    skipped = 0

    with open(output_path, "w", encoding="utf-8") as f:
        for job in parse_all_jobs(sql):
            if not job.get("postedAt"):
                skipped += 1
                continue
            cid = job["companyId"]
            sql_company = sql_companies.get(cid, {})
            job["companyName"]       = sql_company.get("name", "")
            job["resolvedManifestId"] = cid_to_mid.get(cid)
            job["source"]            = "autechjobs"
            # Remove internal integer ID (not useful in BQ)
            del job["jobId"]
            del job["companyId"]
            f.write(json.dumps(job, ensure_ascii=False) + "\n")
            count += 1

    print(f"\n  Exported {count:,} job rows → {output_path}")
    print(f"  Skipped {skipped:,} rows without posted_at date.")
    print(f"\n  Load to BigQuery:")
    print(f"  bq load --source_format=NEWLINE_DELIMITED_JSON \\")
    print(f"      techmarket.autechjobs_historical \\")
    print(f"      {output_path}")


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--sql-file", default=None, help="Path to SQL dump file")
    parser.add_argument("--apply-to-manifests", action="store_true",
                        help="Write validated ATS configs to manifest files")
    parser.add_argument("--no-validate", action="store_true",
                        help="With --apply-to-manifests: skip live API validation")
    parser.add_argument("--inject-seeds", action="store_true",
                        help="Add jobs_page URLs as crawler seeds to manifests")
    parser.add_argument("--export-jsonl", action="store_true",
                        help="Export job rows as JSONL for BigQuery")
    parser.add_argument("--output", default=None, help="JSONL output path (for --export-jsonl)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Preview all writes without touching any files")
    args = parser.parse_args()

    # Locate SQL file
    if args.sql_file:
        sql_path = Path(args.sql_file)
    elif DEFAULT_SQL.exists():
        sql_path = DEFAULT_SQL
    elif FALLBACK_SQL.exists():
        sql_path = FALLBACK_SQL
        print(f"Note: SQL file found at legacy location {FALLBACK_SQL}")
        print(f"      Consider moving to: {DEFAULT_SQL}\n")
    else:
        print(f"Error: SQL dump not found. Expected at:\n  {DEFAULT_SQL}")
        print("Pass --sql-file <path> to specify the location.")
        sys.exit(1)

    print(f"Parsing SQL dump: {sql_path}")
    sql = sql_path.read_text(encoding="utf-8")

    print("  Parsing company rows...")
    sql_companies = parse_companies(sql)
    print(f"  → {len(sql_companies)} companies")

    print("  Extracting ATS slugs from job URLs...")
    company_ats = parse_job_ats_slugs(sql)
    companies_with_ats = sum(1 for ats in company_ats.values() if any(s for s in ats.values()))
    print(f"  → {companies_with_ats} companies with identifiable ATS slugs")

    print("  Loading manifests...")
    manifests = load_manifests()
    lookup = build_name_lookup(manifests)
    print(f"  → {len(manifests)} manifests loaded")

    if args.apply_to_manifests:
        apply_ats_configs(
            sql_companies, company_ats, manifests, lookup,
            dry_run=args.dry_run,
            validate=not args.no_validate,
        )
    elif args.inject_seeds:
        inject_seeds(sql_companies, manifests, lookup, dry_run=args.dry_run)
    elif args.export_jsonl:
        out = Path(args.output) if args.output else THIRD_PARTY / "historical_jobs.jsonl"
        export_jsonl(sql, sql_companies, manifests, lookup, out)
    else:
        run_report(sql_companies, company_ats, manifests, lookup)


if __name__ == "__main__":
    main()
