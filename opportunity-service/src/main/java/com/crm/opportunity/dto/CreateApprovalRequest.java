package com.crm.opportunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApprovalRequest {

    @NotNull
    private UUID opportunityId;

    @NotBlank
    private String approverId;

    private String approverName;

    @NotBlank
    private String approvalType;

    @NotBlank
    private String title;

    private String description;

    private String currentValue;

    private String requestedValue;

    @Builder.Default
    private String priority = "NORMAL";

    private LocalDateTime dueDate;
}
