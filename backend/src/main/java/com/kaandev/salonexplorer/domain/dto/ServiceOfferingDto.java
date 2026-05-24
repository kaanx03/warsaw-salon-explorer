package com.kaandev.salonexplorer.domain.dto;

import java.math.BigDecimal;

public record ServiceOfferingDto(
    Long id,
    String name,
    String category,
    BigDecimal pricePln,
    Integer durationMinutes
) {}
