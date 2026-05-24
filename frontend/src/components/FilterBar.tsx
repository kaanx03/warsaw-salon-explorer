"use client";

import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { useCallback, useState } from "react";
import type { DistrictDto, ServiceDto } from "@/lib/types";

const CATEGORIES = [
  { label: "All",   value: "" },
  { label: "Hair",  value: "HAIR" },
  { label: "Nails", value: "NAILS" },
  { label: "Face",  value: "FACE" },
  { label: "Body",  value: "BODY" },
];

const RATINGS = [
  { label: "Any",  value: "" },
  { label: "4+",   value: "4" },
  { label: "4.5+", value: "4.5" },
  { label: "4.8+", value: "4.8" },
];

interface Props {
  districts: DistrictDto[];
  services:  ServiceDto[];
  total:     number;
}

export default function FilterBar({ districts, services, total }: Props) {
  const router   = useRouter();
  const pathname = usePathname();
  const sp       = useSearchParams();

  const [search, setSearch]   = useState(sp.get("search") ?? "");
  const [focused, setFocused] = useState(false);

  const push = useCallback(
    (updates: Record<string, string>) => {
      const params = new URLSearchParams(sp.toString());
      Object.entries(updates).forEach(([k, v]) => {
        if (v) params.set(k, v); else params.delete(k);
      });
      params.delete("page");
      router.push(`${pathname}?${params}`);
    },
    [router, pathname, sp]
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    push({ search });
  };

  const handleClearSearch = () => {
    setSearch("");
    push({ search: "" });
  };

  const activeCategory = sp.get("category") ?? "";
  const activeDistrict = sp.get("district") ?? "";
  const activeService  = sp.get("service")  ?? "";
  const activeRating   = sp.get("minRating") ?? "";
  const isDirty = !!(activeDistrict || activeService || activeRating || sp.get("search"));

  const clearAll = () => {
    setSearch("");
    router.push(pathname);
  };

  return (
    <div style={{ background: "var(--bg)", paddingBottom: "20px", borderBottom: "1px solid var(--rule)" }}>
      <div className="max-w-[1320px] mx-auto px-8 flex flex-col gap-4">

        {/* Search bar — only fires on submit */}
        <form
          onSubmit={handleSubmit}
          className="flex items-stretch overflow-hidden"
          style={{
            border: `1px solid ${focused ? "var(--ink)" : "var(--rule)"}`,
            borderRadius: "999px",
            background: "var(--surface)",
            boxShadow: focused
              ? "0 16px 40px -18px oklch(0.2 0.02 60 / 0.5)"
              : "0 8px 28px -16px oklch(0.2 0.02 60 / 0.25)",
            transition: "border-color 200ms ease, box-shadow 200ms ease",
          }}
        >
          <svg
            className="ml-7 self-center flex-shrink-0"
            width="18" height="18" viewBox="0 0 24 24"
            fill="none" stroke="currentColor"
            strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"
            style={{ color: "var(--muted)" }}
          >
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>

          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            placeholder="Search by salon, neighbourhood or service…"
            className="flex-1 font-body text-base px-4 py-4 bg-transparent focus:outline-none min-w-0"
            style={{ color: "var(--ink)", cursor: "text" }}
          />

          {search && (
            <button
              type="button"
              onClick={handleClearSearch}
              className="px-4 text-xl self-center"
              style={{ color: "var(--muted)" }}
            >
              ×
            </button>
          )}

          <button
            type="submit"
            className="px-10 font-body text-sm tracking-[0.01em] flex-shrink-0"
            style={{ background: "var(--ink)", color: "var(--bg)", borderRadius: "0 999px 999px 0" }}
            onMouseEnter={(e) => { e.currentTarget.style.background = "var(--accent)"; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = "var(--ink)"; }}
          >
            Search
          </button>
        </form>

        {/* Pills + Refine in one row */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          {/* Category pills */}
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((cat) => {
              const active = activeCategory === cat.value;
              return (
                <button
                  key={cat.value}
                  onClick={() => push({ category: cat.value })}
                  className="font-body text-sm px-4 py-[7px]"
                  style={{
                    borderRadius: "999px",
                    border: `1px solid ${active ? "var(--ink)" : "var(--rule)"}`,
                    background: active ? "var(--ink)" : "transparent",
                    color: active ? "var(--bg)" : "var(--ink-2)",
                  }}
                  onMouseEnter={(e) => {
                    if (!active) {
                      e.currentTarget.style.borderColor = "var(--ink)";
                      e.currentTarget.style.color = "var(--ink)";
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!active) {
                      e.currentTarget.style.borderColor = "var(--rule)";
                      e.currentTarget.style.color = "var(--ink-2)";
                    }
                  }}
                >
                  {cat.label}
                </button>
              );
            })}
          </div>

          {/* Refine controls */}
          <div className="flex flex-wrap items-center gap-3">
            <div className="relative">
              <select
                value={activeDistrict}
                onChange={(e) => push({ district: e.target.value })}
                className="font-body pl-3.5 pr-8 py-2 focus:outline-none appearance-none"
                style={{ background: "var(--surface)", border: "1px solid var(--rule)", borderRadius: "4px", color: "var(--ink)", minWidth: "148px", fontSize: "13px" }}
              >
                <option value="">All districts</option>
                {districts.map((d) => <option key={d.id} value={d.slug}>{d.name}</option>)}
              </select>
              <div className="absolute right-3 top-1/2 pointer-events-none" style={{ transform: "translateY(-70%) rotate(45deg)", width: 6, height: 6, borderRight: "1px solid var(--muted)", borderBottom: "1px solid var(--muted)" }} />
            </div>

            <div className="relative">
              <select
                value={activeService}
                onChange={(e) => push({ service: e.target.value })}
                className="font-body pl-3.5 pr-8 py-2 focus:outline-none appearance-none"
                style={{ background: "var(--surface)", border: "1px solid var(--rule)", borderRadius: "4px", color: "var(--ink)", minWidth: "148px", fontSize: "13px" }}
              >
                <option value="">All services</option>
                {services.map((s) => <option key={s.id} value={s.name}>{s.name}</option>)}
              </select>
              <div className="absolute right-3 top-1/2 pointer-events-none" style={{ transform: "translateY(-70%) rotate(45deg)", width: 6, height: 6, borderRight: "1px solid var(--muted)", borderBottom: "1px solid var(--muted)" }} />
            </div>

            <div
              className="inline-flex items-center gap-0.5 p-[3px]"
              style={{ background: "var(--surface)", border: "1px solid var(--rule)", borderRadius: "4px" }}
            >
              {RATINGS.map((r) => {
                const active = activeRating === r.value;
                return (
                  <button
                    key={r.value}
                    onClick={() => push({ minRating: r.value })}
                    className="font-mono text-[11px] px-2.5 py-1.5"
                    style={{ borderRadius: "3px", background: active ? "var(--ink)" : "transparent", color: active ? "var(--bg)" : "var(--muted)" }}
                    onMouseEnter={(e) => { if (!active) e.currentTarget.style.color = "var(--ink)"; }}
                    onMouseLeave={(e) => { if (!active) e.currentTarget.style.color = "var(--muted)"; }}
                  >
                    {r.label}
                  </button>
                );
              })}
            </div>

            <span className="font-mono text-[11px] tracking-[0.08em] uppercase ml-1" style={{ color: "var(--muted)" }}>
              <b style={{ color: "var(--ink)", fontWeight: 500 }}>{total}</b> results
            </span>

            {isDirty && (
              <button
                onClick={clearAll}
                className="font-mono text-[11px] tracking-[0.1em] uppercase underline underline-offset-4"
                style={{ color: "var(--muted)" }}
                onMouseEnter={(e) => { e.currentTarget.style.color = "var(--accent)"; }}
                onMouseLeave={(e) => { e.currentTarget.style.color = "var(--muted)"; }}
              >
                Clear
              </button>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}
