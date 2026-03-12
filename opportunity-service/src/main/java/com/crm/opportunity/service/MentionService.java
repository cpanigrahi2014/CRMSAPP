package com.crm.opportunity.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.security.TenantContext;
import com.crm.opportunity.dto.MentionResponse;
import com.crm.opportunity.entity.Mention;
import com.crm.opportunity.repository.MentionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class MentionService {

    private final MentionRepository mentionRepo;
    private final EventPublisher eventPublisher;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\[(.*?)\\]\\((.*?)\\)");

    /**
     * Parse @mentions from text content and create Mention records.
     * Expected format: @[User Name](userId)
     */
    @Transactional
    public List<MentionResponse> processMentions(String content, String recordType, UUID recordId,
                                                  String sourceType, UUID sourceId,
                                                  String mentionedById, String mentionedByName) {
        String tenantId = TenantContext.getTenantId();
        List<MentionResponse> result = new ArrayList<>();

        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String userName = matcher.group(1);
            String userId = matcher.group(2);

            Mention mention = Mention.builder()
                    .recordType(recordType)
                    .recordId(recordId)
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .mentionedUserId(userId)
                    .mentionedUserName(userName)
                    .mentionedById(mentionedById)
                    .mentionedByName(mentionedByName)
                    .tenantId(tenantId)
                    .build();

            mention = mentionRepo.save(mention);
            result.add(mapToResponse(mention));

            log.info("Mention created: {} mentioned {} in {} {}", mentionedByName, userName, sourceType, sourceId);

            eventPublisher.publish("notification-events", tenantId, mentionedById,
                    "Mention", mention.getId().toString(), "USER_MENTIONED",
                    mapToResponse(mention));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public PagedResponse<MentionResponse> getMyMentions(String userId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<Mention> p = mentionRepo.findByMentionedUserIdAndTenantIdOrderByCreatedAtDesc(
                userId, tenantId, PageRequest.of(page, size));

        return PagedResponse.<MentionResponse>builder()
                .content(p.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(p.getNumber())
                .pageSize(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .first(p.isFirst())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<MentionResponse> getUnreadMentions(String userId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<Mention> p = mentionRepo.findByMentionedUserIdAndIsReadAndTenantIdOrderByCreatedAtDesc(
                userId, false, tenantId, PageRequest.of(page, size));

        return PagedResponse.<MentionResponse>builder()
                .content(p.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(p.getNumber())
                .pageSize(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .first(p.isFirst())
                .build();
    }

    @Transactional
    public void markAsRead(UUID mentionId) {
        String tenantId = TenantContext.getTenantId();
        Mention mention = mentionRepo.findByIdAndTenantId(mentionId, tenantId)
                .orElseThrow(() -> new RuntimeException("Mention not found"));
        mention.setIsRead(true);
        mentionRepo.save(mention);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        String tenantId = TenantContext.getTenantId();
        Page<Mention> unread = mentionRepo.findByMentionedUserIdAndIsReadAndTenantIdOrderByCreatedAtDesc(
                userId, false, tenantId, PageRequest.of(0, 1000));
        unread.getContent().forEach(m -> m.setIsRead(true));
        mentionRepo.saveAll(unread.getContent());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        String tenantId = TenantContext.getTenantId();
        return mentionRepo.countByMentionedUserIdAndIsReadAndTenantId(userId, false, tenantId);
    }

    private MentionResponse mapToResponse(Mention m) {
        return MentionResponse.builder()
                .id(m.getId())
                .recordType(m.getRecordType())
                .recordId(m.getRecordId())
                .sourceType(m.getSourceType())
                .sourceId(m.getSourceId())
                .mentionedUserId(m.getMentionedUserId())
                .mentionedUserName(m.getMentionedUserName())
                .mentionedById(m.getMentionedById())
                .mentionedByName(m.getMentionedByName())
                .isRead(m.getIsRead())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
