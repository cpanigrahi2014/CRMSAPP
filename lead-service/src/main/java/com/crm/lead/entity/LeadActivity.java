package com.crm.lead.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_activities", indexes = {
        @Index(name = "idx_lead_activities_lead", columnList = "lead_id"),
        @Index(name = "idx_lead_activities_type", columnList = "activity_type")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "activity_type", nullable = false, length = 30)
    private String activityType;  // STATUS_CHANGE, NOTE_ADDED, TAG_ADDED, ASSIGNED, SCORE_CHANGED, CONVERTED, EMAIL_SENT, CALL, MEETING, ATTACHMENT_ADDED, CREATED, UPDATED, etc.

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
