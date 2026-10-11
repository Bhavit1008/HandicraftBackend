package com.handicraft.server.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.handicraft.server.model.PosterStockHistory;

@Repository
public interface PosterStockHistoryRepository extends MongoRepository<PosterStockHistory, String> {

    /** Changes with start <= updatedAt < end. */
    @Query("{ 'updatedAt': { $gte: ?0, $lt: ?1 } }")
    List<PosterStockHistory> findUpdatedBetween(long start, long end, Sort sort);

    @Query("{ 'posterStockId': ?0, 'updatedAt': { $gte: ?1, $lt: ?2 } }")
    List<PosterStockHistory> findUpdatedBetweenForPosterStock(String posterStockId, long start, long end, Sort sort);
}
