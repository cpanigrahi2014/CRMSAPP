package com.crm.lead.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeadTagRequest {
    @NotBlank(message = "Tag name is required")
    private String name;
    private String color;
}
