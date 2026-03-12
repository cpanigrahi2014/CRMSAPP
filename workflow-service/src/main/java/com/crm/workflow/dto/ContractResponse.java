package com.crm.workflow.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ContractResponse {
    private UUID id;
    private UUID opportunityId;
    private UUID proposalId;
    private String title;
    private String content;
    private String status;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime sentAt;
    private LocalDateTime viewedAt;
    private LocalDateTime signedAt;
    private LocalDateTime executedAt;
    private String signerName;
    private String signerEmail;
    private String notes;
    private Integer version;
    private LocalDateTime createdAt;
    private String createdBy;
}
