package com.crm.email.service;

import com.crm.email.dto.*;
import com.crm.email.entity.EmailTemplate;
import com.crm.email.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository repo;

    /* ── CRUD ─────────────────────────────────────────────────── */

    public Page<EmailTemplateDto> list(String tenantId, Pageable pageable) {
        return repo.findByTenantIdAndDeletedFalse(tenantId, pageable).map(this::toDto);
    }

    public EmailTemplateDto getById(String tenantId, UUID id) {
        return toDto(find(tenantId, id));
    }

    public List<EmailTemplateDto> listActive(String tenantId) {
        return repo.findByTenantIdAndIsActiveTrueAndDeletedFalse(tenantId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public Page<EmailTemplateDto> search(String tenantId, String query, Pageable pageable) {
        return repo.search(tenantId, query, pageable)
                .map(this::toDto);
    }

    @Transactional
    public EmailTemplateDto create(String tenantId, String userId, CreateEmailTemplateRequest req) {
        EmailTemplate t = new EmailTemplate();
        t.setTenantId(tenantId);
        t.setCreatedBy(userId);
        t.setName(req.getName());
        t.setSubject(req.getSubject());
        t.setBodyHtml(req.getBodyHtml());
        t.setBodyText(req.getBodyText());
        t.setCategory(req.getCategory());
        t.setVariables(req.getVariables());
        t.setActive(true);
        t.setUsageCount(0);
        return toDto(repo.save(t));
    }

    @Transactional
    public EmailTemplateDto update(String tenantId, UUID id, UpdateEmailTemplateRequest req) {
        EmailTemplate t = find(tenantId, id);
        if (req.getName() != null)     t.setName(req.getName());
        if (req.getSubject() != null)  t.setSubject(req.getSubject());
        if (req.getBodyHtml() != null) t.setBodyHtml(req.getBodyHtml());
        if (req.getBodyText() != null) t.setBodyText(req.getBodyText());
        if (req.getCategory() != null) t.setCategory(req.getCategory());
        if (req.getVariables() != null) t.setVariables(req.getVariables());
        if (req.getIsActive() != null) t.setActive(req.getIsActive());
        return toDto(repo.save(t));
    }

    @Transactional
    public void delete(String tenantId, UUID id) {
        EmailTemplate t = find(tenantId, id);
        t.setDeleted(true);
        repo.save(t);
    }

    /* ── Template Rendering ──────────────────────────────────── */

    /**
     * Resolve a template's subject + body with variables.
     * Variables in the template use the format {{variableName}}.
     */
    public RenderedTemplate render(String tenantId, UUID templateId, Map<String, String> variables) {
        EmailTemplate t = find(tenantId, templateId);
        repo.incrementUsageCount(templateId);
        String subject = substituteVars(t.getSubject(), variables);
        String body    = substituteVars(t.getBodyHtml() != null ? t.getBodyHtml() : t.getBodyText(), variables);
        return new RenderedTemplate(subject, body);
    }

    /** Preview a template with sample data. */
    public RenderedTemplate preview(String tenantId, UUID templateId, Map<String, String> sampleData) {
        EmailTemplate t = find(tenantId, templateId);
        // If no sample data provided, use placeholder values
        Map<String, String> vars = sampleData != null ? sampleData : extractDefaultVars(t);
        String subject = substituteVars(t.getSubject(), vars);
        String body    = substituteVars(t.getBodyHtml() != null ? t.getBodyHtml() : t.getBodyText(), vars);
        return new RenderedTemplate(subject, body);
    }

    /* ── Helpers ──────────────────────────────────────────────── */

    private EmailTemplate find(String tenantId, UUID id) {
        return repo.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Email template not found: " + id));
    }

    private String substituteVars(String text, Map<String, String> vars) {
        if (text == null || vars == null) return text;
        String result = text;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", e.getValue() != null ? e.getValue() : "");
        }
        return result;
    }

    /** Extract variable names from template and provide default placeholder values. */
    private Map<String, String> extractDefaultVars(EmailTemplate t) {
        Map<String, String> vars = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        String all = (t.getSubject() != null ? t.getSubject() : "") + " " +
                     (t.getBodyHtml() != null ? t.getBodyHtml() : "") + " " +
                     (t.getBodyText() != null ? t.getBodyText() : "");
        Matcher matcher = pattern.matcher(all);
        while (matcher.find()) {
            vars.putIfAbsent(matcher.group(1), "[" + matcher.group(1) + "]");
        }
        return vars;
    }

    private EmailTemplateDto toDto(EmailTemplate t) {
        return EmailTemplateDto.builder()
                .id(t.getId()).name(t.getName())
                .subject(t.getSubject()).bodyHtml(t.getBodyHtml())
                .bodyText(t.getBodyText()).category(t.getCategory())
                .variables(t.getVariables()).isActive(t.isActive())
                .usageCount(t.getUsageCount()).createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    /* ── Inner record ────────────────────────────────────────── */
    public record RenderedTemplate(String subject, String bodyHtml) {}
}
