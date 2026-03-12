package com.crm.lead.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_attachments", indexes = {
        @Index(name = "idx_lead_attachments_lead", columnList = "lead_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    @Builder.Default
    private Long fileSize = 0L;

    @Column(name = "file_data", columnDefinition = "BYTEA")
    @Basic(fetch = FetchType.LAZY)
    private byte[] fileData;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
