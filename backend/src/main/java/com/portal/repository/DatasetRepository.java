package com.portal.repository;

import com.portal.model.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Dataset entity.
 */
@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    /**
     * Finds the currently active dataset.
     */
    Optional<Dataset> findByActiveTrue();

    List<Dataset> findByActive(boolean active);
}
