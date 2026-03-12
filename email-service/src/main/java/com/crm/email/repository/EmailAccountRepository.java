package com.crm.email.repository;

import com.crm.email.entity.EmailAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailAccountRepository extends JpaRepository<EmailAccount, UUID> {
    List<EmailAccount> findByTenantIdAndDeletedFalse(String tenantId);
    Optional<EmailAccount> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);
    Optional<EmailAccount> findByTenantIdAndIsDefaultTrueAndDeletedFalse(String tenantId);
    Optional<EmailAccount> findByTenantIdAndEmailAndDeletedFalse(String tenantId, String email);
    long countByTenantIdAndProviderAndDeletedFalse(String tenantId, EmailAccount.Provider provider);
}
