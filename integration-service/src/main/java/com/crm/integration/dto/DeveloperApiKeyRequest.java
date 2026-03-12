package com.crm.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperApiKeyRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private List<String> scopes;
    private int rateLimit;
    private LocalDateTime expiresAt;
}
