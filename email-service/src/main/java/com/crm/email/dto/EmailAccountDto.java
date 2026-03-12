package com.crm.email.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/* ── Email Account DTOs ──────────────────────────────────────── */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailAccountDto {
    private UUID id;
    private String provider;
    private String email;
    private String displayName;
    private boolean isDefault;
    private boolean connected;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;

    /* SMTP-only fields (tokens never exposed) */
    private String smtpHost;
    private Integer smtpPort;
}
