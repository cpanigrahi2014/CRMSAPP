package com.crm.email.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailMessageDto {
    private UUID id;
    private UUID accountId;
    private String fromAddress;
    private String toAddresses;
    private String ccAddresses;
    private String bccAddresses;
    private String subject;
    private String bodyText;
    private String bodyHtml;
    private String direction;
    private String status;
    private String threadId;
    private String providerMessageId;
    private String inReplyTo;
    private UUID templateId;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private boolean hasAttachments;
    private boolean opened;
    private int openCount;
    private int clickCount;
    private LocalDateTime firstOpenedAt;
    private LocalDateTime sentAt;
    private LocalDateTime scheduledAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private String createdBy;
}
