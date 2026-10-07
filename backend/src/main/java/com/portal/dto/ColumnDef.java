package com.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Definition of a dataset column including inferred types and filter characteristics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDef {
    private String key;
    private String label;
    private String type;         // TEXT, NUMBER, DATE
    private String numberKind;   // INTEGER, DECIMAL, or null
    private String filterType;   // CATEGORY, TEXT_SEARCH, RANGE, DATE_RANGE
    private boolean nullable;
    private boolean sequential;
    private int position;

    public ColumnDef(String key, String label) {
        this.key = key;
        this.label = label;
    }
}
