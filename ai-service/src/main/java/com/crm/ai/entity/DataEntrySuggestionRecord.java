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
@Table(name = "data_entry_suggestions")
@EntityListeners(AuditingEntityListener.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DataEntrySuggestionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "entity_name")
    private String entityName;

    @Column(name = "field", nullable = false)
    private String field;

    @Column(name = "current_value")
    private String currentValue;

    @Column(name = "suggested_value", nullable = false)
    private String suggestedValue;

    @Column(name = "confidence", nullable = false)
    private BigDecimal confidence;

    @Column(name = "source")
    private String source;

    @Column(name = "accepted")
    private Boolean accepted;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
