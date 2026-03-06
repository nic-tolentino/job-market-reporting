# Tech & Community Resource Data Modeling Proposal

## Overview
Our current approach uses hard-coded TypeScript constants (`techResources.ts`). While this is excellent for initial prototyping and high performance, it doesn't scale for:
1. **Dynamic Updates**: Adding resources without a code deploy.
2. **Cross-Technology Tagging**: A resource like "Firebase" applies to Android, iOS, and Web.
3. **Advanced Filtering**: Filtering by location, stars, or subscriber counts across 100+ items.

## 1. Schema Design (Relational Approach)

A relational model allows for many-to-many relationships between resources and technologies.

### Base Resource Table (`resources`)
| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary Key |
| `title` | String | Resource name |
| `url` | String | External link |
| `description` | Text | Short blurb |
| `preview_image` | String? | Thumbnail URL |
| `category` | Enum | `course`, `youtube`, `podcast`, `website`, `project`, `expert`, `community`, `event` |
| `country_code` | String? | ISO 3166-1 alpha-2 (e.g., "NZ", "US") or "GLOBAL" |
| `region` | String? | e.g., "Auckland", "California" |
| `city` | String? | e.g., "Auckland Central", "San Francisco" |
| `created_at` | Timestamp | |

> [!NOTE]
> Supra-national regions (e.g., "Europe", "APAC") are currently handled by tagging multiple countries or using the `__GLOBAL__` flag if the resource is universally applicable. We may introduce a `region_groups` table in Phase 3.

### Dynamic Attributes Table (`resource_attributes`)
Instead of hardcoding `stars` and `subscribers`, we use a key-value pair to allow for future metrics (e.g., "Downloads", "Twitter Followers").
| Field | Type | Description |
|-------|------|-------------|
| `resource_id` | UUID | FK to `resources` |
| `attribute_key` | String | `stars`, `subscribers`, `event_date`, `repo_health` |
| `attribute_value`| String | Raw value (e.g., "47k") |
| `value_numeric` | Decimal | For sorting purposes |

### Technology Mapping Table (`resource_tech_mapping`)
| Field | Type | Description |
|-------|------|-------------|
| `resource_id` | UUID | FK to `resources` |
| `tech_id` | String | `android`, `ios`, `react`, etc. |

---

## 2. Storage Strategy

### Phase 1: Managed JSON (Server-Side)
*   **Storage**: Store resources in a single optimized JSON file in an S3 bucket or Supabase Storage.
*   **Update**: Create a simple "Admin" script to validate and sync local additions to the cloud.
*   **Pros**: Low latency, easy to version control.

### Phase 2: Supabase (Headless CMS Style)
*   **Storage**: Use Supabase (PostgreSQL) + Edge Functions.
*   **Pros**: Real-time updates, built-in Auth for moderators, and easy many-to-many queries.
*   **Query Example**: "Fetch all 'projects' tagged as 'android' sorted by 'stars' numeric value."

---

## 3. Handling Cross-Tech Resources
By using a mapping table, we avoid data duplication.
*   **Example**: "Alamofire" entry exists once in `resources`.
*   In `resource_tech_mapping`, it has rows for both `ios` and `macos`.
*   The UI can then query specifically for the current view without duplicating the underlying data.

## 4. Scalability for the UI
To support 100+ items:
1. **Paginated API**: Fetch original 6 items for the "Preview Card".
2. **On-Demand Loading**: Only fetch the "Full List" when the "View Full Directory" modal is opened.
3. **Search Indexing**: Use a lightweight search index (like FlexSearch or Fuse.js) on the client if the data is under 1MB, or use Postgres Full-Text Search for larger datasets.
