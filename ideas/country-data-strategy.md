# Country Data Strategy

To establish DevAssembly as a premier source of tech market insights for both New Zealand and Australia, we must transition our architecture from implicitly assuming "NZ" to explicitly handling multi-country data.

This document outlines the strategy for extending our dataset to include Australian (AU) content and the architectural changes required across the stack.

---

## 1. Data Ingestion & Defining Job Country

Currently, jobs are ingested via third-party scrapers (e.g., Apify for LinkedIn/SEEK) and direct ATS APIs. To support AU content, we need to accurately define whether an incoming job belongs to NZ or AU.

### Strategy
*   **Source-Level Definition (Preferred):** The most reliable way to define a job's country is at the ingestion source. When running scrapers, we should run distinct tasks for AU and NZ (e.g., an Apify task targeting LinkedIn AU, and another for LinkedIn NZ). The ingestion payload to our `RawJobDataMapper` should include an explicit `target_country` parameter.
*   **Location Parsing (Fallback/Validation):** For direct ATS integrations (where the API returns global jobs) or to validate scraped data, we need a location parser. We can map known AU states (NSW, VIC, QLD, WA, SA, TAS, ACT, NT) and major cities to "AU", and NZ regions to "NZ". 
*   **Data Model:** The `JobRecord` in BigQuery already supports a `country` field. We must strictly enforce that this field contains standard ISO codes (`"NZ"`, `"AU"`, `"US"`, etc.).

---

## 2. Handling Company Multi-Nationalism

A significant challenge in the ANZ market is the overlap of companies. Many AU companies operate in NZ and vice versa, and many global companies operate in both.

### Strategy
We will leverage our Master Manifest (`data/companies.json`) and Medallion Architecture:
*   **Explicit Fields:** Our existing schema already includes `hq_country` and `operating_countries`. We must utilize these strictly.
*   **Cross-Pollination:** An Australian company hiring in NZ should be defined as `{ hq_country: "AU", operating_countries: ["AU", "NZ"] }`. 
*   **Frontend Filtering:** When a user explores the "NZ Market", they should see companies where `"NZ" IN operating_countries`. We can add badge indicators in the UI to distinguish "Local HQ" (`hq_country === selectedCountry`) vs "International/Trans-Tasman" (`hq_country !== selectedCountry`).

---

## 3. Learning & Community Resources

Currently, `frontend/src/constants/techResources.ts` blends "Global" resources (like official YouTube channels) with highly localized "NZ" communities (like GDG Wellington or Auckland iOS Meetup).

### Strategy
We need to refactor the resource data structure to separate global tools from local communities, allowing the frontend to swap out local communities based on the selected country context.

*   **Refactor `techResources.ts`:** Introduce a country dimension.
    *   *Option A (Nested Context):* Structure data as `RESOURCES[countryCode][technology]`.
    *   *Option B (Tagging):* Add a `countries: string[]` field to every local resource (e.g., `["NZ"]`, `["AU"]`, or `["Global"]`).
*   **Implementation:** Option B (Tagging) is preferred to prevent duplicating global resources (like "Fireship" on YouTube or React documentation) across both the AU and NZ trees. The frontend will filter the list: `resources.filter(r => r.countries.includes("Global") || r.countries.includes(selectedCountry))`.

---

## 4. Frontend Country Selection Architecture

The existing UI has a country Dropdown in `Navbar.tsx` with a local React state (`const [selectedCountry, setSelectedCountry] = useState('NZ')`), but it does not affect the rest of the app.

### Strategy
To make the application contextually aware of the chosen country, we must lift this state.

*   **Global State Management:** Move `selectedCountry` into a global Zustand store (e.g., `useAppStore`). 
*   **Persistence:** Sync the selected country to `localStorage` so a user's preference (e.g., an Australian developer) is remembered across sessions.
*   **Routing (Alternative):** Alternatively, we could prefix routes (`/nz/tech/react` vs `/au/tech/react`). However, since DevAssembly functions as a unified dash, a global toggle toggle is often a smoother UX than hard reloads.
*   **API Hookup:** Every data-fetching hook in the frontend (fetching jobs, companies, or market stats) must be updated to read `selectedCountry` from the global store and pass it as a query parameter (e.g., `GET /api/jobs?country=AU`).

---

## Implementation Status (March 2026)

✅ **Frontend:** Implemented the Zustand store (`useAppStore`) for the country selector, syncing to `localStorage`, and hooked it up to existing API calls (`api.ts`).
✅ **Resources:** Refactored `techResources.ts` to include explicitly tagged AU communities (e.g., Sydney iOS Meetup, Melbourne CocoaHeads) using `countries` arrays.
✅ **Backend/Ingestion:** Updated the controllers to accept the `country` `@RequestParam`. BigQuery SQL dynamically filters `AND (@country IS NULL OR ${JobFields.COUNTRY} = @country)`. Cache keys use `JobFields.COUNTRY` appropriately to prevent data leakage between country contexts.
✅ **Company Data:** Tested and resolved ghost company mapping. `AnalyticsQueries` accurately use standard `EXISTS` block to only match and suggest companies actively hiring in the requested `country`. Australian tech jobs are successfully synchronizing into Production BigQuery.
