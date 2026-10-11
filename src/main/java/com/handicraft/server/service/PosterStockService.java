package com.handicraft.server.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.handicraft.server.model.Employee;
import com.handicraft.server.model.PosterStock;
import com.handicraft.server.model.PosterStockHistory;
import com.handicraft.server.repository.EmployeeRepository;
import com.handicraft.server.repository.PosterStockHistoryRepository;
import com.handicraft.server.repository.PosterStockRepository;

@Service
public class PosterStockService {

    private static final List<String> SHAPES = List.of("Round", "Square", "Hexagon", "Octagon");

    /** Finish/quality pairs every shape is stocked in — mirrors the Poster Stock sheet. */
    private static final List<String[]> VARIANTS = List.of(
        new String[] { "Raw", "Super White" },
        new String[] { "Polished", "Average" },
        new String[] { "Butti", "Purple" },
        new String[] { null, "Commercial" }
    );

    /** One row of a bulk stock update. */
    public record StockUpdate(String id, Integer stock) {}

    @Autowired
    private PosterStockRepository posterStockRepository;

    @Autowired
    private PosterStockHistoryRepository historyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /** Timezone that decides which calendar day a change belongs to. */
    @Value("${app.timezone:Asia/Kolkata}")
    private String timezone;

    /** Creates the 16 standard poster variants (at zero stock) the first time the app runs against an empty database. */
    @EventListener(ApplicationReadyEvent.class)
    public void seedDefaultVariants() {
        if (posterStockRepository.count() > 0) return;

        long now = System.currentTimeMillis();
        List<PosterStock> rows = new ArrayList<>();
        for (String shape : SHAPES) {
            for (String[] variant : VARIANTS) {
                PosterStock row = new PosterStock();
                row.setShape(shape);
                row.setFinish(variant[0]);
                row.setQuality(variant[1]);
                row.setStock(0);
                row.setSortOrder(rows.size() + 1);
                row.setCreatedAt(now);
                rows.add(row);
            }
        }
        posterStockRepository.saveAll(rows);
    }

    public List<PosterStock> getAll() {
        return posterStockRepository.findAll(Sort.by("sortOrder"));
    }

    /**
     * Applies new absolute stock counts and writes one history entry per row
     * whose count actually changed. The "previous" value is read from the
     * database at save time, so the history stays accurate even if another
     * user saved in between.
     */
    public List<PosterStock> updateStock(List<StockUpdate> updates, String performedBy) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No stock changes were sent.");
        }

        Map<String, Integer> newStockById = new LinkedHashMap<>();
        for (StockUpdate update : updates) {
            if (update == null || update.id() == null || update.id().isBlank()) {
                throw new IllegalArgumentException("Every stock change needs a poster stock id.");
            }
            if (update.stock() == null || update.stock() < 0) {
                throw new IllegalArgumentException("Stock must be a whole number of 0 or more.");
            }
            newStockById.put(update.id(), update.stock());
        }

        Map<String, PosterStock> rowsById = posterStockRepository.findAllById(newStockById.keySet()).stream()
            .collect(Collectors.toMap(PosterStock::getId, Function.identity()));
        for (String id : newStockById.keySet()) {
            if (!rowsById.containsKey(id)) {
                throw new IllegalArgumentException("Poster stock row not found: " + id);
            }
        }

        String updatedBy = performedBy != null && !performedBy.isBlank() ? performedBy.trim() : "Unknown";
        Employee performer = resolveEmployee(updatedBy);
        long now = System.currentTimeMillis();
        String day = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate().toString();

        List<PosterStock> changedRows = new ArrayList<>();
        List<PosterStockHistory> history = new ArrayList<>();
        newStockById.forEach((id, newStock) -> {
            PosterStock row = rowsById.get(id);
            if (row.getStock() == newStock) return;

            PosterStockHistory entry = new PosterStockHistory();
            entry.setPosterStockId(id);
            entry.setShape(row.getShape());
            entry.setFinish(row.getFinish());
            entry.setQuality(row.getQuality());
            entry.setPreviousStock(row.getStock());
            entry.setNewStock(newStock);
            entry.setChange(newStock - row.getStock());
            entry.setUpdatedBy(updatedBy);
            entry.setUpdatedByRole(performer != null ? performer.getRole() : null);
            entry.setUpdatedAt(now);
            entry.setDate(day);
            history.add(entry);

            row.setStock(newStock);
            row.setUpdatedBy(updatedBy);
            row.setUpdatedAt(now);
            changedRows.add(row);
        });

        posterStockRepository.saveAll(changedRows);
        historyRepository.saveAll(history);
        return getAll();
    }

    /** Changes made on any day from {@code from} to {@code to} (inclusive), newest first. */
    public List<PosterStockHistory> getHistory(LocalDate from, LocalDate to, String posterStockId) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must be on or before the 'to' date.");
        }

        long start = from.atStartOfDay(zone()).toInstant().toEpochMilli();
        long end = to.plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli();
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "updatedAt");

        return posterStockId == null || posterStockId.isBlank()
            ? historyRepository.findUpdatedBetween(start, end, newestFirst)
            : historyRepository.findUpdatedBetweenForPosterStock(posterStockId, start, end, newestFirst);
    }

    public LocalDate today() {
        return LocalDate.now(zone());
    }

    private ZoneId zone() {
        return ZoneId.of(timezone);
    }

    private Employee resolveEmployee(String employeeName) {
        return employeeRepository.findByEmployeeNameIgnoreCase(employeeName).orElse(null);
    }
}
