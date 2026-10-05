package com.portal.repository;

import com.portal.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Employee entity.
 * Uses Long primary key and scopes operations to datasetId.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByIdAndDatasetId(Long id, Long datasetId);

    List<Employee> findByDatasetId(Long datasetId);

    boolean existsByDatasetIdAndEmployeeId(Long datasetId, String employeeId);

    boolean existsByDatasetIdAndEmployeeIdAndIdNot(Long datasetId, String employeeId, Long id);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM Employee e WHERE e.datasetId = :datasetId")
    void deleteByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT DISTINCT e.education FROM Employee e WHERE e.datasetId = :datasetId AND e.education IS NOT NULL ORDER BY e.education ASC")
    List<String> findDistinctEducationByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT DISTINCT e.joiningYear FROM Employee e WHERE e.datasetId = :datasetId ORDER BY e.joiningYear ASC")
    List<Integer> findDistinctJoiningYearByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT DISTINCT e.city FROM Employee e WHERE e.datasetId = :datasetId AND e.city IS NOT NULL ORDER BY e.city ASC")
    List<String> findDistinctCityByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT DISTINCT e.paymentTier FROM Employee e WHERE e.datasetId = :datasetId ORDER BY e.paymentTier ASC")
    List<Integer> findDistinctPaymentTierByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT DISTINCT e.gender FROM Employee e WHERE e.datasetId = :datasetId AND e.gender IS NOT NULL ORDER BY e.gender ASC")
    List<String> findDistinctGenderByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT DISTINCT e.everBenched FROM Employee e WHERE e.datasetId = :datasetId AND e.everBenched IS NOT NULL ORDER BY e.everBenched ASC")
    List<String> findDistinctEverBenchedByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT DISTINCT e.experienceInCurrentDomain FROM Employee e WHERE e.datasetId = :datasetId ORDER BY e.experienceInCurrentDomain ASC")
    List<Integer> findDistinctExperienceByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT DISTINCT e.leaveOrNot FROM Employee e WHERE e.datasetId = :datasetId ORDER BY e.leaveOrNot ASC")
    List<Integer> findDistinctLeaveOrNotByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT MIN(e.age) FROM Employee e WHERE e.datasetId = :datasetId")
    Integer findMinAgeByDatasetId(@Param("datasetId") Long datasetId);

    @Query("SELECT MAX(e.age) FROM Employee e WHERE e.datasetId = :datasetId")
    Integer findMaxAgeByDatasetId(@Param("datasetId") Long datasetId);
}
