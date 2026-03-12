package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_endpoints")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiEndpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "auth_required")
    @Builder.Default
    private boolean authRequired = true;

    @Column(name = "rate_limit")
    @Builder.Default
    private int rateLimit = 100;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private String version = "v1";

    @Column(name = "total_calls")
    @Builder.Default
    private long totalCalls = 0;

    @Column(name = "last_called_at")
    private LocalDateTime lastCalledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
