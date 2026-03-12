package com.crm.ai.service;

import com.crm.ai.dto.TranscriptionRequest;
import com.crm.ai.dto.TranscriptionResponse;
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
public class TranscriptionService {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    public TranscriptionResponse transcribe(TranscriptionRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Generating transcription for tenant: {}, sourceType: {}", tenantId, request.getSourceType());

        String prompt = buildPrompt(request);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(3000)
                .temperature(0.2)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        Map<String, Object> parsed = parseResponse(llmResponse.getContent());

        String fullTranscript = String.valueOf(parsed.getOrDefault("full_transcript", request.getContent()));
        String summary = String.valueOf(parsed.getOrDefault("summary", ""));
        List<String> keyTopics = extractStringList(parsed, "key_topics");
        List<TranscriptionResponse.TranscriptSegment> segments = extractSegments(parsed);

        return TranscriptionResponse.builder()
                .id(UUID.randomUUID())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .fullTranscript(fullTranscript)
                .segments(segments)
                .keyTopics(keyTopics)
                .summary(summary)
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String buildPrompt(TranscriptionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI transcription assistant for a CRM system. ");
        sb.append("Analyze and structure the following conversation content.\n\n");
        sb.append("Source Type: ").append(request.getSourceType() != null ? request.getSourceType() : "CONVERSATION").append("\n");
        if (request.getSpeakers() != null && !request.getSpeakers().isEmpty()) {
            sb.append("Known speakers: ").append(String.join(", ", request.getSpeakers())).append("\n");
        }
        sb.append("\nContent:\n").append(request.getContent()).append("\n");
        sb.append("\nRespond in the following JSON format ONLY:\n");
        sb.append("{\n");
        sb.append("  \"full_transcript\": \"<cleaned, formatted full transcript>\",\n");
        sb.append("  \"segments\": [{\"speaker\": \"<name>\", \"text\": \"<what they said>\", \"timestamp\": \"<if available>\"}],\n");
        sb.append("  \"key_topics\": [\"<topic 1>\", \"<topic 2>\"],\n");
        sb.append("  \"summary\": \"<2-3 sentence summary of the conversation>\"\n");
        sb.append("}\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse transcription response: {}", e.getMessage());
            return Map.of("full_transcript", content != null ? content : "",
                    "segments", List.of(), "key_topics", List.of(), "summary", "");
        }
    }

    private List<String> extractStringList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<TranscriptionResponse.TranscriptSegment> extractSegments(Map<String, Object> map) {
        Object val = map.get("segments");
        if (val instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> {
                        Map<String, Object> m = (Map<String, Object>) item;
                        return TranscriptionResponse.TranscriptSegment.builder()
                                .speaker(String.valueOf(m.getOrDefault("speaker", "Unknown")))
                                .text(String.valueOf(m.getOrDefault("text", "")))
                                .timestamp(String.valueOf(m.getOrDefault("timestamp", "")))
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
