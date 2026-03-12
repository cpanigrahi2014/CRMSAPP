package com.crm.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadScoreRequest {

    @NotNull(message = "Lead ID is required")
    private UUID leadId;

    @NotNull(message = "Lead data is required")
    private Map<String, Object> leadData;
}
