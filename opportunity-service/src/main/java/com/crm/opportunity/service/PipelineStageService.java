package com.crm.opportunity.service;

import com.crm.common.exception.BadRequestException;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.opportunity.dto.PipelineStageRequest;
import com.crm.opportunity.dto.PipelineStageResponse;
import com.crm.opportunity.entity.Opportunity;
import com.crm.opportunity.entity.PipelineStage;
import com.crm.opportunity.repository.PipelineStageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineStageService {

    private final PipelineStageRepository pipelineStageRepository;

    /**
     * Get all active pipeline stages for the current tenant.
     * Seeds defaults if none exist yet.
     */
    @Transactional
    public List<PipelineStageResponse> getActiveStages() {
        String tenantId = TenantContext.getTenantId();
        seedDefaultsIfEmpty(tenantId);
        return pipelineStageRepository
                .findByTenantIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(tenantId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Get all pipeline stages (including inactive) for admin management.
     */
    @Transactional
    public List<PipelineStageResponse> getAllStages() {
        String tenantId = TenantContext.getTenantId();
        seedDefaultsIfEmpty(tenantId);
        return pipelineStageRepository
                .findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PipelineStageResponse createStage(PipelineStageRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (pipelineStageRepository.findByTenantIdAndNameAndDeletedFalse(tenantId, request.getName()).isPresent()) {
            throw new BadRequestException("A stage with name '" + request.getName() + "' already exists");
        }
        PipelineStage stage = toEntity(request, tenantId);
        return toResponse(pipelineStageRepository.save(stage));
    }

    @Transactional
    public PipelineStageResponse updateStage(UUID stageId, PipelineStageRequest request) {
        String tenantId = TenantContext.getTenantId();
        PipelineStage stage = pipelineStageRepository.findById(stageId)
                .filter(s -> s.getTenantId().equals(tenantId) && !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline stage not found"));

        // Check name uniqueness if changed
        if (!stage.getName().equals(request.getName())) {
            if (pipelineStageRepository.findByTenantIdAndNameAndDeletedFalse(tenantId, request.getName()).isPresent()) {
                throw new BadRequestException("A stage with name '" + request.getName() + "' already exists");
            }
        }

        stage.setName(request.getName());
        stage.setDisplayName(request.getDisplayName());
        stage.setDisplayOrder(request.getDisplayOrder());
        if (request.getColor() != null) stage.setColor(request.getColor());
        if (request.getDefaultProbability() != null) stage.setDefaultProbability(request.getDefaultProbability());
        if (request.getForecastCategory() != null) {
            stage.setForecastCategory(Opportunity.ForecastCategory.valueOf(request.getForecastCategory()));
        }
        stage.setClosedWon(request.isClosedWon());
        stage.setClosedLost(request.isClosedLost());
        stage.setActive(request.isActive());

        return toResponse(pipelineStageRepository.save(stage));
    }

    @Transactional
    public void deleteStage(UUID stageId) {
        String tenantId = TenantContext.getTenantId();
        PipelineStage stage = pipelineStageRepository.findById(stageId)
                .filter(s -> s.getTenantId().equals(tenantId) && !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline stage not found"));
        stage.setDeleted(true);
        pipelineStageRepository.save(stage);
    }

    @Transactional
    public List<PipelineStageResponse> reorderStages(List<UUID> stageIds) {
        String tenantId = TenantContext.getTenantId();
        List<PipelineStage> stages = pipelineStageRepository
                .findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(tenantId);

        for (int i = 0; i < stageIds.size(); i++) {
            UUID id = stageIds.get(i);
            stages.stream().filter(s -> s.getId().equals(id)).findFirst()
                    .ifPresent(s -> s.setDisplayOrder(stageIds.indexOf(s.getId())));
        }
        pipelineStageRepository.saveAll(stages);
        return pipelineStageRepository
                .findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(tenantId)
                .stream().map(this::toResponse).toList();
    }

    // ─── Seed default stages matching the existing enum ──────────────
    private void seedDefaultsIfEmpty(String tenantId) {
        if (pipelineStageRepository.existsByTenantIdAndDeletedFalse(tenantId)) {
            return;
        }
        log.info("Seeding default pipeline stages for tenant: {}", tenantId);
        List<PipelineStage> defaults = List.of(
                buildDefault(tenantId, "PROSPECTING", "Prospecting", 0, "#1976d2", 10, Opportunity.ForecastCategory.PIPELINE, false, false),
                buildDefault(tenantId, "QUALIFICATION", "Qualification", 1, "#7c3aed", 25, Opportunity.ForecastCategory.PIPELINE, false, false),
                buildDefault(tenantId, "NEEDS_ANALYSIS", "Needs Analysis", 2, "#8b5cf6", 40, Opportunity.ForecastCategory.PIPELINE, false, false),
                buildDefault(tenantId, "PROPOSAL", "Proposal", 3, "#d97706", 60, Opportunity.ForecastCategory.BEST_CASE, false, false),
                buildDefault(tenantId, "NEGOTIATION", "Negotiation", 4, "#0891b2", 80, Opportunity.ForecastCategory.COMMIT, false, false),
                buildDefault(tenantId, "CLOSED_WON", "Closed Won", 5, "#059669", 100, Opportunity.ForecastCategory.CLOSED, true, false),
                buildDefault(tenantId, "CLOSED_LOST", "Closed Lost", 6, "#dc2626", 0, Opportunity.ForecastCategory.CLOSED, false, true)
        );
        pipelineStageRepository.saveAll(defaults);
    }

    private PipelineStage buildDefault(String tenantId, String name, String displayName, int order,
                                        String color, int probability, Opportunity.ForecastCategory forecast,
                                        boolean closedWon, boolean closedLost) {
        PipelineStage s = PipelineStage.builder()
                .name(name)
                .displayName(displayName)
                .displayOrder(order)
                .color(color)
                .defaultProbability(probability)
                .forecastCategory(forecast)
                .closedWon(closedWon)
                .closedLost(closedLost)
                .active(true)
                .build();
        s.setTenantId(tenantId);
        return s;
    }

    private PipelineStageResponse toResponse(PipelineStage s) {
        return PipelineStageResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .displayName(s.getDisplayName())
                .displayOrder(s.getDisplayOrder())
                .color(s.getColor())
                .defaultProbability(s.getDefaultProbability())
                .forecastCategory(s.getForecastCategory() != null ? s.getForecastCategory().name() : null)
                .closedWon(s.isClosedWon())
                .closedLost(s.isClosedLost())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private PipelineStage toEntity(PipelineStageRequest req, String tenantId) {
        PipelineStage s = PipelineStage.builder()
                .name(req.getName())
                .displayName(req.getDisplayName())
                .displayOrder(req.getDisplayOrder())
                .color(req.getColor() != null ? req.getColor() : "#1976d2")
                .defaultProbability(req.getDefaultProbability() != null ? req.getDefaultProbability() : 0)
                .closedWon(req.isClosedWon())
                .closedLost(req.isClosedLost())
                .active(req.isActive())
                .build();
        if (req.getForecastCategory() != null) {
            s.setForecastCategory(Opportunity.ForecastCategory.valueOf(req.getForecastCategory()));
        }
        s.setTenantId(tenantId);
        return s;
    }
}
