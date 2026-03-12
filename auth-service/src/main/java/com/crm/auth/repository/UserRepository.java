package com.crm.auth.repository;

import com.crm.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndTenantIdAndDeletedFalse(String email, String tenantId);
    boolean existsByEmailAndTenantId(String email, String tenantId);
    Optional<User> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);
}
