package com.crm.notification.repository;

import com.crm.notification.entity.SmsMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SmsMessageRepository extends JpaRepository<SmsMessage, UUID> {
    Page<SmsMessage> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);
    Page<SmsMessage> findByTenantIdAndDirectionAndDeletedFalse(String tenantId, SmsMessage.Direction direction, Pageable pageable);
    Page<SmsMessage> findByTenantIdAndToNumberAndDeletedFalse(String tenantId, String toNumber, Pageable pageable);
    Optional<SmsMessage> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);
}
