package com.crm.lead.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.BadRequestException;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.lead.dto.*;
import com.crm.lead.entity.*;
import com.crm.lead.mapper.LeadMapper;
import com.crm.lead.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadMapper leadMapper;
    private final EventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final LeadNoteRepository noteRepository;
    private final LeadTagRepository tagRepository;
    private final LeadAttachmentRepository attachmentRepository;
    private final LeadActivityRepository activityRepository;
    private final AssignmentRuleRepository assignmentRuleRepository;
    private final ScoringRuleRepository scoringRuleRepository;
    private final WebFormRepository webFormRepository;

    @Value("${app.services.opportunity-url:http://localhost:9085}")
    private String opportunityServiceUrl;

    @Value("${app.services.account-url:http://localhost:9083}")
    private String accountServiceUrl;

    @Value("${app.services.contact-url:http://localhost:9084}")
    private String contactServiceUrl;

    @Value("${app.services.activity-url:http://localhost:9086}")
    private String activityServiceUrl;

    // ── Core CRUD ──────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public LeadResponse createLead(CreateLeadRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating lead for tenant: {}", tenantId);

        Lead lead = leadMapper.toEntity(request);
        lead.setTenantId(tenantId);
        lead.setStatus(Lead.LeadStatus.NEW);
        lead.setLeadScore(0);

        // Apply assignment rules
        applyAssignmentRules(lead, tenantId);

        Lead savedLead = leadRepository.save(lead);

        // Apply scoring rules
        applyScoring(savedLead, tenantId);

        // Record timeline activity
        recordActivity(savedLead.getId(), tenantId, userId, "CREATED",
                "Lead created", lead.getFirstName() + " " + lead.getLastName(), null);

        log.info("Lead created: {} for tenant: {}", savedLead.getId(), tenantId);
        eventPublisher.publish("lead-events", tenantId, userId, "Lead",
                savedLead.getId().toString(), "LEAD_CREATED", leadMapper.toResponse(savedLead));

        return enrichResponse(leadMapper.toResponse(savedLead));
    }

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public LeadResponse updateLead(UUID leadId, UpdateLeadRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Updating lead: {} for tenant: {}", leadId, tenantId);

        Lead lead = verifyLeadExists(leadId, tenantId);
        Lead.LeadStatus oldStatus = lead.getStatus();

        if (request.getFirstName() != null) lead.setFirstName(request.getFirstName());
        if (request.getLastName() != null) lead.setLastName(request.getLastName());
        if (request.getEmail() != null) lead.setEmail(request.getEmail());
        if (request.getPhone() != null) lead.setPhone(request.getPhone());
        if (request.getCompany() != null) lead.setCompany(request.getCompany());
        if (request.getTitle() != null) lead.setTitle(request.getTitle());
        if (request.getStatus() != null) lead.setStatus(request.getStatus());
        if (request.getSource() != null) lead.setSource(request.getSource());
        if (request.getLeadScore() != null) lead.setLeadScore(request.getLeadScore());
        if (request.getDescription() != null) lead.setDescription(request.getDescription());
        if (request.getAssignedTo() != null) lead.setAssignedTo(request.getAssignedTo());
        if (request.getCampaignId() != null) lead.setCampaignId(request.getCampaignId());
        if (request.getTerritory() != null) lead.setTerritory(request.getTerritory());
        if (request.getSlaDueDate() != null) lead.setSlaDueDate(request.getSlaDueDate());

        Lead updatedLead = leadRepository.save(lead);

        // Track status changes in timeline
        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
            recordActivity(leadId, tenantId, userId, "STATUS_CHANGE",
                    "Status changed", oldStatus + " → " + request.getStatus(), null);
        }

        // Track first response (for SLA)
        if (oldStatus == Lead.LeadStatus.NEW && request.getStatus() == Lead.LeadStatus.CONTACTED
                && lead.getFirstResponseAt() == null) {
            lead.setFirstResponseAt(LocalDateTime.now());
            leadRepository.save(lead);
        }

        log.info("Lead updated: {}", leadId);
        eventPublisher.publish("lead-events", tenantId, userId, "Lead",
                updatedLead.getId().toString(), "LEAD_UPDATED", leadMapper.toResponse(updatedLead));

        return enrichResponse(leadMapper.toResponse(updatedLead));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "leads", key = "#leadId + '_' + T(com.crm.common.security.TenantContext).getTenantId()")
    public LeadResponse getLeadById(UUID leadId) {
        String tenantId = TenantContext.getTenantId();
        Lead lead = verifyLeadExists(leadId, tenantId);
        return enrichResponse(leadMapper.toResponse(lead));
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadResponse> getAllLeads(int page, int size, String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Lead> leadPage = leadRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        return buildPagedResponse(leadPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadResponse> searchLeads(String query, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Lead> leadPage = leadRepository.searchLeads(tenantId, query, pageable);
        return buildPagedResponse(leadPage);
    }

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public LeadResponse assignLead(UUID leadId, UUID assigneeId, String userId) {
        String tenantId = TenantContext.getTenantId();
        Lead lead = verifyLeadExists(leadId, tenantId);
        lead.setAssignedTo(assigneeId);
        Lead updatedLead = leadRepository.save(lead);

        recordActivity(leadId, tenantId, userId, "ASSIGNED",
                "Lead assigned", "Assigned to " + assigneeId, null);

        eventPublisher.publish("lead-events", tenantId, userId, "Lead",
                updatedLead.getId().toString(), "LEAD_ASSIGNED",
                Map.of("assignedTo", assigneeId.toString()));

        return enrichResponse(leadMapper.toResponse(updatedLead));
    }

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public LeadResponse convertLead(UUID leadId, ConvertLeadRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Converting lead: {} to opportunity", leadId);

        Lead lead = verifyLeadExists(leadId, tenantId);
        if (lead.isConverted()) {
            throw new BadRequestException("Lead is already converted");
        }

        HttpHeaders headers = buildForwardHeaders();

        // Create Account if requested
        if (request.isCreateAccount() && lead.getCompany() != null) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("name", lead.getCompany());
                body.put("phone", lead.getPhone());
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> resp = restTemplate.exchange(
                        accountServiceUrl + "/api/v1/accounts", HttpMethod.POST, entity, Map.class);
                if (resp.getBody() != null && resp.getBody().get("data") instanceof Map data) {
                    Object accId = data.get("id");
                    if (accId != null) lead.setAccountId(UUID.fromString(accId.toString()));
                }
                log.info("Account created from lead conversion: {}", lead.getAccountId());
            } catch (Exception e) {
                log.warn("Could not create account during conversion: {}", e.getMessage());
            }
        }

        // Create Contact if requested
        if (request.isCreateContact()) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("firstName", lead.getFirstName());
                body.put("lastName", lead.getLastName());
                body.put("email", lead.getEmail());
                body.put("phone", lead.getPhone());
                if (lead.getTitle() != null) body.put("title", lead.getTitle());
                if (lead.getSource() != null) body.put("leadSource", lead.getSource().name());
                if (lead.getAccountId() != null) body.put("accountId", lead.getAccountId().toString());
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> resp = restTemplate.exchange(
                        contactServiceUrl + "/api/v1/contacts", HttpMethod.POST, entity, Map.class);
                if (resp.getBody() != null && resp.getBody().get("data") instanceof Map data) {
                    Object ctId = data.get("id");
                    if (ctId != null) lead.setContactId(UUID.fromString(ctId.toString()));
                }
                log.info("Contact created from lead conversion: {}", lead.getContactId());
            } catch (Exception e) {
                log.warn("Could not create contact during conversion: {}", e.getMessage());
            }
        }

        // Create Opportunity
        try {
            Map<String, Object> oppBody = new HashMap<>();
            oppBody.put("name", request.getOpportunityName());
            oppBody.put("amount", request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO);
            oppBody.put("stage", request.getStage() != null ? request.getStage() : "PROSPECTING");
            oppBody.put("description", "Converted from lead: " + lead.getFirstName() + " " + lead.getLastName()
                    + " (" + lead.getCompany() + ")");
            if (lead.getAccountId() != null) oppBody.put("accountId", lead.getAccountId().toString());
            if (lead.getContactId() != null) oppBody.put("contactId", lead.getContactId().toString());
            if (lead.getSource() != null) oppBody.put("leadSource", lead.getSource().name());
            if (lead.getAssignedTo() != null) oppBody.put("assignedTo", lead.getAssignedTo());
            if (lead.getCampaignId() != null) oppBody.put("campaignId", lead.getCampaignId().toString());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(oppBody, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(opportunityServiceUrl + "/api/v1/opportunities",
                    HttpMethod.POST, entity, Map.class);
            if (resp.getBody() != null && resp.getBody().get("data") instanceof Map data) {
                Object oppId = data.get("id");
                if (oppId != null) lead.setOpportunityId(UUID.fromString(oppId.toString()));
            }
            log.info("Opportunity created from lead conversion: {}", lead.getOpportunityId());
        } catch (Exception e) {
            log.error("Failed to create opportunity: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create opportunity: " + e.getMessage());
        }

        lead.setConverted(true);
        lead.setStatus(Lead.LeadStatus.CONVERTED);
        Lead convertedLead = leadRepository.save(lead);

        recordActivity(leadId, tenantId, userId, "CONVERTED",
                "Lead converted", "Opportunity: " + request.getOpportunityName(), null);

        // Transfer lead activities to Contact and Opportunity in the activity-service
        transferActivitiesToConvertedEntities(leadId, lead.getContactId(), lead.getOpportunityId(), headers);

        eventPublisher.publish("lead-events", tenantId, userId, "Lead",
                convertedLead.getId().toString(), "LEAD_CONVERTED",
                Map.of("opportunityName", request.getOpportunityName(),
                        "amount", request.getAmount() != null ? request.getAmount().toString() : "0",
                        "stage", request.getStage() != null ? request.getStage() : "PROSPECTING",
                        "accountId", lead.getAccountId() != null ? lead.getAccountId().toString() : "",
                        "contactId", lead.getContactId() != null ? lead.getContactId().toString() : "",
                        "opportunityId", lead.getOpportunityId() != null ? lead.getOpportunityId().toString() : ""));

        return enrichResponse(leadMapper.toResponse(convertedLead));
    }

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public void deleteLead(UUID leadId, String userId) {
        String tenantId = TenantContext.getTenantId();
        Lead lead = verifyLeadExists(leadId, tenantId);
        lead.setDeleted(true);
        leadRepository.save(lead);
        eventPublisher.publish("lead-events", tenantId, userId, "Lead",
                lead.getId().toString(), "LEAD_DELETED", null);
    }

    // ── Notes ──────────────────────────────────────────────

    @Transactional
    public LeadNoteResponse addNote(UUID leadId, LeadNoteRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        verifyLeadExists(leadId, tenantId);

        LeadNote note = LeadNote.builder()
                .leadId(leadId).content(request.getContent())
                .tenantId(tenantId).createdBy(userId).build();
        note = noteRepository.save(note);

        recordActivity(leadId, tenantId, userId, "NOTE_ADDED",
                "Note added", truncate(request.getContent(), 80), null);

        return mapNoteResponse(note);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadNoteResponse> getNotes(UUID leadId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        verifyLeadExists(leadId, tenantId);
        Page<LeadNote> p = noteRepository.findByLeadIdAndTenantIdOrderByCreatedAtDesc(
                leadId, tenantId, PageRequest.of(page, size));
        return PagedResponse.<LeadNoteResponse>builder()
                .content(p.getContent().stream().map(this::mapNoteResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .last(p.isLast()).first(p.isFirst()).build();
    }

    @Transactional
    public void deleteNote(UUID noteId, String userId) {
        String tenantId = TenantContext.getTenantId();
        LeadNote note = noteRepository.findById(noteId)
                .filter(n -> n.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Note", "id", noteId));
        noteRepository.delete(note);
    }

    // ── Tags ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LeadTagResponse> getAllTags() {
        String tenantId = TenantContext.getTenantId();
        return tagRepository.findByTenantId(tenantId).stream().map(this::mapTagResponse).toList();
    }

    @Transactional
    public LeadTagResponse createTag(LeadTagRequest request) {
        String tenantId = TenantContext.getTenantId();
        tagRepository.findByNameAndTenantId(request.getName(), tenantId)
                .ifPresent(t -> { throw new BadRequestException("Tag already exists: " + request.getName()); });
        LeadTag tag = LeadTag.builder()
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : "#1976d2")
                .tenantId(tenantId).build();
        return mapTagResponse(tagRepository.save(tag));
    }

    @Transactional(readOnly = true)
    public List<LeadTagResponse> getLeadTags(UUID leadId) {
        String tenantId = TenantContext.getTenantId();
        verifyLeadExists(leadId, tenantId);
        return tagRepository.findTagsByLeadId(leadId).stream().map(this::mapTagResponse).toList();
    }

    @Transactional
    public void addTagToLead(UUID leadId, UUID tagId, String userId) {
        String tenantId = TenantContext.getTenantId();
        verifyLeadExists(leadId, tenantId);
        tagRepository.addTagToLead(leadId, tagId);
        recordActivity(leadId, tenantId, userId, "TAG_ADDED", "Tag added", null, null);
    }

    @Transactional
    public void removeTagFromLead(UUID leadId, UUID tagId, String userId) {
        String tenantId = TenantContext.getTenantId();
        verifyLeadExists(leadId, tenantId);
        tagRepository.removeTagFromLead(leadId, tagId);
    }

    // ── Attachments ────────────────────────────────────────

    @Transactional
    public LeadAttachmentResponse addAttachment(UUID leadId, MultipartFile file, String userId) {
        String tenantId = TenantContext.getTenantId();
        verifyLeadExists(leadId, tenantId);
        try {
            LeadAttachment att = LeadAttachment.builder()
                    .leadId(leadId).fileName(file.getOriginalFilename())
                    .fileType(file.getContentType()).fileSize(file.getSize())
                    .fileData(file.getBytes()).tenantId(tenantId).createdBy(userId).build();
            att = attachmentRepository.save(att);
            recordActivity(leadId, tenantId, userId, "ATTACHMENT_ADDED",
                    "Attachment added", file.getOriginalFilename(), null);
            return mapAttachmentResponse(att);
        } catch (IOException e) {
            throw new BadRequestException("Failed to store attachment: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<LeadAttachmentResponse> getAttachments(UUID leadId) {
        String tenantId = TenantContext.getTenantId();
        verifyLeadExists(leadId, tenantId);
        return attachmentRepository.findByLeadIdWithoutData(leadId, tenantId)
                .stream().map(this::mapAttachmentResponse).toList();
    }

    @Transactional(readOnly = true)
    public LeadAttachment getAttachmentWithData(UUID attachmentId) {
        String tenantId = TenantContext.getTenantId();
        return attachmentRepository.findByIdAndTenantId(attachmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId, String userId) {
        String tenantId = TenantContext.getTenantId();
        LeadAttachment att = attachmentRepository.findByIdAndTenantId(attachmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));
        attachmentRepository.delete(att);
    }

    // ── Activities / Timeline ──────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<LeadActivityResponse> getActivities(UUID leadId, String type, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        verifyLeadExists(leadId, tenantId);
        Pageable pageable = PageRequest.of(page, size);
        Page<LeadActivity> p = (type != null && !type.isBlank())
                ? activityRepository.findByLeadIdAndTenantIdAndActivityTypeOrderByCreatedAtDesc(leadId, tenantId, type, pageable)
                : activityRepository.findByLeadIdAndTenantIdOrderByCreatedAtDesc(leadId, tenantId, pageable);
        return PagedResponse.<LeadActivityResponse>builder()
                .content(p.getContent().stream().map(this::mapActivityResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .last(p.isLast()).first(p.isFirst()).build();
    }

    // ── Duplicate Detection ────────────────────────────────

    @Transactional(readOnly = true)
    public List<LeadResponse> findDuplicates(String email, String phone) {
        String tenantId = TenantContext.getTenantId();
        return leadRepository.findDuplicates(tenantId, email, phone)
                .stream().map(leadMapper::toResponse).toList();
    }

    // ── Bulk Operations ────────────────────────────────────

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public Map<String, Object> bulkUpdate(BulkUpdateRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        int affected = 0;
        if (Boolean.TRUE.equals(request.getDelete())) {
            affected = leadRepository.bulkDelete(request.getLeadIds(), tenantId);
        } else {
            if (request.getStatus() != null)
                affected += leadRepository.bulkUpdateStatus(request.getLeadIds(), tenantId, request.getStatus());
            if (request.getAssignTo() != null)
                affected += leadRepository.bulkUpdateAssignee(request.getLeadIds(), tenantId, request.getAssignTo());
            if (request.getTerritory() != null) {
                List<Lead> leads = leadRepository.findByIdInAndTenantIdAndDeletedFalse(request.getLeadIds(), tenantId);
                leads.forEach(l -> l.setTerritory(request.getTerritory()));
                leadRepository.saveAll(leads);
                affected += leads.size();
            }
        }
        return Map.of("affected", affected);
    }

    // ── Import / Export ────────────────────────────────────

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public Map<String, Object> importLeads(MultipartFile file, String userId) {
        String tenantId = TenantContext.getTenantId();
        int imported = 0;
        int errors = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new BadRequestException("CSV file is empty or missing header");
            }
            // Parse header to build column index map
            String[] headers = headerLine.split(",", -1);
            Map<String, Integer> colMap = new java.util.HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colMap.put(headers[i].trim().toLowerCase().replaceAll("[^a-z0-9]", ""), i);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] cols = line.split(",", -1);
                    if (cols.length < 2) continue;
                    Lead lead = Lead.builder()
                            .firstName(getCsvCol(cols, colMap, "firstname"))
                            .lastName(getCsvCol(cols, colMap, "lastname"))
                            .email(getCsvCol(cols, colMap, "email"))
                            .phone(getCsvCol(cols, colMap, "phone"))
                            .company(getCsvCol(cols, colMap, "company"))
                            .title(getCsvCol(cols, colMap, "title"))
                            .status(Lead.LeadStatus.NEW).leadScore(0)
                            .build();
                    lead.setTenantId(tenantId);
                    String sourceVal = getCsvCol(cols, colMap, "source");
                    if (sourceVal != null && !sourceVal.isBlank()) {
                        try { lead.setSource(Lead.LeadSource.valueOf(sourceVal.toUpperCase())); } catch (Exception ignored) {}
                    }
                    String descVal = getCsvCol(cols, colMap, "description");
                    if (descVal != null && !descVal.isBlank()) { lead.setDescription(descVal); }
                    String territoryVal = getCsvCol(cols, colMap, "territory");
                    if (territoryVal != null && !territoryVal.isBlank()) { lead.setTerritory(territoryVal); }
                    applyAssignmentRules(lead, tenantId);
                    leadRepository.save(lead);
                    applyScoring(lead, tenantId);
                    imported++;
                } catch (Exception e) {
                    errors++;
                    log.warn("Import row error: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read CSV: " + e.getMessage());
        }
        return Map.of("imported", imported, "errors", errors);
    }

    private String getCsvCol(String[] cols, Map<String, Integer> colMap, String field) {
        Integer idx = colMap.get(field);
        if (idx == null || idx >= cols.length) return null;
        String val = cols[idx].trim();
        return val.isEmpty() ? null : val;
    }

    @Transactional(readOnly = true)
    public byte[] exportLeads() {
        String tenantId = TenantContext.getTenantId();
        List<Lead> leads = leadRepository.findByTenantIdAndDeletedFalse(tenantId,
                PageRequest.of(0, 10000, Sort.by("createdAt").descending())).getContent();
        StringBuilder sb = new StringBuilder();
        sb.append("First Name,Last Name,Email,Phone,Company,Title,Source,Status,Score,Territory,Created At\n");
        for (Lead l : leads) {
            sb.append(esc(l.getFirstName())).append(',')
                    .append(esc(l.getLastName())).append(',')
                    .append(esc(l.getEmail())).append(',')
                    .append(esc(l.getPhone())).append(',')
                    .append(esc(l.getCompany())).append(',')
                    .append(esc(l.getTitle())).append(',')
                    .append(getCsvValue(l.getSource())).append(',')
                    .append(getCsvValue(l.getStatus())).append(',')
                    .append(l.getLeadScore()).append(',')
                    .append(esc(l.getTerritory())).append(',')
                    .append(l.getCreatedAt()).append('\n');
        }
        return sb.toString().getBytes();
    }

    // ── Assignment Rules ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<AssignmentRuleResponse> getAssignmentRules() {
        String tenantId = TenantContext.getTenantId();
        return assignmentRuleRepository.findByTenantIdOrderByPriorityDesc(tenantId)
                .stream().map(this::mapAssignmentRuleResponse).toList();
    }

    @Transactional
    public AssignmentRuleResponse createAssignmentRule(AssignmentRuleRequest request) {
        String tenantId = TenantContext.getTenantId();
        AssignmentRule rule = AssignmentRule.builder()
                .name(request.getName())
                .criteriaField(request.getCriteriaField())
                .criteriaOperator(request.getCriteriaOperator() != null ? request.getCriteriaOperator() : "EQUALS")
                .criteriaValue(request.getCriteriaValue())
                .assignTo(request.getAssignTo())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .tenantId(tenantId).build();
        return mapAssignmentRuleResponse(assignmentRuleRepository.save(rule));
    }

    @Transactional
    public void deleteAssignmentRule(UUID ruleId) {
        String tenantId = TenantContext.getTenantId();
        AssignmentRule rule = assignmentRuleRepository.findById(ruleId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("AssignmentRule", "id", ruleId));
        assignmentRuleRepository.delete(rule);
    }

    // ── Scoring Rules ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ScoringRule> getScoringRules() {
        return scoringRuleRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    @Transactional
    public ScoringRule createScoringRule(ScoringRule rule) {
        rule.setTenantId(TenantContext.getTenantId());
        return scoringRuleRepository.save(rule);
    }

    @Transactional
    public void deleteScoringRule(UUID ruleId) {
        String tenantId = TenantContext.getTenantId();
        ScoringRule rule = scoringRuleRepository.findById(ruleId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("ScoringRule", "id", ruleId));
        scoringRuleRepository.delete(rule);
    }

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public LeadResponse recalculateScore(UUID leadId, String userId) {
        String tenantId = TenantContext.getTenantId();
        Lead lead = verifyLeadExists(leadId, tenantId);
        int oldScore = lead.getLeadScore();
        applyScoring(lead, tenantId);
        if (lead.getLeadScore() != oldScore) {
            recordActivity(leadId, tenantId, userId, "SCORE_CHANGED",
                    "Score recalculated", oldScore + " → " + lead.getLeadScore(), null);
        }
        return enrichResponse(leadMapper.toResponse(lead));
    }

    // ── Analytics ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public LeadAnalyticsResponse getAnalytics() {
        String tenantId = TenantContext.getTenantId();
        long total = leadRepository.countByTenantIdAndDeletedFalse(tenantId);
        long converted = leadRepository.countByTenantIdAndConvertedTrueAndDeletedFalse(tenantId);
        Double avgScore = leadRepository.avgLeadScore(tenantId);
        long sla = leadRepository.findLeadsPastSla(tenantId).size();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        leadRepository.countByStatus(tenantId).forEach(r ->
                byStatus.put(r[0] != null ? r[0].toString() : "UNKNOWN", (Long) r[1]));

        Map<String, Long> bySource = new LinkedHashMap<>();
        leadRepository.countBySource(tenantId).forEach(r ->
                bySource.put(r[0] != null ? r[0].toString() : "UNKNOWN", (Long) r[1]));

        return LeadAnalyticsResponse.builder()
                .totalLeads(total).convertedLeads(converted)
                .conversionRate(total > 0 ? (double) converted / total * 100.0 : 0)
                .averageScore(avgScore).byStatus(byStatus).bySource(bySource)
                .slaBreached(sla).build();
    }

    // ── SLA Tracking ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LeadResponse> getLeadsPastSla() {
        String tenantId = TenantContext.getTenantId();
        return leadRepository.findLeadsPastSla(tenantId)
                .stream().map(l -> enrichResponse(leadMapper.toResponse(l))).toList();
    }

    // ── Campaign Tracking ──────────────────────────────────

    @Transactional(readOnly = true)
    public List<LeadResponse> getLeadsByCampaign(UUID campaignId) {
        String tenantId = TenantContext.getTenantId();
        return leadRepository.findByCampaignIdAndTenantIdAndDeletedFalse(campaignId, tenantId)
                .stream().map(l -> enrichResponse(leadMapper.toResponse(l))).toList();
    }

    // ── Web Forms ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WebForm> getWebForms() {
        return webFormRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    @Transactional
    public WebForm createWebForm(WebForm form) {
        form.setTenantId(TenantContext.getTenantId());
        return webFormRepository.save(form);
    }

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public LeadResponse submitWebForm(UUID formId, CreateLeadRequest request) {
        String tenantId = TenantContext.getTenantId();
        WebForm form = webFormRepository.findByIdAndActiveTrueAndTenantId(formId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WebForm", "id", formId));

        Lead lead = leadMapper.toEntity(request);
        lead.setTenantId(tenantId);
        lead.setStatus(Lead.LeadStatus.NEW);
        lead.setLeadScore(0);
        if (form.getSource() != null) {
            try { lead.setSource(Lead.LeadSource.valueOf(form.getSource())); } catch (Exception ignored) {}
        }
        if (form.getAssignTo() != null) lead.setAssignedTo(form.getAssignTo());

        applyAssignmentRules(lead, tenantId);
        Lead saved = leadRepository.save(lead);
        applyScoring(saved, tenantId);

        recordActivity(saved.getId(), tenantId, "SYSTEM", "CREATED",
                "Lead from web form", form.getName(), null);

        return enrichResponse(leadMapper.toResponse(saved));
    }

    // ── Email Capture ──────────────────────────────────────

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public LeadResponse captureEmail(String email, String source) {
        String tenantId = TenantContext.getTenantId();
        // Check if lead with this email already exists
        List<Lead> existing = leadRepository.findDuplicates(tenantId, email, null);
        if (!existing.isEmpty()) {
            return enrichResponse(leadMapper.toResponse(existing.get(0)));
        }
        Lead lead = Lead.builder()
                .firstName("Unknown").lastName("Unknown")
                .email(email).status(Lead.LeadStatus.NEW).leadScore(0).build();
        lead.setTenantId(tenantId);
        if (source != null) {
            try { lead.setSource(Lead.LeadSource.valueOf(source.toUpperCase())); } catch (Exception ignored) {}
        }
        applyAssignmentRules(lead, tenantId);
        Lead saved = leadRepository.save(lead);
        applyScoring(saved, tenantId);

        log.info("Lead captured from email {} → id={}", email, saved.getId());
        eventPublisher.publish("lead-events", tenantId, "system", "Lead",
                saved.getId().toString(), "LEAD_CREATED", leadMapper.toResponse(saved));

        return enrichResponse(leadMapper.toResponse(saved));
    }

    @Transactional
    public void recordEmailActivity(UUID leadId, String tenantId, String fromEmail,
                                     String subject, String bodyText, String emailMessageId) {
        String metadata = String.format(
                "{\"fromEmail\":\"%s\",\"subject\":\"%s\",\"emailMessageId\":\"%s\"}",
                fromEmail.replace("\"", "\\\""),
                (subject != null ? subject.replace("\"", "\\\"") : ""),
                (emailMessageId != null ? emailMessageId : ""));
        recordActivity(leadId, tenantId, "system", "EMAIL_RECEIVED",
                "Inbound email from " + fromEmail,
                subject != null ? subject : "(no subject)",
                metadata);
        log.info("Recorded EMAIL_RECEIVED activity on lead {} from {}", leadId, fromEmail);
    }

    // ── Phone Capture (WhatsApp / SMS) ───────────────────

    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public LeadResponse capturePhone(String phone, String source, String firstName, String lastName) {
        String tenantId = TenantContext.getTenantId();
        String normalizedPhone = normalizePhone(phone);

        // Check if lead with this phone already exists
        List<Lead> existing = leadRepository.findDuplicates(tenantId, null, normalizedPhone);
        if (!existing.isEmpty()) {
            return enrichResponse(leadMapper.toResponse(existing.get(0)));
        }

        Lead lead = Lead.builder()
                .firstName(firstName != null ? firstName : "Unknown")
                .lastName(lastName != null ? lastName : "Unknown")
                .phone(normalizedPhone)
                .status(Lead.LeadStatus.NEW)
                .leadScore(0)
                .territory(resolveTerritory(normalizedPhone))
                .build();
        lead.setTenantId(tenantId);

        if (source != null) {
            try { lead.setSource(Lead.LeadSource.valueOf(source.toUpperCase())); } catch (Exception ignored) {}
        } else {
            lead.setSource(Lead.LeadSource.WHATSAPP);
        }

        applyAssignmentRules(lead, tenantId);
        Lead saved = leadRepository.save(lead);
        applyScoring(saved, tenantId);

        log.info("Lead captured from phone {} → id={} territory={}", normalizedPhone, saved.getId(), saved.getTerritory());
        eventPublisher.publish("lead-events", tenantId, "system", "Lead",
                saved.getId().toString(), "LEAD_CREATED", leadMapper.toResponse(saved));

        return enrichResponse(leadMapper.toResponse(saved));
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^+\\d]", "");
        // If it starts with + keep it, otherwise assume US/default
        if (!digits.startsWith("+") && digits.length() == 10) {
            digits = "+1" + digits;
        }
        return digits;
    }

    private String resolveTerritory(String phone) {
        if (phone == null) return "UNKNOWN";
        if (phone.startsWith("+1")) return "US";
        if (phone.startsWith("+44")) return "EMEA";
        if (phone.startsWith("+91")) return "APAC";
        if (phone.startsWith("+61")) return "APAC";
        if (phone.startsWith("+49") || phone.startsWith("+33") || phone.startsWith("+34")
                || phone.startsWith("+39") || phone.startsWith("+31")) return "EMEA";
        if (phone.startsWith("+55")) return "LATAM";
        if (phone.startsWith("+52")) return "LATAM";
        if (phone.startsWith("+81") || phone.startsWith("+82") || phone.startsWith("+86")) return "APAC";
        return "GLOBAL";
    }

    // ── Territory Management ───────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<LeadResponse> getLeadsByTerritory(String territory, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Lead> leadPage = leadRepository.findByTenantIdAndTerritoryAndDeletedFalse(tenantId, territory, pageable);
        return buildPagedResponse(leadPage);
    }

    // ── Private Helpers ────────────────────────────────────

    private void applyAssignmentRules(Lead lead, String tenantId) {
        List<AssignmentRule> rules = assignmentRuleRepository.findByTenantIdAndActiveTrueOrderByPriorityDesc(tenantId);
        for (AssignmentRule rule : rules) {
            if (matchesRule(rule, lead)) {
                if ("ROUND_ROBIN".equals(rule.getAssignmentType()) && rule.getRoundRobinMembers() != null) {
                    // Round-robin assignment
                    UUID assignee = getNextRoundRobinAssignee(rule);
                    if (assignee != null) {
                        lead.setAssignedTo(assignee);
                        log.debug("Round-robin rule '{}' matched, assigning to {}", rule.getName(), assignee);
                    }
                } else if (rule.getAssignTo() != null) {
                    lead.setAssignedTo(rule.getAssignTo());
                    log.debug("Assignment rule '{}' matched, assigning to {}", rule.getName(), rule.getAssignTo());
                }
                break;
            }
        }
    }

    private UUID getNextRoundRobinAssignee(AssignmentRule rule) {
        try {
            String[] members = rule.getRoundRobinMembers().replace("[", "").replace("]", "")
                    .replace("\"", "").split(",");
            if (members.length == 0) return rule.getAssignTo();
            int idx = rule.getRoundRobinIndex() != null ? rule.getRoundRobinIndex() : 0;
            idx = idx % members.length;
            UUID assignee = UUID.fromString(members[idx].trim());
            rule.setRoundRobinIndex((idx + 1) % members.length);
            assignmentRuleRepository.save(rule);
            return assignee;
        } catch (Exception e) {
            log.warn("Round-robin parsing failed for rule '{}', falling back to direct: {}", rule.getName(), e.getMessage());
            return rule.getAssignTo();
        }
    }

    private void applyScoring(Lead lead, String tenantId) {
        List<ScoringRule> rules = scoringRuleRepository.findByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenantId);
        int score = 0;
        for (ScoringRule rule : rules) {
            if (matchesScoringRule(rule, lead)) {
                score += rule.getScoreDelta();
            }
        }
        if (!rules.isEmpty()) {
            lead.setLeadScore(Math.max(0, score));
            leadRepository.save(lead);
        }
    }

    private boolean matchesRule(AssignmentRule rule, Lead lead) {
        String fieldValue = getLeadFieldValue(lead, rule.getCriteriaField());
        if (fieldValue == null) return false;
        String cv = rule.getCriteriaValue();
        return switch (rule.getCriteriaOperator().toUpperCase()) {
            case "EQUALS" -> fieldValue.equalsIgnoreCase(cv);
            case "CONTAINS" -> fieldValue.toLowerCase().contains(cv.toLowerCase());
            case "STARTS_WITH" -> fieldValue.toLowerCase().startsWith(cv.toLowerCase());
            case "IN" -> Arrays.asList(cv.split(",")).stream()
                    .map(String::trim).anyMatch(v -> v.equalsIgnoreCase(fieldValue));
            default -> false;
        };
    }

    private boolean matchesScoringRule(ScoringRule rule, Lead lead) {
        String fieldValue = getLeadFieldValue(lead, rule.getCriteriaField());
        if (fieldValue == null) return false;
        String cv = rule.getCriteriaValue();
        return switch (rule.getCriteriaOperator().toUpperCase()) {
            case "EQUALS" -> fieldValue.equalsIgnoreCase(cv);
            case "CONTAINS" -> fieldValue.toLowerCase().contains(cv.toLowerCase());
            case "STARTS_WITH" -> fieldValue.toLowerCase().startsWith(cv.toLowerCase());
            case "IN" -> Arrays.asList(cv.split(",")).stream()
                    .map(String::trim).anyMatch(v -> v.equalsIgnoreCase(fieldValue));
            default -> false;
        };
    }

    private String getLeadFieldValue(Lead lead, String field) {
        return switch (field.toLowerCase()) {
            case "source" -> lead.getSource() != null ? lead.getSource().name() : null;
            case "status" -> lead.getStatus() != null ? lead.getStatus().name() : null;
            case "company" -> lead.getCompany();
            case "territory" -> lead.getTerritory();
            case "email" -> lead.getEmail();
            case "email_domain" -> {
                if (lead.getEmail() != null && lead.getEmail().contains("@"))
                    yield lead.getEmail().substring(lead.getEmail().indexOf("@") + 1);
                yield null;
            }
            case "title" -> lead.getTitle();
            case "phone" -> lead.getPhone();
            default -> null;
        };
    }

    private void recordActivity(UUID leadId, String tenantId, String userId,
                                String type, String title, String description, String metadata) {
        try {
            LeadActivity activity = LeadActivity.builder()
                    .leadId(leadId).activityType(type).title(title)
                    .description(description).metadata(metadata)
                    .tenantId(tenantId).createdBy(userId).build();
            activityRepository.save(activity);
        } catch (Exception e) {
            log.warn("Could not record activity: {}", e.getMessage());
        }
    }

    /**
     * After conversion, re-link any activity-service activities from the lead to the new contact/opportunity.
     * Also copies lead_activities timeline entries as new activity-service records on the contact.
     */
    @SuppressWarnings("unchecked")
    private void transferActivitiesToConvertedEntities(UUID leadId, UUID contactId, UUID opportunityId,
                                                       HttpHeaders headers) {
        try {
            // 1. Re-link existing activity-service activities from this lead to the contact
            String url = activityServiceUrl + "/api/v1/activities/related/" + leadId + "?page=0&size=100";
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            if (resp.getBody() != null && resp.getBody().get("data") instanceof Map pagedData) {
                Object contentObj = pagedData.get("content");
                if (contentObj instanceof List<?> activities) {
                    for (Object actObj : activities) {
                        if (actObj instanceof Map act) {
                            String actId = act.get("id") != null ? act.get("id").toString() : null;
                            if (actId == null) continue;

                            // Move to contact if available, otherwise to opportunity
                            UUID targetId = contactId != null ? contactId : opportunityId;
                            String targetType = contactId != null ? "CONTACT" : "OPPORTUNITY";
                            if (targetId == null) continue;

                            Map<String, Object> updateBody = new HashMap<>();
                            updateBody.put("relatedEntityType", targetType);
                            updateBody.put("relatedEntityId", targetId.toString());
                            HttpEntity<Map<String, Object>> updateEntity = new HttpEntity<>(updateBody, headers);
                            restTemplate.exchange(activityServiceUrl + "/api/v1/activities/" + actId,
                                    HttpMethod.PUT, updateEntity, Map.class);
                            log.info("Transferred activity {} to {} {}", actId, targetType, targetId);
                        }
                    }
                }
            }

            // 2. Copy lead timeline entries (lead_activities) as activity-service records on contact/opportunity
            List<LeadActivity> leadActivities = activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
            for (LeadActivity la : leadActivities) {
                if ("CONVERTED".equals(la.getActivityType())) continue; // skip the conversion entry itself

                // Create on contact
                if (contactId != null) {
                    createActivityServiceRecord(la, "CONTACT", contactId, headers);
                }
                // Create on opportunity
                if (opportunityId != null) {
                    createActivityServiceRecord(la, "OPPORTUNITY", opportunityId, headers);
                }
            }
            log.info("Lead activities copied to converted entities for lead {}", leadId);
        } catch (Exception e) {
            log.warn("Could not transfer activities during conversion: {}", e.getMessage());
        }
    }

    private void createActivityServiceRecord(LeadActivity la, String entityType, UUID entityId,
                                              HttpHeaders headers) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("type", "TASK"); // default type for transferred activities
            body.put("subject", la.getTitle());
            body.put("description", la.getDescription());
            body.put("relatedEntityType", entityType);
            body.put("relatedEntityId", entityId.toString());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.exchange(activityServiceUrl + "/api/v1/activities",
                    HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            log.warn("Could not create activity-service record for {}: {}", la.getActivityType(), e.getMessage());
        }
    }

    private Lead verifyLeadExists(UUID leadId, String tenantId) {
        return leadRepository.findByIdAndTenantIdAndDeletedFalse(leadId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", leadId));
    }

    private LeadResponse enrichResponse(LeadResponse response) {
        try {
            List<LeadTagResponse> tags = tagRepository.findTagsByLeadId(response.getId())
                    .stream().map(this::mapTagResponse).toList();
            response.setTags(tags);
        } catch (Exception e) {
            response.setTags(List.of());
        }
        return response;
    }

    private PagedResponse<LeadResponse> buildPagedResponse(Page<Lead> page) {
        return PagedResponse.<LeadResponse>builder()
                .content(page.getContent().stream()
                        .map(l -> enrichResponse(leadMapper.toResponse(l))).toList())
                .pageNumber(page.getNumber()).pageSize(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .last(page.isLast()).first(page.isFirst()).build();
    }

    private HttpHeaders buildForwardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String authHeader = attrs.getRequest().getHeader("Authorization");
            if (authHeader != null) headers.set("Authorization", authHeader);
        }
        return headers;
    }

    // ── DTO Mappers ────────────────────────────────────────

    private LeadNoteResponse mapNoteResponse(LeadNote n) {
        return LeadNoteResponse.builder()
                .id(n.getId()).leadId(n.getLeadId()).content(n.getContent())
                .createdBy(n.getCreatedBy()).createdAt(n.getCreatedAt()).updatedAt(n.getUpdatedAt()).build();
    }

    private LeadTagResponse mapTagResponse(LeadTag t) {
        return LeadTagResponse.builder()
                .id(t.getId()).name(t.getName()).color(t.getColor()).createdAt(t.getCreatedAt()).build();
    }

    private LeadAttachmentResponse mapAttachmentResponse(LeadAttachment a) {
        return LeadAttachmentResponse.builder()
                .id(a.getId()).leadId(a.getLeadId()).fileName(a.getFileName())
                .fileType(a.getFileType()).fileSize(a.getFileSize())
                .createdBy(a.getCreatedBy()).createdAt(a.getCreatedAt()).build();
    }

    private LeadActivityResponse mapActivityResponse(LeadActivity a) {
        return LeadActivityResponse.builder()
                .id(a.getId()).leadId(a.getLeadId()).activityType(a.getActivityType())
                .title(a.getTitle()).description(a.getDescription()).metadata(a.getMetadata())
                .createdBy(a.getCreatedBy()).createdAt(a.getCreatedAt()).build();
    }

    private AssignmentRuleResponse mapAssignmentRuleResponse(AssignmentRule r) {
        return AssignmentRuleResponse.builder()
                .id(r.getId()).name(r.getName()).criteriaField(r.getCriteriaField())
                .criteriaOperator(r.getCriteriaOperator()).criteriaValue(r.getCriteriaValue())
            .assignTo(r.getAssignTo())
            .assignmentType(r.getAssignmentType()).roundRobinMembers(r.getRoundRobinMembers())
            .priority(r.getPriority()).active(r.isActive())
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt()).build();
    }

    private String getCsvValue(Object val) { return val != null ? val.toString() : ""; }

    private String esc(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n"))
            return "\"" + val.replace("\"", "\"\"") + "\"";
        return val;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
