package com.crm.contact.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConsentRequest {
    private Boolean emailOptIn;
    private Boolean smsOptIn;
    private Boolean phoneOptIn;
    private Boolean doNotCall;
    private String consentSource;
}
