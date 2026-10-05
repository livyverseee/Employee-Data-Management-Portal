package com.portal.repository;

import com.portal.dto.EmployeeCriteria;
import com.portal.model.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic filtering on Employee entity.
 * Always scopes to the given active datasetId.
 * Supports:
 * - Global search (OR across lower(employeeId), lower(city), lower(education))
 * - Column-specific filters: employeeId contains, education exact, joiningYear exact,
 *   city exact, paymentTier exact, ageMin/ageMax range, gender exact, everBenched exact,
 *   experience exact, leaveOrNot exact.
 * All wildcards (% and _) are escaped.
 */
public class EmployeeSpecifications {

    public static Specification<Employee> filterEmployees(Long datasetId, EmployeeCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory scope: must belong to the active dataset
            predicates.add(builder.equal(root.get("datasetId"), datasetId));

            if (criteria == null) {
                return builder.and(predicates.toArray(new Predicate[0]));
            }

            // 2. Global search: OR across employeeId, city, education (case-insensitive contains)
            if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
                String escaped = escapeLikePattern(criteria.getSearch().trim());
                String pattern = "%" + escaped.toLowerCase() + "%";

                Predicate idMatch = builder.like(builder.lower(root.get("employeeId")), pattern, '\\');
                Predicate cityMatch = builder.like(builder.lower(root.get("city")), pattern, '\\');
                Predicate eduMatch = builder.like(builder.lower(root.get("education")), pattern, '\\');

                predicates.add(builder.or(idMatch, cityMatch, eduMatch));
            }

            // 3. Employee ID column filter (case-insensitive contains)
            if (criteria.getEmployeeId() != null && !criteria.getEmployeeId().trim().isEmpty()) {
                String escaped = escapeLikePattern(criteria.getEmployeeId().trim());
                String pattern = "%" + escaped.toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.get("employeeId")), pattern, '\\'));
            }

            // 4. Education exact match
            if (criteria.getEducation() != null && !criteria.getEducation().trim().isEmpty() && !"All".equalsIgnoreCase(criteria.getEducation().trim())) {
                predicates.add(builder.equal(root.get("education"), criteria.getEducation().trim()));
            }

            // 5. Joining Year exact match
            if (criteria.getJoiningYear() != null) {
                predicates.add(builder.equal(root.get("joiningYear"), criteria.getJoiningYear()));
            }

            // 6. City exact match
            if (criteria.getCity() != null && !criteria.getCity().trim().isEmpty() && !"All".equalsIgnoreCase(criteria.getCity().trim())) {
                predicates.add(builder.equal(root.get("city"), criteria.getCity().trim()));
            }

            // 7. Payment Tier exact match
            if (criteria.getPaymentTier() != null) {
                predicates.add(builder.equal(root.get("paymentTier"), criteria.getPaymentTier()));
            }

            // 8. Age range: minAge and maxAge
            if (criteria.getAgeMin() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("age"), criteria.getAgeMin()));
            }
            if (criteria.getAgeMax() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("age"), criteria.getAgeMax()));
            }

            // 9. Gender exact match
            if (criteria.getGender() != null && !criteria.getGender().trim().isEmpty() && !"All".equalsIgnoreCase(criteria.getGender().trim())) {
                predicates.add(builder.equal(root.get("gender"), criteria.getGender().trim()));
            }

            // 10. Ever Benched exact match
            if (criteria.getEverBenched() != null && !criteria.getEverBenched().trim().isEmpty() && !"All".equalsIgnoreCase(criteria.getEverBenched().trim())) {
                predicates.add(builder.equal(root.get("everBenched"), criteria.getEverBenched().trim()));
            }

            // 11. Experience in current domain exact match
            if (criteria.getExperienceInCurrentDomain() != null) {
                predicates.add(builder.equal(root.get("experienceInCurrentDomain"), criteria.getExperienceInCurrentDomain()));
            }

            // 12. Leave or not exact match (0 or 1)
            if (criteria.getLeaveOrNot() != null) {
                predicates.add(builder.equal(root.get("leaveOrNot"), criteria.getLeaveOrNot()));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLikePattern(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("%", "\\%")
                   .replace("_", "\\_");
    }
}
