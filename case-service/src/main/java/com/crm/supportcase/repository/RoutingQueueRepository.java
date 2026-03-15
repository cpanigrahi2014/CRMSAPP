package com.crm.supportcase.repository;

import com.crm.supportcase.entity.RoutingQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoutingQueueRepository extends JpaRepository<RoutingQueue, UUID> {

    Optional<RoutingQueue> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<RoutingQueue> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    List<RoutingQueue> findByTenantIdAndActiveAndDeletedFalse(String tenantId, boolean active);

    List<RoutingQueue> findByTenantIdAndChannelAndActiveAndDeletedFalse(
            String tenantId, RoutingQueue.Channel channel, boolean active);

    Optional<RoutingQueue> findByTenantIdAndNameAndDeletedFalse(String tenantId, String name);
}
