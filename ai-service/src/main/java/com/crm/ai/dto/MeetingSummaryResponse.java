package com.crm.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingSummaryResponse {

    private UUID id;
    private String meetingTitle;
    private LocalDateTime meetingDate;
    private List<String> participants;
    private String summary;
    private List<String> actionItems;
    private List<String> keyDecisions;
    private List<CrmUpdateSuggestion> crmUpdates;
    private String relatedEntityType;
    private String relatedEntityId;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrmUpdateSuggestion {
        private String entityType;
        private String entityId;
        private String field;
        private String suggestedValue;
        private String reason;
    }
}
