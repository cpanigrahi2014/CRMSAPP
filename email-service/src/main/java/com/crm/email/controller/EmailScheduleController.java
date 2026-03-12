package com.crm.email.controller;

import com.crm.common.security.UserPrincipal;
import com.crm.email.dto.EmailScheduleDto;
import com.crm.email.service.EmailScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/email/schedules")
@RequiredArgsConstructor
public class EmailScheduleController {

    private final EmailScheduleService service;

    @GetMapping
    public ResponseEntity<Page<EmailScheduleDto>> list(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scheduledAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(service.list(p.getTenantId(), PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailScheduleDto> get(@AuthenticationPrincipal UserPrincipal p,
                                                 @PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(p.getTenantId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<EmailScheduleDto> cancel(@AuthenticationPrincipal UserPrincipal p,
                                                    @PathVariable UUID id) {
        return ResponseEntity.ok(service.cancel(p.getTenantId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal p,
                                        @PathVariable UUID id) {
        service.delete(p.getTenantId(), id);
        return ResponseEntity.ok().build();
    }
}
