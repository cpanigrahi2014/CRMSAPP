package com.crm.contact.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_communications", indexes = {
        @Index(name = "idx_cc_contact", columnList = "contact_id"),
        @Index(name = "idx_cc_tenant", columnList = "tenant_id"),
        @Index(name = "idx_cc_date", columnList = "communication_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "comm_type", nullable = false, length = 30)
    private String commType;          // EMAIL, CALL, MEETING, NOTE, SMS

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "direction", nullable = false, length = 20)
    private String direction;         // INBOUND, OUTBOUND

    @Column(name = "status", length = 30)
    private String status;            // COMPLETED, PENDING, MISSED, SCHEDULED

    @Column(name = "communication_date", nullable = false)
    private LocalDateTime communicationDate;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
