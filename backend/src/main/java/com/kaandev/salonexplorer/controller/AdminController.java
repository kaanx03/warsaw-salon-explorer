package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.domain.entity.AuditLog;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
