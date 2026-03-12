package com.crm.email.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_schedules")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EmailSchedule extends BaseEntity {

    public enum ScheduleStatus { PENDING, SENT, CANCELLED, FAILED }

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "to_addresses", nullable = false, length = 2000)
    private String toAddresses;

    @Column(name = "cc_addresses", length = 2000)
    private String ccAddresses;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
