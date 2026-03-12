package com.crm.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateContractRequest {

    @NotNull
    private UUID opportunityId;

    private UUID proposalId;

    @NotBlank
    private String title;

    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String signerName;
    private String signerEmail;
    private String notes;
}
