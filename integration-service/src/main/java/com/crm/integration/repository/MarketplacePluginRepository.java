package com.crm.integration.repository;

import com.crm.integration.entity.MarketplacePlugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplacePluginRepository extends JpaRepository<MarketplacePlugin, UUID> {
    List<MarketplacePlugin> findByStatusOrderByInstallCountDesc(String status);
    List<MarketplacePlugin> findByCategoryAndStatusOrderByInstallCountDesc(String category, String status);
    Optional<MarketplacePlugin> findBySlug(String slug);
}
