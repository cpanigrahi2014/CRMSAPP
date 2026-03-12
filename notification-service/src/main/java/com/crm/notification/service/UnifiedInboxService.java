package com.crm.notification.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.security.TenantContext;
import com.crm.notification.dto.UnifiedInboxResponse;
import com.crm.notification.entity.UnifiedMessage;
import com.crm.notification.repository.UnifiedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedInboxService {

    private final UnifiedMessageRepository unifiedMessageRepository;

    @Transactional(readOnly = true)
    public PagedResponse<UnifiedInboxResponse> getInbox(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<UnifiedMessage> msgPage = unifiedMessageRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size));
        return buildPagedResponse(msgPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UnifiedInboxResponse> getByChannel(String channel, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<UnifiedMessage> msgPage = unifiedMessageRepository
                .findByTenantIdAndChannelOrderByCreatedAtDesc(tenantId, channel.toUpperCase(), PageRequest.of(page, size));
        return buildPagedResponse(msgPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UnifiedInboxResponse> getByEntity(String entityType, String entityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<UnifiedMessage> msgPage = unifiedMessageRepository
                .findByTenantIdAndRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc(
                        tenantId, entityType, entityId, PageRequest.of(page, size));
        return buildPagedResponse(msgPage);
    }

    private UnifiedInboxResponse toResponse(UnifiedMessage msg) {
        return UnifiedInboxResponse.builder()
                .id(msg.getId())
                .channel(msg.getChannel())
                .direction(msg.getDirection())
                .sender(msg.getSender())
                .recipient(msg.getRecipient())
                .subject(msg.getSubject())
                .body(msg.getBody())
                .status(msg.getStatus())
                .sourceId(msg.getSourceId() != null ? msg.getSourceId().toString() : null)
                .relatedEntityType(msg.getRelatedEntityType())
                .relatedEntityId(msg.getRelatedEntityId() != null ? msg.getRelatedEntityId().toString() : null)
                .tenantId(msg.getTenantId())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    private PagedResponse<UnifiedInboxResponse> buildPagedResponse(Page<UnifiedMessage> page) {
        return PagedResponse.<UnifiedInboxResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
