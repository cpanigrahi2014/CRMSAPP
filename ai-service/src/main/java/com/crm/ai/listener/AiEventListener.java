package com.crm.ai.listener;

import com.crm.ai.service.LeadScoringService;
import com.crm.common.event.CrmEvent;
import com.crm.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventListener {

    private final LeadScoringService leadScoringService;

    @KafkaListener(topics = "lead-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleLeadEvent(CrmEvent event) {
        log.info("Received lead event: {} for entity: {}", event.getEventType(), event.getEntityId());

        if ("LEAD_CREATED".equals(event.getEventType())) {
            processNewLeadScoring(event);
        }
    }

    @SuppressWarnings("unchecked")
    private void processNewLeadScoring(CrmEvent event) {
        try {
            String tenantId = event.getTenantId();
            UUID leadId = UUID.fromString(event.getEntityId());

            Map<String, Object> leadData = new HashMap<>();
            if (event.getPayload() instanceof Map) {
                leadData.putAll((Map<String, Object>) event.getPayload());
            } else if (event.getPayload() != null) {
                leadData.put("rawPayload", event.getPayload().toString());
            }

            log.info("Triggering async lead scoring for lead: {} in tenant: {}", leadId, tenantId);
            leadScoringService.scoreLeadAsync(leadId, leadData, tenantId);
        } catch (Exception e) {
            log.error("Failed to process lead scoring for event: {}. Error: {}", event.getEventId(), e.getMessage(), e);
        }
    }
}
