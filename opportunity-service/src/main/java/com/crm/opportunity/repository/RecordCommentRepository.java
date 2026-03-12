package com.crm.opportunity.repository;

import com.crm.opportunity.entity.RecordComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordCommentRepository extends JpaRepository<RecordComment, UUID> {

    Page<RecordComment> findByRecordTypeAndRecordIdAndTenantIdAndDeletedFalseAndParentCommentIdIsNullOrderByIsPinnedDescCreatedAtDesc(
            String recordType, UUID recordId, String tenantId, Pageable pageable);

    List<RecordComment> findByParentCommentIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(
            UUID parentCommentId, String tenantId);

    Optional<RecordComment> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    long countByRecordTypeAndRecordIdAndTenantIdAndDeletedFalse(
            String recordType, UUID recordId, String tenantId);
}
