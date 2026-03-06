# Strategy: Job Technology Exclusions & "Pure" Views

## Context
Currently, our technology extraction is additive. If a job description mentions "iOS" and "Xamarin", it is tagged with both. While technically accurate, this causes "noise" when a user is specifically looking for **Native** roles. A job that is 100% Xamarin will still appear on the iOS and Android pages, which can be frustrating for native developers.

## Proposed Strategy

### 1. Technology Metadata (The Taxonomy)
We should enhance our technology dictionary (currently `TechFormatter.kt`) to include metadata about the "Nature" of the technology.

| Technology | Category | Sub-category | Is Native? |
| :--- | :--- | :--- | :--- |
| Swift | Mobile | Native | Yes |
| Kotlin | Mobile | Native | Yes |
| iOS | Mobile | Native | Yes |
| Android | Mobile | Native | Yes |
| Xamarin | Mobile | Cross-Platform | No |
| React Native | Mobile | Cross-Platform | No |
| Flutter | Mobile | Cross-Platform | No |

### 2. Multi-Tiered Exclusion Logic

#### A. Implicit Exclusions (Classification Level)
During the `extractTechnologies` phase, we can define "Strong" vs "Weak" associations. 
*   **Rule:** If a job contains a **Cross-Platform** keyword (Xamarin, React Native, Flutter), we can optionally flag it as `is_cross_platform = true`.
*   **Refinement:** We might decide that if `Xamarin` is present, the `iOS` tag should be downgraded or specially marked so it doesn't appear in "Native-Only" searches.

#### B. Explicit Exclusions (Query Level) - *Recommended*
Update the API and search queries to support an `exclude` parameter. This is the most flexible approach.

*   **API Change:** `GET /api/tech/{techName}?exclude=Xamarin,ReactNative,Flutter`
*   **Logic:** When viewing the "iOS" page, the UI can optionally toggle a "Native Only" filter which sends these exclusions to the backend.

### 3. Implementation Details (Backend)

#### SQL Update (BigQuery)
We can use a `NOT EXISTS` or `UNNEST` filter to exclude jobs that contain unwanted technologies.

```sql
SELECT *
FROM `jobs` j, UNNEST(j.technologies) as t
WHERE LOWER(t) = 'ios'
  AND NOT EXISTS (
    SELECT 1 
    FROM UNNEST(j.technologies) as t_ex 
    WHERE t_ex IN ('Xamarin', 'React Native', 'Flutter')
  )
```

#### Mapping Logic
In the backend, we can maintain a Map of "Common Exclusions":
```kotlin
val defaultExclusions = mapOf(
    "iOS" to listOf("Xamarin", "React Native", "Flutter"),
    "Android" to listOf("Xamarin", "React Native", "Flutter"),
    "Java" to listOf("Kotlin"), // For "Pure Java" roles
)
```

### 4. Optional: "Anti-Keywords"
Sometimes a technology isn't enough; we might need to look for specific phrases in the description.
*   **Example:** If a job title says "Senior iOS Developer (Xamarin Expert)", the presence of "Xamarin" in the title is a 100% signal to exclude from a native view even if it says "iOS".

## Next Steps
1.  **Phase 1:** Add the `exclude` parameter to the API and update the BigQuery repository to support it. (Low effort, high impact).
2.  **Phase 2:** Update the `TechFormatter` to support category-based metadata so we can automatically suggest exclusions.
3.  **Phase 3:** Update the Frontend to include a "Native Only" toggle on relevant tech pages.

---
*Created: 2026-03-06*
