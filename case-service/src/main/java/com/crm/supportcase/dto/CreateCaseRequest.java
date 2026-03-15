package com.crm.supportcase.dto;

import com.crm.supportcase.entity.SupportCase;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    private String description;
    private SupportCase.CasePriority priority;
    private SupportCase.CaseOrigin origin;
    private String contactName;
    private String contactEmail;
    private String accountName;
    private UUID contactId;
    private UUID accountId;
    private UUID assignedTo;
}
