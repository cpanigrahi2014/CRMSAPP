package com.crm.contact.repository;

import com.crm.contact.entity.ContactActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContactActivityRepository extends JpaRepository<ContactActivity, UUID> {

    Page<ContactActivity> findByContactIdAndTenantIdOrderByCreatedAtDesc(
            UUID contactId, String tenantId, Pageable pageable);

    long countByContactIdAndTenantId(UUID contactId, String tenantId);
}
