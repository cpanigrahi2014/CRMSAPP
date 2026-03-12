package com.crm.email.controller;

import com.crm.common.security.UserPrincipal;
import com.crm.email.dto.*;
import com.crm.email.service.EmailAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/email/accounts")
@RequiredArgsConstructor
public class EmailAccountController {

    private final EmailAccountService service;

    @GetMapping
    public ResponseEntity<List<EmailAccountDto>> list(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(service.listAccounts(p.getTenantId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailAccountDto> get(@AuthenticationPrincipal UserPrincipal p,
                                               @PathVariable UUID id) {
        return ResponseEntity.ok(service.getAccount(p.getTenantId(), id));
    }

    @PostMapping("/smtp")
    public ResponseEntity<EmailAccountDto> createSmtp(@AuthenticationPrincipal UserPrincipal p,
                                                       @RequestBody CreateEmailAccountRequest req) {
        return ResponseEntity.ok(service.createSmtpAccount(p.getTenantId(), p.getUserId(), req));
    }

    /* ── OAuth2 flows ─────────────────────────────────────────── */

    @GetMapping("/oauth/gmail/url")
    public ResponseEntity<OAuthConnectResponse> gmailAuthUrl(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(service.getGmailAuthUrl(p.getTenantId()));
    }

    @PostMapping("/oauth/gmail/callback")
    public ResponseEntity<EmailAccountDto> gmailCallback(@AuthenticationPrincipal UserPrincipal p,
                                                          @RequestParam String code) {
        return ResponseEntity.ok(service.connectGmail(p.getTenantId(), p.getUserId(), code));
    }

    @GetMapping("/oauth/outlook/url")
    public ResponseEntity<OAuthConnectResponse> outlookAuthUrl(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(service.getOutlookAuthUrl(p.getTenantId()));
    }

    @PostMapping("/oauth/outlook/callback")
    public ResponseEntity<EmailAccountDto> outlookCallback(@AuthenticationPrincipal UserPrincipal p,
                                                            @RequestParam String code) {
        return ResponseEntity.ok(service.connectOutlook(p.getTenantId(), p.getUserId(), code));
    }

    /* ── Management ───────────────────────────────────────────── */

    @PutMapping("/{id}/default")
    public ResponseEntity<EmailAccountDto> setDefault(@AuthenticationPrincipal UserPrincipal p,
                                                       @PathVariable UUID id) {
        return ResponseEntity.ok(service.setDefault(p.getTenantId(), id));
    }

    @PostMapping("/{id}/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal UserPrincipal p,
                                            @PathVariable UUID id) {
        service.disconnect(p.getTenantId(), id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal p,
                                        @PathVariable UUID id) {
        service.deleteAccount(p.getTenantId(), id);
        return ResponseEntity.ok().build();
    }
}
