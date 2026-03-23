package com.crm.ai.service;

import com.crm.ai.dto.CsvFieldDetectionRequest;
import com.crm.ai.dto.CsvFieldDetectionResponse;
import com.crm.ai.dto.CsvFieldDetectionResponse.FieldMapping;
import com.crm.ai.dto.CsvFieldDetectionResponse.IndustryFieldInfo;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // ── Industry-specific field templates ──────────────────────────
    private static final Map<String, Map<String, List<IndustryFieldInfo>>> INDUSTRY_FIELDS = new HashMap<>();

    static {
        // Real Estate
        INDUSTRY_FIELDS.put("real_estate", Map.of(
                "lead", List.of(
                        new IndustryFieldInfo("listing_price", "Listing Price", "currency", false),
                        new IndustryFieldInfo("property_type", "Property Type", "picklist", false),
                        new IndustryFieldInfo("bedrooms", "Bedrooms", "number", false),
                        new IndustryFieldInfo("bathrooms", "Bathrooms", "number", false),
                        new IndustryFieldInfo("square_footage", "Square Footage", "number", false),
                        new IndustryFieldInfo("mls_number", "MLS Number", "text", false),
                        new IndustryFieldInfo("listing_status", "Listing Status", "picklist", false),
                        new IndustryFieldInfo("property_address", "Property Address", "text", false),
                        new IndustryFieldInfo("budget_min", "Budget Min", "currency", false),
                        new IndustryFieldInfo("budget_max", "Budget Max", "currency", false)
                ),
                "account", List.of(
                        new IndustryFieldInfo("license_number", "License Number", "text", false),
                        new IndustryFieldInfo("brokerage_name", "Brokerage Name", "text", false),
                        new IndustryFieldInfo("service_area", "Service Area", "text", false),
                        new IndustryFieldInfo("total_listings", "Total Listings", "number", false)
                ),
                "contact", List.of(
                        new IndustryFieldInfo("property_preference", "Property Preference", "text", false),
                        new IndustryFieldInfo("pre_approved", "Pre-Approved", "boolean", false),
                        new IndustryFieldInfo("buyer_or_seller", "Buyer/Seller", "picklist", false),
                        new IndustryFieldInfo("preferred_location", "Preferred Location", "text", false)
                )
        ));

        // Healthcare
        INDUSTRY_FIELDS.put("healthcare", Map.of(
                "lead", List.of(
                        new IndustryFieldInfo("patient_id", "Patient ID", "text", false),
                        new IndustryFieldInfo("insurance_provider", "Insurance Provider", "text", false),
                        new IndustryFieldInfo("referral_doctor", "Referral Doctor", "text", false),
                        new IndustryFieldInfo("appointment_type", "Appointment Type", "picklist", false),
                        new IndustryFieldInfo("urgency_level", "Urgency Level", "picklist", false)
                ),
                "account", List.of(
                        new IndustryFieldInfo("facility_type", "Facility Type", "picklist", false),
                        new IndustryFieldInfo("bed_count", "Bed Count", "number", false),
                        new IndustryFieldInfo("npi_number", "NPI Number", "text", false),
                        new IndustryFieldInfo("specialties", "Specialties", "text", false)
                ),
                "contact", List.of(
                        new IndustryFieldInfo("medical_license", "Medical License", "text", false),
                        new IndustryFieldInfo("specialty", "Specialty", "text", false),
                        new IndustryFieldInfo("credentials", "Credentials", "text", false)
                )
        ));

        // Technology / SaaS
        INDUSTRY_FIELDS.put("technology", Map.of(
                "lead", List.of(
                        new IndustryFieldInfo("tech_stack", "Tech Stack", "text", false),
                        new IndustryFieldInfo("current_solution", "Current Solution", "text", false),
                        new IndustryFieldInfo("license_count", "License Count", "number", false),
                        new IndustryFieldInfo("contract_end_date", "Contract End Date", "date", false),
                        new IndustryFieldInfo("use_case", "Use Case", "text", false)
                ),
                "account", List.of(
                        new IndustryFieldInfo("tech_stack", "Tech Stack", "text", false),
                        new IndustryFieldInfo("arr", "ARR", "currency", false),
                        new IndustryFieldInfo("subscription_tier", "Subscription Tier", "picklist", false),
                        new IndustryFieldInfo("deployment_type", "Deployment Type", "picklist", false)
                ),
                "contact", List.of(
                        new IndustryFieldInfo("github_url", "GitHub URL", "url", false),
                        new IndustryFieldInfo("technical_role", "Technical Role", "boolean", false)
                )
        ));

        // Finance / Insurance
        INDUSTRY_FIELDS.put("finance", Map.of(
                "lead", List.of(
                        new IndustryFieldInfo("policy_type", "Policy Type", "picklist", false),
                        new IndustryFieldInfo("coverage_amount", "Coverage Amount", "currency", false),
                        new IndustryFieldInfo("risk_profile", "Risk Profile", "picklist", false),
                        new IndustryFieldInfo("premium_budget", "Premium Budget", "currency", false)
                ),
                "account", List.of(
                        new IndustryFieldInfo("aum", "Assets Under Management", "currency", false),
                        new IndustryFieldInfo("regulatory_id", "Regulatory ID", "text", false),
                        new IndustryFieldInfo("account_tier", "Account Tier", "picklist", false)
                ),
                "contact", List.of(
                        new IndustryFieldInfo("financial_advisor", "Financial Advisor", "boolean", false),
                        new IndustryFieldInfo("certification", "Certification", "text", false)
                )
        ));

        // Education
        INDUSTRY_FIELDS.put("education", Map.of(
                "lead", List.of(
                        new IndustryFieldInfo("program_interest", "Program Interest", "text", false),
                        new IndustryFieldInfo("enrollment_term", "Enrollment Term", "text", false),
                        new IndustryFieldInfo("gpa", "GPA", "number", false),
                        new IndustryFieldInfo("financial_aid", "Financial Aid Needed", "boolean", false)
                ),
                "account", List.of(
                        new IndustryFieldInfo("institution_type", "Institution Type", "picklist", false),
                        new IndustryFieldInfo("student_count", "Student Count", "number", false),
                        new IndustryFieldInfo("accreditation", "Accreditation", "text", false)
                ),
                "contact", List.of(
                        new IndustryFieldInfo("academic_title", "Academic Title", "text", false),
                        new IndustryFieldInfo("faculty_department", "Faculty Department", "text", false)
                )
        ));

        // Manufacturing
        INDUSTRY_FIELDS.put("manufacturing", Map.of(
                "lead", List.of(
                        new IndustryFieldInfo("product_interest", "Product Interest", "text", false),
                        new IndustryFieldInfo("order_volume", "Order Volume", "number", false),
                        new IndustryFieldInfo("delivery_timeline", "Delivery Timeline", "text", false)
                ),
                "account", List.of(
                        new IndustryFieldInfo("plant_count", "Plant Count", "number", false),
                        new IndustryFieldInfo("iso_certification", "ISO Certification", "text", false),
                        new IndustryFieldInfo("production_capacity", "Production Capacity", "text", false)
                ),
                "contact", List.of(
                        new IndustryFieldInfo("plant_location", "Plant Location", "text", false),
                        new IndustryFieldInfo("procurement_role", "Procurement Role", "boolean", false)
                )
        ));
    }

    /** Get supported industries list */
    public List<String> getSupportedIndustries() {
        return new ArrayList<>(INDUSTRY_FIELDS.keySet());
    }

    /** Get industry-specific fields for an entity type */
    public List<IndustryFieldInfo> getIndustryFields(String industry, String entityType) {
        if (industry == null || industry.isBlank()) return List.of();
        String key = normalizeIndustry(industry);
        Map<String, List<IndustryFieldInfo>> entityMap = INDUSTRY_FIELDS.get(key);
        if (entityMap == null) return List.of();
        return entityMap.getOrDefault(entityType.toLowerCase(), List.of());
    }

    public CsvFieldDetectionResponse detectFields(CsvFieldDetectionRequest request) {
        String entityType = request.getEntityType().toLowerCase();
        String industry = request.getIndustry();
        List<String> customFields = request.getCustomFields();
        String[] lines = request.getCsvContent().split("\n");
        if (lines.length < 1) {
            return CsvFieldDetectionResponse.builder()
                    .entityType(entityType)
                    .fieldMappings(List.of())
                    .unmappedColumns(List.of())
                    .totalColumns(0)
                    .mappedColumns(0)
                    .industryFields(List.of())
                    .industry(industry)
                    .build();
        }

        String[] headers = lines[0].trim().split(",");
        String[] sampleValues = lines.length > 1 ? lines[1].trim().split(",", -1) : new String[0];

        // Build combined target field list: base + industry + custom
        List<String> baseFields = ENTITY_FIELDS.getOrDefault(entityType,
                ENTITY_FIELDS.get("contact"));
        List<IndustryFieldInfo> industryFieldInfos = getIndustryFields(industry, entityType);
        List<String> industryFieldNames = industryFieldInfos.stream()
                .map(IndustryFieldInfo::getFieldName).collect(Collectors.toList());

        List<String> targetFields = Stream.of(
                baseFields.stream(),
                industryFieldNames.stream(),
                customFields != null ? customFields.stream() : Stream.<String>empty()
        ).flatMap(s -> s).distinct().collect(Collectors.toList());

        String prompt = buildDetectionPrompt(headers, sampleValues, entityType, targetFields);

        LlmRequest llmRequest = LlmRequest.builder()
                .prompt(prompt)
                .maxTokens(1024)
                .temperature(0.2)
                .build();

        try {
            LlmResponse response = llmService.call(llmRequest);
            CsvFieldDetectionResponse result = parseDetectionResponse(
                    response.getContent(), entityType, headers, sampleValues);
            result.setIndustryFields(industryFieldInfos);
            result.setIndustry(industry);
            return result;
        } catch (Exception e) {
            log.warn("LLM field detection failed, falling back to heuristic: {}", e.getMessage());
            CsvFieldDetectionResponse result = heuristicDetection(entityType, headers,
                    sampleValues, industryFieldNames, customFields);
            result.setIndustryFields(industryFieldInfos);
            result.setIndustry(industry);
            return result;
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
                                                          String[] sampleValues,
                                                          List<String> industryFieldNames,
                                                          List<String> customFields) {
        List<FieldMapping> mappings = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();

        // Base heuristics
        Map<String, String> heuristics = new HashMap<>(Map.ofEntries(
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
        ));

        // Add industry field heuristics (snake_case → match header variations)
        if (industryFieldNames != null) {
            for (String f : industryFieldNames) {
                String normalized = f.toLowerCase().replace("_", " ");
                heuristics.put(normalized, f);
                heuristics.put(f.toLowerCase(), f);
                heuristics.put(f.toLowerCase().replace("_", ""), f);
            }
        }

        // Add custom field heuristics
        if (customFields != null) {
            for (String f : customFields) {
                String normalized = f.toLowerCase().replace("_", " ");
                heuristics.put(normalized, f);
                heuristics.put(f.toLowerCase(), f);
                heuristics.put(f.toLowerCase().replace("_", ""), f);
            }
        }

        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase();
            String sampleValue = i < sampleValues.length ? sampleValues[i].trim() : "";
            String match = heuristics.get(h);
            if (match != null) {
                boolean isIndustryField = industryFieldNames != null && industryFieldNames.contains(match);
                boolean isCustomField = customFields != null && customFields.contains(match);
                mappings.add(FieldMapping.builder()
                        .csvHeader(headers[i].trim())
                        .crmField(match)
                        .dataType(detectDataType(sampleValue))
                        .confidence(0.85)
                        .sampleValue(sampleValue)
                        .isIndustryField(isIndustryField)
                        .isCustomField(isCustomField)
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

    private String normalizeIndustry(String industry) {
        if (industry == null) return "";
        String normalized = industry.toLowerCase().trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
        // Map common variations
        if (normalized.contains("real") && normalized.contains("estate")) return "real_estate";
        if (normalized.contains("health") || normalized.contains("medical") || normalized.contains("pharma")) return "healthcare";
        if (normalized.contains("tech") || normalized.contains("software") || normalized.contains("saas") || normalized.contains("it_")) return "technology";
        if (normalized.contains("financ") || normalized.contains("bank") || normalized.contains("insur")) return "finance";
        if (normalized.contains("educ") || normalized.contains("university") || normalized.contains("school")) return "education";
        if (normalized.contains("manufactur") || normalized.contains("industrial")) return "manufacturing";
        return normalized;
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
