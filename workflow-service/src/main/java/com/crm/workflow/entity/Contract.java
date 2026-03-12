package com.crm.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(name = "proposal_id")
    private UUID proposalId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(length = 30)
    private String status = "DRAFT";

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "signer_name")
    private String signerName;

    @Column(name = "signer_email")
    private String signerEmail;

    @Column(name = "signer_ip", length = 45)
    private String signerIp;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column
    private Integer version = 1;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column
    private Boolean deleted = false;
}
