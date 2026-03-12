package com.crm.contact.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contact_id", "tag_name"}),
        indexes = {
                @Index(name = "idx_ct_contact", columnList = "contact_id"),
                @Index(name = "idx_ct_tenant", columnList = "tenant_id"),
                @Index(name = "idx_ct_name", columnList = "tag_name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
