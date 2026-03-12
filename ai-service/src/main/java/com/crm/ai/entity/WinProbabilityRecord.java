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
@Table(name = "win_probability_records")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WinProbabilityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "opportunity_id", nullable = false)
    private String opportunityId;

    @Column(name = "opportunity_name")
    private String opportunityName;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "stage")
    private String stage;

    @Column(name = "win_probability", nullable = false)
    private int winProbability;

    @Column(name = "historical_win_rate")
    private int historicalWinRate;

    @Column(name = "days_in_stage")
    private int daysInStage;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors; // JSON array

    @Column(name = "positive_signals", columnDefinition = "TEXT")
    private String positiveSignals; // JSON array

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
