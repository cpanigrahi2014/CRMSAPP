package com.crm.contact.entity;

import com.crm.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contacts", indexes = {
        @Index(name = "idx_contact_tenant", columnList = "tenant_id"),
        @Index(name = "idx_contact_email", columnList = "email"),
        @Index(name = "idx_contact_account", columnList = "account_id"),
        @Index(name = "idx_contact_segment", columnList = "segment"),
        @Index(name = "idx_contact_lifecycle", columnList = "lifecycle_stage"),
        @Index(name = "idx_contact_owner", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "mobile_phone")
    private String mobilePhone;

    @Column(name = "title")
    private String title;

    @Column(name = "department")
    private String department;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "mailing_address", columnDefinition = "TEXT")
    private String mailingAddress;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ── Feature 2: Enhanced linking ──
    @Column(name = "owner_id")
    private UUID ownerId;

    // ── Feature 4: Social profiles ──
    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "twitter_url", length = 500)
    private String twitterUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "other_social_url", length = 500)
    private String otherSocialUrl;

    // ── Feature 5: Segmentation ──
    @Column(name = "lead_source", length = 50)
    private String leadSource;

    @Column(name = "lifecycle_stage", length = 50)
    private String lifecycleStage;

    @Column(name = "segment", length = 100)
    private String segment;

    // ── Feature 6: Marketing consent ──
    @Column(name = "email_opt_in", nullable = false)
    private boolean emailOptIn;

    @Column(name = "sms_opt_in", nullable = false)
    private boolean smsOptIn;

    @Column(name = "phone_opt_in", nullable = false)
    private boolean phoneOptIn;

    @Column(name = "consent_date")
    private LocalDateTime consentDate;

    @Column(name = "consent_source", length = 100)
    private String consentSource;

    @Column(name = "do_not_call", nullable = false)
    private boolean doNotCall;
}
