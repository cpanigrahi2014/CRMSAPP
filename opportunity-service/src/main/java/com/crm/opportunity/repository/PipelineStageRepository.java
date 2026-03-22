package com.crm.opportunity.repository;

import com.crm.opportunity.entity.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {

    List<PipelineStage> findByTenantIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(String tenantId);

    List<PipelineStage> findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(String tenantId);

    Optional<PipelineStage> findByTenantIdAndNameAndDeletedFalse(String tenantId, String name);

    boolean existsByTenantIdAndDeletedFalse(String tenantId);
}
