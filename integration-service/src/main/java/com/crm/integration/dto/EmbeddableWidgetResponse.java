package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddableWidgetResponse {
    private UUID id;
    private String name;
    private String widgetType;
    private String description;
    private Map<String, Object> config;
    private String embedToken;
    private String embedCode;
    private List<String> allowedDomains;
    private boolean active;
    private long viewCount;
    private LocalDateTime createdAt;
}
