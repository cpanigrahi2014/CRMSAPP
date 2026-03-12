package com.crm.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sales_forecast_records")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesForecastRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "period", nullable = false)
    private String period;

    @Column(name = "period_label")
    private String periodLabel;

    @Column(name = "predicted_revenue")
    private BigDecimal predictedRevenue;

    @Column(name = "best_case")
    private BigDecimal bestCase;

    @Column(name = "worst_case")
    private BigDecimal worstCase;

    @Column(name = "confidence")
    private String confidence; // HIGH, MEDIUM, LOW

    @Column(name = "pipeline_value")
    private BigDecimal pipelineValue;

    @Column(name = "weighted_pipeline")
    private BigDecimal weightedPipeline;

    @Column(name = "closed_to_date")
    private BigDecimal closedToDate;

    @Column(name = "quota")
    private BigDecimal quota;

    @Column(name = "attainment_pct")
    private int attainmentPct;

    @Column(name = "factors", columnDefinition = "TEXT")
    private String factors; // JSON array

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
