package com.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filter criteria DTO bundling all optional search and column-level filter parameters.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCriteria {
    private String search;
    private String employeeId;
    private String education;
    private Integer joiningYear;
    private String city;
    private Integer paymentTier;
    private Integer ageMin;
    private Integer ageMax;
    private String gender;
    private String everBenched;
    private Integer experienceInCurrentDomain;
    private Integer leaveOrNot;
}
