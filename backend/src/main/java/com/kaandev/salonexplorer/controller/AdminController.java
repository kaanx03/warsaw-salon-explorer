package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.domain.entity.AuditLog;
import com.kaandev.salonexplorer.ingestion.service.EnrichmentService;
import com.kaandev.salonexplorer.ingestion.service.IngestionResult;
import com.kaandev.salonexplorer.ingestion.service.IngestionService;
import com.kaandev.salonexplorer.repository.AuditLogRepository;
import com.kaandev.salonexplorer.repository.SalonRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin")
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final SalonRepository salonRepository;
    private final EnrichmentService enrichmentService;
    private final IngestionService ingestionService;

    @GetMapping("/audit-log")
    @Operation(summary = "Paginated audit log")
    public Page<AuditLog> getAuditLog(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @GetMapping("/stats")
    @Operation(summary = "Quick stats")
    public Map<String, Object> getStats() {
        return Map.of(
            "totalSalons", salonRepository.count(),
            "activeSalons", salonRepository.countByIsActiveTrue()
        );
    }

    @PostMapping("/enrich")
    @Operation(summary = "Enrich all active salons with description, opening hours and price level from Google Places")
    public EnrichmentService.EnrichmentResult enrich() {
        return enrichmentService.enrichAll();
    }

    @PostMapping("/ingest")
    @Operation(summary = "Run ingestion pipeline with all configured search queries")
    public IngestionResult ingest() {
        return ingestionService.ingestAll();
    }
}
