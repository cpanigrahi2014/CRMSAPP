package com.crm.lead.repository;

import com.crm.lead.entity.LeadAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadAttachmentRepository extends JpaRepository<LeadAttachment, UUID> {
    @Query("SELECT new LeadAttachment(a.id, a.leadId, a.fileName, a.fileType, a.fileSize, null, a.tenantId, a.createdBy, a.createdAt) FROM LeadAttachment a WHERE a.leadId = :leadId AND a.tenantId = :tenantId ORDER BY a.createdAt DESC")
    List<LeadAttachment> findByLeadIdWithoutData(@Param("leadId") UUID leadId, @Param("tenantId") String tenantId);

    Optional<LeadAttachment> findByIdAndTenantId(UUID id, String tenantId);
    long countByLeadIdAndTenantId(UUID leadId, String tenantId);
}
