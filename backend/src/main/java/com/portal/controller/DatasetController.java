package com.portal.controller;

import com.portal.dto.DatasetActiveResponse;
import com.portal.service.DatasetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Controller for dataset lifecycle:
 * - GET /api/dataset/active (DEAN and EMPLOYEE)
 * - POST /api/dataset/upload (DEAN only)
 * - POST /api/dataset/replace (DEAN only)
 * - GET /api/dataset/xml (DEAN only, download current dataset as XML attachment)
 */
@RestController
public class DatasetController {

    @Autowired
    private DatasetService datasetService;

    /**
     * Retrieve current active dataset metadata and schema.
     */
    @GetMapping({"/api/dataset/active", "/api/datasets/active"})
    public ResponseEntity<DatasetActiveResponse> getActiveDataset() {
        DatasetActiveResponse active = datasetService.getActiveDataset();
        return ResponseEntity.ok(active);
    }

    /**
     * Initial upload of a structured file (.xlsx, .csv, .json, .xml) (DEAN only).
     */
    @PostMapping({"/api/dataset/upload", "/api/datasets/upload"})
    public ResponseEntity<DatasetActiveResponse> uploadDataset(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String username = (String) request.getAttribute("currentUser");
        DatasetActiveResponse response = datasetService.uploadDataset(file, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Transactionally replace active dataset with a new file (DEAN only).
     */
    @PostMapping({"/api/dataset/replace", "/api/datasets/replace"})
    public ResponseEntity<DatasetActiveResponse> replaceDataset(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String username = (String) request.getAttribute("currentUser");
        DatasetActiveResponse response = datasetService.replaceDataset(file, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Download the CURRENT dataset (with any adds/edits/deletes) as XML attachment (DEAN only).
     */
    @GetMapping({"/api/dataset/xml", "/api/datasets/xml"})
    public void downloadDatasetXml(HttpServletResponse response) throws Exception {
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dataset.xml\"");
        datasetService.streamCurrentDatasetXml(response.getOutputStream());
        response.flushBuffer();
    }
}
