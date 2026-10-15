package com.handicraft.server.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.handicraft.server.model.CoasterStockHistory;

@Repository
public interface CoasterStockHistoryRepository extends MongoRepository<CoasterStockHistory, String> {

    /** Changes with start <= updatedAt < end. */
    @Query("{ 'updatedAt': { $gte: ?0, $lt: ?1 } }")
    List<CoasterStockHistory> findUpdatedBetween(long start, long end, Sort sort);

    @Query("{ 'coasterStockId': ?0, 'updatedAt': { $gte: ?1, $lt: ?2 } }")
    List<CoasterStockHistory> findUpdatedBetweenForCoasterStock(String coasterStockId, long start, long end, Sort sort);
}
