package com.crm.supportcase.repository;

import com.crm.supportcase.entity.AgentSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentSkillRepository extends JpaRepository<AgentSkill, UUID> {

    Optional<AgentSkill> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    List<AgentSkill> findByUserIdAndTenantIdAndDeletedFalse(UUID userId, String tenantId);

    List<AgentSkill> findByTenantIdAndSkillNameAndDeletedFalse(String tenantId, String skillName);

    List<AgentSkill> findByUserIdAndTenantIdAndSkillNameAndDeletedFalse(
            UUID userId, String tenantId, String skillName);

    List<AgentSkill> findByTenantIdAndDeletedFalse(String tenantId);
}
