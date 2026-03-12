package com.crm.contact.repository;

import com.crm.contact.entity.ContactCommunication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactCommunicationRepository extends JpaRepository<ContactCommunication, UUID> {

    Page<ContactCommunication> findByContactIdAndTenantIdOrderByCommunicationDateDesc(
            UUID contactId, String tenantId, Pageable pageable);

    Optional<ContactCommunication> findByIdAndTenantId(UUID id, String tenantId);

    long countByContactIdAndTenantId(UUID contactId, String tenantId);

    long countByContactIdAndCommTypeAndTenantId(UUID contactId, String commType, String tenantId);
}
