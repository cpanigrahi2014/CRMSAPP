package com.crm.supportcase.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSkillRequest {
    private UUID userId;
    private String skillName;
    private Integer proficiency;
    private String category;
}
