package com.portal.repository;

import com.portal.model.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic filtering on Employee entity.
 * Supports:
 * - Free text search across employeeId, city, and education (case-insensitive contains, with escaped wildcards).
 * - Exact match on city (optional).
 * - Exact match on gender (optional).
 */
public class EmployeeSpecifications {

    /**
     * Builds a composite Specification combining search, city, and gender filters with AND.
     *
     * @param searchText text to search in employeeId, city, or education
     * @param city       exact city name (optional)
     * @param gender     exact gender (optional)
     * @return JPA Specification for Employee
     */
    public static Specification<Employee> filterEmployees(String searchText, String city, String gender) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search text filter: OR across lower(employeeId), lower(city), lower(education)
            if (searchText != null && !searchText.trim().isEmpty()) {
                String trimmed = searchText.trim();
                String escaped = escapeLikePattern(trimmed);
                String pattern = "%" + escaped.toLowerCase() + "%";

                Predicate idMatch = builder.like(builder.lower(root.get("employeeId")), pattern, '\\');
                Predicate cityMatch = builder.like(builder.lower(root.get("city")), pattern, '\\');
                Predicate eduMatch = builder.like(builder.lower(root.get("education")), pattern, '\\');

                predicates.add(builder.or(idMatch, cityMatch, eduMatch));
            }

            // 2. Exact city match filter (only if provided and not blank)
            if (city != null && !city.trim().isEmpty() && !"All".equalsIgnoreCase(city.trim())) {
                predicates.add(builder.equal(root.get("city"), city.trim()));
            }

            // 3. Exact gender match filter (only if provided and not blank)
            if (gender != null && !gender.trim().isEmpty() && !"All".equalsIgnoreCase(gender.trim())) {
                predicates.add(builder.equal(root.get("gender"), gender.trim()));
            }

            // Combine all predicates with AND
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Escapes standard SQL LIKE wildcard characters (% and _) as well as backslashes (\)
     * so user input is treated as literal text.
     *
     * @param text input query string
     * @return sanitized query string with escaped wildcards
     */
    private static String escapeLikePattern(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("%", "\\%")
                   .replace("_", "\\_");
    }
}
