#!/usr/bin/env python3
"""
Generate a company JSON template.

Usage:
    python3 scripts/companies/generate_template.py "Company Name"
"""

import sys
import json
from datetime import datetime
from pathlib import Path

def slugify(name: str) -> str:
    """Convert company name to slug."""
    slug = name.lower()
    slug = slug.replace('&', 'and')
    slug = ''.join(c if c.isalnum() or c == ' ' else '' for c in slug)
    slug = '-'.join(slug.split())
    return slug

def generate_template(company_name: str):
    """Generate company JSON template."""
    company_id = slugify(company_name)
    
    template = {
        "id": company_id,
        "name": company_name,
        "alternateNames": [],
        "description": f"Brief description of {company_name}.",
        "website": "https://",
        "logoUrl": None,
        "industries": [],
        "company_type": "Product",
        "is_agency": False,
        "is_social_enterprise": False,
        "hq_country": "NZ",
        "operating_countries": [],
        "office_locations": [],
        "remote_policy": "Unknown",
        "visa_sponsorship": None,
        "employees_count": None,
        "verification_level": "unverified",
        "updated_at": datetime.utcnow().isoformat() + "Z"
    }
    
    # Output to file
    manifest_dir = Path(__file__).parent.parent.parent / "data" / "companies"
    output_file = manifest_dir / f"{company_id}.json"
    output_file.parent.mkdir(parents=True, exist_ok=True)
    
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(template, f, indent=2, ensure_ascii=False)
        f.write('\n')
    
    print(f"✓ Created template: {output_file}")
    print(f"\nNext steps:")
    print(f"1. Edit {output_file} to add company details")
    print(f"2. Validate: python3 scripts/companies/validate_all.py")
    print(f"3. Commit and submit PR")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 scripts/companies/generate_template.py \"Company Name\"")
        sys.exit(1)
    
    generate_template(sys.argv[1])
