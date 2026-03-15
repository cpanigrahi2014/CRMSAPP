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
public class SocialMediaWebhookResponse {

    private UUID leadId;
    private String leadSource;
    private Integer leadScore;
    private String leadStatus;
    private String message;
}
