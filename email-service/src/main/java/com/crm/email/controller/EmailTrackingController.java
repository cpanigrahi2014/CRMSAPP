package com.crm.email.controller;

import com.crm.email.dto.EmailTrackingEventDto;
import com.crm.email.service.EmailTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.crm.common.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;

/**
 * Tracking endpoints. Open pixel and click redirect are PUBLIC (no auth).
 * Event listing requires authentication.
 */
@RestController
@RequestMapping("/api/v1/email/track")
@RequiredArgsConstructor
public class EmailTrackingController {

    private final EmailTrackingService service;

    /**
     * Tracking pixel — returns 1×1 transparent GIF.
     * Called when an email client loads the embedded image.
     */
    @GetMapping("/open/{messageId}.gif")
    public ResponseEntity<byte[]> trackOpen(@PathVariable UUID messageId,
                                             HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        byte[] pixel = service.recordOpen(messageId, userAgent, ip);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_GIF);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.set("Pragma", "no-cache");
        return new ResponseEntity<>(pixel, headers, HttpStatus.OK);
    }

    /**
     * Click tracking redirect.
     * Rewrites link URLs to pass through this endpoint; records the click then 302-redirects to the original URL.
     */
    @GetMapping("/click/{messageId}")
    public ResponseEntity<Void> trackClick(@PathVariable UUID messageId,
                                            @RequestParam String url,
                                            HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        service.recordClick(messageId, url, userAgent, ip);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    /**
     * List tracking events for a message (authenticated).
     */
    @GetMapping("/events/{messageId}")
    public ResponseEntity<List<EmailTrackingEventDto>> events(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID messageId) {
        return ResponseEntity.ok(service.getEventsForMessage(messageId));
    }
}
