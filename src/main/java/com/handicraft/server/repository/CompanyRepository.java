package com.handicraft.server.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.handicraft.server.model.CompanyDetails;

@Repository
public interface CompanyRepository extends MongoRepository<CompanyDetails, String> {
}
