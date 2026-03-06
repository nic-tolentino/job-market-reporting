import csv
import json
import re
import datetime

def slugify(text):
    text = text.lower()
    text = re.sub(r'[^a-z0-9]+', '-', text)
    text = text.strip('-')
    return text

def parse_industries(industries_str):
    if not industries_str:
        return []
    # Try to split by common separators found in the CSV
    separators = [', and ', ', ', ' and ']
    industries = [industries_str]
    for sep in separators:
        new_industries = []
        for ind in industries:
            new_industries.extend([i.strip() for i in ind.split(sep) if i.strip()])
        industries = new_industries
    return list(set(industries))

def bootstrap():
    companies = []
    with open('/tmp/companies_extract.csv', mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            name = row['name']
            if not name:
                continue
            
            company_id = slugify(name)
            
            # Simple starting point
            company = {
                "id": company_id,
                "name": name,
                "alternateNames": [name], # Will be refined
                "description": "", # To be enriched
                "website": row['website'],
                "logoUrl": row['logoUrl'],
                "industries": parse_industries(row['industries']),
                "company_type": "Unknown", # To be enriched
                "is_agency": False, # To be enriched
                "is_social_enterprise": False, # To be enriched
                "hq_country": "Unknown", # To be enriched
                "operating_countries": [], # To be enriched
                "office_locations": [], # To be enriched
                "remote_policy": "Unknown", # To be enriched
                "visa_sponsorship": False, # To be enriched
                "updated_at": datetime.datetime.now().isoformat() + "Z"
            }
            companies.append(company)
    
    with open('/tmp/companies_initial.json', 'w', encoding='utf-8') as f:
        json.dump(companies, f, indent=2)

if __name__ == "__main__":
    bootstrap()
