package com.handicraft.server.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.handicraft.server.model.Item;

@Repository
public interface ItemRepository extends MongoRepository<Item, String> {

    Page<Item> findByCategory(String category, Pageable pageable);

    List<Item> findByItemCode(String itemCode);

}
