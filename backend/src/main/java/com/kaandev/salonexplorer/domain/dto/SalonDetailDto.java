package com.kaandev.salonexplorer.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Schema(description = "Full salon details")
public record SalonDetailDto(
    Long id,
    String name,
    String address,
    DistrictDto district,
    String phone,
    String website,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal rating,
    Integer reviewCount,
    Short priceLevel,
    String photoUrl,
    String description,
    String openingHours,
    Set<ServiceDto> services,
    List<ServiceOfferingDto> serviceOfferings,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
