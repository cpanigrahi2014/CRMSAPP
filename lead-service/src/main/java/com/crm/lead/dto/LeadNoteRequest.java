package com.crm.lead.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeadNoteRequest {
    @NotBlank(message = "Note content is required")
    private String content;
}
