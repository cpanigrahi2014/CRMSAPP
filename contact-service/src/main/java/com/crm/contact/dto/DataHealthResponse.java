package com.crm.contact.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataHealthResponse {
    private long totalContacts;
    private long staleContacts;      // not updated in 30+ days
    private long missingEmail;
    private long missingPhone;
    private long duplicateGroups;
    private int healthScore;         // 0-100
    private List<StaleRecord> staleRecords;
    private List<IssueBreakdown> issues;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaleRecord {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String updatedAt;
        private int daysSinceUpdate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueBreakdown {
        private String category;
        private String description;
        private long count;
        private String severity; // HIGH, MEDIUM, LOW
    }
}
