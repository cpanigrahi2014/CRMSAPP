package com.crm.supportcase.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "routing_queues", indexes = {
        @Index(name = "idx_rq_tenant", columnList = "tenant_id"),
        @Index(name = "idx_rq_name", columnList = "name"),
        @Index(name = "idx_rq_channel", columnList = "channel")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingQueue extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private Channel channel = Channel.CASE;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_model", nullable = false, length = 30)
    @Builder.Default
    private RoutingModel routingModel = RoutingModel.LEAST_ACTIVE;

    @Column(name = "priority_weight", nullable = false)
    @Builder.Default
    private int priorityWeight = 1;

    @Column(name = "max_wait_seconds")
    @Builder.Default
    private int maxWaitSeconds = 300;

    @Column(name = "overflow_queue_id")
    private java.util.UUID overflowQueueId;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum Channel {
        CASE, CHAT, EMAIL, PHONE, SOCIAL_MEDIA, WHATSAPP
    }

    public enum RoutingModel {
        LEAST_ACTIVE,       // assign to agent with fewest open items
        ROUND_ROBIN,        // cycle through agents in order
        SKILL_BASED,        // match agent skills to work item
        PRIORITY_BASED,     // highest-priority items first, best-skilled agent
        LOAD_BALANCED       // balance by agent capacity percentage
    }
}
