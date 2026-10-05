package com.portal.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.portal.dto.EmployeeListWrapper;
import com.portal.exception.BadRequestException;
import com.portal.model.Employee;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service dedicated to parsing and validating XML files of employee records.
 * Parsing flow: Receive XML -> Parse with XmlMapper -> Convert to List<Employee> -> Validate uniqueness.
 */
@Service
public class XmlParserService {

    private final XmlMapper xmlMapper;

    public XmlParserService() {
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Parses an uploaded XML multipart file into validated Employee records.
     * Validates:
     * - File presence and non-emptiness
     * - .xml extension
     * - Well-formed XML syntax
     * - Every record has a non-blank employeeId
     * - No duplicate employeeId values inside the same XML file
     *
     * @param file uploaded multipart XML file
     * @return List of parsed Employee objects
     */
    public List<Employee> parseAndValidateXml(MultipartFile file) {
        // 1. Validate file presence
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        // 2. Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xml")) {
            throw new BadRequestException("Invalid file type. Only .xml files are supported");
        }

        EmployeeListWrapper wrapper;
        try (InputStream inputStream = file.getInputStream()) {
            // 3. Parse XML using Jackson XmlMapper
            wrapper = xmlMapper.readValue(inputStream, EmployeeListWrapper.class);
        } catch (Exception e) {
            throw new BadRequestException("Malformed or invalid XML file: " + e.getMessage());
        }

        if (wrapper == null || wrapper.getEmployees() == null || wrapper.getEmployees().isEmpty()) {
            throw new BadRequestException("No valid employee records found in the uploaded XML");
        }

        List<Employee> employees = wrapper.getEmployees();
        Set<String> seenIds = new HashSet<>();
        List<String> duplicateIds = new ArrayList<>();

        // 4. Validate every record has a non-blank ID and check for in-file duplicates
        for (int i = 0; i < employees.size(); i++) {
            Employee emp = employees.get(i);
            if (emp.getEmployeeId() == null || emp.getEmployeeId().trim().isEmpty()) {
                throw new BadRequestException("Invalid record at position " + (i + 1) + ": employeeId must not be blank");
            }

            String trimmedId = emp.getEmployeeId().trim();
            emp.setEmployeeId(trimmedId);

            if (!seenIds.add(trimmedId)) {
                if (!duplicateIds.contains(trimmedId)) {
                    duplicateIds.add(trimmedId);
                }
            }
        }

        if (!duplicateIds.isEmpty()) {
            int displayCount = Math.min(5, duplicateIds.size());
            String sample = String.join(", ", duplicateIds.subList(0, displayCount));
            if (duplicateIds.size() > displayCount) {
                sample += " (and " + (duplicateIds.size() - displayCount) + " more)";
            }
            throw new BadRequestException("Duplicate employee IDs found in XML: " + sample);
        }

        return employees;
    }
}
