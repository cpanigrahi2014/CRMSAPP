package com.crm.activity.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.common.security.TenantContext;
import com.crm.common.security.UserPrincipal;
import com.crm.activity.entity.CalendarFeedToken;
import com.crm.activity.service.CalendarFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities/calendar")
@RequiredArgsConstructor
@Tag(name = "Calendar Sync", description = "Calendar feed and sync APIs")
public class CalendarFeedController {

    private final CalendarFeedService calendarFeedService;

    // ─── Token Management (authenticated) ────────────────────────────

    @PostMapping("/tokens")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Generate a calendar feed token for subscribing in external calendars")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateToken(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String name = body.getOrDefault("name", "Calendar Feed");
        String tenantId = TenantContext.getTenantId();
        CalendarFeedToken token = calendarFeedService.generateToken(tenantId, principal.getUserId(), name);
        Map<String, Object> response = Map.of(
                "id", token.getId(),
                "token", token.getToken(),
                "name", token.getName(),
                "feedUrl", "/api/v1/activities/calendar/feed/" + token.getToken() + "/activities.ics",
                "createdAt", token.getCreatedAt() != null ? token.getCreatedAt().toString() : ""
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Calendar feed token generated"));
    }

    @GetMapping("/tokens")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "List all active calendar feed tokens")
    public ResponseEntity<ApiResponse<List<CalendarFeedToken>>> getTokens(
            @AuthenticationPrincipal UserPrincipal principal) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                calendarFeedService.getTokens(tenantId, principal.getUserId())));
    }

    @DeleteMapping("/tokens/{tokenId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Revoke a calendar feed token")
    public ResponseEntity<ApiResponse<Void>> revokeToken(
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal UserPrincipal principal) {
        String tenantId = TenantContext.getTenantId();
        calendarFeedService.revokeToken(tokenId, tenantId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Token revoked"));
    }

    // ─── iCal Feed (token-based, no JWT required) ────────────────────

    @GetMapping(value = "/feed/{token}/activities.ics", produces = "text/calendar")
    @Operation(summary = "iCal subscription feed — use this URL in Google Calendar, Outlook, Apple Calendar")
    public ResponseEntity<String> getICalFeed(@PathVariable String token) {
        return calendarFeedService.validateToken(token)
                .map(feedToken -> {
                    String ical = calendarFeedService.generateICalFeed(feedToken.getTenantId());
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=activities.ics")
                            .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                            .body(ical);
                })
                .orElse(ResponseEntity.status(401).body("Invalid or expired calendar token"));
    }

    @GetMapping(value = "/feed/{token}/event/{activityId}.ics", produces = "text/calendar")
    @Operation(summary = "Download a single activity as .ics file")
    public ResponseEntity<String> getSingleEventIcs(
            @PathVariable String token,
            @PathVariable UUID activityId) {
        return calendarFeedService.validateToken(token)
                .map(feedToken -> {
                    String ics = calendarFeedService.generateSingleEventIcs(feedToken.getTenantId(), activityId);
                    if (ics == null) {
                        return ResponseEntity.notFound().<String>build();
                    }
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=event.ics")
                            .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                            .body(ics);
                })
                .orElse(ResponseEntity.status(401).body("Invalid or expired calendar token"));
    }

    // ─── Authenticated iCal export (for direct download) ─────────────

    @GetMapping(value = "/export.ics", produces = "text/calendar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Export all activities as .ics file (authenticated)")
    public ResponseEntity<String> exportIcal() {
        String tenantId = TenantContext.getTenantId();
        String ical = calendarFeedService.generateICalFeed(tenantId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=crm-activities.ics")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(ical);
    }
}
