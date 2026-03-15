package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSupportWebhookResponse {

    private UUID caseId;
    private String caseNumber;
    private String casePriority;
    private String caseStatus;
    private UUID emailActivityId;
    private String message;
}
