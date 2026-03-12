package com.crm.email.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateEmailAccountRequest {
    @NotBlank private String provider;   // GMAIL, OUTLOOK, SMTP
    @NotBlank private String email;
    private String displayName;
    private boolean isDefault;

    /* SMTP-specific */
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;

    /* OAuth2 auth code (Gmail / Outlook) */
    private String authCode;
}
