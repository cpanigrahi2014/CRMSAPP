package com.crm.opportunity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deal_approvals", indexes = {
        @Index(name = "idx_deal_approval_opp", columnList = "opportunity_id"),
        @Index(name = "idx_deal_approval_approver", columnList = "approver_id"),
        @Index(name = "idx_deal_approval_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealApproval {

    public enum ApprovalType { DISCOUNT, STAGE_CHANGE, CLOSE_DEAL, PRICING, CUSTOM }
    public enum ApprovalStatus { PENDING, APPROVED, REJECTED, EXPIRED }
    public enum ApprovalPriority { LOW, NORMAL, HIGH, URGENT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(name = "requested_by_id", nullable = false)
    private String requestedById;

    @Column(name = "requested_by_name")
    private String requestedByName;

    @Column(name = "approver_id", nullable = false)
    private String approverId;

    @Column(name = "approver_name")
    private String approverName;

    @Column(name = "approval_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ApprovalType approvalType;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "current_value")
    private String currentValue;

    @Column(name = "requested_value")
    private String requestedValue;

    @Column(name = "approver_comment", columnDefinition = "TEXT")
    private String approverComment;

    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalPriority priority = ApprovalPriority.NORMAL;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
