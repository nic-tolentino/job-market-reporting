/**
 * Shared constants for the crawler service.
 * Centralises magic strings and numbers that appear across multiple files.
 */

// ---------------------------------------------------------------------------
// Crawler identity
// ---------------------------------------------------------------------------
export const CRAWLER_USER_AGENT = 'DevAssemblyBot';

// ---------------------------------------------------------------------------
// Page budgets
// ---------------------------------------------------------------------------
/** Max pages to visit in targeted mode (seed URL known). 10 nav + 50 pagination. */
export const PAGE_BUDGET_TARGETED = 60;
/** Max pages to visit in discovery mode (crawling from homepage). */
export const PAGE_BUDGET_DISCOVERY = 10;
/** Max pagination pages to follow from a single seed URL. */
export const DEFAULT_PAGINATION_LIMIT = 50;

// ---------------------------------------------------------------------------
// Timing
// ---------------------------------------------------------------------------
/** Delay between per-page Gemini extraction calls (ms) — avoids TPM limits. */
export const INTER_EXTRACTION_DELAY_MS = 2000;
/**
 * How long (ms) to wait for networkidle before giving up and using whatever
 * content has loaded. Keep this generous enough for Greenhouse/Workable AJAX
 * calls to complete, but short enough to not stall on live-chat beacons.
 */
export const NETWORK_IDLE_TIMEOUT_MS = 8000;

// ---------------------------------------------------------------------------
// Pagination detection
// ---------------------------------------------------------------------------
/**
 * Full set of query-param names that indicate pagination.
 * Used by isPaginationLink() to decide whether to follow a link.
 */
export const PAGINATION_PARAMS = [
  'page', 'p', 'offset', 'start', 'cursor', 'skip', 'index', 'batch'
] as const;

/**
 * Core pagination params used when naming the detected pagination pattern.
 * Subset of PAGINATION_PARAMS — covers the most common, human-readable params.
 */
export const CORE_PAGINATION_PARAMS = ['page', 'p', 'offset', 'start'] as const;

// ---------------------------------------------------------------------------
// Discovery link-following regexps
// ---------------------------------------------------------------------------
/**
 * Patterns matched against each candidate link URL during Discovery mode.
 * A link must match at least one pattern to be enqueued.
 *
 * Two categories of pattern are needed:
 *   1. Path-based  — /careers, /careers/, /careers/anything
 *                    Also covers nested paths: /about/careers, /about-us/jobs …
 *   2. Subdomain   — careers.example.com, jobs.example.com
 *                    Many large companies (Xero → careers.xero.com,
 *                    MYOB → careers.myob.com) host their board on a subdomain.
 *                    Crawlee's `same-domain` strategy allows crossing subdomains
 *                    (it matches on eTLD+1), so we just need the regexp to match.
 *
 * Note: globs were previously used here but silently miss bare /careers paths
 * (no trailing slash) because the glob `**\/careers\/**` requires content after
 * the segment. Regexps give us precise control without that pitfall.
 */
export const DISCOVERY_REGEXPS: RegExp[] = [
  // Path: /career, /careers, /jobs, /open-roles, /vacancies, /work-with-us
  // Accepts the path with or without trailing slash / query / hash / sub-path.
  /\/(career|careers|jobs|open-roles|vacancies|work-with-us)(\/|$|\?|#)/i,

  // Nested path: /about/careers, /about-us/jobs, /company/careers …
  /\/[^/]+\/(career|careers|jobs)(\/|$|\?|#)/i,

  // Subdomain: careers.example.com  or  jobs.example.com
  /^https?:\/\/(careers?|jobs|work)\./i,
];

// ---------------------------------------------------------------------------
// Job role filtering
// ---------------------------------------------------------------------------
/**
 * Keywords that indicate a tech role.
 * At least one must appear in the job title for general/careers seeds.
 */
export const TECH_KEYWORDS = [
  'engineer', 'developer', 'architect', 'sre', 'data', 'platform',
  'security', 'infrastructure', 'automation', 'devops', 'ml', 'ai',
  'qa', 'analyst',
] as const;

/**
 * Keywords that indicate a non-tech role.
 * If matched, the job is dropped even when a tech keyword is also present.
 */
export const NEGATIVE_KEYWORDS = [
  'sales', 'marketing', 'hr', 'finance', 'legal', 'customer success',
  'account manager', 'talent', 'recruiter', 'operations', 'administrative',
] as const;

// ---------------------------------------------------------------------------
// Misc
// ---------------------------------------------------------------------------
/** Maximum character length kept from an error message before truncation. */
export const ERROR_MESSAGE_MAX_LENGTH = 500;
