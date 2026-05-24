package com.kaandev.salonexplorer.domain.dto;

import com.kaandev.salonexplorer.domain.enums.ServiceCategory;

public record ServiceDto(Long id, String name, ServiceCategory category) {}
