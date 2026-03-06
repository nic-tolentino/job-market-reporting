# Renaming Analytics to Insights

This document outlines the plan for renaming all "Analytics" related classes and concepts to "Insights". This move aims to clarify the distinction between market data findings (Insights) and future user behavioral tracking (Analytics).

## Pros and Cons

### Pros
*   **Semantic Clarity**: "Insights" more accurately describes the current functionality (top technologies, company trends, and market stats).
*   **Namespace Reservation**: Frees up the "Analytics" term for future implementation of user behavior tracking, event logging, or integration with tools like Google Analytics or Mixpanel.
*   **Avoids Library Collisions**: The project already uses `import { Analytics } from '@vercel/analytics/react'` in the frontend (`App.tsx`). Renaming internal classes prevents naming conflicts and mental overhead when working across the stack.
*   **Reduced Developer Confusion**: Prevents ambiguity when a developer adds "real" analytics later; they won't have to wonder why `AnalyticsRepository` handles search suggestions.
*   **Logical Grouping**: Feedback and search misses are "Operational Insights" rather than statistical analytics.

### Cons
*   **Refactoring Effort**: Requires updating multiple files across the backend and potentially some frontend references.
*   **Documentation Debt**: Existing documentation, blog posts, or internal notes might become slightly outdated until updated.
*   **Risk of Regression**: Any manual renaming (outside of IDE refactoring tools) carries a small risk of breaking imports or string-based references.

---

## What is Involved

### 1. Backend Refactoring (Kotlin)
The bulk of the work is in the `com.techmarket.persistence` layer.

*   **Package Rename**: 
    *   Rename `com.techmarket.persistence.analytics` to `com.techmarket.persistence.insights`.
*   **Class & Interface Renames**:
    *   `AnalyticsRepository` → `InsightsRepository`
    *   `AnalyticsBigQueryRepository` → `InsightsBigQueryRepository`
    *   `AnalyticsQueries` → `InsightsQueries`
    *   `AnalyticsMapper` → `InsightsMapper`
    *   `AnalyticsFields` → `InsightsFields` (in `BigQueryConstants.kt`)
*   **Variable Renames**:
    *   Instances like `private val analyticsRepository: AnalyticsRepository` in Controllers.
*   **Test Renames**:
    *   `AnalyticsMapperTest` → `InsightsMapperTest`
*   **Spring Bean Updates**:
    *   Update `@Repository` names if explicitly named, though mostly handled by class renaming.

### 2. Controller & Service Updates
*   **Controllers**: Update `LandingController`, `SearchController`, and `FeedbackController` to use `InsightsRepository`.
*   **Imports**: Mass update imports across the `com.techmarket.api` package.

### 3. BigQuery / Database Layer
*   While the *classes* change, the BigQuery table names (e.g., `search_misses`, `user_feedback`) can likely remain as they are, as they were already fairly generic. However, if any table prefix used "analytics", it should be updated in `BigQueryTables`.

### 4. Frontend Updates
*   **Api Hooks/Services**: If there are frontend services named `analyticsService.ts`, rename to `insightsService.ts`.
*   **Variable Names**: Update any usage of `analyticsData` to `insightsData`.

### 5. Documentation
*   Update `README.md`, `data-pipeline.md`, and any other strategy docs in the `ideas/` folder.

---

## Implementation Sequence
1.  **Preparation**: Ensure a clean git state.
2.  **Backend Refactor**: Use IntelliJ's "Rename" refactoring (Shift+F6) to handle packages and classes safely.
3.  **Global Search**: Search for remaining string literals of "Analytics" in comments, logs, and documentation.
4.  **Verification**: Run `./gradlew test` to ensure all mappings and repository logic still hold.
5.  **Documentation Update**: Manually update markdown files.
