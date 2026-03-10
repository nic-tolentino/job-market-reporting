#!/usr/bin/env python3
"""
Validate all company manifest files against JSON schema.
"""

import json
import sys
from pathlib import Path
from typing import List, Dict, Any, Tuple
try:
    import jsonschema
    from jsonschema import Draft7Validator
except ImportError:
    print("Error: jsonschema library not found. Please install it: pip install jsonschema")
    sys.exit(1)

# Paths
PROJECT_ROOT = Path(__file__).parent.parent.parent
MANIFEST_DIR = PROJECT_ROOT / "data" / "companies"
SCHEMA_FILE = MANIFEST_DIR / "schema.json"

def load_schema() -> Dict[str, Any]:
    """Load JSON schema."""
    with open(SCHEMA_FILE, 'r', encoding='utf-8') as f:
        return json.load(f)

def load_company(file: Path) -> Tuple[Dict[str, Any], str]:
    """Load company from JSON file."""
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
        company = json.loads(content)
        return company, content

def validate_schema(company: Dict[str, Any], schema: Dict[str, Any], file: Path) -> List[str]:
    """Validate company against JSON schema."""
    errors = []
    
    validator = Draft7Validator(schema)
    for error in validator.iter_errors(company):
        path = '.'.join(str(p) for p in error.absolute_path)
        if path:
            errors.append(f"{file.name}: {path} - {error.message}")
        else:
            errors.append(f"{file.name}: {error.message}")
    
    return errors

def validate_business_rules(company: Dict[str, Any], file: Path) -> List[str]:
    """Validate business logic rules."""
    errors = []
    
    # ID must match filename
    company_id = company.get('id', '')
    if company_id != file.stem:
        errors.append(f"{file.name}: Company ID '{company_id}' doesn't match filename '{file.stem}'")
    
    # ID must be lowercase
    if company_id != company_id.lower():
        errors.append(f"{file.name}: Company ID must be lowercase: '{company_id}'")
    
    # HQ country check
    hq = company.get('hq_country')
    if hq and len(hq) != 2:
        errors.append(f"{file.name}: HQ country must be a 2-letter ISO code: '{hq}'")
    
    return errors

def validate_all() -> int:
    """Validate all company files."""
    print("🔍 Validating company manifest files...\n")
    
    if not SCHEMA_FILE.exists():
        print(f"❌ Schema file not found: {SCHEMA_FILE}")
        return 1
    
    schema = load_schema()
    
    # Find all JSON files (excluding schema.json)
    json_files = sorted([
        f for f in MANIFEST_DIR.rglob('*.json') 
        if not f.name.startswith('.') and f.name != 'schema.json'
    ])
    
    if not json_files:
        print(f"❌ No company files found in {MANIFEST_DIR}")
        return 1
    
    print(f"✓ Found {len(json_files)} company files\n")
    
    all_errors = []
    for file in json_files:
        try:
            company, content = load_company(file)
            file_errors = validate_schema(company, schema, file)
            file_errors.extend(validate_business_rules(company, file))
            
            if file_errors:
                all_errors.extend(file_errors)
                print(f"❌ {file.name}:")
                for error in file_errors:
                    print(f"   - {error}")
            else:
                # print(f"✓ {file.name}")
                pass
        except json.JSONDecodeError as e:
            all_errors.append(f"{file.name}: Invalid JSON - {e}")
            print(f"❌ {file.name}: Invalid JSON - {e}")
        except Exception as e:
            all_errors.append(f"{file.name}: Unexpected error - {e}")
            print(f"❌ {file.name}: Unexpected error - {e}")
    
    # Summary
    print(f"\n{'='*60}")
    print(f"Validation Results")
    print(f"{'='*60}")
    print(f"Files validated: {len(json_files)}")
    print(f"Errors found: {len(all_errors)}")
    print(f"{'='*60}")
    
    if all_errors:
        print(f"\n❌ Validation FAILED")
        return 1
    else:
        print(f"\n✅ All files are valid!")
        return 0

if __name__ == "__main__":
    sys.exit(validate_all())
