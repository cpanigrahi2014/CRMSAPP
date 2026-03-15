package com.crm.campaign.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_members", indexes = {
        @Index(name = "idx_cm_campaign", columnList = "campaign_id"),
        @Index(name = "idx_cm_lead", columnList = "lead_id"),
        @Index(name = "idx_cm_tenant", columnList = "tenant_id"),
        @Index(name = "idx_cm_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignMember extends BaseEntity {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.SENT;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    public enum MemberStatus {
        SENT, RESPONDED, CONVERTED
    }
}
