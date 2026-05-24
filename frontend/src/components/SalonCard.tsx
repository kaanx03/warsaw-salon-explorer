import Link from "next/link";
import type { SalonListItem } from "@/lib/types";

export default function SalonCard({ salon }: { salon: SalonListItem }) {
  return (
    <Link href={`/salons/${salon.id}`} className="group block">
      {/* Square image */}
      <div
        className="relative w-full overflow-hidden"
        style={{ aspectRatio: "1/1", background: "var(--bg-2)", borderRadius: "2px" }}
      >
        <img
          src={salon.photoUrl ?? "/hero.png"}
          alt={salon.name}
          className="absolute inset-0 w-full h-full object-cover group-hover:scale-[1.04]"
          style={{ transition: "transform 700ms cubic-bezier(0.2, 0.7, 0.2, 1)" }}
        />

        {/* Subtle dark overlay on hover */}
        <div
          className="absolute inset-0 opacity-0 group-hover:opacity-[0.06]"
          style={{ background: "var(--ink)", transition: "opacity 300ms ease", pointerEvents: "none" }}
        />
      </div>

      {/* Info below image */}
      <div className="mt-4">
        <div className="flex items-baseline justify-between gap-3">
          <h2
            className="font-display font-normal leading-[1.1] tracking-[-0.012em] line-clamp-2"
            style={{ fontSize: "22px", color: "var(--ink)" }}
          >
            {salon.name}
          </h2>
          <span
            className="font-mono text-[13px] flex items-baseline gap-1 flex-shrink-0"
            style={{ color: "var(--ink)" }}
          >
            <span style={{ color: "var(--accent)", fontSize: "12px" }}>●</span>
            {salon.rating != null ? salon.rating.toFixed(1) : "—"}
          </span>
        </div>
        <p
          className="font-body text-[13px] mt-1.5 flex items-center gap-2"
          style={{ color: "var(--muted)" }}
        >
          <span
            className="inline-block w-[5px] h-[5px] rounded-full flex-shrink-0"
            style={{ background: "var(--accent)", transform: "translateY(-2px)" }}
          />
          {salon.district}
        </p>
      </div>
    </Link>
  );
}
