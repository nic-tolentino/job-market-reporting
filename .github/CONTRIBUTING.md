# Contributing to Company Data

Thank you for helping improve DevAssembly's company database!

## Quick Start

1. **Fork the repository**
2. **Create a new branch**: `git checkout -b add-company-xero`
3. **Add or edit company file**: `data/companies/xero.json`
4. **Validate your changes**: `python3 scripts/companies/validate_all.py`
5. **Commit and push**: `git commit -m "Add Xero company data"`
6. **Open a Pull Request**

## Company File Structure

Each company has its own JSON file: `data/companies/{company-id}.json`

### Required Fields

```json
{
  "id": "company-slug",
  "name": "Company Name",
  "verification_level": "unverified",
  "hq_country": "NZ",
  "updated_at": "2026-03-10T00:00:00Z"
}
```

### Optional Fields

```json
{
  "alternateNames": ["Alt Name 1", "Alt Name 2"],
  "description": "Brief description",
  "website": "https://example.com",
  "logoUrl": "https://example.com/logo.png",
  "industries": ["Fintech", "SaaS"],
  "company_type": "Product",
  "is_agency": false,
  "is_social_enterprise": false,
  "hq_country": "NZ",
  "operating_countries": ["NZ", "AU"],
  "office_locations": ["Auckland", "Wellington"],
  "remote_policy": "Hybrid",
  "visa_sponsorship": true,
  "employees_count": 500
}
```

### ATS Configuration (Optional)

For companies with known ATS systems:

```json
{
  "ats": {
    "provider": "GREENHOUSE",
    "identifier": "xero"
  }
}
```

Valid providers: `GREENHOUSE`, `LEVER`, `ASHBY`, `WORKDAY`, `SNAPHIRE`, `BAMBOOHR`, `TEAMTAILOR`, `WORKABLE`, `SMARTRECRUITERS`, `JOBADDER`, `EMPLOYMENT_HERO`, `SUCCESSFACTORS`

### Field Guidelines

- **id**: Lowercase slug (e.g., `xero`, `rocket-lab`). Must match filename exactly.
- **name**: Official company name
- **hq_country**: 2-letter ISO code (e.g., `NZ`, `AU`, `US`, `GB`)
- **verification_level**: One of: `verified`, `community_verified`, `silver`, `unverified`, `needs_review`, `blocked`
- **website**: Must use HTTPS
- **industries**: Use common industry terms (Fintech, Healthtech, SaaS, etc.)
- **company_type**: One of: `Product`, `Agency`, `Consultancy`, `Government`, `Non-profit`, `Startup`, `Enterprise`, `Platform`, `Unknown`
- **remote_policy**: One of: `Remote Only`, `Remote`, `Hybrid`, `On-site`, `Flexible`, `Unknown`

## Validation

Before submitting your PR, run validation:

```bash
# Install dependencies
pip install jsonschema

# Validate all files
python3 scripts/companies/validate_all.py

# Auto-fix formatting
python3 scripts/companies/format_all.py

# Validate ATS configs (if added)
python3 scripts/ats/validate_ats_configs.py
```

## Common Issues

### ❌ Invalid country code
```json
"hq_country": "New Zealand"  // Wrong
"hq_country": "NZ"           // Correct (ISO 3166-1 alpha-2)
```

### ❌ Invalid company ID
```json
"id": "Xero"              // Wrong (uppercase)
"id": "xero-inc"          // Wrong (doesn't match filename)
"id": "xero"              // Correct (lowercase, matches filename)
```

### ❌ Filename doesn't match ID
```
File: xero.json
{
  "id": "xero-inc"  // Wrong - must match filename
}
```

### ❌ Missing trailing newline
Make sure your file ends with a newline character.

### ❌ Incorrect indentation
Use 2 spaces for indentation, not tabs.

### ❌ Invalid ATS provider
```json
"ats": {
  "provider": "Greenhouse",  // Wrong (must be uppercase)
  "identifier": "xero"
}
```

```json
"ats": {
  "provider": "GREENHOUSE",  // Correct
  "identifier": "xero"
}
```

## Adding a New Company

### Option 1: Manual Creation

1. Create file: `data/companies/company-name.json`
2. Use this template:

```json
{
  "id": "company-name",
  "name": "Company Name",
  "alternateNames": [],
  "description": "Brief description of what the company does.",
  "website": "https://company.com",
  "logoUrl": "",
  "industries": ["Industry1", "Industry2"],
  "company_type": "Product",
  "is_agency": false,
  "is_social_enterprise": false,
  "hq_country": "NZ",
  "operating_countries": ["NZ"],
  "office_locations": ["City"],
  "remote_policy": "Hybrid",
  "visa_sponsorship": null,
  "employees_count": null,
  "verification_level": "unverified",
  "updated_at": "2026-03-10T00:00:00Z"
}
```

3. Validate: `python3 scripts/companies/validate_all.py`
4. Commit and submit PR

### Option 2: Use Template Generator

```bash
python3 scripts/companies/generate_template.py "Company Name"
```

This creates a pre-filled template file for you to edit.

## Updating an Existing Company

1. Find the file: `data/companies/{company-id}.json`
2. Edit the fields you want to update
3. Validate: `python3 scripts/companies/validate_all.py`
4. Commit with descriptive message: `Update Xero company information`

## Review Process

1. **Automated validation** runs on your PR (GitHub Actions)
2. **Maintainer review** within 48 hours
3. **If approved**, PR is merged
4. **Changes go live** in next production sync

## Questions?

Open an issue if you have questions or need help.

---

**Thank you for contributing!** 🚀
