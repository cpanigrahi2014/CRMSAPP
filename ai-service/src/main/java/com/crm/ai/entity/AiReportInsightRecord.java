package com.crm.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_report_insights")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiReportInsightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "report_name", nullable = false)
    private String reportName;

    @Column(name = "insight_type", nullable = false)
    private String insightType; // TREND, ANOMALY, PREDICTION, RECOMMENDATION, ALERT

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "metric")
    private String metric;

    @Column(name = "current_value")
    private BigDecimal currentValue;

    @Column(name = "previous_value")
    private BigDecimal previousValue;

    @Column(name = "change_pct")
    private BigDecimal changePct;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
