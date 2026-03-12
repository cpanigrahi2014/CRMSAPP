package com.crm.email.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateEmailTemplateRequest {
    private String name;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String category;
    private String variables;
    private Boolean isActive;
}
