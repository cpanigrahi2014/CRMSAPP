package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_health")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntegrationHealthRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_id", nullable = false)
    private ThirdPartyIntegration integration;

    @Column(nullable = false, length = 20)
    private String status;

    private BigDecimal uptime;

    @Column(name = "avg_response_ms")
    private Integer avgResponseMs;

    @Column(name = "success_rate")
    private BigDecimal successRate;

    @Column(name = "total_requests")
    @Builder.Default
    private long totalRequests = 0;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "alerts_count")
    @Builder.Default
    private int alertsCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
