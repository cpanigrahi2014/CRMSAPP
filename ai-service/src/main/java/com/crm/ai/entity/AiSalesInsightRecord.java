package com.crm.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_sales_insights")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiSalesInsightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "insight_type", nullable = false)
    private String insightType; // TREND, ANOMALY, PREDICTION, RECOMMENDATION, ALERT

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "impact_area")
    private String impactArea;

    @Column(name = "severity")
    private String severity; // high, medium, low

    @Column(name = "actionable")
    @Builder.Default
    private boolean actionable = true;

    @Column(name = "related_entities", columnDefinition = "TEXT")
    private String relatedEntities; // JSON

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
