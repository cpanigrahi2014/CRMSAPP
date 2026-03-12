package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_connectors")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalConnector {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String type; // DATABASE, REST_API, FILE, MESSAGE_QUEUE

    @Column(length = 500)
    private String host;

    private Integer port;

    @Column(name = "database_name")
    private String databaseName;

    @Column(name = "base_url", length = 1000)
    private String baseUrl;

    @Column(name = "connection_string", columnDefinition = "TEXT")
    private String connectionString;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "INACTIVE";

    @Builder.Default
    private boolean enabled = false;

    @Column(name = "last_test_at")
    private LocalDateTime lastTestAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
