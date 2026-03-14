import { sanitizeContent, extractContent, wrapContentForLlm } from '../src/extractor/ContentExtractor';

describe('ContentExtractor', () => {
  describe('sanitizeContent', () => {
    it('removes script tags', () => {
      const html = '<div>Hello<script>alert("xss")</script>World</div>';
      const result = sanitizeContent(html);
      
      expect(result).not.toContain('<script>');
      expect(result).not.toContain('alert');
    });
    
    it('removes iframe tags', () => {
      const html = '<div><iframe src="https://evil.com"></iframe>Content</div>';
      const result = sanitizeContent(html);
      
      expect(result).not.toContain('<iframe>');
    });
    
    it('removes event handlers', () => {
      const html = '<div onclick="alert(1)" onload="evil()">Content</div>';
      const result = sanitizeContent(html);
      
      expect(result).not.toContain('onclick');
      expect(result).not.toContain('onload');
    });
    
    it('blocks javascript: URLs', () => {
      const html = '<a href="javascript:alert(1)">Click</a>';
      const result = sanitizeContent(html);
      
      expect(result).toContain('blocked:');
      expect(result).not.toContain('javascript:');
    });
    
    it('removes style tags', () => {
      const html = '<div>Content<style>.evil { display: none; }</style></div>';
      const result = sanitizeContent(html);
      
      expect(result).not.toContain('<style>');
    });
  });
  
  describe('extractContent', () => {
    it('extracts title from page', () => {
      const html = '<html><head><title>Test Page</title></head><body><h1>Other</h1></body></html>';
      const result = extractContent(html);
      
      expect(result.title).toBe('Test Page');
    });
    
    it('extracts meta description', () => {
      const html = '<html><head><meta name="description" content="Test description"></head></html>';
      const result = extractContent(html);
      
      expect(result.metaDescription).toBe('Test description');
    });
    
    it('extracts canonical URL', () => {
      const html = '<html><head><link rel="canonical" href="https://example.com/page"></head></html>';
      const result = extractContent(html);
      
      expect(result.canonicalUrl).toBe('https://example.com/page');
    });
    
    it('extracts Open Graph tags', () => {
      const html = `
        <html>
          <head>
            <meta property="og:title" content="OG Title">
            <meta property="og:description" content="OG Desc">
          </head>
        </html>
      `;
      const result = extractContent(html);
      
      expect(result.ogTags.title).toBe('OG Title');
      expect(result.ogTags.description).toBe('OG Desc');
    });
    
    it('removes navigation elements', () => {
      const html = '<html><body><nav>Nav</nav><main>Content</main></body></html>';
      const result = extractContent(html);
      
      expect(result.mainContent).not.toContain('<nav>');
      expect(result.mainContent).toContain('Content');
    });
    
    it('removes footer elements', () => {
      const html = '<html><body><footer>Footer</footer><main>Content</main></body></html>';
      const result = extractContent(html);
      
      expect(result.mainContent).not.toContain('<footer>');
    });
    
    it('removes cookie banners', () => {
      const html = '<html><body><div class="cookie-banner">Accept cookies</div><main>Content</main></body></html>';
      const result = extractContent(html);
      
      expect(result.mainContent).not.toContain('cookie-banner');
    });

    it('populates textContent as plain text without tags', () => {
      const html = '<html><body><main><p>Hello <b>World</b></p><ul><li>Item 1</li></ul></main></body></html>';
      const result = extractContent(html);
      
      expect(result.textContent).toBe('Hello World Item 1');
      expect(result.textContent).not.toContain('<p>');
      expect(result.textContent).not.toContain('<b>');
    });

    it('sanitizes simplifiedHtml to allowed tags only', () => {
      const html = `
        <html>
          <body>
            <main>
              <h1>Job Title</h1>
              <div class="irrelevant">
                <p>Description with <span style="color: red">span</span></p>
                <button>Apply Now</button>
                <svg>...</svg>
              </div>
            </main>
          </body>
        </html>
      `;
      const result = extractContent(html);
      
      expect(result.simplifiedHtml).toContain('<h1>Job Title</h1>');
      expect(result.simplifiedHtml).toMatch(/<p>\s*Description with span\s*<\/p>/);
      expect(result.simplifiedHtml).not.toContain('<div');
      expect(result.simplifiedHtml).not.toContain('<span');
      expect(result.simplifiedHtml).not.toContain('<button');
      expect(result.simplifiedHtml).not.toContain('<svg');
    });

    it('strips all attributes except href on anchors in simplifiedHtml', () => {
      const html = '<html><body><a href="https://ex.com" target="_blank" class="keep-me">Link</a><p class="text">Text</p></body></html>';
      const result = extractContent(html);
      
      expect(result.simplifiedHtml).toContain('<a href="https://ex.com">Link</a>');
      expect(result.simplifiedHtml).not.toContain('target="_blank"');
      expect(result.simplifiedHtml).not.toContain('class="btn"');
      expect(result.simplifiedHtml).toContain('<p>Text</p>');
      expect(result.simplifiedHtml).not.toContain('class="text"');
    });
  });
  
  describe('wrapContentForLlm', () => {
    it('wraps content in XML tags', () => {
      const content = 'Some page content';
      const result = wrapContentForLlm(content);
      
      expect(result).toBe('<page-content>\nSome page content\n</page-content>');
    });
    
    it('handles multi-line content', () => {
      const content = 'Line 1\nLine 2\nLine 3';
      const result = wrapContentForLlm(content);
      
      expect(result).toContain('<page-content>');
      expect(result).toContain('Line 1');
      expect(result).toContain('Line 3');
      expect(result).toContain('</page-content>');
    });
  });
});
