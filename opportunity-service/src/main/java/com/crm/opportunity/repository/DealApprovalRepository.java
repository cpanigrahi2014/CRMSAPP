package com.crm.opportunity.repository;

import com.crm.opportunity.entity.DealApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DealApprovalRepository extends JpaRepository<DealApproval, UUID> {

    Page<DealApproval> findByOpportunityIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID opportunityId, String tenantId, Pageable pageable);

    Page<DealApproval> findByApproverIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
            String approverId, String tenantId, Pageable pageable);

    Page<DealApproval> findByApproverIdAndStatusAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
            String approverId, DealApproval.ApprovalStatus status, String tenantId, Pageable pageable);

    List<DealApproval> findByRequestedByIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
            String requestedById, String tenantId);

    Optional<DealApproval> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    long countByApproverIdAndStatusAndTenantIdAndDeletedFalse(
            String approverId, DealApproval.ApprovalStatus status, String tenantId);
}
