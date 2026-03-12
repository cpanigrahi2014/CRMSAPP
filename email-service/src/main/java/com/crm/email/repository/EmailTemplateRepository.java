package com.crm.email.repository;

import com.crm.email.entity.EmailTemplate;
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
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    Page<EmailTemplate> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);
    Optional<EmailTemplate> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);
    List<EmailTemplate> findByTenantIdAndIsActiveTrueAndDeletedFalse(String tenantId);
    List<EmailTemplate> findByTenantIdAndCategoryAndDeletedFalse(String tenantId, String category);

    @Query("SELECT t FROM EmailTemplate t WHERE t.tenantId = :tenantId AND t.deleted = false " +
           "AND (LOWER(t.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(t.subject) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<EmailTemplate> search(@Param("tenantId") String tenantId, @Param("q") String q, Pageable pageable);

    @Modifying
    @Query("UPDATE EmailTemplate t SET t.usageCount = t.usageCount + 1 WHERE t.id = :id")
    void incrementUsageCount(@Param("id") UUID id);
}
