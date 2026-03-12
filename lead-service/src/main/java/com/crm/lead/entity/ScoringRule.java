package com.crm.lead.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_scoring_rules", indexes = {
        @Index(name = "idx_scoring_rules_tenant", columnList = "tenant_id, active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScoringRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "criteria_field", nullable = false, length = 50)
    private String criteriaField;  // source, status, company, email_domain

    @Column(name = "criteria_operator", nullable = false, length = 20)
    @Builder.Default
    private String criteriaOperator = "EQUALS";

    @Column(name = "criteria_value", nullable = false)
    private String criteriaValue;

    @Column(name = "score_delta", nullable = false)
    @Builder.Default
    private Integer scoreDelta = 0;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
