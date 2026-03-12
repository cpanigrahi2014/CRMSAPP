package com.crm.notification.dto;

import com.crm.notification.entity.CallRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCallRequest {
    private CallRecord.CallStatus status;
    private String callOutcome;
    private String notes;
    private String recordingUrl;
    private Integer recordingDurationSeconds;
}
