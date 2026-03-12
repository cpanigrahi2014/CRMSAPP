package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "third_party_integrations")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThirdPartyIntegration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "INACTIVE";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "auth_type", length = 50)
    private String authType;

    @Builder.Default
    private boolean enabled = false;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
