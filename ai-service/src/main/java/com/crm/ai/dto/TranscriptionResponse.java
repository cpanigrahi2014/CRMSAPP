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
public class TranscriptionResponse {
    private UUID id;
    private String sourceType;
    private String sourceId;
    private String fullTranscript;
    private List<TranscriptSegment> segments;
    private List<String> keyTopics;
    private String summary;
    private String language;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptSegment {
        private String speaker;
        private String text;
        private String timestamp;
    }
}
