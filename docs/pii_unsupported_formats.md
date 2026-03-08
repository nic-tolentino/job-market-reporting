# PII Sanitizer - Unsupported Formats

This document tracks PII formats that are **not currently supported** by the `PiiSanitizer` utility, to be addressed in future enhancements.

**Last Updated:** March 8, 2026

---

## Currently Supported Formats

### Email Addresses
- ✅ Standard formats: `user@domain.com`
- ✅ Regional TLDs: `.co.nz`, `.com.au`, `.co.uk`

### Phone Numbers - Australia (AU)
- ✅ International: `+61 4 1234 5678`, `+61412345678`
- ✅ Mobile: `0412 345 678`, `0412-345-678`, `0412345678`
- ✅ Landline: `(02) 1234 5678`, `02 1234 5678`

### Phone Numbers - New Zealand (NZ)
- ✅ International: `+64 21 123 4567`, `+64211234567`
- ✅ Mobile: `021 123 4567`, `021-123-4567`, `0211234567`
- ✅ Landline: `09 123 4567`, `03-123-4567`

---

## Unsupported Formats (Future Enhancement)

### Phone Numbers - Spain (ES)
**Priority:** MEDIUM - Spain is one of our target markets

| Format | Example | Notes |
|--------|---------|-------|
| International | `+34 91 123 45 67` | Country code +34 |
| Madrid landline | `91 123 45 67` | 9-digit format |
| Barcelona landline | `93 123 45 67` | 9-digit format |
| Mobile | `612 34 56 78` | Starts with 6 or 7 |
| Mobile with dashes | `612-34-56-78` | Less common |
| International mobile | `+34 612 34 56 78` | |

**Implementation Notes:**
- Spanish numbers are 9 digits after country code
- Landlines start with 8 or 9
- Mobiles start with 6 or 7
- Format varies: space-separated, dash-separated, or continuous

---

### Phone Numbers - Other Countries

#### United Kingdom (UK)
| Format | Example |
|--------|---------|
| International | `+44 20 1234 5678` |
| London | `020 1234 5678` |
| Mobile | `07123 456789` |

#### United States/Canada (US/CA)
| Format | Example |
|--------|---------|
| International | `+1 415 123 4567` |
| Domestic | `(415) 123-4567` |
| Toll-free | `800-123-4567` |

#### India (IN)
| Format | Example |
|--------|---------|
| International | `+91 98765 43210` |
| Domestic | `098765 43210` |
| Mobile | `98765 43210` |

---

### Other PII Types (Not Currently Sanitized)

#### Social Security / National ID Numbers
| Country | Format | Example | Priority |
|---------|--------|---------|----------|
| US SSN | XXX-XX-XXXX | `123-45-6789` | LOW |
| NZ IRD | XXXXXXX | `1234567` | LOW |
| AU TFN | XXX XXX XXX | `123 456 789` | LOW |
| ES DNI | XXXXXXXX-L | `12345678-A` | LOW |

**Reason for LOW priority:** Rarely appears in job descriptions; more common in application forms (which we don't scrape).

#### Physical Addresses
| Type | Example | Priority |
|------|---------|----------|
| Street address | `123 Queen Street, Auckland 1010` | LOW |
| PO Box | `PO Box 12345` | LOW |

**Reason for LOW priority:** Usually company addresses (legitimate business info), not personal PII.

#### Personal Names
- **Challenge:** Distinguishing recruiter names from PII vs. legitimate business contacts
- **Current approach:** Names like "Bob Blob" are not redacted as they're typically recruiter identifiers
- **Future consideration:** Context-aware detection (e.g., "call Bob Blob on..." → redact name + phone together)

#### Dates of Birth
| Format | Example |
|--------|---------|
| ISO | `1990-01-15` |
| AU/NZ | `15/01/1990` |
| US | `01/15/1990` |

**Priority:** LOW - Rarely appears in job postings

---

## Edge Cases in Supported Formats

### Australian Numbers - Not Fully Covered
- ✅ `0412 345 678` (standard mobile)
- ❌ `041234567 8` (unusual spacing)
- ❌ `61 4123 456 78` (inconsistent international format)

### New Zealand Numbers - Not Fully Covered
- ✅ `021 123 4567` (standard mobile)
- ❌ `021123456` (too short - invalid but might appear)
- ❌ `64 21 1234567` (missing + in international)

### Email - Edge Cases
- ✅ `user.name+tag@domain.co.nz`
- ❌ `user@domain` (missing TLD - invalid)
- ❌ `user@localhost` (internal - unlikely in job posts)

---

## Recommended Implementation Approach

### Phase 1: Spain Support (Next Priority)
1. Add `ES_PHONE_REGEX` to `PiiSanitizer.kt`
2. Pattern: `(?:\+?34[\s-]?)?[6-9][\d]{1,2}[\s-]?[\d]{2,3}[\s-]?[\d]{2}[\s-]?[\d]{2}`
3. Add 10+ unit tests for ES formats
4. Update documentation

### Phase 2: Multi-Country Abstraction
Consider refactoring to a more scalable approach:
```kotlin
object PhonePatterns {
    val AU = Regex("""...""")
    val NZ = Regex("""...""")
    val ES = Regex("""...""")
    val UK = Regex("""...""")
    
    val ALL = listOf(AU, NZ, ES, UK)
}

// Usage:
PhonePatterns.ALL.forEach { pattern ->
    text = text.replace(pattern, "[REDACTED PHONE]")
}
```

### Phase 3: Context-Aware Detection
- Use NLP or rule-based context detection
- Example: "call [NAME] on [PHONE]" → redact both
- Higher accuracy, fewer false positives

---

## Testing Strategy for New Formats

When adding support for new formats:

1. **Collect real examples** from job postings
2. **Add 10+ test cases** per country:
   - International format
   - Domestic format
   - With spaces
   - With dashes
   - Without separators
   - Edge cases (unusual spacing)
3. **Test against false positives**:
   - Ensure regular numbers aren't redacted (e.g., "5 offices")
   - Ensure dates aren't redacted
4. **Manual review** of sanitized job descriptions

---

## Related Files
- `backend/src/main/kotlin/com/techmarket/util/PiiSanitizer.kt`
- `backend/src/test/kotlin/com/techmarket/util/PiiSanitizerTest.kt`
