#!/usr/bin/env python3
"""
Discover ATS for companies that don't have ATS configuration yet.

This script:
1. Loads companies without ATS config from manifest files
2. Fetches company website from manifest or BigQuery
3. Scans careers page for ATS markers
4. Extracts ATS provider and identifier
5. Updates manifest files with ATS config

Usage:
    python3 scripts/ats/discover_missing_ats.py [--limit=50]
"""

import json
import sys
import re
from pathlib import Path
from typing import List, Dict, Any, Optional, Tuple

try:
    import requests
except ImportError:
    print("Error: requests library not found")
    print("Install: pip install requests")
    sys.exit(1)

try:
    from google.cloud import bigquery
except ImportError:
    print("Error: google-cloud-bigquery not found")
    print("Install: pip install google-cloud-bigquery")
    sys.exit(1)

# Config
PROJECT_ID = "tech-market-insights"
DATASET_ID = "techmarket"
COMPANIES_TABLE = "raw_companies"
JOBS_TABLE = "raw_jobs"
MANIFEST_DIR = Path(__file__).parent.parent.parent / "data" / "companies"

def get_apply_urls_from_bq(company_ids: List[str]) -> Dict[str, List[str]]:
    """Get some potential apply URLs for companies from BigQuery."""
    if not company_ids:
        return {}
    
    print(f"Querying BigQuery for sample apply URLs...")
    
    try:
        client = bigquery.Client()
    except Exception as e:
        print(f"Error initializing BigQuery client: {e}")
        return {}
    
    # Get up to 5 apply URLs per company to check for ATS markers
    query = f"""
        SELECT 
            companyId as id,
            ARRAY_AGG(url LIMIT 5) as urls
        FROM `{PROJECT_ID}.{DATASET_ID}.{JOBS_TABLE}`,
        UNNEST(applyUrls) as url
        WHERE companyId IN UNNEST(@ids)
            AND url IS NOT NULL 
            AND url != ''
        GROUP BY companyId
    """
    
    job_config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ArrayQueryParameter("ids", "STRING", company_ids)
        ]
    )
    
    try:
        query_job = client.query(query, job_config=job_config)
        results = query_job.result()
        
        apply_urls = {}
        for row in results:
            apply_urls[row.id] = row.urls
        
        print(f"✓ Found sample apply URLs for {len(apply_urls)} companies")
        return apply_urls
        
    except Exception as e:
        print(f"Error querying BigQuery for apply URLs: {e}")
        return {}

# ATS markers to search for
ATS_MARKERS = {
    'GREENHOUSE': [r'boards\.greenhouse\.io/([a-zA-Z0-9_-]+)', r'boards-api\.greenhouse\.io/v1/boards/([a-zA-Z0-9_-]+)'],
    'LEVER': [r'jobs\.lever\.co/([a-zA-Z0-9_-]+)', r'api\.lever\.co/v0/postings/([a-zA-Z0-9_-]+)'],
    'ASHBY': [r'jobs\.ashbyhq\.com/([a-zA-Z0-9_-]+)', r'api\.ashbyhq\.com/posting-api/job-board/([a-zA-Z0-9_-]+)'],
    'WORKDAY': [r'([a-zA-Z0-9_-]+)\.wd[0-9]+\.myworkdayjobs\.com'],
    'BAMBOOHR': [r'([a-zA-Z0-9_-]+)\.bamboohr\.com'],
    'TEAMTAILOR': [r'([a-zA-Z0-9_-]+)\.teamtailor\.com'],
    'WORKABLE': [r'apply\.workable\.com/([a-zA-Z0-9_-]+)'],
    'SMARTRECRUITERS': [r'careers\.smartrecruiters\.com/([a-zA-Z0-9_-]+)', r'jobs\.smartrecruiters\.com/([a-zA-Z0-9_-]+)'],
    'SNAPHIRE': [r'([a-zA-Z0-9_-]+)\.snaphire\.com'],
    'SUCCESSFACTORS': [r'([a-zA-Z0-9_-]+)\.successfactors\.com', r'([a-zA-Z0-9_-]+)\.hcm\.onesource\.sap\.com'],
    'FACTORIAL': [r'([a-zA-Z0-9_-]+)\.factorialhr\.com'],
    'PERSONIO': [r'([a-zA-Z0-9_-]+)\.personio\.com', r'([a-zA-Z0-9_-]+)\.jobs\.personio\.com'],
    'MYRECRUITMENTPLUS': [r'form\.myrecruitmentplus\.com/.*jobAdId=([0-9]+)'],
    'JOBADDER': [r'apply\.jobadder\.com/([a-zA-Z0-9]+)'],
    'EMPLOYMENT_HERO': [r'employmenthero\.com/([a-zA-Z0-9_-]+)'],
    'COMEET': [r'comeet\.com/jobs/([a-zA-Z0-9_-]+)'],
    'RECRUITEE': [r'([a-zA-Z0-9_-]+)\.recruitee\.com'],
    'BREEZY': [r'([a-zA-Z0-9_-]+)\.breezy\.hr'],
    'FOUNTAIN': [r'([a-zA-Z0-9_-]+)\.fountain\.com'],
    'LIVEHIRE': [r'livehire\.com/job/([a-zA-Z0-9_-]+)'],
    'APPLICANTSTACK': [r'([a-zA-Z0-9_-]+)\.applicantstack\.com'],
    'JOIN': [r'join\.com/companies/([a-zA-Z0-9_-]+)'],
    'RECRUITERBOX': [r'([a-zA-Z0-9_-]+)\.recruiterbox\.com'],
    'ZOHO': [r'([a-zA-Z0-9_-]+)\.zohorecruit\.com'],
}

def load_manifest(company_id: str) -> Optional[Dict[str, Any]]:
    """Load company manifest file."""
    file_path = MANIFEST_DIR / f"{company_id}.json"
    
    if not file_path.exists():
        return None
    
    with open(file_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def save_manifest(company_id: str, data: Dict[str, Any]):
    """Save company manifest file."""
    file_path = MANIFEST_DIR / f"{company_id}.json"
    
    # Sort keys for consistent formatting
    sorted_data = dict(sorted(data.items()))
    
    # Atomic write
    temp_file = file_path.with_suffix('.json.tmp')
    with open(temp_file, 'w', encoding='utf-8') as f:
        json.dump(sorted_data, f, indent=2, ensure_ascii=False)
        f.write('\n')
    
    temp_file.rename(file_path)

def get_companies_without_ats() -> List[Dict[str, Any]]:
    """Get companies from manifest that don't have ATS config."""
    companies = []
    
    for file in MANIFEST_DIR.glob('*.json'):
        if file.name == 'schema.json' or file.name.startswith('.'):
            continue
        
        with open(file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Skip if already has ATS config
        if 'ats' in data and data['ats']:
            continue
        
        companies.append({
            'id': data['id'],
            'name': data['name'],
            'website': data.get('website'),
            'manifest_file': file.name
        })
    
    print(f"✓ Found {len(companies)} companies without ATS config")
    return companies

def get_company_websites_from_bq() -> Dict[str, str]:
    """Get company websites from BigQuery."""
    print(f"Querying BigQuery for company websites...")
    
    try:
        client = bigquery.Client()
    except Exception as e:
        print(f"Error initializing BigQuery client: {e}")
        return {}
    
    query = f"""
        SELECT DISTINCT
            companyId as id,
            ANY_VALUE(website) as website
        FROM `{PROJECT_ID}.{DATASET_ID}.{COMPANIES_TABLE}`
        WHERE website IS NOT NULL
        GROUP BY companyId
    """
    
    try:
        query_job = client.query(query)
        results = query_job.result()
        
        websites = {}
        for row in results:
            if row.website:
                websites[row.id] = row.website
        
        print(f"✓ Found {len(websites)} company websites")
        return websites
        
    except Exception as e:
        print(f"Error querying BigQuery: {e}")
        return {}

def scan_careers_page(company_name: str, website: str, apply_urls: List[str] = []) -> Optional[Tuple[str, str]]:
    """Scan company careers page and apply URLs for ATS markers."""
    targets = []
    
    # 1. First, check apply URLs as they are direct leads
    for url in (apply_urls or []):
        if url:
            targets.append(('apply_url', url))
            
    # 2. Then try common careers page patterns on the website
    if website:
        careers_paths = ['/careers', '/jobs', '/work-with-us', '/careers/', '']
        for path in careers_paths:
            url = website.rstrip('/') + '/' + path.lstrip('/')
            targets.append(('site_page', url))
    
    if not targets:
        return None
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (compatible; DevAssembly-ATS-Discovery/1.0)'
    }
    
    scanned_urls = set()
    
    for type_label, url in targets:
        if url in scanned_urls:
            continue
        scanned_urls.add(url)
            
        try:
            print(f"    - Checking {type_label}: {url[:80]}...")
            response = requests.get(url, headers=headers, timeout=10, allow_redirects=True)
            
            # Check final URL after redirects (highest signal)
            final_url = response.url
            for provider, patterns in ATS_MARKERS.items():
                for pattern in patterns:
                    match = re.search(pattern, final_url, re.IGNORECASE)
                    if match:
                        identifier = match.group(1) if match.groups() else "discovered"
                        print(f"  ✓ Found {provider} via {type_label} redirect: {identifier}")
                        return (provider, identifier)
            
            # Check page source for ATS markers
            html = response.text
            for provider, patterns in ATS_MARKERS.items():
                for pattern in patterns:
                    match = re.search(pattern, html, re.IGNORECASE)
                    if match:
                        identifier = match.group(1) if match.groups() else "discovered"
                        print(f"  ✓ Found {provider} in {type_label} source: {identifier}")
                        return (provider, identifier)
            
        except requests.Timeout:
            pass # Silent fail per target
        except requests.RequestException as e:
            pass # Silent fail per target
    
    return None

def discover_ats(limit: int = 50):
    """Main discovery function."""
    print("🔍 Discovering ATS for companies without config...\n")
    
    # Get companies without ATS
    companies = get_companies_without_ats()
    
    if not companies:
        print("\n✅ All companies have ATS config!")
        return
    
    # Get websites from BigQuery
    bq_websites = get_company_websites_from_bq()
    
    # Merge websites
    for company in companies:
        if not company['website'] and company['id'] in bq_websites:
            company['website'] = bq_websites[company['id']]
    
    print(f"\nScanning {min(limit, len(companies))} companies...\n")
    
    # Batch fetch apply URLs for the limit
    target_ids = [c['id'] for c in companies[:limit]]
    all_apply_urls = get_apply_urls_from_bq(target_ids)
    
    discovered = 0
    not_found = 0
    
    for i, company in enumerate(companies[:limit], 1):
        print(f"[{i}/{min(limit, len(companies))}] {company['name']}")
        
        apply_urls = all_apply_urls.get(company['id'], [])
        
        if not company['website'] and not apply_urls:
            print(f"  ⚠️  No website or apply URLs found")
            not_found += 1
            continue
        
        result = scan_careers_page(company['name'], company['website'], apply_urls)
        
        if result:
            provider, identifier = result
            
            # Load manifest
            data = load_manifest(company['id'])
            if data:
                # Add ATS config
                data['ats'] = {
                    'provider': provider,
                    'identifier': identifier
                }
                
                # Save manifest
                save_manifest(company['id'], data)
                print(f"  ✓ Updated manifest with {provider}/{identifier}")
                discovered += 1
        else:
            print(f"  ⚠️  No ATS found")
            not_found += 1
    
    print(f"\n{'='*60}")
    print(f"📊 Discovery Results")
    print(f"{'='*60}")
    print(f"Companies scanned: {min(limit, len(companies))}")
    print(f"ATS discovered: {discovered}")
    print(f"No ATS found: {not_found}")
    print(f"{'='*60}")
    
    if discovered > 0:
        print(f"\n✅ Successfully discovered {discovered} ATS configurations!")
        print(f"\nNext steps:")
        print(f"1. Review changes: git diff data/companies/")
        print(f"2. Validate: python3 scripts/ats/validate_ats_configs.py")
        print(f"3. Commit: git commit -m 'Discover ATS for {discovered} companies'")

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description='Discover ATS for companies')
    parser.add_argument('--limit', type=int, default=50, help='Limit number of companies to scan')
    args = parser.parse_args()
    
    discover_ats(args.limit)
