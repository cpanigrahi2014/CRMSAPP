package com.crm.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoLeadActionRequest {

    @NotNull(message = "Auto-lead ID is required")
    private UUID autoLeadId;

    @NotNull(message = "Action is required (APPROVED or REJECTED)")
    private String action;
}
