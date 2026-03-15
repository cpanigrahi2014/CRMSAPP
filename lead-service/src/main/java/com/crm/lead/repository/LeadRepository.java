package com.crm.lead.repository;

import com.crm.lead.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Page<Lead> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Optional<Lead> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<Lead> findByTenantIdAndStatusAndDeletedFalse(String tenantId, Lead.LeadStatus status, Pageable pageable);

    Page<Lead> findByTenantIdAndAssignedToAndDeletedFalse(String tenantId, UUID assignedTo, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.deleted = false " +
            "AND (LOWER(l.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(l.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(l.company) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Lead> searchLeads(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);

    long countByTenantIdAndStatusAndDeletedFalse(String tenantId, Lead.LeadStatus status);

    long countByTenantIdAndDeletedFalse(String tenantId);

    long countByTenantIdAndConvertedTrueAndDeletedFalse(String tenantId);

    // Duplicate detection – find leads with same email or phone
    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.deleted = false " +
            "AND ((COALESCE(:email, '') <> '' AND LOWER(l.email) = LOWER(CAST(:email AS string))) " +
            "OR (COALESCE(:phone, '') <> '' AND l.phone = :phone))")
    List<Lead> findDuplicates(@Param("tenantId") String tenantId,
                              @Param("email") String email,
                              @Param("phone") String phone);

    // Bulk update support
    @Modifying
    @Query("UPDATE Lead l SET l.status = :status WHERE l.id IN :ids AND l.tenantId = :tenantId AND l.deleted = false")
    int bulkUpdateStatus(@Param("ids") List<UUID> ids, @Param("tenantId") String tenantId, @Param("status") Lead.LeadStatus status);

    @Modifying
    @Query("UPDATE Lead l SET l.assignedTo = :assignTo WHERE l.id IN :ids AND l.tenantId = :tenantId AND l.deleted = false")
    int bulkUpdateAssignee(@Param("ids") List<UUID> ids, @Param("tenantId") String tenantId, @Param("assignTo") UUID assignTo);

    @Modifying
    @Query("UPDATE Lead l SET l.deleted = true WHERE l.id IN :ids AND l.tenantId = :tenantId AND l.deleted = false")
    int bulkDelete(@Param("ids") List<UUID> ids, @Param("tenantId") String tenantId);

    // Analytics queries
    @Query("SELECT l.source, COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.deleted = false GROUP BY l.source")
    List<Object[]> countBySource(@Param("tenantId") String tenantId);

    @Query("SELECT l.status, COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.deleted = false GROUP BY l.status")
    List<Object[]> countByStatus(@Param("tenantId") String tenantId);

    @Query("SELECT AVG(l.leadScore) FROM Lead l WHERE l.tenantId = :tenantId AND l.deleted = false")
    Double avgLeadScore(@Param("tenantId") String tenantId);

    // Find leads by IDs (for bulk operations)
    List<Lead> findByIdInAndTenantIdAndDeletedFalse(List<UUID> ids, String tenantId);

    // Campaign tracking
    List<Lead> findByCampaignIdAndTenantIdAndDeletedFalse(UUID campaignId, String tenantId);
    long countByCampaignIdAndTenantIdAndDeletedFalse(UUID campaignId, String tenantId);

    // SLA tracking – leads past SLA
    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.deleted = false AND l.slaDueDate IS NOT NULL AND l.slaDueDate < CURRENT_TIMESTAMP AND l.status NOT IN ('CONVERTED', 'LOST')")
    List<Lead> findLeadsPastSla(@Param("tenantId") String tenantId);

    // Territory querying
    Page<Lead> findByTenantIdAndTerritoryAndDeletedFalse(String tenantId, String territory, Pageable pageable);
}
