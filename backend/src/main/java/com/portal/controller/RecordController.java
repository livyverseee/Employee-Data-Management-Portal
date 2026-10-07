package com.portal.controller;

import com.portal.dto.RecordItem;
import com.portal.dto.RecordMutationRequest;
import com.portal.dto.RecordPageResponse;
import com.portal.exception.ResourceNotFoundException;
import com.portal.service.DatasetService;
import com.portal.service.DynamicTableService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Controller for row-level record endpoints:
 * - GET /api/records (mandatory pagination, sorting by id ASC)
 * - GET /api/records/filter-options
 * - GET /api/records/{id}
 * - POST /api/records (DEAN only, add record)
 * - PUT /api/records/{id} (DEAN only, update record)
 * - DELETE /api/records/{id} (DEAN only, delete record)
 * - GET /api/records/export/excel (SXSSFWorkbook Excel streaming)
 * - GET /api/records/export (CSV streaming with UTF-8 BOM)
 */
@RestController
public class RecordController {

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private DynamicTableService dynamicTableService;

    /**
     * Mandatory paginated list of records scoped to active dataset.
     */
    @GetMapping({"/api/records", "/api/datasets/records", "/api/employees"})
    public ResponseEntity<RecordPageResponse> getRecords(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam Map<String, String> allParams) {

        DatasetService.ActiveDatasetContext context = datasetService.getActiveContext();
        if (context == null) {
            return ResponseEntity.ok(new RecordPageResponse(Collections.emptyList(), 0, size, 0, 0));
        }

        RecordPageResponse pageResponse = dynamicTableService.queryRecords(
                context.dataset.getTableName(),
                context.columns,
                allParams,
                page,
                size
        );

        return ResponseEntity.ok(pageResponse);
    }

    /**
     * Retrieves distinct options for CATEGORY columns and min/max for NUMBER and DATE columns.
     */
    @GetMapping({"/api/records/filter-options", "/api/datasets/filter-options", "/api/employees/filter-options"})
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        DatasetService.ActiveDatasetContext context = datasetService.getActiveContext();
        if (context == null) {
            return ResponseEntity.ok(Collections.emptyMap());
        }

        Map<String, Object> options = dynamicTableService.getFilterOptions(
                context.dataset.getTableName(),
                context.columns
        );

        return ResponseEntity.ok(options);
    }

    /**
     * Retrieves a single record by ID.
     */
    @GetMapping({"/api/records/{id}", "/api/datasets/records/{id}", "/api/employees/{id}"})
    public ResponseEntity<RecordItem> getRecordById(@PathVariable("id") Long id) {
        DatasetService.ActiveDatasetContext context = datasetService.getActiveContext();
        if (context == null) {
            throw new ResourceNotFoundException("No active dataset exists");
        }

        RecordItem record = dynamicTableService.getRecordById(
                context.dataset.getTableName(),
                context.columns,
                id
        );

        return ResponseEntity.ok(record);
    }

    /**
     * Adds a new record to the active dataset (DEAN only).
     */
    @PostMapping({"/api/records", "/api/datasets/records", "/api/employees"})
    public ResponseEntity<RecordItem> addRecord(@RequestBody RecordMutationRequest body) {
        DatasetService.ActiveDatasetContext context = datasetService.getActiveContext();
        if (context == null) {
            throw new ResourceNotFoundException("No active dataset exists");
        }

        RecordItem saved = dynamicTableService.insertRecord(
                context.dataset.getTableName(),
                context.columns,
                body.getData()
        );

        datasetService.updateRecordCount(context.dataset.getId(), 1);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Updates an existing record by ID (DEAN only).
     */
    @PutMapping({"/api/records/{id}", "/api/datasets/records/{id}", "/api/employees/{id}"})
    public ResponseEntity<RecordItem> updateRecord(
            @PathVariable("id") Long id,
            @RequestBody RecordMutationRequest body) {

        DatasetService.ActiveDatasetContext context = datasetService.getActiveContext();
        if (context == null) {
            throw new ResourceNotFoundException("No active dataset exists");
        }

        RecordItem updated = dynamicTableService.updateRecord(
                context.dataset.getTableName(),
                context.columns,
                id,
                body.getData()
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes an individual record by ID (DEAN only).
     */
    @DeleteMapping({"/api/records/{id}", "/api/datasets/records/{id}", "/api/employees/{id}"})
    public ResponseEntity<Void> deleteRecord(@PathVariable("id") Long id) {
        DatasetService.ActiveDatasetContext context = datasetService.getActiveContext();
        if (context == null) {
            throw new ResourceNotFoundException("No active dataset exists");
        }

        dynamicTableService.deleteRecord(context.dataset.getTableName(), id);
        datasetService.updateRecordCount(context.dataset.getId(), -1);
        return ResponseEntity.noContent().build();
    }

    /**
     * Streams filtered records as Apache POI Excel workbook.
     */
    @GetMapping({"/api/records/export/excel", "/api/datasets/export/excel", "/api/employees/export/excel"})
    public void exportExcel(
            @RequestParam Map<String, String> allParams,
            HttpServletResponse response) throws IOException {

        DatasetService.ActiveDatasetContext context = datasetService.getActiveContext();
        if (context == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employee_data.xlsx\"");

        dynamicTableService.exportExcel(context.dataset.getTableName(), context.columns, allParams, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * Streams filtered records as CSV with UTF-8 BOM.
     */
    @GetMapping({"/api/records/export", "/api/datasets/export", "/api/employees/export"})
    public void exportCsv(
            @RequestParam Map<String, String> allParams,
            HttpServletResponse response) throws IOException {

        DatasetService.ActiveDatasetContext context = datasetService.getActiveContext();
        if (context == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employee_data.csv\"");

        dynamicTableService.exportCsv(context.dataset.getTableName(), context.columns, allParams, response.getOutputStream());
        response.flushBuffer();
    }
}
