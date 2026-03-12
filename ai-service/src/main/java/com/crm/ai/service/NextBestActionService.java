package com.crm.ai.service;

import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.ai.dto.NextBestActionRequest;
import com.crm.ai.dto.NextBestActionResponse;
import com.crm.ai.dto.NextBestActionResponse.SuggestedAction;
import com.crm.common.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NextBestActionService {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    public NextBestActionResponse getNextBestActions(NextBestActionRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Getting next best actions for entity: {} ({}) for tenant: {}",
                request.getEntityId(), request.getEntityType(), tenantId);

        String prompt = buildNextBestActionPrompt(request);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(768)
                .temperature(0.5)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        NextBestActionResponse response = parseNextBestActionResponse(llmResponse.getContent());

        log.info("Generated {} next best actions for entity: {}", response.getActions().size(), request.getEntityId());
        return response;
    }

    private String buildNextBestActionPrompt(NextBestActionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a CRM strategy advisor. Based on the following entity context, suggest the next best actions.\n\n");
        sb.append("Entity Type: ").append(request.getEntityType()).append("\n");
        sb.append("Entity ID: ").append(request.getEntityId()).append("\n");
        sb.append("Context:\n");
        request.getContext().forEach((key, value) -> sb.append("- ").append(key).append(": ").append(value).append("\n"));
        sb.append("\nProvide 3-5 suggested actions. Respond in the following JSON format ONLY (no additional text):\n");
        sb.append("{\n");
        sb.append("  \"actions\": [\n");
        sb.append("    {\"action\": \"<action description>\", \"reason\": \"<why this action>\", \"priority\": <1-5>},\n");
        sb.append("    ...\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("\nPriority: 1 = highest, 5 = lowest. Consider timing, relationship stage, and engagement history.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private NextBestActionResponse parseNextBestActionResponse(String content) {
        try {
            String jsonContent = extractJson(content);
            Map<String, Object> parsed = objectMapper.readValue(jsonContent, new TypeReference<>() {});

            List<Map<String, Object>> actionsList = (List<Map<String, Object>>) parsed.get("actions");
            List<SuggestedAction> actions = new ArrayList<>();

            if (actionsList != null) {
                for (Map<String, Object> actionMap : actionsList) {
                    actions.add(SuggestedAction.builder()
                            .action(String.valueOf(actionMap.getOrDefault("action", "Review entity")))
                            .reason(String.valueOf(actionMap.getOrDefault("reason", "General follow-up")))
                            .priority(actionMap.get("priority") instanceof Number n ? n.intValue() : 3)
                            .build());
                }
            }

            if (actions.isEmpty()) {
                actions.add(SuggestedAction.builder()
                        .action("Follow up with contact")
                        .reason("Maintain engagement")
                        .priority(2)
                        .build());
            }

            return NextBestActionResponse.builder().actions(actions).build();
        } catch (Exception e) {
            log.warn("Failed to parse next best action response, returning default. Error: {}", e.getMessage());
            return NextBestActionResponse.builder()
                    .actions(List.of(SuggestedAction.builder()
                            .action("Review entity and follow up")
                            .reason("AI analysis unavailable - default recommendation")
                            .priority(3)
                            .build()))
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
