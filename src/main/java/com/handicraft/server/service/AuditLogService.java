package com.handicraft.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.handicraft.server.model.AuditChange;
import com.handicraft.server.model.AuditLog;
import com.handicraft.server.model.Employee;
import com.handicraft.server.model.Item;
import com.handicraft.server.repository.AuditLogRepository;
import com.handicraft.server.repository.EmployeeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /** Fields excluded from the diff — too large/noisy to be useful in an audit trail. */
    private static final Set<String> DIFF_IGNORE_FIELDS = Set.of(
        "id", "createdAt", "imageUrl", "imageUrls"
    );

    /**
     * Records a CREATE or UPDATE for an item. `before` is the pre-save
     * snapshot (null for a brand-new item); `after` is what was actually
     * persisted. For UPDATE, a no-op save (nothing actually changed) is
     * silently skipped so the log doesn't fill up with noise.
     */
    public void recordItemChange(String action, Item before, Item after, String performedBy) {
        List<AuditChange> changes = "UPDATE".equals(action) ? diff(before, after) : List.of();
        if ("UPDATE".equals(action) && changes.isEmpty()) return;

        Item reference = after != null ? after : before;
        Employee performer = resolveEmployee(performedBy);

        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType("Item");
        log.setEntityId(reference.getId());
        log.setCategory(reference.getCategory());
        log.setItemCode(reference.getItemCode());
        log.setPerformedBy(performedBy);
        log.setPerformedByRole(performer != null ? performer.getRole() : null);
        log.setPerformedByLocation(performer != null && performer.getAssignedLocations() != null
            ? String.join(", ", performer.getAssignedLocations())
            : null);
        log.setTimestamp(System.currentTimeMillis());
        log.setChanges(changes);
        log.setSummary(buildSummary(action, reference, changes));

        auditLogRepository.save(log);
    }

    public Page<AuditLog> getAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /** Full history for one item (oldest first) — who created it and every edit since. */
    public List<AuditLog> getForEntity(String entityId) {
        return auditLogRepository.findByEntityIdOrderByTimestampAsc(entityId);
    }

    public void deleteAuditLog(String id) {
        if (!auditLogRepository.existsById(id)) {
            throw new IllegalArgumentException("Audit log entry not found: " + id);
        }
        auditLogRepository.deleteById(id);
    }

    private Employee resolveEmployee(String employeeName) {
        if (employeeName == null || employeeName.isBlank()) return null;
        return employeeRepository.findByEmployeeNameIgnoreCase(employeeName.trim()).orElse(null);
    }

    private String displayLabel(Item item) {
        return item.getItemCode() != null && !item.getItemCode().isBlank() ? item.getItemCode() : item.getId();
    }

    private String buildSummary(String action, Item item, List<AuditChange> changes) {
        String label = (item.getCategory() != null ? item.getCategory() : "Item") + " " + displayLabel(item);
        switch (action) {
            case "CREATE": return "Added " + label;
            case "DELETE": return "Deleted " + label;
            default:
                return "Updated " + label + " (" + changes.size() + " field" + (changes.size() == 1 ? "" : "s") + " changed)";
        }
    }

    @SuppressWarnings("unchecked")
    private List<AuditChange> diff(Item before, Item after) {
        if (before == null || after == null) return List.of();

        Map<String, Object> beforeMap = objectMapper.convertValue(before, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> afterMap = objectMapper.convertValue(after, new TypeReference<Map<String, Object>>() {});

        List<AuditChange> result = new ArrayList<>();
        for (String key : afterMap.keySet()) {
            if (DIFF_IGNORE_FIELDS.contains(key)) continue;

            Object oldVal = beforeMap.get(key);
            Object newVal = afterMap.get(key);
            if (!Objects.equals(oldVal, newVal)) {
                result.add(new AuditChange(key, asString(oldVal), asString(newVal)));
            }
        }
        return result;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
