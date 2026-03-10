#!/usr/bin/env python3
import json
import sys
from pathlib import Path
from datetime import datetime

# Config
SCRIPTS_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPTS_DIR.parent.parent
MANIFEST_DIR = PROJECT_ROOT / "data" / "companies"
MISSING_FILE = SCRIPTS_DIR / "missing_companies.json"

def slugify(name: str) -> str:
    """Convert company name to slug."""
    slug = name.lower()
    slug = slug.replace('&', 'and')
    slug = ''.join(c if c.isalnum() or c == ' ' else '' for c in slug)
    slug = '-'.join(slug.split())
    return slug

def batch_generate(limit: int = 100):
    if not MISSING_FILE.exists():
        print(f"Error: {MISSING_FILE} not found. Run extract_missing_from_db.py first.")
        return

    with open(MISSING_FILE, 'r', encoding='utf-8') as f:
        missing_companies = json.load(f)

    print(f"🚀 Processing top {min(limit, len(missing_companies))} out of {len(missing_companies)} missing companies...")

    created = 0
    skipped = 0

    for i, company in enumerate(missing_companies[:limit]):
        name = company['name']
        company_id = company['id'] # Use ID from BigQuery
        
        # Verify slug matches DB id, or fallback
        if not company_id:
            company_id = slugify(name)
            
        file_path = MANIFEST_DIR / f"{company_id}.json"
        
        if file_path.exists():
            skipped += 1
            continue

        template = {
            "id": company_id,
            "name": name,
            "alternateNames": [],
            "description": f"{name} is a company profile created during backlog processing.",
            "website": company.get('website'),
            "logoUrl": company.get('logoUrl'),
            "industries": company.get('industries', []) if isinstance(company.get('industries'), list) else ([company.get('industries')] if company.get('industries') else []),
            "company_type": "Unknown",
            "is_agency": False,
            "is_social_enterprise": False,
            "hq_country": company.get('hqCountry') or "NZ",
            "operating_countries": [],
            "office_locations": [],
            "remote_policy": "Unknown",
            "visa_sponsorship": None,
            "employees_count": None,
            "verification_level": "unverified",
            "updated_at": datetime.utcnow().isoformat() + "Z"
        }

        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(template, f, indent=2, ensure_ascii=False)
            f.write('\n')
        
        created += 1

    print(f"\n✅ Finished batch processing:")
    print(f"  - Created: {created}")
    print(f"  - Skipped (already exists): {skipped}")
    print(f"\nNext step: Run 'python3 scripts/companies/validate_all.py'")

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description='Batch generate manifest templates')
    parser.add_argument('--limit', type=int, default=100, help='Number of companies to process')
    args = parser.parse_args()
    
    batch_generate(args.limit)
