package com.crm.email.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateEmailTemplateRequest {
    @NotBlank private String name;
    @NotBlank private String subject;
    @NotBlank private String bodyHtml;
    private String bodyText;
    private String category;
    private String variables;   // JSON array: ["firstName","companyName"]
}
