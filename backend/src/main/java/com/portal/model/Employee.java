package com.portal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing an employee in the system.
 * Table name: "employees"
 * 
 * Auto-generated Long primary key 'id'.
 * Scoped to an active dataset via 'datasetId' (indexed).
 * Unique constraint on (datasetId, employeeId).
 */
@Entity
@Table(
    name = "employees",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_dataset_employee", columnNames = {"dataset_id", "employee_id"})
    },
    indexes = {
        @Index(name = "idx_dataset_id", columnList = "dataset_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;

    @Column(name = "education", length = 50)
    private String education;

    @Column(name = "joining_year")
    private int joiningYear;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "payment_tier")
    private int paymentTier;

    @Column(name = "age")
    private int age;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "ever_benched", length = 10)
    private String everBenched;

    @Column(name = "experience_in_current_domain")
    private int experienceInCurrentDomain;

    // 1 = left company, 0 = still working
    @Column(name = "leave_or_not")
    private int leaveOrNot;
}
