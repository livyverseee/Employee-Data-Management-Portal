package com.portal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing an employee in the system.
 * Table name: "employees"
 * 
 * Note: Uses @Getter, @Setter, @NoArgsConstructor, and @AllArgsConstructor 
 * instead of @Data to prevent JPA equals/hashCode issues with entity proxies.
 * Because employeeId is the @Id, saveAll() updates existing records on re-upload.
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
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
