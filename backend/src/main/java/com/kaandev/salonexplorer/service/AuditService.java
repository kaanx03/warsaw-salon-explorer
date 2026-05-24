package com.kaandev.salonexplorer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaandev.salonexplorer.domain.entity.AuditLog;
import com.kaandev.salonexplorer.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityType, Long entityId, Object before, Object after) {
        try {
            var diff = computeDiff(before, after);
            saveLog(entityType, entityId, "UPDATE", diff);
        } catch (Exception e) {
            log.error("Failed to write audit log for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityType, Long entityId) {
        saveLog(entityType, entityId, "DELETE", null);
    }

    private void saveLog(String type, Long id, String action, String changes) {
        var entry = AuditLog.builder()
            .entityType(type)
            .entityId(id)
            .action(action)
            .changes(changes)
            .ipAddress(resolveIp())
            .userAgent(request.getHeader("User-Agent"))
            .build();
        repository.save(entry);
    }

    private String computeDiff(Object before, Object after) throws Exception {
        var node = objectMapper.createObjectNode();
        node.set("before", objectMapper.valueToTree(before));
        node.set("after", objectMapper.valueToTree(after));
        return objectMapper.writeValueAsString(node);
    }

    private String resolveIp() {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
