package com.crm.workflow.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProposalResponse {
    private UUID id;
    private UUID opportunityId;
    private UUID templateId;
    private String title;
    private String content;
    private String status;
    private BigDecimal amount;
    private LocalDate validUntil;
    private LocalDateTime sentAt;
    private LocalDateTime viewedAt;
    private LocalDateTime respondedAt;
    private String recipientEmail;
    private String recipientName;
    private String notes;
    private Integer version;
    private List<LineItemResponse> lineItems;
    private LocalDateTime createdAt;
    private String createdBy;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineItemResponse {
        private UUID id;
        private String productName;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal totalPrice;
    }
}
