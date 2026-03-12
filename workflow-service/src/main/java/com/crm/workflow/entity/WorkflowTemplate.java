package com.crm.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "trigger_event", nullable = false, length = 50)
    private String triggerEvent;

    @Column(name = "conditions_json", columnDefinition = "TEXT")
    private String conditionsJson;

    @Column(name = "actions_json", columnDefinition = "TEXT")
    private String actionsJson;

    @Column(name = "canvas_layout", columnDefinition = "TEXT")
    private String canvasLayout;

    @Builder.Default
    @Column
    private Integer popularity = 0;

    @Builder.Default
    @Column(name = "is_system")
    private Boolean isSystem = false;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
