package com.crm.integration.service;

import com.crm.common.exception.BadRequestException;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.integration.dto.*;
import com.crm.integration.entity.CalendarSyncConfig;
import com.crm.integration.repository.CalendarSyncConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.crm.common.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarSyncService {

    private final CalendarSyncConfigRepository calendarSyncRepo;

    @Value("${calendar.google.client-id:}")
    private String googleClientId;

    @Value("${calendar.google.client-secret:}")
    private String googleClientSecret;

    @Value("${calendar.google.redirect-uri:http://localhost:3000/integrations/calendar/callback}")
    private String googleRedirectUri;

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";

    // ─── List Calendar Connections ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CalendarSyncResponse> getCalendarConnections() {
        String tenantId = TenantContext.getTenantId();
        return calendarSyncRepo.findByTenantIdAndUserId(tenantId, getCurrentUserId())
                .stream().map(this::toResponse).toList();
    }

    // ─── Google Calendar OAuth2 Flow ─────────────────────────────────

    public String getGoogleAuthUrl() {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BadRequestException("Google Calendar integration is not configured. Set calendar.google.client-id in application properties.");
        }
        return GOOGLE_AUTH_URL +
                "?client_id=" + googleClientId +
                "&redirect_uri=" + googleRedirectUri +
                "&response_type=code" +
                "&scope=" + GOOGLE_CALENDAR_SCOPE +
                "&access_type=offline" +
                "&prompt=consent";
    }

    @Transactional
    public CalendarSyncResponse handleGoogleCallback(String code) {
        String tenantId = TenantContext.getTenantId();
        String userId = getCurrentUserId();

        // Exchange authorization code for tokens
        Map<String, Object> tokens = exchangeGoogleCode(code);
        String accessToken = (String) tokens.get("access_token");
        String refreshToken = (String) tokens.get("refresh_token");
        Integer expiresIn = (Integer) tokens.get("expires_in");

        // Create or update the sync config
        CalendarSyncConfig config = calendarSyncRepo
                .findByTenantIdAndUserIdAndProvider(tenantId, userId, "GOOGLE")
                .orElse(CalendarSyncConfig.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .provider("GOOGLE")
                        .calendarId("primary")
                        .build());

        config.setAccessToken(accessToken);
        if (refreshToken != null) {
            config.setRefreshToken(refreshToken);
        }
        config.setTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
        config.setStatus("CONNECTED");
        config.setEnabled(true);

        return toResponse(calendarSyncRepo.save(config));
    }

    // ─── Disconnect ──────────────────────────────────────────────────

    @Transactional
    public void disconnect(UUID configId) {
        String tenantId = TenantContext.getTenantId();
        CalendarSyncConfig config = calendarSyncRepo.findByIdAndTenantId(configId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar sync config not found"));
        config.setStatus("DISCONNECTED");
        config.setAccessToken(null);
        config.setRefreshToken(null);
        config.setEnabled(false);
        calendarSyncRepo.save(config);
    }

    // ─── Update Config ───────────────────────────────────────────────

    @Transactional
    public CalendarSyncResponse updateConfig(UUID configId, CalendarSyncUpdateRequest request) {
        String tenantId = TenantContext.getTenantId();
        CalendarSyncConfig config = calendarSyncRepo.findByIdAndTenantId(configId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar sync config not found"));

        if (request.getCalendarId() != null) config.setCalendarId(request.getCalendarId());
        if (request.getSyncDirection() != null) config.setSyncDirection(request.getSyncDirection());
        if (request.getSyncIntervalMinutes() != null) config.setSyncIntervalMinutes(request.getSyncIntervalMinutes());
        if (request.getEnabled() != null) config.setEnabled(request.getEnabled());

        return toResponse(calendarSyncRepo.save(config));
    }

    // ─── Sync Status ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CalendarSyncResponse getSyncStatus(String provider) {
        String tenantId = TenantContext.getTenantId();
        return calendarSyncRepo.findByTenantIdAndUserIdAndProvider(tenantId, getCurrentUserId(), provider)
                .map(this::toResponse)
                .orElse(CalendarSyncResponse.builder()
                        .provider(provider)
                        .status("DISCONNECTED")
                        .enabled(false)
                        .build());
    }

    // ─── Token Exchange ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeGoogleCode(String code) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", googleRedirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BadRequestException("Failed to exchange Google authorization code");
        }
        return response.getBody();
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getUserId() != null ? up.getUserId() : up.getEmail();
        }
        return "SYSTEM";
    }

    private CalendarSyncResponse toResponse(CalendarSyncConfig config) {
        return CalendarSyncResponse.builder()
                .id(config.getId())
                .provider(config.getProvider())
                .status(config.getStatus())
                .calendarId(config.getCalendarId())
                .syncDirection(config.getSyncDirection())
                .syncIntervalMinutes(config.getSyncIntervalMinutes())
                .lastSyncAt(config.getLastSyncAt())
                .lastSyncStatus(config.getLastSyncStatus())
                .eventsSynced(config.getEventsSynced())
                .enabled(config.isEnabled())
                .createdAt(config.getCreatedAt())
                .build();
    }
}
