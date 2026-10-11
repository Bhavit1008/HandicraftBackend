package com.handicraft.server.controller;

import com.handicraft.server.model.CompanyDetails;
import com.handicraft.server.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class CompanyDetailsController {

    @Autowired
    private CompanyRepository companyRepository;

    @PostMapping("/addCompany")
    public ResponseEntity<CompanyDetails> addCompany(@RequestBody CompanyDetails details) {
        return new ResponseEntity<>(companyRepository.save(details), HttpStatus.CREATED);
    }

    @GetMapping("/getCompany")
    public List<CompanyDetails> getCompany() {
        return companyRepository.findAll();
    }

    @PutMapping("/updateCompany")
    public ResponseEntity<?> updateCompany(@RequestBody CompanyDetails details) {
        if (details.getId() == null || !companyRepository.existsById(details.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Company details not found.");
        }
        return ResponseEntity.ok(companyRepository.save(details));
    }

    @DeleteMapping("/deleteCompany/{id}")
    public ResponseEntity<?> deleteCompany(@PathVariable String id) {
        if (!companyRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Company details not found.");
        }
        companyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
