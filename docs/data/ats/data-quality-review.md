# Crawler & ATS Data Quality Review

How to verify raw job data before relying on it for silver-layer ingestion.

**Last updated:** March 2026
**Relates to:** `seed-discovery-pipeline.md`, `discovery-pipeline.md`

---

## Why review raw data first

The crawler extracts jobs from heterogeneous sources — BambooHR SPAs, Workday
tenants, Greenhouse boards, custom career pages. Each source presents data
differently: some list locations per job, others only show titles; some include
posting dates, most don't. Before building silver tables or reporting on job
trends, it's worth knowing which fields are reliably populated for each source
type.

Three categories of quality problem appear in practice:

1. **Extraction gap** — fields the LLM couldn't find in the page (e.g. no
   description because the listing page only has titles; location absent because
   the site uses a filter dropdown that isn't in the HTML).

2. **Validation rejection** — jobs extracted but dropped because enum values
   don't match the allowed set (e.g. "Full Time" instead of "Full-time") or
   required fields are missing.

3. **Enrichment failure** — detail-page descriptions weren't fetched because
   `applyUrl` was missing or cross-origin.

---

## Tiered review approach

### Tier 1 — Automated field coverage matrix (run first, always)

**Script:** `scripts/quality/tier1_coverage_report.py`
**Input:** One or more crawl result JSON files (batch crawl output from the
crawler service) or a directory of per-company JSON files.
**Output:** A table showing per-source-type fill rate for each field.

```
Source type         | title | location | employmentType | workModel | seniorityLevel | applyUrl | postedAt | descriptionText
--------------------+-------+----------+----------------+-----------+----------------+----------+----------+-----------------
BambooHR            | 100%  |   72%    |      45%       |    12%    |      38%       |   89%    |    8%    |      61%
Workday             | 100%  |   95%    |      88%       |    31%    |      52%       |   91%    |   22%    |      78%
Greenhouse (ATS)    | 100%  |   91%    |      82%       |    44%    |      61%       |   98%    |   74%    |      95%
Custom career page  | 100%  |   54%    |      28%       |    18%    |      22%       |   43%    |    5%    |      33%
```

Run this after every batch crawl to catch regressions. A field dropping
significantly between runs indicates a page structure change (ATS update or
CMS migration).

```bash
# After a batch crawl produces output.json:
python3 scripts/quality/tier1_coverage_report.py output.json

# Multiple files:
python3 scripts/quality/tier1_coverage_report.py crawl-results/*.json

# Filter to a single source type:
python3 scripts/quality/tier1_coverage_report.py output.json --source bamboohr
```

---

### Tier 2 — Manual spot-check of raw job objects

Look at 3–5 actual job objects from each source type that showed gaps in Tier 1.
The goal is to distinguish between:

- **Data genuinely absent** — the page doesn't show it (e.g. BambooHR job
  listings often omit location until you click into the detail page).
- **Extraction prompt gap** — the field is on the page but Gemini didn't pick
  it up (e.g. posted date in an unusual format, or location in a tag the prompt
  doesn't mention).
- **Validation rejection** — Gemini extracted the value but the validator
  rejected it (check `extractionStats.jobsRaw` vs `jobsValid` in the crawl
  response).

```bash
# Check raw vs valid counts from a crawl response:
cat output.json | python3 -c "
import json, sys
for r in json.load(sys.stdin):
    s = r.get('extractionStats', {})
    if s.get('jobsRaw', 0) != s.get('jobsValid', 0):
        print(r['companyId'], 'raw:', s['jobsRaw'], 'valid:', s['jobsValid'])
"
```

If `jobsRaw` >> `jobsValid` for a source type, the validator is rejecting jobs
that were successfully extracted. Check the enum normalization (see Known Issues
below) or add logging to `validateJob()`.

---

### Tier 3 — Ground-truth comparison

For sources where Tier 2 reveals persistent gaps, compare against known-good
data for 1–2 specific companies:

1. Manually open the company's career page and note what fields are visible.
2. Run a targeted crawl for that company and dump the raw JSON.
3. Compare: are the fields you saw in the browser present in the JSON?

This confirms whether the gap is an extraction issue (Gemini missed something
visible) or a structural limitation (the field only appears after a JS
interaction the crawler doesn't handle).

Common structural limitations found so far:
- **Location**: Often in a facet panel that doesn't render until a dropdown is
  clicked (Workday, SAP SuccessFactors). The detail page usually has it.
- **Posted date**: Frequently rendered client-side from a relative timestamp
  ("3 days ago") that the extraction prompt doesn't convert to ISO format.
- **Description**: Listing pages show only title + department + location.
  Full JD is on the detail page — requires `followJobLinks: true` and a
  working `applyUrl`.

---

## Known issues by source type

### BambooHR

- Listing pages render via JavaScript (SPA). Playwright renders the shell but
  job data arrives via XHR. Status `empty` in seed verification is expected.
- Jobs on the listing page typically have title + department + location only.
  Descriptions require detail-page fetching.
- Gemini may return `employmentType: "Full Time"` (space, not hyphen). The
  validator rejects this as an invalid enum. Fix: enum normalization in
  `GeminiExtractionService.toNormalizedJob()`.

### Workday

- Full React SPA. `networkidle` timeout alone is insufficient — secondary XHR
  calls load job cards after the shell renders. Fix: Workday-specific selector
  wait on `[data-automation-id="jobItem"]`.
- Tenant URLs vary: `company.wd3.myworkdayjobs.com/Board` — the board path
  must be known (from `discover_seeds_from_apply_urls.py`) for targeted mode.
- Location and posting date are usually available; descriptions are on detail
  pages.

### Greenhouse / Lever / Ashby (direct ATS API)

- Highest data quality — structured API returns all fields consistently.
- Description coverage near 100% because API provides full JD.
- These sources bypass the crawler entirely; this review applies only to
  crawled sources.

### Custom career pages

- Most variable source. Field coverage depends entirely on what the company
  publishes.
- Pages with only a title list (no metadata) will produce jobs with confidence
  just above 0.5 (title only → 0.55). These are valid but sparse.
- If a custom page redirects to an unsupported ATS, the ATS identifier is
  logged in `crawlMeta.detectedAtsProvider` — candidate for ATS config
  addition.

---

## What to do with the findings

| Finding | Action |
|---------|--------|
| `jobsRaw` >> `jobsValid` for a source type | Fix enum normalization or validator; check `extractionStats` |
| Description coverage < 20% on a source | Verify `followJobLinks: true`; check `applyUrl` is same-origin; inspect detail-page logs |
| Location always null for a source type | JD is on detail page — description fetch will include it; or add extraction hint |
| Workday 0 jobs | Playwright selector wait not triggering; check URL pattern match |
| Structural limitation confirmed in Tier 3 | Document it in this file under Known Issues; don't try to fix at extraction level |
