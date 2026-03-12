package com.crm.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting_summary_records")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeetingSummaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "meeting_title", nullable = false)
    private String meetingTitle;

    @Column(name = "meeting_date")
    private LocalDateTime meetingDate;

    @Column(name = "participants", columnDefinition = "TEXT")
    private String participants; // JSON array

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems; // JSON array

    @Column(name = "key_decisions", columnDefinition = "TEXT")
    private String keyDecisions; // JSON array

    @Column(name = "crm_updates", columnDefinition = "TEXT")
    private String crmUpdates; // JSON array

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private String relatedEntityId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
