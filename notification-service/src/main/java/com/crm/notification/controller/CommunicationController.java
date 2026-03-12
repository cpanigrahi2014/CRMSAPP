package com.crm.notification.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.notification.dto.*;
import com.crm.notification.service.CallService;
import com.crm.notification.service.SmsService;
import com.crm.notification.service.UnifiedInboxService;
import com.crm.notification.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communications")
@RequiredArgsConstructor
@Tag(name = "Communications", description = "Unified communication APIs - SMS, WhatsApp, Calling, Inbox")
public class CommunicationController {

    private final SmsService smsService;
    private final WhatsAppService whatsAppService;
    private final CallService callService;
    private final UnifiedInboxService unifiedInboxService;

    // ─── SMS ─────────────────────────────────────────────

    @PostMapping("/sms/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Send an SMS message")
    public ResponseEntity<ApiResponse<SmsMessageResponse>> sendSms(
            @Valid @RequestBody SendSmsRequest request) {
        SmsMessageResponse response = smsService.send(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "SMS sent successfully"));
    }

    @GetMapping("/sms")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "List SMS messages")
    public ResponseEntity<ApiResponse<PagedResponse<SmsMessageResponse>>> listSms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<SmsMessageResponse> response = smsService.getAll(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get SMS message by ID")
    public ResponseEntity<ApiResponse<SmsMessageResponse>> getSms(@PathVariable UUID id) {
        SmsMessageResponse response = smsService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sms/number/{number}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get SMS messages by phone number")
    public ResponseEntity<ApiResponse<PagedResponse<SmsMessageResponse>>> getSmsByNumber(
            @PathVariable String number,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<SmsMessageResponse> response = smsService.getByNumber(number, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── WhatsApp ────────────────────────────────────────

    @PostMapping("/whatsapp/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Send a WhatsApp message")
    public ResponseEntity<ApiResponse<WhatsAppMessageResponse>> sendWhatsApp(
            @Valid @RequestBody SendWhatsAppRequest request) {
        WhatsAppMessageResponse response = whatsAppService.send(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "WhatsApp message sent successfully"));
    }

    @GetMapping("/whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "List WhatsApp messages")
    public ResponseEntity<ApiResponse<PagedResponse<WhatsAppMessageResponse>>> listWhatsApp(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<WhatsAppMessageResponse> response = whatsAppService.getAll(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/whatsapp/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get WhatsApp message by ID")
    public ResponseEntity<ApiResponse<WhatsAppMessageResponse>> getWhatsApp(@PathVariable UUID id) {
        WhatsAppMessageResponse response = whatsAppService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/whatsapp/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Mark WhatsApp message as read")
    public ResponseEntity<ApiResponse<WhatsAppMessageResponse>> markWhatsAppRead(@PathVariable UUID id) {
        WhatsAppMessageResponse response = whatsAppService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Marked as read"));
    }

    @GetMapping("/whatsapp/number/{number}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get WhatsApp messages by phone number")
    public ResponseEntity<ApiResponse<PagedResponse<WhatsAppMessageResponse>>> getWhatsAppByNumber(
            @PathVariable String number,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<WhatsAppMessageResponse> response = whatsAppService.getByNumber(number, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Calls ───────────────────────────────────────────

    @PostMapping("/calls/initiate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Initiate a call")
    public ResponseEntity<ApiResponse<CallRecordResponse>> initiateCall(
            @Valid @RequestBody InitiateCallRequest request) {
        CallRecordResponse response = callService.initiate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Call initiated"));
    }

    @PutMapping("/calls/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Update call record")
    public ResponseEntity<ApiResponse<CallRecordResponse>> updateCall(
            @PathVariable UUID id,
            @RequestBody UpdateCallRequest request) {
        CallRecordResponse response = callService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Call updated"));
    }

    @PostMapping("/calls/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "End an active call")
    public ResponseEntity<ApiResponse<CallRecordResponse>> endCall(@PathVariable UUID id) {
        CallRecordResponse response = callService.endCall(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Call ended"));
    }

    @GetMapping("/calls")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "List call records")
    public ResponseEntity<ApiResponse<PagedResponse<CallRecordResponse>>> listCalls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CallRecordResponse> response = callService.getAll(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/calls/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get call record by ID")
    public ResponseEntity<ApiResponse<CallRecordResponse>> getCall(@PathVariable UUID id) {
        CallRecordResponse response = callService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/calls/number/{number}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get call records by phone number")
    public ResponseEntity<ApiResponse<PagedResponse<CallRecordResponse>>> getCallsByNumber(
            @PathVariable String number,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CallRecordResponse> response = callService.getByNumber(number, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Unified Inbox ──────────────────────────────────

    @GetMapping("/inbox")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get unified inbox across all channels")
    public ResponseEntity<ApiResponse<PagedResponse<UnifiedInboxResponse>>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<UnifiedInboxResponse> response = unifiedInboxService.getInbox(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inbox/channel/{channel}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get inbox items by channel (EMAIL, SMS, WHATSAPP, CALL)")
    public ResponseEntity<ApiResponse<PagedResponse<UnifiedInboxResponse>>> getInboxByChannel(
            @PathVariable String channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<UnifiedInboxResponse> response = unifiedInboxService.getByChannel(channel, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inbox/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get inbox items related to a specific entity")
    public ResponseEntity<ApiResponse<PagedResponse<UnifiedInboxResponse>>> getInboxByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<UnifiedInboxResponse> response = unifiedInboxService.getByEntity(entityType, entityId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
