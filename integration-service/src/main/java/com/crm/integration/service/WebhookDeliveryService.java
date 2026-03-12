package com.crm.integration.service;

import com.crm.common.security.TenantContext;
import com.crm.integration.dto.WebhookDeliveryLogResponse;
import com.crm.integration.entity.WebhookConfig;
import com.crm.integration.entity.WebhookDeliveryLog;
import com.crm.integration.repository.WebhookConfigRepository;
import com.crm.integration.repository.WebhookDeliveryLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final WebhookConfigRepository webhookRepo;
    private final WebhookDeliveryLogRepository deliveryLogRepo;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public List<WebhookDeliveryLogResponse> getDeliveryLogs() {
        String tenantId = TenantContext.getTenantId();
        return deliveryLogRepo.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryLogResponse> getDeliveryLogsByWebhook(UUID webhookId) {
        String tenantId = TenantContext.getTenantId();
        return deliveryLogRepo.findByWebhook_IdAndTenantIdOrderByCreatedAtDesc(webhookId, tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Async
    public void triggerWebhooks(String tenantId, String eventType, Map<String, Object> payload) {
        log.info("Triggering webhooks for event: {} in tenant: {}", eventType, tenantId);
        List<WebhookConfig> webhooks = webhookRepo.findByTenantIdOrderByCreatedAtDesc(tenantId);

        for (WebhookConfig webhook : webhooks) {
            if (!webhook.isActive()) continue;
            List<String> events = parseJsonList(webhook.getEvents());
            if (events.contains(eventType) || events.contains("*")) {
                deliverWebhook(webhook, tenantId, eventType, payload, 1);
            }
        }
    }

    private void deliverWebhook(WebhookConfig webhook, String tenantId, String eventType,
                                 Map<String, Object> payload, int attempt) {
        String payloadJson;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("event", eventType);
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("data", payload);
            payloadJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload", e);
            return;
        }

        WebhookDeliveryLog deliveryLog = WebhookDeliveryLog.builder()
                .webhook(webhook)
                .tenantId(tenantId)
                .eventType(eventType)
                .payload(payloadJson)
                .attempt(attempt)
                .status("PENDING")
                .build();
        deliveryLog = deliveryLogRepo.save(deliveryLog);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Event", eventType);
            headers.set("X-Webhook-Delivery", deliveryLog.getId().toString());

            if (webhook.getSecretKey() != null && !webhook.getSecretKey().isBlank()) {
                String signature = computeHmacSha256(payloadJson, webhook.getSecretKey());
                headers.set("X-Webhook-Signature", "sha256=" + signature);
            }

            HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    webhook.getUrl(), HttpMethod.POST, request, String.class);

            deliveryLog.setResponseStatus(response.getStatusCode().value());
            deliveryLog.setResponseBody(truncate(response.getBody(), 2000));
            deliveryLog.setStatus("SUCCESS");
            deliveryLog.setDeliveredAt(LocalDateTime.now());
            deliveryLogRepo.save(deliveryLog);

            webhook.setSuccessCount(webhook.getSuccessCount() + 1);
            webhook.setLastTriggeredAt(LocalDateTime.now());
            webhookRepo.save(webhook);

            log.info("Webhook delivered successfully: {} -> {}", eventType, webhook.getUrl());

        } catch (Exception e) {
            log.warn("Webhook delivery failed (attempt {}): {} -> {}: {}",
                    attempt, eventType, webhook.getUrl(), e.getMessage());

            deliveryLog.setStatus(attempt < webhook.getRetryCount() ? "RETRYING" : "FAILED");
            deliveryLog.setErrorMessage(truncate(e.getMessage(), 2000));
            deliveryLogRepo.save(deliveryLog);

            if (attempt < webhook.getRetryCount()) {
                try {
                    long delay = (long) webhook.getRetryDelayMs() * attempt;
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                deliverWebhook(webhook, tenantId, eventType, payload, attempt + 1);
            } else {
                webhook.setFailureCount(webhook.getFailureCount() + 1);
                webhook.setLastTriggeredAt(LocalDateTime.now());
                webhookRepo.save(webhook);
            }
        }
    }

    @Transactional
    public WebhookDeliveryLogResponse testWebhook(UUID webhookId) {
        String tenantId = TenantContext.getTenantId();
        WebhookConfig webhook = webhookRepo.findByIdAndTenantId(webhookId, tenantId)
                .orElseThrow(() -> new com.crm.common.exception.ResourceNotFoundException("Webhook not found: " + webhookId));

        Map<String, Object> testPayload = Map.of(
                "test", true,
                "message", "This is a test webhook delivery",
                "webhookId", webhookId.toString()
        );

        String payloadJson;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("event", "webhook.test");
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("data", testPayload);
            payloadJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test payload", e);
        }

        WebhookDeliveryLog deliveryLog = WebhookDeliveryLog.builder()
                .webhook(webhook)
                .tenantId(tenantId)
                .eventType("webhook.test")
                .payload(payloadJson)
                .attempt(1)
                .status("PENDING")
                .build();
        deliveryLog = deliveryLogRepo.save(deliveryLog);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Event", "webhook.test");
            headers.set("X-Webhook-Delivery", deliveryLog.getId().toString());

            if (webhook.getSecretKey() != null && !webhook.getSecretKey().isBlank()) {
                String signature = computeHmacSha256(payloadJson, webhook.getSecretKey());
                headers.set("X-Webhook-Signature", "sha256=" + signature);
            }

            HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    webhook.getUrl(), HttpMethod.POST, request, String.class);

            deliveryLog.setResponseStatus(response.getStatusCode().value());
            deliveryLog.setResponseBody(truncate(response.getBody(), 2000));
            deliveryLog.setStatus("SUCCESS");
            deliveryLog.setDeliveredAt(LocalDateTime.now());

        } catch (Exception e) {
            deliveryLog.setStatus("FAILED");
            deliveryLog.setErrorMessage(truncate(e.getMessage(), 2000));
        }

        return toResponse(deliveryLogRepo.save(deliveryLog));
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private WebhookDeliveryLogResponse toResponse(WebhookDeliveryLog e) {
        return WebhookDeliveryLogResponse.builder()
                .id(e.getId())
                .webhookId(e.getWebhook().getId())
                .webhookName(e.getWebhook().getName())
                .eventType(e.getEventType())
                .payload(e.getPayload())
                .responseStatus(e.getResponseStatus())
                .responseBody(e.getResponseBody())
                .attempt(e.getAttempt())
                .status(e.getStatus())
                .errorMessage(e.getErrorMessage())
                .deliveredAt(e.getDeliveredAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
