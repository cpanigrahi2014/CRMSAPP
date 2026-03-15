package com.crm.campaign.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class CampaignMemberResponse {
    private UUID id;
    private UUID campaignId;
    private UUID leadId;
    private String status;
    private LocalDateTime addedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime convertedAt;
    private LocalDateTime createdAt;
}
