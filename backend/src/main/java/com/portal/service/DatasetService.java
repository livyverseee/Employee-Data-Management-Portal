package com.portal.service;

import com.portal.dto.ColumnDef;
import com.portal.dto.DatasetActiveResponse;
import com.portal.dto.ParsedDataset;
import com.portal.dto.TableData;
import com.portal.exception.BadRequestException;
import com.portal.exception.ConflictException;
import com.portal.exception.ResourceNotFoundException;
import com.portal.model.Dataset;
import com.portal.model.DatasetColumn;
import com.portal.repository.DatasetColumnRepository;
import com.portal.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing dataset lifecycle:
 * - Conversion pipeline: Read -> Convert to XML -> Parse -> Infer types -> Store
 * - Single active dataset rule
 * - Transactionally safe upload and replace with atomic rollback on failure
 */
@Service
public class DatasetService {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DatasetColumnRepository datasetColumnRepository;

    @Autowired
    private FileReaderService fileReaderService;

    @Autowired
    private XmlConverterService xmlConverterService;

    @Autowired
    private XmlParserService xmlParserService;

    @Autowired
    private SchemaInferenceService schemaInferenceService;

    @Autowired
    private DynamicTableService dynamicTableService;

    /**
     * Retrieves current active dataset metadata and schema columns.
     */
    public DatasetActiveResponse getActiveDataset() {
        Optional<Dataset> activeOpt = datasetRepository.findByActiveTrue();
        if (activeOpt.isEmpty()) {
            return DatasetActiveResponse.notFound();
        }

        Dataset dataset = activeOpt.get();
        List<DatasetColumn> dbCols = datasetColumnRepository.findByDatasetIdOrderByPositionAsc(dataset.getId());
        List<ColumnDef> columns = toColumnDefs(dbCols);

        DatasetActiveResponse res = new DatasetActiveResponse();
        res.setExists(true);
        res.setId(dataset.getId());
        res.setFileName(dataset.getFileName());
        res.setSourceFormat(dataset.getSourceFormat());
        res.setTableName(dataset.getTableName());
        res.setUploadedBy(dataset.getUploadedBy());
        res.setUploadedAt(dataset.getUploadedAt());
        res.setRecordCount(dataset.getRecordCount());
        res.setColumns(columns);
        return res;
    }

    /**
     * Upload initial dataset (DEAN only).
     * Rejected with 409 if active dataset already exists.
     */
    public DatasetActiveResponse uploadDataset(MultipartFile file, String uploadedBy) {
        if (datasetRepository.findByActiveTrue().isPresent()) {
            throw new ConflictException("A dataset has already been uploaded. Use replace to update.");
        }
        return processAndStoreUpload(file, uploadedBy, false);
    }

    /**
     * Replace existing dataset (DEAN only).
     * Validates and loads new dataset in its own table first.
     * Old table and metadata are removed only AFTER the new dataset is activated.
     */
    public DatasetActiveResponse replaceDataset(MultipartFile file, String uploadedBy) {
        return processAndStoreUpload(file, uploadedBy, true);
    }

    private DatasetActiveResponse processAndStoreUpload(MultipartFile file, String uploadedBy, boolean isReplace) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BadRequestException("File name cannot be empty");
        }

        String sourceFormat = fileReaderService.detectFormat(fileName);
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new BadRequestException("Failed to read uploaded file: " + e.getMessage());
        }

        // ==========================================
        // PIPELINE STEP A & B: Convert to XML
        // ==========================================
        byte[] xmlBytes;
        if ("XML".equalsIgnoreCase(sourceFormat)) {
            // Step A & B skipped: it already is XML
            xmlBytes = fileBytes;
        } else {
            // Step A: Read into flat TableData
            TableData tableData = fileReaderService.readToTable(fileName, fileBytes);
            // Step B: Convert to standardized internal XML
            String xmlString = xmlConverterService.convertToXml(tableData);
            xmlBytes = xmlString.getBytes(StandardCharsets.UTF_8);
        }

        // ==========================================
        // PIPELINE STEP C: Parse XML into Java objects
        // ==========================================
        ParsedDataset parsedDataset = xmlParserService.parseXml(xmlBytes);

        // ==========================================
        // PIPELINE STEP D: Infer Schema & Filters
        // ==========================================
        List<ColumnDef> columns = schemaInferenceService.inferSchema(parsedDataset);

        // ==========================================
        // PIPELINE STEP E: Store in Dynamic Table
        // ==========================================
        // 1. Create new Dataset metadata placeholder
        Dataset newDataset = new Dataset();
        newDataset.setFileName(fileName);
        newDataset.setSourceFormat(sourceFormat);
        newDataset.setTableName("ds_temp"); // Updated below with ID
        newDataset.setUploadedBy(uploadedBy != null ? uploadedBy : "dean");
        newDataset.setUploadedAt(LocalDateTime.now());
        newDataset.setRecordCount(parsedDataset.getRows().size());
        newDataset.setActive(false);
        newDataset = datasetRepository.save(newDataset);

        String tableName = "ds_" + newDataset.getId();
        newDataset.setTableName(tableName);
        datasetRepository.save(newDataset);

        try {
            // 2. Create dynamic table ds_<id>
            dynamicTableService.createTable(tableName, columns);

            // 3. Batch insert rows
            dynamicTableService.batchInsertRows(tableName, columns, parsedDataset.getRows());

            // 4. Save column metadata
            List<DatasetColumn> dbColumns = new ArrayList<>();
            for (ColumnDef c : columns) {
                DatasetColumn dc = new DatasetColumn();
                dc.setDatasetId(newDataset.getId());
                dc.setKey(c.getKey());
                dc.setLabel(c.getLabel());
                dc.setType(c.getType());
                dc.setNumberKind(c.getNumberKind());
                dc.setFilterType(c.getFilterType());
                dc.setNullable(c.isNullable());
                dc.setSequential(c.isSequential());
                dc.setPosition(c.getPosition());
                dbColumns.add(dc);
            }
            datasetColumnRepository.saveAll(dbColumns);

            // 5. Atomic activation & Old dataset cleanup
            activateNewDatasetAndCleanupOld(newDataset);

        } catch (Exception e) {
            // Rollback DDL: drop the newly created table and remove metadata
            try {
                dynamicTableService.dropTable(tableName);
                datasetColumnRepository.deleteByDatasetId(newDataset.getId());
                datasetRepository.delete(newDataset);
            } catch (Exception ignored) {}

            if (e instanceof BadRequestException) {
                throw (BadRequestException) e;
            }
            throw new BadRequestException("Failed to ingest dataset: " + e.getMessage());
        }

        DatasetActiveResponse res = new DatasetActiveResponse();
        res.setExists(true);
        res.setId(newDataset.getId());
        res.setFileName(newDataset.getFileName());
        res.setSourceFormat(newDataset.getSourceFormat());
        res.setTableName(newDataset.getTableName());
        res.setUploadedBy(newDataset.getUploadedBy());
        res.setUploadedAt(newDataset.getUploadedAt());
        res.setRecordCount(newDataset.getRecordCount());
        res.setColumns(columns);
        return res;
    }

    @Transactional
    public void activateNewDatasetAndCleanupOld(Dataset newDataset) {
        List<Dataset> oldActiveList = datasetRepository.findByActive(true);

        // Activate new dataset
        newDataset.setActive(true);
        datasetRepository.save(newDataset);

        // Deactivate and drop old tables
        for (Dataset old : oldActiveList) {
            if (!old.getId().equals(newDataset.getId())) {
                try {
                    dynamicTableService.dropTable(old.getTableName());
                    datasetColumnRepository.deleteByDatasetId(old.getId());
                    datasetRepository.delete(old);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Streams current active dataset as XML for GET /api/dataset/xml (DEAN only).
     */
    public void streamCurrentDatasetXml(OutputStream os) throws Exception {
        Optional<Dataset> activeOpt = datasetRepository.findByActiveTrue();
        if (activeOpt.isEmpty()) {
            throw new ResourceNotFoundException("No active dataset exists to download as XML");
        }

        Dataset dataset = activeOpt.get();
        List<DatasetColumn> dbCols = datasetColumnRepository.findByDatasetIdOrderByPositionAsc(dataset.getId());
        List<ColumnDef> columns = toColumnDefs(dbCols);

        dynamicTableService.streamDatasetXml(dataset.getTableName(), columns, os);
    }

    /**
     * Active dataset context holder.
     */
    public static class ActiveDatasetContext {
        public Dataset dataset;
        public List<ColumnDef> columns;

        public ActiveDatasetContext(Dataset dataset, List<ColumnDef> columns) {
            this.dataset = dataset;
            this.columns = columns;
        }
    }

    public ActiveDatasetContext getActiveContext() {
        Optional<Dataset> activeOpt = datasetRepository.findByActiveTrue();
        if (activeOpt.isEmpty()) {
            return null;
        }
        Dataset dataset = activeOpt.get();
        List<DatasetColumn> dbCols = datasetColumnRepository.findByDatasetIdOrderByPositionAsc(dataset.getId());
        return new ActiveDatasetContext(dataset, toColumnDefs(dbCols));
    }

    public void updateRecordCount(Long datasetId, int delta) {
        datasetRepository.findById(datasetId).ifPresent(d -> {
            d.setRecordCount(Math.max(0, d.getRecordCount() + delta));
            datasetRepository.save(d);
        });
    }

    private List<ColumnDef> toColumnDefs(List<DatasetColumn> dbCols) {
        List<ColumnDef> list = new ArrayList<>();
        for (DatasetColumn dc : dbCols) {
            ColumnDef c = new ColumnDef();
            c.setKey(dc.getKey());
            c.setLabel(dc.getLabel());
            c.setType(dc.getType());
            c.setNumberKind(dc.getNumberKind());
            c.setFilterType(dc.getFilterType());
            c.setNullable(dc.isNullable());
            c.setSequential(dc.isSequential());
            c.setPosition(dc.getPosition());
            list.add(c);
        }
        return list;
    }
}
