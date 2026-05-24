package com.kaandev.salonexplorer.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Salon summary for list views")
public record SalonListItemDto(
    @Schema(example = "1") Long id,
    @Schema(example = "Salon Fryzjerski Anna") String name,
    @Schema(example = "Mokotów") String district,
    @Schema(example = "4.7") BigDecimal rating,
    @Schema(example = "142") Integer reviewCount,
    @Schema(example = "2") Short priceLevel,
    @Schema(example = "/api/v1/photos/places%2FABC%2Fphotos%2FXYZ") String photoUrl
) {}
