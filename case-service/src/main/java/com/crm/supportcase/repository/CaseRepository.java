package com.crm.supportcase.repository;

import com.crm.supportcase.entity.SupportCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<SupportCase, UUID> {

    Optional<SupportCase> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Optional<SupportCase> findByCaseNumberAndTenantIdAndDeletedFalse(String caseNumber, String tenantId);

    Page<SupportCase> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Page<SupportCase> findByTenantIdAndStatusAndDeletedFalse(String tenantId, SupportCase.CaseStatus status, Pageable pageable);

    Page<SupportCase> findByTenantIdAndPriorityAndDeletedFalse(String tenantId, SupportCase.CasePriority priority, Pageable pageable);

    Page<SupportCase> findByTenantIdAndAssignedToAndDeletedFalse(String tenantId, UUID assignedTo, Pageable pageable);

    @Query(value = "SELECT * FROM cases WHERE tenant_id = :tenantId AND deleted = false " +
           "AND status NOT IN ('RESOLVED', 'CLOSED') " +
           "AND updated_at < :threshold AND escalated = false", nativeQuery = true)
    List<SupportCase> findCasesNeedingEscalation(@Param("tenantId") String tenantId,
                                                  @Param("threshold") LocalDateTime threshold);

    @Query(value = "SELECT * FROM cases WHERE deleted = false " +
           "AND status NOT IN ('RESOLVED', 'CLOSED') " +
           "AND updated_at < :threshold AND escalated = false", nativeQuery = true)
    List<SupportCase> findAllCasesNeedingEscalation(@Param("threshold") LocalDateTime threshold);

    @Query(value = "SELECT * FROM cases WHERE tenant_id = :tenantId AND deleted = false " +
           "AND status NOT IN ('RESOLVED', 'CLOSED') AND sla_due_date < :now", nativeQuery = true)
    List<SupportCase> findSlaBreachedCases(@Param("tenantId") String tenantId,
                                            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(c) FROM SupportCase c WHERE c.tenantId = :tenantId AND c.deleted = false")
    long countByTenant(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(c) FROM SupportCase c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.slaMet = true")
    long countSlaMetByTenant(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(c) FROM SupportCase c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.slaMet = false")
    long countSlaBreachedByTenant(@Param("tenantId") String tenantId);

    @Query("SELECT c.status, COUNT(c) FROM SupportCase c WHERE c.tenantId = :tenantId AND c.deleted = false GROUP BY c.status")
    List<Object[]> countByStatus(@Param("tenantId") String tenantId);

    @Query("SELECT c.priority, COUNT(c) FROM SupportCase c WHERE c.tenantId = :tenantId AND c.deleted = false GROUP BY c.priority")
    List<Object[]> countByPriority(@Param("tenantId") String tenantId);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600) FROM cases " +
           "WHERE tenant_id = :tenantId AND deleted = false AND resolved_at IS NOT NULL", nativeQuery = true)
    Double avgResolutionHours(@Param("tenantId") String tenantId);

    @Query("SELECT AVG(c.csatScore) FROM SupportCase c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.csatScore IS NOT NULL")
    Double avgCsatScore(@Param("tenantId") String tenantId);
}
