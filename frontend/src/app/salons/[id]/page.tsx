import { fetchSalon, fetchSalonPhotos } from "@/lib/api";
import PhotoCarousel from "@/components/PhotoCarousel";
import type { Metadata } from "next";

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }): Promise<Metadata> {
  const { id } = await params;
  try {
    const salon = await fetchSalon(id);
    return { title: `${salon.name} — Warsaw Beauty Explorer` };
  } catch {
    return { title: "Salon — Warsaw Beauty Explorer" };
  }
}

export default async function SalonDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [salon, photoUrls] = await Promise.all([fetchSalon(id), fetchSalonPhotos(id)]);
  const allPhotos = photoUrls.length > 0 ? photoUrls : salon.photoUrl ? [salon.photoUrl] : ["/hero.png"];

  const mapQ = encodeURIComponent(
    `${salon.address ?? ""}, ${salon.district?.name ?? ""}, Warszawa, Poland`
  );
  const mapSrc = salon.latitude && salon.longitude
    ? `https://www.google.com/maps?q=${salon.latitude},${salon.longitude}&z=15&output=embed`
    : `https://maps.google.com/maps?q=${mapQ}&t=&z=15&ie=UTF8&iwloc=&output=embed`;

  const hasOfferings = salon.serviceOfferings && salon.serviceOfferings.length > 0;

  const offeringsByCategory = (salon.serviceOfferings ?? []).reduce<Record<string, typeof salon.serviceOfferings>>(
    (acc, s) => {
      const cat = s.category ?? "Other";
      if (!acc[cat]) acc[cat] = [];
      acc[cat].push(s);
      return acc;
    },
    {}
  );

  const servicesByCategory = (salon.services ?? []).reduce<Record<string, typeof salon.services>>(
    (acc, s) => {
      const cat = s.category ?? "OTHER";
      if (!acc[cat]) acc[cat] = [];
      acc[cat].push(s);
      return acc;
    },
    {}
  );

  return (
    <div style={{ background: "var(--bg)" }}>
      {/* Head: breadcrumb + title + meta */}
      <section className="max-w-[1320px] mx-auto px-8 pt-14 pb-8">
        <div
          className="font-mono text-[11px] tracking-[0.16em] uppercase mb-6"
          style={{ color: "var(--muted)" }}
        >
          {salon.district?.name}
          <span style={{ margin: "0 10px", color: "var(--rule)" }}>/</span>
          Beauty
        </div>

        <h1
          className="font-display font-normal leading-[1] tracking-[-0.025em] mb-6"
          style={{ fontSize: "clamp(48px, 6vw, 84px)", color: "var(--ink)" }}
        >
          {salon.name}
        </h1>

        <div className="flex flex-wrap items-center gap-7 font-body text-sm" style={{ color: "var(--ink-2)" }}>
          <span className="flex items-center gap-2">
            <span style={{ color: "var(--accent)", letterSpacing: "0.1em" }}>
              {"★".repeat(Math.round(salon.rating ?? 0))}{"☆".repeat(5 - Math.round(salon.rating ?? 0))}
            </span>
            <b style={{ marginLeft: 6, color: "var(--ink)" }}>{salon.rating != null ? salon.rating.toFixed(1) : "—"}</b>
            <span style={{ color: "var(--muted)", marginLeft: 4 }}>
              · {salon.reviewCount.toLocaleString()} reviews
            </span>
          </span>
        </div>
      </section>

      {/* Carousel */}
      <div className="max-w-[1320px] mx-auto px-8">
        <PhotoCarousel photoUrls={allPhotos} name={salon.name} />
      </div>

      {/* Body: about + sidebar */}
      <div className="max-w-[1320px] mx-auto px-8 pb-20">
        <div className="grid grid-cols-1 lg:grid-cols-[1.55fr_1fr] gap-12 lg:gap-20">
          {/* Left */}
          <div>
            <h2
              className="font-mono text-[11px] tracking-[0.2em] uppercase font-medium pb-3.5 mb-5"
              style={{ color: "var(--muted)", borderBottom: "1px solid var(--rule)" }}
            >
              About the studio
            </h2>
            <p
              className="font-display font-normal leading-[1.4] mb-10"
              style={{ fontSize: "26px", color: "var(--ink)", maxWidth: "32ch", letterSpacing: "-0.005em" }}
            >
              {salon.description ?? (
                <>
                  {salon.name} offers professional beauty services in{" "}
                  <em style={{ color: "var(--accent)" }}>{salon.district?.name}</em>, Warsaw.
                </>
              )}
            </p>

            {/* Services — Booksy offerings (with price) preferred, fallback to Google catalog */}
            {hasOfferings
              ? Object.entries(offeringsByCategory).map(([cat, svcs]) => (
                  <div key={cat} className="mb-8">
                    <h3
                      className="font-mono text-[10px] tracking-[0.18em] uppercase mb-3 font-medium"
                      style={{ color: "var(--muted)" }}
                    >
                      {cat}
                    </h3>
                    <ul style={{ listStyle: "none", margin: 0, padding: 0 }}>
                      {svcs.map((sv, i) => (
                        <li
                          key={sv.id}
                          className="py-4 flex items-baseline justify-between gap-6"
                          style={{
                            borderBottom: i < svcs.length - 1 ? "1px solid var(--rule-soft)" : "none",
                          }}
                        >
                          <div>
                            <span className="font-display text-[18px] tracking-[-0.005em]" style={{ color: "var(--ink)" }}>
                              {sv.name}
                            </span>
                            {sv.durationMinutes && (
                              <span className="font-mono text-[10px] ml-3 tracking-[0.08em]" style={{ color: "var(--muted)" }}>
                                {sv.durationMinutes} min
                              </span>
                            )}
                          </div>
                          {sv.pricePln && (
                            <span className="font-mono text-[13px] flex-shrink-0" style={{ color: "var(--accent)" }}>
                              {Number(sv.pricePln).toFixed(0)} zł
                            </span>
                          )}
                        </li>
                      ))}
                    </ul>
                  </div>
                ))
              : Object.entries(servicesByCategory).map(([cat, svcs]) => (
                  <div key={cat} className="mb-8">
                    <h3
                      className="font-mono text-[10px] tracking-[0.18em] uppercase mb-3 font-medium"
                      style={{ color: "var(--muted)" }}
                    >
                      {cat.charAt(0) + cat.slice(1).toLowerCase()}
                    </h3>
                    <ul style={{ listStyle: "none", margin: 0, padding: 0 }}>
                      {svcs.map((sv, i) => (
                        <li
                          key={sv.id}
                          className="py-5 flex items-baseline justify-between gap-6"
                          style={{ borderBottom: i < svcs.length - 1 ? "1px solid var(--rule-soft)" : "none" }}
                        >
                          <span className="font-display text-[19px] tracking-[-0.005em]" style={{ color: "var(--ink)" }}>
                            {sv.name}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </div>
                ))
            }

            {!hasOfferings && salon.services?.length === 0 && (
              <p className="font-body text-sm" style={{ color: "var(--muted)" }}>
                Contact the salon for a full list of services and pricing.
              </p>
            )}
          </div>

          {/* Right: sticky info card */}
          <aside>
            <div
              className="sticky"
              style={{
                top: 24,
                background: "var(--surface)",
                border: "1px solid var(--rule)",
                borderRadius: "2px",
                padding: "32px",
              }}
            >
              {salon.address && <InfoRow label="Address" value={salon.address} />}
              {salon.district?.name && <InfoRow label="District" value={salon.district.name} />}
              {salon.phone && <InfoRow label="Phone" value={salon.phone} />}
              {salon.openingHours && <InfoRowMultiline label="Hours" value={salon.openingHours} />}
              {salon.website && (
                <div
                  className="py-4 flex justify-between items-start gap-4"
                  style={{ borderBottom: "1px solid var(--rule-soft)" }}
                >
                  <span
                    className="font-mono text-[10px] tracking-[0.18em] uppercase flex-shrink-0 pt-0.5"
                    style={{ color: "var(--muted)" }}
                  >
                    Website
                  </span>
                  <a
                    href={salon.website}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-body text-sm text-right truncate transition-opacity hover:opacity-70 min-w-0"
                    style={{ color: "var(--accent)", maxWidth: "180px", display: "block" }}
                  >
                    {salon.website.replace(/^https?:\/\//, "")}
                  </a>
                </div>
              )}

              <a
                href={`https://www.google.com/maps?q=${mapQ}`}
                target="_blank"
                rel="noopener noreferrer"
                className="block w-full text-center font-body text-sm tracking-[0.04em] mt-4 py-4 transition-opacity hover:opacity-80"
                style={{ background: "var(--ink)", color: "var(--bg)", borderRadius: "2px" }}
              >
                Open in Google Maps
              </a>
            </div>
          </aside>
        </div>
      </div>

      {/* Map section */}
      <section
        className="max-w-[1320px] mx-auto px-8 pb-20"
        style={{ paddingTop: "64px", borderTop: "1px solid var(--rule)" }}
      >
        <h2
          className="font-mono text-[11px] tracking-[0.2em] uppercase font-medium mb-6"
          style={{ color: "var(--muted)" }}
        >
          Find the studio
        </h2>

        <div className="flex items-end justify-between gap-10 flex-wrap mb-6">
          <h3
            className="font-display font-normal leading-[1.1] m-0"
            style={{ fontSize: "36px", letterSpacing: "-0.015em", color: "var(--ink)" }}
          >
            On the map.<br />
            <span style={{ color: "var(--muted)" }}>Tap to open directions.</span>
          </h3>
          {salon.latitude && salon.longitude && (
            <div
              className="font-mono text-[12px] tracking-[0.06em]"
              style={{ color: "var(--muted)" }}
            >
              {salon.latitude.toFixed(4)}° N · {salon.longitude.toFixed(4)}° E
            </div>
          )}
        </div>

        <div
          className="overflow-hidden"
          style={{
            width: "100%",
            aspectRatio: "16/7",
            border: "1px solid var(--rule)",
            borderRadius: "2px",
            background: "var(--bg-2)",
          }}
        >
          <iframe
            title={`Map of ${salon.name}`}
            src={mapSrc}
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
            allowFullScreen
            style={{
              width: "100%",
              height: "100%",
              border: 0,
              display: "block",
              filter: "saturate(0.6) contrast(0.96)",
            }}
          />
        </div>
      </section>

      {/* Footer */}
      <footer style={{ borderTop: "1px solid var(--rule)", background: "var(--bg-2)" }}>
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
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div
      className="py-4 flex justify-between items-start gap-4"
      style={{ borderBottom: "1px solid var(--rule-soft)" }}
    >
      <span
        className="font-mono text-[10px] tracking-[0.18em] uppercase flex-shrink-0 pt-0.5"
        style={{ color: "var(--muted)" }}
      >
        {label}
      </span>
      <span className="font-body text-sm text-right leading-snug" style={{ color: "var(--ink)" }}>
        {value}
      </span>
    </div>
  );
}

function InfoRowMultiline({ label, value }: { label: string; value: string }) {
  return (
    <div
      className="py-4 flex justify-between items-start gap-4"
      style={{ borderBottom: "1px solid var(--rule-soft)" }}
    >
      <span
        className="font-mono text-[10px] tracking-[0.18em] uppercase flex-shrink-0 pt-0.5"
        style={{ color: "var(--muted)" }}
      >
        {label}
      </span>
      <div className="font-body text-sm text-right leading-relaxed" style={{ color: "var(--ink)" }}>
        {value.split("\n").map((line, i) => (
          <div key={i}>{line}</div>
        ))}
      </div>
    </div>
  );
}
