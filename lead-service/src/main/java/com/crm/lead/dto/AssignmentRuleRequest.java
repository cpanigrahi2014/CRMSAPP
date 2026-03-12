package com.crm.lead.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssignmentRuleRequest {
    @NotBlank(message = "Rule name is required")
    private String name;

    @NotBlank(message = "Criteria field is required")
    private String criteriaField;   // source, company, territory, status

    private String criteriaOperator; // EQUALS, CONTAINS, STARTS_WITH, IN

    @NotBlank(message = "Criteria value is required")
    private String criteriaValue;

    private UUID assignTo;

    private String assignmentType; // DIRECT or ROUND_ROBIN
    private String roundRobinMembers; // JSON array of UUIDs

    private Integer priority;
    private Boolean active;
}
