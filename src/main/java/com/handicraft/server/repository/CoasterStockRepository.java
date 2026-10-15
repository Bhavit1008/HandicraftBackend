package com.handicraft.server.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.handicraft.server.model.CoasterStock;

@Repository
public interface CoasterStockRepository extends MongoRepository<CoasterStock, String> {

}
