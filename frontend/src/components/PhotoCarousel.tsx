"use client";

import { useState, useCallback } from "react";

interface Props {
  photoUrls: string[];
  name: string;
}

export default function PhotoCarousel({ photoUrls, name }: Props) {
  const [idx, setIdx] = useState(0);
  const go = useCallback(
    (d: number) => setIdx((i) => (i + d + photoUrls.length) % photoUrls.length),
    [photoUrls.length]
  );

  if (photoUrls.length === 0) return null;

  return (
    <div style={{ margin: "24px 0 64px" }}>
      {/* Main image — 16:9 */}
      <div
        className="relative overflow-hidden"
        style={{ aspectRatio: "16/9", background: "var(--bg-2)", borderRadius: "2px" }}
      >
        {photoUrls.map((src, i) => (
          <img
            key={i}
            src={src}
            alt={i === 0 ? name : ""}
            className="absolute inset-0 w-full h-full object-cover transition-opacity duration-500"
            style={{ opacity: i === idx ? 1 : 0 }}
          />
        ))}

        {photoUrls.length > 1 && (
          <>
            <button
              onClick={() => go(-1)}
              aria-label="Previous photo"
              className="absolute left-4 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full flex items-center justify-center text-xl transition-colors"
              style={{ background: "oklch(0.99 0 0 / 0.94)", color: "var(--ink)" }}
            >
              ‹
            </button>
            <button
              onClick={() => go(1)}
              aria-label="Next photo"
              className="absolute right-4 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full flex items-center justify-center text-xl transition-colors"
              style={{ background: "oklch(0.99 0 0 / 0.94)", color: "var(--ink)" }}
            >
              ›
            </button>
          </>
        )}

        <div
          className="absolute bottom-4 right-4 font-mono text-[11px] tracking-[0.1em] px-3 py-1.5"
          style={{
            background: "oklch(0.18 0.012 60 / 0.86)",
            color: "var(--bg)",
            borderRadius: "999px",
          }}
        >
          {idx + 1} / {photoUrls.length}
        </div>
      </div>

      {/* Thumbnails */}
      {photoUrls.length > 1 && (
        <div
          className="grid gap-2.5 mt-2.5"
          style={{ gridTemplateColumns: `repeat(${Math.min(photoUrls.length, 5)}, 1fr)` }}
        >
          {photoUrls.slice(0, 5).map((src, i) => (
            <button
              key={i}
              onClick={() => setIdx(i)}
              aria-label={`Photo ${i + 1}`}
              className="relative overflow-hidden transition-opacity"
              style={{
                aspectRatio: "4/3",
                background: "var(--bg-2)",
                borderRadius: "2px",
                opacity: i === idx ? 1 : 0.55,
                border: "none",
                padding: 0,
              }}
            >
              <img src={src} alt="" className="w-full h-full object-cover" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
