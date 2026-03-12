package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginInstallationResponse {
    private UUID id;
    private UUID pluginId;
    private String pluginName;
    private String pluginSlug;
    private String status;
    private Map<String, Object> config;
    private String installedBy;
    private LocalDateTime createdAt;
}
