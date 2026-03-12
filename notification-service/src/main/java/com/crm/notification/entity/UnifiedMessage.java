package com.crm.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "unified_messages", indexes = {
        @Index(name = "idx_unified_tenant", columnList = "tenant_id"),
        @Index(name = "idx_unified_channel", columnList = "channel"),
        @Index(name = "idx_unified_recipient", columnList = "recipient")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnifiedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "direction", nullable = false, length = 10)
    private String direction;

    @Column(name = "sender")
    private String sender;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
