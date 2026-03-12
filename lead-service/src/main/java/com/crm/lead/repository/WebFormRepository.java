package com.crm.lead.repository;

import com.crm.lead.entity.WebForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebFormRepository extends JpaRepository<WebForm, UUID> {
    List<WebForm> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<WebForm> findByIdAndTenantId(UUID id, String tenantId);
    Optional<WebForm> findByIdAndActiveTrueAndTenantId(UUID id, String tenantId);
}
