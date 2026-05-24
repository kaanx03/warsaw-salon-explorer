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

export interface SalonListItem {
  id: number;
  name: string;
  district: string;
  rating: number;
  reviewCount: number;
  priceLevel: number | null;
  photoUrl: string | null;
}

export interface DistrictDto {
  id: number;
  name: string;
  slug: string;
}

export interface ServiceDto {
  id: number;
  name: string;
  category: string;
}

export interface ServiceOfferingDto {
  id: number;
  name: string;
  category: string | null;
  pricePln: number | null;
  durationMinutes: number | null;
}

export interface SalonDetail {
  id: number;
  name: string;
  address: string;
  district: DistrictDto;
  phone: string | null;
  website: string | null;
  latitude: number | null;
  longitude: number | null;
  rating: number;
  reviewCount: number;
  priceLevel: number | null;
  photoUrl: string | null;
  description: string | null;
  openingHours: string | null;
  services: ServiceDto[];
  serviceOfferings: ServiceOfferingDto[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}
