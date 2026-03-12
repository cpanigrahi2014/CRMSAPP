package com.crm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDraftRequest {

    @NotBlank(message = "Recipient is required")
    private String to;

    @NotBlank(message = "Subject context is required")
    private String subjectContext;

    @NotBlank(message = "Tone is required")
    private String tone;

    private String context;
}
