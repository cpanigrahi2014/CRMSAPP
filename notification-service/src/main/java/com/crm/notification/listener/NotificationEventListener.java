package com.crm.notification.listener;

import com.crm.common.event.CrmEvent;
import com.crm.common.security.TenantContext;
import com.crm.notification.dto.CreateNotificationRequest;
import com.crm.notification.entity.Notification;
import com.crm.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "lead-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleLeadEvent(CrmEvent event) {
        log.info("Received lead event: {} for entity: {}", event.getEventType(), event.getEntityId());
        processEvent(event, "Lead");
    }

    @KafkaListener(topics = "contact-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleContactEvent(CrmEvent event) {
        log.info("Received contact event: {} for entity: {}", event.getEventType(), event.getEntityId());
        processEvent(event, "Contact");
    }

    @KafkaListener(topics = "opportunity-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOpportunityEvent(CrmEvent event) {
        log.info("Received opportunity event: {} for entity: {}", event.getEventType(), event.getEntityId());
        processEvent(event, "Opportunity");
    }

    @KafkaListener(topics = "activity-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleActivityEvent(CrmEvent event) {
        log.info("Received activity event: {} for entity: {}", event.getEventType(), event.getEntityId());
        processEvent(event, "Activity");
    }

    private void processEvent(CrmEvent event, String entityType) {
        try {
            TenantContext.setTenantId(event.getTenantId());

            String subject = buildSubject(event.getEventType(), entityType);
            String body = buildBody(event);
            String recipient = resolveRecipient(event);

            if (recipient == null || recipient.isBlank()) {
                log.warn("No recipient resolved for event: {}, skipping notification", event.getEventId());
                return;
            }

            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .type(Notification.NotificationType.IN_APP)
                    .recipient(recipient)
                    .subject(subject)
                    .body(body)
                    .relatedEntityType(entityType)
                    .relatedEntityId(parseUuid(event.getEntityId()))
                    .build();

            notificationService.create(request, event.getUserId());
            log.info("Notification created for event: {} ({})", event.getEventType(), event.getEventId());
        } catch (Exception e) {
            log.error("Failed to process event: {} for entity: {}", event.getEventType(), event.getEntityId(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private String buildSubject(String eventType, String entityType) {
        if ("ACTIVITY_REMINDER".equals(eventType)) {
            return "Reminder: You have a pending " + entityType.toLowerCase();
        }
        return entityType + " - " + eventType.replace("_", " ").toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private String buildBody(CrmEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Event: ").append(event.getEventType()).append("\n");
        sb.append("Entity: ").append(event.getEntityType()).append(" (").append(event.getEntityId()).append(")\n");
        sb.append("Time: ").append(event.getTimestamp()).append("\n");

        if (event.getPayload() instanceof Map) {
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            payload.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String resolveRecipient(CrmEvent event) {
        if (event.getPayload() instanceof Map) {
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            if (payload.containsKey("email")) {
                return (String) payload.get("email");
            }
            if (payload.containsKey("assignedTo")) {
                return (String) payload.get("assignedTo");
            }
        }
        return event.getUserId();
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            return null;
        }
    }
}
