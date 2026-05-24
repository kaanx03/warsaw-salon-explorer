package com.kaandev.salonexplorer.domain.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.Set;

public record SalonPatchRequest(
    @Size(min = 2, max = 255) String name,
    String address,
    Long districtId,
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") String phone,
    @URL @Size(max = 500) String website,
    @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
    @Min(0) Integer reviewCount,
    @Min(1) @Max(4) Short priceLevel,
    Set<Long> serviceIds,
    Boolean isActive
) {}
