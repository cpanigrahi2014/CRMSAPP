package com.crm.integration.service;

import com.crm.common.event.EventPublisher;
import com.crm.common.security.TenantContext;
import com.crm.integration.dto.*;
import com.crm.integration.entity.ChannelWebhookLog;
import com.crm.integration.repository.ChannelWebhookLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * Orchestrates channel integrations (WhatsApp, Email, Social Media)
 * across CRM microservices for automated lead/case/opportunity creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelIntegrationService {

    private final RestTemplate restTemplate;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final ChannelWebhookLogRepository channelLogRepository;

    @Value("${app.services.lead-url:http://localhost:9082}")
    private String leadServiceUrl;

    @Value("${app.services.case-url:http://localhost:9093}")
    private String caseServiceUrl;

    @Value("${app.services.opportunity-url:http://localhost:9085}")
    private String opportunityServiceUrl;

    @Value("${app.services.activity-url:http://localhost:9086}")
    private String activityServiceUrl;

    // ═══════════════════════════════════════════════════════════
    // SCENARIO 18: WhatsApp → Lead → Opportunity
    // ═══════════════════════════════════════════════════════════

    public WhatsAppWebhookResponse processWhatsAppMessage(WhatsAppWebhookRequest request,
                                                           String token, String tenantId) {
        log.info("Processing WhatsApp message from {} for tenant {}", request.getPhone(), tenantId);

        HttpHeaders headers = buildHeaders(token, tenantId);

        // Step 1: Create lead via lead-service /capture-phone
        UUID leadId = captureLeadFromPhone(request, headers);
        if (leadId == null) {
            return WhatsAppWebhookResponse.builder()
                    .message("Failed to create lead from WhatsApp message")
                    .build();
        }

        // Step 2: Attach WhatsApp transcript as activity
        UUID transcriptActivityId = attachWhatsAppTranscript(leadId, request, headers);

        // Step 3: Optionally convert lead → opportunity
        UUID opportunityId = null;
        String opportunityStage = null;
        if (request.isAutoConvert()) {
            Map<String, Object> oppResult = convertLeadToOpportunity(leadId, request, headers);
            if (oppResult != null) {
                opportunityId = (UUID) oppResult.get("opportunityId");
                opportunityStage = (String) oppResult.get("stage");

                // Attach transcript to opportunity as well
                if (opportunityId != null) {
                    attachTranscriptToOpportunity(opportunityId, request, headers);
                }
            }
        }

        // Publish integration event
        eventPublisher.publish("integration-events", tenantId, "system",
                "WhatsAppIntegration", leadId.toString(), "WHATSAPP_LEAD_CREATED",
                Map.of("phone", request.getPhone(),
                        "leadId", leadId.toString(),
                        "opportunityId", opportunityId != null ? opportunityId.toString() : "",
                        "autoConverted", String.valueOf(request.isAutoConvert())));

        // Persist audit log
        channelLogRepository.save(ChannelWebhookLog.builder()
                .channel("WHATSAPP")
                .eventType("WHATSAPP_LEAD_CREATED")
                .sourceIdentifier(request.getPhone())
                .leadId(leadId)
                .opportunityId(opportunityId)
                .activityId(transcriptActivityId)
                .status("SUCCESS")
                .tenantId(tenantId)
                .build());

        log.info("WhatsApp integration complete: leadId={} opportunityId={}", leadId, opportunityId);

        return WhatsAppWebhookResponse.builder()
                .leadId(leadId)
                .leadStatus(request.isAutoConvert() ? "CONVERTED" : "NEW")
                .opportunityId(opportunityId)
                .opportunityStage(opportunityStage)
                .transcriptActivityId(transcriptActivityId)
                .message("WhatsApp message processed successfully")
                .build();
    }

    private UUID captureLeadFromPhone(WhatsAppWebhookRequest request, HttpHeaders headers) {
        try {
            StringBuilder url = new StringBuilder(leadServiceUrl + "/api/v1/leads/capture-phone?phone=")
                    .append(encodeParam(request.getPhone()))
                    .append("&source=WHATSAPP");
            if (request.getFirstName() != null) {
                url.append("&firstName=").append(encodeParam(request.getFirstName()));
            }
            if (request.getLastName() != null) {
                url.append("&lastName=").append(encodeParam(request.getLastName()));
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> resp = restTemplate.exchange(url.toString(),
                    HttpMethod.POST, entity, Map.class);

            return extractIdFromResponse(resp);
        } catch (Exception e) {
            log.error("Failed to capture lead from WhatsApp phone {}: {}",
                    request.getPhone(), e.getMessage(), e);
            return null;
        }
    }

    private UUID attachWhatsAppTranscript(UUID leadId, WhatsAppWebhookRequest request, HttpHeaders headers) {
        try {
            Map<String, Object> activityBody = new LinkedHashMap<>();
            activityBody.put("type", "CALL");
            activityBody.put("subject", "WhatsApp Message from " + request.getPhone());
            activityBody.put("description", buildTranscriptDescription(request));
            activityBody.put("relatedEntityType", "Lead");
            activityBody.put("relatedEntityId", leadId.toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(activityBody, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    activityServiceUrl + "/api/v1/activities",
                    HttpMethod.POST, entity, Map.class);

            UUID activityId = extractIdFromResponse(resp);
            log.info("WhatsApp transcript attached as activity {} to lead {}", activityId, leadId);
            return activityId;
        } catch (Exception e) {
            log.warn("Failed to attach WhatsApp transcript to lead {}: {}", leadId, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> convertLeadToOpportunity(UUID leadId, WhatsAppWebhookRequest request,
                                                          HttpHeaders headers) {
        try {
            String oppName = request.getOpportunityName() != null
                    ? request.getOpportunityName()
                    : "WhatsApp Opportunity - " + request.getPhone();

            Map<String, Object> convertBody = new LinkedHashMap<>();
            convertBody.put("opportunityName", oppName);
            convertBody.put("createAccount", true);
            convertBody.put("createContact", true);
            if (request.getOpportunityAmount() != null) {
                convertBody.put("amount", new BigDecimal(request.getOpportunityAmount()));
            } else {
                convertBody.put("amount", BigDecimal.ZERO);
            }
            convertBody.put("stage", request.getOpportunityStage() != null
                    ? request.getOpportunityStage() : "PROSPECTING");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(convertBody, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    leadServiceUrl + "/api/v1/leads/" + leadId + "/convert",
                    HttpMethod.POST, entity, Map.class);

            if (resp.getBody() != null && resp.getBody().get("data") instanceof Map data) {
                Object oppId = data.get("opportunityId");
                Map<String, Object> result = new HashMap<>();
                if (oppId != null) {
                    result.put("opportunityId", UUID.fromString(oppId.toString()));
                }
                result.put("stage", request.getOpportunityStage() != null
                        ? request.getOpportunityStage() : "PROSPECTING");
                log.info("Lead {} converted: opportunityId={}", leadId, oppId);
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to convert lead {} to opportunity: {}", leadId, e.getMessage(), e);
        }
        return null;
    }

    private void attachTranscriptToOpportunity(UUID opportunityId, WhatsAppWebhookRequest request,
                                                HttpHeaders headers) {
        try {
            Map<String, Object> activityBody = new LinkedHashMap<>();
            activityBody.put("type", "CALL");
            activityBody.put("subject", "WhatsApp Transcript - " + request.getPhone());
            activityBody.put("description", buildTranscriptDescription(request));
            activityBody.put("relatedEntityType", "Opportunity");
            activityBody.put("relatedEntityId", opportunityId.toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(activityBody, headers);
            restTemplate.exchange(activityServiceUrl + "/api/v1/activities",
                    HttpMethod.POST, entity, Map.class);
            log.info("WhatsApp transcript attached to opportunity {}", opportunityId);
        } catch (Exception e) {
            log.warn("Failed to attach transcript to opportunity {}: {}", opportunityId, e.getMessage());
        }
    }

    private String buildTranscriptDescription(WhatsAppWebhookRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[WhatsApp Transcript]\n");
        sb.append("From: ").append(request.getPhone()).append("\n");
        if (request.getFirstName() != null || request.getLastName() != null) {
            sb.append("Contact: ");
            if (request.getFirstName() != null) sb.append(request.getFirstName()).append(" ");
            if (request.getLastName() != null) sb.append(request.getLastName());
            sb.append("\n");
        }
        sb.append("---\n");
        if (request.getMessage() != null) {
            sb.append(request.getMessage());
        }
        if (request.getMediaUrl() != null) {
            sb.append("\n[Media: ").append(request.getMediaUrl()).append("]");
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════
    // SCENARIO 19: Email → Case
    // ═══════════════════════════════════════════════════════════

    public EmailSupportWebhookResponse processInboundSupportEmail(EmailSupportWebhookRequest request,
                                                                    String token, String tenantId) {
        log.info("Processing inbound support email from {} to {} for tenant {}",
                request.getFromAddress(), request.getToAddress(), tenantId);

        HttpHeaders headers = buildHeaders(token, tenantId);

        // Step 1: Auto-create case from email
        Map<String, Object> caseResult = createCaseFromEmail(request, headers);
        if (caseResult == null) {
            return EmailSupportWebhookResponse.builder()
                    .message("Failed to create case from email")
                    .build();
        }

        UUID caseId = (UUID) caseResult.get("caseId");
        String caseNumber = (String) caseResult.get("caseNumber");
        String priority = (String) caseResult.get("priority");

        // Step 2: Attach email as activity on the case
        UUID emailActivityId = attachEmailAsActivity(caseId, request, headers);

        // Publish integration event
        eventPublisher.publish("integration-events", tenantId, "system",
                "EmailIntegration", caseId.toString(), "EMAIL_CASE_CREATED",
                Map.of("fromAddress", request.getFromAddress(),
                        "caseId", caseId.toString(),
                        "caseNumber", caseNumber,
                        "priority", priority));

        // Persist audit log
        channelLogRepository.save(ChannelWebhookLog.builder()
                .channel("EMAIL")
                .eventType("EMAIL_CASE_CREATED")
                .sourceIdentifier(request.getFromAddress())
                .caseId(caseId)
                .activityId(emailActivityId)
                .status("SUCCESS")
                .tenantId(tenantId)
                .build());

        log.info("Email→Case integration complete: caseId={} caseNumber={}", caseId, caseNumber);

        return EmailSupportWebhookResponse.builder()
                .caseId(caseId)
                .caseNumber(caseNumber)
                .casePriority(priority)
                .caseStatus("OPEN")
                .emailActivityId(emailActivityId)
                .message("Support case created from email")
                .build();
    }

    private Map<String, Object> createCaseFromEmail(EmailSupportWebhookRequest request, HttpHeaders headers) {
        try {
            Map<String, Object> caseBody = new LinkedHashMap<>();
            caseBody.put("subject", request.getSubject() != null
                    ? request.getSubject() : "Email from " + request.getFromAddress());
            caseBody.put("description", request.getBodyText() != null
                    ? request.getBodyText() : request.getBodyHtml());
            caseBody.put("origin", "EMAIL");
            caseBody.put("contactEmail", request.getFromAddress());
            if (request.getContactName() != null) {
                caseBody.put("contactName", request.getContactName());
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(caseBody, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    caseServiceUrl + "/api/v1/cases",
                    HttpMethod.POST, entity, Map.class);

            if (resp.getBody() != null && resp.getBody().get("data") instanceof Map data) {
                Map<String, Object> result = new HashMap<>();
                Object id = data.get("id");
                if (id != null) result.put("caseId", UUID.fromString(id.toString()));
                result.put("caseNumber", data.get("caseNumber"));
                result.put("priority", data.get("priority"));
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to create case from email {}: {}", request.getFromAddress(), e.getMessage(), e);
        }
        return null;
    }

    private UUID attachEmailAsActivity(UUID caseId, EmailSupportWebhookRequest request, HttpHeaders headers) {
        try {
            StringBuilder description = new StringBuilder();
            description.append("[Email Activity]\n");
            description.append("From: ").append(request.getFromAddress()).append("\n");
            description.append("To: ").append(request.getToAddress()).append("\n");
            if (request.getCcAddresses() != null) {
                description.append("CC: ").append(request.getCcAddresses()).append("\n");
            }
            description.append("Subject: ").append(request.getSubject() != null ? request.getSubject() : "(no subject)").append("\n");
            description.append("---\n");
            if (request.getBodyText() != null) {
                description.append(request.getBodyText());
            }

            Map<String, Object> activityBody = new LinkedHashMap<>();
            activityBody.put("type", "EMAIL");
            activityBody.put("subject", "Inbound Email: " + (request.getSubject() != null ? request.getSubject() : "(no subject)"));
            activityBody.put("description", description.toString());
            activityBody.put("relatedEntityType", "Case");
            activityBody.put("relatedEntityId", caseId.toString());
            activityBody.put("emailTo", request.getToAddress());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(activityBody, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    activityServiceUrl + "/api/v1/activities",
                    HttpMethod.POST, entity, Map.class);

            UUID activityId = extractIdFromResponse(resp);
            log.info("Email attached as activity {} to case {}", activityId, caseId);
            return activityId;
        } catch (Exception e) {
            log.warn("Failed to attach email activity to case {}: {}", caseId, e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SCENARIO 20: Instagram/Social Media → Lead
    // ═══════════════════════════════════════════════════════════

    public SocialMediaWebhookResponse processSocialMediaComment(SocialMediaWebhookRequest request,
                                                                  String token, String tenantId) {
        log.info("Processing {} comment from @{} for tenant {}",
                request.getPlatform(), request.getUsername(), tenantId);

        HttpHeaders headers = buildHeaders(token, tenantId);

        // Step 1: Create lead with source = SOCIAL_MEDIA
        UUID leadId = createLeadFromSocial(request, headers);
        if (leadId == null) {
            return SocialMediaWebhookResponse.builder()
                    .message("Failed to create lead from social media")
                    .build();
        }

        // Step 2: Attach social comment as activity/note
        attachSocialActivity(leadId, request, headers);

        // Publish integration event
        eventPublisher.publish("integration-events", tenantId, "system",
                "SocialIntegration", leadId.toString(), "SOCIAL_LEAD_CREATED",
                Map.of("platform", request.getPlatform(),
                        "username", request.getUsername(),
                        "leadId", leadId.toString()));

        // Persist audit log
        channelLogRepository.save(ChannelWebhookLog.builder()
                .channel(request.getPlatform().toUpperCase())
                .eventType("SOCIAL_LEAD_CREATED")
                .sourceIdentifier("@" + request.getUsername())
                .leadId(leadId)
                .status("SUCCESS")
                .tenantId(tenantId)
                .build());

        log.info("Social media integration complete: platform={} leadId={}", request.getPlatform(), leadId);

        return SocialMediaWebhookResponse.builder()
                .leadId(leadId)
                .leadSource(request.getPlatform())
                .leadScore(determineSocialWeight(request.getPlatform()))
                .leadStatus("NEW")
                .message("Lead created from " + request.getPlatform() + " comment")
                .build();
    }

    private UUID createLeadFromSocial(SocialMediaWebhookRequest request, HttpHeaders headers) {
        try {
            // Derive first/last name from fullName or username
            String firstName = "Unknown";
            String lastName = "Unknown";
            if (request.getFullName() != null && !request.getFullName().isBlank()) {
                String[] parts = request.getFullName().trim().split("\\s+", 2);
                firstName = parts[0];
                lastName = parts.length > 1 ? parts[1] : request.getUsername();
            } else {
                firstName = request.getUsername();
            }

            Map<String, Object> leadBody = new LinkedHashMap<>();
            leadBody.put("firstName", firstName);
            leadBody.put("lastName", lastName);
            leadBody.put("source", "SOCIAL_MEDIA");
            if (request.getEmail() != null) {
                leadBody.put("email", request.getEmail());
            }
            leadBody.put("description", buildSocialDescription(request));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(leadBody, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    leadServiceUrl + "/api/v1/leads",
                    HttpMethod.POST, entity, Map.class);

            UUID leadId = extractIdFromResponse(resp);

            // Update lead score with social weight
            if (leadId != null) {
                updateLeadScore(leadId, determineSocialWeight(request.getPlatform()), headers);
            }

            log.info("Lead created from {} @{}: {}", request.getPlatform(), request.getUsername(), leadId);
            return leadId;
        } catch (Exception e) {
            log.error("Failed to create lead from social {} @{}: {}",
                    request.getPlatform(), request.getUsername(), e.getMessage(), e);
            return null;
        }
    }

    private void updateLeadScore(UUID leadId, int socialWeight, HttpHeaders headers) {
        try {
            Map<String, Object> updateBody = new LinkedHashMap<>();
            updateBody.put("leadScore", socialWeight);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updateBody, headers);
            restTemplate.exchange(
                    leadServiceUrl + "/api/v1/leads/" + leadId,
                    HttpMethod.PUT, entity, Map.class);
            log.info("Lead {} score updated to social weight {}", leadId, socialWeight);
        } catch (Exception e) {
            log.warn("Failed to update lead score for {}: {}", leadId, e.getMessage());
        }
    }

    private void attachSocialActivity(UUID leadId, SocialMediaWebhookRequest request, HttpHeaders headers) {
        try {
            Map<String, Object> activityBody = new LinkedHashMap<>();
            activityBody.put("type", "TASK");
            activityBody.put("subject", request.getPlatform() + " comment from @" + request.getUsername());
            activityBody.put("description", buildSocialDescription(request));
            activityBody.put("relatedEntityType", "Lead");
            activityBody.put("relatedEntityId", leadId.toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(activityBody, headers);
            restTemplate.exchange(activityServiceUrl + "/api/v1/activities",
                    HttpMethod.POST, entity, Map.class);
            log.info("Social media activity attached to lead {}", leadId);
        } catch (Exception e) {
            log.warn("Failed to attach social activity to lead {}: {}", leadId, e.getMessage());
        }
    }

    private String buildSocialDescription(SocialMediaWebhookRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(request.getPlatform()).append(" Comment]\n");
        sb.append("User: @").append(request.getUsername()).append("\n");
        if (request.getFullName() != null) {
            sb.append("Name: ").append(request.getFullName()).append("\n");
        }
        if (request.getPostUrl() != null) {
            sb.append("Post: ").append(request.getPostUrl()).append("\n");
        }
        if (request.getProfileUrl() != null) {
            sb.append("Profile: ").append(request.getProfileUrl()).append("\n");
        }
        sb.append("---\n");
        if (request.getComment() != null) {
            sb.append(request.getComment());
        }
        return sb.toString();
    }

    /**
     * Returns a lead score weight based on the social platform.
     */
    private int determineSocialWeight(String platform) {
        if (platform == null) return 10;
        return switch (platform.toUpperCase()) {
            case "INSTAGRAM" -> 15;
            case "FACEBOOK" -> 12;
            case "LINKEDIN" -> 20;
            case "TWITTER", "X" -> 10;
            default -> 10;
        };
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private HttpHeaders buildHeaders(String token, String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Tenant-ID", tenantId);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private UUID extractIdFromResponse(ResponseEntity<Map> resp) {
        if (resp.getBody() != null && resp.getBody().get("data") instanceof Map data) {
            Object id = data.get("id");
            if (id != null) return UUID.fromString(id.toString());
        }
        return null;
    }

    private String encodeParam(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
