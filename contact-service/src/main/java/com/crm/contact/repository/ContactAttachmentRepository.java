package com.crm.contact.repository;

import com.crm.contact.entity.ContactAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactAttachmentRepository extends JpaRepository<ContactAttachment, UUID> {

    List<ContactAttachment> findByContactIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(UUID contactId, String tenantId);

    Optional<ContactAttachment> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    List<ContactAttachment> findByContactIdAndDeletedFalse(UUID contactId);
}
