package com.crm.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowSuggestionResponse {
    private UUID id;
    private String title;
    private String description;
    private String suggestionType;
    private String entityType;
    private String triggerEvent;
    private String conditionsJson;
    private String actionsJson;
    private Double confidence;
    private String reason;
    private String status;
    private UUID acceptedRuleId;
    private LocalDateTime createdAt;
}
