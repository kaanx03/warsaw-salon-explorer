package com.kaandev.salonexplorer.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
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
    Set<ServiceDto> services,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
