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
public class WhatsAppWebhookRequest {

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String firstName;
    private String lastName;
    private String message;
    private String mediaUrl;

    /** If true, auto-convert the lead to an opportunity after creation. */
    @Builder.Default
    private boolean autoConvert = false;

    private String opportunityName;
    private String opportunityAmount;
    private String opportunityStage;
}
