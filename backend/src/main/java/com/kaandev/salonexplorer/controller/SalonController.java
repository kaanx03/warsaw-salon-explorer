package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.domain.dto.PagedResponse;
import com.kaandev.salonexplorer.domain.dto.SalonDetailDto;
import com.kaandev.salonexplorer.domain.dto.SalonListItemDto;
import com.kaandev.salonexplorer.domain.dto.SalonPatchRequest;
import com.kaandev.salonexplorer.domain.dto.SalonUpdateRequest;
import com.kaandev.salonexplorer.service.SalonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/salons")
@RequiredArgsConstructor
@Tag(name = "Salons", description = "Beauty salon operations")
public class SalonController {

    private final SalonService salonService;

    @GetMapping
    @Operation(summary = "List salons with pagination and filtering")
    public PagedResponse<SalonListItemDto> list(
        @Parameter(description = "District slug")      @RequestParam(required = false) String district,
        @Parameter(description = "Service name")       @RequestParam(required = false) String service,
        @Parameter(description = "Service category")   @RequestParam(required = false) String category,
        @Parameter(description = "Min rating")         @RequestParam(required = false) BigDecimal minRating,
        @Parameter(description = "Name search")        @RequestParam(required = false) String search,
        @ParameterObject @PageableDefault(size = 20, sort = "rating") Pageable pageable
    ) {
        return salonService.list(district, service, category, minRating, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get salon details")
    public SalonDetailDto getById(@PathVariable Long id) {
        return salonService.getById(id);
    }

    @GetMapping("/{id}/photos")
    @Operation(summary = "Get all photo URLs for a salon")
    public List<String> getPhotos(@PathVariable Long id) {
        return salonService.getPhotoUrls(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Replace salon (admin only)")
    public SalonDetailDto update(@PathVariable Long id, @Valid @RequestBody SalonUpdateRequest request) {
        return salonService.update(id, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Partial update (admin only)")
    public SalonDetailDto patch(@PathVariable Long id, @Valid @RequestBody SalonPatchRequest request) {
        return salonService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete (admin only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salonService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
