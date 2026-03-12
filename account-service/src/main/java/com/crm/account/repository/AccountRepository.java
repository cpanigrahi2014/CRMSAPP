package com.crm.account.repository;

import com.crm.account.entity.Account;
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
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Page<Account> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Optional<Account> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    List<Account> findByParentAccountIdAndTenantIdAndDeletedFalse(UUID parentAccountId, String tenantId);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false " +
            "AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.industry) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.website) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Account> searchAccounts(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);

    // Type queries
    Page<Account> findByTypeAndTenantIdAndDeletedFalse(String type, String tenantId, Pageable pageable);

    // Owner queries
    Page<Account> findByOwnerIdAndTenantIdAndDeletedFalse(String ownerId, String tenantId, Pageable pageable);
    List<Account> findByOwnerIdIsNullAndTenantIdAndDeletedFalse(String tenantId);

    // Territory queries
    Page<Account> findByTerritoryAndTenantIdAndDeletedFalse(String territory, String tenantId, Pageable pageable);

    // Lifecycle queries
    Page<Account> findByLifecycleStageAndTenantIdAndDeletedFalse(String lifecycleStage, String tenantId, Pageable pageable);

    // Segment queries
    Page<Account> findBySegmentAndTenantIdAndDeletedFalse(String segment, String tenantId, Pageable pageable);

    // Duplicate detection
    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false " +
            "AND (LOWER(a.name) = LOWER(:name) OR LOWER(a.phone) = :phone OR LOWER(a.website) = LOWER(:website))")
    List<Account> findPotentialDuplicates(@Param("tenantId") String tenantId,
                                          @Param("name") String name,
                                          @Param("phone") String phone,
                                          @Param("website") String website);

    // Analytics
    long countByTenantIdAndDeletedFalse(String tenantId);
    long countByTypeAndTenantIdAndDeletedFalse(String type, String tenantId);
    long countByLifecycleStageAndTenantIdAndDeletedFalse(String lifecycleStage, String tenantId);
    long countByTerritoryAndTenantIdAndDeletedFalse(String territory, String tenantId);

    @Query("SELECT a.type, COUNT(a) FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false GROUP BY a.type")
    List<Object[]> countByTypeGrouped(@Param("tenantId") String tenantId);

    @Query("SELECT a.industry, COUNT(a) FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false AND a.industry IS NOT NULL GROUP BY a.industry")
    List<Object[]> countByIndustryGrouped(@Param("tenantId") String tenantId);

    @Query("SELECT a.lifecycleStage, COUNT(a) FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false GROUP BY a.lifecycleStage")
    List<Object[]> countByLifecycleStageGrouped(@Param("tenantId") String tenantId);

    @Query("SELECT a.territory, COUNT(a) FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false AND a.territory IS NOT NULL GROUP BY a.territory")
    List<Object[]> countByTerritoryGrouped(@Param("tenantId") String tenantId);

    @Query("SELECT a.segment, COUNT(a) FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false AND a.segment IS NOT NULL GROUP BY a.segment")
    List<Object[]> countBySegmentGrouped(@Param("tenantId") String tenantId);

    @Query("SELECT COALESCE(SUM(a.annualRevenue), 0) FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false")
    java.math.BigDecimal sumAnnualRevenue(@Param("tenantId") String tenantId);

    @Query("SELECT COALESCE(AVG(a.annualRevenue), 0) FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false AND a.annualRevenue IS NOT NULL")
    java.math.BigDecimal avgAnnualRevenue(@Param("tenantId") String tenantId);

    @Query("SELECT COALESCE(AVG(a.healthScore), 0) FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false AND a.healthScore IS NOT NULL")
    Double avgHealthScore(@Param("tenantId") String tenantId);

    @Query("SELECT COALESCE(AVG(a.engagementScore), 0) FROM Account a WHERE a.tenantId = :tenantId AND a.deleted = false AND a.engagementScore IS NOT NULL")
    Double avgEngagementScore(@Param("tenantId") String tenantId);

    // Batch operations
    List<Account> findByIdInAndTenantIdAndDeletedFalse(List<UUID> ids, String tenantId);

    List<Account> findByTenantIdAndDeletedFalse(String tenantId);
}
