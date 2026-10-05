package com.portal.controller;

import com.portal.dto.DatasetActiveResponse;
import com.portal.service.DatasetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for dataset lifecycle operations:
 * - Ingest initial dataset (DEAN only)
 * - Replace existing dataset (DEAN only)
 * - Check active dataset status (DEAN and EMPLOYEE)
 */
@RestController
@RequestMapping("/api/dataset")
public class DatasetController {

    @Autowired
    private DatasetService datasetService;

    /**
     * Check if an active dataset exists and retrieve its metadata.
     */
    @GetMapping("/active")
    public ResponseEntity<DatasetActiveResponse> getActiveDataset() {
        DatasetActiveResponse response = datasetService.getActiveDatasetInfo();
        return ResponseEntity.ok(response);
    }

    /**
     * Upload the initial dataset (DEAN only).
     * Fails with 409 Conflict if an active dataset is already present.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DatasetActiveResponse> uploadDataset(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String username = (String) request.getAttribute("currentUser");
        if (username == null) {
            username = "dean";
        }

        DatasetActiveResponse response = datasetService.uploadDataset(file, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Replace the active dataset with a new XML file (DEAN only).
     */
    @PostMapping(value = "/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DatasetActiveResponse> replaceDataset(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String username = (String) request.getAttribute("currentUser");
        if (username == null) {
            username = "dean";
        }

        DatasetActiveResponse response = datasetService.replaceDataset(file, username);
        return ResponseEntity.ok(response);
    }
}
