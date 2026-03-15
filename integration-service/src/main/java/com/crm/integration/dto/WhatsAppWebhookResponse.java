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
public class WhatsAppWebhookResponse {

    private UUID leadId;
    private String leadStatus;
    private Integer leadScore;
    private UUID opportunityId;
    private String opportunityStage;
    private UUID transcriptActivityId;
    private String message;
}
