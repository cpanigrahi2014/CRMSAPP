package com.crm.notification.dto;

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
public class SendSmsRequest {

    @NotBlank(message = "To number is required")
    private String toNumber;

    private String fromNumber;

    @NotBlank(message = "Message body is required")
    private String body;

    private String relatedEntityType;
    private UUID relatedEntityId;
}
