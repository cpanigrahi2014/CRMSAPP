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
@Table(name = "churn_prediction_records")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChurnPredictionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "industry")
    private String industry;

    @Column(name = "annual_revenue")
    private BigDecimal annualRevenue;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(name = "churn_probability", nullable = false)
    private BigDecimal churnProbability;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors; // JSON array

    @Column(name = "last_activity_days")
    private int lastActivityDays;

    @Column(name = "health_score")
    private int healthScore;

    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions; // JSON array

    @Column(name = "predicted_churn_date")
    private LocalDateTime predictedChurnDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
