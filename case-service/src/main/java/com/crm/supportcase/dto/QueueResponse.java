package com.crm.supportcase.dto;

import com.crm.supportcase.entity.RoutingQueue;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueResponse {
    private UUID id;
    private String name;
    private String description;
    private RoutingQueue.Channel channel;
    private RoutingQueue.RoutingModel routingModel;
    private int priorityWeight;
    private int maxWaitSeconds;
    private UUID overflowQueueId;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
