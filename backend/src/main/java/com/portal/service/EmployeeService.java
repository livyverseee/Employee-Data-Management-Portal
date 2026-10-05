package com.portal.service;

import com.portal.dto.EmployeePageResponse;
import com.portal.exception.ResourceNotFoundException;
import com.portal.model.Employee;
import com.portal.repository.EmployeeRepository;
import com.portal.repository.EmployeeSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

/**
 * Service managing employee operations: search, pagination, retrieval, deletion, and CSV export.
 */
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Retrieves a paginated and filtered list of employees sorted by employeeId ascending.
     *
     * @param search search keyword for id, city, or education
     * @param city   filter by specific city
     * @param gender filter by specific gender
     * @param page   zero-based page index
     * @param size   number of records per page
     * @return EmployeePageResponse containing content and pagination metadata
     */
    @Transactional(readOnly = true)
    public EmployeePageResponse getEmployees(String search, String city, String gender, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "employeeId"));
        Specification<Employee> spec = EmployeeSpecifications.filterEmployees(search, city, gender);

        Page<Employee> employeePage = employeeRepository.findAll(spec, pageable);

        return new EmployeePageResponse(
                employeePage.getContent(),
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements(),
                employeePage.getTotalPages()
        );
    }

    /**
     * Retrieves an individual employee by their ID.
     *
     * @param id employee identifier (e.g. EMP0001)
     * @return Employee object
     * @throws ResourceNotFoundException if no employee exists with given id
     */
    @Transactional(readOnly = true)
    public Employee getEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    /**
     * Deletes an employee by their ID.
     *
     * @param id employee identifier
     * @throws ResourceNotFoundException if no employee exists with given id
     */
    @Transactional
    public void deleteEmployee(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }

    /**
     * Streams all filtered employee records (unpaginated) directly as CSV.
     * Uses the exact same filter specification as the paginated listing.
     *
     * @param search search keyword
     * @param city   city filter
     * @param gender gender filter
     * @param writer output writer for streaming CSV
     */
    @Transactional(readOnly = true)
    public void exportEmployeesCsv(String search, String city, String gender, Writer writer) throws IOException {
        Specification<Employee> spec = EmployeeSpecifications.filterEmployees(search, city, gender);
        List<Employee> employees = employeeRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "employeeId"));

        PrintWriter printWriter = new PrintWriter(writer);

        // Required CSV Header row
        printWriter.println("Employee ID,Education,Joining Year,City,Payment Tier,Age,Gender,Ever Benched,Experience In Current Domain,Left Company");

        for (Employee emp : employees) {
            String leftCompany = emp.getLeaveOrNot() == 1 ? "Yes" : "No";

            StringBuilder row = new StringBuilder();
            row.append(escapeCsv(emp.getEmployeeId())).append(",");
            row.append(escapeCsv(emp.getEducation())).append(",");
            row.append(emp.getJoiningYear()).append(",");
            row.append(escapeCsv(emp.getCity())).append(",");
            row.append(emp.getPaymentTier()).append(",");
            row.append(emp.getAge()).append(",");
            row.append(escapeCsv(emp.getGender())).append(",");
            row.append(escapeCsv(emp.getEverBenched())).append(",");
            row.append(emp.getExperienceInCurrentDomain()).append(",");
            row.append(escapeCsv(leftCompany));

            printWriter.println(row.toString());
        }

        printWriter.flush();
    }

    /**
     * Escapes standard CSV fields: quotes embedded commas and doubles internal quotes.
     */
    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
