package com.crm.account.repository;

import com.crm.account.entity.AccountNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountNoteRepository extends JpaRepository<AccountNote, UUID> {

    List<AccountNote> findByAccountIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(UUID accountId, String tenantId);

    Page<AccountNote> findByAccountIdAndTenantIdAndDeletedFalse(UUID accountId, String tenantId, Pageable pageable);

    Optional<AccountNote> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);
}
