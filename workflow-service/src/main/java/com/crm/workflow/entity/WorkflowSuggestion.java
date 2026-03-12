package com.crm.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_suggestions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "suggestion_type", nullable = false, length = 50)
    private String suggestionType; // PATTERN_DETECTED, BEST_PRACTICE, OPTIMIZATION

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "trigger_event", length = 50)
    private String triggerEvent;

    @Column(name = "conditions_json", columnDefinition = "TEXT")
    private String conditionsJson;

    @Column(name = "actions_json", columnDefinition = "TEXT")
    private String actionsJson;

    @Column(name = "canvas_layout", columnDefinition = "TEXT")
    private String canvasLayout;

    @Builder.Default
    @Column
    private Double confidence = 0.5;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "accepted_rule_id")
    private UUID acceptedRuleId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
