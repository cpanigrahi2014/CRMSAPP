package com.crm.integration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaWebhookListener {

    private final WebhookDeliveryService webhookDeliveryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {
            "lead-events", "contact-events", "account-events",
            "opportunity-events", "activity-events", "workflow-events"
    }, groupId = "webhook-delivery-group")
    public void handleEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, new TypeReference<>() {});
            String tenantId = (String) event.getOrDefault("tenantId", "default");
            String eventType = (String) event.getOrDefault("eventType", "unknown");

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getOrDefault("payload", event);

            log.info("Received event for webhook delivery: {} (tenant: {})", eventType, tenantId);
            webhookDeliveryService.triggerWebhooks(tenantId, eventType, payload);

        } catch (Exception e) {
            log.error("Failed to process webhook event: {}", e.getMessage(), e);
        }
    }
}
