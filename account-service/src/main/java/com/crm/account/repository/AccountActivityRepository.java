package com.crm.account.repository;

import com.crm.account.entity.AccountActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountActivityRepository extends JpaRepository<AccountActivity, UUID> {

    List<AccountActivity> findByAccountIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(UUID accountId, String tenantId);

    Page<AccountActivity> findByAccountIdAndTenantIdAndDeletedFalse(UUID accountId, String tenantId, Pageable pageable);

    List<AccountActivity> findByAccountIdAndTypeAndTenantIdAndDeletedFalse(UUID accountId, String type, String tenantId);

    List<AccountActivity> findByAccountIdAndDeletedFalse(UUID accountId);
}
