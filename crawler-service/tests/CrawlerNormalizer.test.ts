/**
 * CrawlerNormalizer Specification Tests
 * 
 * These tests document the expected behavior of the Kotlin CrawlerNormalizer.
 * They are TypeScript specifications, not actual unit tests.
 * 
 * For actual testing, see:
 * backend/src/test/kotlin/com/techmarket/sync/ats/CrawlerNormalizerTest.kt
 */

describe('CrawlerNormalizer - formatLocation Specification', () => {
  // These are specification tests documenting expected Kotlin behavior
  
  describe('Remote location formatting', () => {
    it('SPEC: converts "Remote, Sydney" to "Remote - Sydney"', () => {
      // Kotlin implementation should:
      // Input: "Remote, Sydney, AU"
      // Expected: "Remote - Sydney, AU"
      expect(formatLocation('Remote, Sydney, AU')).toBe('Remote - Sydney, AU');
    });

    it('SPEC: converts "Remote, " prefix only at start', () => {
      // Input: "Sydney, Remote Office"
      // Expected: "Sydney, Remote Office" (unchanged)
      expect(formatLocation('Sydney, Remote Office')).toBe('Sydney, Remote Office');
    });

    it('SPEC: handles "Remote" alone', () => {
      // Input: "Remote"
      // Expected: "Remote" (unchanged)
      expect(formatLocation('Remote')).toBe('Remote');
    });

    it('SPEC: handles "Hybrid, Sydney" to "Hybrid - Sydney"', () => {
      expect(formatLocation('Hybrid, Sydney, AU')).toBe('Hybrid - Sydney, AU');
    });

    it('SPEC: normalizes whitespace', () => {
      expect(formatLocation('  Sydney  ,   AU  ')).toBe('Sydney , AU');
    });

    it('SPEC: returns empty string for null', () => {
      expect(formatLocation(null as any)).toBe('');
    });

    it('SPEC: returns empty string for blank', () => {
      expect(formatLocation('   ')).toBe('');
    });
  });
});

/**
 * TypeScript reference implementation for specification testing
 * 
 * Kotlin implementation:
 * private fun formatLocation(location: String?): String {
 *     if (location.isNullOrBlank()) return ""
 *     
 *     return location.trim()
 *         .replace(Regex("\\s+"), " ")
 *         .replace(Regex("^Remote,\\s*"), "Remote - ")
 *         .replace(Regex("^Hybrid,\\s*"), "Hybrid - ")
 * }
 */
function formatLocation(location: string | null): string {
  if (!location || location.trim() === '') return '';
  
  return location.trim()
    .replace(/\s+/g, ' ')
    .replace(/^Remote,\s*/, 'Remote - ')
    .replace(/^Hybrid,\s*/, 'Hybrid - ');
}
