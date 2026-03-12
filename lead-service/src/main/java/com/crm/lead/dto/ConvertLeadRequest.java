package com.crm.lead.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertLeadRequest {

    @NotBlank(message = "Opportunity name is required")
    private String opportunityName;

    private BigDecimal amount;
    private String stage;

    /** If true, also create an Account from the lead's company */
    @Builder.Default
    private boolean createAccount = false;

    /** If true, also create a Contact from the lead's name/email/phone */
    @Builder.Default
    private boolean createContact = false;
}
