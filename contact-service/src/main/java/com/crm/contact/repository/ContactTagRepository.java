package com.crm.contact.repository;

import com.crm.contact.entity.ContactTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactTagRepository extends JpaRepository<ContactTag, UUID> {

    List<ContactTag> findByContactIdAndTenantId(UUID contactId, String tenantId);

    Optional<ContactTag> findByContactIdAndTagNameAndTenantId(UUID contactId, String tagName, String tenantId);

    void deleteByContactIdAndTagNameAndTenantId(UUID contactId, String tagName, String tenantId);

    List<ContactTag> findByTagNameAndTenantId(String tagName, String tenantId);

    long countByContactIdAndTenantId(UUID contactId, String tenantId);
}
