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
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.mongodb.MongoNamespace;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import jakarta.annotation.PostConstruct;

import com.handicraft.server.model.Employee;
import com.handicraft.server.model.CoasterStock;
import com.handicraft.server.model.CoasterStockHistory;
import com.handicraft.server.repository.EmployeeRepository;
import com.handicraft.server.repository.CoasterStockHistoryRepository;
import com.handicraft.server.repository.CoasterStockRepository;

@Service
public class CoasterStockService {

    private static final List<String> SHAPES = List.of("Round", "Square", "Hexagon", "Octagon");

    /** Finish/quality pairs every shape is stocked in — mirrors the Coaster Stock sheet. */
    private static final List<String[]> VARIANTS = List.of(
        new String[] { "Raw", "Super White" },
        new String[] { "Polished", "Average" },
        new String[] { "Butti", "Purple" },
        new String[] { null, "Commercial" }
    );

    /** One row of a bulk stock update. */
    public record StockUpdate(String id, Integer stock) {}

    @Autowired
    private CoasterStockRepository coasterStockRepository;

    @Autowired
    private CoasterStockHistoryRepository historyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /** Timezone that decides which calendar day a change belongs to. */
    @Value("${app.timezone:Asia/Kolkata}")
    private String timezone;

    /**
     * Creates the 16 standard coaster variants (at zero stock) the first time the app runs against an empty database.
     * Runs at startup, before requests are served, and after moving any data saved under the old "poster" names.
     */
    @PostConstruct
    public void seedDefaultVariants() {
        migrateLegacyPosterData();
        if (coasterStockRepository.count() > 0) return;

        long now = System.currentTimeMillis();
        List<CoasterStock> rows = new ArrayList<>();
        for (String shape : SHAPES) {
            for (String[] variant : VARIANTS) {
                CoasterStock row = new CoasterStock();
                row.setShape(shape);
                row.setFinish(variant[0]);
                row.setQuality(variant[1]);
                row.setStock(0);
                row.setSortOrder(rows.size() + 1);
                row.setCreatedAt(now);
                rows.add(row);
            }
        }
        coasterStockRepository.saveAll(rows);
    }

    /**
     * One-time move of data saved while this feature was called "poster": renames the old
     * collections and the history's posterStockId field. Does nothing once done, so it is safe
     * on every start; it can be deleted once every environment has run this version.
     */
    private void migrateLegacyPosterData() {
        renameCollectionIfPresent("PosterStock", "CoasterStock");
        renameCollectionIfPresent("PosterStockHistory", "CoasterStockHistory");

        long renamed = mongoTemplate.getCollection("CoasterStockHistory")
            .updateMany(Filters.exists("posterStockId"), Updates.rename("posterStockId", "coasterStockId"))
            .getModifiedCount();
        if (renamed > 0) {
            System.out.println("[Migration] Renamed posterStockId -> coasterStockId on " + renamed + " history entries.");
        }
    }

    private void renameCollectionIfPresent(String from, String to) {
        if (!mongoTemplate.collectionExists(from)) return;
        if (mongoTemplate.collectionExists(to)) {
            System.err.println("[Migration] Both " + from + " and " + to + " exist; leaving " + from + " untouched.");
            return;
        }
        mongoTemplate.getCollection(from).renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), to));
        System.out.println("[Migration] Renamed collection " + from + " -> " + to + ".");
    }

    public List<CoasterStock> getAll() {
        return coasterStockRepository.findAll(Sort.by("sortOrder"));
    }

    /**
     * Applies new absolute stock counts and writes one history entry per row
     * whose count actually changed. The "previous" value is read from the
     * database at save time, so the history stays accurate even if another
     * user saved in between.
     */
    public List<CoasterStock> updateStock(List<StockUpdate> updates, String performedBy) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No stock changes were sent.");
        }

        Map<String, Integer> newStockById = new LinkedHashMap<>();
        for (StockUpdate update : updates) {
            if (update == null || update.id() == null || update.id().isBlank()) {
                throw new IllegalArgumentException("Every stock change needs a coaster stock id.");
            }
            if (update.stock() == null || update.stock() < 0) {
                throw new IllegalArgumentException("Stock must be a whole number of 0 or more.");
            }
            newStockById.put(update.id(), update.stock());
        }

        Map<String, CoasterStock> rowsById = coasterStockRepository.findAllById(newStockById.keySet()).stream()
            .collect(Collectors.toMap(CoasterStock::getId, Function.identity()));
        for (String id : newStockById.keySet()) {
            if (!rowsById.containsKey(id)) {
                throw new IllegalArgumentException("Coaster stock row not found: " + id);
            }
        }

        String updatedBy = performedBy != null && !performedBy.isBlank() ? performedBy.trim() : "Unknown";
        Employee performer = resolveEmployee(updatedBy);
        long now = System.currentTimeMillis();
        String day = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate().toString();

        List<CoasterStock> changedRows = new ArrayList<>();
        List<CoasterStockHistory> history = new ArrayList<>();
        newStockById.forEach((id, newStock) -> {
            CoasterStock row = rowsById.get(id);
            if (row.getStock() == newStock) return;

            CoasterStockHistory entry = new CoasterStockHistory();
            entry.setCoasterStockId(id);
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

        coasterStockRepository.saveAll(changedRows);
        historyRepository.saveAll(history);
        return getAll();
    }

    /** Changes made on any day from {@code from} to {@code to} (inclusive), newest first. */
    public List<CoasterStockHistory> getHistory(LocalDate from, LocalDate to, String coasterStockId) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must be on or before the 'to' date.");
        }

        long start = from.atStartOfDay(zone()).toInstant().toEpochMilli();
        long end = to.plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli();
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "updatedAt");

        return coasterStockId == null || coasterStockId.isBlank()
            ? historyRepository.findUpdatedBetween(start, end, newestFirst)
            : historyRepository.findUpdatedBetweenForCoasterStock(coasterStockId, start, end, newestFirst);
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
