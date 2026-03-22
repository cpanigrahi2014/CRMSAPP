package com.crm.contact.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "contact_attachments", indexes = {
        @Index(name = "idx_contact_attach_contact", columnList = "contact_id"),
        @Index(name = "idx_contact_attach_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactAttachment extends BaseEntity {

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 100)
    private String fileType;
}
