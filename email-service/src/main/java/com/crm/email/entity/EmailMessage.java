package com.crm.email.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_messages")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EmailMessage extends BaseEntity {

    public enum Direction { INBOUND, OUTBOUND }
    public enum Status { DRAFT, QUEUED, SENDING, SENT, DELIVERED, FAILED, RECEIVED }

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "from_address", nullable = false)
    private String fromAddress;

    @Column(name = "to_addresses", nullable = false, length = 2000)
    private String toAddresses;

    @Column(name = "cc_addresses", length = 2000)
    private String ccAddresses;

    @Column(name = "bcc_addresses", length = 2000)
    private String bccAddresses;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "in_reply_to")
    private String inReplyTo;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "has_attachments", nullable = false)
    @Builder.Default
    private boolean hasAttachments = false;

    @Column(name = "opened", nullable = false)
    @Builder.Default
    private boolean opened = false;

    @Column(name = "open_count", nullable = false)
    @Builder.Default
    private int openCount = 0;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private int clickCount = 0;

    @Column(name = "first_opened_at")
    private LocalDateTime firstOpenedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
