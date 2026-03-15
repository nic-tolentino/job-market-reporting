#!/usr/bin/env python3
"""
Check for duplicate companies by name or domain.

Usage:
    python3 scripts/companies/check_duplicates.py
"""

import json
from pathlib import Path
from collections import defaultdict

PROJECT_ROOT = Path(__file__).parent.parent.parent
MANIFEST_DIR = PROJECT_ROOT / "data" / "companies"

def check_duplicates():
    """Check for duplicate companies."""
    print("🔍 Checking for duplicate companies...\n")
    
    # Load all companies
    companies = []
    for file in MANIFEST_DIR.rglob('*.json'):
        if file.name.startswith('.') or file.name == 'schema.json':
            continue
        
        with open(file, 'r', encoding='utf-8') as f:
            company = json.load(f)
            companies.append((file, company))
    
    duplicates_found = False
    
    # Check by name
    names = defaultdict(list)
    for file, company in companies:
        name = company.get('name', '').lower()
        if name:
            names[name].append((file, company))
    
    print("Checking by company name...")
    for name, files in names.items():
        if len(files) > 1:
            duplicates_found = True
            print(f"\n⚠️  Duplicate name: {name}")
            for file, company in files:
                print(f"   - {file.name} (ID: {company.get('id')})")
    
    # Check by domain
    print("\nChecking by website domain...")
    domains = defaultdict(list)
    for file, company in companies:
        website = company.get('website', '')
        if website:
            # Extract domain
            domain = website.replace('https://', '').replace('http://', '').split('/')[0]
            domain = domain.replace('www.', '')
            domains[domain].append((file, company))
    
    for domain, files in domains.items():
        if len(files) > 1:
            duplicates_found = True
            print(f"\n⚠️  Duplicate domain: {domain}")
            for file, company in files:
                print(f"   - {file.name} (ID: {company.get('id')})")
    
    # Check by alternate names
    print("\nChecking by alternate names...")
    alt_names = defaultdict(list)
    for file, company in companies:
        for alt in company.get('alternateNames', []):
            alt_names[alt.lower()].append((file, company))
    
    for name, files in alt_names.items():
        if len(files) > 1:
            duplicates_found = True
            print(f"\n⚠️  Duplicate alternate name: {name}")
            for file, company in files:
                print(f"   - {file.name} (ID: {company.get('id')})")
    
    if not duplicates_found:
        print("\n✅ No duplicates found!")
        return 0
    else:
        print(f"\n⚠️  Duplicates found. Please review and merge if necessary.")
        return 1

if __name__ == "__main__":
    exit(check_duplicates())
