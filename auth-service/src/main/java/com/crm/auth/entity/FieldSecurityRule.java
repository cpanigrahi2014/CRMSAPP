package com.crm.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "field_security_rules", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"entity_type", "field_name", "role_name", "tenant_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FieldSecurityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "access_level", nullable = false, length = 20)
    @Builder.Default
    private String accessLevel = "READ_WRITE"; // HIDDEN, READ_ONLY, READ_WRITE

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
