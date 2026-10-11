package com.handicraft.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.handicraft.server.model.Item;
import com.handicraft.server.repository.ItemRepository;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Save or update an item. Every save is recorded to the audit log (CREATE
     * or UPDATE) — this is the single choke point for all Item writes, so
     * hooking in here catches all of them without duplicating the call at
     * every call site.
     */
    public Item saveItem(Item item, String performedBy) {
        validateUniqueCode(item);

        Item existing = item.getId() != null
                ? itemRepository.findById(item.getId()).orElse(null)
                : null;

        assignCreatedAt(item, existing);
        Item saved = itemRepository.save(item);

        recordAudit(existing == null ? "CREATE" : "UPDATE", existing, saved, performedBy);
        return saved;
    }

    /** Audit logging must never block or fail the actual save/delete. */
    private void recordAudit(String action, Item before, Item after, String performedBy) {
        try {
            auditLogService.recordItemChange(action, before, after, performedBy);
        } catch (Exception e) {
            System.err.println("Audit logging failed: " + e.getMessage());
        }
    }

    /** Item Code must be unique — blank values are exempt, and an item is always allowed to keep its own existing code. */
    private void validateUniqueCode(Item item) {
        String code = item.getItemCode();
        if (code == null || code.isBlank()) return;

        boolean inUse = itemRepository.findByItemCode(code).stream()
                .anyMatch(existing -> !existing.getId().equals(item.getId()));
        if (inUse) {
            throw new IllegalArgumentException("Item Code \"" + code + "\" is already in use.");
        }
    }

    /**
     * Stamp createdAt once, on first insert, and keep it stable across later
     * status/detail updates (which re-post the full item via the same
     * upsert endpoint) so "recently added" ordering reflects creation time,
     * not last edit time.
     */
    private void assignCreatedAt(Item item, Item existing) {
        if (item.getCreatedAt() != null) return;

        Long existingCreatedAt = existing != null ? existing.getCreatedAt() : null;
        item.setCreatedAt(existingCreatedAt != null ? existingCreatedAt : System.currentTimeMillis());
    }

    /** Delete an item by ID. Cleans up its image from Cloudinary. */
    public void deleteItem(String itemId, String performedBy) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
            cloudinaryService.deleteImage(item.getImageUrl());
        }

        itemRepository.deleteById(itemId);
        recordAudit("DELETE", item, null, performedBy);
    }
}
