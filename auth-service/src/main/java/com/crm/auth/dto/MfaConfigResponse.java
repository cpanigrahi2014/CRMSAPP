package com.crm.auth.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MfaConfigResponse {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String mfaType;
    private boolean enabled;
    private boolean hasBackupCodes;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}
