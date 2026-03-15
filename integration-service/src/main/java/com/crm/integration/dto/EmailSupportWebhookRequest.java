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
public class EmailSupportWebhookRequest {

    @NotBlank(message = "From address is required")
    private String fromAddress;

    @NotBlank(message = "To address is required")
    private String toAddress;

    private String subject;
    private String bodyText;
    private String bodyHtml;
    private String ccAddresses;
    private String contactName;
}
