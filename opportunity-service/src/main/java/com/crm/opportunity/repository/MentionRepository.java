package com.crm.opportunity.repository;

import com.crm.opportunity.entity.Mention;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MentionRepository extends JpaRepository<Mention, UUID> {

    Page<Mention> findByMentionedUserIdAndTenantIdOrderByCreatedAtDesc(
            String mentionedUserId, String tenantId, Pageable pageable);

    Page<Mention> findByMentionedUserIdAndIsReadAndTenantIdOrderByCreatedAtDesc(
            String mentionedUserId, Boolean isRead, String tenantId, Pageable pageable);

    List<Mention> findBySourceTypeAndSourceIdAndTenantId(String sourceType, UUID sourceId, String tenantId);

    List<Mention> findByRecordTypeAndRecordIdAndTenantId(String recordType, UUID recordId, String tenantId);

    Optional<Mention> findByIdAndTenantId(UUID id, String tenantId);

    long countByMentionedUserIdAndIsReadAndTenantId(String mentionedUserId, Boolean isRead, String tenantId);
}
