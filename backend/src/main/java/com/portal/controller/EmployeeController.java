package com.portal.controller;

import com.portal.dto.EmployeePageResponse;
import com.portal.dto.UploadResponse;
import com.portal.model.Employee;
import com.portal.service.EmployeeService;
import com.portal.service.XmlParserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Thin REST controller exposing endpoints for Employee operations:
 * - XML upload (DEAN only)
 * - Search and Paginated list (DEAN and EMPLOYEE)
 * - Get by ID (DEAN and EMPLOYEE)
 * - Delete by ID (DEAN only)
 * - CSV Export (DEAN and EMPLOYEE)
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private XmlParserService xmlParserService;

    /**
     * Upload an XML file containing employee records (DEAN only).
     *
     * @param file multipart XML file
     * @return 200 OK with message and recordsSaved count
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadXml(@RequestParam("file") MultipartFile file) {
        int count = xmlParserService.parseAndSave(file);
        return ResponseEntity.ok(new UploadResponse("Upload successful", count));
    }

    /**
     * Get paginated employees filtered by optional search query, city, and gender.
     *
     * @param search keyword for id, city, or education
     * @param city   exact city name (optional)
     * @param gender exact gender (optional)
     * @param page   page number (default 0)
     * @param size   page size (default 10)
     * @return EmployeePageResponse
     */
    @GetMapping
    public ResponseEntity<EmployeePageResponse> getEmployees(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        EmployeePageResponse response = employeeService.getEmployees(search, city, gender, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get an individual employee's complete details by ID.
     *
     * @param id employee identifier (e.g. EMP0001)
     * @return Employee or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable("id") String id) {
        Employee employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    /**
     * Delete an individual employee by ID (DEAN only).
     *
     * @param id employee identifier
     * @return 204 No Content or 404
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable("id") String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Export all employees matching current search/city/gender filters as CSV attachment.
     *
     * @param search   keyword
     * @param city     city filter
     * @param gender   gender filter
     * @param response HttpServletResponse for streaming CSV
     */
    @GetMapping("/export")
    public void exportCsv(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "gender", required = false) String gender,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.csv\"");

        employeeService.exportEmployeesCsv(search, city, gender, response.getWriter());
    }
}
