#!/usr/bin/env python3
"""
Extract companies from BigQuery that don't have manifest files yet.

This script:
1. Queries all unique companies from raw_jobs table
2. Compares against existing manifest files
3. Outputs list of companies needing manifest creation
4. Optionally creates basic manifest templates

Usage:
    python3 scripts/companies/extract_missing_from_db.py [--create-templates]
"""

import json
import sys
from pathlib import Path
from typing import Set, List, Dict, Any

try:
    from google.cloud import bigquery
except ImportError:
    print("Error: google-cloud-bigquery not found")
    print("Install: pip install google-cloud-bigquery")
    sys.exit(1)

# Config
PROJECT_ID = "tech-market-insights"
DATASET_ID = "techmarket"
JOBS_TABLE = "raw_jobs"
COMPANIES_TABLE = "raw_companies"
MANIFEST_DIR = Path(__file__).parent.parent.parent / "data" / "companies"

def get_existing_manifests() -> Set[str]:
    """Get set of existing company IDs from manifest files."""
    existing = set()
    
    for file in MANIFEST_DIR.glob('*.json'):
        if file.name == 'schema.json' or file.name.startswith('.'):
            continue
        
        existing.add(file.stem)
    
    return existing

def get_companies_from_jobs() -> Dict[str, Dict[str, Any]]:
    """Extract unique companies from jobs table."""
    print(f"Querying BigQuery: {PROJECT_ID}.{DATASET_ID}.{JOBS_TABLE}...")
    
    try:
        client = bigquery.Client()
    except Exception as e:
        print(f"Error initializing BigQuery client: {e}")
        sys.exit(1)
    
    query = f"""
        SELECT 
            jobs.companyId as id,
            ANY_VALUE(jobs.companyName) as name,
            COUNT(*) as job_count,
            ANY_VALUE(comp.logoUrl) as logoUrl,
            ANY_VALUE(comp.website) as website,
            ANY_VALUE(comp.hqCountry) as hqCountry,
            ANY_VALUE(comp.industries) as industries
        FROM `{PROJECT_ID}.{DATASET_ID}.{JOBS_TABLE}` as jobs
        LEFT JOIN `{PROJECT_ID}.{DATASET_ID}.{COMPANIES_TABLE}` as comp
          ON jobs.companyId = comp.companyId
        WHERE jobs.companyId IS NOT NULL
            AND jobs.companyId != 'Unknown Company'
        GROUP BY jobs.companyId
        ORDER BY job_count DESC
    """
    
    try:
        query_job = client.query(query)
        results = query_job.result()
        
        companies = {}
        for row in results:
            companies[row.id] = {
                'id': row.id,
                'name': row.name,
                'job_count': row.job_count,
                'logoUrl': row.logoUrl,
                'website': row.website,
                'hqCountry': row.hqCountry,
                'industries': row.industries
            }
        
        print(f"✓ Found {len(companies)} unique companies in jobs table")
        return companies
        
    except Exception as e:
        print(f"Error querying BigQuery: {e}")
        sys.exit(1)

def get_companies_from_companies_table() -> Dict[str, Dict[str, Any]]:
    """Extract companies from raw_companies table."""
    print(f"Querying BigQuery: {PROJECT_ID}.{DATASET_ID}.{COMPANIES_TABLE}...")
    
    try:
        client = bigquery.Client()
    except Exception as e:
        print(f"Error initializing BigQuery client: {e}")
        sys.exit(1)
    
    query = f"""
        SELECT 
            companyId as id,
            name,
            logoUrl,
            website,
            verificationLevel,
            industries,
            hqCountry
        FROM `{PROJECT_ID}.{DATASET_ID}.{COMPANIES_TABLE}`
        WHERE companyId IS NOT NULL
        ORDER BY name
    """
    
    try:
        query_job = client.query(query)
        results = query_job.result()
        
        companies = {}
        for row in results:
            companies[row.id] = {
                'id': row.id,
                'name': row.name,
                'logoUrl': row.logoUrl,
                'website': row.website,
                'verificationLevel': row.verificationLevel,
                'industries': row.industries,
                'hqCountry': row.hqCountry
            }
        
        print(f"✓ Found {len(companies)} companies in companies table")
        return companies
        
    except Exception as e:
        print(f"Error querying BigQuery: {e}")
        sys.exit(1)

def create_template(company_data: Dict[str, Any]) -> Dict[str, Any]:
    """Create a basic manifest template from company data."""
    from datetime import datetime
    
    return {
        'id': company_data['id'],
        'name': company_data.get('name', 'Unknown'),
        'alternateNames': [],
        'description': f"{company_data.get('name', 'Company')} company profile.",
        'website': None,
        'logoUrl': company_data.get('logoUrl'),
        'industries': company_data.get('industries', []) if isinstance(company_data.get('industries'), list) else ([company_data.get('industries')] if company_data.get('industries') else []),
        'company_type': 'Unknown',
        'is_agency': False,
        'is_social_enterprise': False,
        'hq_country': company_data.get('hqCountry'),
        'operating_countries': [],
        'office_locations': [],
        'remote_policy': 'Unknown',
        'visa_sponsorship': None,
        'employees_count': None,
        'verification_level': company_data.get('verificationLevel', 'unverified'),
        'updated_at': datetime.utcnow().isoformat() + 'Z'
    }

def extract_missing():
    """Main extraction function."""
    print("🔍 Extracting missing companies from BigQuery...\n")
    
    # Get existing manifests
    existing = get_existing_manifests()
    print(f"✓ Found {len(existing)} existing manifest files\n")
    
    # Get companies from both tables
    jobs_companies = get_companies_from_jobs()
    table_companies = get_companies_from_companies_table()
    
    # Merge company data (prefer companies table data)
    all_companies = {**jobs_companies, **table_companies}
    
    # Find missing companies
    missing = []
    for company_id, data in all_companies.items():
        if company_id not in existing:
            missing.append(data)
    
    # Sort by job count (descending)
    missing.sort(key=lambda x: x.get('job_count', 0), reverse=True)
    
    print(f"\n{'='*60}")
    print(f"📊 Extraction Results")
    print(f"{'='*60}")
    print(f"Existing manifests: {len(existing)}")
    print(f"Companies in DB: {len(all_companies)}")
    print(f"Missing manifests: {len(missing)}")
    print(f"{'='*60}")
    
    if missing:
        print(f"\n⚠️  {len(missing)} companies need manifest files:\n")
        
        # Show top 20 by job count
        print("Top 20 companies by job count:")
        for i, company in enumerate(missing[:20], 1):
            job_count = company.get('job_count', 0)
            print(f"  {i:2d}. {company['name']:50s} ({job_count} jobs)")
        
        if len(missing) > 20:
            print(f"\n  ... and {len(missing) - 20} more companies")
        
        # Save to file
        output_file = Path(__file__).parent / 'missing_companies.json'
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(missing, f, indent=2, ensure_ascii=False)
        
        print(f"\n✓ Saved missing companies to: {output_file}")
        
        return missing
    else:
        print(f"\n✅ All companies have manifest files!")
        return []

if __name__ == "__main__":
    extract_missing()
