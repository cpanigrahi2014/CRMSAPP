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
public class CustomAppResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String appType;
    private String status;
    private Map<String, Object> layout;
    private Map<String, Object> dataSource;
    private Map<String, Object> style;
    private String publishedVersion;
    private String createdBy;
    private LocalDateTime createdAt;
}
