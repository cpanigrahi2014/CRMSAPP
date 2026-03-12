package com.crm.auth.repository;

import com.crm.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    List<Permission> findByTenantIdOrderByResourceAscNameAsc(String tenantId);

    List<Permission> findByResourceAndTenantId(String resource, String tenantId);

    Optional<Permission> findByNameAndTenantId(String name, String tenantId);

    Optional<Permission> findByIdAndTenantId(UUID id, String tenantId);

    boolean existsByNameAndTenantId(String name, String tenantId);
}
