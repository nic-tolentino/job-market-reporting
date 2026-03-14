/**
 * Discovery Probe — Round 2
 *
 * 10 companies chosen to stress-test the discovery phase across a range of conditions:
 *
 * GROUP A — Pure discovery (no seedData). Crawler must navigate from the homepage
 *            to the jobs listing entirely via glob-filtered link-following.
 *            Tests how reliably the discovery globs find a careers page.
 *
 *   1. Xero         — Large (4 500+). Careers lives on subdomain careers.xero.com
 *                     → Discovery WILL fail (same-domain glob can't cross subdomain).
 *                     Baseline for "ATS subdomain" failure mode.
 *   2. MYOB         — Large AU/NZ. /careers on same domain. Likely Workday inside.
 *   3. IAG          — Large AU insurance. /careers on same domain. Likely Workday.
 *   4. Karbon       — Small-medium (200 ppl). AU/NZ SaaS. Simple career page expected.
 *   5. Displayr     — Small (80 ppl). AU analytics. Likely a bare /careers page.
 *
 * GROUP B — Targeted mode (seedData provided). Discovery phase skipped; crawler
 *            jumps straight to the known careers URL.
 *            Tests extraction quality and pagination on known-good URLs.
 *
 *   6. Airwallex    — Large-medium AU fintech. /careers on same domain.
 *   7. Revolut      — Large international fintech. Custom careers page or Greenhouse.
 *   8. Notion       — Medium. International SaaS. Likely Greenhouse/custom.
 *   9. Hnry         — Small NZ fintech (100 ppl). Simple /careers page expected.
 *  10. Leonardo.Ai  — Small-medium AU AI startup (100 ppl). Emerging ATS likely Lever.
 */

import axios from 'axios';

interface CompanyConfig {
  id: string;
  url: string;
  group: 'A-discovery' | 'B-targeted';
  seedData?: {
    url: string;
    category: 'tech-filtered' | 'general' | 'careers' | 'homepage' | 'unknown';
  };
  notes: string;
}

const companies: CompanyConfig[] = [
  // ── GROUP A: Pure discovery ───────────────────────────────────────────────
  {
    id: 'xero',
    url: 'https://www.xero.com',
    group: 'A-discovery',
    notes: 'Large; careers on subdomain careers.xero.com — expect discovery to miss it',
  },
  {
    id: 'myob',
    url: 'https://www.myob.com',
    group: 'A-discovery',
    notes: 'Large AU/NZ; /careers on same domain; likely Workday inside',
  },
  {
    id: 'iag',
    url: 'https://www.iag.com.au',
    group: 'A-discovery',
    notes: 'Large AU insurance; /careers on same domain; likely Workday',
  },
  {
    id: 'karbon',
    url: 'https://www.karbonhq.com',
    group: 'A-discovery',
    notes: 'Small-medium (200 ppl); AU/NZ SaaS; simple career page expected',
  },
  {
    id: 'displayr',
    url: 'https://www.displayr.com',
    group: 'A-discovery',
    notes: 'Small (80 ppl); AU analytics startup; bare /careers page expected',
  },

  // ── GROUP B: Targeted mode ────────────────────────────────────────────────
  {
    id: 'airwallex',
    url: 'https://www.airwallex.com',
    group: 'B-targeted',
    seedData: { url: 'https://www.airwallex.com/careers', category: 'general' },
    notes: 'Large-medium AU fintech; /careers same domain',
  },
  {
    id: 'revolut',
    url: 'https://www.revolut.com',
    group: 'B-targeted',
    seedData: { url: 'https://www.revolut.com/careers', category: 'general' },
    notes: 'Large international fintech; custom careers or Greenhouse',
  },
  {
    id: 'notion',
    url: 'https://www.notion.so',
    group: 'B-targeted',
    seedData: { url: 'https://www.notion.so/careers', category: 'general' },
    notes: 'Medium international SaaS; likely Greenhouse or custom',
  },
  {
    id: 'hnry',
    url: 'https://www.hnry.co.nz',
    group: 'B-targeted',
    seedData: { url: 'https://www.hnry.co.nz/careers', category: 'general' },
    notes: 'Small NZ fintech (100 ppl); simple /careers page expected',
  },
  {
    id: 'leonardo-ai',
    url: 'https://leonardo.ai',
    group: 'B-targeted',
    seedData: { url: 'https://leonardo.ai/careers', category: 'general' },
    notes: 'Small-medium AU AI startup (100 ppl); likely Lever',
  },
];

// ── Helpers ───────────────────────────────────────────────────────────────────

function banner(msg: string) {
  const line = '─'.repeat(70);
  console.log(`\n${line}\n  ${msg}\n${line}`);
}

function formatMs(ms: number): string {
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${ms}ms`;
}

// ── Main ──────────────────────────────────────────────────────────────────────

async function run() {
  const BASE = 'http://localhost:8083';
  const results: Array<{
    id: string;
    group: string;
    notes: string;
    pagesVisited: number;
    jobsFound: number;
    ats: string | null;
    atsId: string | null;
    paginationPattern: string | null;
    confidence: number;
    durationMs: number;
    status: string;
    error?: string;
  }> = [];

  for (const company of companies) {
    const label = `${company.group} | ${company.id}`;
    banner(`Crawling: ${label}`);
    console.log(`  URL   : ${company.url}`);
    console.log(`  Seed  : ${company.seedData?.url ?? '(none — discovery from homepage)'}`);
    console.log(`  Notes : ${company.notes}`);

    const t0 = Date.now();
    try {
      const payload: Record<string, unknown> = {
        companyId: company.id,
        url: company.url,
        crawlConfig: {
          maxPages: company.group === 'A-discovery' ? 10 : 60,
          followJobLinks: true,
          timeout: 60000,
        },
      };

      if (company.seedData) {
        payload.seedData = company.seedData;
      } else {
        // Force discovery mode when no seed
        (payload.crawlConfig as Record<string, unknown>).isDiscoveryMode = true;
      }

      const res = await axios.post(`${BASE}/crawl`, payload, { timeout: 300_000 });
      const { crawlMeta, jobs } = res.data;
      const wallMs = Date.now() - t0;

      console.log('\n  ── crawlMeta ──');
      console.log(JSON.stringify(crawlMeta, null, 4).split('\n').map(l => '  ' + l).join('\n'));
      console.log(`\n  Jobs returned: ${jobs.length}`);
      if (jobs.length > 0) {
        console.log('  Sample titles:');
        jobs.slice(0, 5).forEach((j: { title: string; location?: string }) =>
          console.log(`    • ${j.title}${j.location ? ` — ${j.location}` : ''}`));
      }

      results.push({
        id: company.id,
        group: company.group,
        notes: company.notes,
        pagesVisited: crawlMeta.pagesVisited,
        jobsFound: jobs.length,
        ats: crawlMeta.detectedAtsProvider,
        atsId: crawlMeta.detectedAtsIdentifier,
        paginationPattern: crawlMeta.pagination_pattern ?? null,
        confidence: crawlMeta.extractionConfidence,
        durationMs: crawlMeta.crawlDurationMs ?? wallMs,
        status: crawlMeta.status,
        error: crawlMeta.errorMessage,
      });

    } catch (err: any) {
      const wallMs = Date.now() - t0;
      const msg = err.response?.data?.message ?? err.message;
      console.error(`  ERROR: ${msg}`);
      results.push({
        id: company.id,
        group: company.group,
        notes: company.notes,
        pagesVisited: 0,
        jobsFound: 0,
        ats: null,
        atsId: null,
        paginationPattern: null,
        confidence: 0,
        durationMs: wallMs,
        status: 'FAILED',
        error: msg,
      });
    }

    // Polite delay between companies
    console.log('\n  Waiting 10 s before next company…');
    await new Promise(r => setTimeout(r, 10_000));
  }

  // ── Summary table ─────────────────────────────────────────────────────────
  banner('SUMMARY');
  console.log(
    'Group       | Company       | Pages | Jobs | ATS Detected          | Pagination   | Confidence | Duration | Status'
  );
  console.log('-'.repeat(110));

  for (const r of results) {
    const ats = r.ats ? `${r.ats}${r.atsId ? ` (${r.atsId.substring(0, 15)})` : ''}` : '—';
    const pag = r.paginationPattern ?? '—';
    console.log(
      `${r.group.padEnd(11)} | ${r.id.padEnd(13)} | ${String(r.pagesVisited).padStart(5)} | ${String(r.jobsFound).padStart(4)} | ${ats.padEnd(21)} | ${pag.padEnd(12)} | ${(r.confidence * 100).toFixed(0).padStart(7)}%   | ${formatMs(r.durationMs).padStart(8)} | ${r.status}${r.error ? ` — ${r.error.substring(0, 60)}` : ''}`
    );
  }

  // ── Discovery analysis ────────────────────────────────────────────────────
  banner('DISCOVERY MODE ANALYSIS (Group A)');
  const groupA = results.filter(r => r.group === 'A-discovery');
  const discovered = groupA.filter(r => r.jobsFound > 0);
  const missed = groupA.filter(r => r.jobsFound === 0);

  console.log(`Successful discovery: ${discovered.length}/${groupA.length}`);
  if (discovered.length > 0) {
    console.log('\nFound jobs:');
    discovered.forEach(r => console.log(`  ✓ ${r.id}: ${r.jobsFound} jobs in ${r.pagesVisited} pages (${formatMs(r.durationMs)})`));
  }
  if (missed.length > 0) {
    console.log('\nFailed to find jobs (discovery miss):');
    missed.forEach(r => console.log(`  ✗ ${r.id}: ${r.status}${r.error ? ` — ${r.error.substring(0, 80)}` : ''} | ATS: ${r.ats ?? 'none detected'}`));
  }

  banner('TARGETED MODE ANALYSIS (Group B)');
  const groupB = results.filter(r => r.group === 'B-targeted');
  const avgConfidenceB = groupB.reduce((s, r) => s + r.confidence, 0) / groupB.length;
  const avgDurationB = groupB.reduce((s, r) => s + r.durationMs, 0) / groupB.length;
  const avgJobsB = groupB.reduce((s, r) => s + r.jobsFound, 0) / groupB.length;

  console.log(`Avg confidence : ${(avgConfidenceB * 100).toFixed(1)}%`);
  console.log(`Avg duration   : ${formatMs(avgDurationB)}`);
  console.log(`Avg jobs found : ${avgJobsB.toFixed(1)}`);
  groupB.forEach(r => console.log(`  ${r.id}: ${r.jobsFound} jobs | confidence ${(r.confidence * 100).toFixed(0)}% | ${formatMs(r.durationMs)} | ATS: ${r.ats ?? '—'}`));
}

run().catch(err => {
  console.error('Fatal:', err.message);
  process.exit(1);
});
