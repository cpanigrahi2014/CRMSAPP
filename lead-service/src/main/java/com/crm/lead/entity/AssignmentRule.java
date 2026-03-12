package com.crm.lead.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_assignment_rules", indexes = {
        @Index(name = "idx_assignment_rules_tenant", columnList = "tenant_id, active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignmentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "criteria_field", nullable = false, length = 50)
    private String criteriaField;  // source, company, territory, status

    @Column(name = "criteria_operator", nullable = false, length = 20)
    @Builder.Default
    private String criteriaOperator = "EQUALS";  // EQUALS, CONTAINS, STARTS_WITH, IN

    @Column(name = "criteria_value", nullable = false)
    private String criteriaValue;

    @Column(name = "assign_to")
    private UUID assignTo;

    @Column(name = "assignment_type", length = 30)
    @Builder.Default
    private String assignmentType = "DIRECT"; // DIRECT, ROUND_ROBIN

    @Column(name = "round_robin_members", columnDefinition = "TEXT")
    private String roundRobinMembers; // JSON array of user UUIDs

    @Column(name = "round_robin_index")
    @Builder.Default
    private Integer roundRobinIndex = 0;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

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
