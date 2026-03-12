package com.crm.opportunity.repository;

import com.crm.opportunity.entity.OpportunityProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityProductRepository extends JpaRepository<OpportunityProduct, UUID> {

    Page<OpportunityProduct> findByOpportunityIdAndTenantId(UUID opportunityId, String tenantId, Pageable pageable);

    Optional<OpportunityProduct> findByIdAndTenantId(UUID id, String tenantId);

    @Query("SELECT COALESCE(SUM(p.totalPrice), 0) FROM OpportunityProduct p WHERE p.opportunityId = :oppId AND p.tenantId = :tenantId")
    BigDecimal sumTotalByOpportunityId(@Param("oppId") UUID opportunityId, @Param("tenantId") String tenantId);

    long countByOpportunityIdAndTenantId(UUID opportunityId, String tenantId);

    void deleteByIdAndTenantId(UUID id, String tenantId);
}
