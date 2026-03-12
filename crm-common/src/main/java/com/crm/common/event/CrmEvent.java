package com.crm.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmEvent {
    private String eventId;
    private String eventType;
    private String tenantId;
    private String userId;
    private String entityType;
    private String entityId;
    private Object payload;
    private LocalDateTime timestamp;
}
