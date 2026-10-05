package com.portal.service;

import com.portal.dto.DatasetActiveResponse;
import com.portal.exception.ConflictException;
import com.portal.model.Dataset;
import com.portal.model.Employee;
import com.portal.repository.DatasetRepository;
import com.portal.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing dataset uploads, replacements, and active dataset ownership.
 */
@Service
public class DatasetService {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private XmlParserService xmlParserService;

    /**
     * Retrieves metadata for the currently active dataset.
     */
    @Transactional(readOnly = true)
    public DatasetActiveResponse getActiveDatasetInfo() {
        return datasetRepository.findByActiveTrue()
                .map(d -> new DatasetActiveResponse(true, d.getFileName(), d.getUploadedBy(), d.getUploadedAt(), d.getRecordCount()))
                .orElseGet(DatasetActiveResponse::notFound);
    }

    /**
     * Ingests the initial dataset (DEAN only).
     * Allowed ONLY when no active dataset exists; otherwise 409 Conflict.
     */
    @Transactional
    public DatasetActiveResponse uploadDataset(MultipartFile file, String uploadedBy) {
        // Enforce rule: at most one active dataset
        if (datasetRepository.findByActiveTrue().isPresent()) {
            throw new ConflictException("A dataset has already been uploaded");
        }

        // Parse and validate XML before touching database
        List<Employee> employees = xmlParserService.parseAndValidateXml(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "employees.xml";

        // Save Dataset entity
        Dataset dataset = new Dataset();
        dataset.setFileName(originalFilename);
        dataset.setUploadedBy(uploadedBy);
        dataset.setUploadedAt(LocalDateTime.now());
        dataset.setRecordCount(employees.size());
        dataset.setActive(true);
        Dataset savedDataset = datasetRepository.save(dataset);

        // Assign datasetId to each employee and persist in one transaction
        for (Employee emp : employees) {
            emp.setDatasetId(savedDataset.getId());
        }
        employeeRepository.saveAll(employees);

        return new DatasetActiveResponse(true, savedDataset.getFileName(), savedDataset.getUploadedBy(), savedDataset.getUploadedAt(), savedDataset.getRecordCount());
    }

    /**
     * Replaces the currently active dataset with a new XML file (DEAN only).
     * Validates and parses the new XML first, so a bad file never destroys existing data.
     */
    @Transactional
    public DatasetActiveResponse replaceDataset(MultipartFile file, String uploadedBy) {
        // 1. Parse and validate new XML first
        List<Employee> newEmployees = xmlParserService.parseAndValidateXml(file);

        // 2. Locate existing active dataset (if any) and clean up
        Optional<Dataset> existingActiveOpt = datasetRepository.findByActiveTrue();
        if (existingActiveOpt.isPresent()) {
            Dataset oldDataset = existingActiveOpt.get();
            // Delete old employees
            employeeRepository.deleteByDatasetId(oldDataset.getId());
            // Delete or deactivate old dataset
            oldDataset.setActive(false);
            datasetRepository.save(oldDataset);
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "employees.xml";

        // 3. Save new Dataset entity
        Dataset newDataset = new Dataset();
        newDataset.setFileName(originalFilename);
        newDataset.setUploadedBy(uploadedBy);
        newDataset.setUploadedAt(LocalDateTime.now());
        newDataset.setRecordCount(newEmployees.size());
        newDataset.setActive(true);
        Dataset savedDataset = datasetRepository.save(newDataset);

        // 4. Associate and save new employees
        for (Employee emp : newEmployees) {
            emp.setDatasetId(savedDataset.getId());
        }
        employeeRepository.saveAll(newEmployees);

        return new DatasetActiveResponse(true, savedDataset.getFileName(), savedDataset.getUploadedBy(), savedDataset.getUploadedAt(), savedDataset.getRecordCount());
    }

    /**
     * Helper to retrieve active dataset entity or empty.
     */
    public Optional<Dataset> getActiveDataset() {
        return datasetRepository.findByActiveTrue();
    }
}
