package com.crm.opportunity.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.security.TenantContext;
import com.crm.opportunity.dto.CreateCommentRequest;
import com.crm.opportunity.dto.RecordCommentResponse;
import com.crm.opportunity.dto.UpdateCommentRequest;
import com.crm.opportunity.entity.RecordComment;
import com.crm.opportunity.repository.RecordCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecordCommentService {

    private final RecordCommentRepository commentRepo;
    private final MentionService mentionService;
    private final EventPublisher eventPublisher;

    @Transactional
    public RecordCommentResponse addComment(CreateCommentRequest request, String userId, String userName) {
        String tenantId = TenantContext.getTenantId();

        RecordComment comment = RecordComment.builder()
                .recordType(request.getRecordType())
                .recordId(request.getRecordId())
                .authorId(userId)
                .authorName(userName)
                .content(request.getContent())
                .parentCommentId(request.getParentCommentId())
                .isInternal(request.getIsInternal() != null ? request.getIsInternal() : true)
                .tenantId(tenantId)
                .build();

        comment = commentRepo.save(comment);
        log.info("Comment added to {} {} by {}", request.getRecordType(), request.getRecordId(), userId);

        // Process @mentions in the comment content
        mentionService.processMentions(request.getContent(), request.getRecordType(),
                request.getRecordId(), "COMMENT", comment.getId(), userId, userName);

        eventPublisher.publish("opportunity-events", tenantId, userId,
                "RecordComment", comment.getId().toString(), "COMMENT_ADDED",
                mapToResponse(comment, Collections.emptyList()));

        return mapToResponse(comment, Collections.emptyList());
    }

    @Transactional
    public RecordCommentResponse updateComment(UUID commentId, UpdateCommentRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        RecordComment comment = commentRepo.findByIdAndTenantIdAndDeletedFalse(commentId, tenantId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthorId().equals(userId)) {
            throw new RuntimeException("Only the author can edit this comment");
        }

        if (request.getContent() != null) {
            comment.setContent(request.getContent());
            comment.setIsEdited(true);
        }
        if (request.getIsPinned() != null) {
            comment.setIsPinned(request.getIsPinned());
        }

        comment = commentRepo.save(comment);

        List<RecordComment> replies = commentRepo
                .findByParentCommentIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(commentId, tenantId);

        return mapToResponse(comment, replies);
    }

    @Transactional(readOnly = true)
    public PagedResponse<RecordCommentResponse> getComments(String recordType, UUID recordId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<RecordComment> p = commentRepo
                .findByRecordTypeAndRecordIdAndTenantIdAndDeletedFalseAndParentCommentIdIsNullOrderByIsPinnedDescCreatedAtDesc(
                        recordType, recordId, tenantId, PageRequest.of(page, size));

        List<RecordCommentResponse> content = p.getContent().stream().map(c -> {
            List<RecordComment> replies = commentRepo
                    .findByParentCommentIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(c.getId(), tenantId);
            return mapToResponse(c, replies);
        }).toList();

        return PagedResponse.<RecordCommentResponse>builder()
                .content(content)
                .pageNumber(p.getNumber())
                .pageSize(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .first(p.isFirst())
                .build();
    }

    @Transactional
    public void deleteComment(UUID commentId, String userId) {
        String tenantId = TenantContext.getTenantId();
        RecordComment comment = commentRepo.findByIdAndTenantIdAndDeletedFalse(commentId, tenantId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        comment.setDeleted(true);
        commentRepo.save(comment);
    }

    private RecordCommentResponse mapToResponse(RecordComment c, List<RecordComment> replies) {
        return RecordCommentResponse.builder()
                .id(c.getId())
                .recordType(c.getRecordType())
                .recordId(c.getRecordId())
                .authorId(c.getAuthorId())
                .authorName(c.getAuthorName())
                .content(c.getContent())
                .parentCommentId(c.getParentCommentId())
                .isInternal(c.getIsInternal())
                .isEdited(c.getIsEdited())
                .isPinned(c.getIsPinned())
                .replies(replies != null ? replies.stream()
                        .map(r -> mapToResponse(r, Collections.emptyList())).toList() : null)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
