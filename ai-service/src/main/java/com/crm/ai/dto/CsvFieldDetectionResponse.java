package com.crm.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvFieldDetectionResponse {

    private String entityType;
    private List<FieldMapping> fieldMappings;
    private List<String> unmappedColumns;
    private int totalColumns;
    private int mappedColumns;

    /** Industry used for field detection (null if none) */
    private String industry;

    /** Industry-specific fields available for this entity type */
    private List<IndustryFieldInfo> industryFields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldMapping {
        private String csvHeader;
        private String crmField;
        private String dataType;
        private double confidence;
        private String sampleValue;
        /** True if this field is from an industry template */
        @Builder.Default
        private boolean isIndustryField = false;
        /** True if this field is a custom field created via AI Config */
        @Builder.Default
        private boolean isCustomField = false;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndustryFieldInfo {
        private String fieldName;
        private String label;
        private String fieldType;
        /** Whether this field is required for the industry */
        private boolean required;
    }
}
