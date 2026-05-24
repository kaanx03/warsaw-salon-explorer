package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.domain.dto.ServiceDto;
import com.kaandev.salonexplorer.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Services")
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping
    public List<ServiceDto> findAll() {
        return serviceCatalogService.findAll();
    }
}
