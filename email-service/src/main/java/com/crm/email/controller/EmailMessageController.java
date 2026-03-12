package com.crm.email.controller;

import com.crm.common.security.UserPrincipal;
import com.crm.email.dto.*;
import com.crm.email.entity.EmailMessage;
import com.crm.email.service.EmailSendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/email/messages")
@RequiredArgsConstructor
public class EmailMessageController {

    private final EmailSendService service;

    @PostMapping("/send")
    public ResponseEntity<EmailMessageDto> send(@AuthenticationPrincipal UserPrincipal p,
                                                 @Valid @RequestBody SendEmailRequest req) {
        return ResponseEntity.ok(service.send(p.getTenantId(), p.getUserId(), req));
    }

    @GetMapping
    public ResponseEntity<Page<EmailMessageDto>> list(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(service.listMessages(p.getTenantId(), PageRequest.of(page, size, sort)));
    }

    @GetMapping("/sent")
    public ResponseEntity<Page<EmailMessageDto>> sent(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listByDirection(p.getTenantId(),
                EmailMessage.Direction.OUTBOUND, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/inbox")
    public ResponseEntity<Page<EmailMessageDto>> inbox(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listByDirection(p.getTenantId(),
                EmailMessage.Direction.INBOUND, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EmailMessageDto>> search(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(p.getTenantId(), query, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailMessageDto> get(@AuthenticationPrincipal UserPrincipal p,
                                                @PathVariable UUID id) {
        return ResponseEntity.ok(service.getMessage(p.getTenantId(), id));
    }

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<EmailMessageDto>> thread(@AuthenticationPrincipal UserPrincipal p,
                                                         @PathVariable String threadId) {
        return ResponseEntity.ok(service.getThread(p.getTenantId(), threadId));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<EmailMessageDto>> byEntity(@AuthenticationPrincipal UserPrincipal p,
                                                           @PathVariable String entityType,
                                                           @PathVariable UUID entityId) {
        return ResponseEntity.ok(service.getByEntity(p.getTenantId(), entityType, entityId));
    }
}
