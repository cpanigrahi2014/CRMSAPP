package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "embeddable_widgets")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmbeddableWidget {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "widget_type", nullable = false)
    @Builder.Default
    private String widgetType = "CHART"; // FORM, TABLE, CHART, METRIC, TIMELINE, CUSTOM

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON object

    @Column(name = "embed_token", unique = true, nullable = false)
    private String embedToken;

    @Column(name = "allowed_domains", columnDefinition = "TEXT")
    private String allowedDomains; // JSON array

    @Builder.Default
    private boolean active = true;

    @Column(name = "view_count")
    @Builder.Default
    private long viewCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
