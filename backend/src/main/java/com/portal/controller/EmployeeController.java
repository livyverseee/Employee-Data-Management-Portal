package com.portal.controller;

import com.portal.dto.EmployeeCriteria;
import com.portal.dto.EmployeePageResponse;
import com.portal.dto.EmployeeUpdateRequest;
import com.portal.dto.FilterOptionsResponse;
import com.portal.model.Employee;
import com.portal.service.EmployeeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * REST controller exposing endpoints for Employee operations:
 * - Search and Paginated list (scoped to active dataset)
 * - Filter options for dropdown population
 * - Single employee details by database ID
 * - Edit employee (DEAN only)
 * - Delete employee (DEAN only)
 * - CSV Export with active filters
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * Get paginated employees filtered by optional search query and column filters.
     */
    @GetMapping
    public ResponseEntity<EmployeePageResponse> getEmployees(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "employeeId", required = false) String employeeId,
            @RequestParam(value = "education", required = false) String education,
            @RequestParam(value = "joiningYear", required = false) Integer joiningYear,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "paymentTier", required = false) Integer paymentTier,
            @RequestParam(value = "ageMin", required = false) Integer ageMin,
            @RequestParam(value = "ageMax", required = false) Integer ageMax,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "everBenched", required = false) String everBenched,
            @RequestParam(value = "experienceInCurrentDomain", required = false) Integer experienceInCurrentDomain,
            @RequestParam(value = "leaveOrNot", required = false) Integer leaveOrNot,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        EmployeeCriteria criteria = new EmployeeCriteria(
                search, employeeId, education, joiningYear, city,
                paymentTier, ageMin, ageMax, gender, everBenched,
                experienceInCurrentDomain, leaveOrNot
        );

        EmployeePageResponse response = employeeService.getEmployees(criteria, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves distinct values for each filterable column from the active dataset.
     */
    @GetMapping("/filter-options")
    public ResponseEntity<FilterOptionsResponse> getFilterOptions() {
        FilterOptionsResponse options = employeeService.getFilterOptions();
        return ResponseEntity.ok(options);
    }

    /**
     * Get an individual employee's complete details by database ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable("id") Long id) {
        Employee employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    /**
     * Update an employee's details (DEAN only).
     */
    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable("id") Long id,
            @RequestBody EmployeeUpdateRequest request) {

        Employee updated = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an individual employee by database ID (DEAN only).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable("id") Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Export all employees matching active filters as a CSV attachment.
     */
    @GetMapping("/export")
    public void exportCsv(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "employeeId", required = false) String employeeId,
            @RequestParam(value = "education", required = false) String education,
            @RequestParam(value = "joiningYear", required = false) Integer joiningYear,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "paymentTier", required = false) Integer paymentTier,
            @RequestParam(value = "ageMin", required = false) Integer ageMin,
            @RequestParam(value = "ageMax", required = false) Integer ageMax,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "everBenched", required = false) String everBenched,
            @RequestParam(value = "experienceInCurrentDomain", required = false) Integer experienceInCurrentDomain,
            @RequestParam(value = "leaveOrNot", required = false) Integer leaveOrNot,
            HttpServletResponse response) throws IOException {

        EmployeeCriteria criteria = new EmployeeCriteria(
                search, employeeId, education, joiningYear, city,
                paymentTier, ageMin, ageMax, gender, everBenched,
                experienceInCurrentDomain, leaveOrNot
        );

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.csv\"");

        employeeService.exportEmployeesCsv(criteria, response.getWriter());
    }
}
