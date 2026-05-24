import Link from "next/link";
import { fetchSalonCount } from "@/lib/api";

function BeautyLogo() {
  return (
    <svg width="30" height="30" viewBox="0 0 30 30" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* Outer petal ring */}
      <ellipse cx="15" cy="7"  rx="3" ry="5.5" fill="var(--ink)" transform="rotate(0   15 15)" />
      <ellipse cx="15" cy="7"  rx="3" ry="5.5" fill="var(--ink)" transform="rotate(45  15 15)" />
      <ellipse cx="15" cy="7"  rx="3" ry="5.5" fill="var(--ink)" transform="rotate(90  15 15)" />
      <ellipse cx="15" cy="7"  rx="3" ry="5.5" fill="var(--ink)" transform="rotate(135 15 15)" />
      {/* Center */}
      <circle cx="15" cy="15" r="4" fill="var(--bg)" />
      <circle cx="15" cy="15" r="2" fill="var(--accent)" />
    </svg>
  );
}

export default async function Navbar() {
  const count = await fetchSalonCount();
  return (
    <header style={{ borderBottom: "1px solid var(--rule)", background: "var(--bg)" }}>
      <div className="max-w-[1320px] mx-auto px-8 h-[76px] flex items-center justify-between">
        <Link href="/" className="flex items-center gap-3">
          <BeautyLogo />
          <span className="font-display text-[24px] leading-none" style={{ color: "var(--ink)" }}>
            Warsaw Beauty <em style={{ color: "var(--accent)" }}>Explorer</em>
          </span>
        </Link>

        <div
          className="font-mono text-[11px] tracking-[0.12em] uppercase hidden sm:flex gap-6"
          style={{ color: "var(--muted)" }}
        >
          <span><b style={{ color: "var(--ink)", fontWeight: 500 }}>{count}</b> salons</span>
          <span><b style={{ color: "var(--ink)", fontWeight: 500 }}>18</b> districts</span>
          <span className="hidden lg:block">Edition 01 · 2026</span>
        </div>
      </div>
    </header>
  );
}
