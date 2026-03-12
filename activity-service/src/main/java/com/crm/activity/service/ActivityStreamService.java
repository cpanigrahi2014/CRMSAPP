package com.crm.activity.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.security.TenantContext;
import com.crm.activity.dto.ActivityStreamResponse;
import com.crm.activity.entity.ActivityStreamEvent;
import com.crm.activity.repository.ActivityStreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityStreamService {

    private final ActivityStreamRepository streamRepo;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Transactional
    public ActivityStreamResponse recordEvent(String eventType, String entityType, UUID entityId,
                                               String entityName, String description,
                                               String performedBy, String performedByName,
                                               String metadata) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) tenantId = "default";

        ActivityStreamEvent event = ActivityStreamEvent.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .description(description)
                .performedBy(performedBy)
                .performedByName(performedByName)
                .metadata(metadata)
                .tenantId(tenantId)
                .build();

        event = streamRepo.save(event);
        ActivityStreamResponse response = mapToResponse(event);

        // Push to all SSE clients
        pushToClients(response);

        return response;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityStreamResponse> getStream(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) tenantId = "default";
        Page<ActivityStreamEvent> p = streamRepo.findByTenantIdOrderByCreatedAtDesc(
                tenantId, PageRequest.of(page, size));

        return buildPagedResponse(p);
    }

    @Transactional(readOnly = true)
    public List<ActivityStreamResponse> getStreamSince(LocalDateTime since) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) tenantId = "default";
        return streamRepo.findByTenantIdAndCreatedAtAfterOrderByCreatedAtAsc(tenantId, since)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityStreamResponse> getEntityStream(String entityType, UUID entityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) tenantId = "default";
        Page<ActivityStreamEvent> p = streamRepo
                .findByEntityTypeAndEntityIdAndTenantIdOrderByCreatedAtDesc(
                        entityType, entityId, tenantId, PageRequest.of(page, size));

        return buildPagedResponse(p);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(300_000L); // 5-minute timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        log.info("New SSE client subscribed. Total clients: {}", emitters.size());
        return emitter;
    }

    private void pushToClients(ActivityStreamResponse event) {
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("activity")
                        .data(event));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    private PagedResponse<ActivityStreamResponse> buildPagedResponse(Page<ActivityStreamEvent> p) {
        return PagedResponse.<ActivityStreamResponse>builder()
                .content(p.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(p.getNumber())
                .pageSize(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .first(p.isFirst())
                .build();
    }

    private ActivityStreamResponse mapToResponse(ActivityStreamEvent e) {
        return ActivityStreamResponse.builder()
                .id(e.getId())
                .eventType(e.getEventType())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .entityName(e.getEntityName())
                .description(e.getDescription())
                .performedBy(e.getPerformedBy())
                .performedByName(e.getPerformedByName())
                .metadata(e.getMetadata())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
