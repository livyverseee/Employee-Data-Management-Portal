package com.portal.service;

import com.portal.dto.EmployeeCriteria;
import com.portal.dto.EmployeePageResponse;
import com.portal.dto.EmployeeUpdateRequest;
import com.portal.dto.FilterOptionsResponse;
import com.portal.exception.BadRequestException;
import com.portal.exception.ConflictException;
import com.portal.exception.ResourceNotFoundException;
import com.portal.model.Dataset;
import com.portal.model.Employee;
import com.portal.repository.DatasetRepository;
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
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing employee operations: search, column filtering, details lookup, editing, deletion, and CSV export.
 * All operations are strictly scoped to the single active dataset.
 */
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    /**
     * Retrieves paginated employees matching criteria, scoped to the active dataset.
     */
    @Transactional(readOnly = true)
    public EmployeePageResponse getEmployees(EmployeeCriteria criteria, int page, int size) {
        Optional<Dataset> activeDatasetOpt = datasetRepository.findByActiveTrue();
        if (activeDatasetOpt.isEmpty()) {
            return new EmployeePageResponse(new ArrayList<>(), page, size, 0, 0);
        }

        Long datasetId = activeDatasetOpt.get().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "employeeId"));
        Specification<Employee> spec = EmployeeSpecifications.filterEmployees(datasetId, criteria);

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
     * Retrieves distinct values for each filterable column within the active dataset.
     */
    @Transactional(readOnly = true)
    public FilterOptionsResponse getFilterOptions() {
        Optional<Dataset> activeDatasetOpt = datasetRepository.findByActiveTrue();
        if (activeDatasetOpt.isEmpty()) {
            return new FilterOptionsResponse();
        }

        Long datasetId = activeDatasetOpt.get().getId();
        FilterOptionsResponse res = new FilterOptionsResponse();
        res.setEducation(employeeRepository.findDistinctEducationByDatasetId(datasetId));
        res.setJoiningYear(employeeRepository.findDistinctJoiningYearByDatasetId(datasetId));
        res.setCity(employeeRepository.findDistinctCityByDatasetId(datasetId));
        res.setPaymentTier(employeeRepository.findDistinctPaymentTierByDatasetId(datasetId));
        res.setGender(employeeRepository.findDistinctGenderByDatasetId(datasetId));
        res.setEverBenched(employeeRepository.findDistinctEverBenchedByDatasetId(datasetId));
        res.setExperienceInCurrentDomain(employeeRepository.findDistinctExperienceByDatasetId(datasetId));
        res.setLeaveOrNot(employeeRepository.findDistinctLeaveOrNotByDatasetId(datasetId));

        Integer minAge = employeeRepository.findMinAgeByDatasetId(datasetId);
        Integer maxAge = employeeRepository.findMaxAgeByDatasetId(datasetId);
        res.setMinAge(minAge != null ? minAge : 18);
        res.setMaxAge(maxAge != null ? maxAge : 70);

        return res;
    }

    /**
     * Retrieves an individual employee by database id, ensuring it belongs to the active dataset.
     */
    @Transactional(readOnly = true)
    public Employee getEmployeeById(Long id) {
        Dataset activeDataset = getActiveDatasetOrThrow();
        return employeeRepository.findByIdAndDatasetId(id, activeDataset.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    /**
     * Updates an existing employee record (DEAN only).
     * Validates all business constraints and ensures no duplicate employeeId within the same dataset.
     */
    @Transactional
    public Employee updateEmployee(Long id, EmployeeUpdateRequest req) {
        Dataset activeDataset = getActiveDatasetOrThrow();
        Employee employee = employeeRepository.findByIdAndDatasetId(id, activeDataset.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        validateEmployeeUpdate(req);

        String trimmedEmpId = req.getEmployeeId().trim();

        // Check if employeeId is used by another record in the same dataset
        if (employeeRepository.existsByDatasetIdAndEmployeeIdAndIdNot(activeDataset.getId(), trimmedEmpId, id)) {
            throw new ConflictException("Employee ID already exists");
        }

        employee.setEmployeeId(trimmedEmpId);
        employee.setEducation(req.getEducation().trim());
        employee.setJoiningYear(req.getJoiningYear());
        employee.setCity(req.getCity().trim());
        employee.setPaymentTier(req.getPaymentTier());
        employee.setAge(req.getAge());
        employee.setGender(req.getGender().trim());
        employee.setEverBenched(req.getEverBenched().trim());
        employee.setExperienceInCurrentDomain(req.getExperienceInCurrentDomain());
        employee.setLeaveOrNot(req.getLeaveOrNot());

        return employeeRepository.save(employee);
    }

    /**
     * Deletes an employee record by database id (DEAN only).
     */
    @Transactional
    public void deleteEmployee(Long id) {
        Dataset activeDataset = getActiveDatasetOrThrow();
        Employee employee = employeeRepository.findByIdAndDatasetId(id, activeDataset.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        employeeRepository.delete(employee);

        // Update dataset record count
        if (activeDataset.getRecordCount() > 0) {
            activeDataset.setRecordCount(activeDataset.getRecordCount() - 1);
            datasetRepository.save(activeDataset);
        }
    }

    /**
     * Streams all employees matching current filter criteria as a CSV attachment.
     */
    @Transactional(readOnly = true)
    public void exportEmployeesCsv(EmployeeCriteria criteria, Writer writer) throws IOException {
        PrintWriter printWriter = new PrintWriter(writer);
        // Header row
        printWriter.println("Employee ID,Education,Joining Year,City,Payment Tier,Age,Gender,Ever Benched,Experience In Current Domain,Left Company");

        Optional<Dataset> activeDatasetOpt = datasetRepository.findByActiveTrue();
        if (activeDatasetOpt.isEmpty()) {
            printWriter.flush();
            return;
        }

        Long datasetId = activeDatasetOpt.get().getId();
        Specification<Employee> spec = EmployeeSpecifications.filterEmployees(datasetId, criteria);
        List<Employee> employees = employeeRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "employeeId"));

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

    private Dataset getActiveDatasetOrThrow() {
        return datasetRepository.findByActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active dataset found"));
    }

    private void validateEmployeeUpdate(EmployeeUpdateRequest req) {
        if (req == null) {
            throw new BadRequestException("Update payload cannot be empty");
        }
        if (req.getEmployeeId() == null || req.getEmployeeId().trim().isEmpty()) {
            throw new BadRequestException("Employee ID cannot be blank");
        }
        if (req.getEducation() == null || req.getEducation().trim().isEmpty()) {
            throw new BadRequestException("Education cannot be blank");
        }
        int currentYear = Year.now().getValue();
        if (req.getJoiningYear() == null || req.getJoiningYear() < 1990 || req.getJoiningYear() > currentYear) {
            throw new BadRequestException("Joining Year must be between 1990 and " + currentYear);
        }
        if (req.getCity() == null || req.getCity().trim().isEmpty()) {
            throw new BadRequestException("City cannot be blank");
        }
        if (req.getPaymentTier() == null || req.getPaymentTier() < 1 || req.getPaymentTier() > 3) {
            throw new BadRequestException("Payment Tier must be 1, 2, or 3");
        }
        if (req.getAge() == null || req.getAge() < 18 || req.getAge() > 70) {
            throw new BadRequestException("Age must be between 18 and 70");
        }
        if (req.getGender() == null || (!req.getGender().equalsIgnoreCase("Male") && !req.getGender().equalsIgnoreCase("Female"))) {
            throw new BadRequestException("Gender must be Male or Female");
        }
        if (req.getEverBenched() == null || (!req.getEverBenched().equalsIgnoreCase("Yes") && !req.getEverBenched().equalsIgnoreCase("No"))) {
            throw new BadRequestException("Ever Benched must be Yes or No");
        }
        if (req.getExperienceInCurrentDomain() == null || req.getExperienceInCurrentDomain() < 0 || req.getExperienceInCurrentDomain() > 50) {
            throw new BadRequestException("Experience must be between 0 and 50 years");
        }
        if (req.getLeaveOrNot() == null || (req.getLeaveOrNot() != 0 && req.getLeaveOrNot() != 1)) {
            throw new BadRequestException("Left Company must be 0 or 1");
        }
    }

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
