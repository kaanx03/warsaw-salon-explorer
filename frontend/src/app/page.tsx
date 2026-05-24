import { Suspense } from "react";
import { fetchDistricts, fetchSalons, fetchServices } from "@/lib/api";
import FilterBar from "@/components/FilterBar";
import Pagination from "@/components/Pagination";
import SalonGrid from "@/components/SalonGrid";

interface SearchParams {
  district?: string;
  service?: string;
  category?: string;
  minRating?: string;
  search?: string;
  page?: string;
  sort?: string;
}

export default async function HomePage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const sp   = await searchParams;
  const page = sp.page ?? "0";
  const sort = sp.sort ?? "rating,desc";

  const [salonsData, districts, services] = await Promise.all([
    fetchSalons({
      district:  sp.district,
      service:   sp.service,
      category:  sp.category,
      minRating: sp.minRating,
      search:    sp.search,
      page, size: "20", sort,
    }),
    fetchDistricts(),
    fetchServices(),
  ]);

  return (
    <>
      {/* Hero — no bottom border, fades into filter area */}
      <section
        className="relative overflow-hidden text-center"
        style={{
          padding: "110px 0 72px",
          backgroundImage: `
            linear-gradient(180deg,
              oklch(0.972 0.008 80 / 0.10) 0%,
              oklch(0.972 0.008 80 / 0.38) 45%,
              oklch(0.972 0.008 80 / 0.92) 80%,
              oklch(0.972 0.008 80 / 1.00) 100%),
            url('/hero.png')`,
          backgroundSize: "cover",
          backgroundPosition: "center 30%",
          backgroundRepeat: "no-repeat",
        }}
      >
        <div className="relative max-w-[1320px] mx-auto px-8">
          <div
            className="font-mono text-[11px] tracking-[0.22em] uppercase mb-8 flex items-center justify-center gap-4"
            style={{ color: "var(--muted)" }}
          >
            <span className="block w-7 h-px" style={{ background: "var(--ink)" }} />
            A directory of Warsaw&apos;s beauty studios
            <span className="block w-7 h-px" style={{ background: "var(--ink)" }} />
          </div>

          <h1
            className="font-display font-normal leading-[0.98] tracking-[-0.025em] mx-auto mb-7"
            style={{ fontSize: "clamp(48px, 7vw, 96px)", maxWidth: "16ch", color: "var(--ink)" }}
          >
            Discover Warsaw&apos;s<br />
            finest <em style={{ color: "var(--accent)" }}>beauty</em> destinations
          </h1>

          <p
            className="font-body text-[17px] mx-auto leading-relaxed"
            style={{ maxWidth: "44ch", color: "var(--ink-2)" }}
          >
            Explore {salonsData.page.totalElements} curated salons across 18 districts.
            Filter by service, neighbourhood or rating to find your perfect studio.
          </p>
        </div>
      </section>

      {/* Filters — same background, seamless with hero */}
      <Suspense>
        <FilterBar
          districts={districts}
          services={services}
          total={salonsData.page.totalElements}
        />
      </Suspense>

      {/* Grid */}
      <main className="max-w-[1320px] mx-auto px-8 py-14">
        {salonsData.content.length === 0 ? (
          <div className="text-center py-32">
            <h3
              className="font-display font-normal text-[32px] mb-3"
              style={{ color: "var(--ink)" }}
            >
              No salons match those filters
            </h3>
            <p className="font-body text-sm" style={{ color: "var(--muted)" }}>
              Try widening your district or lowering the minimum rating.
            </p>
          </div>
        ) : (
          <SalonGrid salons={salonsData.content} />
        )}

        <Suspense>
          <Pagination
            currentPage={salonsData.page.number}
            totalPages={salonsData.page.totalPages}
            totalElements={salonsData.page.totalElements}
          />
        </Suspense>
      </main>

      {/* Footer */}
      <footer style={{ borderTop: "1px solid var(--rule)", background: "var(--bg-2)", marginTop: "40px" }}>
        <div className="max-w-[1320px] mx-auto px-8 py-10 flex flex-col sm:flex-row items-center justify-between gap-4">
          <span className="font-display text-base italic" style={{ color: "var(--ink-2)" }}>
            Warsaw Beauty <em style={{ color: "var(--accent)" }}>Explorer</em>
          </span>
          <span className="font-body text-sm" style={{ color: "var(--muted)" }}>
            Made by{" "}
            <span className="font-medium" style={{ color: "var(--ink)" }}>Mustafa Kaan Yavuz</span>
            {" "}· 2026
          </span>
        </div>
      </footer>
    </>
  );
}
