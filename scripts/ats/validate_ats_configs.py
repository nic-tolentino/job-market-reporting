#!/usr/bin/env python3
"""
Validate ATS configurations in company manifest files.

Checks:
- Provider is from allowed list
- Identifier matches expected pattern
- Provider/identifier combination is valid (via API for public endpoints)

Usage:
    python3 scripts/ats/validate_ats_configs.py
"""

import json
import sys
from pathlib import Path
from typing import List
import requests

# Paths
PROJECT_ROOT = Path(__file__).parent.parent.parent
MANIFEST_DIR = PROJECT_ROOT / "data" / "companies"

# ATS Providers with public APIs for validation
PROVIDER_API_ENDPOINTS = {
    'GREENHOUSE': 'https://boards-api.greenhouse.io/v1/boards/{id}/jobs',
    'LEVER': 'https://api.lever.co/v0/postings/{id}?limit=1',
    'ASHBY': 'https://api.ashbyhq.com/posting-api/job-board/{id}'
}

VALID_PROVIDERS = [
    'GREENHOUSE',
    'LEVER',
    'ASHBY',
    'WORKDAY',
    'SNAPHIRE',
    'BAMBOOHR',
    'TEAMTAILOR',
    'WORKABLE',
    'SMARTRECRUITERS',
    'JOBADDER',
    'EMPLOYMENT_HERO',
    'SUCCESSFACTORS',
    'FACTORIAL',
    'PERSONIO',
    'MYRECRUITMENTPLUS',
    'COMEET',
    'RECRUITEE',
    'BREEZY',
    'FOUNTAIN',
    'LIVEHIRE',
    'APPLICANTSTACK',
    'JOIN',
    'RECRUITERBOX',
    'ZOHO'
]

def validate_ats_config(company_file: Path) -> List[str]:
    """Validate ATS config for a single company."""
    errors = []
    
    try:
        with open(company_file, 'r', encoding='utf-8') as f:
            company = json.load(f)
    except json.JSONDecodeError as e:
        return [f"{company_file.name}: Invalid JSON - {e}"]
    
    ats = company.get('ats')
    if not ats:
        return errors  # ATS config is optional
    
    # Validate provider
    provider = ats.get('provider')
    if not provider:
        errors.append(f"{company_file.name}: Missing 'provider' field")
    elif provider not in VALID_PROVIDERS:
        errors.append(f"{company_file.name}: Unknown ATS provider '{provider}'. Valid: {', '.join(VALID_PROVIDERS)}")
    
    # Validate identifier format
    identifier = ats.get('identifier')
    if not identifier:
        errors.append(f"{company_file.name}: Missing 'identifier' field")
    elif len(identifier) > 100:
        errors.append(f"{company_file.name}: Identifier too long (max 100 chars)")
    elif not identifier.replace('-', '').replace('_', '').replace('.', '').isalnum():
        errors.append(f"{company_file.name}: Invalid identifier format '{identifier}' (alphanumeric, hyphens, underscores, dots only)")
    
    # Test API (for providers with public APIs)
    if provider in PROVIDER_API_ENDPOINTS and identifier:
        url = PROVIDER_API_ENDPOINTS[provider].format(id=identifier)
        try:
            response = requests.get(url, timeout=10)
            if response.status_code == 404:
                errors.append(f"{company_file.name}: Invalid {provider} identifier '{identifier}' (404 Not Found)")
            elif response.status_code != 200:
                errors.append(f"{company_file.name}: API error for {provider} '{identifier}': HTTP {response.status_code}")
            else:
                # Success - optionally verify jobs are returned
                try:
                    data = response.json()
                    if 'jobs' in data and len(data['jobs']) == 0:
                        print(f"  ⚠️  {company_file.name}: {provider} identifier valid but no jobs returned")
                except:
                    pass  # Not critical
        except requests.Timeout:
            errors.append(f"{company_file.name}: Timeout testing {provider} identifier '{identifier}'")
        except requests.RequestException as e:
            errors.append(f"{company_file.name}: Network error testing {provider}: {e}")
    
    return errors

def validate_all():
    """Validate all company files with ATS configs."""
    print("🔍 Validating ATS configurations in company manifest files...\n")
    
    if not MANIFEST_DIR.exists():
        print(f"❌ Manifest directory not found: {MANIFEST_DIR}")
        return 1
    
    json_files = sorted([
        f for f in MANIFEST_DIR.glob('*.json')
        if not f.name.startswith('.') and f.name != 'schema.json'
    ])
    
    if not json_files:
        print(f"❌ No company files found in {MANIFEST_DIR}")
        return 1
    
    print(f"Found {len(json_files)} company files\n")
    
    # Validate each file
    all_errors = []
    files_with_ats = 0
    
    for file in json_files:
        errors = validate_ats_config(file)
        
        # Check if file has ATS config
        try:
            with open(file, 'r') as f:
                company = json.load(f)
                if company.get('ats'):
                    files_with_ats += 1
        except:
            pass
        
        if errors:
            all_errors.extend(errors)
            print(f"❌ {file.name}:")
            for error in errors:
                print(f"   - {error.split(': ', 1)[-1] if ': ' in error else error}")
    
    # Summary
    print(f"\n{'='*60}")
    print(f"ATS Configuration Validation Results")
    print(f"{'='*60}")
    print(f"Files validated: {len(json_files)}")
    print(f"Files with ATS config: {files_with_ats}")
    print(f"Errors found: {len(all_errors)}")
    
    if all_errors:
        print(f"\n❌ Validation FAILED")
        print(f"\nPlease fix the errors above and re-run validation.")
        return 1
    else:
        print(f"\n✅ All ATS configurations are valid!")
        return 0

if __name__ == "__main__":
    sys.exit(validate_all())
