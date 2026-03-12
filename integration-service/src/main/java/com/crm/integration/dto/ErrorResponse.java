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
public class ErrorResponse {
    private UUID id;
    private UUID integrationId;
    private String integrationName;
    private String level;
    private String message;
    private String endpoint;
    private Integer httpStatus;
    private String requestPayload;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
