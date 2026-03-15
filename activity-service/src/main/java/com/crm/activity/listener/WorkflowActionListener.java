package com.crm.activity.listener;

import com.crm.activity.dto.CreateActivityRequest;
import com.crm.activity.entity.Activity;
import com.crm.activity.service.ActivityService;
import com.crm.common.event.CrmEvent;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowActionListener {

    private final ActivityService activityService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "workflow-actions",
            groupId = "activity-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleWorkflowAction(CrmEvent event) {
        if (event == null) {
            log.warn("Received null workflow action event, skipping");
            return;
        }

        log.info("Received workflow action: type={}, entityId={}, tenant={}",
                event.getEventType(), event.getEntityId(), event.getTenantId());

        try {
            TenantContext.setTenantId(event.getTenantId());

            if ("CREATE_TASK".equals(event.getEventType())) {
                handleCreateTask(event);
            } else {
                log.info("Ignoring workflow action type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing workflow action: type={}, entityId={}",
                    event.getEventType(), event.getEntityId(), e);
        } finally {
            TenantContext.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCreateTask(CrmEvent event) {
        Map<String, Object> payload = extractPayload(event);
        if (payload == null) {
            log.warn("CREATE_TASK event has no payload, skipping");
            return;
        }

        String taskSubject = (String) payload.getOrDefault("taskSubject", "Follow-up Task");
        String taskDescription = (String) payload.getOrDefault("taskDescription", "Auto-created by workflow rule");
        String entityId = (String) payload.getOrDefault("entityId", event.getEntityId());

        // Determine entity type from nested entityData
        String relatedEntityType = "Lead";
        Map<String, Object> entityData = null;
        if (payload.get("entityData") instanceof Map) {
            entityData = (Map<String, Object>) payload.get("entityData");
        }

        CreateActivityRequest request = CreateActivityRequest.builder()
                .type(Activity.ActivityType.TASK)
                .subject(taskSubject)
                .description(taskDescription)
                .priority(Activity.ActivityPriority.HIGH)
                .dueDate(LocalDateTime.now().plusDays(1))
                .reminderAt(LocalDateTime.now().plusHours(2))
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(parseUuid(entityId))
                .assignedTo(parseUuid(event.getUserId()))
                .build();

        activityService.createActivity(request, event.getUserId() != null ? event.getUserId() : "workflow-engine");

        log.info("Auto-created follow-up task '{}' for entity {} ({})",
                taskSubject, entityId, relatedEntityType);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(CrmEvent event) {
        if (event.getPayload() == null) return null;
        if (event.getPayload() instanceof Map) {
            return (Map<String, Object>) event.getPayload();
        }
        try {
            return objectMapper.convertValue(event.getPayload(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to extract payload from event", e);
            return null;
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
