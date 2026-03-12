package com.crm.activity.repository;

import com.crm.activity.entity.ActivityStreamEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityStreamRepository extends JpaRepository<ActivityStreamEvent, UUID> {

    Page<ActivityStreamEvent> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<ActivityStreamEvent> findByTenantIdAndCreatedAtAfterOrderByCreatedAtAsc(
            String tenantId, LocalDateTime since);

    Page<ActivityStreamEvent> findByEntityTypeAndEntityIdAndTenantIdOrderByCreatedAtDesc(
            String entityType, UUID entityId, String tenantId, Pageable pageable);

    Page<ActivityStreamEvent> findByPerformedByAndTenantIdOrderByCreatedAtDesc(
            String performedBy, String tenantId, Pageable pageable);
}
