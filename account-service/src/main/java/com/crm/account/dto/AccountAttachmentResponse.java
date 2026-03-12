package com.crm.account.dto;

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
public class AccountAttachmentResponse {
    private UUID id;
    private UUID accountId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String createdBy;
    private LocalDateTime createdAt;
}
