package com.crm.workflow.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SignContractRequest {
    private String signerName;
    private String signerEmail;
    private String signatureData; // base64 signature image
}
