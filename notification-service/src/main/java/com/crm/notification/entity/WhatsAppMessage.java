package com.crm.notification.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_messages", indexes = {
        @Index(name = "idx_wa_tenant", columnList = "tenant_id"),
        @Index(name = "idx_wa_to_number", columnList = "to_number"),
        @Index(name = "idx_wa_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsAppMessage extends BaseEntity {

    public enum Direction { INBOUND, OUTBOUND }
    public enum WaStatus { PENDING, SENDING, SENT, DELIVERED, READ, FAILED, RECEIVED }
    public enum MessageType { TEXT, IMAGE, DOCUMENT, AUDIO, VIDEO, TEMPLATE }

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    @Builder.Default
    private Direction direction = Direction.OUTBOUND;

    @Column(name = "from_number", length = 20)
    private String fromNumber;

    @Column(name = "to_number", nullable = false, length = 20)
    private String toNumber;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    @Column(name = "media_type", length = 50)
    private String mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private WaStatus status = WaStatus.PENDING;

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

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
