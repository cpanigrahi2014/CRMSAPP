package com.crm.lead.listener;

import com.crm.common.event.CrmEvent;
import com.crm.common.security.TenantContext;
import com.crm.lead.dto.LeadResponse;
import com.crm.lead.service.LeadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final LeadService leadService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "email-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleEmailEvent(CrmEvent event) {
        try {
            if (event == null || !"EMAIL_RECEIVED".equals(event.getEventType())) return;

            log.info("Received EMAIL_RECEIVED event: entityId={} tenant={}",
                    event.getEntityId(), event.getTenantId());

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event.getPayload(), Map.class);

            String fromAddress = (String) payload.getOrDefault("fromAddress", "");
            String subject = (String) payload.getOrDefault("subject", "");
            String bodyText = (String) payload.getOrDefault("bodyText", "");
            String emailMessageId = (String) payload.getOrDefault("emailMessageId", "");

            if (fromAddress.isBlank()) {
                log.warn("EMAIL_RECEIVED event missing fromAddress, skipping");
                return;
            }

            // Set tenant context for the service call
            TenantContext.setTenantId(event.getTenantId());

            // Auto-create lead from sender email
            LeadResponse lead = leadService.captureEmail(fromAddress, "EMAIL");

            // Record the email as an activity on the lead
            leadService.recordEmailActivity(lead.getId(), event.getTenantId(),
                    fromAddress, subject, bodyText, emailMessageId);

            log.info("Lead auto-created/found from inbound email: leadId={} email={} tenant={}",
                    lead.getId(), fromAddress, event.getTenantId());

        } catch (Exception e) {
            log.error("Error processing EMAIL_RECEIVED event: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
