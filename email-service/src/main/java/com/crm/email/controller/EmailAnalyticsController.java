package com.crm.email.controller;

import com.crm.common.security.UserPrincipal;
import com.crm.email.dto.EmailAnalyticsDto;
import com.crm.email.service.EmailAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email/analytics")
@RequiredArgsConstructor
public class EmailAnalyticsController {

    private final EmailAnalyticsService service;

    @GetMapping
    public ResponseEntity<EmailAnalyticsDto> getAnalytics(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(service.getAnalytics(p.getTenantId()));
    }
}
