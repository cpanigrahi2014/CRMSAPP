package com.crm.notification.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sms_messages", indexes = {
        @Index(name = "idx_sms_tenant", columnList = "tenant_id"),
        @Index(name = "idx_sms_to_number", columnList = "to_number"),
        @Index(name = "idx_sms_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmsMessage extends BaseEntity {

    public enum Direction { INBOUND, OUTBOUND }
    public enum SmsStatus { PENDING, SENDING, SENT, DELIVERED, FAILED, RECEIVED }

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    @Builder.Default
    private Direction direction = Direction.OUTBOUND;

    @Column(name = "from_number", length = 20)
    private String fromNumber;

    @Column(name = "to_number", nullable = false, length = 20)
    private String toNumber;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SmsStatus status = SmsStatus.PENDING;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}
