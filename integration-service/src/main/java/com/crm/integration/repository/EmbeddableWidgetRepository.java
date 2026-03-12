package com.crm.integration.repository;

import com.crm.integration.entity.EmbeddableWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmbeddableWidgetRepository extends JpaRepository<EmbeddableWidget, UUID> {
    List<EmbeddableWidget> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<EmbeddableWidget> findByIdAndTenantId(UUID id, String tenantId);
    Optional<EmbeddableWidget> findByEmbedToken(String embedToken);
}
