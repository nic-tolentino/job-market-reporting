# Frontend Deployment Guide

This document outlines how to configure, build, and deploy the tech market reporting frontend.

## Environment Configuration

The frontend uses Vite environment variables. These are typically managed via `.env` files.

### Key Variables

| Variable | Description |
| :--- | :--- |
| `VITE_API_URL` | The base URL for the backend API (e.g., `http://localhost:8080/api` or the Cloud Run URL). |
| `VITE_FORCE_MOCK_DATA` | Set to `true` to bypass the API and use local mock data defined in `src/lib/mockData.ts`. |

### Local Development Setup

To configure where your local frontend points to, use a `.env.local` file (this file is ignored by git):

#### Option A: Pointing to Local Backend
Use this when you are making changes to both frontend and backend.
```bash
VITE_API_URL=http://localhost:8080/api
VITE_FORCE_MOCK_DATA=false
```

#### Option B: Pointing to Production/Remote Backend
Use this when you want to work on the UI with "real" data without running the backend locally.
```bash
VITE_API_URL=https://tech-market-backend-181692518949.australia-southeast1.run.app/api
VITE_FORCE_MOCK_DATA=false
```

#### Option C: Pure Mock Mode
Use this for UI/styling work when no internet or backend is available.
```bash
VITE_FORCE_MOCK_DATA=true
```

> [!TIP]
> You can also toggle mock mode without restarting the server by running `localStorage.setItem('USE_MOCK_DATA', 'true')` in your browser's developer console and refreshing.

---

## Build and Preview

### 1. Build for Production
Pushing any changes to the main branch will trigger a new build and deployment to Vercel.

### 2. Preview Production Build
It's always recommended to test the production bundle locally before deploying.
```bash
npm run preview
```

---

## Deployment Targets

### Vercel (Recommended)
The project is already instrumented with Vercel Analytics. Deployment is typically handled via the Vercel CLI or GitHub integration.

**Using Vercel CLI:**
1. Install: `npm i -g vercel`
2. Link: `vercel link`
3. Deploy: `vercel --prod`

**Environment Variables in Vercel:**
Ensure you add `VITE_API_URL` to your Vercel project settings under "Environment Variables" so the production build knows where to find the API. 
With a value of `https://tech-market-backend-181692518949.australia-southeast1.run.app/api`

---

## Troubleshooting

### API Connectivity Issues
- **CORS Errors**: If you see CORS errors when pointing to local backend, ensure the Spring Boot backend is running with the `local` profile (which typically enables CORS for `http://localhost:5173`).
- **Mixed Content**: If you deploy the frontend to HTTPS (like Vercel) but point `VITE_API_URL` to an `http` address, the browser will block the requests. Always use `https` for remote APIs.
