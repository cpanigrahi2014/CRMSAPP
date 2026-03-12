package com.crm.notification.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.exception.BadRequestException;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.notification.dto.CallRecordResponse;
import com.crm.notification.dto.InitiateCallRequest;
import com.crm.notification.dto.UpdateCallRequest;
import com.crm.notification.entity.CallRecord;
import com.crm.notification.entity.UnifiedMessage;
import com.crm.notification.repository.CallRecordRepository;
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
public class CallService {

    private final CallRecordRepository callRecordRepository;
    private final UnifiedMessageRepository unifiedMessageRepository;

    @Transactional
    public CallRecordResponse initiate(InitiateCallRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Initiating call to {} for tenant {}", request.getToNumber(), tenantId);

        CallRecord call = CallRecord.builder()
                .fromNumber(request.getFromNumber() != null ? request.getFromNumber() : "+10000000000")
                .toNumber(request.getToNumber())
                .direction(CallRecord.Direction.OUTBOUND)
                .status(CallRecord.CallStatus.INITIATED)
                .startedAt(LocalDateTime.now())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .build();
        call.setTenantId(tenantId);

        CallRecord saved = callRecordRepository.save(call);
        log.info("Call initiated: {} to {}", saved.getId(), request.getToNumber());

        return toResponse(saved);
    }

    @Transactional
    public CallRecordResponse update(UUID callId, UpdateCallRequest request) {
        String tenantId = TenantContext.getTenantId();
        CallRecord call = callRecordRepository.findByIdAndTenantIdAndDeletedFalse(callId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CallRecord", "id", callId));

        if (request.getStatus() != null) {
            call.setStatus(request.getStatus());
            if (request.getStatus() == CallRecord.CallStatus.IN_PROGRESS && call.getAnsweredAt() == null) {
                call.setAnsweredAt(LocalDateTime.now());
            }
            if (request.getStatus() == CallRecord.CallStatus.COMPLETED ||
                request.getStatus() == CallRecord.CallStatus.FAILED ||
                request.getStatus() == CallRecord.CallStatus.NO_ANSWER ||
                request.getStatus() == CallRecord.CallStatus.BUSY) {
                call.setEndedAt(LocalDateTime.now());
                if (call.getAnsweredAt() != null) {
                    long seconds = java.time.Duration.between(call.getAnsweredAt(), call.getEndedAt()).getSeconds();
                    call.setDurationSeconds((int) seconds);
                }
            }
        }
        if (request.getCallOutcome() != null) call.setCallOutcome(request.getCallOutcome());
        if (request.getNotes() != null) call.setNotes(request.getNotes());
        if (request.getRecordingUrl() != null) call.setRecordingUrl(request.getRecordingUrl());
        if (request.getRecordingDurationSeconds() != null) call.setRecordingDurationSeconds(request.getRecordingDurationSeconds());

        CallRecord saved = callRecordRepository.save(call);

        if (call.getEndedAt() != null) {
            indexUnifiedMessage(saved, tenantId);
        }

        return toResponse(saved);
    }

    @Transactional
    public CallRecordResponse endCall(UUID callId) {
        String tenantId = TenantContext.getTenantId();
        CallRecord call = callRecordRepository.findByIdAndTenantIdAndDeletedFalse(callId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CallRecord", "id", callId));

        if (call.getStatus() == CallRecord.CallStatus.COMPLETED) {
            throw new BadRequestException("Call has already ended");
        }

        call.setStatus(CallRecord.CallStatus.COMPLETED);
        call.setEndedAt(LocalDateTime.now());
        if (call.getAnsweredAt() != null) {
            long seconds = java.time.Duration.between(call.getAnsweredAt(), call.getEndedAt()).getSeconds();
            call.setDurationSeconds((int) seconds);
        }

        CallRecord saved = callRecordRepository.save(call);
        indexUnifiedMessage(saved, tenantId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CallRecordResponse getById(UUID id) {
        String tenantId = TenantContext.getTenantId();
        CallRecord call = callRecordRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("CallRecord", "id", id));
        return toResponse(call);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CallRecordResponse> getAll(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<CallRecord> callPage = callRecordRepository.findByTenantIdAndDeletedFalse(
                tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return buildPagedResponse(callPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CallRecordResponse> getByNumber(String number, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<CallRecord> callPage = callRecordRepository.findByTenantIdAndToNumberAndDeletedFalse(
                tenantId, number, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return buildPagedResponse(callPage);
    }

    private void indexUnifiedMessage(CallRecord call, String tenantId) {
        UnifiedMessage unified = new UnifiedMessage();
        unified.setTenantId(tenantId);
        unified.setChannel("CALL");
        unified.setDirection(call.getDirection().name());
        unified.setSender(call.getFromNumber());
        unified.setRecipient(call.getToNumber());
        unified.setBody("Call " + call.getStatus().name().toLowerCase().replace('_', ' ') +
                (call.getDurationSeconds() != null ? " (" + call.getDurationSeconds() + "s)" : ""));
        unified.setStatus(call.getStatus().name());
        unified.setSourceId(call.getId());
        unified.setRelatedEntityType(call.getRelatedEntityType());
        unified.setRelatedEntityId(call.getRelatedEntityId());
        unifiedMessageRepository.save(unified);
    }

    private CallRecordResponse toResponse(CallRecord call) {
        return CallRecordResponse.builder()
                .id(call.getId())
                .fromNumber(call.getFromNumber())
                .toNumber(call.getToNumber())
                .direction(call.getDirection())
                .status(call.getStatus())
                .durationSeconds(call.getDurationSeconds())
                .recordingUrl(call.getRecordingUrl())
                .recordingDurationSeconds(call.getRecordingDurationSeconds())
                .voicemailUrl(call.getVoicemailUrl())
                .callOutcome(call.getCallOutcome())
                .notes(call.getNotes())
                .relatedEntityType(call.getRelatedEntityType())
                .relatedEntityId(call.getRelatedEntityId())
                .startedAt(call.getStartedAt())
                .answeredAt(call.getAnsweredAt())
                .endedAt(call.getEndedAt())
                .tenantId(call.getTenantId())
                .createdAt(call.getCreatedAt())
                .updatedAt(call.getUpdatedAt())
                .build();
    }

    private PagedResponse<CallRecordResponse> buildPagedResponse(Page<CallRecord> page) {
        return PagedResponse.<CallRecordResponse>builder()
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
