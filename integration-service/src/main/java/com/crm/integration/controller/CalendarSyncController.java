package com.crm.integration.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.integration.dto.CalendarSyncResponse;
import com.crm.integration.dto.CalendarSyncUpdateRequest;
import com.crm.integration.service.CalendarSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/integrations/calendar")
@RequiredArgsConstructor
@Tag(name = "Calendar Sync", description = "Calendar synchronization APIs")
public class CalendarSyncController {

    private final CalendarSyncService calendarSyncService;

    @GetMapping("/connections")
    @Operation(summary = "Get calendar connections for current user")
    public ResponseEntity<ApiResponse<List<CalendarSyncResponse>>> getConnections() {
        log.info("REST request to get calendar connections");
        return ResponseEntity.ok(ApiResponse.success(
                calendarSyncService.getCalendarConnections(), "Calendar connections retrieved"));
    }

    @GetMapping("/status/{provider}")
    @Operation(summary = "Get sync status for a specific provider")
    public ResponseEntity<ApiResponse<CalendarSyncResponse>> getSyncStatus(@PathVariable String provider) {
        log.info("REST request to get calendar sync status for provider: {}", provider);
        return ResponseEntity.ok(ApiResponse.success(
                calendarSyncService.getSyncStatus(provider.toUpperCase()), "Sync status retrieved"));
    }

    @GetMapping("/google/auth-url")
    @Operation(summary = "Get Google Calendar OAuth2 authorization URL")
    public ResponseEntity<ApiResponse<Map<String, String>>> getGoogleAuthUrl() {
        log.info("REST request to get Google Calendar auth URL");
        String url = calendarSyncService.getGoogleAuthUrl();
        return ResponseEntity.ok(ApiResponse.success(Map.of("authUrl", url), "Auth URL generated"));
    }

    @PostMapping("/google/callback")
    @Operation(summary = "Handle Google Calendar OAuth2 callback")
    public ResponseEntity<ApiResponse<CalendarSyncResponse>> handleGoogleCallback(
            @RequestParam String code) {
        log.info("REST request to handle Google Calendar callback");
        return ResponseEntity.ok(ApiResponse.success(
                calendarSyncService.handleGoogleCallback(code), "Google Calendar connected"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update calendar sync configuration")
    public ResponseEntity<ApiResponse<CalendarSyncResponse>> updateConfig(
            @PathVariable UUID id, @RequestBody CalendarSyncUpdateRequest request) {
        log.info("REST request to update calendar sync config: {}", id);
        return ResponseEntity.ok(ApiResponse.success(
                calendarSyncService.updateConfig(id, request), "Config updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disconnect calendar sync")
    public ResponseEntity<ApiResponse<Void>> disconnect(@PathVariable UUID id) {
        log.info("REST request to disconnect calendar sync: {}", id);
        calendarSyncService.disconnect(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Calendar disconnected"));
    }
}
