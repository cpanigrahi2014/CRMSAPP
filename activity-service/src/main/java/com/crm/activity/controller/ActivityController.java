package com.crm.activity.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import com.crm.activity.dto.*;
import com.crm.activity.entity.Activity;
import com.crm.activity.service.ActivityService;
import com.crm.activity.service.ActivityStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
@Tag(name = "Activities", description = "Activity management APIs")
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityStreamService activityStreamService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Create a new activity")
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(
            @Valid @RequestBody CreateActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ActivityResponse response = activityService.createActivity(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Activity created successfully"));
    }

    @PutMapping("/{activityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Update an existing activity")
    public ResponseEntity<ApiResponse<ActivityResponse>> updateActivity(
            @PathVariable UUID activityId,
            @Valid @RequestBody UpdateActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ActivityResponse response = activityService.updateActivity(activityId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Activity updated successfully"));
    }

    @GetMapping("/{activityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activity by ID")
    public ResponseEntity<ApiResponse<ActivityResponse>> getActivityById(@PathVariable UUID activityId) {
        ActivityResponse response = activityService.getActivityById(activityId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all activities with pagination and optional filters")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityResponse>>> getAllActivities(
            @RequestParam(required = false) Activity.ActivityType type,
            @RequestParam(required = false) Activity.ActivityStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PagedResponse<ActivityResponse> response;
        if (type != null && status != null) {
            response = activityService.getActivitiesByTypeAndStatus(type, status, page, size, sortBy, sortDir);
        } else if (type != null) {
            response = activityService.getActivitiesByType(type, page, size, sortBy, sortDir);
        } else if (status != null) {
            response = activityService.getActivitiesByStatus(status, page, size, sortBy, sortDir);
        } else {
            response = activityService.getAllActivities(page, size, sortBy, sortDir);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Search activities")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityResponse>>> searchActivities(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ActivityResponse> response = activityService.searchActivities(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/related/{relatedEntityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activities by related entity ID")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityResponse>>> getByRelatedEntityId(
            @PathVariable UUID relatedEntityId,
            @RequestParam(required = false) String relatedEntityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<ActivityResponse> response;
        if (relatedEntityType != null) {
            response = activityService.getByRelatedEntity(relatedEntityType, relatedEntityId, page, size);
        } else {
            response = activityService.getByRelatedEntityId(relatedEntityId, page, size);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/assignee/{assignedTo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activities by assignee")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityResponse>>> getByAssignee(
            @PathVariable UUID assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ActivityResponse> response = activityService.getByAssignee(assignedTo, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{activityId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Mark activity as complete")
    public ResponseEntity<ApiResponse<ActivityResponse>> markComplete(
            @PathVariable UUID activityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ActivityResponse response = activityService.markComplete(activityId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Activity marked as complete"));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get overdue activities")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityResponse>>> getOverdueActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ActivityResponse> response = activityService.getOverdueActivities(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{activityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete an activity (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(
            @PathVariable UUID activityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        activityService.deleteActivity(activityId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Activity deleted successfully"));
    }

    /* ============================================================
       Timeline endpoints
       ============================================================ */

    @GetMapping("/timeline")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get full activity timeline for the tenant")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getFullTimeline() {
        List<ActivityResponse> timeline = activityService.getFullTimeline();
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }

    @GetMapping("/timeline/{relatedEntityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activity timeline for a specific entity")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getEntityTimeline(
            @PathVariable UUID relatedEntityId) {
        List<ActivityResponse> timeline = activityService.getTimeline(relatedEntityId);
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }

    /* ============================================================
       Calendar / Upcoming
       ============================================================ */

    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get upcoming activities for the next N days")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getUpcoming(
            @RequestParam(defaultValue = "7") int days) {
        List<ActivityResponse> upcoming = activityService.getUpcoming(days);
        return ResponseEntity.ok(ApiResponse.success(upcoming));
    }

    /* ============================================================
       Analytics
       ============================================================ */

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activity analytics")
    public ResponseEntity<ApiResponse<ActivityAnalytics>> getAnalytics() {
        ActivityAnalytics analytics = activityService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    /* ============================================================
       Real-Time Activity Stream
       ============================================================ */

    @GetMapping(value = "/stream/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time activity stream via SSE")
    public SseEmitter subscribeToStream() {
        return activityStreamService.subscribe();
    }

    @GetMapping("/stream")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activity stream (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityStreamResponse>>> getStream(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(activityStreamService.getStream(page, size)));
    }

    @GetMapping("/stream/since")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activity stream events since a timestamp")
    public ResponseEntity<ApiResponse<List<ActivityStreamResponse>>> getStreamSince(
            @RequestParam String since) {
        LocalDateTime sinceTime = LocalDateTime.parse(since);
        return ResponseEntity.ok(ApiResponse.success(activityStreamService.getStreamSince(sinceTime)));
    }

    @GetMapping("/stream/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get activity stream for a specific entity")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityStreamResponse>>> getEntityStream(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                activityStreamService.getEntityStream(entityType, entityId, page, size)));
    }

    @PostMapping("/stream/record")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Record a new activity stream event")
    public ResponseEntity<ApiResponse<ActivityStreamResponse>> recordStreamEvent(
            @RequestParam String eventType,
            @RequestParam String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal UserPrincipal principal) {
        String userName = principal.getEmail();
        ActivityStreamResponse response = activityStreamService.recordEvent(
                eventType, entityType, entityId, entityName, description,
                principal.getUserId(), userName, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Event recorded"));
    }
}
