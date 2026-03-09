# Job Market Reporting - Frontend

This is the React frontend for the DevAssembly project, providing a professional, data-heavy dashboard for tech market insights in Australia, New Zealand, and Spain.

## 🏗️ Architecture
- **Framework:** React 18+ (bootstrapped with Vite and TypeScript)
- **Styling:** Tailwind CSS (built for a clean, levels.fyi-inspired aesthetic with dark mode support)
- **State Management:** Zustand (for global app state, such as selected country and theme persistence)
- **Routing:** React Router v6
- **Data Visualization:** Recharts (for timeline and bar charts)

## 🌎 Country Context
The frontend is deeply integrated with a multi-country architecture:
- Users can toggle between target countries (e.g., New Zealand vs. Australia) via the main navigation menu.
- The `useAppStore` (Zustand) holds the `selectedCountry` and persists it to `localStorage`.
- All data-fetching hooks automatically append `?country=[CODE]` to backend API calls.
- UI components dynamically render country-specific community resources and insights based on this global state.

## 🚀 Getting Started

### Prerequisites
- Node.js 18+
- Backend running locally on `localhost:8080` (or update `VITE_API_URL`)

### Installation
```bash
npm install
```

### Local Development
```bash
npm run dev
```

### Production Build
```bash
npm run build
```
The production site is typically hosted on Vercel. Ensure `VITE_API_URL` is set in the Vercel environment variables to point to the Cloud Run backend (e.g., `https://tech-market-backend-xxx.a.run.app/api`).
