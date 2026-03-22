package com.crm.activity.repository;

import com.crm.activity.entity.CalendarFeedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarFeedTokenRepository extends JpaRepository<CalendarFeedToken, UUID> {

    Optional<CalendarFeedToken> findByTokenAndActiveTrue(String token);

    List<CalendarFeedToken> findByTenantIdAndUserIdAndActiveTrue(String tenantId, String userId);

    Optional<CalendarFeedToken> findByIdAndTenantIdAndUserId(UUID id, String tenantId, String userId);
}
