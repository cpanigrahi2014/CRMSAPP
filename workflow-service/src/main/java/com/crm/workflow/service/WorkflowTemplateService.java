package com.crm.workflow.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.security.TenantContext;
import com.crm.workflow.dto.WorkflowTemplateResponse;
import com.crm.workflow.entity.WorkflowTemplate;
import com.crm.workflow.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTemplateService {

    private final WorkflowTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public PagedResponse<WorkflowTemplateResponse> getTemplates(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<WorkflowTemplate> p = templateRepository.findAvailableTemplates(tenantId, PageRequest.of(page, size));
        return PagedResponse.<WorkflowTemplateResponse>builder()
                .content(p.getContent().stream().map(this::toResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WorkflowTemplateResponse> getByEntityType(String entityType) {
        String tenantId = TenantContext.getTenantId();
        return templateRepository.findByEntityTypeAndTenantIdOrTenantIdIsNull(entityType, tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public WorkflowTemplateResponse createTemplate(WorkflowTemplate template) {
        template.setTenantId(TenantContext.getTenantId());
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public void incrementPopularity(UUID templateId) {
        templateRepository.findById(templateId).ifPresent(t -> {
            t.setPopularity(t.getPopularity() + 1);
            templateRepository.save(t);
        });
    }

    private WorkflowTemplateResponse toResponse(WorkflowTemplate t) {
        return WorkflowTemplateResponse.builder()
                .id(t.getId()).name(t.getName()).description(t.getDescription())
                .category(t.getCategory()).entityType(t.getEntityType())
                .triggerEvent(t.getTriggerEvent())
                .conditionsJson(t.getConditionsJson()).actionsJson(t.getActionsJson())
                .canvasLayout(t.getCanvasLayout())
                .popularity(t.getPopularity()).isSystem(t.getIsSystem())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
