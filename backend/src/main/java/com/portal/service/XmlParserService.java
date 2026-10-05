package com.portal.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.portal.dto.EmployeeListWrapper;
import com.portal.exception.BadRequestException;
import com.portal.model.Employee;
import com.portal.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * Service dedicated to parsing XML files and persisting employee records into the database.
 * 
 * Parsing flow:
 * Receive XML -> Parse XML -> Convert to Java Object -> Store in Database.
 */
@Service
public class XmlParserService {

    @Autowired
    private EmployeeRepository employeeRepository;

    private final XmlMapper xmlMapper;

    public XmlParserService() {
        this.xmlMapper = new XmlMapper();
        // Ignore unknown XML elements gracefully to prevent failures on optional attributes
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Parses an uploaded XML multipart file and persists employee records in the database.
     *
     * @param file uploaded XML multipart file
     * @return number of records saved/updated
     */
    @Transactional
    public int parseAndSave(MultipartFile file) {
        // Step 1: Validate file presence and non-emptiness
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        // Step 2: Validate file extension is .xml
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xml")) {
            throw new BadRequestException("Invalid file type. Only .xml files are supported");
        }

        EmployeeListWrapper wrapper;
        try (InputStream inputStream = file.getInputStream()) {
            // Step 3: Parse XML into Java DTO objects using Jackson XmlMapper
            wrapper = xmlMapper.readValue(inputStream, EmployeeListWrapper.class);
        } catch (Exception e) {
            // Step 4: Catch malformed XML or deserialization errors and throw a clean 400 Bad Request
            throw new BadRequestException("Malformed or invalid XML file: " + e.getMessage());
        }

        if (wrapper == null || wrapper.getEmployees() == null || wrapper.getEmployees().isEmpty()) {
            throw new BadRequestException("No valid employee records found in the uploaded XML");
        }

        List<Employee> employees = wrapper.getEmployees();

        // Step 5: Store in database.
        // Because employeeId is marked with @Id, saveAll() automatically updates existing rows
        // instead of duplicating them when the same XML is uploaded again.
        List<Employee> saved = employeeRepository.saveAll(employees);

        return saved.size();
    }
}
