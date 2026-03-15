package com.crm.supportcase.dto;

import com.crm.supportcase.entity.AgentPresence;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPresenceResponse {
    private UUID id;
    private UUID userId;
    private String agentName;
    private String agentEmail;
    private AgentPresence.PresenceStatus status;
    private UUID queueId;
    private int capacity;
    private int activeWorkCount;
    private double utilization;
    private boolean available;
    private LocalDateTime lastRoutedAt;
    private LocalDateTime statusChangedAt;
    private boolean autoAccept;
    private LocalDateTime createdAt;
}
