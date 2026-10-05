package com.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for XML file upload result.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String message;
    private int recordsSaved;
}
