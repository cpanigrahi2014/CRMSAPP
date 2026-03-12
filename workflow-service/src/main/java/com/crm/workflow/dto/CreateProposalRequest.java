package com.crm.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateProposalRequest {

    @NotNull
    private UUID opportunityId;

    private UUID templateId;

    @NotBlank
    private String title;

    private BigDecimal amount;
    private LocalDate validUntil;
    private String recipientEmail;
    private String recipientName;
    private String notes;
    private List<LineItemRequest> lineItems;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineItemRequest {
        private String productName;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
    }
}
