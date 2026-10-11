package com.handicraft.server.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.handicraft.server.model.Employee;

@Repository
public interface EmployeeRepository extends MongoRepository<Employee, String> {

    Optional<Employee> findByEmployeeNameIgnoreCase(String employeeName);

    boolean existsByEmployeeNameIgnoreCase(String employeeName);

}
