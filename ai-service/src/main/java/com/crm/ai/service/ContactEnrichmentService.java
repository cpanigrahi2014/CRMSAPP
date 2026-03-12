package com.crm.ai.service;

import com.crm.ai.dto.ContactEnrichmentRequest;
import com.crm.ai.dto.ContactEnrichmentResponse;
import com.crm.ai.dto.ContactEnrichmentResponse.EnrichedField;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactEnrichmentService {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public ContactEnrichmentResponse enrichContact(ContactEnrichmentRequest request) {
        log.info("Enriching contact: {} ({})", request.getName(), request.getContactId());

        String prompt = buildEnrichmentPrompt(request);

        LlmRequest llmRequest = LlmRequest.builder()
                .prompt(prompt)
                .maxTokens(1024)
                .temperature(0.3)
                .build();

        try {
            LlmResponse response = llmService.call(llmRequest);
            return parseEnrichmentResponse(response.getContent(), request);
        } catch (Exception e) {
            log.warn("Contact enrichment failed: {}", e.getMessage());
            return ContactEnrichmentResponse.builder()
                    .contactId(request.getContactId())
                    .enrichedFields(List.of())
                    .overallConfidence(0.0)
                    .enrichmentSource("AI (failed)")
                    .build();
        }
    }

    private String buildEnrichmentPrompt(ContactEnrichmentRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a CRM data enrichment AI. Based on the information provided about a contact, ");
        sb.append("infer and suggest additional data fields that are likely accurate.\n\n");
        sb.append("Known contact data:\n");
        if (request.getName() != null) sb.append("- Name: ").append(request.getName()).append("\n");
        if (request.getEmail() != null) sb.append("- Email: ").append(request.getEmail()).append("\n");
        if (request.getCompany() != null) sb.append("- Company: ").append(request.getCompany()).append("\n");
        if (request.getTitle() != null) sb.append("- Title: ").append(request.getTitle()).append("\n");
        if (request.getPhone() != null) sb.append("- Phone: ").append(request.getPhone()).append("\n");
        if (request.getLinkedInUrl() != null) sb.append("- LinkedIn: ").append(request.getLinkedInUrl()).append("\n");

        sb.append("\nAnalyze the contact info and suggest enrichment for these fields:\n");
        sb.append("- industry (from company/email domain)\n");
        sb.append("- company_size (estimated: small/medium/large/enterprise)\n");
        sb.append("- department (from title)\n");
        sb.append("- seniority_level (from title: entry/mid/senior/executive/c-level)\n");
        sb.append("- timezone (from area code or domain)\n");
        sb.append("- social_profiles (inferred LinkedIn URL from name + company if not provided)\n");
        sb.append("- professional_summary (brief bio based on title + company)\n");

        sb.append("\nReturn JSON with \"enriched_fields\" array. Each field has:\n");
        sb.append("- \"field\": field name\n");
        sb.append("- \"current_value\": existing value or empty\n");
        sb.append("- \"suggested_value\": your suggestion\n");
        sb.append("- \"confidence\": 0.0-1.0\n");
        sb.append("- \"source\": reasoning for the suggestion\n");
        sb.append("Also include \"overall_confidence\": average confidence.\n");
        sb.append("Return ONLY valid JSON, no markdown.");
        return sb.toString();
    }

    private ContactEnrichmentResponse parseEnrichmentResponse(String content,
                                                               ContactEnrichmentRequest request) {
        List<EnrichedField> fields = new ArrayList<>();
        double overallConfidence = 0.5;

        try {
            String json = content.contains("{") ? content.substring(content.indexOf("{")) : content;
            if (json.lastIndexOf("}") > 0) {
                json = json.substring(0, json.lastIndexOf("}") + 1);
            }
            JsonNode root = objectMapper.readTree(json);

            if (root.has("overall_confidence")) {
                overallConfidence = root.get("overall_confidence").asDouble();
            }

            JsonNode fieldsNode = root.has("enriched_fields") ? root.get("enriched_fields") : null;
            if (fieldsNode != null && fieldsNode.isArray()) {
                for (JsonNode node : fieldsNode) {
                    fields.add(EnrichedField.builder()
                            .field(node.has("field") ? node.get("field").asText() : "")
                            .currentValue(node.has("current_value") ? node.get("current_value").asText() : "")
                            .suggestedValue(node.has("suggested_value") ? node.get("suggested_value").asText() : "")
                            .confidence(node.has("confidence") ? node.get("confidence").asDouble() : 0.5)
                            .source(node.has("source") ? node.get("source").asText() : "AI inference")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse enrichment response: {}", e.getMessage());
        }

        return ContactEnrichmentResponse.builder()
                .contactId(request.getContactId())
                .enrichedFields(fields)
                .overallConfidence(overallConfidence)
                .enrichmentSource("AI-powered enrichment")
                .build();
    }
}
