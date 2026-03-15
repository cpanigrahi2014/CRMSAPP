package com.crm.supportcase.dto;

import com.crm.supportcase.entity.SupportCase;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseResponse {
    private UUID id;
    private String caseNumber;
    private String subject;
    private String description;
    private SupportCase.CaseStatus status;
    private SupportCase.CasePriority priority;
    private SupportCase.CaseOrigin origin;
    private String contactName;
    private String contactEmail;
    private String accountName;
    private UUID contactId;
    private UUID accountId;
    private UUID assignedTo;
    private LocalDateTime slaDueDate;
    private Boolean slaMet;
    private boolean escalated;
    private LocalDateTime escalatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime firstResponseAt;
    private Integer csatScore;
    private String csatComment;
    private boolean csatSent;
    private String resolutionNotes;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
