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
public class EmailReplyRequest {

    @NotBlank(message = "Original sender is required")
    private String originalFrom;

    @NotBlank(message = "Original subject is required")
    private String originalSubject;

    @NotBlank(message = "Original body is required")
    private String originalBody;

    @Builder.Default
    private String tone = "professional";

    private String additionalContext;
}
