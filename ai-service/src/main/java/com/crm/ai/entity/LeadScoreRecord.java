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
@Table(name = "lead_score_records")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadScoreRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "lead_id", nullable = false)
    private String leadId;

    @Column(name = "lead_name")
    private String leadName;

    @Column(name = "email")
    private String email;

    @Column(name = "company")
    private String company;

    @Column(name = "current_score")
    private int currentScore;

    @Column(name = "predicted_score")
    private int predictedScore;

    @Column(name = "trend")
    private String trend; // RISING, DECLINING, STABLE

    @Column(name = "conversion_probability")
    private BigDecimal conversionProbability;

    @Column(name = "top_factors", columnDefinition = "TEXT")
    private String topFactors; // JSON array

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
