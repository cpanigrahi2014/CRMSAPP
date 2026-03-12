package com.crm.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MfaConfigRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;
    private String mfaType; // TOTP, SMS, EMAIL
    private boolean enabled;
}
