package com.portal.dto;

import com.portal.model.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Standard pagination response wrapper for employee list queries.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePageResponse {
    private List<Employee> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
