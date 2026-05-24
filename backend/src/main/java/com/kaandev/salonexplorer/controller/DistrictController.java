package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.domain.dto.DistrictDto;
import com.kaandev.salonexplorer.service.DistrictService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/districts")
@RequiredArgsConstructor
@Tag(name = "Districts")
public class DistrictController {

    private final DistrictService districtService;

    @GetMapping
    public List<DistrictDto> findAll() {
        return districtService.findAll();
    }
}
