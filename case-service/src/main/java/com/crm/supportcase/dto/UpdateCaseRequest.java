package com.crm.supportcase.dto;

import com.crm.supportcase.entity.SupportCase;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseRequest {
    private String subject;
    private String description;
    private SupportCase.CaseStatus status;
    private SupportCase.CasePriority priority;
    private UUID assignedTo;
    private String resolutionNotes;
}
