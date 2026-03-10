"""
Enrich Unverified Companies Script
==================================

This script automates the transformation of 'unverified' company records (Bronze layer) 
into 'silver' tier records in the Master Manifest (data/companies/*.json).

When to use this script:
------------------------
Run this script whenever the number of 'unverified' companies in BigQuery increases. 
These are companies discovered automatically by the job ingestion pipeline that lack 
structured metadata (industries, HQ location, company type, etc.).

How to use this script:
-----------------------
1. Ensure your local environment has Google Cloud credentials configured (ADC).
2. Set the GEMINI_API_KEY environment variable:
   export GEMINI_API_KEY="your-api-key"
3. Run the script:
   python3 enrich_unverified_companies.py
4. Review the changes in data/companies/.
5. Commit the updated manifest to the repository.
6. Trigger a production sync via the Admin API or the next scheduled sync.

Workflow:
---------
1. Fetches up to 5 unverified companies from BigQuery 'raw_companies' table.
2. Pulls recent job descriptions for each company to provide context.
3. Uses Google Gemini API to intelligently profile the company.
4. Saves structured JSON to data/companies/{id}.json with 'silver' verification status.
"""

import json
import os
import sys
import datetime
from pathlib import Path
from typing import List, Dict, Any
from google.cloud import bigquery
from google import genai
from google.genai import types

# Configurations
PROJECT_ID = "tech-market-insights"
DATASET_ID = "techmarket"
COMPANIES_TABLE = "raw_companies"
JOBS_TABLE = "raw_jobs"
MANIFEST_DIR = Path(__file__).parent.parent.parent / "data" / "companies"

# Initialize clients
try:
    bq_client = bigquery.Client()
except Exception as e:
    print(f"Error initializing BigQuery client: {e}")
    sys.exit(1)

api_key = os.environ.get("GEMINI_API_KEY")
if not api_key:
    print("Error: GEMINI_API_KEY environment variable is not set. Please set it to run the enrichment pipeline.")
    sys.exit(1)

genai_client = genai.Client(api_key=api_key)

def fetch_unverified_companies(limit=5):
    """Fetches unverified companies from BigQuery."""
    print(f"Fetching up to {limit} unverified companies...")
    query = f"""
    SELECT companyId, name, description, website, industries
    FROM `{PROJECT_ID}.{DATASET_ID}.{COMPANIES_TABLE}`
    WHERE verificationLevel = 'unverified'
        AND name != 'Unknown Company'
    ORDER BY companyId ASC
    LIMIT {limit}
    """
    query_job = bq_client.query(query)
    return list(query_job.result())

def fetch_recent_jobs_for_company(company_id, limit=3):
    """Fetches recent job descriptions to give the LLM more context."""
    query = f"""
    SELECT title, description
    FROM `{PROJECT_ID}.{DATASET_ID}.{JOBS_TABLE}`
    WHERE companyId = @company_id
    ORDER BY postedDate DESC
    LIMIT {limit}
    """
    job_config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter("company_id", "STRING", company_id)
        ]
    )
    query_job = bq_client.query(query, job_config=job_config)
    return list(query_job.result())

def enrich_company_via_llm(company_row, job_rows):
    """Calls Gemini to generate the missing silver metadata."""
    print(f"  🤖 Asking Gemini to profile: {company_row.name}...")
    
    # Construct context for LLM
    context = f"Company Name: {company_row.name}\n"
    if company_row.website: context += f"Website: {company_row.website}\n"
    if company_row.description: context += f"Self-Description: {company_row.description}\n"
    if company_row.industries: context += f"Scraped Industries: {company_row.industries}\n"
    
    context += "\nRecent Job Postings Data:\n"
    for idx, job in enumerate(job_rows):
        context += f"--- Job {idx+1}: {job.title} ---\n{job.description[:1000]}...\n" # Truncate description

    prompt = f"""
    You are a data enrichment pipeline for a New Zealand Tech Job Board.
    Given the following context about a company extracted from their job postings, please determine their core metadata.
    
    Context:
    {context}
    
    Tasks:
    1. Clean the 'name' to the primary human-readable brand name (e.g., remove 'Ltd', 'Limited', 'Pty'). Add the old name to 'alternateNames' if you cleaned it.
    2. Determine the 'company_type' (e.g. 'Product', 'Agency', 'Consultancy', 'Government', 'Enterprise').
    3. Determine 'is_agency' (true if they are a recruitment agency or dev-shop consulting firm).
    4. Determine 'hq_country' (Use ISO 2-letter codes like 'NZ', 'US', 'AU', 'UK'. Default to 'NZ' if highly likely).
    5. Summarize the company 'description' in 1-2 sentences.
    6. Extract primary 'industries' (array of strings, e.g. ["SaaS", "Fintech"]).
    7. Guess 'employees_count' if any hint is given, else 0.
    
    Respond STRICTLY in JSON matching the defined schema.
    """

    try:
        response = genai_client.models.generate_content(
            model='gemini-2.0-flash',
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=types.Schema(
                    type=types.Type.OBJECT,
                    properties={
                        "name": types.Schema(type=types.Type.STRING),
                        "alternateNames": types.Schema(type=types.Type.ARRAY, items=types.Schema(type=types.Type.STRING)),
                        "description": types.Schema(type=types.Type.STRING),
                        "company_type": types.Schema(type=types.Type.STRING),
                        "is_agency": types.Schema(type=types.Type.BOOLEAN),
                        "hq_country": types.Schema(type=types.Type.STRING),
                        "industries": types.Schema(type=types.Type.ARRAY, items=types.Schema(type=types.Type.STRING)),
                        "employees_count": types.Schema(type=types.Type.INTEGER),
                    },
                    required=["name", "description", "company_type", "is_agency", "hq_country", "industries", "employees_count"]
                ),
                temperature=0.1
            ),
        )
        return json.loads(response.text)
    except Exception as e:
        print(f"  ❌ LLM Request Failed: {e}")
        return None

def save_to_manifest(companies: List[Dict[str, Any]]):
    """Save enriched companies to individual JSON files in the manifest directory."""
    MANIFEST_DIR.mkdir(parents=True, exist_ok=True)

    print(f"Saving {len(companies)} companies to {MANIFEST_DIR}...")

    for company in companies:
        company_id = company["id"]
        output_file = MANIFEST_DIR / f"{company_id}.json"
        
        # Add metadata and timestamps if not present
        if "updated_at" not in company:
            company["updated_at"] = datetime.datetime.now().isoformat() + "Z"
        
        # Sort keys for consistent formatting
        sorted_company = dict(sorted(company.items()))

        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(sorted_company, f, indent=2, ensure_ascii=False)
            f.write("\n")
        
        print(f"  ✓ Saved {company_id}.json")

    print(f"\n✅ Saved {len(companies)} companies to individual files")

def main(limit=5):
    print(f"🚀 Starting Unverified Enrichment Pipeline (Limit: {limit})...")
    
    # Identify companies in manifest that are still 'unverified'
    unverified_manifests = []
    for file in MANIFEST_DIR.glob('*.json'):
        if file.name == 'schema.json' or file.name.startswith('.'):
            continue
        with open(file, 'r') as f:
            try:
                data = json.load(f)
                if data.get('verification_level') == 'unverified':
                    unverified_manifests.append(data)
            except:
                continue
    
    if not unverified_manifests:
        print("No unverified companies found in manifest. All caught up!")
        return

    print(f"Found {len(unverified_manifests)} unverified manifests localy.")
    silver_records_to_save = []

    for i, company_data in enumerate(unverified_manifests[:limit]):
        company_id = company_data['id']
        name = company_data['name']
        
        print(f"\n[{i+1}/{limit}] Processing unverified: {company_id} ({name})")
        
        # We still want some job context from BQ if possible
        jobs = fetch_recent_jobs_for_company(company_id)
        
        # Build a temporary row object to support existing logic
        class Row:
            def __init__(self, data):
                self.name = data.get('name')
                self.website = data.get('website')
                self.description = data.get('description')
                self.industries = data.get('industries')
        
        enrichment = enrich_company_via_llm(Row(company_data), jobs)
        if not enrichment:
            continue
            
        # Compile final silver record, merging with existing manifest data
        silver_record = company_data.copy()
        silver_record.update({
            "name": enrichment.get("name", name),
            "alternateNames": list(set(enrichment.get("alternateNames", []) + company_data.get("alternateNames", []) + [name])),
            "description": enrichment.get("description", company_data.get("description", "")),
            "industries": list(set(enrichment.get("industries", []) + (company_data.get("industries") if isinstance(company_data.get("industries"), list) else ([company_data.get('industries')] if company_data.get('industries') else [])))),
            "company_type": enrichment.get("company_type", "Unknown"),
            "is_agency": enrichment.get("is_agency", False),
            "hq_country": enrichment.get("hq_country", company_data.get('hq_country', "NZ")),
            "employees_count": enrichment.get("employees_count", 0),
            "verification_level": "silver",
            "updated_at": datetime.datetime.utcnow().isoformat() + "Z"
        })
        
        silver_records_to_save.append(silver_record)
        print(f"  ✅ Prepared {company_id} for saving as silver status!")

    if silver_records_to_save:
        save_to_manifest(silver_records_to_save)
        print("Success! Trigger a backend sync to ingest the new Silver records.")
    else:
        print("\nNo new updates made to manifest.")

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description='Enrich unverified companies using AI')
    parser.add_argument('--limit', type=int, default=5, help='Number of companies to process')
    args = parser.parse_args()
    
    main(args.limit)
