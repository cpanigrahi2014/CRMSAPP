package com.crm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvFieldDetectionRequest {

    @NotBlank(message = "CSV content is required")
    private String csvContent;

    @NotBlank(message = "Entity type is required (e.g. account, contact, lead)")
    private String entityType;
}
