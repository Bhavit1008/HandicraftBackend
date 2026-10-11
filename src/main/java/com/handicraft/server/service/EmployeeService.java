package com.handicraft.server.service;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.handicraft.server.model.Employee;
import com.handicraft.server.repository.EmployeeRepository;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Seeds a single admin account on first boot so there's always a way to log in. Change the password after first login. */
    @PostConstruct
    public void seedDefaultEmployeesIfEmpty() {
        if (employeeRepository.count() > 0) return;

        Employee admin = new Employee();
        admin.setEmployeeName("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("admin");
        admin.setAssignedLocations(List.of("Head Office"));
        admin.setCreatedAt(System.currentTimeMillis());
        employeeRepository.save(admin);
    }

    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    public Employee addEmployee(Employee employee) {
        if (employee.getEmployeeName() == null || employee.getEmployeeName().isBlank()) {
            throw new IllegalArgumentException("Employee name is required.");
        }
        if (employee.getPassword() == null || employee.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        validateRoleAndLocation(employee);
        if (employeeRepository.existsByEmployeeNameIgnoreCase(employee.getEmployeeName())) {
            throw new IllegalArgumentException(
                "Employee name \"" + employee.getEmployeeName() + "\" is already in use.");
        }

        employee.setId(null);
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setCreatedAt(System.currentTimeMillis());
        return employeeRepository.save(employee);
    }

    /**
     * Updates an existing employee. Password is only re-hashed and replaced
     * when a new one is supplied (non-blank) — otherwise the stored hash is
     * left untouched, since the edit form never round-trips the existing hash
     * back (the entity's password field is write-only in JSON responses).
     */
    public Employee updateEmployee(String id, Employee changes) {
        Employee existing = employeeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));

        if (changes.getEmployeeName() == null || changes.getEmployeeName().isBlank()) {
            throw new IllegalArgumentException("Employee name is required.");
        }
        validateRoleAndLocation(changes);

        boolean nameChanged = !existing.getEmployeeName().equalsIgnoreCase(changes.getEmployeeName());
        if (nameChanged && employeeRepository.existsByEmployeeNameIgnoreCase(changes.getEmployeeName())) {
            throw new IllegalArgumentException(
                "Employee name \"" + changes.getEmployeeName() + "\" is already in use.");
        }

        existing.setEmployeeName(changes.getEmployeeName());
        existing.setEmployeeNumber(changes.getEmployeeNumber());
        existing.setEmployeeEmail(changes.getEmployeeEmail());
        existing.setAssignedLocations(changes.getAssignedLocations());
        existing.setRole(changes.getRole());
        if (changes.getPassword() != null && !changes.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(changes.getPassword()));
        }

        return employeeRepository.save(existing);
    }

    /**
     * requestedBy is the caller's own employeeName (self-reported by the
     * frontend). It exists purely to stop an admin from deleting the account
     * they're currently logged in as, not as a security boundary.
     */
    public void deleteEmployee(String id, String requestedBy) {
        Employee target = employeeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));

        if (requestedBy != null && target.getEmployeeName().equalsIgnoreCase(requestedBy.trim())) {
            throw new IllegalArgumentException("You can't delete the account you're currently logged in as.");
        }

        employeeRepository.deleteById(id);
    }

    private void validateRoleAndLocation(Employee employee) {
        if (employee.getRole() == null || employee.getRole().isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }
        if (!"admin".equals(employee.getRole()) && !"store_manager".equals(employee.getRole())) {
            throw new IllegalArgumentException("Role must be either \"admin\" or \"store_manager\".");
        }
        if (employee.getAssignedLocations() == null || employee.getAssignedLocations().isEmpty()) {
            throw new IllegalArgumentException("At least one location is required.");
        }
    }

    /** Returns the matching employee (password already stripped by the entity's write-only mapping) or empty if invalid. */
    public Optional<Employee> login(String employeeName, String rawPassword) {
        if (employeeName == null || rawPassword == null) return Optional.empty();

        return employeeRepository.findByEmployeeNameIgnoreCase(employeeName.trim())
            .filter(e -> passwordEncoder.matches(rawPassword, e.getPassword()));
    }
}
