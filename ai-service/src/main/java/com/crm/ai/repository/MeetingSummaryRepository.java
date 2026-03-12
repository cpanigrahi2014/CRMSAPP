package com.crm.ai.repository;

import com.crm.ai.entity.MeetingSummaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingSummaryRepository extends JpaRepository<MeetingSummaryRecord, UUID> {
    List<MeetingSummaryRecord> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<MeetingSummaryRecord> findByIdAndTenantId(UUID id, String tenantId);
    List<MeetingSummaryRecord> findByRelatedEntityTypeAndRelatedEntityIdAndTenantId(
            String relatedEntityType, String relatedEntityId, String tenantId);
}
