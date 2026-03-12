package com.crm.contact.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContactRequest {

    private String firstName;
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String mobilePhone;
    private String title;
    private String department;
    private UUID accountId;
    private String mailingAddress;
    private String description;

    // Feature 2: Enhanced linking
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
    private Boolean emailOptIn;
    private Boolean smsOptIn;
    private Boolean phoneOptIn;
    private String consentSource;
    private Boolean doNotCall;
}
