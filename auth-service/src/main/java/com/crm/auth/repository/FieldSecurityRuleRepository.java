package com.crm.auth.repository;

import com.crm.auth.entity.FieldSecurityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldSecurityRuleRepository extends JpaRepository<FieldSecurityRule, UUID> {

    List<FieldSecurityRule> findByTenantIdOrderByEntityTypeAscFieldNameAsc(String tenantId);

    List<FieldSecurityRule> findByEntityTypeAndTenantId(String entityType, String tenantId);

    List<FieldSecurityRule> findByRoleNameAndTenantId(String roleName, String tenantId);

    Optional<FieldSecurityRule> findByIdAndTenantId(UUID id, String tenantId);
}
