package com.crm.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadScoreResponse {

    private UUID leadId;
    private int score;
    private List<String> factors;
    private double confidence;
}
