package com.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO returning distinct values for every filterable column to populate frontend dropdowns.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionsResponse {
    private List<String> education = new ArrayList<>();
    private List<Integer> joiningYear = new ArrayList<>();
    private List<String> city = new ArrayList<>();
    private List<Integer> paymentTier = new ArrayList<>();
    private List<String> gender = new ArrayList<>();
    private List<String> everBenched = new ArrayList<>();
    private List<Integer> experienceInCurrentDomain = new ArrayList<>();
    private List<Integer> leaveOrNot = new ArrayList<>();
    private Integer minAge = 0;
    private Integer maxAge = 100;
}
