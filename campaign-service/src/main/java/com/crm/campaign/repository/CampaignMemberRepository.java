package com.crm.campaign.repository;

import com.crm.campaign.entity.CampaignMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {

    Optional<CampaignMember> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<CampaignMember> findByCampaignIdAndTenantIdAndDeletedFalse(UUID campaignId, String tenantId, Pageable pageable);

    List<CampaignMember> findByCampaignIdAndTenantIdAndDeletedFalse(UUID campaignId, String tenantId);

    Optional<CampaignMember> findByCampaignIdAndLeadIdAndTenantIdAndDeletedFalse(UUID campaignId, UUID leadId, String tenantId);

    long countByCampaignIdAndTenantIdAndDeletedFalse(UUID campaignId, String tenantId);

    @Query(value = "SELECT count(*) FROM campaign_members WHERE campaign_id = :campaignId AND tenant_id = :tenantId AND deleted = false AND status = :status",
            nativeQuery = true)
    long countByCampaignIdAndStatusAndTenantId(@Param("campaignId") UUID campaignId,
                                                @Param("status") String status,
                                                @Param("tenantId") String tenantId);
}
