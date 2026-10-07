package com.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordPageResponse {
    private List<RecordItem> content = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
