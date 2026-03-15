package com.crm.supportcase.dto;

import com.crm.supportcase.entity.SupportCase;
import com.crm.supportcase.entity.WorkItem;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private UUID queueId;
    private UUID assignedAgentId;
    private WorkItem.WorkItemStatus status;
    private SupportCase.CasePriority priority;
    private String channel;
    private String subject;
    private LocalDateTime queuedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private int declinedCount;
    private Long waitTimeSeconds;
    private Long handleTimeSeconds;
    private LocalDateTime createdAt;
}
