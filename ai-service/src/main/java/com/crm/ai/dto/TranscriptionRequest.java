package com.crm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionRequest {

    @NotBlank(message = "Content to transcribe is required")
    private String content;

    private String sourceType; // CALL_RECORDING, MEETING, VOICEMAIL, CONVERSATION
    private String sourceId;
    private List<String> speakers;
    private String language;
}
