package com.crm.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, String tenantId, String userId, String entityType,
                        String entityId, String eventType, Object payload) {
        CrmEvent event = CrmEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .tenantId(tenantId)
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(topic, entityId, event);
        log.info("Published event: {} for entity: {} ({})", eventType, entityType, entityId);
    }
}
