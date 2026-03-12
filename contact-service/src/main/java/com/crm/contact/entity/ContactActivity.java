package com.crm.contact.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_activities", indexes = {
        @Index(name = "idx_ca_contact", columnList = "contact_id"),
        @Index(name = "idx_ca_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ca_date", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;      // CREATED, UPDATED, EMAIL_SENT, CALL_LOGGED, TAG_ADDED, CONSENT_CHANGED, etc.

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;          // JSON extra data

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
