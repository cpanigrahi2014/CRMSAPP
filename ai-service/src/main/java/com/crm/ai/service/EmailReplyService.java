package com.crm.ai.service;

import com.crm.ai.dto.EmailReplyRequest;
import com.crm.ai.dto.EmailReplyResponse;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.ai.entity.EmailReplyRecord;
import com.crm.ai.repository.EmailReplyRepository;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReplyService {

    private final LlmService llmService;
    private final EmailReplyRepository emailReplyRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    @Transactional
    public EmailReplyResponse generateReply(EmailReplyRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Generating email reply for tenant: {}, from: {}", tenantId, request.getOriginalFrom());

        String prompt = buildPrompt(request);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(1024)
                .temperature(0.7)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        Map<String, Object> parsed = parseResponse(llmResponse.getContent());

        String replySubject = String.valueOf(parsed.getOrDefault("subject", "Re: " + request.getOriginalSubject()));
        String replyBody = String.valueOf(parsed.getOrDefault("body", ""));
        List<String> suggestions = extractList(parsed, "suggestions");

        EmailReplyRecord record = EmailReplyRecord.builder()
                .tenantId(tenantId)
                .originalFrom(request.getOriginalFrom())
                .originalSubject(request.getOriginalSubject())
                .originalBody(request.getOriginalBody())
                .replySubject(replySubject)
                .replyBody(replyBody)
                .tone(request.getTone())
                .suggestions(toJson(suggestions))
                .build();
        record = emailReplyRepository.save(record);

        log.info("Email reply generated and saved: {}", record.getId());
        return toResponse(record, suggestions);
    }

    @Transactional(readOnly = true)
    public List<EmailReplyResponse> getRecentReplies() {
        String tenantId = TenantContext.getTenantId();
        return emailReplyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(r -> toResponse(r, parseJsonList(r.getSuggestions()))).toList();
    }

    private String buildPrompt(EmailReplyRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI email assistant for a CRM system. Generate a professional reply to the following email.\n\n");
        sb.append("Original From: ").append(request.getOriginalFrom()).append("\n");
        sb.append("Original Subject: ").append(request.getOriginalSubject()).append("\n");
        sb.append("Original Body:\n").append(request.getOriginalBody()).append("\n\n");
        sb.append("Desired Tone: ").append(request.getTone()).append("\n");
        if (request.getAdditionalContext() != null && !request.getAdditionalContext().isBlank()) {
            sb.append("Additional Context: ").append(request.getAdditionalContext()).append("\n");
        }
        sb.append("\nRespond in the following JSON format ONLY (no additional text):\n");
        sb.append("{\n");
        sb.append("  \"subject\": \"<reply subject line>\",\n");
        sb.append("  \"body\": \"<complete reply email body with greeting and sign-off>\",\n");
        sb.append("  \"suggestions\": [\"<improvement tip 1>\", \"<improvement tip 2>\"]\n");
        sb.append("}\n");
        sb.append("\nEnsure the reply directly addresses the sender's points, is ").append(request.getTone());
        sb.append(" in tone, and includes 2-3 suggestions for improvement.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse email reply response: {}", e.getMessage());
            return Map.of("subject", "Re: Follow Up", "body",
                    content != null ? content : "Unable to generate reply.", "suggestions", List.of());
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

    private EmailReplyResponse toResponse(EmailReplyRecord r, List<String> suggestions) {
        return EmailReplyResponse.builder()
                .id(r.getId())
                .originalFrom(r.getOriginalFrom())
                .originalSubject(r.getOriginalSubject())
                .replySubject(r.getReplySubject())
                .replyBody(r.getReplyBody())
                .tone(r.getTone())
                .suggestions(suggestions)
                .createdAt(r.getCreatedAt())
                .build();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
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
