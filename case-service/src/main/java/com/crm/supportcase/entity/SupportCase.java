package com.crm.supportcase.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cases", indexes = {
        @Index(name = "idx_case_tenant", columnList = "tenant_id"),
        @Index(name = "idx_case_status", columnList = "status"),
        @Index(name = "idx_case_priority", columnList = "priority"),
        @Index(name = "idx_case_assigned", columnList = "assigned_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportCase extends BaseEntity {

    @Column(name = "case_number", nullable = false, unique = true, length = 20)
    private String caseNumber;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private CasePriority priority = CasePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20)
    @Builder.Default
    private CaseOrigin origin = CaseOrigin.PORTAL;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "sla_due_date")
    private LocalDateTime slaDueDate;

    @Column(name = "sla_met")
    private Boolean slaMet;

    @Column(name = "escalated")
    @Builder.Default
    private boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "csat_score")
    private Integer csatScore;

    @Column(name = "csat_comment", columnDefinition = "TEXT")
    private String csatComment;

    @Column(name = "csat_sent")
    @Builder.Default
    private boolean csatSent = false;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    public enum CaseStatus {
        OPEN, IN_PROGRESS, ESCALATED, RESOLVED, CLOSED
    }

    public enum CasePriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum CaseOrigin {
        PORTAL, EMAIL, PHONE, CHAT, SOCIAL_MEDIA
    }
}
