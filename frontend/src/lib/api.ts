import type { DistrictDto, PagedResponse, SalonDetail, SalonListItem, ServiceDto } from "./types";

const BACKEND = process.env.BACKEND_URL ?? "http://localhost:8080";

export async function fetchSalons(
  params: Record<string, string | undefined>
): Promise<PagedResponse<SalonListItem>> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== "") query.set(k, v);
  });
  const res = await fetch(`${BACKEND}/api/v1/salons?${query}`, { cache: "no-store" });
  if (!res.ok) throw new Error("Failed to fetch salons");
  return res.json();
}

export async function fetchSalon(id: string): Promise<SalonDetail> {
  const res = await fetch(`${BACKEND}/api/v1/salons/${id}`, {
    cache: "no-store",
  });
  if (!res.ok) throw new Error("Salon not found");
  return res.json();
}

export async function fetchDistricts(): Promise<DistrictDto[]> {
  const res = await fetch(`${BACKEND}/api/v1/districts`, {
    next: { revalidate: 3600 },
  });
  if (!res.ok) throw new Error("Failed to fetch districts");
  return res.json();
}

export async function fetchSalonPhotos(id: string): Promise<string[]> {
  try {
    const res = await fetch(`${BACKEND}/api/v1/salons/${id}/photos`, {
      cache: "no-store",
    });
    if (!res.ok) return [];
    return res.json();
  } catch {
    return [];
  }
}

export async function fetchServices(): Promise<ServiceDto[]> {
  const res = await fetch(`${BACKEND}/api/v1/services`, {
    next: { revalidate: 3600 },
  });
  if (!res.ok) throw new Error("Failed to fetch services");
  return res.json();
}

export async function fetchSalonCount(): Promise<number> {
  try {
    const res = await fetch(`${BACKEND}/api/v1/salons?size=1`, { cache: "no-store" });
    if (!res.ok) return 0;
    const data: PagedResponse<SalonListItem> = await res.json();
    return data.page.totalElements;
  } catch {
    return 0;
  }
}
