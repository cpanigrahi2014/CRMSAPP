package com.crm.notification.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.notification.dto.SendSmsRequest;
import com.crm.notification.dto.SmsMessageResponse;
import com.crm.notification.entity.SmsMessage;
import com.crm.notification.entity.UnifiedMessage;
import com.crm.notification.repository.SmsMessageRepository;
import com.crm.notification.repository.UnifiedMessageRepository;
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
public class SmsService {

    private final SmsMessageRepository smsMessageRepository;
    private final UnifiedMessageRepository unifiedMessageRepository;

    @Transactional
    public SmsMessageResponse send(SendSmsRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Sending SMS to {} for tenant {}", request.getToNumber(), tenantId);

        SmsMessage sms = SmsMessage.builder()
                .fromNumber(request.getFromNumber() != null ? request.getFromNumber() : "+10000000000")
                .toNumber(request.getToNumber())
                .body(request.getBody())
                .direction(SmsMessage.Direction.OUTBOUND)
                .status(SmsMessage.SmsStatus.SENT)
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .sentAt(LocalDateTime.now())
                .build();
        sms.setTenantId(tenantId);

        SmsMessage saved = smsMessageRepository.save(sms);
        indexUnifiedMessage(saved, tenantId);
        log.info("SMS sent: {} to {}", saved.getId(), request.getToNumber());

        return toResponse(saved);
    }

    @Transactional
    public SmsMessageResponse receiveInbound(String fromNumber, String toNumber, String body) {
        String tenantId = TenantContext.getTenantId();
        log.info("Receiving inbound SMS from {} for tenant {}", fromNumber, tenantId);

        SmsMessage sms = SmsMessage.builder()
                .fromNumber(fromNumber)
                .toNumber(toNumber)
                .body(body)
                .direction(SmsMessage.Direction.INBOUND)
                .status(SmsMessage.SmsStatus.RECEIVED)
                .build();
        sms.setTenantId(tenantId);

        SmsMessage saved = smsMessageRepository.save(sms);
        indexUnifiedMessage(saved, tenantId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SmsMessageResponse getById(UUID id) {
        String tenantId = TenantContext.getTenantId();
        SmsMessage sms = smsMessageRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SmsMessage", "id", id));
        return toResponse(sms);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SmsMessageResponse> getAll(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<SmsMessage> smsPage = smsMessageRepository.findByTenantIdAndDeletedFalse(
                tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return buildPagedResponse(smsPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SmsMessageResponse> getByNumber(String number, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<SmsMessage> smsPage = smsMessageRepository.findByTenantIdAndToNumberAndDeletedFalse(
                tenantId, number, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return buildPagedResponse(smsPage);
    }

    private void indexUnifiedMessage(SmsMessage sms, String tenantId) {
        UnifiedMessage unified = new UnifiedMessage();
        unified.setTenantId(tenantId);
        unified.setChannel("SMS");
        unified.setDirection(sms.getDirection().name());
        unified.setSender(sms.getFromNumber());
        unified.setRecipient(sms.getToNumber());
        unified.setBody(sms.getBody());
        unified.setStatus(sms.getStatus().name());
        unified.setSourceId(sms.getId());
        unified.setRelatedEntityType(sms.getRelatedEntityType());
        unified.setRelatedEntityId(sms.getRelatedEntityId());
        unifiedMessageRepository.save(unified);
    }

    private SmsMessageResponse toResponse(SmsMessage sms) {
        return SmsMessageResponse.builder()
                .id(sms.getId())
                .fromNumber(sms.getFromNumber())
                .toNumber(sms.getToNumber())
                .body(sms.getBody())
                .direction(sms.getDirection())
                .status(sms.getStatus())
                .externalId(sms.getExternalId())
                .errorMessage(sms.getErrorMessage())
                .relatedEntityType(sms.getRelatedEntityType())
                .relatedEntityId(sms.getRelatedEntityId())
                .sentAt(sms.getSentAt())
                .deliveredAt(sms.getDeliveredAt())
                .tenantId(sms.getTenantId())
                .createdAt(sms.getCreatedAt())
                .updatedAt(sms.getUpdatedAt())
                .build();
    }

    private PagedResponse<SmsMessageResponse> buildPagedResponse(Page<SmsMessage> page) {
        return PagedResponse.<SmsMessageResponse>builder()
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
