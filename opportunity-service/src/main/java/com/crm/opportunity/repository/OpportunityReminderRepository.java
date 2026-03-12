package com.crm.opportunity.repository;

import com.crm.opportunity.entity.OpportunityReminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityReminderRepository extends JpaRepository<OpportunityReminder, UUID> {

    Page<OpportunityReminder> findByOpportunityIdAndTenantIdOrderByRemindAtAsc(UUID opportunityId, String tenantId, Pageable pageable);

    Optional<OpportunityReminder> findByIdAndTenantId(UUID id, String tenantId);

    List<OpportunityReminder> findByTenantIdAndIsCompletedFalseAndRemindAtBefore(String tenantId, LocalDateTime now);

    long countByOpportunityIdAndTenantIdAndIsCompletedFalse(UUID opportunityId, String tenantId);

    void deleteByIdAndTenantId(UUID id, String tenantId);
}
