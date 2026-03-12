package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointResponse {
    private UUID id;
    private String name;
    private String path;
    private String method;
    private String description;
    private boolean authRequired;
    private int rateLimit;
    private boolean enabled;
    private String version;
    private long totalCalls;
    private LocalDateTime createdAt;
    private LocalDateTime lastCalledAt;
}
