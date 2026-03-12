package com.crm.workflow.listener;

import com.crm.common.event.CrmEvent;
import com.crm.common.security.TenantContext;
import com.crm.workflow.service.WorkflowEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEventListener {

    private final WorkflowEngine workflowEngine;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"lead-events", "opportunity-events", "contact-events", "account-events", "activity-events"},
            groupId = "workflow-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCrmEvent(CrmEvent event) {
        if (event == null) {
            log.warn("Received null event, skipping");
            return;
        }

        log.info("Received CRM event: type={}, entity={}, entityId={}, tenant={}",
                event.getEventType(), event.getEntityType(), event.getEntityId(), event.getTenantId());

        try {
            TenantContext.setTenantId(event.getTenantId());

            String entityType = event.getEntityType();
            String triggerEvent = mapEventTypeToTrigger(event.getEventType());
            String entityId = event.getEntityId();
            Map<String, Object> entityData = extractEntityData(event);

            workflowEngine.evaluateRules(event.getTenantId(), entityType, triggerEvent, entityId, entityData);

        } catch (Exception e) {
            log.error("Error processing CRM event: type={}, entityId={}", event.getEventType(), event.getEntityId(), e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Maps specific event types (e.g., "LEAD_CREATED", "OPPORTUNITY_STAGE_CHANGED") to
     * generic trigger events (e.g., "CREATED", "STAGE_CHANGED") that workflow rules use.
     */
    private String mapEventTypeToTrigger(String eventType) {
        if (eventType == null) return "UNKNOWN";

        // Strip entity prefix: "LEAD_CREATED" -> "CREATED", "OPPORTUNITY_UPDATED" -> "UPDATED"
        String[] parts = eventType.split("_", 2);
        if (parts.length > 1) {
            return parts[1]; // Return everything after the first underscore
        }
        return eventType;
    }

    /**
     * Extracts entity data from the event payload as a flat Map for condition evaluation.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractEntityData(CrmEvent event) {
        Map<String, Object> data = new HashMap<>();

        // Add event metadata
        data.put("_eventType", event.getEventType());
        data.put("_entityType", event.getEntityType());
        data.put("_entityId", event.getEntityId());
        data.put("_tenantId", event.getTenantId());

        if (event.getPayload() != null) {
            try {
                if (event.getPayload() instanceof Map) {
                    data.putAll((Map<String, Object>) event.getPayload());
                } else {
                    // Convert the payload object to a Map
                    Map<String, Object> payloadMap = objectMapper.convertValue(
                            event.getPayload(), new TypeReference<Map<String, Object>>() {});
                    data.putAll(payloadMap);
                }
            } catch (Exception e) {
                log.warn("Could not convert event payload to map: {}", e.getMessage());
                data.put("_rawPayload", String.valueOf(event.getPayload()));
            }
        }

        return data;
    }
}
