package com.crm.supportcase.listener;

import com.crm.common.event.CrmEvent;
import com.crm.supportcase.service.OmniChannelRoutingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.crm.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingEventListener {

    private final OmniChannelRoutingService routingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "case-events", groupId = "case-routing-group")
    public void handleCaseEvent(CrmEvent event) {
        try {
            if (!"CASE_CREATED".equals(event.getEventType())) {
                return;
            }

            log.info("Received CASE_CREATED event for entity {}", event.getEntityId());

            TenantContext.setTenantId(event.getTenantId());

            UUID caseId = UUID.fromString(event.getEntityId());
            routingService.routeCaseById(caseId, event.getTenantId());

            log.info("Successfully routed case {} through omnichannel routing", event.getEntityId());
        } catch (IllegalArgumentException e) {
            log.warn("Could not route case from event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing case routing event: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
