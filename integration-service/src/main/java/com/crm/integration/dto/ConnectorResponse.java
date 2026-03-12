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
public class ConnectorResponse {
    private UUID id;
    private String name;
    private String type;
    private String host;
    private Integer port;
    private String databaseName;
    private String baseUrl;
    private String connectionString;
    private String status;
    private boolean enabled;
    private LocalDateTime lastTestAt;
    private LocalDateTime createdAt;
}
