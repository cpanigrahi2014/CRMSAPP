package com.crm.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowTemplateResponse {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private String entityType;
    private String triggerEvent;
    private String conditionsJson;
    private String actionsJson;
    private String canvasLayout;
    private Integer popularity;
    private Boolean isSystem;
    private LocalDateTime createdAt;
}
