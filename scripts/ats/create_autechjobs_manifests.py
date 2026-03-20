#!/usr/bin/env python3
"""
Create company manifest stubs for the autechjobs SQL companies that
don't yet exist in our manifest dataset.

Reads the SQL dump, parses company + ATS data, generates a manifest file
for each company that doesn't already have one, and writes it to the
correct data/companies/<first-letter>/<id>.json path.

ATS slugs are included where found in the SQL job URL data, but are
NOT live-validated — run validate_ats_configs.py afterwards.

Usage:
    python3 scripts/ats/create_autechjobs_manifests.py --dry-run
    python3 scripts/ats/create_autechjobs_manifests.py
"""

import argparse
import json
import re
import unicodedata
from collections import defaultdict
from pathlib import Path
from typing import Optional

REPO_ROOT    = Path(__file__).parent.parent.parent
MANIFEST_DIR = REPO_ROOT / "data" / "companies"
SQL_PATH     = REPO_ROOT / "data" / "third-party" / "autechjobs" / "Cloud_SQL_Export_2026-03-17.sql"

GH_EMBED_SLUGS = {"embed"}

# ── Companies to skip entirely ────────────────────────────────────────────────
# Retail/non-tech, acquired-with-no-AU-presence, or duplicates of existing entries
SKIP_IDS = {
    48,   # Coles — national retailer, not a tech company
    22,   # Flayr — 1 job, very small
    131,  # Nexigen Digital — 0 jobs, VentraIP group entity (already tracked via ventraip)
    39,   # BigCommerce — 0 AU jobs in this dataset
    24,   # Kounta — acquired by Lightspeed 2019, data is stale
    126,  # Akuna Capital — ATS slug "dovetail" is wrong (uses Dovetail's shared board)
    79,   # T-shirt Ventures — parent company of Redbubble; track via Redbubble directly
    8,    # Ratesetter — rebranded to Plenti in 2021; already in our manifests as "plenti"
    33,   # ezyCollect — 3 jobs, very small; acquired by Archa 2024
    44,   # Zip — already in manifests as "zip-co"
}

# ── Manual metadata overrides ─────────────────────────────────────────────────
# For companies where the SQL data is incomplete or we know better values.
# Keys are SQL company IDs. Each dict is merged over the auto-generated manifest.
OVERRIDES: dict[int, dict] = {
    9: {   # AfterpayTouch → now Block (parent) / Afterpay (brand)
        "id":           "afterpay",
        "name":         "Afterpay",
        "alternateNames": ["AfterpayTouch", "Afterpay Touch", "Block"],
        "hq_country":   "AU",
        "industries":   ["Fintech", "Payments", "BNPL"],
        "website":      "https://www.afterpay.com",
        "notes":        "Rebranded from AfterpayTouch; acquired by Block (formerly Square) in 2022.",
    },
    73: {  # Auth0 → acquired by Okta 2021
        "id":           "auth0",
        "name":         "Auth0",
        "alternateNames": ["Auth0 by Okta"],
        "hq_country":   "US",
        "industries":   ["Identity", "Security", "SaaS"],
        "notes":        "AU engineering hub; acquired by Okta in 2021.",
    },
    15: {  # Clipchamp → acquired by Microsoft 2021
        "id":           "clipchamp",
        "name":         "Clipchamp",
        "hq_country":   "AU",
        "industries":   ["Video", "SaaS", "Creator Tools"],
        "notes":        "AU-founded; acquired by Microsoft in 2021.",
    },
    57: {  # A Cloud Guru → acquired by Pluralsight 2021
        "id":           "a-cloud-guru",
        "name":         "A Cloud Guru",
        "hq_country":   "AU",
        "industries":   ["EdTech", "Cloud", "SaaS"],
        "notes":        "AU-founded; acquired by Pluralsight in 2021.",
    },
    88: {  # Automattic — US HQ, fully remote
        "id":           "automattic",
        "name":         "Automattic",
        "hq_country":   "US",
        "industries":   ["Open Source", "SaaS", "CMS"],
        "remote_policy": "Remote",
    },
    82: {  # Klarna — SE HQ, AU presence
        "id":           "klarna",
        "name":         "Klarna",
        "hq_country":   "SE",
        "industries":   ["Fintech", "Payments", "BNPL"],
    },
    71: {  # Palantir — US HQ, AU presence
        "id":           "palantir",
        "name":         "Palantir",
        "hq_country":   "US",
        "industries":   ["Data Analytics", "AI", "Defence"],
    },
    113: {  # Rackspace — US HQ, AU presence
        "id":           "rackspace",
        "name":         "Rackspace",
        "hq_country":   "US",
        "industries":   ["Cloud", "Managed Services", "Infrastructure"],
    },
    124: {  # Veeva — US HQ, AU office
        "id":           "veeva",
        "name":         "Veeva Systems",
        "alternateNames": ["Veeva"],
        "hq_country":   "US",
        "industries":   ["Life Sciences", "SaaS", "CRM"],
    },
    84: {  # Contino — UK HQ, AU presence
        "id":           "contino",
        "name":         "Contino",
        "hq_country":   "GB",
        "industries":   ["Cloud", "DevOps", "Consulting"],
        "is_agency":    True,
    },
    25: {  # Thiga — FR HQ, global
        "id":           "thiga",
        "name":         "Thiga",
        "hq_country":   "FR",
        "industries":   ["Product Management", "Consulting"],
        "is_agency":    True,
    },
    119: {  # Datto — US HQ, AU presence; acquired by Kaseya 2022
        "id":           "datto",
        "name":         "Datto",
        "hq_country":   "US",
        "industries":   ["MSP", "Backup", "SaaS"],
        "notes":        "Acquired by Kaseya in 2022.",
    },
    128: {  # MILKRUN — acquired by DoorDash
        "id":           "milkrun",
        "name":         "MILKRUN",
        "hq_country":   "AU",
        "industries":   ["Delivery", "Q-Commerce", "Marketplace"],
        "notes":        "Acquired by DoorDash in 2023.",
    },
    7: {   # REAGroup
        "id":           "rea-group",
        "name":         "REA Group",
        "alternateNames": ["REAGroup", "realestate.com.au"],
        "hq_country":   "AU",
        "industries":   ["PropTech", "Marketplace", "Real Estate"],
    },
    # Agencies
    26:  {"is_agency": True, "industries": ["Software Development", "Consulting"]},
    35:  {"is_agency": True, "industries": ["Cloud", "AWS", "Consulting"]},
    36:  {"is_agency": True, "industries": ["Data Engineering", "Cloud", "Consulting"]},
    49:  {"is_agency": True, "industries": ["Digital Transformation", "Consulting"]},
    52:  {"is_agency": True, "industries": ["Cloud", "DevOps", "Consulting"]},
    60:  {"is_agency": True, "industries": ["Quality", "Compliance", "Consulting"]},
    68:  {"is_agency": True, "industries": ["Digital Transformation", "Consulting"]},
    123: {"is_agency": True, "industries": ["Data Analytics", "AI", "Consulting"]},
    125: {"is_agency": True, "industries": ["Mobile", "Software Development", "Consulting"]},
    130: {"is_agency": True, "industries": ["Cloud", "Data", "Consulting"]},
    142: {"is_agency": True, "industries": ["Marketing", "Data Analytics", "Agency"]},
}

# ── Helpers ───────────────────────────────────────────────────────────────────

def norm(s: str) -> str:
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]", "", s.lower())


def make_id(name: str) -> str:
    """Derive a URL-safe manifest ID from a company name."""
    s = unicodedata.normalize("NFKD", name).encode("ascii", "ignore").decode()
    s = s.lower()
    # Replace dots (e.g. "Annalise.ai" → "annalise-ai") before stripping
    s = s.replace(".", "-")
    s = re.sub(r"[^a-z0-9\s\-]", "", s)
    s = re.sub(r"[\s\-]+", "-", s.strip())
    s = s.strip("-")
    return s


def infer_hq(website: Optional[str], name: str) -> str:
    """Best-guess hq_country from TLD or name."""
    if website:
        host = website.lower()
        if ".co.nz" in host or ".nz" in host:
            return "NZ"
        if ".com.au" in host or ".net.au" in host or ".org.au" in host or ".au" in host:
            return "AU"
    # Default to AU since this is an AU job board dataset
    return "AU"


# ── SQL parsing (minimal — reuses parse_autechjobs logic) ────────────────────

def parse_sql(sql_path: Path) -> tuple[dict, dict]:
    sql = sql_path.read_text(encoding="utf-8")

    row_pattern = re.compile(
        r"\((\d+),'((?:[^'\\]|\\.)*)',((?:NULL|'(?:[^'\\]|\\.)*')),(\d+),'([^']+)','([^']+)',"
        r"(NULL|'[^']*'),(NULL|'[^']*'),(NULL|'[^']*')\)",
        re.DOTALL,
    )

    def unquote(v: str) -> Optional[str]:
        if v == "NULL":
            return None
        return v[1:-1].replace("\\'", "'").replace("\\\\", "\\").strip()

    company_block = re.search(r"INSERT INTO `company` VALUES (.*?);\n", sql, re.DOTALL).group(1)
    companies = {}
    for m in row_pattern.finditer(company_block):
        cid = int(m.group(1))
        companies[cid] = {
            "id":           cid,
            "name":         m.group(2).replace("\\'", "'"),
            "description":  unquote(m.group(3)),
            "visa_sponsor": m.group(4) == "1",
            "website":      unquote(m.group(7)),
            "jobs_page":    unquote(m.group(9)),
        }

    ats: dict[int, dict[str, set]] = defaultdict(lambda: defaultdict(set))
    for block in re.findall(r"INSERT INTO `job` VALUES (.*?);$", sql, re.DOTALL | re.MULTILINE):
        for cid_s, url in re.findall(r"\(\d+,(\d+),'(https?://[^']+)'", block):
            cid = int(cid_s)
            if "boards.greenhouse.io" in url:
                m2 = re.search(r"boards\.greenhouse\.io/([^/?#]+)", url)
                if m2 and m2.group(1).lower() not in GH_EMBED_SLUGS:
                    ats[cid]["GREENHOUSE"].add(m2.group(1))
            elif "jobs.lever.co" in url:
                m2 = re.search(r"jobs\.lever\.co/([^/?#]+)", url)
                if m2:
                    ats[cid]["LEVER"].add(m2.group(1))
            elif "jobs.ashbyhq.com" in url:
                m2 = re.search(r"jobs\.ashbyhq\.com/([^/?#]+)", url)
                if m2:
                    ats[cid]["ASHBY"].add(m2.group(1))

    return companies, ats


# ── Manifest construction ─────────────────────────────────────────────────────

def build_manifest(sql_co: dict, ats: dict[str, set]) -> dict:
    name     = sql_co["name"]
    website  = sql_co.get("website")
    desc     = sql_co.get("description")
    cid      = sql_co["id"]

    manifest_id = make_id(name)
    hq          = infer_hq(website, name)

    manifest: dict = {
        "id":                 manifest_id,
        "name":               name,
        "hq_country":         hq,
        "is_agency":          False,
        "is_social_enterprise": False,
        "verification_level": "unverified",
    }

    if website:
        manifest["website"] = website

    if desc and len(desc) > 20:
        manifest["description"] = desc

    if sql_co.get("visa_sponsor"):
        manifest["visa_sponsorship"] = {
            "offered": True,
            "source":  "autechjobs",
            "last_verified": None,
            "notes": None,
            "types": [],
        }

    # Add jobs_page as seed
    jobs_page = sql_co.get("jobs_page")
    if jobs_page:
        manifest["crawler"] = {
            "seeds": [{"category": "careers", "source": "autechjobs", "url": jobs_page}]
        }

    # ATS: prefer free-API providers; if multiple, take the one with most slugs
    free_providers = ["GREENHOUSE", "LEVER", "ASHBY"]
    for provider in free_providers:
        slugs = ats.get(provider, set())
        if slugs:
            slug = sorted(slugs, key=len)[0]  # prefer shorter/cleaner slug
            manifest["ats"] = {"identifier": slug, "provider": provider}
            break

    # Apply overrides
    overrides = OVERRIDES.get(cid, {})
    for key, val in overrides.items():
        if isinstance(val, dict) and key in manifest and isinstance(manifest[key], dict):
            manifest[key].update(val)
        else:
            manifest[key] = val

    # Re-sort keys
    return dict(sorted(manifest.items()))


def load_existing_ids() -> set[str]:
    ids = set()
    for f in MANIFEST_DIR.rglob("*.json"):
        if f.name == "schema.json":
            continue
        try:
            d = json.loads(f.read_text(encoding="utf-8"))
            ids.add(d["id"])
        except Exception:
            pass
    return ids


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dry-run", action="store_true", help="Print what would be created, don't write files")
    args = parser.parse_args()

    print(f"Parsing SQL dump: {SQL_PATH}")
    sql_companies, company_ats = parse_sql(SQL_PATH)

    existing_ids = load_existing_ids()
    print(f"Existing manifests: {len(existing_ids)}")

    created = 0
    skipped_exists = 0
    skipped_rule = 0

    for cid, sql_co in sorted(sql_companies.items(), key=lambda x: x[1]["name"]):
        if cid in SKIP_IDS:
            skipped_rule += 1
            continue

        manifest = build_manifest(sql_co, company_ats.get(cid, {}))
        manifest_id = manifest["id"]

        if manifest_id in existing_ids:
            skipped_exists += 1
            continue

        first_char = manifest_id[0] if manifest_id[0].isalpha() else "0"
        out_path = MANIFEST_DIR / first_char / f"{manifest_id}.json"

        ats_str  = f"  ats={manifest['ats']['provider']}/{manifest['ats']['identifier']}" if "ats" in manifest else ""
        note_str = f"  NOTE: {manifest['notes']}" if "notes" in manifest else ""
        print(f"  {'WOULD CREATE' if args.dry_run else 'CREATE'} {manifest_id:<45}{ats_str}{note_str}")

        if not args.dry_run:
            out_path.parent.mkdir(parents=True, exist_ok=True)
            out_path.write_text(
                json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
                encoding="utf-8",
            )
            created += 1

    print(f"\n{'─' * 60}")
    if args.dry_run:
        print(f"  DRY RUN complete. Would create {len(sql_companies) - skipped_exists - skipped_rule} manifests.")
    else:
        print(f"  Created {created} manifests.")
    print(f"  Skipped {skipped_exists} (already exist) + {skipped_rule} (excluded by rule)")
    if not args.dry_run:
        print(f"\n  Next: run validate_ats_configs.py to verify ATS slugs against live APIs.")


if __name__ == "__main__":
    main()
