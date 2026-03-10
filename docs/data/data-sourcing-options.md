# Data Sourcing Options & Cost Analysis

**Last Updated:** March 10, 2026  
**Status:** Active Decision Document

---

## Executive Summary

This document outlines the available data sourcing options for DevAssembly's job aggregation platform, with a focus on cost-effectiveness during the pre-revenue/bootstrapping phase.

**Current Situation:**
- Apify's cheapest plan: **$35/month** (subscription commitment)
- Estimated multi-country scraping cost: **$36–$48/month** ($3–$4 per country × 3 countries × 4 weeks)
- Direct ATS scraping covers only **~50% of companies**
- LinkedIn and job board aggregation remains essential for market completeness

---

## Option Comparison Matrix

| Option | Cost Model | Monthly Estimate | Pros | Cons | Best For |
|--------|-----------|------------------|------|------|----------|
| **Apify (Current)** | Subscription | $35–$48 | Easy setup, maintained scrapers, proxy rotation | Fixed monthly cost, pays even when idle | Primary scraping infrastructure |
| **Bright Data** | Pay-per-success | $5–$15 | No commitment, pay only for results, cheaper at low volume | Higher per-request cost, less turnkey | Bootstrapping, intermittent scraping |
| **TheirStack** | Subscription | $59+ | Aggregates 315K+ sources, deduplication built-in | More expensive, less control | Teams prioritizing dev time over cost |
| **Direct ATS APIs** | Free | $0 | Real-time, no anti-bot concerns, highest quality | Only covers ~50% of companies | Tier 1 sourcing (identified companies) |
| **Custom Scrapers** | Proxy costs only | $10–$20 | Full control, lowest marginal cost | High maintenance, constant arms race | Niche sources (gov portals) |
| **Hybrid Approach** | Mixed | $15–$25 | Optimizes cost vs. coverage | More complex architecture | **Recommended for bootstrapping** |

---

## Detailed Option Analysis

### 1. Apify (Current Setup)

**Pricing:**
- Starter Plan: $35/month (includes platform credits)
- Additional compute: Pay-as-you-go
- Estimated 3-country weekly scraping: $36–$48/month

**Advantages:**
- Pre-built, well-maintained scrapers for LinkedIn, SEEK, Indeed
- Automatic proxy rotation and anti-bot handling
- Reliable uptime and error handling
- Easy scheduling and API integration

**Disadvantages:**
- **Fixed subscription cost** — you pay even if you don't run scrapers
- Cost scales linearly with frequency (weekly runs × 3 countries = $36–$48/month)
- Vendor lock-in for scraper logic

**Verdict:** Good for production, expensive for bootstrapping. Consider downgrading frequency or switching to pay-as-you-go during pre-revenue phase.

---

### 2. Bright Data (Pay-Per-Success)

**Pricing:**
- **~$1.50 per 1,000 successful requests**
- No monthly commitment
- Estimated monthly cost (3 countries, weekly): **$5–$15**

**Advantages:**
- **Pay only when you get data** — if you skip a week, you pay $0
- No subscription lock-in
- LinkedIn Scraper API specifically optimized
- Handles proxy rotation and CAPTCHAs

**Disadvantages:**
- Higher per-request cost than Apify at scale
- Less turnkey — more integration work required
- Fewer pre-built scrapers for niche sources (SEEK, Trade Me, gov portals)

**Verdict:** **Best for bootstrapping.** At early-stage volumes, could save $20–$30/month vs. Apify. Ideal while you're not running scrapers consistently.

**Migration Path:**
```
1. Test Bright Data LinkedIn API with 1 country
2. Compare success rate and data quality vs. Apify
3. If acceptable, migrate country-by-country
4. Keep Apify as fallback for non-LinkedIn sources (SEEK, Trade Me)
```

---

### 3. TheirStack (Aggregator)

**Pricing:**
- Starts at **$59/month**
- Access to 315,000+ sources (LinkedIn, Indeed, Greenhouse, Lever, etc.)

**Advantages:**
- **Single API** for all sources — no scraper maintenance
- Built-in deduplication and normalization
- Covers ATS platforms + job boards + LinkedIn in one call
- Saves ~10 hours/month of scraper maintenance

**Disadvantages:**
- **More expensive** than Apify ($59 vs. $35)
- Less control over data freshness and scraping frequency
- Vendor lock-in at a higher price point

**Verdict:** Only worthwhile if scraper maintenance is consuming >10 hours/month. Not cost-effective for bootstrapping.

---

### 4. Direct ATS APIs (Free Tier)

**Pricing:**
- **$0** — public APIs are free to access

**Coverage:**
- ~50% of companies use Greenhouse, Lever, Ashby, or Workday
- Does NOT cover companies using proprietary ATS or job-board-only posting

**Advantages:**
- **Zero cost**
- Real-time data (no scraping delays)
- No anti-bot concerns or proxy costs
- Highest data quality (structured JSON, not HTML)

**Disadvantages:**
- **Limited coverage** — misses ~50% of companies
- Requires company identification first (need to know which ATS each company uses)
- API schemas vary by provider

**Verdict:** **Essential foundation.** Should be Tier 1 sourcing regardless of other options. Combine with LinkedIn/SEEK scraping for full coverage.

**Implementation Status:** See `docs/data/job-data-strategy.md` — Tier 1 sourcing is already identified as the "Real-Time Moat."

---

### 5. Custom Scrapers (Self-Built)

**Pricing:**
- Proxy costs only: **$10–$20/month** (residential proxies)
- Development time: High (ongoing maintenance)

**Advantages:**
- Lowest marginal cost at scale
- Full control over scraping logic and frequency
- Can target niche sources (gov portals, regional boards)

**Disadvantages:**
- **High maintenance burden** — constant cat-and-mouse with bot protection
- Requires expertise in Crawlee/Playwright, proxy rotation, CAPTCHA solving
- Easy to break when target sites change DOM structure

**Verdict:** Only build custom scrapers for sources without commercial alternatives (e.g., Jobs.govt.nz, APSJobs). Use Apify Store or Bright Data for LinkedIn/SEEK.

---

### 6. Hybrid Approach (Recommended)

**Strategy:** Combine free ATS APIs with pay-as-you-go scraping for maximum coverage at minimum cost.

**Proposed Architecture:**
```
Tier 1: Direct ATS APIs (Free)
  └─ Greenhouse, Lever, Ashby, Workday
  └─ Coverage: ~50% of companies
  └─ Cost: $0/month

Tier 2: Bright Data Pay-Per-Success (LinkedIn)
  └─ 3 countries, weekly scraping
  └─ Coverage: ~30% of companies
  └─ Cost: ~$10–$15/month

Tier 3: Apify Store (SEEK, Trade Me, Gov Portals)
  └─ Run monthly or bi-weekly (not weekly)
  └─ Coverage: ~20% of companies
  └─ Cost: ~$10–$15/month (reduced frequency)

Total Estimated Cost: $20–$30/month (vs. $35–$48 for Apify-only)
```

**Advantages:**
- **Saves $15–$18/month** vs. Apify-only approach
- Maintains full market coverage
- Reduces subscription risk (can pause Bright Data runs if needed)
- Leverages free ATS APIs for highest-quality data

**Disadvantages:**
- More complex architecture (multiple data sources)
- Requires managing 2–3 different API integrations

**Verdict:** **Recommended for bootstrapping phase.** Re-evaluate at $1K+ MRR when subscription simplicity may be worth the premium.

---

## Cost Projection: 12-Month Runway

| Scenario | Monthly Cost | Annual Cost | Notes |
|----------|-------------|-------------|-------|
| **Apify-Only (Current)** | $48 | $576 | 3 countries, weekly scraping |
| **Bright Data Only** | $15 | $180 | Assumes consistent weekly runs |
| **Hybrid (Recommended)** | $25 | $300 | ATS + Bright Data + reduced Apify |
| **TheirStack** | $59 | $708 | Not recommended for bootstrapping |

**Potential Savings (Hybrid vs. Apify-Only): $276/year**

---

## Decision Framework

### Choose Apify If:
- ✅ You value simplicity over cost optimization
- ✅ You're running scrapers consistently (no idle weeks)
- ✅ You have revenue to absorb $35–$48/month fixed costs

### Choose Bright Data If:
- ✅ You're in bootstrapping/pre-revenue phase
- ✅ Your scraping schedule is inconsistent (e.g., moving countries, focused on other features)
- ✅ You only need LinkedIn scraping (not SEEK/Trade Me)

### Choose Hybrid If:
- ✅ You want to minimize costs without sacrificing coverage
- ✅ You're comfortable managing multiple data sources
- ✅ You have ~50% of companies already identified for ATS integration

### Choose TheirStack If:
- ✅ Scraper maintenance is consuming >10 hours/month
- ✅ You have revenue to prioritize dev time over cost
- ✅ You need rapid expansion to new countries/sources

---

## Recommended Action Plan

### Immediate (This Sprint)
1. **Audit current Apify usage** — how many runs per week? What's the actual success rate?
2. **Test Bright Data LinkedIn API** — run a pilot with 1 country, compare data quality
3. **Expand ATS coverage** — identify more companies using Greenhouse/Lever/Ashby from existing LinkedIn data

### Short-Term (Next 2–4 Weeks)
4. **Migrate LinkedIn scraping to Bright Data** — if pilot is successful
5. **Reduce Apify frequency** — run SEEK/Trade Me scrapers bi-weekly instead of weekly
6. **Monitor cost savings** — track actual Bright Data spend vs. projections

### Long-Term (Post-Revenue)
7. **Re-evaluate at $1K+ MRR** — consider consolidating back to Apify or TheirStack if simplicity is worth the premium
8. **Build custom scrapers for niche sources** — only if commercial options don't exist (gov portals, regional boards)

---

## Related Documents

- `job-data-strategy.md` — Comprehensive sourcing strategy and tier model
- `country-data-strategy.md` — Multi-country ingestion architecture
- `ats/` — Direct ATS integration documentation

---

## Notes from Team Discussion

**Gemini's Assessment (March 2026):**
> "USD$35/month is a steep 'subscription tax' when you're still in the pre-revenue/bootstrapping phase of a community project."

**Key Insight:** Bright Data's pay-per-success model could reduce monthly costs from $35 to $5–$10 if scraping frequency is inconsistent.

**Caveat:** TheirStack's $59/month price point is only justified if it saves >10 hours/month of maintenance time. At an effective hourly rate of ~$6/hour, this is a high bar for a pre-revenue project.
