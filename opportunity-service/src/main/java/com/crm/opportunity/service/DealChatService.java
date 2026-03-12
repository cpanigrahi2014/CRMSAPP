package com.crm.opportunity.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.security.TenantContext;
import com.crm.opportunity.dto.ChatMessageResponse;
import com.crm.opportunity.dto.SendChatMessageRequest;
import com.crm.opportunity.entity.DealChatMessage;
import com.crm.opportunity.repository.DealChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DealChatService {

    private final DealChatMessageRepository chatRepo;
    private final EventPublisher eventPublisher;

    @Transactional
    public ChatMessageResponse sendMessage(SendChatMessageRequest request, String userId, String userName) {
        String tenantId = TenantContext.getTenantId();

        DealChatMessage msg = DealChatMessage.builder()
                .opportunityId(request.getOpportunityId())
                .senderId(userId)
                .senderName(userName)
                .message(request.getMessage())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .parentMessageId(request.getParentMessageId())
                .tenantId(tenantId)
                .build();

        msg = chatRepo.save(msg);
        log.info("Chat message sent in deal {} by {}", request.getOpportunityId(), userId);

        eventPublisher.publish("opportunity-events", tenantId, userId,
                "DealChat", msg.getId().toString(), "CHAT_MESSAGE_SENT",
                mapToResponse(msg));

        return mapToResponse(msg);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> getMessages(UUID opportunityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<DealChatMessage> p = chatRepo
                .findByOpportunityIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
                        opportunityId, tenantId, PageRequest.of(page, size));

        return PagedResponse.<ChatMessageResponse>builder()
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
    public List<ChatMessageResponse> getMessagesSince(UUID opportunityId, LocalDateTime since) {
        String tenantId = TenantContext.getTenantId();
        return chatRepo.findByOpportunityIdAndTenantIdAndDeletedFalseAndCreatedAtAfterOrderByCreatedAtAsc(
                opportunityId, tenantId, since)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getReplies(UUID parentMessageId) {
        String tenantId = TenantContext.getTenantId();
        return chatRepo.findByParentMessageIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(
                parentMessageId, tenantId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public ChatMessageResponse editMessage(UUID messageId, String newContent, String userId) {
        String tenantId = TenantContext.getTenantId();
        DealChatMessage msg = chatRepo.findByIdAndTenantIdAndDeletedFalse(messageId, tenantId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!msg.getSenderId().equals(userId)) {
            throw new RuntimeException("Only the sender can edit this message");
        }

        msg.setMessage(newContent);
        msg.setIsEdited(true);
        msg = chatRepo.save(msg);

        return mapToResponse(msg);
    }

    @Transactional
    public void deleteMessage(UUID messageId, String userId) {
        String tenantId = TenantContext.getTenantId();
        DealChatMessage msg = chatRepo.findByIdAndTenantIdAndDeletedFalse(messageId, tenantId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        msg.setDeleted(true);
        chatRepo.save(msg);
    }

    private ChatMessageResponse mapToResponse(DealChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .opportunityId(msg.getOpportunityId())
                .senderId(msg.getSenderId())
                .senderName(msg.getSenderName())
                .message(msg.getMessage())
                .messageType(msg.getMessageType())
                .parentMessageId(msg.getParentMessageId())
                .isEdited(msg.getIsEdited())
                .createdAt(msg.getCreatedAt())
                .updatedAt(msg.getUpdatedAt())
                .build();
    }
}
