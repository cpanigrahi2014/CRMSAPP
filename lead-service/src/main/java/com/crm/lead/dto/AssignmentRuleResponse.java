package com.crm.lead.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssignmentRuleResponse {
    private UUID id;
    private String name;
    private String criteriaField;
    private String criteriaOperator;
    private String criteriaValue;
    private UUID assignTo;
    private String assignmentType;
    private String roundRobinMembers;
    private Integer priority;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
