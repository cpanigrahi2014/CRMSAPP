package com.crm.ai.service;

import com.crm.ai.dto.CsvFieldDetectionRequest;
import com.crm.ai.dto.CsvFieldDetectionResponse;
import com.crm.ai.dto.CsvFieldDetectionResponse.FieldMapping;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvFieldDetectionService {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    private static final Map<String, List<String>> ENTITY_FIELDS = Map.of(
            "account", List.of("name", "industry", "website", "phone", "type", "territory", "segment",
                    "lifecycle_stage", "billing_address", "shipping_address", "annual_revenue",
                    "number_of_employees", "description"),
            "contact", List.of("first_name", "last_name", "email", "phone", "mobile_phone", "company",
                    "title", "department", "linkedin_url", "twitter_handle", "address",
                    "city", "state", "country", "zip_code", "lead_source", "lifecycle_stage"),
            "lead", List.of("first_name", "last_name", "email", "phone", "company", "title",
                    "lead_source", "status", "score", "website", "industry", "address",
                    "city", "state", "country", "notes")
    );

    public CsvFieldDetectionResponse detectFields(CsvFieldDetectionRequest request) {
        String entityType = request.getEntityType().toLowerCase();
        String[] lines = request.getCsvContent().split("\n");
        if (lines.length < 1) {
            return CsvFieldDetectionResponse.builder()
                    .entityType(entityType)
                    .fieldMappings(List.of())
                    .unmappedColumns(List.of())
                    .totalColumns(0)
                    .mappedColumns(0)
                    .build();
        }

        String[] headers = lines[0].trim().split(",");
        // Collect sample values from first data row
        String[] sampleValues = lines.length > 1 ? lines[1].trim().split(",", -1) : new String[0];

        List<String> targetFields = ENTITY_FIELDS.getOrDefault(entityType,
                ENTITY_FIELDS.get("contact"));

        String prompt = buildDetectionPrompt(headers, sampleValues, entityType, targetFields);

        LlmRequest llmRequest = LlmRequest.builder()
                .prompt(prompt)
                .maxTokens(1024)
                .temperature(0.2)
                .build();

        try {
            LlmResponse response = llmService.call(llmRequest);
            return parseDetectionResponse(response.getContent(), entityType, headers, sampleValues);
        } catch (Exception e) {
            log.warn("LLM field detection failed, falling back to heuristic: {}", e.getMessage());
            return heuristicDetection(entityType, headers, sampleValues);
        }
    }

    private String buildDetectionPrompt(String[] headers, String[] sampleValues,
                                         String entityType, List<String> targetFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a CRM data mapping assistant. Analyze these CSV column headers ");
        sb.append("and map them to CRM ").append(entityType).append(" fields.\n\n");
        sb.append("CSV Headers: ").append(String.join(", ", headers)).append("\n");
        if (sampleValues.length > 0) {
            sb.append("Sample Values: ").append(String.join(", ", sampleValues)).append("\n");
        }
        sb.append("\nAvailable CRM fields for ").append(entityType).append(": ");
        sb.append(String.join(", ", targetFields)).append("\n\n");
        sb.append("Return a JSON object with a \"mappings\" array. Each mapping has:\n");
        sb.append("- \"csv_header\": the original CSV column name\n");
        sb.append("- \"crm_field\": the matching CRM field name (or \"unmapped\" if no match)\n");
        sb.append("- \"data_type\": detected data type (string, number, email, phone, url, date, currency)\n");
        sb.append("- \"confidence\": confidence score 0.0 to 1.0\n\n");
        sb.append("Use fuzzy matching. For example, \"Company Name\" -> \"company\", ");
        sb.append("\"Annual Rev\" -> \"annual_revenue\", \"# Employees\" -> \"number_of_employees\".\n");
        sb.append("Return ONLY valid JSON, no markdown.");
        return sb.toString();
    }

    private CsvFieldDetectionResponse parseDetectionResponse(String content, String entityType,
                                                              String[] headers, String[] sampleValues) {
        List<FieldMapping> mappings = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();

        try {
            String json = content.contains("{") ? content.substring(content.indexOf("{")) : content;
            if (json.lastIndexOf("}") > 0) {
                json = json.substring(0, json.lastIndexOf("}") + 1);
            }
            JsonNode root = objectMapper.readTree(json);
            JsonNode mappingsNode = root.has("mappings") ? root.get("mappings") : root;

            if (mappingsNode.isArray()) {
                for (JsonNode node : mappingsNode) {
                    String csvHeader = node.has("csv_header") ? node.get("csv_header").asText() : "";
                    String crmField = node.has("crm_field") ? node.get("crm_field").asText() : "unmapped";
                    String dataType = node.has("data_type") ? node.get("data_type").asText() : "string";
                    double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.5;

                    String sampleValue = findSampleValue(csvHeader, headers, sampleValues);

                    if ("unmapped".equals(crmField) || crmField.isBlank()) {
                        unmapped.add(csvHeader);
                    } else {
                        mappings.add(FieldMapping.builder()
                                .csvHeader(csvHeader)
                                .crmField(crmField)
                                .dataType(dataType)
                                .confidence(confidence)
                                .sampleValue(sampleValue)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse LLM response, using heuristic: {}", e.getMessage());
            return heuristicDetection(entityType, headers, sampleValues);
        }

        return CsvFieldDetectionResponse.builder()
                .entityType(entityType)
                .fieldMappings(mappings)
                .unmappedColumns(unmapped)
                .totalColumns(headers.length)
                .mappedColumns(mappings.size())
                .build();
    }

    private CsvFieldDetectionResponse heuristicDetection(String entityType, String[] headers,
                                                          String[] sampleValues) {
        List<FieldMapping> mappings = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();
        Map<String, String> heuristics = Map.ofEntries(
                Map.entry("name", "name"), Map.entry("company name", "name"),
                Map.entry("company", "company"), Map.entry("email", "email"),
                Map.entry("e-mail", "email"), Map.entry("phone", "phone"),
                Map.entry("telephone", "phone"), Map.entry("website", "website"),
                Map.entry("url", "website"), Map.entry("industry", "industry"),
                Map.entry("first name", "first_name"), Map.entry("firstname", "first_name"),
                Map.entry("last name", "last_name"), Map.entry("lastname", "last_name"),
                Map.entry("title", "title"), Map.entry("job title", "title"),
                Map.entry("department", "department"), Map.entry("address", "address"),
                Map.entry("city", "city"), Map.entry("state", "state"),
                Map.entry("country", "country"), Map.entry("zip", "zip_code"),
                Map.entry("zipcode", "zip_code"), Map.entry("zip code", "zip_code"),
                Map.entry("description", "description"), Map.entry("notes", "notes"),
                Map.entry("revenue", "annual_revenue"), Map.entry("annual revenue", "annual_revenue"),
                Map.entry("employees", "number_of_employees"), Map.entry("# employees", "number_of_employees")
        );

        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase();
            String sampleValue = i < sampleValues.length ? sampleValues[i].trim() : "";
            String match = heuristics.get(h);
            if (match != null) {
                mappings.add(FieldMapping.builder()
                        .csvHeader(headers[i].trim())
                        .crmField(match)
                        .dataType(detectDataType(sampleValue))
                        .confidence(0.85)
                        .sampleValue(sampleValue)
                        .build());
            } else {
                unmapped.add(headers[i].trim());
            }
        }

        return CsvFieldDetectionResponse.builder()
                .entityType(entityType)
                .fieldMappings(mappings)
                .unmappedColumns(unmapped)
                .totalColumns(headers.length)
                .mappedColumns(mappings.size())
                .build();
    }

    private String detectDataType(String value) {
        if (value == null || value.isBlank()) return "string";
        if (value.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) return "email";
        if (value.matches("^[+]?[\\d\\s()-]{7,}$")) return "phone";
        if (value.matches("^https?://.*")) return "url";
        if (value.matches("^\\d+\\.?\\d*$")) return "number";
        if (value.matches("^\\$?[\\d,]+\\.?\\d*$")) return "currency";
        return "string";
    }

    private String findSampleValue(String csvHeader, String[] headers, String[] sampleValues) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(csvHeader) && i < sampleValues.length) {
                return sampleValues[i].trim();
            }
        }
        return "";
    }
}
