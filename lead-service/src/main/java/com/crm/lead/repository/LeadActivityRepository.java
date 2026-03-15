package com.crm.lead.repository;

import com.crm.lead.entity.LeadActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadActivityRepository extends JpaRepository<LeadActivity, UUID> {
    Page<LeadActivity> findByLeadIdAndTenantIdOrderByCreatedAtDesc(UUID leadId, String tenantId, Pageable pageable);
    Page<LeadActivity> findByLeadIdAndTenantIdAndActivityTypeOrderByCreatedAtDesc(UUID leadId, String tenantId, String activityType, Pageable pageable);
    List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}
