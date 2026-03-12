package com.crm.opportunity.dto;

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
public class DealApprovalResponse {
    private UUID id;
    private UUID opportunityId;
    private String requestedById;
    private String requestedByName;
    private String approverId;
    private String approverName;
    private String approvalType;
    private String status;
    private String title;
    private String description;
    private String currentValue;
    private String requestedValue;
    private String approverComment;
    private String priority;
    private LocalDateTime dueDate;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
