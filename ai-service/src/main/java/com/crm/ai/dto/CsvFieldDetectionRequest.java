package com.crm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvFieldDetectionRequest {

    @NotBlank(message = "CSV content is required")
    private String csvContent;

    @NotBlank(message = "Entity type is required (e.g. account, contact, lead)")
    private String entityType;

    /** Optional industry for industry-specific field suggestions (e.g. "Real Estate", "Healthcare") */
    private String industry;

    /** Optional list of custom field names (from AI Config) to include in detection */
    private List<String> customFields;
}
