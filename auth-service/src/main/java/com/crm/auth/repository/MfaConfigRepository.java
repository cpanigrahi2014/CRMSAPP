package com.crm.auth.repository;

import com.crm.auth.entity.MfaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaConfigRepository extends JpaRepository<MfaConfig, UUID> {

    List<MfaConfig> findByUserIdAndTenantId(UUID userId, String tenantId);

    Optional<MfaConfig> findByUserIdAndMfaTypeAndTenantId(UUID userId, String mfaType, String tenantId);

    Optional<MfaConfig> findByIdAndTenantId(UUID id, String tenantId);

    boolean existsByUserIdAndMfaTypeAndTenantId(UUID userId, String mfaType, String tenantId);
}
