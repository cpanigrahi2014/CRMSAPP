package com.crm.opportunity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionResponse {
    private UUID id;
    private String recordType;
    private UUID recordId;
    private String sourceType;
    private UUID sourceId;
    private String mentionedUserId;
    private String mentionedUserName;
    private String mentionedById;
    private String mentionedByName;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
