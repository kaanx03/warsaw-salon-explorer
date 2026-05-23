# Phase 4: Frontend (Next.js 16 + TypeScript + Tailwind)

> **Hedef:** Faz 3'te yazdığımız REST API'yi tüketen, production-grade bir Next.js uygulaması. Server components, type-safe API client, TanStack Query, accessibility, loading/error states, optimistic updates ve JWT auth — hepsi enterprise standardında.

---

## 📋 İçindekiler

1. [Faz Özeti](#1-faz-özeti)
2. [Mimari Kararlar](#2-mimari-kararlar)
3. [Sayfa Haritası](#3-sayfa-haritası)
4. [Proje Kurulumu](#4-proje-kurulumu)
5. [Klasör Yapısı](#5-klasör-yapısı)
6. [Environment & Config](#6-environment--config)
7. [Type Definitions (API ile Senkron)](#7-type-definitions-api-ile-senkron)
8. [API Client Layer](#8-api-client-layer)
9. [TanStack Query Setup](#9-tanstack-query-setup)
10. [Authentication](#10-authentication)
11. [Layout & Global UI](#11-layout--global-ui)
12. [Listing Page](#12-listing-page)
13. [Detail Page](#13-detail-page)
14. [Edit Page (Form)](#14-edit-page-form)
15. [Admin Login Page](#15-admin-login-page)
16. [Component Library (shadcn/ui)](#16-component-library-shadcnui)
17. [Loading & Error States](#17-loading--error-states)
18. [Accessibility](#18-accessibility)
19. [Testing](#19-testing)
20. [Performance](#20-performance)
21. [Doğrulama](#21-doğrulama)
22. [Mülakat Soruları](#22-mülakat-soruları)
23. [Definition of Done](#23-definition-of-done)

---

## 1. Faz Özeti

**Bu fazın sonunda elimizde olacaklar:**

- Next.js 16 App Router projesi (TypeScript strict mode)
- Server-side rendered listing page (SEO-friendly)
- Client-side filtreleme + URL state sync
- Detail page (server component)
- Admin edit form (react-hook-form + zod)
- JWT auth (httpOnly cookie, Server Action ile)
- TanStack Query mutations + optimistic updates
- shadcn/ui ile tutarlı component library
- Skeleton loading + error boundaries
- Tailwind 4 + responsive design
- WCAG 2.1 AA accessibility compliance
- Vitest unit + Playwright e2e (örnekler)

**Bu fazda YAPMAYACAĞIZ:**

- Container'ization (Faz 5)
- CI/CD pipeline (Faz 6)
- Production deploy

---

## 2. Mimari Kararlar

### 2.1 App Router (Pages Router DEĞİL)

Next.js 16'da App Router default ve stable. Server Components, streaming, parallel routes — modern feature'lar burada.

### 2.2 Server Components by default

Listing ve detail page **server component**. Faydaları:
- Bundle'a JavaScript inmesi azalır
- API çağrısı browser → backend yerine server → backend (daha hızlı, network hop yok)
- SEO için HTML hazır gelir
- API key'i (varsa) server-side'da kalır

**Client component sadece** interaktivite gereken yerlerde (`'use client'`):
- Filter sidebar
- Edit form
- Login form
- Pagination kontrolleri (URL state)

### 2.3 Data fetching stratejisi

| Veri | Yöntem | Sebebi |
|------|--------|--------|
| Salon listesi (initial) | Server Component + fetch | SEO, hızlı first paint |
| Salon detayı | Server Component + fetch | SEO, paylaşılabilir link |
| Filtre değişimi | Client + TanStack Query | Anında reaktif UI |
| Update mutation | TanStack Query mutation | Optimistic update + invalidation |
| District/Service listesi | Server Component (cache: 1h) | Nadir değişir, edge cache |

### 2.4 State management

- **Server state:** TanStack Query (cache, refetch, invalidation)
- **URL state:** `useSearchParams` (filter, page) — paylaşılabilir, browser history
- **Form state:** react-hook-form
- **UI state:** local `useState`
- **Global client state:** **YOK** — Zustand/Redux gereksiz

### 2.5 Style

- **Tailwind CSS 4** — utility-first, JIT compile
- **shadcn/ui** — kopyalanabilir component'lar (npm dependency değil)
- **CSS variable'lar** — light/dark theme için hazır altyapı

---

## 3. Sayfa Haritası

| Route | Tip | İçerik |
|-------|-----|--------|
| `/` | Server | Salon listesi, filter sidebar, pagination |
| `/salons/[id]` | Server | Salon detayı, harita, "Edit" linki |
| `/salons/[id]/edit` | Client | Edit formu (auth gerekli) |
| `/login` | Client | Admin login |
| `/admin` | Server | Admin dashboard (audit log, ingest tetikle) |
| `/404` | Static | Not found page |
| `/error` | Client | Global error boundary |

### URL örnek state

```
/?district=mokotow&minRating=4.0&service=haircut&page=2&size=20&sort=rating&order=desc
```

---

## 4. Proje Kurulumu

### 4.1 Project oluştur

```bash
npx create-next-app@latest frontend \
  --typescript \
  --tailwind \
  --app \
  --src-dir \
  --import-alias "@/*" \
  --eslint
```

Sorulara cevaplar:
- TypeScript: Yes
- ESLint: Yes
- Tailwind: Yes (v4 yüklenir)
- src/ directory: Yes
- App Router: Yes
- Turbopack: Yes (build için)
- Customize import alias: Yes (`@/*`)

### 4.2 Ek dependency'ler

```bash
cd frontend

# Core
npm install @tanstack/react-query @tanstack/react-query-devtools
npm install zod react-hook-form @hookform/resolvers
npm install jose                  # JWT decode (server-side)
npm install ky                    # küçük HTTP client (fetch wrapper)

# UI
npm install lucide-react
npm install class-variance-authority clsx tailwind-merge
npm install sonner                # toast notifications
npm install react-loading-skeleton

# Dev
npm install -D @types/node prettier prettier-plugin-tailwindcss
npm install -D vitest @vitejs/plugin-react @testing-library/react @testing-library/jest-dom
npm install -D @playwright/test
```

### 4.3 shadcn/ui setup

```bash
npx shadcn@latest init
```

Konfig sorularına:
- Style: New York (veya Default)
- Base color: Slate
- CSS variables: Yes

İlk component'ları ekle:

```bash
npx shadcn@latest add button input label card badge dialog dropdown-menu select form skeleton table toast separator
```

### 4.4 TypeScript strict mode

`tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": false,
    "skipLibCheck": true,
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "forceConsistentCasingInFileNames": true,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "preserve",
    "incremental": true,
    "plugins": [{ "name": "next" }],
    "paths": { "@/*": ["./src/*"] }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx", ".next/types/**/*.ts"],
  "exclude": ["node_modules"]
}
```

---

## 5. Klasör Yapısı

```
frontend/
├── .env.local                       # ⚠️ gitignore'da
├── .env.example
├── next.config.ts
├── tsconfig.json
├── tailwind.config.ts
├── components.json                  # shadcn config
├── playwright.config.ts
├── vitest.config.ts
└── src/
    ├── app/
    │   ├── layout.tsx               # Root layout
    │   ├── page.tsx                 # / (listing)
    │   ├── loading.tsx              # Global loading
    │   ├── error.tsx                # Global error boundary
    │   ├── not-found.tsx
    │   ├── globals.css
    │   ├── salons/
    │   │   └── [id]/
    │   │       ├── page.tsx         # Detail
    │   │       ├── loading.tsx
    │   │       └── edit/
    │   │           └── page.tsx     # Edit form
    │   ├── login/
    │   │   └── page.tsx
    │   ├── admin/
    │   │   ├── layout.tsx           # Auth-protected layout
    │   │   └── page.tsx
    │   └── api/
    │       └── auth/
    │           ├── login/route.ts    # Server-side login
    │           └── logout/route.ts
    ├── components/
    │   ├── ui/                       # shadcn/ui
    │   ├── salons/
    │   │   ├── salon-card.tsx
    │   │   ├── salon-list.tsx
    │   │   ├── salon-detail.tsx
    │   │   ├── salon-edit-form.tsx
    │   │   └── salon-filters.tsx
    │   ├── layout/
    │   │   ├── header.tsx
    │   │   ├── footer.tsx
    │   │   └── pagination.tsx
    │   └── common/
    │       ├── rating-stars.tsx
    │       ├── price-level.tsx
    │       └── district-badge.tsx
    ├── lib/
    │   ├── api/
    │   │   ├── client.ts             # ky wrapper
    │   │   ├── salons.ts             # salon endpoints
    │   │   ├── districts.ts
    │   │   ├── services.ts
    │   │   └── auth.ts
    │   ├── auth/
    │   │   ├── session.ts            # cookie reader
    │   │   └── guards.ts
    │   ├── hooks/
    │   │   ├── use-salons.ts
    │   │   ├── use-salon.ts
    │   │   └── use-update-salon.ts
    │   ├── utils/
    │   │   ├── cn.ts                 # Tailwind merge
    │   │   ├── format.ts             # phone, rating, date
    │   │   └── url-params.ts
    │   ├── types/
    │   │   └── api.ts                # Type definitions
    │   ├── validators/
    │   │   ├── salon.schema.ts       # Zod
    │   │   └── auth.schema.ts
    │   └── providers/
    │       ├── query-provider.tsx
    │       └── theme-provider.tsx
    ├── middleware.ts                 # Auth + i18n
    └── e2e/
        └── salon-flow.spec.ts
```

---

## 6. Environment & Config

### 6.1 `.env.example`

```bash
# Public (client'a bundle'lanır)
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1

# Server-side only
INTERNAL_API_URL=http://localhost:8080/api/v1
# Production'da: http://backend:8080/api/v1 (Docker internal network)

# Session cookie
SESSION_COOKIE_NAME=salon_session
SESSION_COOKIE_DOMAIN=localhost
```

### 6.2 `.env.local` (gitignore'da)

Local development için kopyala:

```bash
cp .env.example .env.local
```

### 6.3 `next.config.ts`

```typescript
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,

  // Image optimization için Google Places photo'ları proxy'leniyor backend'den,
  // remote pattern gerekmiyor. Local backend görseli sunuyor.
  images: {
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '8080',
        pathname: '/api/v1/photos/**',
      },
    ],
  },

  // Security headers
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: [
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
          { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=(self)' },
        ],
      },
    ];
  },

  experimental: {
    typedRoutes: true,        // Type-safe routing
  },
};

export default nextConfig;
```

---

## 7. Type Definitions (API ile Senkron)

Backend'in DTO'larıyla **birebir aynı** olmalı. İdeal olarak OpenAPI'den generate edilir ama MVP için manuel.

### 7.1 `src/lib/types/api.ts`

```typescript
// ─────────────────────────────────────────────
// Domain types — backend DTO'larıyla birebir
// ─────────────────────────────────────────────

export type ServiceCategory = 'HAIR' | 'NAILS' | 'FACE' | 'BODY' | 'OTHER';

export interface DistrictDto {
  id: number;
  name: string;
  slug: string;
}

export interface ServiceDto {
  id: number;
  name: string;
  category: ServiceCategory;
}

export interface SalonListItemDto {
  id: number;
  name: string;
  district: string | null;
  rating: number | null;
  reviewCount: number;
  priceLevel: number | null;
  photoUrl: string | null;
}

export interface SalonDetailDto {
  id: number;
  name: string;
  address: string;
  district: DistrictDto | null;
  phone: string | null;
  website: string | null;
  latitude: number | null;
  longitude: number | null;
  rating: number | null;
  reviewCount: number;
  priceLevel: number | null;
  photoUrl: string | null;
  services: ServiceDto[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SalonUpdateRequest {
  name: string;
  address: string;
  districtId: number | null;
  phone: string | null;
  website: string | null;
  rating: number | null;
  reviewCount: number;
  priceLevel: number | null;
  serviceIds: number[];
  isActive: boolean;
}

export interface SalonPatchRequest {
  name?: string;
  address?: string;
  districtId?: number | null;
  phone?: string | null;
  website?: string | null;
  rating?: number | null;
  reviewCount?: number;
  priceLevel?: number | null;
  serviceIds?: number[];
  isActive?: boolean;
}

// ─────────────────────────────────────────────
// Pagination wrapper
// ─────────────────────────────────────────────

export interface PageMetadata {
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: PageMetadata;
}

// ─────────────────────────────────────────────
// Query params
// ─────────────────────────────────────────────

export interface SalonListParams {
  district?: string;
  service?: string;
  minRating?: number;
  maxPriceLevel?: number;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ─────────────────────────────────────────────
// Auth
// ─────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
}

// ─────────────────────────────────────────────
// Error responses (RFC 7807)
// ─────────────────────────────────────────────

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  timestamp: string;
  errors?: Record<string, string>;
}
```

---

## 8. API Client Layer

### 8.1 `src/lib/api/client.ts`

```typescript
import ky, { HTTPError, type KyInstance } from 'ky';
import { cookies } from 'next/headers';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';
const INTERNAL_URL = process.env.INTERNAL_API_URL ?? BASE_URL;
const SESSION_COOKIE = process.env.SESSION_COOKIE_NAME ?? 'salon_session';

/**
 * Client-side fetch (browser).
 * JWT token cookie'den okunur (httpOnly olmadığı varsayılıyor — basitlik için).
 * Production'da httpOnly cookie + same-site=strict, server action ile inject edilir.
 */
export const apiClient: KyInstance = ky.create({
  prefixUrl: BASE_URL,
  timeout: 15000,
  retry: {
    limit: 2,
    methods: ['get'],
    statusCodes: [408, 502, 503, 504],
  },
  hooks: {
    beforeRequest: [
      (request) => {
        // Browser'da çalışırken cookie'yi browser zaten otomatik ekler
        // veya client component için token'ı oradan alabilirsin
        if (typeof document !== 'undefined') {
          const token = readCookieClient(SESSION_COOKIE);
          if (token) request.headers.set('Authorization', `Bearer ${token}`);
        }
      },
    ],
    afterResponse: [
      async (_request, _options, response) => {
        if (response.status === 401 && typeof window !== 'undefined') {
          // Token expired veya invalid → login'e at
          window.location.href = '/login';
        }
        return response;
      },
    ],
  },
});

/**
 * Server-side fetch (Server Component, Route Handler).
 * Auth header'ı manuel ekleyeceğiz, çünkü cookie API'si farklı.
 */
export async function serverFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const cookieStore = await cookies();
  const token = cookieStore.get(SESSION_COOKIE)?.value;

  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(`${INTERNAL_URL}${path}`, {
    ...options,
    headers,
    // Next.js caching: revalidate every 60s for GET
    next: options.method && options.method !== 'GET' ? { revalidate: 0 } : { revalidate: 60 },
  });

  if (!res.ok) {
    const problem = await res.json().catch(() => ({ detail: 'Unknown error' }));
    throw new ApiError(res.status, problem);
  }

  if (res.status === 204) return null as T;
  return res.json() as Promise<T>;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public problem: { detail?: string; errors?: Record<string, string> }
  ) {
    super(problem.detail ?? `HTTP ${status}`);
  }
}

function readCookieClient(name: string): string | undefined {
  if (typeof document === 'undefined') return undefined;
  return document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${name}=`))
    ?.split('=')[1];
}
```

### 8.2 `src/lib/api/salons.ts`

```typescript
import { apiClient, serverFetch } from './client';
import type {
  PagedResponse,
  SalonDetailDto,
  SalonListItemDto,
  SalonListParams,
  SalonPatchRequest,
  SalonUpdateRequest,
} from '@/lib/types/api';

function buildSearchParams(params: SalonListParams): URLSearchParams {
  const sp = new URLSearchParams();
  if (params.district)      sp.set('district', params.district);
  if (params.service)       sp.set('service', params.service);
  if (params.minRating)     sp.set('minRating', params.minRating.toString());
  if (params.maxPriceLevel) sp.set('maxPriceLevel', params.maxPriceLevel.toString());
  if (params.search)        sp.set('search', params.search);
  if (params.page !== undefined) sp.set('page', params.page.toString());
  if (params.size !== undefined) sp.set('size', params.size.toString());
  if (params.sort)          sp.set('sort', params.sort);
  return sp;
}

// ──────────── Server-side (Server Components) ────────────

export async function fetchSalonsServer(
  params: SalonListParams = {}
): Promise<PagedResponse<SalonListItemDto>> {
  const sp = buildSearchParams(params);
  return serverFetch<PagedResponse<SalonListItemDto>>(`/salons?${sp.toString()}`);
}

export async function fetchSalonServer(id: number): Promise<SalonDetailDto> {
  return serverFetch<SalonDetailDto>(`/salons/${id}`);
}

// ──────────── Client-side (TanStack Query) ────────────

export async function fetchSalons(
  params: SalonListParams = {}
): Promise<PagedResponse<SalonListItemDto>> {
  const sp = buildSearchParams(params);
  return apiClient.get(`salons?${sp.toString()}`).json();
}

export async function fetchSalon(id: number): Promise<SalonDetailDto> {
  return apiClient.get(`salons/${id}`).json();
}

export async function updateSalon(
  id: number,
  body: SalonUpdateRequest
): Promise<SalonDetailDto> {
  return apiClient.put(`salons/${id}`, { json: body }).json();
}

export async function patchSalon(
  id: number,
  body: SalonPatchRequest
): Promise<SalonDetailDto> {
  return apiClient.patch(`salons/${id}`, { json: body }).json();
}

export async function deleteSalon(id: number): Promise<void> {
  await apiClient.delete(`salons/${id}`);
}
```

### 8.3 `src/lib/api/districts.ts` & `services.ts`

```typescript
// districts.ts
import { serverFetch } from './client';
import type { DistrictDto } from '@/lib/types/api';

export async function fetchDistrictsServer(): Promise<DistrictDto[]> {
  return serverFetch<DistrictDto[]>('/districts');
}
```

```typescript
// services.ts
import { serverFetch } from './client';
import type { ServiceDto } from '@/lib/types/api';

export async function fetchServicesServer(): Promise<ServiceDto[]> {
  return serverFetch<ServiceDto[]>('/services');
}
```

---

## 9. TanStack Query Setup

### 9.1 `src/lib/providers/query-provider.tsx`

```typescript
'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useState, type ReactNode } from 'react';

export function QueryProvider({ children }: { children: ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,           // 1 min — backend cache zaten var
            refetchOnWindowFocus: false,
            retry: (count, error: any) => {
              // 4xx hatalarda retry yapma
              if (error?.response?.status >= 400 && error?.response?.status < 500) return false;
              return count < 2;
            },
          },
          mutations: {
            retry: false,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={client}>
      {children}
      {process.env.NODE_ENV === 'development' && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
}
```

### 9.2 `src/lib/hooks/use-salons.ts`

```typescript
'use client';

import { useQuery } from '@tanstack/react-query';
import { fetchSalons } from '@/lib/api/salons';
import type { SalonListParams } from '@/lib/types/api';

export function useSalons(params: SalonListParams) {
  return useQuery({
    queryKey: ['salons', params],
    queryFn: () => fetchSalons(params),
  });
}
```

### 9.3 `src/lib/hooks/use-update-salon.ts`

```typescript
'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { patchSalon } from '@/lib/api/salons';
import type { SalonDetailDto, SalonPatchRequest } from '@/lib/types/api';
import { toast } from 'sonner';

export function useUpdateSalon(id: number) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: (body: SalonPatchRequest) => patchSalon(id, body),

    // Optimistic update
    onMutate: async (newData) => {
      await qc.cancelQueries({ queryKey: ['salon', id] });
      const previous = qc.getQueryData<SalonDetailDto>(['salon', id]);
      if (previous) {
        qc.setQueryData<SalonDetailDto>(['salon', id], { ...previous, ...newData });
      }
      return { previous };
    },

    onError: (err, _vars, context) => {
      // Rollback
      if (context?.previous) qc.setQueryData(['salon', id], context.previous);
      toast.error('Update failed', { description: err.message });
    },

    onSuccess: () => {
      toast.success('Salon updated');
      qc.invalidateQueries({ queryKey: ['salons'] });   // listing'i refetch
      qc.invalidateQueries({ queryKey: ['salon', id] });
    },
  });
}
```

---

## 10. Authentication

### 10.1 Strateji

JWT token'ı **httpOnly cookie**'de tutuyoruz, ama bunu Next.js Server Action / Route Handler üzerinden inject ediyoruz:

```
Browser → POST /api/auth/login → Next.js Route Handler
                                  ↓
                               Spring Backend /auth/login → JWT döner
                                  ↓
                              Next.js Set-Cookie (httpOnly, Secure, SameSite=Lax)
                                  ↓
                              Browser cookie'yi otomatik gönderir
```

### 10.2 `src/app/api/auth/login/route.ts`

```typescript
import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

const SESSION_COOKIE = process.env.SESSION_COOKIE_NAME ?? 'salon_session';
const INTERNAL_URL = process.env.INTERNAL_API_URL ?? 'http://localhost:8080/api/v1';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const parsed = loginSchema.safeParse(body);
    if (!parsed.success) {
      return NextResponse.json({ detail: 'Invalid input' }, { status: 400 });
    }

    const res = await fetch(`${INTERNAL_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(parsed.data),
    });

    if (!res.ok) {
      const problem = await res.json().catch(() => ({ detail: 'Auth failed' }));
      return NextResponse.json(problem, { status: res.status });
    }

    const { accessToken, expiresInSeconds } = await res.json();

    const response = NextResponse.json({ success: true });
    response.cookies.set({
      name: SESSION_COOKIE,
      value: accessToken,
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      maxAge: expiresInSeconds,
      path: '/',
    });

    return response;
  } catch (error) {
    return NextResponse.json({ detail: 'Unexpected error' }, { status: 500 });
  }
}
```

### 10.3 `src/app/api/auth/logout/route.ts`

```typescript
import { NextResponse } from 'next/server';

const SESSION_COOKIE = process.env.SESSION_COOKIE_NAME ?? 'salon_session';

export async function POST() {
  const response = NextResponse.json({ success: true });
  response.cookies.set({
    name: SESSION_COOKIE,
    value: '',
    maxAge: 0,
    path: '/',
  });
  return response;
}
```

### 10.4 `src/middleware.ts` — protected routes

```typescript
import { NextResponse, type NextRequest } from 'next/server';

const SESSION_COOKIE = process.env.SESSION_COOKIE_NAME ?? 'salon_session';

const PROTECTED_PATHS = ['/admin', '/salons/*/edit'];

export function middleware(request: NextRequest) {
  const token = request.cookies.get(SESSION_COOKIE)?.value;
  const { pathname } = request.nextUrl;

  const isProtected = PROTECTED_PATHS.some((path) => {
    const pattern = path.replace('*', '[^/]+');
    return new RegExp(`^${pattern}$`).test(pathname);
  });

  if (isProtected && !token) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('redirect', pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/admin/:path*', '/salons/:id/edit'],
};
```

### 10.5 `src/lib/auth/session.ts`

```typescript
import { cookies } from 'next/headers';
import { jwtDecode } from 'jose';

const SESSION_COOKIE = process.env.SESSION_COOKIE_NAME ?? 'salon_session';

export interface SessionUser {
  email: string;
  role: string;
  expiresAt: number;
}

export async function getSession(): Promise<SessionUser | null> {
  const store = await cookies();
  const token = store.get(SESSION_COOKIE)?.value;
  if (!token) return null;

  try {
    // jose ile decode — verification backend'in işi, biz sadece okumak için decode ediyoruz
    const decoded: any = decodeJwtPayload(token);
    if (!decoded.exp || decoded.exp * 1000 < Date.now()) return null;

    return {
      email: decoded.sub,
      role: extractRole(decoded.authorities),
      expiresAt: decoded.exp * 1000,
    };
  } catch {
    return null;
  }
}

function decodeJwtPayload(token: string): any {
  const [, payload] = token.split('.');
  if (!payload) throw new Error('Invalid JWT');
  return JSON.parse(Buffer.from(payload, 'base64').toString('utf-8'));
}

function extractRole(authorities: Array<{ authority: string }>): string {
  if (!Array.isArray(authorities)) return 'USER';
  const adminAuth = authorities.find((a) => a.authority?.includes('ADMIN'));
  return adminAuth ? 'ADMIN' : 'USER';
}
```

---

## 11. Layout & Global UI

### 11.1 `src/app/layout.tsx`

```typescript
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import { Toaster } from 'sonner';
import { QueryProvider } from '@/lib/providers/query-provider';
import { Header } from '@/components/layout/header';
import { Footer } from '@/components/layout/footer';
import './globals.css';

const inter = Inter({ subsets: ['latin'], variable: '--font-sans' });

export const metadata: Metadata = {
  title: { default: 'Warsaw Salon Explorer', template: '%s | Salon Explorer' },
  description: 'Discover and explore beauty salons across Warsaw',
  openGraph: {
    title: 'Warsaw Salon Explorer',
    description: 'Discover beauty salons across Warsaw',
    type: 'website',
    locale: 'en_US',
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${inter.variable} font-sans antialiased min-h-screen flex flex-col`}>
        <QueryProvider>
          <Header />
          <main className="flex-1 container mx-auto px-4 py-6">{children}</main>
          <Footer />
          <Toaster position="bottom-right" />
        </QueryProvider>
      </body>
    </html>
  );
}
```

### 11.2 `src/components/layout/header.tsx`

```typescript
import Link from 'next/link';
import { getSession } from '@/lib/auth/session';
import { Button } from '@/components/ui/button';
import { LogoutButton } from './logout-button';

export async function Header() {
  const session = await getSession();

  return (
    <header className="border-b bg-background sticky top-0 z-50">
      <div className="container mx-auto px-4 h-14 flex items-center justify-between">
        <Link href="/" className="font-semibold text-lg">
          🇵🇱 Warsaw Salons
        </Link>

        <nav className="flex items-center gap-3" aria-label="Main navigation">
          {session ? (
            <>
              <span className="text-sm text-muted-foreground" aria-label={`Logged in as ${session.email}`}>
                {session.email}
              </span>
              {session.role === 'ADMIN' && (
                <Button variant="ghost" size="sm" asChild>
                  <Link href="/admin">Admin</Link>
                </Button>
              )}
              <LogoutButton />
            </>
          ) : (
            <Button variant="outline" size="sm" asChild>
              <Link href="/login">Login</Link>
            </Button>
          )}
        </nav>
      </div>
    </header>
  );
}
```

---

## 12. Listing Page

### 12.1 `src/app/page.tsx` — server component

```typescript
import { Suspense } from 'react';
import { fetchSalonsServer } from '@/lib/api/salons';
import { fetchDistrictsServer } from '@/lib/api/districts';
import { SalonList } from '@/components/salons/salon-list';
import { SalonFilters } from '@/components/salons/salon-filters';
import { Pagination } from '@/components/layout/pagination';
import { SalonListSkeleton } from '@/components/salons/salon-list-skeleton';
import type { SalonListParams } from '@/lib/types/api';

interface PageProps {
  searchParams: Promise<{
    district?: string;
    service?: string;
    minRating?: string;
    maxPriceLevel?: string;
    search?: string;
    page?: string;
    size?: string;
    sort?: string;
  }>;
}

export default async function HomePage({ searchParams }: PageProps) {
  const sp = await searchParams;

  const params: SalonListParams = {
    district: sp.district,
    service: sp.service,
    minRating: sp.minRating ? Number(sp.minRating) : undefined,
    maxPriceLevel: sp.maxPriceLevel ? Number(sp.maxPriceLevel) : undefined,
    search: sp.search,
    page: sp.page ? Number(sp.page) : 0,
    size: sp.size ? Number(sp.size) : 20,
    sort: sp.sort ?? 'rating,desc',
  };

  // Parallel fetch
  const [salonsPromise, districts] = await Promise.all([
    fetchSalonsServer(params),
    fetchDistrictsServer(),
  ]);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-[260px_1fr] gap-6">
      <aside className="lg:sticky lg:top-20 lg:self-start" aria-label="Filters">
        <SalonFilters districts={districts} initialParams={params} />
      </aside>

      <section aria-label="Salon results">
        <div className="mb-4 flex items-center justify-between">
          <h1 className="text-2xl font-semibold">
            Warsaw Beauty Salons
            <span className="ml-2 text-sm text-muted-foreground">
              ({salonsPromise.page.totalElements} found)
            </span>
          </h1>
        </div>

        <Suspense fallback={<SalonListSkeleton />}>
          <SalonList salons={salonsPromise.content} />
        </Suspense>

        <Pagination
          currentPage={salonsPromise.page.number}
          totalPages={salonsPromise.page.totalPages}
        />
      </section>
    </div>
  );
}
```

### 12.2 `src/components/salons/salon-card.tsx`

```typescript
import Image from 'next/image';
import Link from 'next/link';
import { Star } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { DistrictBadge } from '@/components/common/district-badge';
import { PriceLevel } from '@/components/common/price-level';
import type { SalonListItemDto } from '@/lib/types/api';

export function SalonCard({ salon }: { salon: SalonListItemDto }) {
  const photoSrc = salon.photoUrl
    ? `http://localhost:8080${salon.photoUrl}`
    : '/placeholder-salon.svg';

  return (
    <Card className="overflow-hidden hover:shadow-md transition-shadow">
      <Link
        href={`/salons/${salon.id}`}
        className="block focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-lg"
        aria-label={`View details for ${salon.name}`}
      >
        <div className="relative aspect-[4/3] bg-muted">
          <Image
            src={photoSrc}
            alt={`Photo of ${salon.name}`}
            fill
            sizes="(max-width: 768px) 100vw, 33vw"
            className="object-cover"
          />
        </div>
        <CardContent className="p-4 space-y-2">
          <div className="flex items-start justify-between gap-2">
            <h2 className="font-semibold leading-tight line-clamp-2">{salon.name}</h2>
            {salon.priceLevel && <PriceLevel level={salon.priceLevel} />}
          </div>

          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            {salon.district && <DistrictBadge name={salon.district} />}
          </div>

          {salon.rating != null && (
            <div className="flex items-center gap-1 text-sm" aria-label={`Rating ${salon.rating} out of 5`}>
              <Star className="size-4 fill-yellow-400 text-yellow-400" aria-hidden="true" />
              <span className="font-medium">{salon.rating.toFixed(1)}</span>
              <span className="text-muted-foreground">({salon.reviewCount})</span>
            </div>
          )}
        </CardContent>
      </Link>
    </Card>
  );
}
```

### 12.3 `src/components/salons/salon-list.tsx`

```typescript
import { SalonCard } from './salon-card';
import type { SalonListItemDto } from '@/lib/types/api';

export function SalonList({ salons }: { salons: SalonListItemDto[] }) {
  if (salons.length === 0) {
    return (
      <div className="rounded-lg border border-dashed py-16 text-center">
        <p className="text-muted-foreground">No salons match your filters.</p>
        <p className="text-sm text-muted-foreground mt-1">Try adjusting your search.</p>
      </div>
    );
  }

  return (
    <ul className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
      {salons.map((salon) => (
        <li key={salon.id}>
          <SalonCard salon={salon} />
        </li>
      ))}
    </ul>
  );
}
```

### 12.4 `src/components/salons/salon-filters.tsx`

```typescript
'use client';

import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { DistrictDto, SalonListParams } from '@/lib/types/api';

interface Props {
  districts: DistrictDto[];
  initialParams: SalonListParams;
}

export function SalonFilters({ districts, initialParams }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const updateParam = useCallback(
    (key: string, value: string | undefined) => {
      const params = new URLSearchParams(searchParams.toString());
      if (value && value !== 'all') {
        params.set(key, value);
      } else {
        params.delete(key);
      }
      params.delete('page'); // reset to page 0 on filter change
      router.push(`${pathname}?${params.toString()}`);
    },
    [pathname, router, searchParams]
  );

  const clearAll = () => router.push(pathname);

  return (
    <div className="space-y-5 p-4 border rounded-lg">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold">Filters</h2>
        <Button variant="ghost" size="sm" onClick={clearAll}>Clear</Button>
      </div>

      <div className="space-y-2">
        <Label htmlFor="search">Search by name</Label>
        <Input
          id="search"
          type="search"
          placeholder="e.g. Anna"
          defaultValue={initialParams.search ?? ''}
          onBlur={(e) => updateParam('search', e.target.value || undefined)}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="district">District</Label>
        <Select
          defaultValue={initialParams.district ?? 'all'}
          onValueChange={(v) => updateParam('district', v)}
        >
          <SelectTrigger id="district"><SelectValue placeholder="All districts" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All districts</SelectItem>
            {districts.map((d) => (
              <SelectItem key={d.id} value={d.slug}>{d.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label htmlFor="minRating">Min rating</Label>
        <Select
          defaultValue={initialParams.minRating?.toString() ?? 'all'}
          onValueChange={(v) => updateParam('minRating', v)}
        >
          <SelectTrigger id="minRating"><SelectValue placeholder="Any rating" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Any rating</SelectItem>
            <SelectItem value="3.0">3.0+</SelectItem>
            <SelectItem value="4.0">4.0+</SelectItem>
            <SelectItem value="4.5">4.5+</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label htmlFor="priceLevel">Max price</Label>
        <Select
          defaultValue={initialParams.maxPriceLevel?.toString() ?? 'all'}
          onValueChange={(v) => updateParam('maxPriceLevel', v)}
        >
          <SelectTrigger id="priceLevel"><SelectValue placeholder="Any price" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Any price</SelectItem>
            <SelectItem value="1">€ (cheap)</SelectItem>
            <SelectItem value="2">€€ (moderate)</SelectItem>
            <SelectItem value="3">€€€ (expensive)</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
```

### 12.5 `src/components/layout/pagination.tsx`

```typescript
'use client';

import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface Props {
  currentPage: number;
  totalPages: number;
}

export function Pagination({ currentPage, totalPages }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  if (totalPages <= 1) return null;

  const goto = (page: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set('page', page.toString());
    router.push(`${pathname}?${params.toString()}`);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <nav className="mt-8 flex items-center justify-center gap-2" aria-label="Pagination">
      <Button
        variant="outline"
        size="sm"
        disabled={currentPage === 0}
        onClick={() => goto(currentPage - 1)}
        aria-label="Previous page"
      >
        <ChevronLeft className="size-4" /> Previous
      </Button>
      <span className="text-sm text-muted-foreground" aria-live="polite">
        Page {currentPage + 1} of {totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        disabled={currentPage >= totalPages - 1}
        onClick={() => goto(currentPage + 1)}
        aria-label="Next page"
      >
        Next <ChevronRight className="size-4" />
      </Button>
    </nav>
  );
}
```

---

## 13. Detail Page

### 13.1 `src/app/salons/[id]/page.tsx`

```typescript
import { notFound } from 'next/navigation';
import Image from 'next/image';
import Link from 'next/link';
import { Globe, Phone, MapPin, Edit } from 'lucide-react';
import { fetchSalonServer } from '@/lib/api/salons';
import { getSession } from '@/lib/auth/session';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { ApiError } from '@/lib/api/client';

interface Props {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: Props) {
  const { id } = await params;
  try {
    const salon = await fetchSalonServer(Number(id));
    return {
      title: salon.name,
      description: `${salon.name} - beauty salon in ${salon.district?.name ?? 'Warsaw'}`,
    };
  } catch {
    return { title: 'Salon not found' };
  }
}

export default async function SalonDetailPage({ params }: Props) {
  const { id } = await params;
  const session = await getSession();

  let salon;
  try {
    salon = await fetchSalonServer(Number(id));
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  const photoSrc = salon.photoUrl
    ? `http://localhost:8080${salon.photoUrl}`
    : '/placeholder-salon.svg';

  return (
    <article className="max-w-4xl mx-auto space-y-6">
      <div className="relative aspect-video rounded-lg overflow-hidden bg-muted">
        <Image src={photoSrc} alt={`${salon.name} photo`} fill className="object-cover" priority />
      </div>

      <header className="space-y-2">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold">{salon.name}</h1>
            {salon.district && (
              <p className="text-muted-foreground">📍 {salon.district.name}</p>
            )}
          </div>
          {session?.role === 'ADMIN' && (
            <Button asChild>
              <Link href={`/salons/${salon.id}/edit`}>
                <Edit className="size-4 mr-2" /> Edit
              </Link>
            </Button>
          )}
        </div>

        {salon.rating != null && (
          <div className="flex items-center gap-2">
            <span className="text-yellow-500" aria-hidden="true">★</span>
            <span className="font-semibold">{salon.rating.toFixed(1)}</span>
            <span className="text-muted-foreground">({salon.reviewCount} reviews)</span>
          </div>
        )}
      </header>

      <Separator />

      <section className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <div className="space-y-3">
          <h2 className="font-semibold text-lg">Contact</h2>
          <ul className="space-y-2 text-sm">
            <li className="flex items-start gap-2">
              <MapPin className="size-4 mt-0.5 shrink-0" aria-hidden="true" />
              <span>{salon.address}</span>
            </li>
            {salon.phone && (
              <li className="flex items-center gap-2">
                <Phone className="size-4 shrink-0" aria-hidden="true" />
                <a href={`tel:${salon.phone}`} className="hover:underline">{salon.phone}</a>
              </li>
            )}
            {salon.website && (
              <li className="flex items-center gap-2">
                <Globe className="size-4 shrink-0" aria-hidden="true" />
                <a
                  href={salon.website}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:underline truncate"
                >
                  {salon.website}
                </a>
              </li>
            )}
          </ul>
        </div>

        <div className="space-y-3">
          <h2 className="font-semibold text-lg">Services</h2>
          {salon.services.length === 0 ? (
            <p className="text-sm text-muted-foreground">No services listed</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {salon.services.map((s) => (
                <Badge key={s.id} variant="secondary">{s.name}</Badge>
              ))}
            </div>
          )}
        </div>
      </section>
    </article>
  );
}
```

---

## 14. Edit Page (Form)

### 14.1 Zod schema

`src/lib/validators/salon.schema.ts`:

```typescript
import { z } from 'zod';

export const salonPatchSchema = z.object({
  name: z.string().min(2, 'Min 2 characters').max(255).optional(),
  address: z.string().min(1, 'Required').optional(),
  districtId: z.coerce.number().int().positive().optional().nullable(),
  phone: z
    .string()
    .regex(/^\+[1-9]\d{1,14}$/, 'E.164 format e.g. +48221234567')
    .or(z.literal(''))
    .optional(),
  website: z.string().url('Invalid URL').or(z.literal('')).optional(),
  rating: z.coerce.number().min(0).max(5).optional().nullable(),
  reviewCount: z.coerce.number().int().min(0).optional(),
  priceLevel: z.coerce.number().int().min(1).max(4).optional().nullable(),
  isActive: z.boolean().optional(),
});

export type SalonPatchFormValues = z.infer<typeof salonPatchSchema>;
```

### 14.2 `src/app/salons/[id]/edit/page.tsx`

```typescript
import { notFound } from 'next/navigation';
import { fetchSalonServer } from '@/lib/api/salons';
import { fetchDistrictsServer } from '@/lib/api/districts';
import { SalonEditForm } from '@/components/salons/salon-edit-form';
import { ApiError } from '@/lib/api/client';

interface Props {
  params: Promise<{ id: string }>;
}

export default async function EditPage({ params }: Props) {
  const { id } = await params;
  let salon;
  try {
    salon = await fetchSalonServer(Number(id));
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  const districts = await fetchDistrictsServer();

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-semibold mb-6">Edit: {salon.name}</h1>
      <SalonEditForm salon={salon} districts={districts} />
    </div>
  );
}
```

### 14.3 `src/components/salons/salon-edit-form.tsx`

```typescript
'use client';

import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { salonPatchSchema, type SalonPatchFormValues } from '@/lib/validators/salon.schema';
import { useUpdateSalon } from '@/lib/hooks/use-update-salon';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { DistrictDto, SalonDetailDto } from '@/lib/types/api';

interface Props {
  salon: SalonDetailDto;
  districts: DistrictDto[];
}

export function SalonEditForm({ salon, districts }: Props) {
  const router = useRouter();
  const updateMutation = useUpdateSalon(salon.id);

  const form = useForm<SalonPatchFormValues>({
    resolver: zodResolver(salonPatchSchema),
    defaultValues: {
      name: salon.name,
      address: salon.address,
      districtId: salon.district?.id ?? null,
      phone: salon.phone ?? '',
      website: salon.website ?? '',
      rating: salon.rating ?? null,
      reviewCount: salon.reviewCount,
      priceLevel: salon.priceLevel ?? null,
      isActive: salon.isActive,
    },
  });

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting, isDirty },
  } = form;

  const onSubmit = async (values: SalonPatchFormValues) => {
    // Boş string'leri null'a çevir
    const payload = {
      ...values,
      phone:   values.phone   === '' ? null : values.phone,
      website: values.website === '' ? null : values.website,
    };

    await updateMutation.mutateAsync(payload);
    router.push(`/salons/${salon.id}`);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
      <div className="space-y-2">
        <Label htmlFor="name">Name *</Label>
        <Input id="name" {...register('name')} aria-invalid={!!errors.name} />
        {errors.name && <p className="text-sm text-destructive" role="alert">{errors.name.message}</p>}
      </div>

      <div className="space-y-2">
        <Label htmlFor="address">Address *</Label>
        <Input id="address" {...register('address')} aria-invalid={!!errors.address} />
        {errors.address && <p className="text-sm text-destructive" role="alert">{errors.address.message}</p>}
      </div>

      <div className="space-y-2">
        <Label htmlFor="districtId">District</Label>
        <Select
          value={watch('districtId')?.toString() ?? ''}
          onValueChange={(v) => setValue('districtId', v ? Number(v) : null, { shouldDirty: true })}
        >
          <SelectTrigger id="districtId"><SelectValue placeholder="Select district" /></SelectTrigger>
          <SelectContent>
            {districts.map((d) => (
              <SelectItem key={d.id} value={d.id.toString()}>{d.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="phone">Phone (E.164)</Label>
          <Input id="phone" placeholder="+48221234567" {...register('phone')} aria-invalid={!!errors.phone} />
          {errors.phone && <p className="text-sm text-destructive" role="alert">{errors.phone.message}</p>}
        </div>

        <div className="space-y-2">
          <Label htmlFor="website">Website</Label>
          <Input id="website" type="url" {...register('website')} aria-invalid={!!errors.website} />
          {errors.website && <p className="text-sm text-destructive" role="alert">{errors.website.message}</p>}
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        <div className="space-y-2">
          <Label htmlFor="rating">Rating (0-5)</Label>
          <Input id="rating" type="number" step="0.1" min="0" max="5" {...register('rating')} aria-invalid={!!errors.rating} />
          {errors.rating && <p className="text-sm text-destructive" role="alert">{errors.rating.message}</p>}
        </div>

        <div className="space-y-2">
          <Label htmlFor="reviewCount">Reviews</Label>
          <Input id="reviewCount" type="number" min="0" {...register('reviewCount')} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="priceLevel">Price (1-4)</Label>
          <Input id="priceLevel" type="number" min="1" max="4" {...register('priceLevel')} aria-invalid={!!errors.priceLevel} />
          {errors.priceLevel && <p className="text-sm text-destructive" role="alert">{errors.priceLevel.message}</p>}
        </div>
      </div>

      <div className="flex items-center gap-2">
        <input id="isActive" type="checkbox" {...register('isActive')} className="size-4" />
        <Label htmlFor="isActive">Active</Label>
      </div>

      <div className="flex gap-3 pt-4">
        <Button type="submit" disabled={isSubmitting || !isDirty}>
          {isSubmitting ? 'Saving…' : 'Save Changes'}
        </Button>
        <Button type="button" variant="outline" onClick={() => router.back()}>
          Cancel
        </Button>
      </div>
    </form>
  );
}
```

---

## 15. Admin Login Page

### 15.1 `src/app/login/page.tsx`

```typescript
'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

const schema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(1, 'Required'),
});

type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectTo = searchParams.get('redirect') ?? '/';
  const [serverError, setServerError] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (values: FormValues) => {
    setServerError(null);
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(values),
    });

    if (!res.ok) {
      const problem = await res.json().catch(() => ({}));
      setServerError(problem.detail ?? 'Login failed');
      return;
    }

    toast.success('Welcome back');
    router.push(redirectTo);
    router.refresh();
  };

  return (
    <div className="max-w-sm mx-auto mt-16">
      <Card>
        <CardHeader>
          <CardTitle>Admin Login</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                {...register('email')}
                aria-invalid={!!errors.email}
              />
              {errors.email && (
                <p className="text-sm text-destructive" role="alert">{errors.email.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                {...register('password')}
                aria-invalid={!!errors.password}
              />
              {errors.password && (
                <p className="text-sm text-destructive" role="alert">{errors.password.message}</p>
              )}
            </div>

            {serverError && (
              <p className="text-sm text-destructive" role="alert">{serverError}</p>
            )}

            <Button type="submit" disabled={isSubmitting} className="w-full">
              {isSubmitting ? 'Signing in…' : 'Sign In'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
```

---

## 16. Component Library (shadcn/ui)

Yukarıda her component'i `npx shadcn add ...` ile ekledik. Bunlar `src/components/ui/` altında dosya olarak yaşar — npm dependency değil, kopyalanmış. Bu sayede:

- Kontrolünde
- Custom değişiklik kolay
- Bundle size daha küçük (sadece kullandığın)

### 16.1 Common componentlar (özelden senin yazacakların)

**`src/components/common/rating-stars.tsx`:**

```typescript
import { Star } from 'lucide-react';

export function RatingStars({ value, max = 5 }: { value: number; max?: number }) {
  return (
    <div
      className="inline-flex items-center gap-0.5"
      role="img"
      aria-label={`Rating ${value.toFixed(1)} out of ${max}`}
    >
      {Array.from({ length: max }).map((_, i) => {
        const filled = i < Math.round(value);
        return (
          <Star
            key={i}
            className={`size-4 ${filled ? 'fill-yellow-400 text-yellow-400' : 'text-muted-foreground'}`}
            aria-hidden="true"
          />
        );
      })}
    </div>
  );
}
```

**`src/components/common/price-level.tsx`:**

```typescript
export function PriceLevel({ level }: { level: number }) {
  const symbols = '€'.repeat(level);
  const labels: Record<number, string> = {
    1: 'Inexpensive', 2: 'Moderate', 3: 'Expensive', 4: 'Very expensive',
  };
  return (
    <span
      className="text-sm font-medium text-muted-foreground"
      aria-label={labels[level] ?? 'Unknown price'}
    >
      {symbols}
    </span>
  );
}
```

**`src/components/common/district-badge.tsx`:**

```typescript
import { Badge } from '@/components/ui/badge';
import { MapPin } from 'lucide-react';

export function DistrictBadge({ name }: { name: string }) {
  return (
    <Badge variant="secondary" className="font-normal">
      <MapPin className="size-3 mr-1" aria-hidden="true" /> {name}
    </Badge>
  );
}
```

---

## 17. Loading & Error States

### 17.1 `src/app/loading.tsx` — global

```typescript
import { SalonListSkeleton } from '@/components/salons/salon-list-skeleton';

export default function Loading() {
  return <SalonListSkeleton />;
}
```

### 17.2 `src/components/salons/salon-list-skeleton.tsx`

```typescript
import { Skeleton } from '@/components/ui/skeleton';

export function SalonListSkeleton() {
  return (
    <ul className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4" aria-label="Loading salons">
      {Array.from({ length: 6 }).map((_, i) => (
        <li key={i} className="border rounded-lg overflow-hidden">
          <Skeleton className="aspect-[4/3] w-full" />
          <div className="p-4 space-y-2">
            <Skeleton className="h-5 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-4 w-1/3" />
          </div>
        </li>
      ))}
    </ul>
  );
}
```

### 17.3 `src/app/error.tsx` — global error boundary

```typescript
'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('App error:', error);
  }, [error]);

  return (
    <div className="max-w-md mx-auto text-center py-16">
      <h1 className="text-2xl font-semibold mb-2">Something went wrong</h1>
      <p className="text-muted-foreground mb-6">
        An unexpected error occurred. Please try again.
      </p>
      <Button onClick={reset}>Try again</Button>
    </div>
  );
}
```

### 17.4 `src/app/not-found.tsx`

```typescript
import Link from 'next/link';
import { Button } from '@/components/ui/button';

export default function NotFound() {
  return (
    <div className="max-w-md mx-auto text-center py-16">
      <h1 className="text-3xl font-semibold mb-2">404</h1>
      <p className="text-muted-foreground mb-6">The page you're looking for doesn't exist.</p>
      <Button asChild>
        <Link href="/">Go home</Link>
      </Button>
    </div>
  );
}
```

---

## 18. Accessibility

WCAG 2.1 AA hedefi.

**Yapılanlar (yukarıdaki kodda):**

- ✅ Semantic HTML (`<header>`, `<nav>`, `<main>`, `<aside>`, `<article>`, `<section>`)
- ✅ `aria-label`, `aria-invalid`, `aria-live` doğru kullanım
- ✅ `<label htmlFor>` form input'larıyla bağlı
- ✅ Focus ring (Tailwind `focus:ring-2`)
- ✅ Klavye navigasyonu (Link, Button tüm interactive'ler tab'lanabilir)
- ✅ `alt` text resimlerde
- ✅ Renk kontrastı (shadcn/ui zaten WCAG AA uyumlu)
- ✅ `role="alert"` validation hatalarında
- ✅ Screen reader friendly skeleton (`aria-label="Loading"`)

**Test:**

```bash
# Browser extension: axe DevTools
# CLI:
npm install -D @axe-core/cli
npx axe http://localhost:3000
```

---

## 19. Testing

### 19.1 Vitest config

`vitest.config.ts`:

```typescript
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.ts'],
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
});
```

`vitest.setup.ts`:

```typescript
import '@testing-library/jest-dom';
```

### 19.2 Component test örneği

`src/components/salons/__tests__/salon-card.test.tsx`:

```typescript
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { SalonCard } from '../salon-card';

describe('SalonCard', () => {
  const baseSalon = {
    id: 1,
    name: 'Test Salon',
    district: 'Mokotów',
    rating: 4.5,
    reviewCount: 100,
    priceLevel: 2,
    photoUrl: null,
  };

  it('renders salon name and district', () => {
    render(<SalonCard salon={baseSalon} />);
    expect(screen.getByText('Test Salon')).toBeInTheDocument();
    expect(screen.getByText('Mokotów')).toBeInTheDocument();
  });

  it('shows rating with review count', () => {
    render(<SalonCard salon={baseSalon} />);
    expect(screen.getByText('4.5')).toBeInTheDocument();
    expect(screen.getByText('(100)')).toBeInTheDocument();
  });

  it('hides rating when null', () => {
    render(<SalonCard salon={{ ...baseSalon, rating: null }} />);
    expect(screen.queryByText('4.5')).not.toBeInTheDocument();
  });
});
```

### 19.3 Playwright e2e örneği

`src/e2e/salon-flow.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test.describe('Salon listing flow', () => {
  test('user can filter by district and view a salon', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { name: /warsaw beauty salons/i })).toBeVisible();

    // Apply district filter
    await page.getByLabel('District').click();
    await page.getByRole('option', { name: 'Mokotów' }).click();

    // URL should update
    await expect(page).toHaveURL(/district=mokotow/);

    // At least one salon card
    const cards = page.locator('article, li > a[href^="/salons/"]');
    await expect(cards.first()).toBeVisible();

    // Navigate to detail
    await cards.first().click();
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByText(/contact/i)).toBeVisible();
  });
});
```

`playwright.config.ts`:

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './src/e2e',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },
});
```

---

## 20. Performance

### 20.1 İyileştirmeler

- ✅ **Server Components** by default — JS bundle küçük
- ✅ **`next/image`** — otomatik AVIF/WebP, lazy load
- ✅ **`next/font`** — font CLS yok, self-hosted
- ✅ **Parallel data fetching** — `Promise.all` ile
- ✅ **Suspense boundaries** — streaming UI
- ✅ **Route prefetching** — `<Link>` hover'da prefetch
- ✅ **Image priority** — above-the-fold için `priority`
- ✅ **`sizes` prop** — responsive image
- ✅ **TanStack Query staleTime** — gereksiz refetch'i önler

### 20.2 Lighthouse skoru hedefleri

- Performance: 90+
- Accessibility: 95+
- Best Practices: 95+
- SEO: 95+

### 20.3 Bundle analiz

```bash
npm install -D @next/bundle-analyzer
```

`next.config.ts`:

```typescript
import withBundleAnalyzer from '@next/bundle-analyzer';

const config = {
  // ...
};

export default withBundleAnalyzer({ enabled: process.env.ANALYZE === 'true' })(config);
```

Çalıştır:

```bash
ANALYZE=true npm run build
```

---

## 21. Doğrulama

### 21.1 Backend ayakta olmalı

```bash
# Project root
docker compose up -d postgres redis
cd backend && SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

### 21.2 Frontend başlat

```bash
cd frontend
npm run dev
# Open http://localhost:3000
```

### 21.3 Smoke test checklist

| Test | Beklenen | ✅ |
|------|----------|---|
| `/` açılır | Listing görünür, 20 salon | |
| Filter district seç | URL `?district=...` olur, sonuç filtrelenir | |
| Pagination → Next | URL `?page=1` olur, scroll top'a gider | |
| Card'a tıkla | Detail page açılır | |
| Detail page direct URL | SSR doğru, SEO meta dolu | |
| `/salons/9999` | 404 sayfası | |
| `/login` POST | Cookie set olur, `/` redirect | |
| Login sonrası `/admin` | Erişim açılır | |
| Logout | Cookie silinir, `/` redirect | |
| Edit form invalid input | Inline hata, submit disabled | |
| Edit form submit | Optimistic update + toast | |
| Backend kapalıyken | Error boundary çıkar, "Try again" çalışır | |
| Tab navigation | Tüm interactive'lere ulaşılır | |
| Screen reader | Card label'ları okunur | |
| Mobile viewport | Responsive, sidebar üstte | |

---

## 22. Mülakat Soruları

**S: Server Component vs Client Component, ne zaman hangisi?**
Default Server. JavaScript gereksiz inmesin. Client sadece state, effect, browser API (event listener, localStorage, window) gerekiyorsa. Form, filter, button handler → client. Veri fetch + render → server.

**S: Neden TanStack Query, SWR yerine?**
İkisi de iyi. TanStack daha geniş ekosistem, mutation/invalidation API'si daha güçlü. Optimistic update için olgun pattern'ler var. SWR daha minimal — basit cache yetiyorsa onu da kullanırdım.

**S: Listing'i neden server component yaptın, client da olabilirdi?**
3 sebep: 1) SEO — Google JS render etmiyor garantili, HTML hazır gelmeli. 2) İlk paint hızı — backend → Next.js server-to-server çağrı browser → backend'den hızlı. 3) Bundle size — TanStack Query bundle'ı initial page'de inmiyor.

**S: Filter değişiminde sayfayı reload mu ediyorsun?**
Hayır, `router.push` ile soft navigation. Next.js sadece değişen segment'i fetch'liyor, Layout cache'leniyor. Yan filter sidebar da re-render olmuyor (client component, URL state'i kendi okuyor).

**S: JWT'yi neden httpOnly cookie'de tutuyorsun, localStorage değil?**
XSS koruması. JavaScript localStorage'a erişebilir, ama httpOnly cookie'ye erişemez. XSS açığı olursa token çalınmaz. Trade-off: CSRF riski, ama SameSite=Lax ile mitigate ediliyor.

**S: Optimistic update'te rollback nasıl?**
`onMutate`'te eski veriyi `context`'e kaydediyorum, `onError`'de eski veriyi geri yazıyorum. TanStack Query'nin `onMutate → onError → onSettled` lifecycle'ı bu pattern için yapılmış.

**S: Server component'te neden `serverFetch` ayrı, client'ta `apiClient`?**
İki farklı ortam. Server'da `cookies()` Next.js API'siyle erişilir, browser'da `document.cookie`. Server'da `fetch` Next.js cache'i ile entegre çalışır (`revalidate`), browser'da kontrol bizde (TanStack).

**S: Why ky over axios?**
Daha küçük (~5KB vs 15KB). Native `fetch` üzerine kuruluş, modern. Built-in retry/timeout. Axios da ok, sadece bundle size kazanımı.

**S: Type safety için OpenAPI codegen kullanır mıydın?**
Production'da evet. `openapi-typescript-codegen` veya `orval` ile API client otomatik üretilir, backend değişince frontend type'ları senkron kalır. MVP için manuel daha hızlı.

**S: Validation hem frontend hem backend, neden çift?**
Frontend UX için (anında feedback, network yok). Backend güvenlik için (frontend bypass edilebilir). Frontend'siz API çağrısı yapan biri olursa backend hâlâ koruyor. Same schema (zod ↔ Jakarta Bean Validation) ideal, MVP için manuel mirror.

**S: i18n yapacak mıydın?**
Next.js'in `next-intl` veya yerleşik i18n routing'i var. `/pl/...` ve `/en/...` route'ları. MVP'de İngilizce yeter ama yapı hazır — server component'ler dil parametresini props olarak alır.

**S: Mobile-first responsive yaklaşımın?**
Tailwind'in mobile-first breakpoint'leri (sm:, md:, lg:). Grid `grid-cols-1 sm:grid-cols-2 xl:grid-cols-3` → mobilde tek kolon, geniş ekranda 3. Filter sidebar mobilde üstte, desktop'ta solda sticky. Touch target'lar minimum 44px (Button default'u uygun).

**S: Tailwind 4 farkı?**
JIT artık default, config dosyası optional, `@theme` directive ile design token tanımı, CSS-first konfig. Daha hızlı build.

**S: Frontend'i nasıl scale ederdin?**
- CDN cache (Vercel/Cloudflare) — static asset'lar edge'de
- ISR (Incremental Static Regeneration) — popular salon detay sayfaları cache'lenip arada revalidate
- Server-side cache (Next.js `fetch` cache) — district list 1h
- Image CDN (Cloudinary, Imgix) — Google photo proxy yerine
- Service Worker ile offline read
- Web Vitals monitoring (Sentry, DataDog RUM)

**S: SEO için yaptıkların?**
`generateMetadata`, semantic HTML, server-side render, dynamic sitemap (`/sitemap.ts`), `robots.txt`, structured data (JSON-LD ile `LocalBusiness` schema), Open Graph tag'leri, canonical URL'ler.

---

## 23. Definition of Done

- [ ] `npm install` hatasız
- [ ] `npm run dev` → http://localhost:3000 açılır
- [ ] Listing page'de salonlar görünür (Faz 3 backend'i çalışıyorken)
- [ ] District filter çalışır, URL update olur
- [ ] Rating filter çalışır
- [ ] Search çalışır
- [ ] Pagination Previous/Next çalışır, sayfa scroll top'a gider
- [ ] Salon card'a tıklayınca detail page açılır
- [ ] Detail page URL'i direkt açıldığında SSR yapılır (View Source'ta HTML görünür)
- [ ] `/salons/9999` → 404 sayfası
- [ ] `/login` form'u render olur, validation çalışır
- [ ] Doğru credential ile login → cookie set, `/` redirect
- [ ] Yanlış credential → inline error
- [ ] `/salons/[id]/edit` login olmadan → `/login?redirect=...`
- [ ] Edit form'da validation hatası inline gösterilir
- [ ] Edit submit → optimistic update + toast
- [ ] Backend kapalıyken graceful error
- [ ] Mobil viewport'ta layout düzgün
- [ ] Keyboard ile tüm sayfa navigate edilebilir
- [ ] Lighthouse > 90 Performance, > 95 Accessibility
- [ ] TypeScript strict, `npm run build` hatasız
- [ ] ESLint hatasız
- [ ] En az 3 component test (Vitest) geçer
- [ ] En az 1 e2e test (Playwright) geçer
- [ ] `.env.local` git'te değil, `.env.example` var

---

## ➡️ Sonraki Adım

**Faz 5: Docker Compose ile Tam Stack Entegrasyonu**

Faz 5'te:
- Backend için multi-stage `Dockerfile`
- Frontend için multi-stage `Dockerfile`
- Tek `docker-compose.yml` ile postgres + redis + backend + frontend
- Environment variable yönetimi
- Network isolation
- Health check'ler ve startup order
- Production build optimizasyonları
- Reverse proxy (nginx/Caddy) — opsiyonel
