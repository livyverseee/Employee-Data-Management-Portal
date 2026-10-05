package com.portal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO representing current active dataset metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatasetActiveResponse {
    private boolean exists;
    private String fileName;
    private String uploadedBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;

    private Integer recordCount;

    public static DatasetActiveResponse notFound() {
        DatasetActiveResponse res = new DatasetActiveResponse();
        res.setExists(false);
        return res;
    }
}
