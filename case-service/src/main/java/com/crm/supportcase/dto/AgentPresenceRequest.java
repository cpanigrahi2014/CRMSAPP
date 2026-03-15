package com.crm.supportcase.dto;

import com.crm.supportcase.entity.AgentPresence;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPresenceRequest {
    private UUID userId;
    private String agentName;
    private String agentEmail;
    private AgentPresence.PresenceStatus status;
    private UUID queueId;
    private Integer capacity;
    private Boolean autoAccept;
}
