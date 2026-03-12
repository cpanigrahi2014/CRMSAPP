package com.crm.account.repository;

import com.crm.account.entity.AccountAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountAttachmentRepository extends JpaRepository<AccountAttachment, UUID> {

    List<AccountAttachment> findByAccountIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(UUID accountId, String tenantId);

    Optional<AccountAttachment> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);
}
