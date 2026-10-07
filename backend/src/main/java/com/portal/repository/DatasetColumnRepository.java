package com.portal.repository;

import com.portal.model.DatasetColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DatasetColumnRepository extends JpaRepository<DatasetColumn, Long> {

    List<DatasetColumn> findByDatasetIdOrderByPositionAsc(Long datasetId);

    @Modifying
    @Transactional
    void deleteByDatasetId(Long datasetId);
}
