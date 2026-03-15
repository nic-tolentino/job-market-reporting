#!/usr/bin/env python3
"""
Format all company JSON files consistently (sorted keys, 2-space indent).
"""

import json
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent.parent
MANIFEST_DIR = PROJECT_ROOT / "data" / "companies"

def sort_dict_recursive(d):
    """Recursively sort dictionary keys."""
    if isinstance(d, dict):
        return {k: sort_dict_recursive(v) for k, v in sorted(d.items())}
    if isinstance(d, list):
        return [sort_dict_recursive(i) for i in d]
    return d

def format_all():
    """Format all company files."""
    json_files = [
        f for f in MANIFEST_DIR.rglob('*.json')
        if not f.name.startswith('.') and f.name != 'schema.json'
    ]
    
    print(f"Formatting {len(json_files)} company files...")
    
    for file in json_files:
        with open(file, 'r', encoding='utf-8') as f:
            company = json.load(f)
        
        # Recursively sort keys
        sorted_company = sort_dict_recursive(company)
        
        with open(file, 'w', encoding='utf-8') as f:
            json.dump(sorted_company, f, indent=2, ensure_ascii=False)
            f.write('\n')
    
    print(f"\n✅ Recursively formatted {len(json_files)} files")

if __name__ == "__main__":
    format_all()
