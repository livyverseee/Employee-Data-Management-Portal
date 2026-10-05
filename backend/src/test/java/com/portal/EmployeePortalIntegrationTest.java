package com.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.dto.LoginRequest;
import com.portal.dto.LoginResponse;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests verifying:
 * - Authentication (login, invalid credentials)
 * - Role-Based Access Control (403 for employee on upload/delete, 401 on missing token)
 * - XML upload by DEAN
 * - Search, filtering, and pagination
 * - Single record lookup and 404 handling
 * - Record deletion by DEAN
 * - CSV export with filters
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EmployeePortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String deanToken;
    private String employeeToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate as DEAN
        LoginRequest deanLogin = new LoginRequest("dean", "dean123");
        MvcResult deanResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deanLogin)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse deanResponse = objectMapper.readValue(deanResult.getResponse().getContentAsString(), LoginResponse.class);
        this.deanToken = deanResponse.getToken();

        // Authenticate as EMPLOYEE
        LoginRequest empLogin = new LoginRequest("employee", "emp123");
        MvcResult empResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empLogin)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse empResponse = objectMapper.readValue(empResult.getResponse().getContentAsString(), LoginResponse.class);
        this.employeeToken = empResponse.getToken();
    }

    @Test
    @DisplayName("POST /api/auth/login - Success for DEAN")
    void testLoginDeanSuccess() throws Exception {
        LoginRequest req = new LoginRequest("dean", "dean123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("dean"))
                .andExpect(jsonPath("$.role").value("DEAN"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Failure on wrong password (401)")
    void testLoginFailure() throws Exception {
        LoginRequest req = new LoginRequest("dean", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /api/employees/upload - DEAN can upload XML (200)")
    void testDeanUploadXml() throws Exception {
        Path xmlPath = Paths.get("../sample-data/sample-employees.xml");
        byte[] content = Files.exists(xmlPath) ? Files.readAllBytes(xmlPath) : (
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><employees><employee><employeeId>EMP0001</employeeId><education>Bachelors</education><joiningYear>2017</joiningYear><city>Bangalore</city><paymentTier>3</paymentTier><age>34</age><gender>Male</gender><everBenched>No</everBenched><experienceInCurrentDomain>0</experienceInCurrentDomain><leaveOrNot>0</leaveOrNot></employee></employees>".getBytes()
        );

        MockMultipartFile file = new MockMultipartFile("file", "sample-employees.xml", "application/xml", content);

        mockMvc.perform(multipart("/api/employees/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Upload successful"))
                .andExpect(jsonPath("$.recordsSaved").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("POST /api/employees/upload - EMPLOYEE upload rejected with 403 Forbidden")
    void testEmployeeUploadForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.xml", "application/xml",
                "<employees></employees>".getBytes());

        mockMvc.perform(multipart("/api/employees/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You don't have permission"));
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} - EMPLOYEE delete rejected with 403 Forbidden")
    void testEmployeeDeleteForbidden() throws Exception {
        mockMvc.perform(delete("/api/employees/EMP0001")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You don't have permission"));
    }

    @Test
    @DisplayName("GET /api/employees - Missing token rejected with 401 Unauthorized")
    void testMissingTokenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Missing or invalid authorization token"));
    }

    @Test
    @DisplayName("GET /api/employees - Search & Pagination check")
    void testSearchAndPagination() throws Exception {
        // First ensure sample records exist
        testDeanUploadXml();

        // 1. Pagination check (size 5, page 0)
        mockMvc.perform(get("/api/employees?page=0&size=5")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));

        // 2. City filter check (Bangalore)
        mockMvc.perform(get("/api/employees?city=Bangalore")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].city", everyItem(equalTo("Bangalore"))));

        // 3. Gender filter check (Female)
        mockMvc.perform(get("/api/employees?gender=Female")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].gender", everyItem(equalTo("Female"))));

        // 4. Search text check (case-insensitive contains)
        mockMvc.perform(get("/api/employees?search=pune")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].city", everyItem(equalToIgnoringCase("pune"))));
    }

    @Test
    @DisplayName("GET /api/employees/{id} - Get single employee details")
    void testGetEmployeeById() throws Exception {
        testDeanUploadXml();

        mockMvc.perform(get("/api/employees/EMP0001")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP0001"))
                .andExpect(jsonPath("$.city").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/employees/{id} - Non-existent ID returns 404")
    void testGetEmployeeByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/employees/EMP_NON_EXISTENT")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} - DEAN can delete record (204)")
    void testDeanDeleteEmployee() throws Exception {
        testDeanUploadXml();

        mockMvc.perform(delete("/api/employees/EMP0001")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isNoContent());

        // Verify it is gone
        mockMvc.perform(get("/api/employees/EMP0001")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/employees/export - CSV Export with headers")
    void testExportCsv() throws Exception {
        testDeanUploadXml();

        mockMvc.perform(get("/api/employees/export?city=Bangalore")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"employees.csv\"")))
                .andExpect(content().string(startsWith("Employee ID,Education,Joining Year,City,Payment Tier,Age,Gender,Ever Benched,Experience In Current Domain,Left Company")));
    }
}
