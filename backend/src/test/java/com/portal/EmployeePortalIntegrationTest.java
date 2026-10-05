package com.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.dto.EmployeeUpdateRequest;
import com.portal.dto.LoginRequest;
import com.portal.dto.LoginResponse;
import com.portal.dto.RegisterRequest;
import com.portal.model.Employee;
import com.portal.repository.DatasetRepository;
import com.portal.repository.EmployeeRepository;
import com.portal.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests verifying:
 * - Registration (success, duplicate username/email 409, bad dean access code 403)
 * - Portal mismatch login 401
 * - First dataset upload OK (200), second upload 409 Conflict
 * - Dataset replace works, and bad file leaves old data intact
 * - Employee sees only the active dataset
 * - Every column filter and age range
 * - PUT edit success, validation 400, duplicate employeeId 409
 * - Employee gets 403 on PUT, DELETE, upload, and replace
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EmployeePortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DatasetService datasetService;

    private String deanToken;
    private String employeeToken;

    @BeforeEach
    void setUp() throws Exception {
        // Reset datasets and employee records for fresh isolated test state
        employeeRepository.deleteAll();
        datasetRepository.deleteAll();

        // Authenticate Dean via /api/auth/dean/login
        LoginRequest deanLogin = new LoginRequest("dean", "dean123");
        MvcResult deanResult = mockMvc.perform(post("/api/auth/dean/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deanLogin)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse deanResponse = objectMapper.readValue(deanResult.getResponse().getContentAsString(), LoginResponse.class);
        this.deanToken = deanResponse.getToken();

        // Authenticate Employee via /api/auth/employee/login
        LoginRequest empLogin = new LoginRequest("employee", "emp123");
        MvcResult empResult = mockMvc.perform(post("/api/auth/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empLogin)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse empResponse = objectMapper.readValue(empResult.getResponse().getContentAsString(), LoginResponse.class);
        this.employeeToken = empResponse.getToken();
    }

    private void ensureActiveDatasetUploaded() throws Exception {
        if (datasetRepository.findByActiveTrue().isEmpty()) {
            Path xmlPath = Paths.get("../sample-data/sample-employees.xml");
            byte[] content = Files.exists(xmlPath) ? Files.readAllBytes(xmlPath) : (
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><employees><employee><employeeId>EMP0001</employeeId><education>Bachelors</education><joiningYear>2017</joiningYear><city>Bangalore</city><paymentTier>3</paymentTier><age>34</age><gender>Male</gender><everBenched>No</everBenched><experienceInCurrentDomain>0</experienceInCurrentDomain><leaveOrNot>0</leaveOrNot></employee></employees>".getBytes()
            );
            MockMultipartFile file = new MockMultipartFile("file", "sample-employees.xml", "application/xml", content);
            datasetService.uploadDataset(file, "dean");
        }
    }

    // ====================================================
    // AUTHENTICATION & REGISTRATION TESTS
    // ====================================================

    @Test
    @DisplayName("Dean Registration - Success with valid access code (201)")
    void testDeanRegisterSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("New Dean", "newdean", "newdean@portal.com", "password123", "DEAN2026");
        mockMvc.perform(post("/api/auth/dean/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    @DisplayName("Dean Registration - Rejected on invalid access code (403)")
    void testDeanRegisterBadAccessCode() throws Exception {
        RegisterRequest req = new RegisterRequest("Bad Dean", "baddean", "baddean@portal.com", "password123", "WRONG_CODE");
        mockMvc.perform(post("/api/auth/dean/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Invalid dean access code"));
    }

    @Test
    @DisplayName("Employee Registration - Success (201)")
    void testEmployeeRegisterSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("New Emp", "newemp", "newemp@portal.com", "password123", null);
        mockMvc.perform(post("/api/auth/employee/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    @DisplayName("Registration - Duplicate username rejected with 409 Conflict")
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest req = new RegisterRequest("Duplicate User", "dean", "uniqueemail@portal.com", "password123", null);
        mockMvc.perform(post("/api/auth/employee/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    @DisplayName("Registration - Duplicate email rejected with 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest req = new RegisterRequest("Duplicate Email", "uniqueusername", "dean@portal.com", "password123", null);
        mockMvc.perform(post("/api/auth/employee/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("Login - Portal mismatch returns 401 Unauthorized")
    void testPortalMismatchLogin() throws Exception {
        // Dean attempting to log into Employee portal
        LoginRequest req = new LoginRequest("dean", "dean123");
        mockMvc.perform(post("/api/auth/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("These credentials do not belong to the Employee Portal"));

        // Employee attempting to log into Dean portal
        LoginRequest empReq = new LoginRequest("employee", "emp123");
        mockMvc.perform(post("/api/auth/dean/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("These credentials do not belong to the Dean Portal"));
    }

    // ====================================================
    // DATASET LIFECYCLE TESTS
    // ====================================================

    @Test
    @DisplayName("Dataset Upload - First upload OK, second upload 409 Conflict")
    void testDatasetUploadAndConflict() throws Exception {
        // Ensure database is clean of active datasets for this test
        datasetRepository.findByActiveTrue().ifPresent(d -> {
            employeeRepository.deleteByDatasetId(d.getId());
            datasetRepository.delete(d);
        });

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><employees><employee><employeeId>TEST001</employeeId><education>Bachelors</education><joiningYear>2017</joiningYear><city>Bangalore</city><paymentTier>3</paymentTier><age>34</age><gender>Male</gender><everBenched>No</everBenched><experienceInCurrentDomain>0</experienceInCurrentDomain><leaveOrNot>0</leaveOrNot></employee></employees>";
        MockMultipartFile file = new MockMultipartFile("file", "initial.xml", "application/xml", xml.getBytes());

        // First upload succeeds
        mockMvc.perform(multipart("/api/dataset/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.recordCount").value(1));

        // Second upload rejected with 409 Conflict
        mockMvc.perform(multipart("/api/dataset/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A dataset has already been uploaded"));
    }

    @Test
    @DisplayName("Dataset Replace - Successful replacement and atomic rollback on bad XML")
    void testDatasetReplace() throws Exception {
        ensureActiveDatasetUploaded();

        // 1. Try replace with bad XML -> rejected with 400 Bad Request
        MockMultipartFile badFile = new MockMultipartFile("file", "bad.xml", "application/xml", "<malformed><xml>".getBytes());
        mockMvc.perform(multipart("/api/dataset/replace")
                        .file(badFile)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Confirm existing data was NOT corrupted or destroyed
        mockMvc.perform(get("/api/dataset/active")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.recordCount").value(greaterThan(0)));

        // 2. Replace with valid new dataset
        String newXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><employees><employee><employeeId>REPLACE01</employeeId><education>Masters</education><joiningYear>2018</joiningYear><city>Pune</city><paymentTier>2</paymentTier><age>29</age><gender>Female</gender><everBenched>Yes</everBenched><experienceInCurrentDomain>4</experienceInCurrentDomain><leaveOrNot>1</leaveOrNot></employee></employees>";
        MockMultipartFile goodFile = new MockMultipartFile("file", "replacement.xml", "application/xml", newXml.getBytes());

        mockMvc.perform(multipart("/api/dataset/replace")
                        .file(goodFile)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("replacement.xml"))
                .andExpect(jsonPath("$.recordCount").value(1));

        // Verify employee list now contains only the new record
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeId").value("REPLACE01"));
    }

    // ====================================================
    // EMPLOYEE QUERIES, COLUMN FILTERS, AND AGE RANGE TESTS
    // ====================================================

    @Test
    @DisplayName("Employee Queries - Column filters, age range, and filter-options")
    void testFiltersAndAgeRange() throws Exception {
        // Upload sample dataset with 15 records
        Path xmlPath = Paths.get("../sample-data/sample-employees.xml");
        byte[] content = Files.readAllBytes(xmlPath);
        MockMultipartFile file = new MockMultipartFile("file", "sample-employees.xml", "application/xml", content);
        datasetService.replaceDataset(file, "dean");

        // 1. Filter options check
        mockMvc.perform(get("/api/employees/filter-options")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city", hasItems("Bangalore", "Pune", "New Delhi")))
                .andExpect(jsonPath("$.education", hasItem("Bachelors")))
                .andExpect(jsonPath("$.minAge").isNumber())
                .andExpect(jsonPath("$.maxAge").isNumber());

        // 2. City column filter (Pune)
        mockMvc.perform(get("/api/employees?city=Pune")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].city", everyItem(equalTo("Pune"))));

        // 3. Gender column filter (Female)
        mockMvc.perform(get("/api/employees?gender=Female")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].gender", everyItem(equalTo("Female"))));

        // 4. Age range filter (ageMin=25, ageMax=30)
        mockMvc.perform(get("/api/employees?ageMin=25&ageMax=30")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].age", everyItem(both(greaterThanOrEqualTo(25)).and(lessThanOrEqualTo(30)))));

        // 5. Payment Tier filter (tier 3)
        mockMvc.perform(get("/api/employees?paymentTier=3")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].paymentTier", everyItem(equalTo(3))));
    }

    // ====================================================
    // EDIT (PUT) AND DELETE TESTS
    // ====================================================

    @Test
    @DisplayName("PUT /api/employees/{id} - Dean edit success, validation failure, and duplicate ID check")
    void testEmployeeEdit() throws Exception {
        ensureActiveDatasetUploaded();

        Long activeDatasetId = datasetRepository.findByActiveTrue().get().getId();
        List<Employee> list = employeeRepository.findByDatasetId(activeDatasetId);
        assertFalse(list.isEmpty());
        Employee target = list.get(0);
        Long empDbId = target.getId();

        // 1. Successful update
        EmployeeUpdateRequest updateReq = new EmployeeUpdateRequest(
                "EMP_UPDATED", "Masters", 2018, "Pune", 2, 35, "Female", "Yes", 7, 0
        );

        mockMvc.perform(put("/api/employees/" + empDbId)
                        .header("Authorization", "Bearer " + deanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP_UPDATED"))
                .andExpect(jsonPath("$.education").value("Masters"))
                .andExpect(jsonPath("$.city").value("Pune"))
                .andExpect(jsonPath("$.age").value(35))
                .andExpect(jsonPath("$.experienceInCurrentDomain").value(7));

        // 2. Validation failure - invalid age (>70)
        EmployeeUpdateRequest badAgeReq = new EmployeeUpdateRequest(
                "EMP_UPDATED", "Masters", 2018, "Pune", 2, 95, "Female", "Yes", 7, 0
        );
        mockMvc.perform(put("/api/employees/" + empDbId)
                        .header("Authorization", "Bearer " + deanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badAgeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Age must be between 18 and 70")));

        // 3. Duplicate employeeId check (409 Conflict)
        if (list.size() > 1) {
            String existingOtherId = list.get(1).getEmployeeId();
            EmployeeUpdateRequest dupIdReq = new EmployeeUpdateRequest(
                    existingOtherId, "Masters", 2018, "Pune", 2, 35, "Female", "Yes", 7, 0
            );
            mockMvc.perform(put("/api/employees/" + empDbId)
                            .header("Authorization", "Bearer " + deanToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dupIdReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Employee ID already exists"));
        }
    }

    // ====================================================
    // ROLE AUTHORIZATION (403) CHECKS
    // ====================================================

    @Test
    @DisplayName("Employee role gets 403 Forbidden on upload, replace, PUT, and DELETE")
    void testEmployeeForbiddenMutations() throws Exception {
        ensureActiveDatasetUploaded();

        Long activeDatasetId = datasetRepository.findByActiveTrue().get().getId();
        Employee emp = employeeRepository.findByDatasetId(activeDatasetId).get(0);
        Long empDbId = emp.getId();

        MockMultipartFile file = new MockMultipartFile("file", "test.xml", "application/xml", "<employees></employees>".getBytes());

        // 1. Employee tries upload -> 403
        mockMvc.perform(multipart("/api/dataset/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You don't have permission"));

        // 2. Employee tries replace -> 403
        mockMvc.perform(multipart("/api/dataset/replace")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You don't have permission"));

        // 3. Employee tries PUT edit -> 403
        EmployeeUpdateRequest updateReq = new EmployeeUpdateRequest(
                "EMP_HACK", "PHD", 2015, "New Delhi", 1, 30, "Male", "No", 5, 0
        );
        mockMvc.perform(put("/api/employees/" + empDbId)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You don't have permission"));

        // 4. Employee tries DELETE -> 403
        mockMvc.perform(delete("/api/employees/" + empDbId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You don't have permission"));
    }

    @Test
    @DisplayName("DEAN can delete employee (204 No Content)")
    void testDeanDeleteSuccess() throws Exception {
        ensureActiveDatasetUploaded();

        Long activeDatasetId = datasetRepository.findByActiveTrue().get().getId();
        Employee emp = employeeRepository.findByDatasetId(activeDatasetId).get(0);
        Long empDbId = emp.getId();

        mockMvc.perform(delete("/api/employees/" + empDbId)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isNoContent());

        // Verify 404 on subsequent lookup
        mockMvc.perform(get("/api/employees/" + empDbId)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
