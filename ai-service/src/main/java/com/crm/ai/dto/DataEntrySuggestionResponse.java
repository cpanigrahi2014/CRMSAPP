package com.crm.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataEntrySuggestionResponse {
    private UUID id;
    private String entityType;
    private String entityId;
    private String entityName;
    private String field;
    private String currentValue;
    private String suggestedValue;
    private BigDecimal confidence;
    private String source;
    private Boolean accepted;
    private LocalDateTime createdAt;
}
