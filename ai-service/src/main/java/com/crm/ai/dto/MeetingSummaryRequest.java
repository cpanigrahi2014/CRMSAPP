package com.crm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingSummaryRequest {

    @NotBlank(message = "Meeting title is required")
    private String meetingTitle;

    private String meetingDate;

    private List<String> participants;

    @NotBlank(message = "Transcript or notes are required")
    private String transcript;

    private String relatedEntityType;

    private String relatedEntityId;
}
