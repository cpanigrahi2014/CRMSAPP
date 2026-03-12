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
public class EmailReplyResponse {

    private UUID id;
    private String originalFrom;
    private String originalSubject;
    private String replySubject;
    private String replyBody;
    private String tone;
    private List<String> suggestions;
    private LocalDateTime createdAt;
}
