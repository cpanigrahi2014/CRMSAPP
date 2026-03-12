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
public class AutoLeadRequest {

    @NotBlank(message = "Source type is required (EMAIL or MEETING)")
    private String sourceType;

    private String sourceReference;

    @NotBlank(message = "Content is required")
    private String content;
}
