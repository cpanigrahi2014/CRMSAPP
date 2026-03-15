package com.crm.supportcase.listener;

import com.crm.common.event.CrmEvent;
import com.crm.common.security.TenantContext;
import com.crm.supportcase.dto.CaseResponse;
import com.crm.supportcase.dto.CreateCaseRequest;
import com.crm.supportcase.entity.SupportCase;
import com.crm.supportcase.service.CaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens for inbound email events and auto-creates support cases.
 * Scenario 19: Email to support@ → Case auto-created.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailCaseListener {

    private final CaseService caseService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "email-events", groupId = "case-service-group")
    public void handleEmailEvent(CrmEvent event) {
        try {
            if (event == null || !"EMAIL_RECEIVED".equals(event.getEventType())) return;

            log.info("Received EMAIL_RECEIVED event for potential case creation: entityId={} tenant={}",
                    event.getEntityId(), event.getTenantId());

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event.getPayload(), Map.class);

            String toAddress = (String) payload.getOrDefault("toAddress", "");
            String fromAddress = (String) payload.getOrDefault("fromAddress", "");
            String subject = (String) payload.getOrDefault("subject", "");
            String bodyText = (String) payload.getOrDefault("bodyText", "");

            // Only auto-create cases for emails sent to support addresses
            if (!isSupportAddress(toAddress)) {
                log.debug("Email to {} is not a support address, skipping case creation", toAddress);
                return;
            }

            // Set tenant context
            TenantContext.setTenantId(event.getTenantId());

            CreateCaseRequest caseRequest = CreateCaseRequest.builder()
                    .subject(subject != null && !subject.isBlank() ? subject : "Email from " + fromAddress)
                    .description(bodyText)
                    .origin(SupportCase.CaseOrigin.EMAIL)
                    .contactEmail(fromAddress)
                    .contactName(extractNameFromEmail(fromAddress))
                    .build();

            CaseResponse caseResponse = caseService.createCase(caseRequest,
                    event.getUserId() != null ? event.getUserId() : "system");

            log.info("Case auto-created from email: caseId={} caseNumber={} from={} tenant={}",
                    caseResponse.getId(), caseResponse.getCaseNumber(), fromAddress, event.getTenantId());

        } catch (Exception e) {
            log.error("Error processing EMAIL_RECEIVED event for case creation: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isSupportAddress(String toAddress) {
        if (toAddress == null || toAddress.isBlank()) return false;
        String lower = toAddress.toLowerCase();
        return lower.contains("support@") || lower.contains("help@")
                || lower.contains("service@") || lower.contains("helpdesk@");
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) return "Unknown";
        String local = email.substring(0, email.indexOf("@"));
        // Try to extract a name from the local part (e.g., john.smith → John Smith)
        return local.replace(".", " ").replace("_", " ").replace("-", " ");
    }
}
