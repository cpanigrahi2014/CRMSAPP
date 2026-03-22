package com.crm.opportunity.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pipeline_stages", indexes = {
        @Index(name = "idx_pipeline_stage_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pipeline_stage_order", columnList = "tenant_id, display_order")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_pipeline_stage_name_tenant", columnNames = {"tenant_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStage extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "color", length = 7)
    @Builder.Default
    private String color = "#1976d2";

    @Column(name = "default_probability", nullable = false)
    @Builder.Default
    private Integer defaultProbability = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "forecast_category")
    @Builder.Default
    private Opportunity.ForecastCategory forecastCategory = Opportunity.ForecastCategory.PIPELINE;

    @Column(name = "is_closed_won", nullable = false)
    @Builder.Default
    private boolean closedWon = false;

    @Column(name = "is_closed_lost", nullable = false)
    @Builder.Default
    private boolean closedLost = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
