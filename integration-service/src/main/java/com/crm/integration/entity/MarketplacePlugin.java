package com.crm.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketplace_plugins")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketplacePlugin {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    private String category;

    private String author;

    @Builder.Default
    private String version = "1.0.0";

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(columnDefinition = "TEXT")
    private String screenshots; // JSON array

    @Column(name = "download_url")
    private String downloadUrl;

    @Column(name = "documentation_url")
    private String documentationUrl;

    @Builder.Default
    private String status = "DRAFT"; // DRAFT, PUBLISHED, DEPRECATED, REMOVED

    @Builder.Default
    private String pricing = "FREE"; // FREE, PAID, FREEMIUM

    @Column(name = "price_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceAmount = BigDecimal.ZERO;

    @Column(name = "install_count")
    @Builder.Default
    private long installCount = 0;

    @Builder.Default
    private double rating = 0.0;

    @Column(name = "rating_count")
    @Builder.Default
    private int ratingCount = 0;

    @Column(name = "required_scopes", columnDefinition = "TEXT")
    private String requiredScopes; // JSON array

    @Column(name = "config_schema", columnDefinition = "TEXT")
    private String configSchema; // JSON object

    @Column(name = "is_verified")
    @Builder.Default
    private boolean isVerified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
