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
public class AccountActivityResponse {
    private UUID id;
    private UUID accountId;
    private String type;
    private String description;
    private String performedBy;
    private String createdBy;
    private LocalDateTime createdAt;
}
