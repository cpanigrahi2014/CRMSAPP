package com.crm.supportcase.dto;

import com.crm.supportcase.entity.RoutingRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoutingRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotNull(message = "Queue ID is required")
    private UUID queueId;

    @NotNull(message = "Match field is required")
    private RoutingRule.MatchField matchField;

    private RoutingRule.MatchOperator matchOperator;

    @NotBlank(message = "Match value is required")
    private String matchValue;

    private String requiredSkill;
    private Integer minProficiency;
    private Integer rulePriority;
}
