package com.handicraft.server.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

@Document("Employee")
public class Employee {

    @Id
    private String id;

    @Indexed(unique = true)
    private String employeeName;

    private String employeeNumber;
    private String employeeEmail;

    /** Warehouses/showrooms this employee is assigned to — a store manager can be assigned more than one. */
    private List<String> assignedLocations;

    /** Hashed with BCrypt before being persisted — never returned in a response. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** "admin" or "store_manager" */
    private String role;

    private Long createdAt;

    public Employee() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public List<String> getAssignedLocations() {
        return assignedLocations;
    }

    public void setAssignedLocations(List<String> assignedLocations) {
        this.assignedLocations = assignedLocations;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
