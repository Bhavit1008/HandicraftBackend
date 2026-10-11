package com.handicraft.server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.handicraft.server.model.Item;
import com.handicraft.server.repository.ItemRepository;
import com.handicraft.server.service.CloudinaryService;
import com.handicraft.server.service.ItemService;

@RestController
@CrossOrigin(origins = "*")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private ItemService itemService;

    @PostMapping("/addItem")
    public ResponseEntity<?> addItem(
            @RequestBody Item item,
            @RequestHeader(value = "X-Employee-Name", required = false) String performedBy) {
        try {
            Item saved = itemService.saveItem(item, performedBy);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/getAllItems")
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    /** Single item by id — lets a view page re-fetch its data on a hard refresh. */
    @GetMapping("/items/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable String id) {
        return itemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Paginated item listing, newest-first, optionally scoped to a category. */
    @GetMapping("/items")
    public ResponseEntity<Page<Item>> getItems(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Item> result = (category == null || category.isBlank())
                ? itemRepository.findAll(pageable)
                : itemRepository.findByCategory(category, pageable);

        return ResponseEntity.ok(result);
    }

    /** Delete an item by ID. Removes it from MongoDB and deletes its image from Cloudinary. */
    @DeleteMapping("/deleteItem/{id}")
    public ResponseEntity<?> deleteItem(
            @PathVariable String id,
            @RequestHeader(value = "X-Employee-Name", required = false) String performedBy) {
        try {
            itemService.deleteItem(id, performedBy);
            return ResponseEntity.ok("Item deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile file) {
        String imageUrl = cloudinaryService.uploadImage(file);
        return ResponseEntity.ok(imageUrl);
    }
}
