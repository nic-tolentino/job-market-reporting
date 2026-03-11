import * as cheerio from 'cheerio';

/**
 * Content extraction result
 */
export interface ExtractedContent {
  mainContent: string;
  title: string;
  metaDescription: string | null;
  canonicalUrl: string | null;
  jsonLd: Record<string, unknown>[];
  ogTags: Record<string, string>;
}

/**
 * Selectors for elements to remove (navigation, footer, ads, etc.)
 */
const REMOVE_SELECTORS = [
  // Navigation
  'nav',
  '.nav',
  '.navbar',
  '.navigation',
  
  // Footer
  'footer',
  '.footer',
  '.site-footer',
  
  // Ads and promotional content
  '.ad',
  '.ads',
  '.advertisement',
  '.promo',
  '.promotional',
  '.banner',
  
  // Cookie banners and popups
  '.cookie-banner',
  '.cookie-notice',
  '.cookie-consent',
  '.popup',
  '.modal',
  '[role="dialog"]',
  
  // Sidebars and widgets
  'aside',
  '.sidebar',
  '.widget',
  
  // Social media widgets
  '.social-share',
  '.social-media',
  '[class*="social"]',
  
  // Comments sections
  '.comments',
  '#comments',
  
  // Forms (except job application forms)
  'form:not(.apply-form)',
  '.newsletter',
  '.subscribe',
  
  // Breadcrumbs
  '.breadcrumbs',
  '.breadcrumb'
];

/**
 * Sanitizes HTML content to prevent prompt injection
 * Removes scripts, iframes, event handlers, and javascript: URLs
 */
export function sanitizeContent(html: string): string {
  return html
    // Remove script tags and content
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    // Remove iframe tags
    .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
    // Remove event handlers (onclick, onload, etc.)
    .replace(/\s*on\w+="[^"]*"/g, '')
    .replace(/\s*on\w+='[^']*'/g, '')
    // Remove javascript: URLs
    .replace(/javascript:/gi, 'blocked:')
    // Remove data: URLs in potentially dangerous contexts
    .replace(/href\s*=\s*["']data:/gi, 'href="blocked:')
    // Remove style tags (can contain expressions)
    .replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '');
}

/**
 * Extracts main content from HTML page
 * Strips navigation, footer, ads, and other non-essential elements
 */
export function extractContent(html: string): ExtractedContent {
  const $ = cheerio.load(html);
  
  // Remove unwanted elements
  for (const selector of REMOVE_SELECTORS) {
    $(selector).remove();
  }
  
  // Extract metadata
  const title = $('title').text().trim() || $('h1').first().text().trim();
  const metaDescription = $('meta[name="description"]').attr('content') || null;
  const canonicalUrl = $('link[rel="canonical"]').attr('href') || null;
  
  // Extract Open Graph tags
  const ogTags: Record<string, string> = {};
  $('meta[property^="og:"]').each((_, el) => {
    const property = $(el).attr('property');
    const content = $(el).attr('content');
    if (property && content) {
      ogTags[property.replace('og:', '')] = content;
    }
  });
  
  // Extract JSON-LD structured data
  const jsonLd: Record<string, unknown>[] = [];
  $('script[type="application/ld+json"]').each((_, el) => {
    try {
      const data = JSON.parse($(el).html() || '{}');
      jsonLd.push(data);
    } catch {
      // Ignore invalid JSON-LD
    }
  });
  
  // Get main content area
  let mainContent = '';
  
  // Try common main content selectors first
  const mainSelectors = ['main', '[role="main"]', '.main-content', '.content', '#content'];
  for (const selector of mainSelectors) {
    const $main = $(selector);
    if ($main.length > 0) {
      mainContent = $main.html() || '';
      break;
    }
  }
  
  // Fallback: use body content
  if (!mainContent) {
    mainContent = $('body').html() || '';
  }
  
  // Sanitize the extracted content
  mainContent = sanitizeContent(mainContent);
  
  return {
    mainContent,
    title,
    metaDescription,
    canonicalUrl,
    jsonLd,
    ogTags
  };
}

/**
 * Wraps content in XML tags for safe LLM processing
 * Helps prevent prompt injection by clearly delimiting user content
 */
export function wrapContentForLlm(content: string): string {
  return `<page-content>\n${content}\n</page-content>`;
}
