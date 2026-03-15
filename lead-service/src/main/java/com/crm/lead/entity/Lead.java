package com.crm.lead.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_lead_tenant", columnList = "tenant_id"),
        @Index(name = "idx_lead_status", columnList = "status"),
        @Index(name = "idx_lead_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_lead_email", columnList = "email"),
        @Index(name = "idx_lead_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "company")
    private String company;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private LeadSource source;

    @Column(name = "lead_score")
    @Builder.Default
    private Integer leadScore = 0;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "converted")
    @Builder.Default
    private boolean converted = false;

    @Column(name = "opportunity_id")
    private UUID opportunityId;

    // ── V2 fields ───────────────────────────────────────────
    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "territory")
    private String territory;

    @Column(name = "sla_due_date")
    private LocalDateTime slaDueDate;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "contact_id")
    private UUID contactId;

    public enum LeadStatus {
        NEW, CONTACTED, WORKING, QUALIFIED, UNQUALIFIED, CONVERTED, LOST
    }

    public enum LeadSource {
        WEB, PHONE, EMAIL, REFERRAL, SOCIAL_MEDIA, TRADE_SHOW, WHATSAPP, OTHER
    }
}
