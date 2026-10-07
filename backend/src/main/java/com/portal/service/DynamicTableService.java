package com.portal.service;

import com.portal.dto.ColumnDef;
import com.portal.dto.RecordItem;
import com.portal.dto.RecordPageResponse;
import com.portal.exception.BadRequestException;
import com.portal.exception.ResourceNotFoundException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service managing dynamic MySQL/H2 tables (ds_<datasetId>) for row-level storage.
 * Handles DDL, batch ingestion, dynamic querying with strict parameter binding,
 * validation, CRUD operations, and streaming exports.
 */
@Service
public class DynamicTableService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    /**
     * Creates table ds_<datasetId> based on inferred columns.
     */
    public void createTable(String tableName, List<ColumnDef> columns) {
        validateTableName(tableName);

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(tableName).append(" (");
        sql.append("id BIGINT AUTO_INCREMENT PRIMARY KEY");

        for (ColumnDef col : columns) {
            validateKey(col.getKey());
            sql.append(", ").append("c_").append(col.getKey()).append(" ");
            String type = col.getType();
            if ("NUMBER".equalsIgnoreCase(type)) {
                if ("INTEGER".equalsIgnoreCase(col.getNumberKind())) {
                    sql.append("BIGINT");
                } else {
                    sql.append("DOUBLE");
                }
            } else if ("DATE".equalsIgnoreCase(type)) {
                sql.append("DATE");
            } else {
                sql.append("VARCHAR(500)");
            }
        }
        sql.append(")");

        jdbcTemplate.execute(sql.toString());
    }

    /**
     * Drops table if exists safely.
     */
    public void dropTable(String tableName) {
        if (tableName == null || !tableName.matches("^ds_[0-9]+$")) return;
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
    }

    /**
     * Batch inserts rows in batches of 500.
     */
    public void batchInsertRows(String tableName, List<ColumnDef> columns, List<Map<String, String>> rows) {
        validateTableName(tableName);
        if (rows == null || rows.isEmpty()) return;

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("c_").append(columns.get(i).getKey());
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        String insertSql = sql.toString();
        int batchSize = 500;

        for (int i = 0; i < rows.size(); i += batchSize) {
            List<Map<String, String>> batch = rows.subList(i, Math.min(i + batchSize, rows.size()));
            List<Object[]> batchArgs = new ArrayList<>(batch.size());

            for (Map<String, String> row : batch) {
                Object[] args = new Object[columns.size()];
                for (int c = 0; c < columns.size(); c++) {
                    ColumnDef col = columns.get(c);
                    String val = row.get(col.getKey());
                    args[c] = convertStringToColumnValue(val, col);
                }
                batchArgs.add(args);
            }

            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
    }

    private Object convertStringToColumnValue(String val, ColumnDef col) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        val = val.trim();
        String type = col.getType();
        try {
            if ("NUMBER".equalsIgnoreCase(type)) {
                if ("INTEGER".equalsIgnoreCase(col.getNumberKind())) {
                    return Long.parseLong(val);
                } else {
                    return Double.parseDouble(val);
                }
            } else if ("DATE".equalsIgnoreCase(type)) {
                LocalDate date = LocalDate.parse(val, ISO_DATE_FORMAT);
                return Date.valueOf(date);
            } else {
                return val.length() > 500 ? val.substring(0, 500) : val;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper to build SQL WHERE clause and parameters from filter map and search query.
     */
    private static class WhereClause {
        String clause = "";
        List<Object> params = new ArrayList<>();
    }

    private WhereClause buildWhereClause(String tableName, List<ColumnDef> columns, Map<String, String> filterParams) {
        WhereClause wc = new WhereClause();
        List<String> conditions = new ArrayList<>();
        Map<String, ColumnDef> colMap = columns.stream().collect(Collectors.toMap(ColumnDef::getKey, c -> c));

        // 1. Global search: search across ALL TEXT columns with OR
        String search = filterParams.get("search");
        if (search != null && !search.trim().isEmpty()) {
            String trimmed = search.trim();
            String likePattern = "%" + escapeLike(trimmed.toLowerCase()) + "%";
            List<String> textConditions = new ArrayList<>();
            for (ColumnDef col : columns) {
                if ("TEXT".equalsIgnoreCase(col.getType())) {
                    textConditions.add("LOWER(c_" + col.getKey() + ") LIKE ?");
                    wc.params.add(likePattern);
                }
            }
            if (!textConditions.isEmpty()) {
                conditions.add("(" + String.join(" OR ", textConditions) + ")");
            }
        }

        // 2. Per-column filters
        for (Map.Entry<String, String> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (val == null || val.trim().isEmpty()) continue;
            if ("search".equals(key) || "page".equals(key) || "size".equals(key)) continue;

            String trimmedVal = val.trim();

            if (key.startsWith("eq_")) {
                String colKey = key.substring(3);
                ColumnDef col = colMap.get(colKey);
                if (col != null) {
                    conditions.add("c_" + colKey + " = ?");
                    wc.params.add(convertStringToColumnValue(trimmedVal, col));
                }
            } else if (key.startsWith("like_")) {
                String colKey = key.substring(5);
                ColumnDef col = colMap.get(colKey);
                if (col != null) {
                    conditions.add("LOWER(c_" + colKey + ") LIKE ?");
                    wc.params.add("%" + escapeLike(trimmedVal.toLowerCase()) + "%");
                }
            } else if (key.startsWith("min_")) {
                String colKey = key.substring(4);
                ColumnDef col = colMap.get(colKey);
                if (col != null && "NUMBER".equalsIgnoreCase(col.getType())) {
                    try {
                        double minNum = Double.parseDouble(trimmedVal);
                        conditions.add("c_" + colKey + " >= ?");
                        wc.params.add("INTEGER".equalsIgnoreCase(col.getNumberKind()) ? (long) minNum : minNum);
                    } catch (NumberFormatException ignored) {}
                }
            } else if (key.startsWith("max_")) {
                String colKey = key.substring(4);
                ColumnDef col = colMap.get(colKey);
                if (col != null && "NUMBER".equalsIgnoreCase(col.getType())) {
                    try {
                        double maxNum = Double.parseDouble(trimmedVal);
                        conditions.add("c_" + colKey + " <= ?");
                        wc.params.add("INTEGER".equalsIgnoreCase(col.getNumberKind()) ? (long) maxNum : maxNum);
                    } catch (NumberFormatException ignored) {}
                }
            } else if (key.startsWith("from_")) {
                String colKey = key.substring(5);
                ColumnDef col = colMap.get(colKey);
                if (col != null && "DATE".equalsIgnoreCase(col.getType())) {
                    try {
                        LocalDate d = LocalDate.parse(trimmedVal, ISO_DATE_FORMAT);
                        conditions.add("c_" + colKey + " >= ?");
                        wc.params.add(Date.valueOf(d));
                    } catch (Exception ignored) {}
                }
            } else if (key.startsWith("to_")) {
                String colKey = key.substring(3);
                ColumnDef col = colMap.get(colKey);
                if (col != null && "DATE".equalsIgnoreCase(col.getType())) {
                    try {
                        LocalDate d = LocalDate.parse(trimmedVal, ISO_DATE_FORMAT);
                        conditions.add("c_" + colKey + " <= ?");
                        wc.params.add(Date.valueOf(d));
                    } catch (Exception ignored) {}
                }
            }
        }

        if (!conditions.isEmpty()) {
            wc.clause = " WHERE " + String.join(" AND ", conditions);
        }

        return wc;
    }

    private String escapeLike(String str) {
        return str.replace("\\", "\\\\")
                  .replace("%", "\\%")
                  .replace("_", "\\_");
    }

    /**
     * Executes paginated query against table ds_<datasetId>.
     */
    public RecordPageResponse queryRecords(String tableName, List<ColumnDef> columns, Map<String, String> filterParams, int page, int size) {
        validateTableName(tableName);

        // Clamp pagination parameters
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        WhereClause wc = buildWhereClause(tableName, columns, filterParams);

        // Count total matching elements
        String countSql = "SELECT COUNT(*) FROM " + tableName + wc.clause;
        Long totalElements = jdbcTemplate.queryForObject(countSql, wc.params.toArray(), Long.class);
        if (totalElements == null) totalElements = 0L;

        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Select page
        int offset = page * size;
        StringBuilder selectSql = new StringBuilder();
        selectSql.append("SELECT id");
        for (ColumnDef col : columns) {
            selectSql.append(", c_").append(col.getKey());
        }
        selectSql.append(" FROM ").append(tableName)
                 .append(wc.clause)
                 .append(" ORDER BY id ASC LIMIT ? OFFSET ?");

        List<Object> queryParams = new ArrayList<>(wc.params);
        queryParams.add(size);
        queryParams.add(offset);

        List<RecordItem> items = jdbcTemplate.query(selectSql.toString(), queryParams.toArray(), (rs, rowNum) -> {
            long id = rs.getLong("id");
            Map<String, Object> data = new LinkedHashMap<>();
            for (ColumnDef col : columns) {
                Object val = rs.getObject("c_" + col.getKey());
                if (val instanceof Date) {
                    data.put(col.getKey(), val.toString());
                } else {
                    data.put(col.getKey(), val);
                }
            }
            return new RecordItem(id, data);
        });

        return new RecordPageResponse(items, page, size, totalElements, totalPages);
    }

    /**
     * Computes distinct filter options for CATEGORY columns and min/max for NUMBER and DATE columns.
     */
    public Map<String, Object> getFilterOptions(String tableName, List<ColumnDef> columns) {
        validateTableName(tableName);
        Map<String, Object> result = new LinkedHashMap<>();

        for (ColumnDef col : columns) {
            String key = col.getKey();
            String colName = "c_" + key;

            if ("CATEGORY".equalsIgnoreCase(col.getFilterType())) {
                String sql = "SELECT DISTINCT " + colName + " FROM " + tableName +
                             " WHERE " + colName + " IS NOT NULL AND " + colName + " != '' ORDER BY " + colName + " ASC";
                List<String> values = jdbcTemplate.queryForList(sql, String.class);
                result.put(key, values);

            } else if ("RANGE".equalsIgnoreCase(col.getFilterType()) || "DATE_RANGE".equalsIgnoreCase(col.getFilterType())) {
                String sql = "SELECT MIN(" + colName + "), MAX(" + colName + ") FROM " + tableName +
                             " WHERE " + colName + " IS NOT NULL";
                Map<String, Object> range = jdbcTemplate.query(sql, rs -> {
                    if (rs.next()) {
                        Map<String, Object> r = new LinkedHashMap<>();
                        Object minVal = rs.getObject(1);
                        Object maxVal = rs.getObject(2);
                        if ("DATE".equalsIgnoreCase(col.getType())) {
                            r.put("min", minVal != null ? minVal.toString() : null);
                            r.put("max", maxVal != null ? maxVal.toString() : null);
                        } else {
                            r.put("min", minVal);
                            r.put("max", maxVal);
                        }
                        return r;
                    }
                    return Collections.emptyMap();
                });
                if (range != null && !range.isEmpty()) {
                    result.put(key, range);
                }
            }
        }

        return result;
    }

    /**
     * Retrieves single record by ID.
     */
    public RecordItem getRecordById(String tableName, List<ColumnDef> columns, Long recordId) {
        validateTableName(tableName);
        StringBuilder sql = new StringBuilder("SELECT id");
        for (ColumnDef col : columns) {
            sql.append(", c_").append(col.getKey());
        }
        sql.append(" FROM ").append(tableName).append(" WHERE id = ?");

        List<RecordItem> list = jdbcTemplate.query(sql.toString(), new Object[]{recordId}, (rs, rowNum) -> {
            long id = rs.getLong("id");
            Map<String, Object> data = new LinkedHashMap<>();
            for (ColumnDef col : columns) {
                Object val = rs.getObject("c_" + col.getKey());
                if (val instanceof Date) {
                    data.put(col.getKey(), val.toString());
                } else {
                    data.put(col.getKey(), val);
                }
            }
            return new RecordItem(id, data);
        });

        if (list.isEmpty()) {
            throw new ResourceNotFoundException("Record with id " + recordId + " not found");
        }
        return list.get(0);
    }

    /**
     * Validates input data against schema columns.
     */
    public void validateRecordData(List<ColumnDef> columns, Map<String, Object> data) {
        if (data == null) {
            throw new BadRequestException("Record data is required");
        }

        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, ColumnDef> colMap = columns.stream().collect(Collectors.toMap(ColumnDef::getKey, c -> c));

        // Reject unknown keys
        for (String key : data.keySet()) {
            if (!colMap.containsKey(key)) {
                errors.put(key, "Unknown field: " + key);
            }
        }

        for (ColumnDef col : columns) {
            String key = col.getKey();
            Object raw = data.get(key);
            String val = raw != null ? raw.toString().trim() : "";

            if (!col.isNullable() && val.isEmpty()) {
                errors.put(key, "This field is required");
                continue;
            }

            if (!val.isEmpty()) {
                if ("NUMBER".equalsIgnoreCase(col.getType())) {
                    if ("INTEGER".equalsIgnoreCase(col.getNumberKind())) {
                        try {
                            Long.parseLong(val);
                        } catch (NumberFormatException e) {
                            errors.put(key, "Must be an integer");
                        }
                    } else {
                        try {
                            Double.parseDouble(val);
                        } catch (NumberFormatException e) {
                            errors.put(key, "Must be a valid number");
                        }
                    }
                } else if ("DATE".equalsIgnoreCase(col.getType())) {
                    try {
                        LocalDate.parse(val, ISO_DATE_FORMAT);
                    } catch (Exception e) {
                        errors.put(key, "Must be a valid date in yyyy-MM-dd format");
                    }
                } else if ("TEXT".equalsIgnoreCase(col.getType())) {
                    if (val.length() > 500) {
                        errors.put(key, "Maximum length is 500 characters");
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }

    /**
     * Inserts new record into ds_<datasetId> and returns saved RecordItem.
     */
    public RecordItem insertRecord(String tableName, List<ColumnDef> columns, Map<String, Object> data) {
        validateTableName(tableName);
        validateRecordData(columns, data);

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("c_").append(columns.get(i).getKey());
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        Object[] params = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            ColumnDef col = columns.get(i);
            Object raw = data.get(col.getKey());
            String val = raw != null ? raw.toString() : "";
            params[i] = convertStringToColumnValue(val, col);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        long newId = key != null ? key.longValue() : 0L;

        return getRecordById(tableName, columns, newId);
    }

    /**
     * Updates existing record in ds_<datasetId>.
     */
    public RecordItem updateRecord(String tableName, List<ColumnDef> columns, Long recordId, Map<String, Object> data) {
        validateTableName(tableName);
        // Check existence
        getRecordById(tableName, columns, recordId);
        validateRecordData(columns, data);

        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("c_").append(columns.get(i).getKey()).append(" = ?");
        }
        sql.append(" WHERE id = ?");

        Object[] params = new Object[columns.size() + 1];
        for (int i = 0; i < columns.size(); i++) {
            ColumnDef col = columns.get(i);
            Object raw = data.get(col.getKey());
            String val = raw != null ? raw.toString() : "";
            params[i] = convertStringToColumnValue(val, col);
        }
        params[columns.size()] = recordId;

        jdbcTemplate.update(sql.toString(), params);
        return getRecordById(tableName, columns, recordId);
    }

    /**
     * Deletes record by ID.
     */
    public void deleteRecord(String tableName, Long recordId) {
        validateTableName(tableName);
        int rows = jdbcTemplate.update("DELETE FROM " + tableName + " WHERE id = ?", recordId);
        if (rows == 0) {
            throw new ResourceNotFoundException("Record with id " + recordId + " not found");
        }
    }

    /**
     * Streams all filtered records as XML (used by GET /api/dataset/xml).
     */
    public void streamDatasetXml(String tableName, List<ColumnDef> columns, OutputStream os) throws IOException {
        validateTableName(tableName);
        Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<dataset>\n  <columns>\n");
        for (ColumnDef col : columns) {
            writer.write("    <column key=\"" + XmlConverterService.escapeXmlAttribute(col.getKey()) +
                         "\" label=\"" + XmlConverterService.escapeXmlAttribute(col.getLabel()) + "\"/>\n");
        }
        writer.write("  </columns>\n  <records>\n");

        StringBuilder sql = new StringBuilder("SELECT id");
        for (ColumnDef col : columns) {
            sql.append(", c_").append(col.getKey());
        }
        sql.append(" FROM ").append(tableName).append(" ORDER BY id ASC");

        jdbcTemplate.query(sql.toString(), rs -> {
            try {
                writer.write("    <record>\n");
                for (ColumnDef col : columns) {
                    Object val = rs.getObject("c_" + col.getKey());
                    String strVal = val != null ? val.toString() : "";
                    writer.write("      <" + col.getKey() + ">" + XmlConverterService.escapeXmlContent(strVal) + "</" + col.getKey() + ">\n");
                }
                writer.write("    </record>\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        writer.write("  </records>\n</dataset>\n");
        writer.flush();
    }

    /**
     * Streams all filtered records as Apache POI Excel SXSSFWorkbook.
     */
    public void exportExcel(String tableName, List<ColumnDef> columns, Map<String, String> filterParams, OutputStream os) throws IOException {
        validateTableName(tableName);
        WhereClause wc = buildWhereClause(tableName, columns, filterParams);

        StringBuilder sql = new StringBuilder("SELECT id");
        for (ColumnDef col : columns) {
            sql.append(", c_").append(col.getKey());
        }
        sql.append(" FROM ").append(tableName).append(wc.clause).append(" ORDER BY id ASC");

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("Employees");
            sheet.createFreezePane(0, 1);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Date cell style
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int c = 0; c < columns.size(); c++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(c);
                cell.setCellValue(columns.get(c).getLabel());
                cell.setCellStyle(headerStyle);
            }

            final int[] rowIdx = {1};
            jdbcTemplate.query(sql.toString(), wc.params.toArray(), rs -> {
                Row row = sheet.createRow(rowIdx[0]++);
                for (int c = 0; c < columns.size(); c++) {
                    ColumnDef col = columns.get(c);
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(c);
                    Object val = rs.getObject("c_" + col.getKey());

                    if (val == null) {
                        cell.setBlank();
                    } else if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else if (val instanceof Date) {
                        cell.setCellValue((Date) val);
                        cell.setCellStyle(dateStyle);
                    } else {
                        cell.setCellValue(val.toString());
                    }
                }
            });

            // Apply AutoFilter if there's at least one column
            if (!columns.isEmpty()) {
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(1, rowIdx[0] - 1), 0, columns.size() - 1));
            }

            // Set sensible column widths
            for (int c = 0; c < columns.size(); c++) {
                int labelLen = columns.get(c).getLabel().length();
                int width = Math.max(12, Math.min(30, labelLen + 4)) * 256;
                sheet.setColumnWidth(c, width);
            }

            workbook.write(os);
            os.flush();
        }
    }

    /**
     * Streams all filtered records as CSV with UTF-8 BOM.
     */
    public void exportCsv(String tableName, List<ColumnDef> columns, Map<String, String> filterParams, OutputStream os) throws IOException {
        validateTableName(tableName);
        WhereClause wc = buildWhereClause(tableName, columns, filterParams);

        StringBuilder sql = new StringBuilder("SELECT id");
        for (ColumnDef col : columns) {
            sql.append(", c_").append(col.getKey());
        }
        sql.append(" FROM ").append(tableName).append(wc.clause).append(" ORDER BY id ASC");

        // Write UTF-8 BOM
        os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);

        // Header row
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) writer.write(",");
            writer.write(escapeCsvField(columns.get(i).getLabel()));
        }
        writer.write("\r\n");

        jdbcTemplate.query(sql.toString(), wc.params.toArray(), rs -> {
            try {
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) writer.write(",");
                    Object val = rs.getObject("c_" + columns.get(i).getKey());
                    writer.write(escapeCsvField(val != null ? val.toString() : ""));
                }
                writer.write("\r\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        writer.flush();
    }

    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private void validateTableName(String tableName) {
        if (tableName == null || !tableName.matches("^ds_[0-9]+$")) {
            throw new BadRequestException("Invalid table name: " + tableName);
        }
    }

    private void validateKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new BadRequestException("Invalid column key identifier: " + key);
        }
    }

    /**
     * Custom exception for 400 Field Validation errors.
     */
    public static class FieldValidationException extends RuntimeException {
        private final Map<String, String> errors;

        public FieldValidationException(Map<String, String> errors) {
            super("Field validation failed");
            this.errors = errors;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }
}
