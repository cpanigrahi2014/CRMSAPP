package com.crm.supportcase.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.supportcase.dto.*;
import com.crm.supportcase.entity.SupportCase;
import com.crm.supportcase.entity.SupportCase.*;
import com.crm.supportcase.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final EventPublisher eventPublisher;

    private static final AtomicLong CASE_COUNTER = new AtomicLong(System.currentTimeMillis() % 100000);

    @Value("${app.case.escalation-hours:4}")
    private int escalationHours;

    // ═══════════════════════════════════════════════════════════════
    // CASE CREATION — with auto-priority & SLA
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public CaseResponse createCase(CreateCaseRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating case for tenant: {}", tenantId);

        SupportCase supportCase = SupportCase.builder()
                .caseNumber(generateCaseNumber())
                .subject(request.getSubject())
                .description(request.getDescription())
                .origin(request.getOrigin() != null ? request.getOrigin() : CaseOrigin.PORTAL)
                .contactName(request.getContactName())
                .contactEmail(request.getContactEmail())
                .accountName(request.getAccountName())
                .contactId(request.getContactId())
                .accountId(request.getAccountId())
                .assignedTo(request.getAssignedTo())
                .build();
        supportCase.setTenantId(tenantId);

        // Auto-assign priority based on keywords
        CasePriority autoPriority = request.getPriority() != null
                ? request.getPriority()
                : detectPriority(request.getSubject(), request.getDescription());
        supportCase.setPriority(autoPriority);

        // Start SLA timer
        supportCase.setSlaDueDate(calculateSlaDue(autoPriority));

        SupportCase saved = caseRepository.save(supportCase);
        log.info("Case created: {} priority={} sla={}", saved.getCaseNumber(), saved.getPriority(), saved.getSlaDueDate());

        eventPublisher.publish("case-events", tenantId, userId, "Case",
                saved.getId().toString(), "CASE_CREATED", toResponse(saved));

        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE & GET
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public CaseResponse updateCase(UUID caseId, UpdateCaseRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        SupportCase supportCase = caseRepository.findByIdAndTenantIdAndDeletedFalse(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "id", caseId));

        if (request.getSubject() != null) supportCase.setSubject(request.getSubject());
        if (request.getDescription() != null) supportCase.setDescription(request.getDescription());
        if (request.getStatus() != null) supportCase.setStatus(request.getStatus());
        if (request.getPriority() != null) supportCase.setPriority(request.getPriority());
        if (request.getAssignedTo() != null) supportCase.setAssignedTo(request.getAssignedTo());
        if (request.getResolutionNotes() != null) supportCase.setResolutionNotes(request.getResolutionNotes());

        // Track first response
        if (supportCase.getFirstResponseAt() == null && request.getStatus() == CaseStatus.IN_PROGRESS) {
            supportCase.setFirstResponseAt(LocalDateTime.now());
        }

        SupportCase saved = caseRepository.save(supportCase);

        eventPublisher.publish("case-events", tenantId, userId, "Case",
                saved.getId().toString(), "CASE_UPDATED", toResponse(saved));

        return toResponse(saved);
    }

    public CaseResponse getCaseById(UUID caseId) {
        String tenantId = TenantContext.getTenantId();
        SupportCase supportCase = caseRepository.findByIdAndTenantIdAndDeletedFalse(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "id", caseId));
        return toResponse(supportCase);
    }

    public CaseResponse getCaseByCaseNumber(String caseNumber) {
        String tenantId = TenantContext.getTenantId();
        SupportCase supportCase = caseRepository.findByCaseNumberAndTenantIdAndDeletedFalse(caseNumber, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "caseNumber", caseNumber));
        return toResponse(supportCase);
    }

    public PagedResponse<CaseResponse> getAllCases(int page, int size, String status, String priority, String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        Page<SupportCase> casePage;
        if (status != null && !status.isBlank()) {
            casePage = caseRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, CaseStatus.valueOf(status), pageable);
        } else if (priority != null && !priority.isBlank()) {
            casePage = caseRepository.findByTenantIdAndPriorityAndDeletedFalse(tenantId, CasePriority.valueOf(priority), pageable);
        } else {
            casePage = caseRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        }

        return PagedResponse.<CaseResponse>builder()
                .content(casePage.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
                .pageNumber(casePage.getNumber())
                .pageSize(casePage.getSize())
                .totalElements(casePage.getTotalElements())
                .totalPages(casePage.getTotalPages())
                .last(casePage.isLast())
                .first(casePage.isFirst())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // RESOLVE & CLOSE — Scenario 15
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public CaseResponse resolveCase(UUID caseId, String resolutionNotes, String userId) {
        String tenantId = TenantContext.getTenantId();
        SupportCase supportCase = caseRepository.findByIdAndTenantIdAndDeletedFalse(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "id", caseId));

        supportCase.setStatus(CaseStatus.RESOLVED);
        supportCase.setResolvedAt(LocalDateTime.now());
        if (resolutionNotes != null) supportCase.setResolutionNotes(resolutionNotes);

        // Calculate SLA compliance
        boolean slaMet = supportCase.getSlaDueDate() != null
                && supportCase.getResolvedAt().isBefore(supportCase.getSlaDueDate());
        supportCase.setSlaMet(slaMet);

        SupportCase saved = caseRepository.save(supportCase);
        log.info("Case {} resolved. SLA met: {}", saved.getCaseNumber(), slaMet);

        // Publish resolve event (triggers CSAT survey via notification)
        eventPublisher.publish("case-events", tenantId, userId, "Case",
                saved.getId().toString(), "CASE_RESOLVED", toResponse(saved));

        // Send CSAT survey notification
        if (saved.getContactEmail() != null && !saved.isCsatSent()) {
            sendCsatSurvey(saved, tenantId, userId);
        }

        return toResponse(saved);
    }

    @Transactional
    public CaseResponse closeCase(UUID caseId, String userId) {
        String tenantId = TenantContext.getTenantId();
        SupportCase supportCase = caseRepository.findByIdAndTenantIdAndDeletedFalse(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "id", caseId));

        supportCase.setStatus(CaseStatus.CLOSED);
        supportCase.setClosedAt(LocalDateTime.now());
        if (supportCase.getResolvedAt() == null) {
            supportCase.setResolvedAt(LocalDateTime.now());
        }
        if (supportCase.getSlaMet() == null) {
            boolean slaMet = supportCase.getSlaDueDate() != null
                    && LocalDateTime.now().isBefore(supportCase.getSlaDueDate());
            supportCase.setSlaMet(slaMet);
        }

        SupportCase saved = caseRepository.save(supportCase);
        log.info("Case {} closed. SLA met: {}", saved.getCaseNumber(), saved.getSlaMet());

        eventPublisher.publish("case-events", tenantId, userId, "Case",
                saved.getId().toString(), "CASE_CLOSED", toResponse(saved));

        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // CSAT SURVEY
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public CaseResponse submitCsat(UUID caseId, CsatRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        SupportCase supportCase = caseRepository.findByIdAndTenantIdAndDeletedFalse(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "id", caseId));

        supportCase.setCsatScore(request.getScore());
        supportCase.setCsatComment(request.getComment());

        SupportCase saved = caseRepository.save(supportCase);
        log.info("CSAT submitted for case {}: score={}", saved.getCaseNumber(), request.getScore());

        eventPublisher.publish("case-events", tenantId, userId, "Case",
                saved.getId().toString(), "CASE_CSAT_SUBMITTED", toResponse(saved));

        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // ESCALATION — Scenario 14 (Scheduled)
    // ═══════════════════════════════════════════════════════════════

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional
    public void checkEscalations() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(escalationHours);
        List<SupportCase> needsEscalation = caseRepository.findAllCasesNeedingEscalation(threshold);

        for (SupportCase c : needsEscalation) {
            try {
                TenantContext.setTenantId(c.getTenantId());
                c.setStatus(CaseStatus.ESCALATED);
                c.setEscalated(true);
                c.setEscalatedAt(LocalDateTime.now());
                caseRepository.save(c);

                log.warn("Case {} escalated! Not updated for {}+ hours", c.getCaseNumber(), escalationHours);

                // Notify manager
                Map<String, Object> payload = new HashMap<>();
                payload.put("caseNumber", c.getCaseNumber());
                payload.put("subject", c.getSubject());
                payload.put("priority", c.getPriority().name());
                payload.put("hoursInactive", escalationHours);
                payload.put("contactName", c.getContactName());

                eventPublisher.publish("notification-events", c.getTenantId(), "escalation-engine",
                        "Case", c.getId().toString(), "CASE_ESCALATED", payload);

                eventPublisher.publish("case-events", c.getTenantId(), "escalation-engine",
                        "Case", c.getId().toString(), "CASE_ESCALATED", toResponse(c));

            } finally {
                TenantContext.clear();
            }
        }

        if (!needsEscalation.isEmpty()) {
            log.info("Escalation check complete: {} case(s) escalated", needsEscalation.size());
        }
    }

    // Manual escalation endpoint
    @Transactional
    public CaseResponse escalateCase(UUID caseId, String userId) {
        String tenantId = TenantContext.getTenantId();
        SupportCase supportCase = caseRepository.findByIdAndTenantIdAndDeletedFalse(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "id", caseId));

        supportCase.setStatus(CaseStatus.ESCALATED);
        supportCase.setEscalated(true);
        supportCase.setEscalatedAt(LocalDateTime.now());

        SupportCase saved = caseRepository.save(supportCase);
        log.warn("Case {} manually escalated by {}", saved.getCaseNumber(), userId);

        eventPublisher.publish("case-events", tenantId, userId, "Case",
                saved.getId().toString(), "CASE_ESCALATED", toResponse(saved));

        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    // ANALYTICS
    // ═══════════════════════════════════════════════════════════════

    public CaseAnalytics getAnalytics() {
        String tenantId = TenantContext.getTenantId();
        long total = caseRepository.countByTenant(tenantId);
        long slaMet = caseRepository.countSlaMetByTenant(tenantId);
        long slaBreached = caseRepository.countSlaBreachedByTenant(tenantId);
        double slaRate = (slaMet + slaBreached) > 0 ? (double) slaMet / (slaMet + slaBreached) * 100 : 100.0;

        Map<String, Long> byStatus = caseRepository.countByStatus(tenantId).stream()
                .collect(Collectors.toMap(r -> r[0].toString(), r -> (Long) r[1]));
        Map<String, Long> byPriority = caseRepository.countByPriority(tenantId).stream()
                .collect(Collectors.toMap(r -> r[0].toString(), r -> (Long) r[1]));

        return CaseAnalytics.builder()
                .totalCases(total)
                .openCases(byStatus.getOrDefault("OPEN", 0L) + byStatus.getOrDefault("IN_PROGRESS", 0L))
                .resolvedCases(byStatus.getOrDefault("RESOLVED", 0L) + byStatus.getOrDefault("CLOSED", 0L))
                .escalatedCases(byStatus.getOrDefault("ESCALATED", 0L))
                .slaComplianceRate(Math.round(slaRate * 100.0) / 100.0)
                .slaMetCount(slaMet)
                .slaBreachedCount(slaBreached)
                .avgResolutionHours(caseRepository.avgResolutionHours(tenantId))
                .avgCsatScore(caseRepository.avgCsatScore(tenantId))
                .countByStatus(byStatus)
                .countByPriority(byPriority)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // SOFT DELETE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void deleteCase(UUID caseId, String userId) {
        String tenantId = TenantContext.getTenantId();
        SupportCase supportCase = caseRepository.findByIdAndTenantIdAndDeletedFalse(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", "id", caseId));
        supportCase.setDeleted(true);
        caseRepository.save(supportCase);

        eventPublisher.publish("case-events", tenantId, userId, "Case",
                supportCase.getId().toString(), "CASE_DELETED", null);
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIORITY AUTO-DETECTION (Scenario 13)
    // ═══════════════════════════════════════════════════════════════

    CasePriority detectPriority(String subject, String description) {
        String text = ((subject != null ? subject : "") + " " + (description != null ? description : "")).toLowerCase();

        // CRITICAL keywords
        if (text.contains("down") || text.contains("outage") || text.contains("critical")
                || text.contains("production") || text.contains("security breach")
                || text.contains("data loss") || text.contains("system down")
                || text.contains("not working") || text.contains("emergency")) {
            return CasePriority.CRITICAL;
        }

        // HIGH keywords
        if (text.contains("urgent") || text.contains("bug") || text.contains("error")
                || text.contains("broken") || text.contains("crash") || text.contains("fail")
                || text.contains("cannot login") || text.contains("blocked")
                || text.contains("integration failure")) {
            return CasePriority.HIGH;
        }

        // LOW keywords
        if (text.contains("question") || text.contains("how to") || text.contains("feature request")
                || text.contains("enhancement") || text.contains("nice to have")
                || text.contains("suggestion") || text.contains("cosmetic")) {
            return CasePriority.LOW;
        }

        return CasePriority.MEDIUM;
    }

    // ═══════════════════════════════════════════════════════════════
    // SLA CALCULATION
    // ═══════════════════════════════════════════════════════════════

    private LocalDateTime calculateSlaDue(CasePriority priority) {
        return switch (priority) {
            case CRITICAL -> LocalDateTime.now().plusHours(4);
            case HIGH     -> LocalDateTime.now().plusHours(8);
            case MEDIUM   -> LocalDateTime.now().plusHours(24);
            case LOW      -> LocalDateTime.now().plusHours(72);
        };
    }

    private String generateCaseNumber() {
        return "CS-" + String.format("%06d", CASE_COUNTER.incrementAndGet());
    }

    private void sendCsatSurvey(SupportCase supportCase, String tenantId, String userId) {
        supportCase.setCsatSent(true);
        caseRepository.save(supportCase);

        Map<String, Object> surveyPayload = new HashMap<>();
        surveyPayload.put("caseNumber", supportCase.getCaseNumber());
        surveyPayload.put("caseId", supportCase.getId().toString());
        surveyPayload.put("subject", supportCase.getSubject());
        surveyPayload.put("contactEmail", supportCase.getContactEmail());
        surveyPayload.put("contactName", supportCase.getContactName());
        surveyPayload.put("message", "Your case " + supportCase.getCaseNumber() + " has been resolved. Please rate your experience (1-5).");
        surveyPayload.put("surveyType", "CSAT");

        eventPublisher.publish("notification-events", tenantId, userId,
                "Case", supportCase.getId().toString(), "CSAT_SURVEY_SENT", surveyPayload);

        log.info("CSAT survey sent for case {} to {}", supportCase.getCaseNumber(), supportCase.getContactEmail());
    }

    // ═══════════════════════════════════════════════════════════════
    // MAPPER
    // ═══════════════════════════════════════════════════════════════

    private CaseResponse toResponse(SupportCase c) {
        return CaseResponse.builder()
                .id(c.getId())
                .caseNumber(c.getCaseNumber())
                .subject(c.getSubject())
                .description(c.getDescription())
                .status(c.getStatus())
                .priority(c.getPriority())
                .origin(c.getOrigin())
                .contactName(c.getContactName())
                .contactEmail(c.getContactEmail())
                .accountName(c.getAccountName())
                .contactId(c.getContactId())
                .accountId(c.getAccountId())
                .assignedTo(c.getAssignedTo())
                .slaDueDate(c.getSlaDueDate())
                .slaMet(c.getSlaMet())
                .escalated(c.isEscalated())
                .escalatedAt(c.getEscalatedAt())
                .resolvedAt(c.getResolvedAt())
                .closedAt(c.getClosedAt())
                .firstResponseAt(c.getFirstResponseAt())
                .csatScore(c.getCsatScore())
                .csatComment(c.getCsatComment())
                .csatSent(c.isCsatSent())
                .resolutionNotes(c.getResolutionNotes())
                .tenantId(c.getTenantId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .createdBy(c.getCreatedBy())
                .build();
    }
}
