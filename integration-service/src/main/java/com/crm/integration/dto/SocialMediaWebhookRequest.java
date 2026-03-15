package com.crm.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialMediaWebhookRequest {

    @NotBlank(message = "Platform is required")
    private String platform;   // INSTAGRAM, FACEBOOK, TWITTER, LINKEDIN

    @NotBlank(message = "Username is required")
    private String username;

    private String fullName;
    private String email;
    private String comment;
    private String postUrl;
    private String profileUrl;
}
