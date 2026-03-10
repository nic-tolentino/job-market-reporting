# Contributing to Job Market Reporting

This project is optimized for a hybrid workflow between human developers and AI agents (like Antigravity/Qwen). To maintain the integrity of our data and the performance of our repository, please follow these guidelines.

## 🤖 AI Alignment Guidelines

If you are an AI agent working in this repository:

### 1. Master Manifest Management
- **Directory Structure**: All company manifests are stored in `data/companies/[a-z0-9]/`. Never add files to the root of `data/companies/`.
- **Validation**: Any change to a manifest MUST be verified by running `python3 scripts/companies/validate_all.py`.
- **ID Trimming**: Keep company IDs concise (e.g., `mbie` instead of `ministry-of-business-innovation-and-employment`).
- **Logo Harvesting**: Use `scripts/companies/harvest_logos.py` to pull logo URLs from BigQuery rather than hardcoding external links.

### 2. BigQuery Integration
- **Credentials**: Ensure `GOOGLE_APPLICATION_CREDENTIALS` is set.
- **Project IDs**: Use `tech-market-insights` as the project ID and `techmarket` as the dataset ID.
- **SQL Best Practices**: Use standard SQL and proper date intervals (e.g., `DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)`).

### 3. Backend Ingestion
- **Recursive Sync**: The `CompanySyncService` scans `data/companies/` recursively. No additional configuration is needed when adding new alphabetized subfolders.
- **Unit Tests**: If you modify the sync logic, update `CompanySyncServiceTest.kt` to ensure recursive scanning remains intact.

## 🧑‍💻 Human Developer Guidelines

- **Review Policy**: AI-generated manifests should be spot-checked for high-level accuracy (website, name, industries).
- **Batch Processing**: When using AI to enrich the backlog, use batch processing (50-100 at a time) to avoid context window overflows.

## 🚀 Deployment

Deployment is managed via scripts in `scripts/deployment/`.
- `deploy-local.sh`: For local development inside Docker.
- `deploy.sh`: For production deployments.

---
*This file was generated with the help of Antigravity to ensure consistent AI-human collaboration.*
