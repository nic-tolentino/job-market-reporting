import { AtsDetectionResult } from '../api/types';

/**
 * ATS signature patterns for detection
 */
interface AtsSignature {
  provider: string;
  patterns: {
    iframe?: RegExp[];
    script?: RegExp[];
    url?: RegExp[];
    meta?: RegExp[];
    cssClass?: RegExp[];
  };
}

const ATS_SIGNATURES: AtsSignature[] = [
  {
    provider: 'GREENHOUSE',
    patterns: {
      iframe: [/boards\.greenhouse\.io/i],
      url: [/greenhouse\.io/i],
      cssClass: [/greenhouse-/i]
    }
  },
  {
    provider: 'LEVER',
    patterns: {
      script: [/assets\.lever\.co/i, /lever\.co\/widget/i],
      url: [/lever\.co/i],
      cssClass: [/lever-/i]
    }
  },
  {
    provider: 'ASHBY',
    patterns: {
      script: [/jobs\.ashbyhq\.com/i, /ashbyhq\.com/i],
      url: [/ashbyhq\.com/i],
      cssClass: [/ashby-/i]
    }
  },
  {
    provider: 'WORKDAY',
    patterns: {
      url: [/myworkdayjobs\.com/i, /wd\d+\.myworkdayjobs\.com/i],
      cssClass: [/workday-/i]
    }
  },
  {
    provider: 'WORKABLE',
    patterns: {
      meta: [/generator.*workable/i],
      url: [/workable\.com/i],
      cssClass: [/workable-/i]
    }
  },
  {
    provider: 'JOBADDERS',
    patterns: {
      url: [/jobadder\.com/i],
      cssClass: [/jobadder/i]
    }
  },
  {
    provider: 'SNAPHIRE',
    patterns: {
      url: [/snaphire\.com/i],
      cssClass: [/snaphire/i]
    }
  },
  {
    provider: 'EMPLOYMENT_HERO',
    patterns: {
      url: [/employmenthero\.com/i],
      cssClass: [/employment-hero/i]
    }
  }
];

/**
 * Detects ATS provider from page HTML using signature matching.
 * 
 * Checks for:
 * - iframe src patterns (Greenhouse, Ashby)
 * - Script tag sources (Lever, Workable)
 * - Meta tags (generator, application-name)
 * - URL patterns in links
 * - CSS class patterns
 * 
 * @param html - Raw page HTML
 * @returns ATS detection result or null if unknown
 * 
 * @example
 * ```typescript
 * const result = detectAts('<iframe src="boards.greenhouse.io/acme">');
 * // Returns: { provider: 'GREENHOUSE', identifier: 'acme', confidence: 0.95 }
 * ```
 */
export function detectAts(html: string): AtsDetectionResult | null {
  for (const signature of ATS_SIGNATURES) {
    const result = checkSignature(html, signature);
    if (result) {
      return result;
    }
  }
  return null;
}

function checkSignature(html: string, signature: AtsSignature): AtsDetectionResult | null {
  const { patterns } = signature;
  
  // Check iframe patterns (high confidence)
  if (patterns.iframe) {
    for (const pattern of patterns.iframe) {
      const match = html.match(pattern);
      if (match) {
        const identifier = extractIdentifier(html, signature.provider);
        return {
          provider: signature.provider,
          identifier,
          confidence: 0.95,
          evidence: `iframe src matching ${pattern.source}`
        };
      }
    }
  }
  
  // Check script patterns (high confidence)
  if (patterns.script) {
    for (const pattern of patterns.script) {
      const match = html.match(pattern);
      if (match) {
        const identifier = extractIdentifier(html, signature.provider);
        return {
          provider: signature.provider,
          identifier,
          confidence: 0.90,
          evidence: `script tag matching ${pattern.source}`
        };
      }
    }
  }
  
  // Check URL patterns (high confidence)
  if (patterns.url) {
    for (const pattern of patterns.url) {
      const match = html.match(pattern);
      if (match) {
        const identifier = extractIdentifier(html, signature.provider);
        return {
          provider: signature.provider,
          identifier,
          confidence: 0.85,
          evidence: `URL matching ${pattern.source}`
        };
      }
    }
  }
  
  // Check meta patterns (medium confidence)
  if (patterns.meta) {
    for (const pattern of patterns.meta) {
      const match = html.match(pattern);
      if (match) {
        return {
          provider: signature.provider,
          identifier: null,
          confidence: 0.70,
          evidence: `meta tag matching ${pattern.source}`
        };
      }
    }
  }
  
  // Check CSS class patterns (medium confidence)
  if (patterns.cssClass) {
    for (const pattern of patterns.cssClass) {
      const match = html.match(pattern);
      if (match) {
        return {
          provider: signature.provider,
          identifier: null,
          confidence: 0.60,
          evidence: `CSS class matching ${pattern.source}`
        };
      }
    }
  }
  
  return null;
}

/**
 * Extracts company identifier from HTML based on ATS provider
 */
function extractIdentifier(html: string, provider: string): string | null {
  switch (provider) {
    case 'GREENHOUSE': {
      // Extract from boards.greenhouse.io/{identifier}
      const match = html.match(/boards\.greenhouse\.io\/([^"/]+)/i);
      return match?.[1] || null;
    }
    case 'LEVER': {
      // Extract from lever.co/{identifier} (not from assets.lever.co/widget.js)
      const match = html.match(/(?:web|api)\.lever\.co\/([^"/]+)/i);
      return match?.[1] || null;
    }
    case 'ASHBY': {
      // Extract from jobs.ashbyhq.com/{identifier}
      const match = html.match(/jobs\.ashbyhq\.com\/([^"/]+)/i);
      return match?.[1] || null;
    }
    case 'WORKDAY': {
      // Extract from {slug}.myworkdayjobs.com or {slug}.wd3.myworkdayjobs.com
      const match = html.match(/([a-z0-9-]+)\.(?:wd\d+\.)?myworkdayjobs\.com/i);
      return match?.[1] || null;
    }
    default:
      return null;
  }
}
