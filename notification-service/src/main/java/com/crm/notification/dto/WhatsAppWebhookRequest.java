package com.crm.notification.dto;

import com.crm.notification.entity.WhatsAppMessage;
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

    @NotBlank(message = "From number is required")
    private String fromNumber;

    private String toNumber;

    private String body;

    private String mediaUrl;
    private String mediaType;

    @Builder.Default
    private WhatsAppMessage.MessageType messageType = WhatsAppMessage.MessageType.TEXT;
}
