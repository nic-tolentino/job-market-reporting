import json
from google.cloud import bigquery
from pathlib import Path

PROJECT_ID = "tech-market-insights"
DATASET_ID = "techmarket"
COMPANIES_TABLE = "raw_companies"
MANIFEST_DIR = Path(__file__).parent.parent.parent / "data" / "companies"

client = bigquery.Client()

def harvest_logos():
    print("🎨 Harvesting logo URLs from BigQuery...")
    
    # Get all logo URLs from BQ
    query = f"""
    SELECT DISTINCT
        companyId as id,
        ANY_VALUE(logoUrl) as logoUrl
    FROM `{PROJECT_ID}.{DATASET_ID}.{COMPANIES_TABLE}`
    WHERE logoUrl IS NOT NULL AND logoUrl != ''
    GROUP BY companyId
    """
    
    results = list(client.query(query).result())
    logo_map = {row.id: row.logoUrl for row in results}
    print(f"✓ Found {len(logo_map)} logo URLs in BigQuery.")
    
    updated_count = 0
    
    # Update manifests
    for file_path in MANIFEST_DIR.rglob("*.json"):
        if file_path.name == "schema.json" or file_path.name.startswith("."):
            continue
            
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
            
        company_id = data.get("id")
        if not company_id:
            continue
            
        # Only update if current logoUrl is null and we found one in BQ
        if data.get("logoUrl") is None and company_id in logo_map:
            data["logoUrl"] = logo_map[company_id]
            
            # Save manifest
            with open(file_path, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
                f.write("\n")
            
            updated_count += 1
            print(f"  ✓ Updated logo for {data.get('name', company_id)}")
            
    print(f"\n✅ Successfully updated {updated_count} manifests with logo URLs.")

if __name__ == "__main__":
    harvest_logos()
