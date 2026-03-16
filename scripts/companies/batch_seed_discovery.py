
import os
import json
import requests
import subprocess
import time
from pathlib import Path

# Configuration
ADMIN_PANEL_TOKEN = os.environ.get("ADMIN_PANEL_TOKEN")
API_BASE_URL = os.environ.get("BACKEND_URL", "http://localhost:8081").rstrip('/') + "/api/admin/crawler"
COMPANIES_DIR = Path("data/companies")

def get_companies_with_websites():
    companies = []
    for file in COMPANIES_DIR.rglob("*.json"):
        try:
            with open(file, 'r') as f:
                data = json.load(f)
                if data.get("website") and data["website"].startswith("http"):
                    companies.append({
                        "id": data["id"],
                        "website": data["website"]
                    })
        except Exception as e:
            print(f"Error reading {file}: {e}")
    return companies

def get_active_seeds():
    # Query BigQuery for companies that already have active seeds
    try:
        print("Executing BigQuery query...")
        cmd = [
            "bq", "query", "--use_legacy_sql=false", "--format=json",
            "SELECT DISTINCT company_id FROM `tech-market-insights.techmarket.crawler_seeds` WHERE status = 'ACTIVE'"
        ]
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"BigQuery error: {result.stderr}")
            return set()
        
        print(f"Query successful. Parsing JSON output...")
        seeds = json.loads(result.stdout)
        ids = {s["company_id"] for s in seeds}
        print(f"Parsed {len(ids)} unique company IDs.")
        return ids
    except Exception as e:
        print(f"Failed to fetch active seeds: {e}")
        return set()

def trigger_discovery(company_id, website):
    if not ADMIN_PANEL_TOKEN:
        print("Error: ADMIN_PANEL_TOKEN environment variable not set.")
        return False

    url = f"{API_BASE_URL}/companies/{company_id}/crawl"
    headers = {
        "Authorization": f"Bearer {ADMIN_PANEL_TOKEN}",
        "Content-Type": "application/json"
    }
    payload = {
        "url": website,
        "isDiscovery": True,
        "maxPages": 10 # Discovery budget
    }

    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        if response.status_code == 200:
            print(f"Successfully triggered discovery for {company_id}")
            return True
        else:
            print(f"Failed to trigger {company_id}: {response.status_code} - {response.text}")
            return False
    except Exception as e:
        print(f"Request error for {company_id}: {e}")
        return False

def main():
    print("Scanning manifests...")
    all_with_web = get_companies_with_websites()
    print(f"Found {len(all_with_web)} companies with websites in manifests.")

    print("Checking existing active seeds in BigQuery...")
    active_seeds = get_active_seeds()
    print(f"Found {len(active_seeds)} companies with active seeds.")

    to_crawl = [c for c in all_with_web if c["id"] not in active_seeds]
    print(f"Targeting {len(to_crawl)} companies for discovery crawl.")

    # Limit to 10 for safety in this manual run, or let the user decide
    limit = 10 
    print(f"Starting batch of {limit}...")
    
    count = 0
    for company in to_crawl[:limit]:
        if trigger_discovery(company["id"], company["website"]):
            count += 1
        time.sleep(1) # Rate limit requests to our own backend

    print(f"\nDone! Triggered {count} discovery crawls.")
    print("You can check the progress in the Admin UI Crawler Logs.")

if __name__ == "__main__":
    main()
