# Company Manifest

This directory contains company data for DevAssembly.

## Structure

Each company has its own JSON file:

```
data/companies/
├── schema.json          # JSON Schema (validation rules)
├── xero.json           # Company data
├── canva.json
└── ...
```

## Validation

### Automatic Validation

- **Pre-commit hook**: Validates on every commit
- **GitHub Actions**: Validates on every PR
- **CI check**: Blocks invalid PRs

### Manual Validation

```bash
# Install dependencies
pip install jsonschema

# Validate all files
python3 scripts/companies/validate_all.py

# Fix formatting automatically
python3 scripts/companies/format_all.py

# Check for duplicates
python3 scripts/companies/check_duplicates.py

# Validate ATS configs (if added)
python3 scripts/ats/validate_ats_configs.py
```

## Contributing

See [CONTRIBUTING.md](../../.github/CONTRIBUTING.md) for contribution guidelines.

## Schema

All files must conform to the JSON schema in `schema.json`.

### Required Fields

- `id`: Unique company identifier (slug) - **must match filename**
- `name`: Official company name
- `verification_level`: Data verification status
- `hq_country`: Headquarters country (ISO code)
- `updated_at`: Last update timestamp (ISO 8601)

### Optional Fields

- `alternateNames`: Alternative names for matching
- `description`: Company description
- `website`: Company website
- `logoUrl`: Logo URL
- `industries`: Industry categories
- `company_type`: Type of company
- `is_agency`: Whether recruitment agency
- `is_social_enterprise`: Social enterprise status
- `operating_countries`: Countries of operation
- `office_locations`: Office cities
- `remote_policy`: Remote work policy
- `visa_sponsorship`: Visa sponsorship available
- `employees_count`: Number of employees
- `ats`: ATS integration configuration

### ATS Configuration

For companies with known ATS systems:

```json
{
  "ats": {
    "provider": "GREENHOUSE",
    "identifier": "xero"
  }
}
```

**Valid providers:**
- `GREENHOUSE` - Public API, auto-validated
- `LEVER` - Public API, auto-validated
- `ASHBY` - Public API, auto-validated
- `WORKDAY` - Enterprise ATS
- `SNAPHIRE` - NZ provider
- `BAMBOOHR` - SMB ATS
- `TEAMTAILOR` - European ATS
- `WORKABLE` - SMB ATS
- `SMARTRECRUITERS` - Enterprise ATS
- `JOBADDER` - AU/NZ provider (OAuth required)
- `EMPLOYMENT_HERO` - AU/NZ provider (OAuth required)
- `SUCCESSFACTORS` - Enterprise ATS

## Verification Levels

- **verified**: Manually reviewed by core team
- **community_verified**: Validated by trusted community member
- **silver**: AI-enriched data
- **unverified**: Automatically discovered
- **needs_review**: Flagged for manual audit
- **blocked**: Spam/scam (requires `blocked_reason`)

## Tools

### Generate Template

```bash
python3 scripts/companies/generate_template.py "Company Name"
```

### Validate

```bash
python3 scripts/companies/validate_all.py
```

### Format

```bash
python3 scripts/companies/format_all.py
```

### Check Duplicates

```bash
python3 scripts/companies/check_duplicates.py
```

### Validate ATS Configs

```bash
python3 scripts/ats/validate_ats_configs.py
```

## Questions?

Open an issue or discussion for help.
