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
public class LlmRequest {

    @NotBlank(message = "Model is required")
    private String model;

    @NotBlank(message = "Prompt is required")
    private String prompt;

    @Builder.Default
    private int maxTokens = 1024;

    @Builder.Default
    private double temperature = 0.7;
}
