package com.crm.email.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SendEmailRequest {
    @NotBlank private String to;
    private String cc;
    private String bcc;
    @NotBlank private String subject;
    private String bodyHtml;
    private String bodyText;
    private UUID templateId;
    private Map<String, String> templateVars;   // merge fields for template
    private UUID accountId;                     // which connected account to use
    private String relatedEntityType;
    private UUID relatedEntityId;
    private String inReplyTo;                   // for threading / replies
    private LocalDateTime scheduledAt;          // null = send now
    private boolean trackOpens;
    private boolean trackClicks;
}
