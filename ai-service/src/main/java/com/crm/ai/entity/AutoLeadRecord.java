package com.crm.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auto_lead_records")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AutoLeadRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "source_type", nullable = false)
    private String sourceType; // EMAIL, MEETING

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "lead_name", nullable = false)
    private String leadName;

    @Column(name = "email")
    private String email;

    @Column(name = "company")
    private String company;

    @Column(name = "title")
    private String title;

    @Column(name = "phone")
    private String phone;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "confidence", nullable = false)
    @Builder.Default
    private BigDecimal confidence = BigDecimal.ZERO;

    @Column(name = "status")
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, CREATED

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
