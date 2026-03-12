package com.crm.email.controller;

import com.crm.common.security.UserPrincipal;
import com.crm.email.dto.*;
import com.crm.email.service.EmailTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/email/templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService service;

    @GetMapping
    public ResponseEntity<Page<EmailTemplateDto>> list(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(service.list(p.getTenantId(), PageRequest.of(page, size, sort)));
    }

    @GetMapping("/active")
    public ResponseEntity<List<EmailTemplateDto>> listActive(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(service.listActive(p.getTenantId()));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EmailTemplateDto>> search(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(p.getTenantId(), query, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateDto> get(@AuthenticationPrincipal UserPrincipal p,
                                                @PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(p.getTenantId(), id));
    }

    @PostMapping
    public ResponseEntity<EmailTemplateDto> create(@AuthenticationPrincipal UserPrincipal p,
                                                    @Valid @RequestBody CreateEmailTemplateRequest req) {
        return ResponseEntity.ok(service.create(p.getTenantId(), p.getUserId(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateDto> update(@AuthenticationPrincipal UserPrincipal p,
                                                    @PathVariable UUID id,
                                                    @Valid @RequestBody UpdateEmailTemplateRequest req) {
        return ResponseEntity.ok(service.update(p.getTenantId(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal p,
                                        @PathVariable UUID id) {
        service.delete(p.getTenantId(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/preview")
    public ResponseEntity<Map<String, String>> preview(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> sampleData) {
        var rendered = service.preview(p.getTenantId(), id, sampleData);
        return ResponseEntity.ok(Map.of("subject", rendered.subject(), "bodyHtml", rendered.bodyHtml()));
    }
}
