# Documentation Index

**Last Updated:** March 10, 2026

---

## Quick Start

**For New Contributors:**
1. Start with [`../README.md`](../README.md) - Project overview
2. Read [`architecture/ADR-001-company-manifest-ats-integration.md`](architecture/ADR-001-company-manifest-ats-integration.md) - System architecture
3. Follow [`../.github/CONTRIBUTING.md`](../.github/CONTRIBUTING.md) - Contribution guide

**For Developers:**
- [`todo_implementation_plan.md`](todo_implementation_plan.md) - Strategic roadmap
- [`data/company-data-strategy.md`](data/company-data-strategy.md) - Company data strategy
- [`data/ats/ats-identification-plan.md`](data/ats/ats-identification-plan.md) - ATS integration strategy

---

## Documentation Structure

### Architecture Decision Records (`architecture/`)
**Purpose:** Record architectural decisions, context, and rationale.

| Document | Description |
|----------|-------------|
| [ADR-001](architecture/ADR-001-company-manifest-ats-integration.md) | **Company Manifest & ATS Integration** - Master ADR covering directory-based manifest, declarative ATS config, validation system |

### Implementation Plans (`implementation-plans/`)
**Purpose:** Detailed implementation guides for upcoming features.

| Plan | Priority | Status |
|------|----------|--------|
| [2.1 Salary Normalization](implementation-plans/2.1-salary-normalization-plan.md) | HIGH | ✅ Complete |
| [2.2 Cloud Tasks](implementation-plans/2.2-cloud-tasks-plan.md) | HIGH | ✅ Complete |
| [2.3 Dead Link Detection](implementation-plans/2.3-dead-link-detection-plan.md) | HIGH | ⏳ Pending |
| [2.4 ATS Integrations](implementation-plans/2.4-ats-integrations-plan.md) | HIGH | ⏳ Pending |
| [3.1 SEEK & TradeMe](implementation-plans/3.1-seek-trademe-integration-plan.md) | HIGH | ⏳ Pending |
| [3.2 Domain Hubs](implementation-plans/3.2-technology-domain-hubs-plan.md) | HIGH | ⏳ Pending |
| [4.1 Mobile UX](implementation-plans/4.1-mobile-ux-overhaul-plan.md) | HIGH | ⏳ Pending |
| [5.1 Directory Manifest](implementation-plans/5.1-directory-based-manifest-migration.md) | MEDIUM | ✅ Complete |
| [5.2 Validation System](implementation-plans/5.2-company-manifest-validation.md) | HIGH | ✅ Complete |

### Phase 2 Completed Features (`phase2/`)
**Purpose:** Technical specifications for completed features.

| Feature | Description |
|---------|-------------|
| [2.1 Salary Normalization](phase2/2.1-salary-normalization.md) | Multi-currency salary parsing |
| [2.2 Background Processing](phase2/2.2-background-processing.md) | Cloud Tasks integration |
| [2.3 Dead Link Detection](phase2/2.3-dead-link-detection.md) | Ghost job elimination |
| [2.4 ATS Integrations](phase2/2.4-ats-integrations.md) | Direct ATS API integration |
| [2.5 Visa Sponsorship](phase2/2.5-visa-sponsorship.md) | Visa sponsorship tracking |

### Data Strategy (`data/`)
**Purpose:** Data pipeline, ingestion, and quality strategies.

| Document | Description |
|----------|-------------|
| [company-data-strategy.md](data/company-data-strategy.md) | Company manifest system |
| [ats-identification-plan.md](data/ats/ats-identification-plan.md) | ATS identification & integration |
| [ats-identification-findings.md](data/ats/ats-identification-findings.md) | ATS identification results |
| [job-data-strategy.md](data/job-data-strategy.md) | Multi-channel job sourcing |
| [data-pipeline.md](data/data-pipeline.md) | Data pipeline flow |

### Features (`features/`)
**Purpose:** Feature specifications and enhancement plans.

| Feature | Description |
|---------|-------------|
| [Technology Grouping](features/technology-grouping-plan.md) | Tech categorization |
| [Domain Hubs](features/hubs-and-career-stages.md) | Career stage hubs |
| [Company Tech Stack](features/company-tech-stack-fix.md) | Tech stack aggregation |

### UI (`ui/`)
**Purpose:** UI/UX specifications.

| Document | Description |
|----------|-------------|
| [Dark Theme](ui/dark-theme.md) | Dark theme implementation |
| [Tech Icon Tinting](ui/tech-icon-tinting.md) | Brand color icons |

---

## Key Scripts

### Company Management
```bash
# Validate all company files
python3 scripts/companies/validate_all.py

# Check for duplicates
python3 scripts/companies/check_duplicates.py

# Generate new company template
python3 scripts/companies/generate_template.py "Company Name"

# Extract missing companies from DB
python3 scripts/companies/extract_missing_from_db.py

# AI enrichment
export GEMINI_API_KEY="your-key"
python3 scripts/companies/enrich_unverified_companies.py
```

### ATS Management
```bash
# Validate ATS configs
python3 scripts/ats/validate_ats_configs.py

# Discover ATS for companies
python3 scripts/ats/discover_missing_ats.py --limit=50
```

### Cleanup
```bash
# Remove obsolete scripts
python3 scripts/cleanup_obsolete.py --dry-run
python3 scripts/cleanup_obsolete.py
```

---

## Decision Log

### March 10, 2026
- ✅ Company manifest migration complete (108 files)
- ✅ ATS migration complete (60 configs)
- ✅ Documentation consolidated into ADR-001
- ✅ Obsolete scripts identified for removal

### February 2026
- ✅ Directory-based manifest implemented
- ✅ Validation system implemented
- ✅ Backend ATS sync implemented

---

## Getting Help

- **General questions:** Open a GitHub issue
- **Documentation issues:** Edit the relevant `.md` file and submit a PR
- **Architecture questions:** See ADR-001

---

**Status:** ✅ **Production Ready**  
**Next Review:** Q3 2026
