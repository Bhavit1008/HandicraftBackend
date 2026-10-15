package com.handicraft.server.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.handicraft.server.model.CoasterStock;
import com.handicraft.server.service.CoasterStockService;
import com.handicraft.server.service.CoasterStockService.StockUpdate;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/coasterStock")
public class CoasterStockController {

    @Autowired
    private CoasterStockService coasterStockService;

    @GetMapping
    public List<CoasterStock> getAll() {
        return coasterStockService.getAll();
    }

    /** Bulk update of stock counts, e.g. [{ "id": "...", "stock": 120 }]. Returns the full, updated list. */
    @PutMapping("/stock")
    public ResponseEntity<?> updateStock(
            @RequestBody List<StockUpdate> updates,
            @RequestHeader(value = "X-Employee-Name", required = false) String performedBy) {
        try {
            return ResponseEntity.ok(coasterStockService.updateStock(updates, performedBy));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Stock change history between two days (inclusive, yyyy-MM-dd). Defaults to the last 7 days. */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String coasterStockId) {
        LocalDate end = to != null ? to : coasterStockService.today();
        LocalDate start = from != null ? from : end.minusDays(6);
        try {
            return ResponseEntity.ok(coasterStockService.getHistory(start, end, coasterStockId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
