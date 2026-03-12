package com.crm.email.listener;

import com.crm.common.event.CrmEvent;
import com.crm.email.dto.SendEmailRequest;
import com.crm.email.service.EmailSendService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens to the workflow-actions Kafka topic.
 * When the workflow-engine publishes a SEND_EMAIL action, this consumer
 * picks it up and sends the email via EmailSendService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowActionListener {

    private final EmailSendService emailSendService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "workflow-actions", groupId = "email-service-group")
    public void handleWorkflowAction(CrmEvent event) {
        try {
            if (event == null || event.getPayload() == null) return;

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event.getPayload(), Map.class);

            String actionType = (String) payload.get("actionType");
            if (!"SEND_EMAIL".equals(actionType)) return;

            log.info("Received SEND_EMAIL workflow action: event={}, entity={}/{}",
                    event.getEventId(), event.getEntityType(), event.getEntityId());

            String recipient     = (String) payload.getOrDefault("recipient", "");
            String templateOrBody = (String) payload.getOrDefault("templateOrBody", "");

            if (recipient.isBlank()) {
                log.warn("SEND_EMAIL action missing recipient, skipping. eventId={}", event.getEventId());
                return;
            }

            SendEmailRequest req = new SendEmailRequest();
            req.setTo(recipient);
            req.setSubject("CRM Notification - " + event.getEntityType());
            req.setBodyHtml(templateOrBody.isBlank()
                    ? "<p>Automated notification for " + event.getEntityType() + " " + event.getEntityId() + "</p>"
                    : templateOrBody);
            req.setRelatedEntityType(event.getEntityType());
            if (event.getEntityId() != null && !event.getEntityId().isBlank()) {
                try { req.setRelatedEntityId(java.util.UUID.fromString(event.getEntityId())); } catch (Exception ignored) {}
            }
            req.setTrackOpens(true);
            req.setTrackClicks(true);

            emailSendService.send(
                    event.getTenantId(),
                    event.getUserId() != null ? event.getUserId() : "system",
                    req
            );

            log.info("Workflow email sent to {} for {}/{}", recipient, event.getEntityType(), event.getEntityId());

        } catch (Exception e) {
            log.error("Error processing workflow action: {}", e.getMessage(), e);
        }
    }
}
