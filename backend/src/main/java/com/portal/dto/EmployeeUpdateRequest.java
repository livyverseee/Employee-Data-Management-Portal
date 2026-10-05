package com.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for updating an existing employee record (DEAN only).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUpdateRequest {
    private String employeeId;
    private String education;
    private Integer joiningYear;
    private String city;
    private Integer paymentTier;
    private Integer age;
    private String gender;
    private String everBenched;
    private Integer experienceInCurrentDomain;
    private Integer leaveOrNot;
}
