package com.crm.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAppRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String appType;
    private Map<String, Object> layout;
    private Map<String, Object> dataSource;
    private Map<String, Object> style;
}
