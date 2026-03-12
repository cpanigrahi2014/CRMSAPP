package com.crm.email.repository;

import com.crm.email.entity.EmailSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailScheduleRepository extends JpaRepository<EmailSchedule, UUID> {
    Page<EmailSchedule> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);
    Optional<EmailSchedule> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    @Query("SELECT s FROM EmailSchedule s WHERE s.deleted = false AND s.status = 'PENDING' AND s.scheduledAt <= :now")
    List<EmailSchedule> findReadyToSend(@Param("now") LocalDateTime now);

    long countByTenantIdAndStatusAndDeletedFalse(String tenantId, EmailSchedule.ScheduleStatus status);
}
