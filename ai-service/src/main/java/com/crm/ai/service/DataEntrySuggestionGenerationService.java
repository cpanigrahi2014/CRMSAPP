package com.crm.ai.service;

import com.crm.ai.dto.DataEntrySuggestionResponse;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.ai.entity.DataEntrySuggestionRecord;
import com.crm.ai.repository.DataEntrySuggestionRepository;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataEntrySuggestionGenerationService {

    private final LlmService llmService;
    private final DataEntrySuggestionRepository dataEntrySuggestionRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    @Transactional
    public List<DataEntrySuggestionResponse> generateSuggestions(String entityType, String entityId,
                                                                  String entityName, Map<String, Object> entityData) {
        String tenantId = TenantContext.getTenantId();
        log.info("Generating data entry suggestions for tenant: {}, entity: {} {}", tenantId, entityType, entityId);

        String prompt = buildPrompt(entityType, entityName, entityData);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(1024)
                .temperature(0.3)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        List<Map<String, Object>> suggestions = parseResponse(llmResponse.getContent());

        List<DataEntrySuggestionRecord> records = new ArrayList<>();
        for (Map<String, Object> s : suggestions) {
            DataEntrySuggestionRecord record = DataEntrySuggestionRecord.builder()
                    .tenantId(tenantId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName)
                    .field(String.valueOf(s.getOrDefault("field", "")))
                    .currentValue(String.valueOf(s.getOrDefault("current_value", "")))
                    .suggestedValue(String.valueOf(s.getOrDefault("suggested_value", "")))
                    .confidence(s.get("confidence") instanceof Number n
                            ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.valueOf(0.5))
                    .source(String.valueOf(s.getOrDefault("source", "AI Analysis")))
                    .build();
            records.add(record);
        }
        records = dataEntrySuggestionRepository.saveAll(records);

        log.info("Generated {} data entry suggestions for {} {}", records.size(), entityType, entityId);
        return records.stream().map(this::toResponse).toList();
    }

    private String buildPrompt(String entityType, String entityName, Map<String, Object> entityData) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a CRM data quality AI. Analyze the following entity and suggest field corrections or additions.\n\n");
        sb.append("Entity Type: ").append(entityType).append("\n");
        sb.append("Entity Name: ").append(entityName).append("\n");
        sb.append("Current Data:\n");
        entityData.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v != null ? v : "").append("\n"));
        sb.append("\nRespond with a JSON array ONLY:\n");
        sb.append("[\n");
        sb.append("  {\"field\": \"<field name>\", \"current_value\": \"<current>\", \"suggested_value\": \"<suggestion>\", \"confidence\": <0.0-1.0>, \"source\": \"<reason>\"}\n");
        sb.append("]\n");
        sb.append("\nSuggest corrections for incomplete, incorrect, or missing fields. Only suggest high-confidence changes.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseResponse(String content) {
        try {
            String json = extractJsonArray(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse data suggestions response: {}", e.getMessage());
            return List.of();
        }
    }

    private DataEntrySuggestionResponse toResponse(DataEntrySuggestionRecord r) {
        return DataEntrySuggestionResponse.builder()
                .id(r.getId())
                .entityType(r.getEntityType())
                .entityId(r.getEntityId())
                .entityName(r.getEntityName())
                .field(r.getField())
                .currentValue(r.getCurrentValue())
                .suggestedValue(r.getSuggestedValue())
                .confidence(r.getConfidence())
                .source(r.getSource())
                .accepted(r.getAccepted())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private String extractJsonArray(String content) {
        if (content == null) return "[]";
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start >= 0 && end > start) return content.substring(start, end + 1);
        return "[]";
    }
}
