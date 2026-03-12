package com.crm.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSyncRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Integration ID is required")
    private UUID integrationId;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    @NotBlank(message = "Direction is required")
    private String direction;

    private String schedule;
    private Map<String, String> fieldMapping;
    private boolean enabled;
}
