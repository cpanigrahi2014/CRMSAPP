package com.crm.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionActionRequest {
    @NotNull(message = "Suggestion ID is required")
    private UUID suggestionId;

    @NotNull(message = "Accepted flag is required")
    private Boolean accepted;
}
