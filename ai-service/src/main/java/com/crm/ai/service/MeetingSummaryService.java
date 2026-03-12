package com.crm.ai.service;

import com.crm.ai.dto.MeetingSummaryRequest;
import com.crm.ai.dto.MeetingSummaryResponse;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.ai.entity.MeetingSummaryRecord;
import com.crm.ai.repository.MeetingSummaryRepository;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingSummaryService {

    private final LlmService llmService;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    @Transactional
    public MeetingSummaryResponse summarizeMeeting(MeetingSummaryRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Generating meeting summary for tenant: {}, title: {}", tenantId, request.getMeetingTitle());

        String prompt = buildPrompt(request);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(2048)
                .temperature(0.4)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        Map<String, Object> parsed = parseResponse(llmResponse.getContent());

        String summary = String.valueOf(parsed.getOrDefault("summary", ""));
        List<String> actionItems = extractList(parsed, "action_items");
        List<String> keyDecisions = extractList(parsed, "key_decisions");
        List<Map<String, String>> crmUpdatesRaw = extractMapList(parsed, "crm_updates");

        LocalDateTime meetingDate = null;
        if (request.getMeetingDate() != null && !request.getMeetingDate().isBlank()) {
            try { meetingDate = LocalDateTime.parse(request.getMeetingDate()); }
            catch (Exception e) { log.warn("Could not parse meeting date: {}", request.getMeetingDate()); }
        }

        MeetingSummaryRecord record = MeetingSummaryRecord.builder()
                .tenantId(tenantId)
                .meetingTitle(request.getMeetingTitle())
                .meetingDate(meetingDate)
                .participants(toJson(request.getParticipants()))
                .transcript(request.getTranscript())
                .summary(summary)
                .actionItems(toJson(actionItems))
                .keyDecisions(toJson(keyDecisions))
                .crmUpdates(toJson(crmUpdatesRaw))
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .build();
        record = meetingSummaryRepository.save(record);

        log.info("Meeting summary generated and saved: {}", record.getId());
        return toResponse(record, actionItems, keyDecisions, crmUpdatesRaw);
    }

    @Transactional(readOnly = true)
    public List<MeetingSummaryResponse> getRecentSummaries() {
        String tenantId = TenantContext.getTenantId();
        return meetingSummaryRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::mapRecord).toList();
    }

    private String buildPrompt(MeetingSummaryRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI meeting assistant for a CRM system. Analyze the following meeting transcript/notes and provide a structured summary.\n\n");
        sb.append("Meeting Title: ").append(request.getMeetingTitle()).append("\n");
        if (request.getParticipants() != null && !request.getParticipants().isEmpty()) {
            sb.append("Participants: ").append(String.join(", ", request.getParticipants())).append("\n");
        }
        sb.append("\nTranscript/Notes:\n").append(request.getTranscript()).append("\n");
        sb.append("\nRespond in the following JSON format ONLY (no additional text):\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"<concise meeting summary in 2-4 sentences>\",\n");
        sb.append("  \"action_items\": [\"<action item 1>\", \"<action item 2>\", ...],\n");
        sb.append("  \"key_decisions\": [\"<decision 1>\", \"<decision 2>\", ...],\n");
        sb.append("  \"crm_updates\": [\n");
        sb.append("    {\"entity_type\": \"<LEAD|CONTACT|OPPORTUNITY|ACCOUNT>\", \"entity_id\": \"\", \"field\": \"<field to update>\", \"suggested_value\": \"<new value>\", \"reason\": \"<why>\"}\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("\nExtract all actionable items, decisions made, and suggest any CRM record updates based on the meeting discussion.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse meeting summary response: {}", e.getMessage());
            return Map.of("summary", content != null ? content : "Unable to generate summary.",
                    "action_items", List.of(), "key_decisions", List.of(), "crm_updates", List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractMapList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> {
                        Map<String, Object> m = (Map<String, Object>) item;
                        Map<String, String> result = new java.util.HashMap<>();
                        m.forEach((k, v) -> result.put(k, String.valueOf(v)));
                        return result;
                    }).toList();
        }
        return List.of();
    }

    private MeetingSummaryResponse mapRecord(MeetingSummaryRecord r) {
        List<String> actionItems = parseJsonStringList(r.getActionItems());
        List<String> keyDecisions = parseJsonStringList(r.getKeyDecisions());
        List<Map<String, String>> crmUpdatesRaw = parseJsonMapList(r.getCrmUpdates());
        return toResponse(r, actionItems, keyDecisions, crmUpdatesRaw);
    }

    private MeetingSummaryResponse toResponse(MeetingSummaryRecord r, List<String> actionItems,
                                               List<String> keyDecisions, List<Map<String, String>> crmUpdatesRaw) {
        List<MeetingSummaryResponse.CrmUpdateSuggestion> crmUpdates = crmUpdatesRaw.stream()
                .map(m -> MeetingSummaryResponse.CrmUpdateSuggestion.builder()
                        .entityType(m.getOrDefault("entity_type", ""))
                        .entityId(m.getOrDefault("entity_id", ""))
                        .field(m.getOrDefault("field", ""))
                        .suggestedValue(m.getOrDefault("suggested_value", ""))
                        .reason(m.getOrDefault("reason", ""))
                        .build())
                .toList();

        return MeetingSummaryResponse.builder()
                .id(r.getId())
                .meetingTitle(r.getMeetingTitle())
                .meetingDate(r.getMeetingDate())
                .participants(parseJsonStringList(r.getParticipants()))
                .summary(r.getSummary())
                .actionItems(actionItems)
                .keyDecisions(keyDecisions)
                .crmUpdates(crmUpdates)
                .relatedEntityType(r.getRelatedEntityType())
                .relatedEntityId(r.getRelatedEntityId())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() {}); }
        catch (Exception e) { return Collections.emptyList(); }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseJsonMapList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return Collections.emptyList(); }
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
