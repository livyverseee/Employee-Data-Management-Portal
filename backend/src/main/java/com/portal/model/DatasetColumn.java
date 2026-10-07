package com.portal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing schema metadata for a dataset column.
 * Table name: "dataset_columns"
 */
@Entity
@Table(name = "dataset_columns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatasetColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "col_key", nullable = false, length = 100)
    private String key; // sanitized key [a-z0-9_]

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // TEXT, NUMBER, DATE

    @Column(name = "number_kind", length = 20)
    private String numberKind; // INTEGER, DECIMAL, or null

    @Column(name = "filter_type", nullable = false, length = 30)
    private String filterType; // CATEGORY, TEXT_SEARCH, RANGE, DATE_RANGE

    @Column(name = "nullable", nullable = false)
    private boolean nullable;

    @Column(name = "sequential", nullable = false)
    private boolean sequential;

    @Column(name = "position", nullable = false)
    private int position;
}
