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
public class SentimentAnalysisRequest {

    @NotBlank(message = "Content to analyze is required")
    private String content;

    private String sourceType; // CALL, EMAIL, WHATSAPP, SMS, MEETING
    private String sourceId;
    private String contactName;
}
