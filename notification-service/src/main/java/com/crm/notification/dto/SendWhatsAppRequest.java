package com.crm.notification.dto;

import com.crm.notification.entity.WhatsAppMessage;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendWhatsAppRequest {

    @NotBlank(message = "To number is required")
    private String toNumber;

    private String fromNumber;

    private String body;

    private String mediaUrl;
    private String mediaType;

    @Builder.Default
    private WhatsAppMessage.MessageType messageType = WhatsAppMessage.MessageType.TEXT;

    private String relatedEntityType;
    private UUID relatedEntityId;
}
