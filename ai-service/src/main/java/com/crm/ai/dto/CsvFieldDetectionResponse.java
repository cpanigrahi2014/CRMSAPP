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
    }
}
