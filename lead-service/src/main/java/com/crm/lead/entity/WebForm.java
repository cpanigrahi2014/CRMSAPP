package com.crm.lead.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_web_forms", indexes = {
        @Index(name = "idx_web_forms_tenant", columnList = "tenant_id, active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebForm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "fields", columnDefinition = "JSONB")
    @Builder.Default
    private String fields = "[\"firstName\",\"lastName\",\"email\",\"phone\",\"company\"]";

    @Column(name = "source", length = 30)
    @Builder.Default
    private String source = "WEB";

    @Column(name = "assign_to")
    private UUID assignTo;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
