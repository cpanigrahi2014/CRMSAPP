package com.crm.contact.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String mobilePhone;
    private String title;
    private String department;
    private UUID accountId;
    private String mailingAddress;
    private String description;

    // Feature 2
    private UUID ownerId;

    // Feature 4: Social profiles
    private String linkedinUrl;
    private String twitterUrl;
    private String facebookUrl;
    private String otherSocialUrl;

    // Feature 5: Segmentation
    private String leadSource;
    private String lifecycleStage;
    private String segment;

    // Feature 6: Marketing consent
    private boolean emailOptIn;
    private boolean smsOptIn;
    private boolean phoneOptIn;
    private LocalDateTime consentDate;
    private String consentSource;
    private boolean doNotCall;

    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
