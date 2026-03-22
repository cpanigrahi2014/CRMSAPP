package com.crm.contact.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "contact_notes", indexes = {
        @Index(name = "idx_contact_notes_contact", columnList = "contact_id"),
        @Index(name = "idx_contact_notes_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactNote extends BaseEntity {

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
