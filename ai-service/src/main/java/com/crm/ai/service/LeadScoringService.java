package com.crm.ai.service;

import com.crm.ai.dto.LeadScoreRequest;
import com.crm.ai.dto.LeadScoreResponse;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadScoringService {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    public LeadScoreResponse scoreLead(LeadScoreRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Scoring lead: {} for tenant: {}", request.getLeadId(), tenantId);

        String prompt = buildLeadScoringPrompt(request.getLeadData());

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(512)
                .temperature(0.3)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        LeadScoreResponse scoreResponse = parseLeadScoreResponse(request.getLeadId(), llmResponse.getContent());

        log.info("Lead {} scored: {} (confidence: {})", request.getLeadId(), scoreResponse.getScore(), scoreResponse.getConfidence());
        return scoreResponse;
    }

    @Async("aiTaskExecutor")
    public void scoreLeadAsync(UUID leadId, Map<String, Object> leadData, String tenantId) {
        try {
            TenantContext.setTenantId(tenantId);
            log.info("Async scoring lead: {} for tenant: {}", leadId, tenantId);

            LeadScoreRequest request = LeadScoreRequest.builder()
                    .leadId(leadId)
                    .leadData(leadData)
                    .build();

            LeadScoreResponse response = scoreLead(request);
            log.info("Async lead scoring completed for lead: {}, score: {}", leadId, response.getScore());
        } catch (Exception e) {
            log.error("Async lead scoring failed for lead: {}: {}", leadId, e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private String buildLeadScoringPrompt(Map<String, Object> leadData) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a CRM lead scoring AI assistant. Analyze the following lead data and provide a score from 0 to 100.\n\n");
        sb.append("Lead Data:\n");
        leadData.forEach((key, value) -> sb.append("- ").append(key).append(": ").append(value).append("\n"));
        sb.append("\nRespond in the following JSON format ONLY (no additional text):\n");
        sb.append("{\n");
        sb.append("  \"score\": <integer 0-100>,\n");
        sb.append("  \"factors\": [\"<factor1>\", \"<factor2>\", ...],\n");
        sb.append("  \"confidence\": <decimal 0.0-1.0>\n");
        sb.append("}\n");
        sb.append("\nConsider factors like: company size, title/role seniority, email domain (corporate vs free), ");
        sb.append("engagement level, industry, source channel, and completeness of information.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private LeadScoreResponse parseLeadScoreResponse(UUID leadId, String content) {
        try {
            String jsonContent = extractJson(content);
            Map<String, Object> parsed = objectMapper.readValue(jsonContent, new TypeReference<>() {});

            int score = parsed.get("score") instanceof Number n ? n.intValue() : 50;
            double confidence = parsed.get("confidence") instanceof Number n ? n.doubleValue() : 0.5;
            List<String> factors = parsed.get("factors") instanceof List<?> list
                    ? list.stream().map(Object::toString).toList()
                    : List.of("Unable to determine specific factors");

            return LeadScoreResponse.builder()
                    .leadId(leadId)
                    .score(Math.max(0, Math.min(100, score)))
                    .factors(factors)
                    .confidence(Math.max(0.0, Math.min(1.0, confidence)))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse LLM lead score response, returning default. Error: {}", e.getMessage());
            return LeadScoreResponse.builder()
                    .leadId(leadId)
                    .score(50)
                    .factors(List.of("AI analysis unavailable - default score assigned"))
                    .confidence(0.0)
                    .build();
        }
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}
