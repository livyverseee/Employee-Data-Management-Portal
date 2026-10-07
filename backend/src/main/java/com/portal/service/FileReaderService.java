package com.portal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.dto.TableData;
import com.portal.exception.BadRequestException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Step A: Reads uploaded file (.xlsx, .csv, .json) into a flat in-memory TableData.
 * Enforces size limits: 10 MB, 100,000 rows, 60 columns, at least 1 data row.
 */
@Service
public class FileReaderService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_ROWS = 100000;
    private static final int MAX_COLUMNS = 60;
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Determines source format from file name.
     */
    public String detectFormat(String fileName) {
        if (fileName == null) {
            throw new BadRequestException("File name cannot be empty");
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xlsx")) return "XLSX";
        if (lower.endsWith(".csv")) return "CSV";
        if (lower.endsWith(".json")) return "JSON";
        if (lower.endsWith(".xml")) return "XML";
        throw new BadRequestException("Unsupported file format. Accepted formats: .xlsx, .csv, .json, .xml");
    }

    /**
     * Reads file bytes into TableData (headers + rows of strings).
     * For XML files, this returns null because XML skips Step B.
     */
    public TableData readToTable(String fileName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("Uploaded file is empty");
        }
        if (bytes.length > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 10 MB limit (actual: " + (bytes.length / (1024 * 1024)) + " MB)");
        }

        String format = detectFormat(fileName);
        TableData tableData;

        switch (format) {
            case "XLSX":
                tableData = readXlsx(bytes);
                break;
            case "CSV":
                tableData = readCsv(bytes);
                break;
            case "JSON":
                tableData = readJson(bytes);
                break;
            case "XML":
                return null; // Handled directly in XML pipeline
            default:
                throw new BadRequestException("Unsupported format: " + format);
        }

        validateLimits(tableData);
        return tableData;
    }

    private void validateLimits(TableData tableData) {
        if (tableData == null) return;

        if (tableData.getHeaders().isEmpty()) {
            throw new BadRequestException("File contains no header row or columns");
        }
        if (tableData.getHeaders().size() > MAX_COLUMNS) {
            throw new BadRequestException("File exceeds maximum of " + MAX_COLUMNS + " columns (found " + tableData.getHeaders().size() + ")");
        }
        if (tableData.getRows().isEmpty()) {
            throw new BadRequestException("File must contain at least 1 data row");
        }
        if (tableData.getRows().size() > MAX_ROWS) {
            throw new BadRequestException("File exceeds maximum of " + MAX_ROWS + " rows (found " + tableData.getRows().size() + ")");
        }
    }

    /**
     * Reads XLSX via Apache POI. First sheet only.
     */
    private TableData readXlsx(byte[] bytes) {
        try (InputStream is = new ByteArrayInputStream(bytes);
             Workbook workbook = WorkbookFactory.create(is)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("Excel workbook contains no sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum < 0) {
                throw new BadRequestException("Excel sheet is completely empty");
            }

            // Find header row (first non-empty row)
            Row headerRow = null;
            int headerRowIdx = 0;
            for (int r = 0; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row != null && !isRowBlank(row)) {
                    headerRow = row;
                    headerRowIdx = r;
                    break;
                }
            }

            if (headerRow == null) {
                throw new BadRequestException("Excel sheet contains no data or headers");
            }

            int lastCellNum = headerRow.getLastCellNum();
            if (lastCellNum <= 0) {
                throw new BadRequestException("Excel header row has no columns");
            }

            List<String> headers = new ArrayList<>();
            for (int c = 0; c < lastCellNum; c++) {
                Cell cell = headerRow.getCell(c);
                String val = cell != null ? getCellValueAsString(cell).trim() : "";
                if (val.isEmpty()) {
                    val = "Column " + (c + 1);
                }
                headers.add(val);
            }

            List<List<String>> dataRows = new ArrayList<>();
            for (int r = headerRowIdx + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row)) {
                    continue; // Skip fully blank rows
                }

                List<String> rowValues = new ArrayList<>(headers.size());
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    rowValues.add(cell != null ? getCellValueAsString(cell) : "");
                }
                dataRows.add(rowValues);
            }

            return new TableData(headers, dataRows);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
    }

    private boolean isRowBlank(Row row) {
        int lastCellNum = row.getLastCellNum();
        if (lastCellNum <= 0) return true;
        for (int c = 0; c < lastCellNum; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellValueAsString(cell).trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        switch (type) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        return cell.getDateCellValue().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(ISO_DATE_FORMATTER);
                    } catch (Exception e) {
                        return cell.getLocalDateTimeCellValue().toLocalDate().format(ISO_DATE_FORMATTER);
                    }
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * Reads CSV (RFC 4180 compliant). First row = headers. Strips UTF-8 BOM if present.
     */
    private TableData readCsv(byte[] bytes) {
        try {
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }

            List<List<String>> allRows = parseCsvString(content);
            if (allRows.isEmpty()) {
                throw new BadRequestException("CSV file is empty");
            }

            List<String> rawHeaders = allRows.get(0);
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < rawHeaders.size(); c++) {
                String h = rawHeaders.get(c).trim();
                if (h.isEmpty()) {
                    h = "Column " + (c + 1);
                }
                headers.add(h);
            }

            List<List<String>> dataRows = new ArrayList<>();
            for (int r = 1; r < allRows.size(); r++) {
                List<String> row = allRows.get(r);
                boolean allEmpty = true;
                List<String> paddedRow = new ArrayList<>(headers.size());
                for (int c = 0; c < headers.size(); c++) {
                    String val = c < row.size() ? row.get(c) : "";
                    if (!val.trim().isEmpty()) {
                        allEmpty = false;
                    }
                    paddedRow.add(val);
                }
                if (!allEmpty) {
                    dataRows.add(paddedRow);
                }
            }

            return new TableData(headers, dataRows);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to read CSV file: " + e.getMessage());
        }
    }

    private List<List<String>> parseCsvString(String text) {
        List<List<String>> records = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return records;

        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && text.charAt(i + 1) == '"') {
                        currentField.append('"');
                        i++; // skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRecord.add(currentField.toString());
                    currentField.setLength(0);
                } else if (c == '\r') {
                    if (i + 1 < len && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    currentRecord.add(currentField.toString());
                    currentField.setLength(0);
                    records.add(currentRecord);
                    currentRecord = new ArrayList<>();
                } else if (c == '\n') {
                    currentRecord.add(currentField.toString());
                    currentField.setLength(0);
                    records.add(currentRecord);
                    currentRecord = new ArrayList<>();
                } else {
                    currentField.append(c);
                }
            }
        }

        if (currentField.length() > 0 || !currentRecord.isEmpty()) {
            currentRecord.add(currentField.toString());
            records.add(currentRecord);
        }

        return records;
    }

    /**
     * Reads JSON array of flat objects. Headers = union of keys in first-seen order.
     */
    private TableData readJson(byte[] bytes) {
        try {
            JsonNode root = objectMapper.readTree(bytes);
            if (!root.isArray()) {
                throw new BadRequestException("JSON file must be an array of flat objects");
            }
            if (root.isEmpty()) {
                throw new BadRequestException("JSON array is empty");
            }

            Set<String> headerSet = new LinkedHashSet<>();
            for (JsonNode node : root) {
                if (!node.isObject()) {
                    throw new BadRequestException("Each item in JSON array must be an object");
                }
                node.fieldNames().forEachRemaining(headerSet::add);
            }

            if (headerSet.isEmpty()) {
                throw new BadRequestException("JSON objects have no properties");
            }

            List<String> headers = new ArrayList<>(headerSet);
            List<List<String>> rows = new ArrayList<>();

            for (JsonNode node : root) {
                boolean allEmpty = true;
                List<String> row = new ArrayList<>(headers.size());
                for (String header : headers) {
                    JsonNode valNode = node.get(header);
                    String val = "";
                    if (valNode != null && !valNode.isNull()) {
                        if (valNode.isObject() || valNode.isArray()) {
                            throw new BadRequestException("Nested objects or arrays are not allowed. Table must be flat.");
                        }
                        val = valNode.asText();
                        if (!val.trim().isEmpty()) {
                            allEmpty = false;
                        }
                    }
                    row.add(val);
                }
                if (!allEmpty) {
                    rows.add(row);
                }
            }

            return new TableData(headers, rows);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse JSON file: " + e.getMessage());
        }
    }
}
