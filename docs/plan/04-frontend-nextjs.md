# Phase 4: Frontend (Next.js + TypeScript + Tailwind)

## 1. Phase Summary

## 2. Architectural Decisions

### 2.1 App Router

### 2.2 Server Components by Default

### 2.3 Data Fetching Strategy

### 2.4 State Management

### 2.5 Styling

## 3. Page Map

## 4. Project Setup

### 4.1 Create Project

### 4.2 Additional Dependencies

### 4.3 shadcn/ui Setup

### 4.4 TypeScript Strict Mode

## 5. Project Structure

## 6. Environment & Config

### 6.1 .env.example

### 6.2 .env.local

### 6.3 next.config.ts

## 7. Type Definitions (Synced with API)

### 7.1 src/lib/types/api.ts

## 8. API Client Layer

### 8.1 src/lib/api/client.ts

### 8.2 src/lib/api/salons.ts

### 8.3 src/lib/api/districts.ts & services.ts

## 9. TanStack Query Setup

### 9.1 src/lib/providers/query-provider.tsx

### 9.2 src/lib/hooks/use-salons.ts

### 9.3 src/lib/hooks/use-update-salon.ts

## 10. Authentication

### 10.1 Strategy

### 10.2 src/app/api/auth/login/route.ts

### 10.3 src/app/api/auth/logout/route.ts

### 10.4 src/middleware.ts

### 10.5 src/lib/auth/session.ts

## 11. Layout & Global UI

### 11.1 src/app/layout.tsx

### 11.2 src/components/layout/header.tsx

## 12. Listing Page

### 12.1 src/app/page.tsx

### 12.2 src/components/salons/salon-card.tsx

### 12.3 src/components/salons/salon-list.tsx

### 12.4 src/components/salons/salon-filters.tsx

### 12.5 src/components/layout/pagination.tsx

## 13. Detail Page

### 13.1 src/app/salons/[id]/page.tsx

## 14. Edit Page (Form)

### 14.1 Zod Schema

### 14.2 src/app/salons/[id]/edit/page.tsx

### 14.3 src/components/salons/salon-edit-form.tsx

## 15. Admin Login Page

### 15.1 src/app/login/page.tsx

## 16. Component Library (shadcn/ui)

### 16.1 Common Components

## 17. Loading & Error States

### 17.1 src/app/loading.tsx

### 17.2 src/components/salons/salon-list-skeleton.tsx

### 17.3 src/app/error.tsx

### 17.4 src/app/not-found.tsx

## 18. Accessibility

## 19. Testing

### 19.1 Vitest Config

### 19.2 Component Test Example

### 19.3 Playwright E2E Example

## 20. Performance

### 20.1 Optimizations Applied

### 20.2 Lighthouse Score Targets

### 20.3 Bundle Analysis

## 21. Verification

### 21.1 Start Backend

### 21.2 Start Frontend

### 21.3 Smoke Test Checklist

## 22. Interview Questions

## 23. Definition of Done
