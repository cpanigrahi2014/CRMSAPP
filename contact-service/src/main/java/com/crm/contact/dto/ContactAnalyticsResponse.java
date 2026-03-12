package com.crm.contact.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactAnalyticsResponse {
    private long totalContacts;
    private long contactsWithEmail;
    private long contactsWithPhone;
    private long contactsWithAccount;
    private long emailOptInCount;
    private long smsOptInCount;
    private long doNotCallCount;
    private Map<String, Long> bySegment;
    private Map<String, Long> byLifecycleStage;
    private Map<String, Long> byLeadSource;
    private Map<String, Long> byDepartment;
    private Map<String, Long> communicationsByType;
    private long totalCommunications;
    private long totalTags;
}
