package com.crm.ai.service;

import com.crm.ai.dto.AutoLeadActionRequest;
import com.crm.ai.dto.AutoLeadRequest;
import com.crm.ai.dto.AutoLeadResponse;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.ai.entity.AutoLeadRecord;
import com.crm.ai.repository.AutoLeadRepository;
import com.crm.common.exception.ResourceNotFoundException;
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
public class AutoLeadService {

    private final LlmService llmService;
    private final AutoLeadRepository autoLeadRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    @Transactional
    public AutoLeadResponse extractLead(AutoLeadRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Extracting lead from {} for tenant: {}", request.getSourceType(), tenantId);

        String prompt = buildPrompt(request);

        LlmRequest llmRequest = LlmRequest.builder()
                .model(defaultModel)
                .prompt(prompt)
                .maxTokens(512)
                .temperature(0.3)
                .build();

        LlmResponse llmResponse = llmService.call(llmRequest);
        Map<String, Object> parsed = parseResponse(llmResponse.getContent());

        String leadName = String.valueOf(parsed.getOrDefault("name", "Unknown"));
        String email = String.valueOf(parsed.getOrDefault("email", ""));
        String company = String.valueOf(parsed.getOrDefault("company", ""));
        String title = String.valueOf(parsed.getOrDefault("title", ""));
        String phone = String.valueOf(parsed.getOrDefault("phone", ""));
        String notes = String.valueOf(parsed.getOrDefault("notes", ""));
        double confidence = parsed.get("confidence") instanceof Number n ? n.doubleValue() : 0.5;

        AutoLeadRecord record = AutoLeadRecord.builder()
                .tenantId(tenantId)
                .sourceType(request.getSourceType())
                .sourceReference(request.getSourceReference())
                .leadName(leadName)
                .email(email)
                .company(company)
                .title(title)
                .phone(phone)
                .notes(notes)
                .confidence(BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, confidence))))
                .status("PENDING")
                .build();
        record = autoLeadRepository.save(record);

        log.info("Auto-lead extracted and saved: {}, name: {}", record.getId(), leadName);
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public List<AutoLeadResponse> getPendingLeads() {
        String tenantId = TenantContext.getTenantId();
        return autoLeadRepository.findByStatusAndTenantId("PENDING", tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AutoLeadResponse> getAllAutoLeads() {
        String tenantId = TenantContext.getTenantId();
        return autoLeadRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AutoLeadResponse actionAutoLead(AutoLeadActionRequest request) {
        String tenantId = TenantContext.getTenantId();
        AutoLeadRecord record = autoLeadRepository.findByIdAndTenantId(request.getAutoLeadId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Auto-lead not found: " + request.getAutoLeadId()));
        record.setStatus(request.getAction());
        record = autoLeadRepository.save(record);
        log.info("Auto-lead {} action: {}", record.getId(), request.getAction());
        return toResponse(record);
    }

    private String buildPrompt(AutoLeadRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a CRM AI assistant that extracts lead/contact information from emails and meeting notes.\n\n");
        sb.append("Source Type: ").append(request.getSourceType()).append("\n");
        if (request.getSourceReference() != null) {
            sb.append("Source Reference: ").append(request.getSourceReference()).append("\n");
        }
        sb.append("\nContent:\n").append(request.getContent()).append("\n");
        sb.append("\nExtract any potential lead information. Respond in the following JSON format ONLY (no additional text):\n");
        sb.append("{\n");
        sb.append("  \"name\": \"<full name of the potential lead>\",\n");
        sb.append("  \"email\": \"<email address if found>\",\n");
        sb.append("  \"company\": \"<company name if found>\",\n");
        sb.append("  \"title\": \"<job title if found>\",\n");
        sb.append("  \"phone\": \"<phone number if found>\",\n");
        sb.append("  \"notes\": \"<relevant context about this lead>\",\n");
        sb.append("  \"confidence\": <decimal 0.0-1.0, how confident you are this is a real lead>\n");
        sb.append("}\n");
        sb.append("\nIf a field is not found in the content, use an empty string. Be conservative with confidence scores.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse auto-lead response: {}", e.getMessage());
            return Map.of("name", "Unknown", "confidence", 0.0);
        }
    }

    private AutoLeadResponse toResponse(AutoLeadRecord r) {
        return AutoLeadResponse.builder()
                .id(r.getId())
                .sourceType(r.getSourceType())
                .sourceReference(r.getSourceReference())
                .leadName(r.getLeadName())
                .email(r.getEmail())
                .company(r.getCompany())
                .title(r.getTitle())
                .phone(r.getPhone())
                .notes(r.getNotes())
                .confidence(r.getConfidence())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) return content.substring(start, end + 1);
        return content;
    }
}
