package com.crm.lead.repository;

import com.crm.lead.entity.LeadTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadTagRepository extends JpaRepository<LeadTag, UUID> {
    List<LeadTag> findByTenantId(String tenantId);
    Optional<LeadTag> findByNameAndTenantId(String name, String tenantId);

    @Query(value = "SELECT t.* FROM lead_tags t INNER JOIN lead_tag_mappings m ON t.id = m.tag_id WHERE m.lead_id = :leadId", nativeQuery = true)
    List<LeadTag> findTagsByLeadId(@Param("leadId") UUID leadId);

    @Modifying
    @Query(value = "INSERT INTO lead_tag_mappings (lead_id, tag_id) VALUES (:leadId, :tagId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void addTagToLead(@Param("leadId") UUID leadId, @Param("tagId") UUID tagId);

    @Modifying
    @Query(value = "DELETE FROM lead_tag_mappings WHERE lead_id = :leadId AND tag_id = :tagId", nativeQuery = true)
    void removeTagFromLead(@Param("leadId") UUID leadId, @Param("tagId") UUID tagId);
}
