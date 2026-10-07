package com.portal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing current active dataset metadata and schema columns.
 * Format for active dataset:
 * { exists:false } or
 * { exists:true, fileName, sourceFormat, uploadedBy, uploadedAt, recordCount, columns:[...] }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatasetActiveResponse {

    private boolean exists;
    private Long id;
    private String fileName;
    private String sourceFormat;
    private String tableName;
    private String uploadedBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;

    private Integer recordCount;
    private List<ColumnDef> columns;

    public static DatasetActiveResponse notFound() {
        DatasetActiveResponse res = new DatasetActiveResponse();
        res.setExists(false);
        return res;
    }
}
