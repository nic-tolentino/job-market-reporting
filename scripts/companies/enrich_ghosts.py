import json
import os
import sys
from google.cloud import bigquery
from google import genai
from google.genai import types

# Configurations
PROJECT_ID = "techmarket"
DATASET_ID = "raw"
COMPANIES_TABLE = "raw_companies"
JOBS_TABLE = "raw_jobs"
JSON_MANIFEST_PATH = "../../data/companies.json"

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
    """Fetches unverified companies that don't have a recent scrape attached."""
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
    job_config = bigquery.QueryJobConfiguration(
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
            model='gemini-2.5-flash',
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

def main():
    print("🚀 Starting Unverified Enrichment Pipeline...")
    ghosts = fetch_unverified_companies(limit=5)
    
    if not ghosts:
        print("No unverified companies found. All caught up!")
        return

    # Load existing manifest
    try:
        with open(JSON_MANIFEST_PATH, 'r') as f:
            manifest = json.load(f)
    except FileNotFoundError:
        print(f"Manifest not found at {JSON_MANIFEST_PATH}")
        sys.exit(1)
        
    existing_ids = {c['id'] for c in manifest}
    updates_made = False

    for ghost in ghosts:
        company_id = ghost.companyId
        
        # Double check it hasn't literally just been added
        if company_id in existing_ids:
            continue
            
        print(f"\nProcessing unverified: {company_id} ({ghost.name})")
        jobs = fetch_recent_jobs_for_company(company_id)
        
        enrichment = enrich_company_via_llm(ghost, jobs)
        if not enrichment:
            continue
            
        # Compile final silver record
        silver_record = {
            "id": company_id,
            "name": enrichment.get("name", ghost.name),
            "alternateNames": enrichment.get("alternateNames", [ghost.name]),
            "description": enrichment.get("description", ghost.description or ""),
            "website": ghost.website or "",
            "industries": enrichment.get("industries", []),
            "company_type": enrichment.get("company_type", "Unknown"),
            "is_agency": enrichment.get("is_agency", False),
            "is_social_enterprise": False, # Requires human to confirm via B-Corp Db
            "hq_country": enrichment.get("hq_country", "NZ"),
            "operating_countries": ["NZ"],
            "office_locations": [],
            "remote_policy": "Unknown",
            "employees_count": enrichment.get("employees_count", 0),
            "verification_level": "silver"
        }
        
        manifest.append(silver_record)
        updates_made = True
        print(f"  ✅ Added to manifest as silver status!")

    if updates_made:
        print("\n💾 Saving updated manifest and sorting alphabetically...")
        manifest.sort(key=lambda x: x['name'].lower())
        with open(JSON_MANIFEST_PATH, 'w') as f:
            json.dump(manifest, f, indent=2)
        print("Success! Run `./gradlew run` and manually trigger BigQuery sync to ingest the new Silver records.")
    else:
        print("\nNo updates made to manifest.")

if __name__ == "__main__":
    main()
