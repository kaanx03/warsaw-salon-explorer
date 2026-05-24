"use client";

import { useRouter, useSearchParams, usePathname } from "next/navigation";

interface Props {
  currentPage: number;
  totalPages: number;
  totalElements: number;
}

export default function Pagination({ currentPage, totalPages }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const goTo = (page: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(page));
    router.push(`${pathname}?${params}`);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  if (totalPages <= 1) return null;

  const page = currentPage + 1;
  const pages: (number | string)[] = [];
  pages.push(1);
  if (page > 3) pages.push("..a");
  for (let p = Math.max(2, page - 1); p <= Math.min(totalPages - 1, page + 1); p++) pages.push(p);
  if (page < totalPages - 2) pages.push("..b");
  if (totalPages > 1) pages.push(totalPages);

  return (
    <nav
      className="flex items-center justify-center gap-1.5 mt-20 pt-10"
      style={{ borderTop: "1px solid var(--rule-soft)" }}
      aria-label="Pagination"
    >
      <button
        onClick={() => goTo(currentPage - 1)}
        disabled={currentPage === 0}
        className="font-mono text-[12px] tracking-[0.14em] uppercase px-4 h-10 flex items-center justify-center transition-colors disabled:opacity-30"
        style={{ border: "1px solid transparent", borderRadius: "50px", color: "var(--ink-2)" }}
      >
        ← Prev
      </button>

      {pages.map((p, i) =>
        typeof p === "string" ? (
          <span key={i} className="font-mono text-[13px] px-1" style={{ color: "var(--muted)" }}>…</span>
        ) : (
          <button
            key={i}
            onClick={() => goTo(p - 1)}
            className="font-mono text-[13px] w-10 h-10 flex items-center justify-center transition-all"
            style={{
              borderRadius: "50%",
              border: `1px solid ${currentPage + 1 === p ? "var(--ink)" : "transparent"}`,
              background: currentPage + 1 === p ? "var(--ink)" : "transparent",
              color: currentPage + 1 === p ? "var(--bg)" : "var(--ink-2)",
            }}
            aria-current={currentPage + 1 === p ? "page" : undefined}
          >
            {p}
          </button>
        )
      )}

      <button
        onClick={() => goTo(currentPage + 1)}
        disabled={currentPage >= totalPages - 1}
        className="font-mono text-[12px] tracking-[0.14em] uppercase px-4 h-10 flex items-center justify-center transition-colors disabled:opacity-30"
        style={{ border: "1px solid transparent", borderRadius: "50px", color: "var(--ink-2)" }}
      >
        Next →
      </button>
    </nav>
  );
}
