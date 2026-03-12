package com.crm.notification.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.notification.dto.SendWhatsAppRequest;
import com.crm.notification.dto.WhatsAppMessageResponse;
import com.crm.notification.entity.UnifiedMessage;
import com.crm.notification.entity.WhatsAppMessage;
import com.crm.notification.repository.UnifiedMessageRepository;
import com.crm.notification.repository.WhatsAppMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final WhatsAppMessageRepository whatsAppMessageRepository;
    private final UnifiedMessageRepository unifiedMessageRepository;

    @Transactional
    public WhatsAppMessageResponse send(SendWhatsAppRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Sending WhatsApp message to {} for tenant {}", request.getToNumber(), tenantId);

        WhatsAppMessage msg = WhatsAppMessage.builder()
                .fromNumber(request.getFromNumber() != null ? request.getFromNumber() : "+10000000000")
                .toNumber(request.getToNumber())
                .body(request.getBody())
                .mediaUrl(request.getMediaUrl())
                .mediaType(request.getMediaType())
                .messageType(request.getMessageType() != null ? request.getMessageType() : WhatsAppMessage.MessageType.TEXT)
                .direction(WhatsAppMessage.Direction.OUTBOUND)
                .status(WhatsAppMessage.WaStatus.SENT)
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .build();
        msg.setTenantId(tenantId);

        WhatsAppMessage saved = whatsAppMessageRepository.save(msg);
        indexUnifiedMessage(saved, tenantId);
        log.info("WhatsApp message sent: {} to {}", saved.getId(), request.getToNumber());

        return toResponse(saved);
    }

    @Transactional
    public WhatsAppMessageResponse receiveInbound(String fromNumber, String toNumber, String body,
                                                   String mediaUrl, String mediaType,
                                                   WhatsAppMessage.MessageType messageType) {
        String tenantId = TenantContext.getTenantId();
        log.info("Receiving inbound WhatsApp from {} for tenant {}", fromNumber, tenantId);

        WhatsAppMessage msg = WhatsAppMessage.builder()
                .fromNumber(fromNumber)
                .toNumber(toNumber)
                .body(body)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .messageType(messageType != null ? messageType : WhatsAppMessage.MessageType.TEXT)
                .direction(WhatsAppMessage.Direction.INBOUND)
                .status(WhatsAppMessage.WaStatus.DELIVERED)
                .build();
        msg.setTenantId(tenantId);

        WhatsAppMessage saved = whatsAppMessageRepository.save(msg);
        indexUnifiedMessage(saved, tenantId);
        return toResponse(saved);
    }

    @Transactional
    public WhatsAppMessageResponse markAsRead(UUID id) {
        String tenantId = TenantContext.getTenantId();
        WhatsAppMessage msg = whatsAppMessageRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsAppMessage", "id", id));
        msg.setStatus(WhatsAppMessage.WaStatus.READ);
        msg.setReadAt(LocalDateTime.now());
        whatsAppMessageRepository.save(msg);
        return toResponse(msg);
    }

    @Transactional(readOnly = true)
    public WhatsAppMessageResponse getById(UUID id) {
        String tenantId = TenantContext.getTenantId();
        WhatsAppMessage msg = whatsAppMessageRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsAppMessage", "id", id));
        return toResponse(msg);
    }

    @Transactional(readOnly = true)
    public PagedResponse<WhatsAppMessageResponse> getAll(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<WhatsAppMessage> msgPage = whatsAppMessageRepository.findByTenantIdAndDeletedFalse(
                tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return buildPagedResponse(msgPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<WhatsAppMessageResponse> getByNumber(String number, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<WhatsAppMessage> msgPage = whatsAppMessageRepository.findByTenantIdAndToNumberAndDeletedFalse(
                tenantId, number, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return buildPagedResponse(msgPage);
    }

    private void indexUnifiedMessage(WhatsAppMessage msg, String tenantId) {
        UnifiedMessage unified = new UnifiedMessage();
        unified.setTenantId(tenantId);
        unified.setChannel("WHATSAPP");
        unified.setDirection(msg.getDirection().name());
        unified.setSender(msg.getFromNumber());
        unified.setRecipient(msg.getToNumber());
        unified.setBody(msg.getBody());
        unified.setStatus(msg.getStatus().name());
        unified.setSourceId(msg.getId());
        unified.setRelatedEntityType(msg.getRelatedEntityType());
        unified.setRelatedEntityId(msg.getRelatedEntityId());
        unifiedMessageRepository.save(unified);
    }

    private WhatsAppMessageResponse toResponse(WhatsAppMessage msg) {
        return WhatsAppMessageResponse.builder()
                .id(msg.getId())
                .fromNumber(msg.getFromNumber())
                .toNumber(msg.getToNumber())
                .body(msg.getBody())
                .mediaUrl(msg.getMediaUrl())
                .mediaType(msg.getMediaType())
                .messageType(msg.getMessageType())
                .direction(msg.getDirection())
                .status(msg.getStatus())
                .externalId(msg.getExternalId())
                .relatedEntityType(msg.getRelatedEntityType())
                .relatedEntityId(msg.getRelatedEntityId())
                .readAt(msg.getReadAt())
                .tenantId(msg.getTenantId())
                .createdAt(msg.getCreatedAt())
                .updatedAt(msg.getUpdatedAt())
                .build();
    }

    private PagedResponse<WhatsAppMessageResponse> buildPagedResponse(Page<WhatsAppMessage> page) {
        return PagedResponse.<WhatsAppMessageResponse>builder()
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
