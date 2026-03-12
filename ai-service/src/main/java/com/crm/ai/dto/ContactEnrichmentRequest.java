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
public class ContactEnrichmentRequest {

    @NotBlank(message = "Contact ID is required")
    private String contactId;

    private String name;
    private String email;
    private String company;
    private String title;
    private String phone;
    private String linkedInUrl;
}
