package com.crm.notification.repository;

import com.crm.notification.entity.CallRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CallRecordRepository extends JpaRepository<CallRecord, UUID> {
    Page<CallRecord> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);
    Page<CallRecord> findByTenantIdAndDirectionAndDeletedFalse(String tenantId, CallRecord.Direction direction, Pageable pageable);
    Page<CallRecord> findByTenantIdAndToNumberAndDeletedFalse(String tenantId, String toNumber, Pageable pageable);
    Optional<CallRecord> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);
}
