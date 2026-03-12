package com.crm.lead.repository;

import com.crm.lead.entity.LeadNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LeadNoteRepository extends JpaRepository<LeadNote, UUID> {
    Page<LeadNote> findByLeadIdAndTenantIdOrderByCreatedAtDesc(UUID leadId, String tenantId, Pageable pageable);
    long countByLeadIdAndTenantId(UUID leadId, String tenantId);
}
