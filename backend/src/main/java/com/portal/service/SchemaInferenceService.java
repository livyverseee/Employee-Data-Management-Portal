package com.portal.service;

import com.portal.dto.ColumnDef;
import com.portal.dto.ParsedDataset;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Step D: Infers column data types, filter characteristics, nullability, and sequential properties
 * based on all data rows in the parsed dataset.
 * 
 * Rules:
 * - NUMBER: every non-empty value is numeric (INTEGER if all integers, else DECIMAL)
 * - DATE: every non-empty value matches yyyy-MM-dd
 * - TEXT: otherwise
 * - nullable: true if any row has an empty/null value
 * - filterType:
 *     NUMBER -> RANGE
 *     DATE -> DATE_RANGE
 *     TEXT -> CATEGORY if distinct <= 20 and distinct * 2 <= rowCount, else TEXT_SEARCH
 * - sequential: true if integer column holds exactly 1..N with no gaps
 */
@Service
public class SchemaInferenceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<ColumnDef> inferSchema(ParsedDataset parsedDataset) {
        List<ColumnDef> columns = parsedDataset.getColumns();
        List<Map<String, String>> rows = parsedDataset.getRows();
        int rowCount = rows.size();

        for (int i = 0; i < columns.size(); i++) {
            ColumnDef col = columns.get(i);
            col.setPosition(i);
            inferColumnProperties(col, rows, rowCount);
        }

        return columns;
    }

    private void inferColumnProperties(ColumnDef col, List<Map<String, String>> rows, int rowCount) {
        String key = col.getKey();
        boolean hasBlanks = false;
        boolean allNumeric = true;
        boolean allInteger = true;
        boolean allDate = true;
        int nonBlankCount = 0;

        Set<String> distinctValues = new HashSet<>();
        List<Long> integerValues = new ArrayList<>();

        for (Map<String, String> row : rows) {
            String raw = row.get(key);
            String val = raw != null ? raw.trim() : "";

            if (val.isEmpty()) {
                hasBlanks = true;
                continue;
            }

            nonBlankCount++;
            distinctValues.add(val);

            // Test Numeric
            if (allNumeric) {
                if (isInteger(val)) {
                    try {
                        long num = Long.parseLong(val);
                        integerValues.add(num);
                    } catch (NumberFormatException e) {
                        allInteger = false;
                    }
                } else if (isDecimal(val)) {
                    allInteger = false;
                } else {
                    allNumeric = false;
                    allInteger = false;
                }
            }

            // Test Date
            if (allDate) {
                if (!isValidIsoDate(val)) {
                    allDate = false;
                }
            }
        }

        col.setNullable(hasBlanks || nonBlankCount == 0);

        // Assign Type
        if (nonBlankCount > 0 && allNumeric) {
            col.setType("NUMBER");
            col.setNumberKind(allInteger ? "INTEGER" : "DECIMAL");
            col.setFilterType("RANGE");

            // Check if sequential: exactly 1..N with no gaps
            if (allInteger && !hasBlanks && integerValues.size() == rowCount) {
                boolean seq = true;
                TreeSet<Long> sortedVals = new TreeSet<>(integerValues);
                if (sortedVals.size() == rowCount && sortedVals.first() == 1L && sortedVals.last() == (long) rowCount) {
                    long expected = 1;
                    for (Long v : sortedVals) {
                        if (v != expected++) {
                            seq = false;
                            break;
                        }
                    }
                } else {
                    seq = false;
                }
                col.setSequential(seq);
            } else {
                col.setSequential(false);
            }

        } else if (nonBlankCount > 0 && allDate) {
            col.setType("DATE");
            col.setNumberKind(null);
            col.setFilterType("DATE_RANGE");
            col.setSequential(false);

        } else {
            col.setType("TEXT");
            col.setNumberKind(null);
            col.setSequential(false);

            int distinctCount = distinctValues.size();
            if (distinctCount <= 20 && (distinctCount * 2 <= rowCount)) {
                col.setFilterType("CATEGORY");
            } else {
                col.setFilterType("TEXT_SEARCH");
            }
        }
    }

    private boolean isInteger(String val) {
        if (val == null || val.isEmpty()) return false;
        try {
            Long.parseLong(val);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDecimal(String val) {
        if (val == null || val.isEmpty()) return false;
        try {
            Double.parseDouble(val);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidIsoDate(String val) {
        if (val == null || val.length() != 10) return false;
        try {
            LocalDate.parse(val, DATE_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
