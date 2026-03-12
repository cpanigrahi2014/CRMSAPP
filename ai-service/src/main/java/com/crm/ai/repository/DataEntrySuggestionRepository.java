package com.crm.ai.repository;

import com.crm.ai.entity.DataEntrySuggestionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataEntrySuggestionRepository extends JpaRepository<DataEntrySuggestionRecord, UUID> {
    List<DataEntrySuggestionRecord> findByTenantIdAndAcceptedIsNullOrderByConfidenceDesc(String tenantId);
    List<DataEntrySuggestionRecord> findByEntityTypeAndEntityIdAndTenantId(String entityType, String entityId, String tenantId);
    Optional<DataEntrySuggestionRecord> findByIdAndTenantId(UUID id, String tenantId);
}
