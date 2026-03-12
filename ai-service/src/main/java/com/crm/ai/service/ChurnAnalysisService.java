package com.crm.ai.service;

import com.crm.ai.dto.ChurnPredictionResponse;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.ai.entity.ChurnPredictionRecord;
import com.crm.ai.repository.ChurnPredictionRepository;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChurnAnalysisService {

    private final LlmService llmService;
    private final ChurnPredictionRepository churnPredictionRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    @Transactional
    public ChurnPredictionResponse analyzeChurnRisk(String accountId, Map<String, Object> accountData) {
        String tenantId = TenantContext.getTenantId();
        log.info("Analyzing churn risk for tenant: {}, account: {}", tenantId, accountId);

        String prompt = buildPrompt(accountId, accountData);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(1024)
                .temperature(0.3)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        Map<String, Object> parsed = parseResponse(llmResponse.getContent());

        ChurnPredictionRecord record = ChurnPredictionRecord.builder()
                .tenantId(tenantId)
                .accountId(accountId)
                .accountName(String.valueOf(accountData.getOrDefault("accountName", "")))
                .industry(String.valueOf(accountData.getOrDefault("industry", "")))
                .annualRevenue(toBigDecimal(accountData.get("annualRevenue")))
                .riskLevel(String.valueOf(parsed.getOrDefault("risk_level", "MEDIUM")))
                .churnProbability(toBigDecimal(parsed.getOrDefault("churn_probability", 0.5)))
                .riskFactors(toJson(extractList(parsed, "risk_factors")))
                .lastActivityDays(parsed.get("last_activity_days") instanceof Number n ? n.intValue() : 0)
                .healthScore(parsed.get("health_score") instanceof Number n ? n.intValue() : 50)
                .recommendedActions(toJson(extractList(parsed, "recommended_actions")))
                .build();
        record = churnPredictionRepository.save(record);

        log.info("Churn analysis completed for account: {}, risk: {}", accountId, record.getRiskLevel());
        return toResponse(record);
    }

    private String buildPrompt(String accountId, Map<String, Object> accountData) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a CRM churn prediction AI. Analyze the following account data and predict churn risk.\n\n");
        sb.append("Account ID: ").append(accountId).append("\n");
        sb.append("Account Data:\n");
        accountData.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        sb.append("\nRespond in the following JSON format ONLY:\n");
        sb.append("{\n");
        sb.append("  \"risk_level\": \"<CRITICAL|HIGH|MEDIUM|LOW>\",\n");
        sb.append("  \"churn_probability\": <decimal 0.0-1.0>,\n");
        sb.append("  \"risk_factors\": [\"<factor1>\", \"<factor2>\", ...],\n");
        sb.append("  \"last_activity_days\": <integer>,\n");
        sb.append("  \"health_score\": <integer 0-100>,\n");
        sb.append("  \"recommended_actions\": [\"<action1>\", \"<action2>\", ...]\n");
        sb.append("}\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse churn analysis response: {}", e.getMessage());
            return Map.of("risk_level", "MEDIUM", "churn_probability", 0.5);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) return list.stream().map(Object::toString).toList();
        return List.of();
    }

    private ChurnPredictionResponse toResponse(ChurnPredictionRecord r) {
        return ChurnPredictionResponse.builder()
                .id(r.getId())
                .accountId(r.getAccountId())
                .accountName(r.getAccountName())
                .industry(r.getIndustry())
                .annualRevenue(r.getAnnualRevenue())
                .riskLevel(r.getRiskLevel())
                .churnProbability(r.getChurnProbability())
                .riskFactors(parseJsonList(r.getRiskFactors()))
                .lastActivityDays(r.getLastActivityDays())
                .healthScore(r.getHealthScore())
                .recommendedActions(parseJsonList(r.getRecommendedActions()))
                .predictedChurnDate(r.getPredictedChurnDate())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (val instanceof String s) { try { return new BigDecimal(s); } catch (Exception e) { return BigDecimal.ZERO; } }
        return BigDecimal.ZERO;
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() {}); }
        catch (Exception e) { return List.of(); }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) return content.substring(start, end + 1);
        return content;
    }
}
