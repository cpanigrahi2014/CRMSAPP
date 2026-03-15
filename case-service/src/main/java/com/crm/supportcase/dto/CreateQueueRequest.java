package com.crm.supportcase.dto;

import com.crm.supportcase.entity.RoutingQueue;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueueRequest {

    @NotBlank(message = "Queue name is required")
    private String name;

    private String description;
    private RoutingQueue.Channel channel;
    private RoutingQueue.RoutingModel routingModel;
    private Integer priorityWeight;
    private Integer maxWaitSeconds;
    private UUID overflowQueueId;
}
