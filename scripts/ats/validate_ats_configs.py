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
from typing import List, Optional, Tuple
import requests

# Paths
PROJECT_ROOT = Path(__file__).parent.parent.parent
MANIFEST_DIR = PROJECT_ROOT / "data" / "companies"

# Simple GET-based validators: 200 = valid, 404 = invalid board
# These all use {id} substitution in the URL.
PROVIDER_GET_ENDPOINTS = {
    'GREENHOUSE': 'https://boards-api.greenhouse.io/v1/boards/{id}/jobs',
    'LEVER': 'https://api.lever.co/v0/postings/{id}?limit=1',
    'ASHBY': 'https://api.ashbyhq.com/posting-api/job-board/{id}',
}

VALID_PROVIDERS = [
    # Tier 1 — directly integrated (public APIs)
    'GREENHOUSE',
    'LEVER',
    'ASHBY',
    'SMARTRECRUITERS',
    'TEAMTAILOR',
    'WORKABLE',
    # Tier 2 — no public job-list API (Workday SSO, BambooHR internal, etc.)
    'WORKDAY',
    'SNAPHIRE',
    'BAMBOOHR',
    'JOBADDER',
    'EMPLOYMENT_HERO',
    'SUCCESSFACTORS',
    'FACTORIAL',
    'FACTORIALHR',      # alias observed in data — treated same as FACTORIAL
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
    'ZOHO',
    'PAGEUP',           # used by AU/NZ universities / government
    'JOBVITE',
    'RIPPLING',
    'INRECRUITING',
    'ORACLE_TALEO',
    # Catch-all values — company has a custom career page or ATS is unidentified
    'CUSTOM',
    'OTHER',
]

# Providers with public APIs we can validate against (superset of GET_ENDPOINTS)
VALIDATABLE_PROVIDERS = set(PROVIDER_GET_ENDPOINTS.keys()) | {
    'SMARTRECRUITERS', 'TEAMTAILOR', 'WORKABLE'
}

# Providers that use a catch-all value with no standard identifier to validate
NO_IDENTIFIER_PROVIDERS = {'CUSTOM', 'OTHER'}


def _get(url: str, timeout: int = 15, allow_redirects: bool = True, **kwargs) -> Tuple[Optional[requests.Response], Optional[Exception]]:
    """Perform a GET with one retry on timeout. Returns (response, last_exc)."""
    last_exc = None
    for _ in range(2):
        try:
            return requests.get(url, timeout=timeout, allow_redirects=allow_redirects, **kwargs), None
        except requests.Timeout as e:
            last_exc = e
        except requests.RequestException as e:
            return None, e  # Non-timeout won't improve on retry
    return None, last_exc


def _validate_smartrecruiters(identifier: str, company_file_name: str) -> Optional[str]:
    """
    SmartRecruiters validation via redirect check.

    The postings API (api.smartrecruiters.com/v1/companies/{slug}/postings) returns HTTP 200
    for ANY slug, including non-existent ones — it's a false-positive trap.

    Instead: GET jobs.smartrecruiters.com/{slug} with redirect following.
    - Real board  → redirects to careers.smartrecruiters.com/{slug}
    - Fake slug   → redirects to careers.smartrecruiters.com root (no slug path)
    """
    board_url = f"https://jobs.smartrecruiters.com/{identifier}"
    resp, exc = _get(board_url, timeout=15, allow_redirects=True)

    if exc is not None:
        print(f"  ⚠️  {company_file_name}: Timeout verifying SMARTRECRUITERS '{identifier}' (skipped)")
        return None

    final_url = resp.url
    # A real board lands at careers.smartrecruiters.com/{slug}/...
    # A fake slug lands at careers.smartrecruiters.com with no meaningful path
    path = final_url.rstrip('/').split('careers.smartrecruiters.com', 1)[-1].strip('/')
    if 'careers.smartrecruiters.com' not in final_url or not path:
        return f"{company_file_name}: Invalid SMARTRECRUITERS identifier '{identifier}' (no board found at redirect destination)"

    return None  # valid


def _validate_teamtailor(identifier: str, company_file_name: str) -> Optional[str]:
    """
    TeamTailor validation via subdomain JSON feed.

    GET https://{identifier}.teamtailor.com/feed/jobs.json
    - 200 + JSON array → valid board
    - Connection error / non-200 → invalid or no board set up
    """
    feed_url = f"https://{identifier}.teamtailor.com/feed/jobs.json"
    resp, exc = _get(feed_url, timeout=15)

    if exc is not None:
        if isinstance(exc, requests.Timeout):
            print(f"  ⚠️  {company_file_name}: Timeout verifying TEAMTAILOR '{identifier}' (skipped)")
            return None
        # DNS resolution failure = subdomain definitively does not exist
        if 'NameResolutionError' in str(exc) or 'Failed to resolve' in str(exc):
            return f"{company_file_name}: Invalid TEAMTAILOR identifier '{identifier}' (subdomain does not exist)"
        return f"{company_file_name}: Invalid TEAMTAILOR identifier '{identifier}' (connection error: {exc})"

    if resp.status_code == 404:
        return f"{company_file_name}: Invalid TEAMTAILOR identifier '{identifier}' (404 — subdomain not found)"
    if resp.status_code != 200:
        return f"{company_file_name}: TEAMTAILOR API error for '{identifier}': HTTP {resp.status_code}"

    try:
        data = resp.json()
        if isinstance(data, list) and len(data) == 0:
            print(f"  ⚠️  {company_file_name}: TEAMTAILOR board valid but has no open jobs")
    except Exception:
        pass  # Not critical — empty or non-JSON responses can occur for boards with zero jobs

    return None  # valid


def _validate_workable(identifier: str, company_file_name: str) -> Optional[str]:
    """
    Workable validation via the public jobs POST endpoint.

    POST https://apply.workable.com/api/v3/accounts/{slug}/jobs
    - 200 → valid account slug
    - 404 → slug does not exist
    """
    url = f"https://apply.workable.com/api/v3/accounts/{identifier}/jobs"
    body = {"query": "", "location": [], "department": [], "worktype": [], "remote": []}

    try:
        resp = requests.post(url, json=body, timeout=15)
    except requests.Timeout:
        print(f"  ⚠️  {company_file_name}: Timeout verifying WORKABLE '{identifier}' (skipped)")
        return None
    except requests.RequestException as e:
        return f"{company_file_name}: WORKABLE connection error for '{identifier}': {e}"

    if resp.status_code == 404:
        return f"{company_file_name}: Invalid WORKABLE identifier '{identifier}' (404 — account not found)"
    if resp.status_code == 429:
        print(f"  ⚠️  {company_file_name}: WORKABLE rate-limited for '{identifier}' — board likely valid (skipped)")
        return None
    if resp.status_code != 200:
        return f"{company_file_name}: WORKABLE API error for '{identifier}': HTTP {resp.status_code}"

    try:
        data = resp.json()
        if data.get('total', -1) == 0:
            print(f"  ⚠️  {company_file_name}: WORKABLE account valid but has no open jobs")
    except Exception:
        pass

    return None  # valid


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

    # CUSTOM / OTHER: no identifier needed — company has its own career page
    if provider in NO_IDENTIFIER_PROVIDERS:
        return errors

    # Validate identifier format
    identifier = ats.get('identifier')
    if not identifier:
        errors.append(f"{company_file.name}: Missing 'identifier' field")
    elif len(identifier) > 100:
        errors.append(f"{company_file.name}: Identifier too long (max 100 chars)")
    elif provider in ('ASHBY', 'SMARTRECRUITERS'):
        # These slugs are case-sensitive and can contain mixed case, spaces, dots.
        # (e.g. Ashby: "Checkbox Technology", "leonardo.ai"; SR: "CarSales", "carsales")
        # Only reject if empty — format is validated by the live API check below.
        pass
    elif not identifier.replace('-', '').replace('_', '').replace('.', '').isalnum():
        errors.append(f"{company_file.name}: Invalid identifier format '{identifier}' (alphanumeric, hyphens, underscores, dots only)")

    if not identifier or not provider or provider not in VALIDATABLE_PROVIDERS:
        return errors

    # ── Live API validation ───────────────────────────────────────────────────

    if provider in PROVIDER_GET_ENDPOINTS:
        from urllib.parse import quote
        url = PROVIDER_GET_ENDPOINTS[provider].format(id=quote(identifier, safe='-_.'))
        resp, exc = _get(url, timeout=15)

        if exc is not None:
            print(f"  ⚠️  {company_file.name}: Timeout verifying {provider} '{identifier}' (skipped)")
        elif resp.status_code == 404:
            if provider == 'ASHBY':
                # Ashby returns 404 when the board exists but has zero open jobs.
                # Confirm by checking the public job board page.
                page_url = f"https://jobs.ashbyhq.com/{requests.utils.quote(identifier, safe='')}"
                page_r, _ = _get(page_url, timeout=10)
                if page_r and page_r.status_code == 200:
                    print(f"  ⚠️  {company_file.name}: ASHBY board valid but has no open jobs (API 404)")
                else:
                    status = page_r.status_code if page_r else 'timeout'
                    errors.append(f"{company_file.name}: Invalid ASHBY identifier '{identifier}' (API 404, board page also {status})")
            else:
                errors.append(f"{company_file.name}: Invalid {provider} identifier '{identifier}' (404 Not Found)")
        elif resp.status_code != 200:
            errors.append(f"{company_file.name}: API error for {provider} '{identifier}': HTTP {resp.status_code}")
        else:
            try:
                data = resp.json()
                if 'jobs' in data and len(data['jobs']) == 0:
                    print(f"  ⚠️  {company_file.name}: {provider} identifier valid but no jobs returned")
            except Exception:
                pass

    elif provider == 'SMARTRECRUITERS':
        err = _validate_smartrecruiters(identifier, company_file.name)
        if err:
            errors.append(err)

    elif provider == 'TEAMTAILOR':
        err = _validate_teamtailor(identifier, company_file.name)
        if err:
            errors.append(err)

    elif provider == 'WORKABLE':
        err = _validate_workable(identifier, company_file.name)
        if err:
            errors.append(err)

    return errors


def validate_all():
    """Validate all company files with ATS configs."""
    print("🔍 Validating ATS configurations in company manifest files...\n")

    if not MANIFEST_DIR.exists():
        print(f"❌ Manifest directory not found: {MANIFEST_DIR}")
        return 1

    json_files = sorted([
        f for f in MANIFEST_DIR.rglob('*.json')
        if not f.name.startswith('.') and f.name != 'schema.json'
    ])

    if not json_files:
        print(f"❌ No company files found in {MANIFEST_DIR}")
        return 1

    print(f"Found {len(json_files)} company files\n")

    all_errors = []
    files_with_ats = 0

    for file in json_files:
        errors = validate_ats_config(file)

        try:
            with open(file, 'r') as f:
                company = json.load(f)
                if company.get('ats'):
                    files_with_ats += 1
        except Exception:
            pass

        if errors:
            all_errors.extend(errors)
            print(f"❌ {file.name}:")
            for error in errors:
                print(f"   - {error.split(': ', 1)[-1] if ': ' in error else error}")

    print(f"\n{'='*60}")
    print(f"ATS Configuration Validation Results")
    print(f"{'='*60}")
    print(f"Files validated:      {len(json_files)}")
    print(f"Files with ATS config: {files_with_ats}")
    print(f"Errors found:         {len(all_errors)}")

    if all_errors:
        print(f"\n❌ Validation FAILED")
        print(f"\nPlease fix the errors above and re-run validation.")
        return 1
    else:
        print(f"\n✅ All ATS configurations are valid!")
        return 0


if __name__ == "__main__":
    sys.exit(validate_all())
