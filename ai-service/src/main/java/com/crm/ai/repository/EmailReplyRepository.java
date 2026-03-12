package com.crm.ai.repository;

import com.crm.ai.entity.EmailReplyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailReplyRepository extends JpaRepository<EmailReplyRecord, UUID> {
    List<EmailReplyRecord> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<EmailReplyRecord> findByIdAndTenantId(UUID id, String tenantId);
}
