package com.crm.opportunity.dto;

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
public class RecordCommentResponse {
    private UUID id;
    private String recordType;
    private UUID recordId;
    private String authorId;
    private String authorName;
    private String content;
    private UUID parentCommentId;
    private Boolean isInternal;
    private Boolean isEdited;
    private Boolean isPinned;
    private List<RecordCommentResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
