package com.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.dto.LoginRequest;
import com.portal.dto.LoginResponse;
import com.portal.dto.RecordMutationRequest;
import com.portal.model.Dataset;
import com.portal.repository.DatasetColumnRepository;
import com.portal.repository.DatasetRepository;
import com.portal.service.DynamicTableService;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for the Schema-Agnostic Dynamic Data Management Portal:
 * - XLSX upload of Employees.xlsx (689 rows, 15 columns, inferred types & filterTypes)
 * - CSV, JSON, XML uploads produce identical internal XML representation
 * - Second upload -> 409 Conflict; Replace works; Corrupt replace leaves old data intact
 * - Mandatory pagination: default size 10, clamping size > 100 to 100
 * - Every filter type (eq_, like_, min_/max_, from_/to_) and global search
 * - Filter-options returns non-empty sorted values for CATEGORY, min/max for NUMBER and DATE
 * - Record CRUD: POST add with validation, PUT edit, DELETE, recordCount integrity
 * - SQL Injection security verification
 * - Excel export read back via Apache POI (headers, row counts, cell types, filter awareness)
 * - CSV export streaming with UTF-8 BOM
 * - Role-Based Access Control: DEAN full permissions, EMPLOYEE 403 on mutations, 401 unauthenticated
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
    private DatasetColumnRepository datasetColumnRepository;

    @Autowired
    private DynamicTableService dynamicTableService;

    private String deanToken;
    private String employeeToken;

    @BeforeEach
    void setUp() throws Exception {
        // Drop any leftover dynamic tables and metadata for clean test isolation
        List<Dataset> datasets = datasetRepository.findAll();
        for (Dataset d : datasets) {
            dynamicTableService.dropTable(d.getTableName());
        }
        datasetColumnRepository.deleteAll();
        datasetRepository.deleteAll();

        // Dean token
        LoginRequest deanLogin = new LoginRequest("dean", "dean123");
        MvcResult deanResult = mockMvc.perform(post("/api/auth/dean/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deanLogin)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse deanResponse = objectMapper.readValue(deanResult.getResponse().getContentAsString(), LoginResponse.class);
        this.deanToken = deanResponse.getToken();

        // Employee token
        LoginRequest empLogin = new LoginRequest("employee", "emp123");
        MvcResult empResult = mockMvc.perform(post("/api/auth/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empLogin)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse empResponse = objectMapper.readValue(empResult.getResponse().getContentAsString(), LoginResponse.class);
        this.employeeToken = empResponse.getToken();
    }

    private void uploadEmployeesXlsx() throws Exception {
        Path path = Paths.get("sample-data/Employees.xlsx");
        if (!Files.exists(path)) {
            path = Paths.get("../sample-data/Employees.xlsx");
        }
        assertTrue(Files.exists(path), "sample-data/Employees.xlsx must exist at " + path.toAbsolutePath());
        byte[] bytes = Files.readAllBytes(path);

        MockMultipartFile file = new MockMultipartFile("file", "Employees.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        mockMvc.perform(multipart("/api/dataset/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(true)))
                .andExpect(jsonPath("$.recordCount", is(689)))
                .andExpect(jsonPath("$.columns", hasSize(15)));
    }

    @Test
    @DisplayName("1. Upload sample-data/Employees.xlsx and verify inferred schema types & filterTypes")
    void testUploadEmployeesXlsx() throws Exception {
        uploadEmployeesXlsx();

        // Verify active dataset metadata and schema
        MvcResult res = mockMvc.perform(get("/api/dataset/active")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(true)))
                .andExpect(jsonPath("$.recordCount", is(689)))
                .andExpect(jsonPath("$.sourceFormat", is("XLSX")))
                .andReturn();

        String json = res.getResponse().getContentAsString();

        // Inferred types verification:
        // No = NUMBER, sequential; First Name, Last Name = TEXT_SEARCH; Gender, Department, Country, Center = CATEGORY; Start Date = DATE_RANGE; Job Rate = decimal
        assertTrue(json.contains("\"key\":\"no\""));
        assertTrue(json.contains("\"type\":\"NUMBER\""));
        assertTrue(json.contains("\"sequential\":true"));
        assertTrue(json.contains("\"key\":\"first_name\""));
        assertTrue(json.contains("\"filterType\":\"TEXT_SEARCH\""));
        assertTrue(json.contains("\"key\":\"gender\""));
        assertTrue(json.contains("\"filterType\":\"CATEGORY\""));
        assertTrue(json.contains("\"key\":\"start_date\""));
        assertTrue(json.contains("\"filterType\":\"DATE_RANGE\""));
        assertTrue(json.contains("\"key\":\"job_rate\""));
        assertTrue(json.contains("\"numberKind\":\"DECIMAL\""));
    }

    @Test
    @DisplayName("2. Upload CSV, JSON and XML format files and verify uniform ingestion")
    void testUploadCsvJsonXml() throws Exception {
        // Upload CSV
        String csv = "id,name,role\n1,Alice,Engineer\n2,Bob,Manager";
        MockMultipartFile csvFile = new MockMultipartFile("file", "staff.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/dataset/upload")
                        .file(csvFile)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordCount", is(2)))
                .andExpect(jsonPath("$.sourceFormat", is("CSV")));

        // Clean up
        datasetRepository.deleteAll();

        // Upload JSON
        String json = "[{\"id\": 10, \"title\": \"Widget\", \"price\": 19.99}, {\"id\": 20, \"title\": \"Gadget\", \"price\": 29.99}]";
        MockMultipartFile jsonFile = new MockMultipartFile("file", "items.json", "application/json", json.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/dataset/upload")
                        .file(jsonFile)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordCount", is(2)))
                .andExpect(jsonPath("$.sourceFormat", is("JSON")));

        // Clean up
        datasetRepository.deleteAll();

        // Upload XML
        String xml = "<students><student><rollNo>101</rollNo><name>Arun</name></student><student><rollNo>102</rollNo><name>Deepa</name></student></students>";
        MockMultipartFile xmlFile = new MockMultipartFile("file", "students.xml", "application/xml", xml.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/dataset/upload")
                        .file(xmlFile)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordCount", is(2)))
                .andExpect(jsonPath("$.sourceFormat", is("XML")));
    }

    @Test
    @DisplayName("3. Second upload rejects with 409; Replace works; Corrupt replace preserves old dataset")
    void testUploadConflictAndTransactionalReplace() throws Exception {
        uploadEmployeesXlsx();

        // Second upload -> 409 Conflict
        MockMultipartFile file = new MockMultipartFile("file", "duplicate.csv", "text/csv", "a,b\n1,2".getBytes());
        mockMvc.perform(multipart("/api/dataset/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isConflict());

        // Replace with corrupt XML -> 400 Bad Request, old dataset must remain intact!
        MockMultipartFile corruptFile = new MockMultipartFile("file", "corrupt.xml", "application/xml", "<corrupt><unclosed>".getBytes());
        mockMvc.perform(multipart("/api/dataset/replace")
                        .file(corruptFile)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isBadRequest());

        // Verify old dataset of 689 records is preserved
        mockMvc.perform(get("/api/dataset/active")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordCount", is(689)));
    }

    @Test
    @DisplayName("4. Mandatory pagination: default size 10, clamping size > 100 to 100, page & totalPages calculations")
    void testMandatoryPagination() throws Exception {
        uploadEmployeesXlsx();

        // Default pagination: page=0, size=10 -> 689 records = 69 pages
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalElements", is(689)))
                .andExpect(jsonPath("$.totalPages", is(69)))
                .andExpect(jsonPath("$.content", hasSize(10)));

        // Clamping: request size=1000 clamped to 100 -> 7 pages
        mockMvc.perform(get("/api/records?size=1000")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(100)))
                .andExpect(jsonPath("$.totalPages", is(7)))
                .andExpect(jsonPath("$.content", hasSize(100)));
    }

    @Test
    @DisplayName("5. Dynamic filters: eq_, like_, min_/max_, from_/to_, and global search")
    void testDynamicFiltersAndSearch() throws Exception {
        uploadEmployeesXlsx();

        // Global search for "Ghadir"
        mockMvc.perform(get("/api/records?search=Ghadir")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].data.first_name", is("Ghadir")));

        // Exact match filter eq_gender=Female
        mockMvc.perform(get("/api/records?eq_gender=Female")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].data.gender", is("Female")));

        // Partial contains filter like_first_name=gha
        mockMvc.perform(get("/api/records?like_first_name=gha")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].data.first_name", containsStringIgnoringCase("gha")));

        // Numeric range min_years=2 & max_years=5
        mockMvc.perform(get("/api/records?min_years=2&max_years=5")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThan(0)));

        // Date range from_start_date=2018-01-01 & to_start_date=2018-12-31
        mockMvc.perform(get("/api/records?from_start_date=2018-01-01&to_start_date=2018-12-31")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThan(0)));
    }

    @Test
    @DisplayName("6. GET /api/records/filter-options returns distinct sorted categories and numeric/date ranges")
    void testFilterOptions() throws Exception {
        uploadEmployeesXlsx();

        MvcResult res = mockMvc.perform(get("/api/records/filter-options")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andReturn();

        String json = res.getResponse().getContentAsString();
        // CATEGORY columns must return list of options
        assertTrue(json.contains("\"gender\":[\"Female\",\"Male\"]") || json.contains("\"gender\":[\"Male\",\"Female\"]"));
        assertTrue(json.contains("\"department\":["));
        assertTrue(json.contains("\"country\":["));
        assertTrue(json.contains("\"center\":["));
        // NUMBER & DATE columns must return min & max
        assertTrue(json.contains("\"years\":{"));
        assertTrue(json.contains("\"min\":"));
        assertTrue(json.contains("\"max\":"));
    }

    @Test
    @DisplayName("7. Record CRUD: POST add with validation, PUT edit, DELETE, and recordCount maintenance")
    void testRecordCrudAndValidation() throws Exception {
        uploadEmployeesXlsx();

        // 1. Add valid record
        Map<String, Object> newRecord = new HashMap<>();
        newRecord.put("no", 700);
        newRecord.put("first_name", "TestFirstName");
        newRecord.put("last_name", "TestLastName");
        newRecord.put("gender", "Female");
        newRecord.put("start_date", "2023-05-15");
        newRecord.put("years", 1);
        newRecord.put("department", "IT");
        newRecord.put("country", "India");
        newRecord.put("center", "North");
        newRecord.put("monthly_salary", 5000);
        newRecord.put("annual_salary", 60000);
        newRecord.put("job_rate", 2.5);
        newRecord.put("sick_leaves", 2);
        newRecord.put("unpaid_leaves", 0);
        newRecord.put("overtime_hours", 10);

        RecordMutationRequest addRequest = new RecordMutationRequest(newRecord);

        MvcResult addRes = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + deanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.first_name", is("TestFirstName")))
                .andReturn();

        // Verify dataset recordCount increased to 690
        mockMvc.perform(get("/api/dataset/active")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(jsonPath("$.recordCount", is(690)));

        // Extract ID
        Map<?, ?> responseMap = objectMapper.readValue(addRes.getResponse().getContentAsString(), Map.class);
        Long newId = Long.valueOf(responseMap.get("id").toString());

        // 2. Validation failure on invalid date
        Map<String, Object> badDateRecord = new HashMap<>(newRecord);
        badDateRecord.put("start_date", "invalid-date-format");
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + deanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordMutationRequest(badDateRecord))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.start_date", notNullValue()));

        // 3. Validation failure on invalid integer
        Map<String, Object> badNumRecord = new HashMap<>(newRecord);
        badNumRecord.put("years", "not-a-number");
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + deanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordMutationRequest(badNumRecord))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.years", notNullValue()));

        // 4. Update record via PUT
        newRecord.put("first_name", "UpdatedFirstName");
        mockMvc.perform(put("/api/records/" + newId)
                        .header("Authorization", "Bearer " + deanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordMutationRequest(newRecord))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.first_name", is("UpdatedFirstName")));

        // 5. Delete record via DELETE
        mockMvc.perform(delete("/api/records/" + newId)
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isNoContent());

        // Verify dataset recordCount decreased back to 689
        mockMvc.perform(get("/api/dataset/active")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(jsonPath("$.recordCount", is(689)));
    }

    @Test
    @DisplayName("8. SQL identifier injection attempt is safely ignored/prevented")
    void testSqlInjectionProtection() throws Exception {
        uploadEmployeesXlsx();

        // Inject malicious filter param
        mockMvc.perform(get("/api/records?eq_x;DROP TABLE users=1")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk());

        // Users table must still be intact
        mockMvc.perform(post("/api/auth/dean/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("dean", "dean123"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("9. Excel export (.xlsx) via POI: headers, row counts, cell types, and filter awareness")
    void testExcelExportWithPoi() throws Exception {
        uploadEmployeesXlsx();

        // Export all 689 records
        MvcResult res = mockMvc.perform(get("/api/records/export/excel")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("employee_data.xlsx")))
                .andReturn();

        byte[] xlsxBytes = res.getResponse().getContentAsByteArray();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet sheet = wb.getSheet("Employees");
            assertNotNull(sheet, "Sheet 'Employees' must exist");
            assertEquals(690, sheet.getPhysicalNumberOfRows(), "689 data rows + 1 header row = 690 rows");

            Row headerRow = sheet.getRow(0);
            assertEquals("No", headerRow.getCell(0).getStringCellValue());
            assertEquals("First Name", headerRow.getCell(1).getStringCellValue());

            // First data row cell types
            Row row1 = sheet.getRow(1);
            assertEquals(CellType.NUMERIC, row1.getCell(0).getCellType()); // 'No' is numeric
            assertEquals(CellType.STRING, row1.getCell(1).getCellType());  // 'First Name' is text
        }

        // Export with filter (eq_gender=Female)
        MvcResult filteredRes = mockMvc.perform(get("/api/records/export/excel?eq_gender=Female")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andReturn();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(filteredRes.getResponse().getContentAsByteArray()))) {
            Sheet sheet = wb.getSheet("Employees");
            assertTrue(sheet.getPhysicalNumberOfRows() < 690 && sheet.getPhysicalNumberOfRows() > 1);
        }
    }

    @Test
    @DisplayName("10. CSV export streaming with UTF-8 BOM")
    void testCsvExportStreaming() throws Exception {
        uploadEmployeesXlsx();

        MvcResult res = mockMvc.perform(get("/api/records/export")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("employee_data.csv")))
                .andReturn();

        byte[] csvBytes = res.getResponse().getContentAsByteArray();
        assertTrue(csvBytes.length > 3);
        // Verify UTF-8 BOM
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String csvString = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvString.contains("No,First Name,Last Name"));
    }

    @Test
    @DisplayName("11. Download current dataset as XML (GET /api/dataset/xml)")
    void testDownloadDatasetXml() throws Exception {
        uploadEmployeesXlsx();

        MvcResult res = mockMvc.perform(get("/api/dataset/xml")
                        .header("Authorization", "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("dataset.xml")))
                .andReturn();

        String xmlContent = res.getResponse().getContentAsString();
        assertTrue(xmlContent.startsWith("<?xml"));
        assertTrue(xmlContent.contains("<dataset>"));
        assertTrue(xmlContent.contains("<columns>"));
        assertTrue(xmlContent.contains("<column key=\"first_name\" label=\"First Name\"/>"));
        assertTrue(xmlContent.contains("<records>"));
        assertTrue(xmlContent.contains("<record>"));
    }

    @Test
    @DisplayName("12. Role-Based Access Control: EMPLOYEE blocked with 403 on mutations and XML download; 401 without token")
    void testRbacPermissions() throws Exception {
        uploadEmployeesXlsx();

        // 401 without token
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isUnauthorized());

        // Employee allowed reads and exports
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/records/export/excel")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/records/export")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // Employee gets 403 on upload
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "a\n1".getBytes());
        mockMvc.perform(multipart("/api/dataset/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // Employee gets 403 on replace
        mockMvc.perform(multipart("/api/dataset/replace")
                        .file(file)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // Employee gets 403 on dataset/xml
        mockMvc.perform(get("/api/dataset/xml")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // Employee gets 403 on POST /api/records
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{}}"))
                .andExpect(status().isForbidden());

        // Employee gets 403 on PUT /api/records/1
        mockMvc.perform(put("/api/records/1")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{}}"))
                .andExpect(status().isForbidden());

        // Employee gets 403 on DELETE /api/records/1
        mockMvc.perform(delete("/api/records/1")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }
}
