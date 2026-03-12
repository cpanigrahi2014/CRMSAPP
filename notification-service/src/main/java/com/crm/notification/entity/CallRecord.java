package com.crm.notification.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "call_records", indexes = {
        @Index(name = "idx_call_tenant", columnList = "tenant_id"),
        @Index(name = "idx_call_to_number", columnList = "to_number"),
        @Index(name = "idx_call_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CallRecord extends BaseEntity {

    public enum Direction { INBOUND, OUTBOUND }
    public enum CallStatus { INITIATED, RINGING, IN_PROGRESS, COMPLETED, FAILED, NO_ANSWER, BUSY, VOICEMAIL }

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    @Builder.Default
    private Direction direction = Direction.OUTBOUND;

    @Column(name = "from_number", length = 20)
    private String fromNumber;

    @Column(name = "to_number", nullable = false, length = 20)
    private String toNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CallStatus status = CallStatus.INITIATED;

    @Column(name = "duration_seconds")
    @Builder.Default
    private Integer durationSeconds = 0;

    @Column(name = "recording_url", length = 1000)
    private String recordingUrl;

    @Column(name = "recording_duration_seconds")
    @Builder.Default
    private Integer recordingDurationSeconds = 0;

    @Column(name = "voicemail_url", length = 1000)
    private String voicemailUrl;

    @Column(name = "call_outcome", length = 50)
    private String callOutcome;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
