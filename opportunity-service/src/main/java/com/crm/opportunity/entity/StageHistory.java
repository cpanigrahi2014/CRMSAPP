package com.crm.opportunity.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stage_history", indexes = {
        @Index(name = "idx_stage_history_opportunity", columnList = "opportunity_id"),
        @Index(name = "idx_stage_history_tenant", columnList = "tenant_id"),
        @Index(name = "idx_stage_history_to_stage", columnList = "to_stage"),
        @Index(name = "idx_stage_history_changed_at", columnList = "changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageHistory extends BaseEntity {

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(name = "from_stage", length = 50)
    private String fromStage;

    @Column(name = "to_stage", nullable = false, length = 50)
    private String toStage;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(name = "time_in_stage")
    private Long timeInStage; // seconds spent in previous stage
}
