package com.crm.supportcase.dto;

import com.crm.supportcase.entity.RoutingRule;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRuleResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID queueId;
    private RoutingRule.MatchField matchField;
    private RoutingRule.MatchOperator matchOperator;
    private String matchValue;
    private String requiredSkill;
    private int minProficiency;
    private int rulePriority;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
