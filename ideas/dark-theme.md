# Dark Theme Implementation Plan

## Current State

The frontend uses **Tailwind CSS v4** (zero-config, `@tailwindcss/vite` 4.2.1) with hardcoded light-mode color classes scattered across components and pages. There is no theming abstraction — colors like `bg-white`, `text-slate-900`, `border-gray-200` are used directly in JSX.

### Key Color Patterns in Use

| Role | Current Classes | Approx. Count |
|------|----------------|---------------|
| Page background | `bg-[#F9FAFB]` (body), `bg-slate-50` | ~21 |
| Card/surface | `bg-white` | ~41 |
| Primary text | `text-slate-900` | ~61 |
| Secondary text | `text-gray-600`, `text-gray-500` | ~30 |
| Muted text | `text-gray-400`, `text-slate-400` | ~20 |
| Borders | `border-gray-200`, `border-gray-100` | ~45 |
| Hover states | `hover:bg-gray-50`, `hover:bg-blue-50` | ~20 |
| Accent | `text-blue-600`, `bg-blue-50` | ~25 |

### Files That Need Changes

**UI primitives (highest leverage — change once, propagate everywhere):**
- `components/ui/Card.tsx` — `bg-white`, `border-gray-200`
- `components/ui/Badge.tsx` — variant color maps (slate/blue/purple/emerald/gray) + `TechBadge` inline colors
- `components/ui/Typography.tsx` — `text-slate-900`, `text-gray-400`
- `components/ui/Dropdown.tsx` — `bg-white`, `border-gray-200`, hover/active states
- `components/ui/SimplePager.tsx` — button backgrounds and borders

**Layout (`components/layout/`):**
- `Navbar.tsx` — `bg-white`, `border-gray-200`, text colors
- `Footer.tsx` — background, text, and link colors

**Common components (`components/common/`):**
- `SearchBox.tsx` — input field, dropdown results, `ring-gray-300`
- `Feedback.tsx` — modal, form fields, buttons
- `SubmitResourceModal.tsx` — modal overlay, form, input fields
- `ErrorState.tsx` — background, text
- `CompanyLogo.tsx` — fallback `bg-white`
- `TechBadge.tsx` — already uses dark colors (`bg-neutral-800`), needs *light mode* fix — see [Special Considerations](#techbadgetsx-already-dark)
- `PageLoader.tsx` — loading state

**Tech-specific (`components/tech/`):**
- `ResourceCard.tsx` — card, upvote button, link styles
- `ResourceModal.tsx` — full modal overlay, items, header
- `SpotlightSection.tsx` — card backgrounds, heading
- `LearnTab.tsx` — CTA banner gradient, CTA button
- `CommunityTab.tsx` — CTA banner gradient, CTA button
- `MarketTab.tsx` — chart tooltips (inline `contentStyle`)

**Pages (`pages/`):**
- `LandingPage.tsx` — hero gradient, stat cards, chart `contentStyle`
- `TechDetailsPage.tsx` — header, tabs, icon container
- `CompanyProfilePage.tsx` — header, stats, tab toggle styles
- `JobPage.tsx` — job detail layout, sidebar cards
- `TransparencyPage.tsx` — content cards
- `ContactPage.tsx`, `PrivacyPolicyPage.tsx`, `TermsPage.tsx` — prose/content styling

**CSS:**
- `index.css` — body `bg-[#F9FAFB] text-slate-900`

---

## Strategy: Semantic Tokens via `@theme` + CSS Custom Properties

### Why Not Just `dark:` Everywhere?

Adding `dark:bg-slate-800 dark:text-white dark:border-slate-700` to every single class string would:
- Roughly double the length of every `className` prop
- Be extremely error-prone (miss one and you get a white flash)
- Be impossible to maintain consistently

### Recommended Approach

Define semantic color tokens as CSS custom properties. Map them into the Tailwind theme with the **`@theme` directive** (Tailwind v4). Toggling dark mode then only requires changing the variable values — no component changes needed after the initial migration.

#### Step 1: Define the Design Tokens in `index.css`

The `@theme` directive tells Tailwind to generate utilities from the declared variables. For example, `--color-card: ...` automatically creates `bg-card`, `text-card`, etc. We use an indirection layer (`var(--theme-*)`) so that the `.dark` selector can swap values without touching `@theme`.

```css
@import "tailwindcss";

@theme {
  /* Surfaces */
  --color-page:      var(--theme-page);
  --color-card:      var(--theme-card);
  --color-elevated:  var(--theme-elevated);
  --color-inset:     var(--theme-inset);
  --color-surface-hover: var(--theme-hover);

  /* Text */
  --color-primary:   var(--theme-text-primary);
  --color-secondary: var(--theme-text-secondary);
  --color-muted:     var(--theme-text-muted);
  --color-inverted:  var(--theme-text-inverted);

  /* Borders */
  --color-border:        var(--theme-border);
  --color-border-subtle: var(--theme-border-subtle);

  /* Accent */
  --color-accent:        var(--theme-accent);
  --color-accent-hover:  var(--theme-accent-hover);
  --color-accent-subtle: var(--theme-accent-subtle);

  /* Shadows */
  --shadow-theme-sm: var(--theme-shadow-sm);
  --shadow-theme-md: var(--theme-shadow-md);
}

@layer base {
  :root {
    --theme-page:       #F9FAFB;
    --theme-card:       #FFFFFF;
    --theme-elevated:   #F8FAFC;
    --theme-inset:      #F1F5F9;
    --theme-hover:      #F1F5F9;

    --theme-text-primary:   #0F172A;
    --theme-text-secondary: #475569;
    --theme-text-muted:     #94A3B8;
    --theme-text-inverted:  #FFFFFF;

    --theme-border:        #E2E8F0;
    --theme-border-subtle: #F1F5F9;

    --theme-accent:        #2563EB;
    --theme-accent-hover:  #1D4ED8;
    --theme-accent-subtle: #EFF6FF;

    --theme-shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
    --theme-shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  }

  .dark {
    --theme-page:       #0F172A;
    --theme-card:       #1E293B;
    --theme-elevated:   #1E293B;
    --theme-inset:      #334155;
    --theme-hover:      #334155;

    --theme-text-primary:   #F1F5F9;
    --theme-text-secondary: #94A3B8;
    --theme-text-muted:     #64748B;
    --theme-text-inverted:  #0F172A;

    --theme-border:        #334155;
    --theme-border-subtle: #1E293B;

    --theme-accent:        #3B82F6;
    --theme-accent-hover:  #60A5FA;
    --theme-accent-subtle: rgba(59, 130, 246, 0.1);

    --theme-shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.3);
    --theme-shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.4);
  }

  body {
    background-color: var(--theme-page);
    color: var(--theme-text-primary);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
  }
}
```

> **Note on naming**: We avoid `--color-default` for borders because Tailwind already has a built-in `border-default` concept. Using `--color-border` generates `border-border` which is slightly unusual but unambiguous and avoids collisions. `--color-surface-hover` avoids collision with Tailwind's `hover:` modifier prefix.

#### Step 2: Migrate Components to Use Tokens

Since we declared variables inside `@theme`, Tailwind v4 automatically generates first-class utility classes. No arbitrary value syntax needed:

**Before (`Card.tsx`):**
```tsx
<div className="bg-white rounded-xl border border-gray-200 shadow-sm">
```

**After:**
```tsx
<div className="bg-card rounded-xl border border-border shadow-theme-sm">
```

**Before (`Typography.tsx`):**
```tsx
<h1 className="text-3xl font-bold text-slate-900">
```

**After:**
```tsx
<h1 className="text-3xl font-bold text-primary">
```

Clean, readable, and theme-aware.

---

## Theme Toggle Mechanism

### Recommended: `next-themes`

Despite its name, `next-themes` works perfectly with Vite + React (no Next.js required). It handles:
- System preference detection (`prefers-color-scheme`)
- `localStorage` persistence
- Cross-tab syncing
- Flash-of-Incorrect-Theme (FOIT) prevention via an injected blocking script
- SSR safety (not applicable here, but good to have)

```bash
npm install next-themes
```

Wire it into `main.tsx` alongside the existing `QueryClientProvider`:

```tsx
// src/main.tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from 'next-themes';
import './index.css';
import App from './App.tsx';

const queryClient = new QueryClient();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
);
```

### Toggle Button (Navbar)

Add a sun/moon icon button to `Navbar.tsx` using `useTheme()` from `next-themes` and icons from `lucide-react` (already a dependency):

```tsx
import { useTheme } from 'next-themes';
import { Sun, Moon, Monitor } from 'lucide-react';

function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  const next = theme === 'light' ? 'dark' : theme === 'dark' ? 'system' : 'light';
  const Icon = theme === 'dark' ? Moon : theme === 'light' ? Sun : Monitor;

  return (
    <button
      onClick={() => setTheme(next)}
      className="p-2 rounded-lg text-secondary hover:bg-surface-hover transition-colors"
      aria-label={`Switch to ${next} theme`}
    >
      <Icon className="h-4 w-4" />
    </button>
  );
}
```

Place this next to the country Dropdown and the "About" link in the Navbar's right-side `<div>`.

---

## Migration Order

Work bottom-up from primitives to pages, testing each layer before moving on.

| Phase | Files | Description |
|-------|-------|-------------|
| **1. Foundation** | `index.css`, `main.tsx` | Define tokens, install `next-themes`, wire provider |
| **2. UI Primitives** | `Card`, `Badge`, `Typography`, `Dropdown`, `SimplePager` | Migrate the 5 shared UI components |
| **3. Layout** | `Navbar`, `Footer` | Structural chrome + theme toggle button |
| **4. Common** | `SearchBox`, `Feedback`, `SubmitResourceModal`, `ErrorState`, `CompanyLogo`, `TechBadge`, `PageLoader` | Shared interactive components |
| **5. Tech Components** | `ResourceCard`, `ResourceModal`, `SpotlightSection`, `LearnTab`, `CommunityTab`, `MarketTab` | Resource directory UI |
| **6. Pages** | `LandingPage`, `TechDetailsPage`, `CompanyProfilePage`, `JobPage`, `TransparencyPage`, `ContactPage`, `PrivacyPolicyPage`, `TermsPage` | Full page layouts |
| **7. Charts** | `useThemeColors` hook, `LandingPage`, `MarketTab` | Recharts tooltip/grid/axis theming |
| **8. Polish** | Scrollbar styling, selection color, focus rings, transitions | Final visual consistency pass |

---

## Special Considerations

### Recharts (Charts)

Recharts accepts inline style objects for `contentStyle`, `stroke`, `fill`, etc. — not Tailwind classes. The two files that need changes are:
- `LandingPage.tsx` (line ~147) — `BarChart` tooltip
- `MarketTab.tsx` (line ~83) — `PieChart` tooltip

Both currently hardcode `border: '1px solid #E5E7EB'`. Create a small hook to resolve CSS variable values at render time:

```tsx
// src/hooks/useThemeColors.ts
import { useMemo } from 'react';
import { useTheme } from 'next-themes';

export function useChartStyles() {
  const { resolvedTheme } = useTheme();

  return useMemo(() => {
    const style = getComputedStyle(document.documentElement);
    const get = (v: string) => style.getPropertyValue(v).trim();

    return {
      tooltipStyle: {
        backgroundColor: get('--theme-card'),
        borderColor: get('--theme-border'),
        borderRadius: '8px',
        border: `1px solid ${get('--theme-border')}`,
        boxShadow: get('--theme-shadow-md'),
        color: get('--theme-text-primary'),
      },
      gridStroke: get('--theme-border-subtle'),
      axisTickFill: get('--theme-text-muted'),
    };
    // Re-compute when theme changes
  }, [resolvedTheme]);
}
```

Usage in components:
```tsx
const { tooltipStyle, gridStroke } = useChartStyles();
// ...
<Tooltip contentStyle={tooltipStyle} />
<CartesianGrid stroke={gridStroke} />
```

### Badge Variant Colors

`Badge.tsx` uses a `variantStyles` map with per-variant Tailwind classes (`bg-blue-50 text-blue-700 border-blue-100`, etc.). These are *colorful* badges — not surface/text tokens. There are two approaches:

1. **Keep hardcoded light/dark pairs** using `dark:` on each variant (acceptable since there are only 5 variants in one place):
   ```ts
   blue: 'bg-blue-50 text-blue-700 border-blue-100 dark:bg-blue-500/10 dark:text-blue-300 dark:border-blue-500/20',
   ```

2. **Define per-variant CSS variables** in `@theme` (more work, cleaner long-term).

Option 1 is simpler and recommended since the Badge is a single file.

### TechBadge.tsx (Already Dark)

`components/common/TechBadge.tsx` already uses dark-mode-like colors (`bg-neutral-800`, `text-neutral-300`). This will look broken in light mode today and invisible in dark mode tomorrow. During migration, swap its palette to use the semantic tokens or at minimum add light/dark variant handling.

### Images & SVG Icons

- Tech SVG icons in `public/icons/tech/` are monochrome — apply `dark:invert` or use CSS `filter: invert(1)` in dark mode.
- Full-color images (logos, avatars) need no changes.
- The hero gradient on `LandingPage.tsx` (`from-[#EBF4FF]`) needs a dark variant (e.g., `from-slate-900`).

### CTA Buttons (LearnTab / CommunityTab)

Both tabs have white CTA buttons with `bg-white text-blue-600` and intricate hover shadows. These need attention in dark mode — swap to `bg-card text-accent` using the tokens.

### Third-Party Components

- **Recharts**: Handled via `useChartStyles()` hook above.
- **Lucide icons**: Inherit `currentColor` by default — no changes needed.

### Accessibility

- Maintain WCAG AA contrast ratios (4.5:1 for body text, 3:1 for large text and UI elements).
- The dark palette (slate-100 `#F1F5F9` on slate-900 `#0F172A`) provides ~15.4:1 contrast — well above AA.
- Verify accent-on-dark contrast: blue-500 `#3B82F6` on slate-800 `#1E293B` = ~4.8:1 ✓

### Transition Smoothness

Add a global CSS transition so theme switches aren't jarring:

```css
/* In index.css, inside @layer base */
*,
*::before,
*::after {
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.15s ease;
}
```

> ⚠️ This may interfere with existing hover/animation transitions. An alternative is to add the transition only while switching themes (toggle a `.theme-transitioning` class on `<html>` for 300ms).

---

## Estimated Effort

| Phase | Estimated Time |
|-------|---------------|
| Foundation (tokens + provider) | 30 min |
| UI primitives | 1 hour |
| Layout + common components | 1.5 hours |
| Tech components + pages | 3 hours |
| Charts + polish | 1 hour |
| **Total** | **~7 hours** |

The majority of the work is mechanical find-and-replace of color classes — not architecturally complex, but tedious. The `@theme` token system pays for itself by making future color changes (brand refresh, high-contrast mode, etc.) trivial.

---

## Follow-Up Improvements (Post-Implementation Audit)

> **Audit date**: 5 March 2026. Phases 1–7 are complete and the build passes with zero errors. The items below are remaining polish and refinements from Phase 8 and edge cases discovered during the audit.

### 1. Theme Transition Smoothness (Phase 8 — Not Yet Implemented)

The plan called for a global CSS transition so theme switches aren't jarring:

```css
*, *::before, *::after {
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.15s ease;
}
```

This was **intentionally deferred** because it can interfere with hover animations and page transitions. The safer alternative is to toggle a `.theme-transitioning` class on `<html>` for ~300ms only during theme switches (via the `next-themes` `onThemeChange` callback or a wrapper around `setTheme`). This limits the transition blast radius.

**Recommendation**: Implement the `.theme-transitioning` approach. Low risk, high visual impact.

---

### 2. Scrollbar Styling (Phase 8 — Not Yet Implemented)

Dark mode scrollbars still use the browser default (light grey on white), which looks jarring on dark backgrounds. Add themed scrollbar styles:

```css
@layer base {
  /* Webkit (Chrome, Edge, Safari) */
  ::-webkit-scrollbar { width: 8px; height: 8px; }
  ::-webkit-scrollbar-track { background: var(--theme-page); }
  ::-webkit-scrollbar-thumb {
    background: var(--theme-border);
    border-radius: 4px;
  }
  ::-webkit-scrollbar-thumb:hover { background: var(--theme-text-muted); }

  /* Firefox */
  * { scrollbar-color: var(--theme-border) var(--theme-page); scrollbar-width: thin; }
}
```

**Recommendation**: Add to `index.css`. Very noticeable improvement in dark mode.

---

### 3. Text Selection Color (Phase 8 — Not Yet Implemented)

The default browser text selection highlight (blue on white) clashes in dark mode. Add:

```css
::selection {
  background-color: var(--theme-accent-subtle);
  color: var(--theme-text-primary);
}
```

**Recommendation**: Quick one-liner in `index.css`.

---

### 4. Recharts `Cell fill` Hardcoded Hex Colors

The `useChartStyles` hook correctly themes tooltips, grid lines, and axis ticks. However, the actual **bar/pie chart segment colors** are still hardcoded hex values:

- `LandingPage.tsx` — Bar cells use `'#2563EB'` (first bar) and `'#94A3B8'` (others)
- `MarketTab.tsx` — Pie cells use `COLORS = ['#f563EB', '#4F46E5', '#10B981', '#F59E0B']`

These are **brand/data colors**, not surface colors, so they're acceptable in both themes. However, the secondary bar color (`#94A3B8`) may lack contrast against the dark card background (`#1E293B`).

**Recommendation**: Test visually in dark mode. If contrast is poor, swap `#94A3B8` for a lighter value (e.g., `#CBD5E1`) in dark mode via the `useChartStyles` hook. The pie chart colors are vibrant enough to work in both themes.

---

### 5. Recharts Tooltip Cursor Color

`LandingPage.tsx` uses `cursor={{ fill: 'var(--theme-hover)' }}` which correctly references the CSS variable. ✅ No action needed.

---

### 6. SVG Dropdown Chevron in Modals

`ResourceModal.tsx` and `SubmitResourceModal.tsx` embed an inline SVG chevron for `<select>` elements via a `data:image/svg+xml` URL. The SVG stroke color is hardcoded to `#6b7280` (gray-500), which works in light mode but may be invisible in dark mode on the `bg-elevated` background.

**Recommendation**: Replace the hardcoded SVG stroke with a lighter color for dark mode, or use a CSS `filter: invert(1)` on the `background-image` in dark mode. Alternatively, replace the native `<select>` with the existing `Dropdown` component which is already themed.

---

### 7. `ResourceCard.tsx` — `bg-white/20` on Featured Image Overlay

Line ~103 in `ResourceCard.tsx` uses `bg-white/20 backdrop-blur-md` for an overlay on the featured card image. This is a **translucent glass effect** on top of an image — it's intentional and looks fine in both themes. ✅ No action needed.

---

### 8. `ContactPage.tsx` — Intentionally Dark CTA Card

The "Open Source & Community" card at the bottom uses `bg-slate-900 dark:bg-slate-800`, `text-white`, `bg-white text-slate-900` (for the GitHub button), and `text-slate-400`. These are **intentionally dark** in both themes — it's a hero-style CTA that should remain dark. However, the slight `dark:bg-slate-800` adjustment could be refined to `bg-slate-800` since it's already a dark card and doesn't need to change much between themes.

**Recommendation**: Consider whether this card should remain intentionally dark in both themes (current approach) or blend with the dark theme background. Current approach is fine.

---

### 9. Focus Ring Consistency

Focus rings across the app use a mix of:
- `focus:ring-accent/20` (ResourceModal, SearchBox)
- `focus:ring-indigo-500/20` (SubmitResourceModal)
- `focus:ring-4 focus:ring-accent/10` (Feedback textarea)

**Recommendation**: Standardize all focus rings to `focus:ring-2 focus:ring-accent/20` for consistency. The `indigo` rings in `SubmitResourceModal` should align with the accent token unless the indigo branding is intentional for that specific modal.

---

### 10. `prose` Dark Mode

`JobPage.tsx`, `PrivacyPolicyPage.tsx`, and `TermsPage.tsx` correctly use `dark:prose-invert` alongside `prose prose-slate`. ✅ Implemented correctly.

`TransparencyPage.tsx` uses `prose prose-slate dark:prose-invert` on the Source Disclosure section. ✅ Correct.

---

### Summary Table

| Item | Priority | Effort | Status |
|------|----------|--------|--------|
| Theme transition smoothness | Medium | 30 min | Not started |
| Scrollbar styling | Medium | 15 min | Not started |
| Text selection color | Low | 5 min | Not started |
| Recharts bar contrast check | Low | 15 min | Needs visual testing |
| SVG dropdown chevron | Low | 20 min | Not started |
| Focus ring standardization | Low | 15 min | Not started |
| `ContactPage` dark CTA review | Cosmetic | 5 min | Acceptable as-is |
| **Tech icon brand tinting** | **High** | **1.5 hours** | **Not started** |

---

### 11. Tech Icon Brand-Color Tinting

#### Current State

All 61 tech icons in `public/icons/tech/` are **monochrome Simple Icons** — single `<path>` elements with no `fill` attribute (they render as black by default). Currently used in:
- `TechDetailsPage.tsx` — rendered as `<img>` tags with `dark:invert` to flip black → white in dark mode

The current approach (`dark:invert`) makes them visible in dark mode but they remain **unbranded** — every icon is the same black/white regardless of the technology.

#### Goal

Tint each tech icon with its official **brand color** in light mode (e.g., React → `#61DAFB`, Python → `#3776AB`, Kotlin → `#7F52FF`), and ensure they remain legible and attractive in dark mode.

#### Approach: CSS `filter` Tinting via `<img>` + Brand Color Map

Since the SVGs are loaded as `<img>` tags (not inline), we can't use `fill` directly. Instead, use a combination of techniques:

**Option A — Inline SVG React Components (Recommended)**

Convert the `<img>` tag to inline SVG (via a React component or `dangerouslySetInnerHTML`) so that `fill` can be set directly with CSS. This gives full control:

```tsx
// src/constants/techBrandColors.ts
export const TECH_BRAND_COLORS: Record<string, { light: string; dark: string }> = {
  react:          { light: '#61DAFB', dark: '#61DAFB' },
  python:         { light: '#3776AB', dark: '#5A9BD5' },
  kotlin:         { light: '#7F52FF', dark: '#A380FF' },
  android:        { light: '#34A853', dark: '#5BC47C' },
  ios:            { light: '#000000', dark: '#FFFFFF' },
  javascript:     { light: '#F7DF1E', dark: '#F7DF1E' },
  typescript:     { light: '#3178C6', dark: '#5A9BD5' },
  swift:          { light: '#F05138', dark: '#F47B68' },
  'ruby':         { light: '#CC342D', dark: '#E05A53' },
  'ruby-on-rails':{ light: '#D30001', dark: '#E54B4B' },
  vue:            { light: '#4FC08D', dark: '#6DD5A4' },
  'next-js':      { light: '#000000', dark: '#FFFFFF' },
  nodejs:         { light: '#339933', dark: '#5BBF5B' },
  'node-js':      { light: '#339933', dark: '#5BBF5B' },
  'node':         { light: '#339933', dark: '#5BBF5B' },
  graphql:        { light: '#E10098', dark: '#FF4DB8' },
  aws:            { light: '#232F3E', dark: '#FF9900' },
  azure:          { light: '#0078D4', dark: '#4BA3E3' },
  gcp:            { light: '#4285F4', dark: '#6FA8F7' },
  kubernetes:     { light: '#326CE5', dark: '#6B9AEF' },
  terraform:      { light: '#844FBA', dark: '#A87DD4' },
  redis:          { light: '#DC382D', dark: '#E96A62' },
  mongodb:        { light: '#47A248', dark: '#6DC46E' },
  postgres:       { light: '#4169E1', dark: '#7A9AEF' },
  postgresql:     { light: '#4169E1', dark: '#7A9AEF' },
  mysql:          { light: '#4479A1', dark: '#6FA3C8' },
  docker:         { light: '#2496ED', dark: '#5BB5F5' },
  flask:          { light: '#000000', dark: '#FFFFFF' },
  'spring-boot':  { light: '#6DB33F', dark: '#8FCC6A' },
  scala:          { light: '#DC322F', dark: '#E96A67' },
  elixir:         { light: '#4B275F', dark: '#8A5DA0' },
  haskell:        { light: '#5D4F85', dark: '#8A7CB3' },
  perl:           { light: '#39457E', dark: '#6B75A8' },
  sass:           { light: '#CC6699', dark: '#E08AB3' },
  html:           { light: '#E34F26', dark: '#F07B5E' },
  ember:          { light: '#E04E39', dark: '#E97B6A' },
  ionic:          { light: '#3880FF', dark: '#6FA3FF' },
  jenkins:        { light: '#D24939', dark: '#E47A6E' },
  'gitlab-ci':    { light: '#FC6D26', dark: '#FD9A6B' },
  'travis-ci':    { light: '#3EAAAF', dark: '#6DC4C8' },
  tensorflow:     { light: '#FF6F00', dark: '#FF9A4D' },
  numpy:          { light: '#013243', dark: '#4DA6C8' },
  bigquery:       { light: '#669DF6', dark: '#8FBAF9' },
  databricks:     { light: '#FF3621', dark: '#FF7060' },
  spark:          { light: '#E25A1C', dark: '#EC8555' },
  airflow:        { light: '#017CEE', dark: '#4DA6F5' },
  cassandra:      { light: '#1287B1', dark: '#4DADD0' },
  couchbase:      { light: '#EA2328', dark: '#F06065' },
  mariadb:        { light: '#003545', dark: '#4D8FA5' },
  rabbitmq:       { light: '#FF6600', dark: '#FF944D' },
  lambda:         { light: '#FF9900', dark: '#FFB74D' },
  redshift:       { light: '#8C4FFF', dark: '#AD80FF' },
  chef:           { light: '#F09820', dark: '#F5B560' },
  shell:          { light: '#4EAA25', dark: '#7ACC5E' },
  c:              { light: '#A8B9CC', dark: '#C4D0DC' },
  dotnet:         { light: '#512BD4', dark: '#7E5CE0' },
  net:            { light: '#512BD4', dark: '#7E5CE0' },
  'asp-net':      { light: '#512BD4', dark: '#7E5CE0' },
  'react-native': { light: '#61DAFB', dark: '#61DAFB' },
  'sql-server':   { light: '#CC2927', dark: '#E06260' },
  sql:            { light: '#4479A1', dark: '#6FA3C8' },
  ubuntu:         { light: '#E95420', dark: '#F07D56' },
};
```

Then create a `TechIcon` component:

```tsx
// src/components/common/TechIcon.tsx
import { useEffect, useState } from 'react';
import { useTheme } from 'next-themes';
import { TECH_BRAND_COLORS } from '../../constants/techBrandColors';

interface TechIconProps {
  techId: string;
  className?: string;
  fallback?: string; // fallback letter
}

export function TechIcon({ techId, className = 'w-12 h-12', fallback }: TechIconProps) {
  const { resolvedTheme } = useTheme();
  const [svgContent, setSvgContent] = useState<string | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    fetch(`/icons/tech/${techId.toLowerCase()}.svg`)
      .then(r => r.ok ? r.text() : Promise.reject())
      .then(setSvgContent)
      .catch(() => setError(true));
  }, [techId]);

  if (error || !svgContent) {
    return fallback ? (
      <span className={className}>{fallback}</span>
    ) : null;
  }

  const colors = TECH_BRAND_COLORS[techId.toLowerCase()];
  const fill = colors
    ? (resolvedTheme === 'dark' ? colors.dark : colors.light)
    : (resolvedTheme === 'dark' ? '#F1F5F9' : '#0F172A'); // fallback to semantic primary

  // Inject fill into the SVG
  const tinted = svgContent.replace('<svg ', `<svg fill="${fill}" `);

  return (
    <div
      className={className}
      dangerouslySetInnerHTML={{ __html: tinted }}
    />
  );
}
```

**Option B — CSS `filter` Hue Rotation (Simpler, Less Accurate)**

Keep `<img>` and apply CSS filters to approximate the brand color. This is simpler but gives less precise color control and doesn't work well with certain hues.

> **Recommendation**: Option A (inline SVG component) is strongly recommended for precise brand color control and clean dark mode support.

#### Dark Mode Considerations

- **Vibrant colors** (React cyan, Kotlin purple, Android green) work well in both themes — use the same or slightly brightened values in dark mode.
- **Dark/black icons** (iOS, Next.js, Flask, AWS) must flip to white or a light variant in dark mode, otherwise they'll be invisible.
- **Yellow icons** (JavaScript `#F7DF1E`) work in both themes since they're naturally bright on dark backgrounds and contrasty on light backgrounds.
- The `dark` values in the map above are pre-lightened (~15-20% brighter) to maintain visibility against the `#1E293B` card background.

#### Where to Apply

1. **`TechDetailsPage.tsx`** — Replace the current `<img>` + `dark:invert` with `<TechIcon>`.
2. **Future use** — The `TechIcon` component can also be used in `ResourceCard`, `TechBadge`, search results, or anywhere a tech logo appears.

#### Implementation Steps

1. Create `src/constants/techBrandColors.ts` with the color map above.
2. Create `src/components/common/TechIcon.tsx` with the inline SVG approach.
3. Update `TechDetailsPage.tsx` to replace the `<img>` + `dark:invert` with `<TechIcon>`.
4. Visually test all 61 icons in both light and dark mode.
5. Optionally add a loading skeleton while the SVG fetches.
