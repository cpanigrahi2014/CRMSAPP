package com.crm.contact.dto;

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
public class CommunicationResponse {
    private UUID id;
    private UUID contactId;
    private String commType;
    private String subject;
    private String body;
    private String direction;
    private String status;
    private LocalDateTime communicationDate;
    private String tenantId;
    private LocalDateTime createdAt;
    private String createdBy;
}
