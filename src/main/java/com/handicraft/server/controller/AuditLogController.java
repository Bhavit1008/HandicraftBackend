package com.handicraft.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.handicraft.server.model.AuditLog;
import com.handicraft.server.service.AuditLogService;

@RestController
@CrossOrigin(origins = "*")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    /** Newest-first, paginated. Access is gated client-side (admin-only route). */
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getAll(pageable));
    }

    /** Full history for one item — not admin-gated, since both roles can see who added/edited an item. */
    @GetMapping("/audit-logs/entity/{entityId}")
    public ResponseEntity<java.util.List<AuditLog>> getAuditLogsForEntity(@PathVariable String entityId) {
        return ResponseEntity.ok(auditLogService.getForEntity(entityId));
    }

    @DeleteMapping("/audit-logs/{id}")
    public ResponseEntity<?> deleteAuditLog(@PathVariable String id) {
        try {
            auditLogService.deleteAuditLog(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
