package com.handicraft.server.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

/** One entry per poster stock change — who changed which variant, from what to what, and when. */
@Document("PosterStockHistory")
public class PosterStockHistory {

    @Id
    private String id;

    @Indexed
    private String posterStockId;

    // Denormalized so the history reads on its own even if the variant is later renamed.
    private String shape;
    private String finish;
    private String quality;

    private int previousStock;
    private int newStock;
    /** newStock - previousStock */
    private int change;

    private String updatedBy;
    private String updatedByRole;

    /** Epoch millis of the change. */
    @Indexed
    private Long updatedAt;
    /** Business day of the change (yyyy-MM-dd in the configured app timezone), for day-wise grouping. */
    private String date;

    public PosterStockHistory() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPosterStockId() {
        return posterStockId;
    }

    public void setPosterStockId(String posterStockId) {
        this.posterStockId = posterStockId;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getFinish() {
        return finish;
    }

    public void setFinish(String finish) {
        this.finish = finish;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public int getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(int previousStock) {
        this.previousStock = previousStock;
    }

    public int getNewStock() {
        return newStock;
    }

    public void setNewStock(int newStock) {
        this.newStock = newStock;
    }

    public int getChange() {
        return change;
    }

    public void setChange(int change) {
        this.change = change;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedByRole() {
        return updatedByRole;
    }

    public void setUpdatedByRole(String updatedByRole) {
        this.updatedByRole = updatedByRole;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
