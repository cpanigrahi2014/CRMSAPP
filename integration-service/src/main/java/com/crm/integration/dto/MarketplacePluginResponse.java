package com.crm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplacePluginResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String longDescription;
    private String category;
    private String author;
    private String version;
    private String iconUrl;
    private List<String> screenshots;
    private String downloadUrl;
    private String documentationUrl;
    private String status;
    private String pricing;
    private BigDecimal priceAmount;
    private long installCount;
    private double rating;
    private int ratingCount;
    private List<String> requiredScopes;
    private Map<String, Object> configSchema;
    private boolean isVerified;
    private boolean installed; // Whether current tenant has it installed
    private LocalDateTime createdAt;
}
