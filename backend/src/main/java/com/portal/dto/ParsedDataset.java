package com.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result of parsing XML dataset into Java objects.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDataset {
    private List<ColumnDef> columns = new ArrayList<>();
    private List<Map<String, String>> rows = new ArrayList<>();
}
