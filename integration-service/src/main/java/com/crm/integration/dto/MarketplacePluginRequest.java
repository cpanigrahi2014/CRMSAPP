package com.crm.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplacePluginRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String longDescription;
    private String category;
    private String author;
    private String version;
    private String iconUrl;
    private List<String> screenshots;
    private String downloadUrl;
    private String documentationUrl;
    private String pricing;
    private BigDecimal priceAmount;
    private List<String> requiredScopes;
    private Map<String, Object> configSchema;
}
