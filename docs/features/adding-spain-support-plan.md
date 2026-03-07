# Plan: Adding Spain Support

To establish DevAssembly as a prominent source of tech market insights in Europe, we are extending support to Spain (ES). This document outlines the technical changes required across the stack.

> **Status**: Partially implemented. Spanish cities exist in parser but work model keywords and frontend selector are missing.

## 1. Backend & Data Ingestion

### Advanced Location Parsing
We will expand `RawJobDataParser.kt` to map all 17 Autonomous Communities and their capitals. This ensures that any job posted in a major Spanish city is correctly attributed.

| City | Autonomous Community | ISO Code | Status |
| :--- | :--- | :--- | :--- |
| Madrid | Community of Madrid | ES | ✅ Implemented |
| Barcelona | Catalonia | ES | ✅ Implemented |
| Valencia | Valencian Community | ES | ✅ Implemented |
| Sevilla / Seville | Andalusia | ES | ✅ Implemented |
| Zaragoza | Aragon | ES | ✅ Implemented |
| Málaga | Andalusia | ES | ✅ Implemented |
| Bilbao | Basque Country | ES | ✅ Implemented |
| Murcia | Region of Murcia | ES | ❌ TODO |
| Palma | Balearic Islands | ES | ❌ TODO |
| Las Palmas / Santa Cruz | Canary Islands | ES | ❌ TODO |
| Alicante | Valencian Community | ES | ❌ TODO |
| Granada | Andalusia | ES | ❌ TODO |

**Required Code Changes:**

```kotlin
// RawJobDataParser.kt - Add to knownLocations map
"murcia" to Triple("Murcia", "Region of Murcia", "ES"),
"palma" to Triple("Palma", "Balearic Islands", "ES"),
"las palmas" to Triple("Las Palmas", "Canary Islands", "ES"),
"santa cruz" to Triple("Santa Cruz", "Canary Islands", "ES"),
"alicante" to Triple("Alicante", "Valencian Community", "ES"),
"granada" to Triple("Granada", "Andalusia", "ES"),
```

### Work Model Keywords (CRITICAL)
The current `extractWorkModel()` only detects English keywords. Spanish job postings must be supported.

**Current Implementation (Incomplete):**
```kotlin
return when {
    combined.contains("remote") -> "Remote"
    combined.contains("hybrid") -> "Hybrid"
    else -> null
}
```

**Required Fix:**
```kotlin
return when {
    combined.contains("remote") || combined.contains("teletrabajo") -> "Remote"
    combined.contains("hybrid") || combined.contains("híbrido") -> "Hybrid"
    combined.contains("presencial") -> "On-site"
    else -> null
}
```

### Salary Normalization (CRITICAL)
Spanish salaries use European number formatting. Current `parseSalary()` strips all non-digits, which breaks `35.000€` format.

**Issue:** European format uses `.` as thousands separator and `,` as decimal separator (opposite of US/UK).

**Required Fix:**
```kotlin
fun parseSalary(salaryStr: String?, country: String = "Unknown"): Int? {
    if (salaryStr == null) return null
    return try {
        val normalized = if (country == "ES") {
            // European: 35.000€ -> 35000
            salaryStr.replace(Regex("[^0-9]"), "")
        } else {
            salaryStr.replace(Regex("[^0-9]"), "")
        }
        if (normalized.isNotBlank()) normalized.toInt() else null
    } catch (e: Exception) {
        null
    }
}
```

### Verification Strategy (Backend)
- **`RawJobDataParserTest.kt`**: Add a dedicated `Spain support` test block.
    - ✅ Test `determineCountry` returns "ES" for Madrid (already exists)
    - ❌ Test city-to-community mapping for all 12 cities
    - ❌ Test Spanish keywords for work models (`Teletrabajo`, `Híbrido`, `Presencial`)
    - ❌ Test `determineCountry` with "España" and "Spain" location strings
    - ❌ Test salary parsing with European format (`35.000€`, `€40.000`)

- **`LocationFormatterTest.kt`**: Ensure Spanish address formats are handled correctly.
    - Test addresses with Spanish street formats (e.g., "Calle de Alcalá, 1, Madrid")
    - Test deduplication with Spanish locations (e.g., "Madrid, Madrid")
    - Test accented characters (e.g., "Málaga, Málaga, Andalucía")

**Example Test Cases to Add:**
```kotlin
@Test
fun `extractWorkModel detects Teletrabajo as Remote`() {
    assertEquals("Remote", parser.extractWorkModel("Madrid, España", "Desarrollador - Teletrabajo"))
}

@Test
fun `extractWorkModel detects Híbrido as Hybrid`() {
    assertEquals("Hybrid", parser.extractWorkModel("Barcelona", "Ingeniero Híbrido"))
}

@Test
fun `parseSalary handles European format`() {
    assertEquals(35000, parser.parseSalary("35.000€", "ES"))
    assertEquals(40000, parser.parseSalary("€40.000", "ES"))
}
```

> **Note:** `LocationFormatter.kt` is located at `backend/src/main/kotlin/com/techmarket/util/LocationFormatter.kt` and handles deduplication/normalization.

## 2. Frontend & UI Architecture

### Global State & Selector (CRITICAL)
**Navbar Update:** Add Spain to the country selector dropdown.

**Current Implementation (`Navbar.tsx`):**
```typescript
const countries = [
    { code: 'AU', name: 'Australia', flag: '🇦🇺' },
    { code: 'NZ', name: 'New Zealand', flag: '🇳🇿' },
];
```

**Required Change:**
```typescript
const countries = [
    { code: 'AU', name: 'Australia', flag: '🇦🇺' },
    { code: 'NZ', name: 'New Zealand', flag: '🇳🇿' },
    { code: 'ES', name: 'Spain', flag: '🇪🇸' },
];
```

**ISO Standard:** Ensure `ES` is used consistently across all API calls from the frontend.

### Refined Learning & Community Resources
`techResources.ts` will be updated with high-quality, localized links following the existing structure for NZ/AU communities.

#### Structure to Follow
Each tech stack (android, ios, flutter) should have Spanish entries in:
- `communities` - Local meetups and GDG chapters
- `upcomingEvents` - Conferences with dates
- `localProjects` - Spanish open-source projects
- `localExperts` - Spanish GDEs and community leaders

#### Android Communities (TODO)
| Name | Type | Location | URL |
| :--- | :--- | :--- | :--- |
| GDG Madrid | Community | Madrid | [meetup.com/gdg-madrid](https://www.meetup.com/gdg-madrid/) |
| GDG Barcelona | Community | Barcelona | [meetup.com/gdg-barcelona](https://www.meetup.com/gdg-barcelona/) |
| Android Madrid | Meetup | Madrid | [meetup.com/android-madrid](https://www.meetup.com/android-madrid/) |
| Android Barcelona | Meetup | Barcelona | [meetup.com/android-barcelona](https://www.meetup.com/android-barcelona/) |

#### iOS Communities (TODO)
| Name | Type | Location | URL |
| :--- | :--- | :--- | :--- |
| SwiftBarcelona | Meetup | Barcelona | [meetup.com/swiftbarcelona](https://www.meetup.com/swiftbarcelona/) |
| NSCoder Night Madrid | Meetup | Madrid | [meetup.com/nscoder-night-madrid](https://www.meetup.com/nscoder-night-madrid/) |
| GDG Madrid iOS | Community | Madrid | [meetup.com/gdg-madrid](https://www.meetup.com/gdg-madrid/) |

#### Flutter Communities (TODO)
| Name | Type | Location | URL |
| :--- | :--- | :--- | :--- |
| Flutter Madrid | Meetup | Madrid | [meetup.com/flutter-madrid](https://www.meetup.com/flutter-madrid/) |
| Flutter Barcelona | Meetup | Barcelona | [meetup.com/flutter-barcelona](https://www.meetup.com/flutter-barcelona/) |
| GDG Valencia Flutter | Community | Valencia | [meetup.com/gdg-valencia](https://www.meetup.com/gdg-valencia/) |

#### Major Events (TODO)
| Event | Type | Location | URL |
| :--- | :--- | :--- | :--- |
| T3chFest | Conference | Madrid | [t3chfest.es](https://t3chfest.es/) |
| JBCNConf | Conference | Barcelona | [jbcnconf.com](https://www.jbcnconf.com/) |
| DevFest Madrid | Conference | Madrid | [devfest.gdgmadrid.es](https://devfest.gdgmadrid.es/) |
| DevFest Barcelona | Conference | Barcelona | [devfest.gdgbarcelona.com](https://devfest.gdgbarcelona.com/) |

## 3. Company Master Manifest (`data/companies.json`)

We will add the following "Spanish Unicorns" and tech leaders following the existing schema format.

### Required Schema Format
Each company must include all fields to match existing entries:

```json
{
  "id": "glovo",
  "name": "Glovo",
  "alternateNames": [],
  "description": "On-demand delivery platform.",
  "website": "https://glovoapp.com",
  "logoUrl": "<linkedin-logo-url>",
  "industries": ["Delivery", "Marketplace", "SaaS"],
  "company_type": "Product",
  "is_agency": false,
  "is_social_enterprise": false,
  "hq_country": "ES",
  "operating_countries": ["ES", "IT", "FR", "PT", "AR"],
  "office_locations": ["Barcelona"],
  "remote_policy": "Hybrid",
  "visa_sponsorship": true,
  "updated_at": "2026-03-07T10:00:00Z",
  "employees_count": null,
  "verification_level": "verified"
}
```

### Companies to Add

| Company | HQ | Description | Website | Industries |
| :--- | :--- | :--- | :--- | :--- |
| **Glovo** | Barcelona | On-demand delivery platform. | [glovoapp.com](https://glovoapp.com) | Delivery, Marketplace, SaaS |
| **Cabify** | Madrid | Multi-mobility platform (Ride-hailing). | [cabify.com](https://cabify.com) | Transportation, Mobility, SaaS |
| **Wallapop** | Barcelona | Leading second-hand marketplace. | [wallapop.com](https://wallapop.com) | Marketplace, E-commerce, Consumer |
| **Idealista** | Madrid | Largest real estate portal in Spain. | [idealista.com](https://idealista.com) | Real Estate, PropTech, Marketplace |
| **Typeform** | Barcelona | Conversational interaction platform. | [typeform.com](https://typeform.com) | SaaS, Forms, Data Collection |
| **Factorial** | Barcelona | All-in-one HR software for SMEs. | [factorialhr.com](https://factorialhr.com) | HR Tech, SaaS, Enterprise |
| **TravelPerk** | Barcelona | Business travel management platform. | [travelperk.com](https://travelperk.com) | Travel, SaaS, Enterprise |
| **Fever** | Madrid | Live entertainment discovery platform. | [feverup.com](https://feverup.com) | Entertainment, Events, Consumer |

### Additional Spanish Tech Companies (Optional)

| Company | HQ | Description | Website |
| :--- | :--- | :--- | :--- |
| **Jobandtalent** | Madrid | Digital employment platform. | [jobandtalent.com](https://www.jobandtalent.com) |
| **Holded** | Barcelona | Cloud-based ERP and invoicing. | [holded.com](https://www.holded.com) |
| **Red Points** | Barcelona | Online brand protection platform. | [redpoints.com](https://www.redpoints.com) |
| **Adevinta** | Barcelona | Online classifieds marketplace. | [adevinta.com](https://www.adevinta.com) |
| **eDreams ODIGEO** | Barcelona | Travel technology company. | [odigeo.com](https://www.odigeo.com) |

> **Note:** Logo URLs should be fetched from LinkedIn company pages or official press kits.

## 4. Further Improvements & Suggestions (Future Phases)

### Phase 2 - Post-Launch Enhancements

1.  **Language Detection**
    - Many Spanish job postings are written in Spanish
    - Add a `language` field to job data using lightweight detection (e.g., `langdetect` library)
    - Consider implementing a "Translate to English" toggle in the UI
    - **Effort**: Medium | **Impact**: High for international users

2.  **Digital Nomad Visa Badge**
    - Spain launched a Digital Nomad Visa in 2023
    - Add `digital_nomad_friendly: Boolean` field to companies
    - Research which companies actively support visa sponsorship
    - **Effort**: Medium (requires research) | **Impact**: High for expat talent
    - **Warning**: Don't overpromise - verify claims before adding badges

3.  **Autonomous Community Filters**
    - Regions (Catalonia, Andalusia, Basque Country) are important for local identity
    - Allow filtering by Autonomous Community in addition to city
    - Requires UI filter component updates
    - **Effort**: Low | **Impact**: Medium

4.  **Salary Context Enhancement**
    - Display salary context (e.g., "€35,000 - Madrid average: €32,000")
    - Requires building salary benchmarks per city/role
    - **Effort**: High | **Impact**: High

5.  **Gross vs Net Estimates (WOW Factor)**
    - In Spain, candidates are highly sensitive to "Sueldo Neto" (Take-home pay).
    - Suggestion: Integrate a basic tax estimation logic or link to a localized calculator (e.g., TaxDown or similar API).
    - **Effort**: Medium | **Impact**: Very High

6.  **Labour Law & Holidays Context**
    - Spain has 14 standard bank holidays (some national, some regional).
    - This impacts project timelines and "Availability" statistics if we ever track developer availability.
    - **Effort**: Low (Metadata only) | **Impact**: Medium

---

## Implementation Checklist

### Phase 1 - Critical (Blocks Launch)

- [ ] **Backend - Parser**: Add remaining 5 Spanish cities (Murcia, Palma, Las Palmas, Alicante, Granada)
- [ ] **Backend - Work Models**: Add Spanish keywords (`teletrabajo`, `híbrido`, `presencial`)
- [ ] **Backend - Salary**: Handle European number formatting (`35.000€`)
- [ ] **Backend - Tests**: Comprehensive Spain test suite
- [ ] **Frontend - Navbar**: Add Spain selector (`{ code: 'ES', name: 'Spain', flag: '🇪🇸' }`)

### Phase 2 - Quality Enhancements

- [ ] **Frontend - Resources**: Add Spanish communities to `techResources.ts` (Android, iOS, Flutter)
- [ ] **Frontend - Events**: Add Spanish tech events (T3chFest, JBCNConf, DevFests)
- [ ] **Data - Companies**: Add 8 Spanish companies with full schema
- [ ] **Data - Logos**: Fetch LinkedIn logos for all Spanish companies

### Phase 3 - Future Enhancements

- [ ] **Feature**: Language detection for job postings
- [ ] **Feature**: Digital Nomad visa badge/filter
- [ ] **Feature**: Autonomous Community filters
- [ ] **Feature**: Salary benchmarking per city

---

## Notes

- **Valencia Ambiguity**: "Valencia" exists in both Spain (ES) and Venezuela (VE). Consider adding context-based disambiguation if Venezuelan jobs appear.
- **GDG Coverage**: Spain has active GDG chapters in Madrid, Barcelona, Valencia, Sevilla, Málaga, Bilbao, Zaragoza, and Canary Islands. Prioritize these for community resources.
- **LocationFormatter**: Exists at `backend/src/main/kotlin/com/techmarket/util/LocationFormatter.kt`. Handles deduplication and normalization of Spanish address formats.
