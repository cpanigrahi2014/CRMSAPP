package com.crm.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sso_providers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "tenant_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SsoProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType; // SAML, OIDC, OAUTH2

    @Column(name = "client_id", nullable = false, length = 500)
    private String clientId;

    @Column(name = "issuer_url", length = 1000)
    private String issuerUrl;

    @Column(name = "metadata_url", length = 1000)
    private String metadataUrl;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "auto_provision", nullable = false)
    @Builder.Default
    private boolean autoProvision = false;

    @Column(name = "default_role", length = 50)
    @Builder.Default
    private String defaultRole = "USER";

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
