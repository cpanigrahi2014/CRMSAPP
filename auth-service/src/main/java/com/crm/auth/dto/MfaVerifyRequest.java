package com.crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "MFA code is required")
    private String code;

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    /** Temporary token issued after password check when MFA is required */
    @NotBlank(message = "MFA token is required")
    private String mfaToken;
}
