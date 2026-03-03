# Tech Icon Brand-Color Tinting

## Problem
Technology icons on the tech detail pages are currently rendered as raw black/monochrome SVGs. The visual impact would be significantly improved if each icon were tinted with its official brand color (e.g., Android green `#3DDC84`, Flutter blue `#02569B`, Kotlin purple `#7F52FF`).

## Design Constraints
1. **Source of truth must be the backend.** The color data is a property of the technology itself, not the frontend presentation layer. Hardcoding it in the frontend would create drift and duplication.
2. **Must fit within the medallion pipeline.** Any new data should follow the existing Bronze → Silver → Gold pattern, or have a clear justification for being a static lookup.
3. **Should be extensible.** As technologies are added (especially via the planned dynamic `tech_dictionary` — see `technology-extraction.md`), colors should be added alongside them without extra work.

---

## Where Does This Data Live?

### Option A: Extend `TechFormatter` with a Color Map (Recommended Short-Term)

`TechFormatter.kt` already serves as the canonical reference for technology names. It would be natural to extend it with a companion `colorMap`:

```kotlin
object TechFormatter {
    private val techMap = mapOf("kotlin" to "Kotlin", ...)
    private val colorMap = mapOf("kotlin" to "#7F52FF", "android" to "#3DDC84", ...)
    
    fun format(tech: String): String = techMap[tech.lowercase()] ?: tech.capitalize()
    fun color(tech: String): String? = colorMap[tech.lowercase()]
}
```

**Pros:**
- Zero infrastructure changes. Ships immediately.
- Co-located with the existing tech name source of truth.
- Colors are not dynamic data — they change extremely rarely (brand guidelines), so there is no real benefit to putting them in BigQuery.

**Cons:**
- Requires a code deploy to add or change a color.
- Duplicates the concept of "technology metadata" across code and (eventually) the `tech_dictionary` table.

### Option B: Column in the Planned `tech_dictionary` Table (Recommended Long-Term)

When Phase 2 of the technology extraction roadmap is implemented (externalizing the tech keywords into a BigQuery `tech_dictionary` table), the schema could include a `brand_color` column:

```sql
CREATE TABLE tech_dictionary (
    tech_id       STRING NOT NULL,   -- e.g. "kotlin"
    display_name  STRING NOT NULL,   -- e.g. "Kotlin"
    brand_color   STRING,            -- e.g. "#7F52FF"
    icon_slug     STRING,            -- e.g. "kotlin" (for simple-icons or local SVG lookup)
    is_active     BOOL DEFAULT TRUE
);
```

This table would serve as the **single source of truth** for everything about a technology: name, color, icon, and active status. `TechFormatter.kt` would load from this table on startup (with caching) instead of using hardcoded maps.

**Pros:**
- Single source of truth, no code deploys for new tech.
- Naturally extensible (add `icon_slug`, `brand_color`, `category`, etc).
- Aligns perfectly with the Phase 2 architecture already planned.

**Cons:**
- Requires the `tech_dictionary` table to exist first.
- Slightly more complex: cache invalidation, startup loading, etc.

### Option C: A Hybrid Static JSON File (Alternative)

Serve a static `/api/tech/metadata.json` from the backend containing all tech IDs → `{ displayName, brandColor, iconSlug }`. This file could be generated at build time from a simple script (reusing our existing `simple-icons` extraction logic), cached by the CDN, and consumed by the frontend on app load.

**Pros:**
- No database schema changes.
- Extremely cacheable (CDN / service worker).
- Decoupled from the per-tech API calls.

**Cons:**
- Another artifact to maintain (build-time generation step).
- Not dynamically driven by the pipeline.

---

## How the Data Flows to the Frontend

Regardless of which storage option is chosen, the data reaches the frontend through one of two paths:

### Path 1: Embed in `TechDetailsPageDto` (Per-Tech Page)

Add a `brandColor` field to the existing DTO:

```kotlin
data class TechDetailsPageDto(
    val techName: String,
    val brandColor: String?,   // <-- NEW
    val totalJobs: Int,
    val seniorityDistribution: List<SeniorityDistributionDto>,
    val hiringCompanies: List<CompanyLeaderboardDto>,
    val roles: List<JobRoleDto>
)
```

The `TechMapper` would call `TechFormatter.color(techName)` and include it in the response. The frontend would read `data.brandColor` and use it directly.

**When to use:** When the color is only needed contextually on the tech details page.

### Path 2: Embed in `SearchSuggestionsResponse` (App-Wide)

The existing `/api/search/suggestions` endpoint returns a list of all technologies. Extending `SearchSuggestionDto` with color would give the frontend a full tech color palette at app startup:

```kotlin
data class SearchSuggestionDto(
    val type: String,       // "TECHNOLOGY" | "COMPANY"
    val id: String,
    val name: String,
    val brandColor: String? // <-- NEW (null for companies)
)
```

**When to use:** When the color is needed in multiple places (landing page tech chips, search results, sidebar, etc).

---

## Frontend Rendering

The icons are currently plain SVGs stored in `public/icons/tech/`. We can tint them using **CSS `mask-image`** without changing the SVG files:

```tsx
// TechIcon.tsx
interface TechIconProps {
    techId: string;
    color?: string;         // brandColor from the API
    size?: string;          // Tailwind size class
    fallbackLetter?: string;
}

const TechIcon = ({ techId, color, size = 'w-12 h-12', fallbackLetter }: TechIconProps) => {
    const [error, setError] = useState(false);
    
    if (error && fallbackLetter) {
        return <span>{fallbackLetter}</span>;
    }
    
    return (
        <div
            className={size}
            style={{
                backgroundColor: color || '#64748b',
                maskImage: `url(/icons/tech/${techId.toLowerCase()}.svg)`,
                WebkitMaskImage: `url(/icons/tech/${techId.toLowerCase()}.svg)`,
                maskSize: 'contain',
                WebkitMaskSize: 'contain',
                maskRepeat: 'no-repeat',
                WebkitMaskRepeat: 'no-repeat',
                maskPosition: 'center',
                WebkitMaskPosition: 'center',
            }}
        />
    );
};
```

**Why `mask-image`?**
- No need to fetch, parse, or inline SVGs at runtime.
- SVGs stay as static assets in `public/` — no build pipeline changes.
- Supports any CSS color, gradients, or even animations on the icon shape.
- [Browser support](https://caniuse.com/css-masks) is excellent (99%+).

---

## Recommended Rollout

| Phase | Action | Depends On |
|-------|--------|------------|
| **Now** | Add `colorMap` to `TechFormatter.kt` (hardcoded). Add `brandColor` to `TechDetailsPageDto`. Build `TechIcon.tsx`. | Nothing |
| **With `tech_dictionary`** | Migrate `colorMap` + `techMap` into the BigQuery table. `TechFormatter` becomes a cached reader. | Phase 2 of tech extraction roadmap |
| **Later** | Extend `SearchSuggestionDto` with `brandColor` for app-wide color data. Use in landing page tech chips, search dropdown, etc. | Phase 1 above |

---

## Generating the Initial Color Data

We already have the `simple-icons` npm package installed from our icon download work. We can extract brand colors from it with a one-liner script:

```js
import * as icons from 'simple-icons';
// icons.siKotlin.hex → "7F52FF"
// icons.siAndroid.hex → "3DDC84"
```

This script can generate the initial `colorMap` entries for `TechFormatter.kt`, giving us a complete, accurate starting dataset sourced directly from the official brand guidelines maintained by the simple-icons community.
