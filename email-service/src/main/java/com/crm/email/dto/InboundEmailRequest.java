package com.crm.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundEmailRequest {

    @NotBlank(message = "From address is required")
    @Email(message = "From address must be a valid email")
    private String fromAddress;

    @NotBlank(message = "To address is required")
    private String toAddress;

    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String ccAddresses;

    private String relatedEntityType;
    private UUID relatedEntityId;
}
