package com.kaandev.salonexplorer.domain.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.Set;

public record SalonUpdateRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255)
    String name,

    @NotBlank(message = "Address is required")
    String address,

    Long districtId,

    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone must be in E.164 format")
    String phone,

    @URL(message = "Website must be a valid URL")
    @Size(max = 500)
    String website,

    @DecimalMin("0.0") @DecimalMax("5.0")
    BigDecimal rating,

    @Min(0)
    Integer reviewCount,

    @Min(1) @Max(4)
    Short priceLevel,

    Set<Long> serviceIds,

    Boolean isActive
) {}
