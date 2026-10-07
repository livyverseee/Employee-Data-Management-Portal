package com.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Intermediate in-memory representation of a flat table extracted from uploaded files.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableData {
    private List<String> headers = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
}
