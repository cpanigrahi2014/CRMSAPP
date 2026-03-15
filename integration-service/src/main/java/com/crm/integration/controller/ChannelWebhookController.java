package com.crm.integration.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.security.TenantContext;
import com.crm.integration.dto.*;
import com.crm.integration.service.ChannelIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/integrations/channels")
@RequiredArgsConstructor
@Tag(name = "Channel Integrations", description = "WhatsApp, Email, Social Media integration webhooks")
public class ChannelWebhookController {

    private final ChannelIntegrationService channelService;

    // ══════════════════════════════════════════════════════════
    // Scenario 18: WhatsApp → Lead → Opportunity
    // ══════════════════════════════════════════════════════════

    @PostMapping("/whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Receive WhatsApp message webhook",
            description = "Creates a lead from a WhatsApp message. Optionally auto-converts to opportunity with transcript attached.")
    public ResponseEntity<ApiResponse<WhatsAppWebhookResponse>> receiveWhatsApp(
            @Valid @RequestBody WhatsAppWebhookRequest request,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        String tenantId = TenantContext.getTenantId();

        log.info("WhatsApp webhook received: phone={} tenant={} autoConvert={}",
                request.getPhone(), tenantId, request.isAutoConvert());

        WhatsAppWebhookResponse response = channelService.processWhatsAppMessage(request, token, tenantId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "WhatsApp message processed"));
    }

    // ══════════════════════════════════════════════════════════
    // Scenario 19: Email → Case
    // ══════════════════════════════════════════════════════════

    @PostMapping("/email-support")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Receive inbound support email webhook",
            description = "Auto-creates a support case from an inbound email to support@. Email is attached as activity.")
    public ResponseEntity<ApiResponse<EmailSupportWebhookResponse>> receiveEmailSupport(
            @Valid @RequestBody EmailSupportWebhookRequest request,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        String tenantId = TenantContext.getTenantId();

        log.info("Email support webhook received: from={} to={} tenant={}",
                request.getFromAddress(), request.getToAddress(), tenantId);

        EmailSupportWebhookResponse response = channelService.processInboundSupportEmail(request, token, tenantId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Support case created from email"));
    }

    // ══════════════════════════════════════════════════════════
    // Scenario 20: Instagram/Social → Lead
    // ══════════════════════════════════════════════════════════

    @PostMapping("/social")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Receive social media comment webhook",
            description = "Creates a lead from a social media comment (Instagram, Facebook, LinkedIn, Twitter). Lead score = social weight.")
    public ResponseEntity<ApiResponse<SocialMediaWebhookResponse>> receiveSocialMedia(
            @Valid @RequestBody SocialMediaWebhookRequest request,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        String tenantId = TenantContext.getTenantId();

        log.info("Social media webhook received: platform={} user=@{} tenant={}",
                request.getPlatform(), request.getUsername(), tenantId);

        SocialMediaWebhookResponse response = channelService.processSocialMediaComment(request, token, tenantId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Lead created from " + request.getPlatform()));
    }

    // ══════════════════════════════════════════════════════════

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return "";
    }
}
