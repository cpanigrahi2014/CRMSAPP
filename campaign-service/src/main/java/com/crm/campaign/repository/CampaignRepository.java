package com.crm.campaign.repository;

import com.crm.campaign.entity.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Optional<Campaign> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<Campaign> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    @Query(value = "SELECT * FROM campaigns WHERE tenant_id = :tenantId AND deleted = false AND status = :status",
            countQuery = "SELECT count(*) FROM campaigns WHERE tenant_id = :tenantId AND deleted = false AND status = :status",
            nativeQuery = true)
    Page<Campaign> findByTenantIdAndStatusAndDeletedFalse(@Param("tenantId") String tenantId,
                                                           @Param("status") String status,
                                                           Pageable pageable);

    @Query(value = "SELECT * FROM campaigns WHERE tenant_id = :tenantId AND deleted = false AND type = :type",
            countQuery = "SELECT count(*) FROM campaigns WHERE tenant_id = :tenantId AND deleted = false AND type = :type",
            nativeQuery = true)
    Page<Campaign> findByTenantIdAndTypeAndDeletedFalse(@Param("tenantId") String tenantId,
                                                         @Param("type") String type,
                                                         Pageable pageable);

    long countByTenantIdAndDeletedFalse(String tenantId);
}
