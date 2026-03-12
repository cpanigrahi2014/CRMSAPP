package com.crm.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddableWidgetRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String widgetType;
    private String description;
    private Map<String, Object> config;
    private List<String> allowedDomains;
}
