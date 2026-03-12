package com.crm.notification.dto;

import com.crm.notification.entity.CallRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallRecordResponse {
    private UUID id;
    private String fromNumber;
    private String toNumber;
    private CallRecord.Direction direction;
    private CallRecord.CallStatus status;
    private Integer durationSeconds;
    private String recordingUrl;
    private Integer recordingDurationSeconds;
    private String voicemailUrl;
    private String callOutcome;
    private String notes;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private LocalDateTime startedAt;
    private LocalDateTime answeredAt;
    private LocalDateTime endedAt;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
