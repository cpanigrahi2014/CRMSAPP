package com.crm.ai.service;

import com.crm.ai.dto.SentimentAnalysisRequest;
import com.crm.ai.dto.SentimentAnalysisResponse;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentimentAnalysisService {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    public SentimentAnalysisResponse analyze(SentimentAnalysisRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Analyzing sentiment for tenant: {}, sourceType: {}", tenantId, request.getSourceType());

        String prompt = buildPrompt(request);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(2048)
                .temperature(0.3)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        Map<String, Object> parsed = parseResponse(llmResponse.getContent());

        String overallSentiment = String.valueOf(parsed.getOrDefault("overall_sentiment", "NEUTRAL"));
        double sentimentScore = extractDouble(parsed, "sentiment_score", 0.0);
        double confidence = extractDouble(parsed, "confidence", 0.5);
        String summary = String.valueOf(parsed.getOrDefault("summary", ""));
        List<SentimentAnalysisResponse.EmotionBreakdown> emotions = extractEmotions(parsed);
        List<String> keyPhrases = extractStringList(parsed, "key_phrases");
        List<String> concerns = extractStringList(parsed, "concerns");
        List<String> positiveIndicators = extractStringList(parsed, "positive_indicators");
        String recommendation = String.valueOf(parsed.getOrDefault("recommendation", ""));

        return SentimentAnalysisResponse.builder()
                .id(UUID.randomUUID())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .overallSentiment(overallSentiment)
                .sentimentScore(sentimentScore)
                .confidence(confidence)
                .summary(summary)
                .emotions(emotions)
                .keyPhrases(keyPhrases)
                .concerns(concerns)
                .positiveIndicators(positiveIndicators)
                .recommendation(recommendation)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String buildPrompt(SentimentAnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI sentiment analysis expert for a CRM system. ");
        sb.append("Analyze the following conversation/communication for customer sentiment and emotional tone.\n\n");
        sb.append("Source Type: ").append(request.getSourceType() != null ? request.getSourceType() : "CONVERSATION").append("\n");
        if (request.getContactName() != null) {
            sb.append("Contact: ").append(request.getContactName()).append("\n");
        }
        sb.append("\nContent:\n").append(request.getContent()).append("\n");
        sb.append("\nRespond in the following JSON format ONLY:\n");
        sb.append("{\n");
        sb.append("  \"overall_sentiment\": \"<POSITIVE|NEGATIVE|NEUTRAL|MIXED>\",\n");
        sb.append("  \"sentiment_score\": <-1.0 to 1.0>,\n");
        sb.append("  \"confidence\": <0.0 to 1.0>,\n");
        sb.append("  \"summary\": \"<brief analysis summary>\",\n");
        sb.append("  \"emotions\": [{\"emotion\": \"<name>\", \"score\": <0.0-1.0>}],\n");
        sb.append("  \"key_phrases\": [\"<phrase indicating sentiment>\"],\n");
        sb.append("  \"concerns\": [\"<any customer concerns detected>\"],\n");
        sb.append("  \"positive_indicators\": [\"<positive signals from conversation>\"],\n");
        sb.append("  \"recommendation\": \"<CRM action recommendation based on sentiment>\"\n");
        sb.append("}\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse sentiment response: {}", e.getMessage());
            return Map.of("overall_sentiment", "NEUTRAL", "sentiment_score", 0.0,
                    "confidence", 0.5, "summary", content != null ? content : "",
                    "emotions", List.of(), "key_phrases", List.of(),
                    "concerns", List.of(), "positive_indicators", List.of(),
                    "recommendation", "");
        }
    }

    private double extractDouble(Map<String, Object> map, String key, double defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number num) return num.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception e) { return defaultVal; }
        }
        return defaultVal;
    }

    private List<String> extractStringList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<SentimentAnalysisResponse.EmotionBreakdown> extractEmotions(Map<String, Object> map) {
        Object val = map.get("emotions");
        if (val instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> {
                        Map<String, Object> m = (Map<String, Object>) item;
                        return SentimentAnalysisResponse.EmotionBreakdown.builder()
                                .emotion(String.valueOf(m.getOrDefault("emotion", "")))
                                .score(m.get("score") instanceof Number n ? n.doubleValue() : 0.0)
                                .build();
                    }).toList();
        }
        return List.of();
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) return content.substring(start, end + 1);
        return content;
    }
}
