package com.crm.supportcase.repository;

import com.crm.supportcase.entity.AgentPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentPresenceRepository extends JpaRepository<AgentPresence, UUID> {

    Optional<AgentPresence> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Optional<AgentPresence> findByUserIdAndTenantIdAndDeletedFalse(UUID userId, String tenantId);

    List<AgentPresence> findByTenantIdAndDeletedFalse(String tenantId);

    List<AgentPresence> findByTenantIdAndStatusAndDeletedFalse(
            String tenantId, AgentPresence.PresenceStatus status);

    List<AgentPresence> findByTenantIdAndQueueIdAndDeletedFalse(String tenantId, UUID queueId);

    @Query("SELECT a FROM AgentPresence a WHERE a.tenantId = :tenantId AND a.deleted = false " +
           "AND a.queueId = :queueId AND a.status = 'ONLINE' AND a.activeWorkCount < a.capacity " +
           "ORDER BY a.activeWorkCount ASC, a.lastRoutedAt ASC NULLS FIRST")
    List<AgentPresence> findAvailableAgentsInQueue(
            @Param("tenantId") String tenantId,
            @Param("queueId") UUID queueId);

    @Query("SELECT COUNT(a) FROM AgentPresence a WHERE a.tenantId = :tenantId AND a.deleted = false " +
           "AND a.status = 'ONLINE'")
    long countOnlineAgents(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(a) FROM AgentPresence a WHERE a.tenantId = :tenantId AND a.deleted = false " +
           "AND a.status = 'BUSY'")
    long countBusyAgents(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(a) FROM AgentPresence a WHERE a.tenantId = :tenantId AND a.deleted = false " +
           "AND a.status != 'OFFLINE'")
    long countActiveAgents(@Param("tenantId") String tenantId);

    @Query("SELECT AVG(CAST(a.activeWorkCount AS double) / CASE WHEN a.capacity > 0 THEN a.capacity ELSE 1 END) " +
           "FROM AgentPresence a WHERE a.tenantId = :tenantId AND a.deleted = false AND a.status != 'OFFLINE'")
    Double avgUtilization(@Param("tenantId") String tenantId);
}
