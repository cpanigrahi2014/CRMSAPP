package com.crm.ai.service;

import com.crm.ai.dto.EmailDraftRequest;
import com.crm.ai.dto.EmailDraftResponse;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDraftService {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    public EmailDraftResponse generateEmailDraft(EmailDraftRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Generating email draft for tenant: {}, recipient: {}", tenantId, request.getTo());

        String prompt = buildEmailDraftPrompt(request);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(1024)
                .temperature(0.7)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        EmailDraftResponse response = parseEmailDraftResponse(llmResponse.getContent());

        log.info("Email draft generated for tenant: {}, subject: {}", tenantId, response.getSubject());
        return response;
    }

    private String buildEmailDraftPrompt(EmailDraftRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a professional email writer for a CRM system. Draft an email based on the following parameters.\n\n");
        sb.append("Recipient: ").append(request.getTo()).append("\n");
        sb.append("Subject Context: ").append(request.getSubjectContext()).append("\n");
        sb.append("Tone: ").append(request.getTone()).append("\n");
        if (request.getContext() != null && !request.getContext().isBlank()) {
            sb.append("Additional Context: ").append(request.getContext()).append("\n");
        }
        sb.append("\nRespond in the following JSON format ONLY (no additional text):\n");
        sb.append("{\n");
        sb.append("  \"subject\": \"<email subject line>\",\n");
        sb.append("  \"body\": \"<complete email body with proper greeting and sign-off>\",\n");
        sb.append("  \"suggestions\": [\"<improvement tip 1>\", \"<improvement tip 2>\", ...]\n");
        sb.append("}\n");
        sb.append("\nEnsure the email is professional, concise, and matches the specified tone. ");
        sb.append("Include 2-3 suggestions for how the email could be further improved.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private EmailDraftResponse parseEmailDraftResponse(String content) {
        try {
            String jsonContent = extractJson(content);
            Map<String, Object> parsed = objectMapper.readValue(jsonContent, new TypeReference<>() {});

            String subject = String.valueOf(parsed.getOrDefault("subject", "Follow Up"));
            String body = String.valueOf(parsed.getOrDefault("body", ""));
            List<String> suggestions = parsed.get("suggestions") instanceof List<?> list
                    ? list.stream().map(Object::toString).toList()
                    : List.of("Consider personalizing the greeting", "Add a clear call-to-action");

            return EmailDraftResponse.builder()
                    .subject(subject)
                    .body(body)
                    .suggestions(suggestions)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse email draft response, returning raw content. Error: {}", e.getMessage());
            return EmailDraftResponse.builder()
                    .subject("Follow Up")
                    .body(content != null ? content : "Unable to generate email draft. Please try again.")
                    .suggestions(List.of("AI parsing failed - consider refining your request"))
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
