package com.handicraft.server.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.handicraft.server.model.PosterStock;

@Repository
public interface PosterStockRepository extends MongoRepository<PosterStock, String> {

}
