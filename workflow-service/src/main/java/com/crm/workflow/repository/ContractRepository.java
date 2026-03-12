package com.crm.workflow.repository;

import com.crm.workflow.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Page<Contract> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<Contract> findByOpportunityIdAndDeletedFalseOrderByVersionDesc(UUID opportunityId, Pageable pageable);

    Optional<Contract> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    List<Contract> findByTenantIdAndStatusAndDeletedFalse(String tenantId, String status);

    long countByTenantIdAndDeletedFalse(String tenantId);
}
