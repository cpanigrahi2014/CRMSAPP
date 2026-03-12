package com.crm.opportunity.repository;

import com.crm.opportunity.entity.DealChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DealChatMessageRepository extends JpaRepository<DealChatMessage, UUID> {

    Page<DealChatMessage> findByOpportunityIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID opportunityId, String tenantId, Pageable pageable);

    List<DealChatMessage> findByOpportunityIdAndTenantIdAndDeletedFalseAndCreatedAtAfterOrderByCreatedAtAsc(
            UUID opportunityId, String tenantId, LocalDateTime since);

    Optional<DealChatMessage> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    List<DealChatMessage> findByParentMessageIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(
            UUID parentMessageId, String tenantId);

    long countByOpportunityIdAndTenantIdAndDeletedFalse(UUID opportunityId, String tenantId);
}
