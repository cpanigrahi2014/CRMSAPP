package com.crm.ai.service;

import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.ai.dto.SalesForecastResponse;
import com.crm.ai.entity.SalesForecastRecord;
import com.crm.ai.repository.SalesForecastRepository;
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
public class SalesForecastGenerationService {

    private final LlmService llmService;
    private final SalesForecastRepository salesForecastRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    @Transactional
    public SalesForecastResponse generateForecast(String period, Map<String, Object> pipelineData) {
        String tenantId = TenantContext.getTenantId();
        log.info("Generating sales forecast for tenant: {}, period: {}", tenantId, period);

        String prompt = buildPrompt(period, pipelineData);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(1024)
                .temperature(0.3)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        Map<String, Object> parsed = parseResponse(llmResponse.getContent());

        SalesForecastRecord record = SalesForecastRecord.builder()
                .tenantId(tenantId)
                .period(period)
                .periodLabel(String.valueOf(parsed.getOrDefault("period_label", period)))
                .predictedRevenue(toBigDecimal(parsed.get("predicted_revenue")))
                .bestCase(toBigDecimal(parsed.get("best_case")))
                .worstCase(toBigDecimal(parsed.get("worst_case")))
                .confidence(String.valueOf(parsed.getOrDefault("confidence", "MEDIUM")))
                .pipelineValue(toBigDecimal(parsed.get("pipeline_value")))
                .weightedPipeline(toBigDecimal(parsed.get("weighted_pipeline")))
                .closedToDate(toBigDecimal(parsed.getOrDefault("closed_to_date", 0)))
                .quota(toBigDecimal(parsed.get("quota")))
                .attainmentPct(parsed.get("attainment_pct") instanceof Number n ? n.intValue() : 0)
                .factors(toJson(extractList(parsed, "factors")))
                .build();
        record = salesForecastRepository.save(record);

        log.info("Sales forecast generated: {}", record.getId());
        return toResponse(record);
    }

    private String buildPrompt(String period, Map<String, Object> pipelineData) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a CRM sales forecasting AI. Generate a revenue forecast based on the following pipeline data.\n\n");
        sb.append("Forecast Period: ").append(period).append("\n");
        sb.append("Pipeline Data:\n");
        pipelineData.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        sb.append("\nRespond in the following JSON format ONLY:\n");
        sb.append("{\n");
        sb.append("  \"period_label\": \"<readable period label>\",\n");
        sb.append("  \"predicted_revenue\": <number>,\n");
        sb.append("  \"best_case\": <number>,\n");
        sb.append("  \"worst_case\": <number>,\n");
        sb.append("  \"confidence\": \"<HIGH|MEDIUM|LOW>\",\n");
        sb.append("  \"pipeline_value\": <number>,\n");
        sb.append("  \"weighted_pipeline\": <number>,\n");
        sb.append("  \"quota\": <number>,\n");
        sb.append("  \"attainment_pct\": <integer>,\n");
        sb.append("  \"factors\": [\"<factor1>\", \"<factor2>\", ...]\n");
        sb.append("}\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse forecast response: {}", e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) return list.stream().map(Object::toString).toList();
        return List.of();
    }

    private SalesForecastResponse toResponse(SalesForecastRecord r) {
        return SalesForecastResponse.builder()
                .id(r.getId())
                .period(r.getPeriod())
                .periodLabel(r.getPeriodLabel())
                .predictedRevenue(r.getPredictedRevenue())
                .bestCase(r.getBestCase())
                .worstCase(r.getWorstCase())
                .confidence(r.getConfidence())
                .pipelineValue(r.getPipelineValue())
                .weightedPipeline(r.getWeightedPipeline())
                .closedToDate(r.getClosedToDate())
                .quota(r.getQuota())
                .attainmentPct(r.getAttainmentPct())
                .factors(parseJsonList(r.getFactors()))
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
