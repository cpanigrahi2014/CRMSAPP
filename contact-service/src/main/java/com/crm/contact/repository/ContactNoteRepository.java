package com.crm.contact.repository;

import com.crm.contact.entity.ContactNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactNoteRepository extends JpaRepository<ContactNote, UUID> {

    List<ContactNote> findByContactIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(UUID contactId, String tenantId);

    Optional<ContactNote> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    List<ContactNote> findByContactIdAndDeletedFalse(UUID contactId);
}
