package com.crm.opportunity.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.BadRequestException;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.opportunity.dto.*;
import com.crm.opportunity.entity.*;
import com.crm.opportunity.mapper.OpportunityMapper;
import com.crm.opportunity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityProductRepository productRepository;
    private final OpportunityCompetitorRepository competitorRepository;
    private final OpportunityActivityRepository activityRepository;
    private final OpportunityCollaboratorRepository collaboratorRepository;
    private final OpportunityNoteRepository noteRepository;
    private final OpportunityReminderRepository reminderRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final SalesQuotaRepository salesQuotaRepository;
    private final OpportunityMapper opportunityMapper;
    private final EventPublisher eventPublisher;

    // ─── 1. Opportunity Creation ─────────────────────────────────────
    @Transactional
    @CacheEvict(value = "opportunities", allEntries = true)
    public OpportunityResponse createOpportunity(CreateOpportunityRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating opportunity for tenant: {}", tenantId);

        Opportunity opportunity = opportunityMapper.toEntity(request);
        opportunity.setTenantId(tenantId);

        if (opportunity.getStage() == null) {
            opportunity.setStage(Opportunity.OpportunityStage.PROSPECTING);
        }
        if (opportunity.getForecastCategory() == null) {
            opportunity.setForecastCategory(Opportunity.ForecastCategory.PIPELINE);
        }
        // Auto-set probability based on stage if not provided
        if (opportunity.getProbability() == null) {
            opportunity.setProbability(getStageProbabilityWeight(opportunity.getStage()));
        }

        // Validate close date is not in the past
        if (opportunity.getCloseDate() != null && opportunity.getCloseDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Close date cannot be in the past");
        }

        Opportunity saved = opportunityRepository.save(opportunity);
        recordActivity(saved.getId(), tenantId, "CREATED", "Opportunity created: " + saved.getName(), userId);

        eventPublisher.publish("opportunity-events", tenantId, userId, "Opportunity",
                saved.getId().toString(), "OPPORTUNITY_CREATED", toResponseWithExpectedRevenue(saved));

        return toResponseWithExpectedRevenue(saved);
    }

    // ─── Update ──────────────────────────────────────────────────────
    @Transactional
    @CacheEvict(value = "opportunities", allEntries = true)
    public OpportunityResponse updateOpportunity(UUID opportunityId, UpdateOpportunityRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        Opportunity opportunity = findOpp(opportunityId, tenantId);

        if (request.getName() != null) opportunity.setName(request.getName());
        if (request.getAccountId() != null) opportunity.setAccountId(request.getAccountId());
        if (request.getContactId() != null) opportunity.setContactId(request.getContactId());
        if (request.getAmount() != null) opportunity.setAmount(request.getAmount());
        if (request.getStage() != null) opportunity.setStage(request.getStage());
        if (request.getProbability() != null) opportunity.setProbability(request.getProbability());
        if (request.getCloseDate() != null) opportunity.setCloseDate(request.getCloseDate());
        if (request.getDescription() != null) opportunity.setDescription(request.getDescription());
        if (request.getAssignedTo() != null) opportunity.setAssignedTo(request.getAssignedTo());
        if (request.getForecastCategory() != null) opportunity.setForecastCategory(request.getForecastCategory());
        if (request.getLostReason() != null) opportunity.setLostReason(request.getLostReason());
        if (request.getCurrency() != null) opportunity.setCurrency(request.getCurrency());
        if (request.getNextStep() != null) opportunity.setNextStep(request.getNextStep());
        if (request.getLeadSource() != null) opportunity.setLeadSource(request.getLeadSource());
        if (request.getCampaignId() != null) opportunity.setCampaignId(request.getCampaignId());
        if (request.getOwnerId() != null) opportunity.setOwnerId(request.getOwnerId());

        Opportunity updated = opportunityRepository.save(opportunity);
        recordActivity(updated.getId(), tenantId, "UPDATED", "Opportunity updated", userId);

        eventPublisher.publish("opportunity-events", tenantId, userId, "Opportunity",
                updated.getId().toString(), "OPPORTUNITY_UPDATED", toResponseWithExpectedRevenue(updated));

        return toResponseWithExpectedRevenue(updated);
    }

    // ─── Read ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    @Cacheable(value = "opportunities", key = "#opportunityId + '_' + T(com.crm.common.security.TenantContext).getTenantId()")
    public OpportunityResponse getOpportunityById(UUID opportunityId) {
        String tenantId = TenantContext.getTenantId();
        return toResponseWithExpectedRevenue(findOpp(opportunityId, tenantId));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OpportunityResponse> getAllOpportunities(int page, int size, String sortBy, String sortDir) {
        String tenantId = TenantContext.getTenantId();
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Opportunity> p = opportunityRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        return buildPaged(p);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OpportunityResponse> getOpportunitiesByStage(Opportunity.OpportunityStage stage, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<Opportunity> p = opportunityRepository.findByTenantIdAndStageAndDeletedFalse(tenantId, stage, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return buildPaged(p);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OpportunityResponse> getOpportunitiesByAccount(UUID accountId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<Opportunity> p = opportunityRepository.findByTenantIdAndAccountIdAndDeletedFalse(tenantId, accountId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return buildPaged(p);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OpportunityResponse> getOpportunitiesByAssignee(UUID assignedTo, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<Opportunity> p = opportunityRepository.findByTenantIdAndAssignedToAndDeletedFalse(tenantId, assignedTo, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return buildPaged(p);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OpportunityResponse> searchOpportunities(String query, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<Opportunity> p = opportunityRepository.searchOpportunities(tenantId, query, PageRequest.of(page, size));
        return buildPaged(p);
    }

    @Transactional
    @CacheEvict(value = "opportunities", allEntries = true)
    public void deleteOpportunity(UUID opportunityId, String userId) {
        String tenantId = TenantContext.getTenantId();
        Opportunity opp = findOpp(opportunityId, tenantId);
        opp.setDeleted(true);
        opportunityRepository.save(opp);
        recordActivity(opp.getId(), tenantId, "DELETED", "Opportunity soft-deleted", userId);
        eventPublisher.publish("opportunity-events", tenantId, userId, "Opportunity",
                opp.getId().toString(), "OPPORTUNITY_DELETED", null);
    }

    // ─── 2. Sales Pipeline Stages + 11. Stage Automation ─────────────
    @Transactional
    @CacheEvict(value = "opportunities", allEntries = true)
    public OpportunityResponse updateStage(UUID opportunityId, Opportunity.OpportunityStage newStage, String lostReason, String competitor, String userId) {
        String tenantId = TenantContext.getTenantId();
        Opportunity opp = findOpp(opportunityId, tenantId);
        Opportunity.OpportunityStage previousStage = opp.getStage();

        if (previousStage == Opportunity.OpportunityStage.CLOSED_WON || previousStage == Opportunity.OpportunityStage.CLOSED_LOST) {
            throw new BadRequestException("Cannot change stage of a closed opportunity");
        }

        // Require lost reason when marking as CLOSED_LOST
        if (newStage == Opportunity.OpportunityStage.CLOSED_LOST && (lostReason == null || lostReason.isBlank())) {
            throw new BadRequestException("Lost reason is required when closing an opportunity as lost");
        }
        // Require competitor when marking as CLOSED_LOST
        if (newStage == Opportunity.OpportunityStage.CLOSED_LOST && (competitor == null || competitor.isBlank())) {
            throw new BadRequestException("Competitor is required when closing an opportunity as lost");
        }

        opp.setStage(newStage);
        // Stage automation: auto-set probability based on stage
        opp.setProbability(getStageProbabilityWeight(newStage));

        if (newStage == Opportunity.OpportunityStage.CLOSED_WON) {
            opp.setWonDate(LocalDateTime.now());
            opp.setProbability(100);
            opp.setForecastCategory(Opportunity.ForecastCategory.CLOSED);
        } else if (newStage == Opportunity.OpportunityStage.CLOSED_LOST) {
            opp.setLostDate(LocalDateTime.now());
            opp.setLostReason(lostReason);
            opp.setProbability(0);
            opp.setForecastCategory(Opportunity.ForecastCategory.CLOSED);
        } else if (newStage == Opportunity.OpportunityStage.NEGOTIATION) {
            opp.setForecastCategory(Opportunity.ForecastCategory.COMMIT);
        } else if (newStage == Opportunity.OpportunityStage.PROPOSAL) {
            opp.setForecastCategory(Opportunity.ForecastCategory.BEST_CASE);
        }

        Opportunity updated = opportunityRepository.save(opp);
        recordActivity(updated.getId(), tenantId, "STAGE_CHANGED",
                "Stage changed from " + previousStage + " to " + newStage, userId);

        // Auto-record competitor when closing as lost
        if (newStage == Opportunity.OpportunityStage.CLOSED_LOST && competitor != null && !competitor.isBlank()) {
            try {
                OpportunityCompetitor comp = new OpportunityCompetitor();
                comp.setOpportunityId(updated.getId());
                comp.setCompetitorName(competitor);
                comp.setStrengths(lostReason);
                comp.setTenantId(tenantId);
                competitorRepository.save(comp);
            } catch (Exception e) {
                log.warn("Failed to record competitor: {}", e.getMessage());
            }
        }

        // Record stage history for conversion analytics
        long timeInStage = 0;
        if (opp.getUpdatedAt() != null) {
            timeInStage = ChronoUnit.SECONDS.between(opp.getUpdatedAt(), LocalDateTime.now());
        }
        try {
            StageHistory sh = StageHistory.builder()
                    .opportunityId(updated.getId())
                    .fromStage(previousStage.name())
                    .toStage(newStage.name())
                    .changedBy(userId)
                    .changedAt(LocalDateTime.now())
                    .timeInStage(timeInStage)
                    .build();
            sh.setTenantId(tenantId);
            stageHistoryRepository.save(sh);
        } catch (Exception e) {
            log.warn("Failed to record stage history: {}", e.getMessage());
        }

        eventPublisher.publish("opportunity-events", tenantId, userId, "Opportunity",
                updated.getId().toString(), "OPPORTUNITY_STAGE_CHANGED",
                Map.of("previousStage", previousStage.name(), "newStage", newStage.name()));

        return toResponseWithExpectedRevenue(updated);
    }

    // ─── 3. Value Tracking (amount on entity) + Products ─────────────

    // ─── 4. Close Date Prediction ────────────────────────────────────
    @Transactional
    @CacheEvict(value = "opportunities", allEntries = true)
    public OpportunityResponse predictCloseDate(UUID opportunityId) {
        String tenantId = TenantContext.getTenantId();
        Opportunity opp = findOpp(opportunityId, tenantId);

        // Simple heuristic: average days from creation to close of won deals
        List<Opportunity> wonOpps = opportunityRepository.findByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_WON);
        if (!wonOpps.isEmpty()) {
            double avgDays = wonOpps.stream()
                    .filter(w -> w.getWonDate() != null && w.getCreatedAt() != null)
                    .mapToLong(w -> ChronoUnit.DAYS.between(w.getCreatedAt().toLocalDate(), w.getWonDate().toLocalDate()))
                    .average()
                    .orElse(30.0);

            LocalDate predicted = opp.getCreatedAt().toLocalDate().plusDays((long) avgDays);
            if (predicted.isBefore(LocalDate.now())) {
                predicted = LocalDate.now().plusDays(7);
            }
            opp.setPredictedCloseDate(predicted);

            // Confidence based on sample size
            int confidence = Math.min(95, 40 + wonOpps.size() * 5);
            opp.setConfidenceScore(confidence);
        } else {
            // No historical data — default prediction
            opp.setPredictedCloseDate(LocalDate.now().plusDays(30));
            opp.setConfidenceScore(20);
        }

        Opportunity saved = opportunityRepository.save(opp);
        recordActivity(saved.getId(), tenantId, "PREDICTION",
                "Close date predicted: " + saved.getPredictedCloseDate() + " (confidence: " + saved.getConfidenceScore() + "%)", null);
        return toResponseWithExpectedRevenue(saved);
    }

    // ─── 5. Probability Scoring (auto on stage change, see updateStage) ──

    // ─── 6. Product Management ───────────────────────────────────────
    @Transactional
    public ProductResponse addProduct(UUID opportunityId, CreateProductRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        findOpp(opportunityId, tenantId); // verify existence

        OpportunityProduct product = OpportunityProduct.builder()
                .opportunityId(opportunityId)
                .productName(request.getProductName())
                .productCode(request.getProductCode())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .unitPrice(request.getUnitPrice() != null ? request.getUnitPrice() : BigDecimal.ZERO)
                .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
                .description(request.getDescription())
                .tenantId(tenantId)
                .build();
        product.calculateTotal();

        OpportunityProduct saved = productRepository.save(product);
        recordActivity(opportunityId, tenantId, "PRODUCT_ADDED", "Product added: " + saved.getProductName(), userId);
        return opportunityMapper.toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProducts(UUID opportunityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<OpportunityProduct> p = productRepository.findByOpportunityIdAndTenantId(opportunityId, tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.<ProductResponse>builder()
                .content(p.getContent().stream().map(opportunityMapper::toProductResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .last(p.isLast()).first(p.isFirst()).build();
    }

    @Transactional
    public void deleteProduct(UUID productId, String userId) {
        String tenantId = TenantContext.getTenantId();
        OpportunityProduct product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        recordActivity(product.getOpportunityId(), tenantId, "PRODUCT_REMOVED", "Product removed: " + product.getProductName(), userId);
        productRepository.delete(product);
    }

    // ─── 7. Competitor Tracking ──────────────────────────────────────
    @Transactional
    public CompetitorResponse addCompetitor(UUID opportunityId, CreateCompetitorRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        findOpp(opportunityId, tenantId);

        OpportunityCompetitor competitor = OpportunityCompetitor.builder()
                .opportunityId(opportunityId)
                .competitorName(request.getCompetitorName())
                .strengths(request.getStrengths())
                .weaknesses(request.getWeaknesses())
                .strategy(request.getStrategy())
                .threatLevel(request.getThreatLevel() != null ? request.getThreatLevel() : "MEDIUM")
                .tenantId(tenantId)
                .build();

        OpportunityCompetitor saved = competitorRepository.save(competitor);
        recordActivity(opportunityId, tenantId, "COMPETITOR_ADDED", "Competitor added: " + saved.getCompetitorName(), userId);
        return opportunityMapper.toCompetitorResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CompetitorResponse> getCompetitors(UUID opportunityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<OpportunityCompetitor> p = competitorRepository.findByOpportunityIdAndTenantId(opportunityId, tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.<CompetitorResponse>builder()
                .content(p.getContent().stream().map(opportunityMapper::toCompetitorResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .last(p.isLast()).first(p.isFirst()).build();
    }

    @Transactional
    public void deleteCompetitor(UUID competitorId, String userId) {
        String tenantId = TenantContext.getTenantId();
        OpportunityCompetitor comp = competitorRepository.findByIdAndTenantId(competitorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Competitor", "id", competitorId));
        recordActivity(comp.getOpportunityId(), tenantId, "COMPETITOR_REMOVED", "Competitor removed: " + comp.getCompetitorName(), userId);
        competitorRepository.delete(comp);
    }

    // ─── 8. Activity Timeline ────────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<ActivityResponse> getActivities(UUID opportunityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<OpportunityActivity> p = activityRepository.findByOpportunityIdAndTenantIdOrderByCreatedAtDesc(opportunityId, tenantId, PageRequest.of(page, size));
        return PagedResponse.<ActivityResponse>builder()
                .content(p.getContent().stream().map(opportunityMapper::toActivityResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .last(p.isLast()).first(p.isFirst()).build();
    }

    // ─── 9. Collaboration ────────────────────────────────────────────
    @Transactional
    public CollaboratorResponse addCollaborator(UUID opportunityId, UUID userId, String role, String currentUserId) {
        String tenantId = TenantContext.getTenantId();
        findOpp(opportunityId, tenantId);

        Optional<OpportunityCollaborator> existing = collaboratorRepository.findByOpportunityIdAndUserIdAndTenantId(opportunityId, userId, tenantId);
        if (existing.isPresent()) {
            throw new BadRequestException("User is already a collaborator on this opportunity");
        }

        OpportunityCollaborator collaborator = OpportunityCollaborator.builder()
                .opportunityId(opportunityId)
                .userId(userId)
                .role(role != null ? role : "MEMBER")
                .tenantId(tenantId)
                .build();

        OpportunityCollaborator saved = collaboratorRepository.save(collaborator);
        recordActivity(opportunityId, tenantId, "COLLABORATOR_ADDED", "Collaborator added: " + userId, currentUserId);
        return opportunityMapper.toCollaboratorResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CollaboratorResponse> getCollaborators(UUID opportunityId) {
        String tenantId = TenantContext.getTenantId();
        return collaboratorRepository.findByOpportunityIdAndTenantId(opportunityId, tenantId)
                .stream().map(opportunityMapper::toCollaboratorResponse).toList();
    }

    @Transactional
    public void removeCollaborator(UUID opportunityId, UUID userId, String currentUserId) {
        String tenantId = TenantContext.getTenantId();
        OpportunityCollaborator collab = collaboratorRepository.findByOpportunityIdAndUserIdAndTenantId(opportunityId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator", "userId", userId));
        collaboratorRepository.delete(collab);
        recordActivity(opportunityId, tenantId, "COLLABORATOR_REMOVED", "Collaborator removed: " + userId, currentUserId);
    }

    // Notes (part of collaboration)
    @Transactional
    public NoteResponse addNote(UUID opportunityId, CreateNoteRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        findOpp(opportunityId, tenantId);

        OpportunityNote note = OpportunityNote.builder()
                .opportunityId(opportunityId)
                .content(request.getContent())
                .isPinned(request.getIsPinned() != null ? request.getIsPinned() : false)
                .tenantId(tenantId)
                .createdBy(userId)
                .build();

        OpportunityNote saved = noteRepository.save(note);
        recordActivity(opportunityId, tenantId, "NOTE_ADDED", "Note added", userId);
        return opportunityMapper.toNoteResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NoteResponse> getNotes(UUID opportunityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<OpportunityNote> p = noteRepository.findByOpportunityIdAndTenantIdOrderByIsPinnedDescCreatedAtDesc(opportunityId, tenantId, PageRequest.of(page, size));
        return PagedResponse.<NoteResponse>builder()
                .content(p.getContent().stream().map(opportunityMapper::toNoteResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .last(p.isLast()).first(p.isFirst()).build();
    }

    @Transactional
    public void deleteNote(UUID noteId, String userId) {
        String tenantId = TenantContext.getTenantId();
        OpportunityNote note = noteRepository.findByIdAndTenantId(noteId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", "id", noteId));
        recordActivity(note.getOpportunityId(), tenantId, "NOTE_DELETED", "Note deleted", userId);
        noteRepository.delete(note);
    }

    // ─── 10. Forecasting ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ForecastSummary getRevenueForecast() {
        String tenantId = TenantContext.getTenantId();

        BigDecimal pipeline = opportunityRepository.sumAmountByTenantIdAndForecastCategory(tenantId, Opportunity.ForecastCategory.PIPELINE);
        BigDecimal bestCase = opportunityRepository.sumAmountByTenantIdAndForecastCategory(tenantId, Opportunity.ForecastCategory.BEST_CASE);
        BigDecimal commit = opportunityRepository.sumAmountByTenantIdAndForecastCategory(tenantId, Opportunity.ForecastCategory.COMMIT);
        BigDecimal closed = opportunityRepository.sumAmountByTenantIdAndForecastCategory(tenantId, Opportunity.ForecastCategory.CLOSED);

        long closedWon = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_WON);
        long closedLost = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_LOST);

        List<Opportunity.OpportunityStage> openStages = List.of(
                Opportunity.OpportunityStage.PROSPECTING,
                Opportunity.OpportunityStage.QUALIFICATION,
                Opportunity.OpportunityStage.NEEDS_ANALYSIS,
                Opportunity.OpportunityStage.PROPOSAL,
                Opportunity.OpportunityStage.NEGOTIATION);
        long totalOpen = opportunityRepository.countByTenantIdAndStageInAndDeletedFalse(tenantId, openStages);

        List<Object[]> stageData = opportunityRepository.getRevenueByStage(tenantId);
        List<ForecastSummary.StageSummary> stageBreakdown = new ArrayList<>();
        BigDecimal totalWeighted = BigDecimal.ZERO;

        for (Object[] row : stageData) {
            String stageName = ((Opportunity.OpportunityStage) row[0]).name();
            long count = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            stageBreakdown.add(ForecastSummary.StageSummary.builder()
                    .stage(stageName).count(count).totalAmount(amount).build());
            int weight = getStageProbabilityWeight((Opportunity.OpportunityStage) row[0]);
            totalWeighted = totalWeighted.add(amount.multiply(BigDecimal.valueOf(weight)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        return ForecastSummary.builder()
                .totalPipeline(pipeline).bestCase(bestCase).commit(commit).closed(closed)
                .totalWeightedRevenue(totalWeighted).totalOpenOpportunities(totalOpen)
                .totalClosedWon(closedWon).totalClosedLost(closedLost)
                .stageBreakdown(stageBreakdown).build();
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> getOpportunitiesByDateRange(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getTenantId();
        return opportunityRepository.findByTenantIdAndCloseDateBetweenAndDeletedFalse(tenantId, startDate, endDate)
                .stream().map(opportunityMapper::toResponse).toList();
    }

    // ─── 12. Reminders ───────────────────────────────────────────────
    @Transactional
    public ReminderResponse addReminder(UUID opportunityId, CreateReminderRequest request, String userId) {
        String tenantId = TenantContext.getTenantId();
        findOpp(opportunityId, tenantId);

        OpportunityReminder reminder = OpportunityReminder.builder()
                .opportunityId(opportunityId)
                .reminderType(request.getReminderType())
                .message(request.getMessage())
                .remindAt(request.getRemindAt())
                .tenantId(tenantId)
                .createdBy(userId)
                .build();

        OpportunityReminder saved = reminderRepository.save(reminder);
        recordActivity(opportunityId, tenantId, "REMINDER_SET", "Reminder set for " + saved.getRemindAt(), userId);
        return opportunityMapper.toReminderResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReminderResponse> getReminders(UUID opportunityId, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Page<OpportunityReminder> p = reminderRepository.findByOpportunityIdAndTenantIdOrderByRemindAtAsc(opportunityId, tenantId, PageRequest.of(page, size));
        return PagedResponse.<ReminderResponse>builder()
                .content(p.getContent().stream().map(opportunityMapper::toReminderResponse).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .last(p.isLast()).first(p.isFirst()).build();
    }

    @Transactional
    public ReminderResponse completeReminder(UUID reminderId, String userId) {
        String tenantId = TenantContext.getTenantId();
        OpportunityReminder reminder = reminderRepository.findByIdAndTenantId(reminderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", reminderId));
        reminder.setIsCompleted(true);
        reminder.setCompletedAt(LocalDateTime.now());
        OpportunityReminder saved = reminderRepository.save(reminder);
        recordActivity(saved.getOpportunityId(), tenantId, "REMINDER_COMPLETED", "Reminder completed: " + saved.getMessage(), userId);
        return opportunityMapper.toReminderResponse(saved);
    }

    @Transactional
    public void deleteReminder(UUID reminderId, String userId) {
        String tenantId = TenantContext.getTenantId();
        OpportunityReminder reminder = reminderRepository.findByIdAndTenantId(reminderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", reminderId));
        recordActivity(reminder.getOpportunityId(), tenantId, "REMINDER_DELETED", "Reminder deleted", userId);
        reminderRepository.delete(reminder);
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> getDueReminders() {
        String tenantId = TenantContext.getTenantId();
        return reminderRepository.findByTenantIdAndIsCompletedFalseAndRemindAtBefore(tenantId, LocalDateTime.now())
                .stream().map(opportunityMapper::toReminderResponse).toList();
    }

    // ─── 13. Revenue Analytics ───────────────────────────────────────
    @Transactional(readOnly = true)
    public RevenueAnalytics getRevenueAnalytics() {
        String tenantId = TenantContext.getTenantId();

        long total = opportunityRepository.countByTenantIdAndDeletedFalse(tenantId);
        long closedWon = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_WON);
        long closedLost = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_LOST);
        long open = total - closedWon - closedLost;

        BigDecimal totalRevenue = opportunityRepository.sumAllAmountByTenantId(tenantId);
        BigDecimal wonRevenue = opportunityRepository.sumWonAmount(tenantId);
        BigDecimal pipelineAmount = opportunityRepository.sumAmountByTenantIdAndForecastCategory(tenantId, Opportunity.ForecastCategory.PIPELINE);

        BigDecimal avgDealSize = total > 0 ? totalRevenue.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal winRate = (closedWon + closedLost) > 0
                ? BigDecimal.valueOf(closedWon).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(closedWon + closedLost), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Revenue by stage
        List<Object[]> stageData = opportunityRepository.getRevenueByStage(tenantId);
        Map<String, BigDecimal> revenueByStage = new LinkedHashMap<>();
        Map<String, Long> countByStage = new LinkedHashMap<>();
        List<ForecastSummary.StageSummary> stageBreakdown = new ArrayList<>();
        BigDecimal totalWeighted = BigDecimal.ZERO;

        for (Object[] row : stageData) {
            String stageName = ((Opportunity.OpportunityStage) row[0]).name();
            long count = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            revenueByStage.put(stageName, amount);
            countByStage.put(stageName, count);
            stageBreakdown.add(ForecastSummary.StageSummary.builder().stage(stageName).count(count).totalAmount(amount).build());
            int weight = getStageProbabilityWeight((Opportunity.OpportunityStage) row[0]);
            totalWeighted = totalWeighted.add(amount.multiply(BigDecimal.valueOf(weight)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        // Revenue by lead source
        Map<String, BigDecimal> revenueByLeadSource = new LinkedHashMap<>();
        for (Object[] row : opportunityRepository.getRevenueByLeadSource(tenantId)) {
            revenueByLeadSource.put((String) row[0], (BigDecimal) row[1]);
        }

        return RevenueAnalytics.builder()
                .totalOpportunities(total).openOpportunities(open)
                .closedWon(closedWon).closedLost(closedLost)
                .totalRevenue(totalRevenue).totalPipeline(pipelineAmount)
                .avgDealSize(avgDealSize).winRate(winRate)
                .totalWeightedPipeline(totalWeighted)
                .revenueByStage(revenueByStage).countByStage(countByStage)
                .revenueByLeadSource(revenueByLeadSource)
                .stageBreakdown(stageBreakdown).build();
    }

    // ─── 14. Win/Loss Analysis ───────────────────────────────────────
    @Transactional(readOnly = true)
    public WinLossAnalysis getWinLossAnalysis() {
        String tenantId = TenantContext.getTenantId();

        long closedWon = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_WON);
        long closedLost = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_LOST);
        BigDecimal wonRevenue = opportunityRepository.sumWonAmount(tenantId);
        BigDecimal lostRevenue = opportunityRepository.sumLostAmount(tenantId);

        BigDecimal winRate = (closedWon + closedLost) > 0
                ? BigDecimal.valueOf(closedWon).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(closedWon + closedLost), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgWonSize = closedWon > 0 ? wonRevenue.divide(BigDecimal.valueOf(closedWon), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal avgLostSize = closedLost > 0 ? lostRevenue.divide(BigDecimal.valueOf(closedLost), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Average days to close for won deals
        List<Opportunity> wonOpps = opportunityRepository.findByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_WON);
        Double avgDaysToClose = wonOpps.stream()
                .filter(o -> o.getWonDate() != null && o.getCreatedAt() != null)
                .mapToLong(o -> ChronoUnit.DAYS.between(o.getCreatedAt().toLocalDate(), o.getWonDate().toLocalDate()))
                .average()
                .orElse(0.0);

        // Lost reason breakdown
        Map<String, Long> lostReasonBreakdown = new LinkedHashMap<>();
        for (Object[] row : opportunityRepository.getLostReasonBreakdown(tenantId)) {
            lostReasonBreakdown.put((String) row[0], (Long) row[1]);
        }

        // Won by stage entry (which stage was the deal in before winning)
        Map<String, Long> wonByStageEntry = new LinkedHashMap<>();
        List<Object[]> stageTransitions = stageHistoryRepository.getStageTransitionCounts(tenantId);
        for (Object[] row : stageTransitions) {
            String from = (String) row[0];
            String to = (String) row[1];
            long count = (Long) row[2];
            if ("CLOSED_WON".equals(to)) {
                wonByStageEntry.put(from, wonByStageEntry.getOrDefault(from, 0L) + count);
            }
        }

        return WinLossAnalysis.builder()
                .totalClosedWon(closedWon).totalClosedLost(closedLost)
                .winRate(winRate).avgWonDealSize(avgWonSize).avgLostDealSize(avgLostSize)
                .totalWonRevenue(wonRevenue).totalLostRevenue(lostRevenue)
                .avgDaysToClose(avgDaysToClose)
                .lostReasonBreakdown(lostReasonBreakdown)
                .wonByStageEntry(wonByStageEntry).build();
    }

    // ─── 15. Alerts (due reminders + stale deals) ────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getAlerts() {
        String tenantId = TenantContext.getTenantId();
        Map<String, Object> alerts = new LinkedHashMap<>();

        // Overdue reminders
        List<ReminderResponse> dueReminders = getDueReminders();
        alerts.put("overdueReminders", dueReminders);
        alerts.put("overdueReminderCount", dueReminders.size());

        // Deals closing soon (within 7 days)
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(7);
        List<OpportunityResponse> closingSoon = opportunityRepository.findByTenantIdAndCloseDateBetweenAndDeletedFalse(tenantId, start, end)
                .stream()
                .filter(o -> o.getStage() != Opportunity.OpportunityStage.CLOSED_WON && o.getStage() != Opportunity.OpportunityStage.CLOSED_LOST)
                .map(opportunityMapper::toResponse).toList();
        alerts.put("closingSoon", closingSoon);
        alerts.put("closingSoonCount", closingSoon.size());

        // Overdue deals (close date passed but still open)
        List<OpportunityResponse> overdue = opportunityRepository.findByTenantIdAndCloseDateBetweenAndDeletedFalse(tenantId, LocalDate.of(2020, 1, 1), LocalDate.now().minusDays(1))
                .stream()
                .filter(o -> o.getStage() != Opportunity.OpportunityStage.CLOSED_WON && o.getStage() != Opportunity.OpportunityStage.CLOSED_LOST)
                .map(opportunityMapper::toResponse).toList();
        alerts.put("overdueDeals", overdue);
        alerts.put("overdueDealCount", overdue.size());

        // Stale deals (no update in 30+ days, still open)
        LocalDateTime staleThreshold = LocalDateTime.now().minusDays(30);
        List<Opportunity.OpportunityStage> openStages = List.of(
                Opportunity.OpportunityStage.PROSPECTING, Opportunity.OpportunityStage.QUALIFICATION,
                Opportunity.OpportunityStage.NEEDS_ANALYSIS, Opportunity.OpportunityStage.PROPOSAL,
                Opportunity.OpportunityStage.NEGOTIATION);
        // Use getAllOpportunities and filter stale
        Page<Opportunity> allOpen = opportunityRepository.findByTenantIdAndDeletedFalse(tenantId, PageRequest.of(0, 500));
        List<OpportunityResponse> stale = allOpen.getContent().stream()
                .filter(o -> openStages.contains(o.getStage()))
                .filter(o -> o.getUpdatedAt() != null && o.getUpdatedAt().isBefore(staleThreshold))
                .map(opportunityMapper::toResponse).toList();
        alerts.put("staleDeals", stale);
        alerts.put("staleDealCount", stale.size());

        int totalAlerts = dueReminders.size() + closingSoon.size() + overdue.size() + stale.size();
        alerts.put("totalAlerts", totalAlerts);

        return alerts;
    }

    // ─── 16. Stage Conversion Analytics ─────────────────────────────
    @Transactional(readOnly = true)
    public StageConversionAnalytics getConversionAnalytics() {
        String tenantId = TenantContext.getTenantId();

        // Transition counts
        List<Object[]> transitionData = stageHistoryRepository.getStageTransitionCounts(tenantId);
        List<StageConversionAnalytics.StageTransition> transitions = new ArrayList<>();
        Map<String, Long> fromTotals = new LinkedHashMap<>();
        Map<String, Map<String, Long>> fromToMap = new LinkedHashMap<>();

        for (Object[] row : transitionData) {
            String from = (String) row[0];
            String to = (String) row[1];
            long count = (Long) row[2];
            transitions.add(StageConversionAnalytics.StageTransition.builder()
                    .fromStage(from).toStage(to).count(count).build());
            fromTotals.merge(from, count, Long::sum);
            fromToMap.computeIfAbsent(from, k -> new LinkedHashMap<>()).merge(to, count, Long::sum);
        }

        // Compute per-stage conversion rates (forward movement)
        Map<String, StageConversionAnalytics.StageConversionRate> conversionRates = new LinkedHashMap<>();
        String[] stageOrder = {"PROSPECTING", "QUALIFICATION", "NEEDS_ANALYSIS", "PROPOSAL", "NEGOTIATION", "CLOSED_WON"};
        for (int i = 0; i < stageOrder.length - 1; i++) {
            String from = stageOrder[i];
            String to = stageOrder[i + 1];
            long total = fromTotals.getOrDefault(from, 0L);
            long transitioned = fromToMap.getOrDefault(from, Map.of()).getOrDefault(to, 0L);
            double pct = total > 0 ? (transitioned * 100.0) / total : 0.0;
            conversionRates.put(from, StageConversionAnalytics.StageConversionRate.builder()
                    .fromStage(from).toStage(to).transitioned(transitioned).total(total).conversionPct(pct).build());
        }

        // Average time in stage
        Map<String, Double> avgTime = new LinkedHashMap<>();
        for (Object[] row : stageHistoryRepository.getAvgTimeInStage(tenantId)) {
            avgTime.put((String) row[0], ((Number) row[1]).doubleValue());
        }

        // Overall conversion: PROSPECTING -> CLOSED_WON
        long prospectingTotal = fromTotals.getOrDefault("PROSPECTING", 0L);
        long closedWonFromAny = stageHistoryRepository.countByTenantIdAndToStage(tenantId, "CLOSED_WON");
        double overallRate = prospectingTotal > 0 ? (closedWonFromAny * 100.0) / prospectingTotal : 0.0;

        return StageConversionAnalytics.builder()
                .conversionRates(conversionRates)
                .avgTimeInStage(avgTime)
                .transitions(transitions)
                .overallConversionRate(overallRate)
                .build();
    }

    // ─── 17. Pipeline Dashboard (unified) ────────────────────────────
    @Transactional(readOnly = true)
    public PipelineDashboard getPipelineDashboard() {
        String tenantId = TenantContext.getTenantId();

        // Core metrics
        long total = opportunityRepository.countByTenantIdAndDeletedFalse(tenantId);
        long closedWon = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_WON);
        long closedLost = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_LOST);
        long open = total - closedWon - closedLost;
        BigDecimal totalRevenue = opportunityRepository.sumWonAmount(tenantId);
        BigDecimal totalPipeline = opportunityRepository.sumAllAmountByTenantId(tenantId);
        BigDecimal avgDealSize = total > 0 ? totalPipeline.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal winRate = (closedWon + closedLost) > 0
                ? BigDecimal.valueOf(closedWon * 100).divide(BigDecimal.valueOf(closedWon + closedLost), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Stage breakdown
        List<Object[]> stageData = opportunityRepository.getRevenueByStage(tenantId);
        List<ForecastSummary.StageSummary> stageBreakdown = new ArrayList<>();
        BigDecimal weighted = BigDecimal.ZERO;
        for (Object[] row : stageData) {
            Opportunity.OpportunityStage stage = (Opportunity.OpportunityStage) row[0];
            long count = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            stageBreakdown.add(ForecastSummary.StageSummary.builder().stage(stage.name()).count(count).totalAmount(amount).build());
            weighted = weighted.add(amount.multiply(BigDecimal.valueOf(getStageProbabilityWeight(stage))).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        // Forecast categories
        BigDecimal fPipeline = opportunityRepository.sumAmountByTenantIdAndForecastCategory(tenantId, Opportunity.ForecastCategory.PIPELINE);
        BigDecimal fBestCase = opportunityRepository.sumAmountByTenantIdAndForecastCategory(tenantId, Opportunity.ForecastCategory.BEST_CASE);
        BigDecimal fCommit = opportunityRepository.sumAmountByTenantIdAndForecastCategory(tenantId, Opportunity.ForecastCategory.COMMIT);
        BigDecimal fClosed = opportunityRepository.sumAmountByTenantIdAndForecastCategory(tenantId, Opportunity.ForecastCategory.CLOSED);

        // Revenue by lead source
        Map<String, BigDecimal> revenueByLeadSource = new LinkedHashMap<>();
        for (Object[] row : opportunityRepository.getRevenueByLeadSource(tenantId)) {
            revenueByLeadSource.put((String) row[0], (BigDecimal) row[1]);
        }

        // Alerts counts
        LocalDate now = LocalDate.now();
        long closingSoon = opportunityRepository.findByTenantIdAndCloseDateBetweenAndDeletedFalse(tenantId, now, now.plusDays(7))
                .stream().filter(o -> o.getStage() != Opportunity.OpportunityStage.CLOSED_WON && o.getStage() != Opportunity.OpportunityStage.CLOSED_LOST).count();
        long overdueDeals = opportunityRepository.findByTenantIdAndCloseDateBetweenAndDeletedFalse(tenantId, LocalDate.of(2020, 1, 1), now.minusDays(1))
                .stream().filter(o -> o.getStage() != Opportunity.OpportunityStage.CLOSED_WON && o.getStage() != Opportunity.OpportunityStage.CLOSED_LOST).count();
        LocalDateTime staleThreshold = LocalDateTime.now().minusDays(30);
        List<Opportunity.OpportunityStage> openStages = List.of(
                Opportunity.OpportunityStage.PROSPECTING, Opportunity.OpportunityStage.QUALIFICATION,
                Opportunity.OpportunityStage.NEEDS_ANALYSIS, Opportunity.OpportunityStage.PROPOSAL,
                Opportunity.OpportunityStage.NEGOTIATION);
        Page<Opportunity> allOpen = opportunityRepository.findByTenantIdAndDeletedFalse(tenantId, PageRequest.of(0, 500));
        long staleDeals = allOpen.getContent().stream()
                .filter(o -> openStages.contains(o.getStage()))
                .filter(o -> o.getUpdatedAt() != null && o.getUpdatedAt().isBefore(staleThreshold)).count();

        long activeReminders = reminderRepository.findByTenantIdAndIsCompletedFalseAndRemindAtBefore(tenantId, LocalDateTime.now().plusDays(365)).size();

        return PipelineDashboard.builder()
                .totalPipelineValue(totalPipeline).totalOpenDeals(open).totalClosedWon(closedWon).totalClosedLost(closedLost)
                .totalRevenue(totalRevenue).avgDealSize(avgDealSize).winRate(winRate).weightedPipeline(weighted)
                .stageBreakdown(stageBreakdown).revenueByLeadSource(revenueByLeadSource)
                .forecastPipeline(fPipeline).forecastBestCase(fBestCase).forecastCommit(fCommit).forecastClosed(fClosed)
                .overdueDeals(overdueDeals).closingSoonDeals(closingSoon).staleDeals(staleDeals)
                .activeReminders(activeReminders)
                .build();
    }

    // ─── 18. Pipeline Performance + Velocity ─────────────────────────
    @Transactional(readOnly = true)
    public PipelinePerformance getPipelinePerformance() {
        String tenantId = TenantContext.getTenantId();

        long total = opportunityRepository.countByTenantIdAndDeletedFalse(tenantId);
        long closedWon = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_WON);
        long closedLost = opportunityRepository.countByTenantIdAndStageAndDeletedFalse(tenantId, Opportunity.OpportunityStage.CLOSED_LOST);
        BigDecimal wonRevenue = opportunityRepository.sumWonAmount(tenantId);
        BigDecimal totalPipeline = opportunityRepository.sumAllAmountByTenantId(tenantId);

        BigDecimal avgDealSize = total > 0 ? totalPipeline.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal winRate = (closedWon + closedLost) > 0
                ? BigDecimal.valueOf(closedWon * 100).divide(BigDecimal.valueOf(closedWon + closedLost), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Average cycle time (days from creation to close for won deals)
        List<Opportunity> wonOpps = opportunityRepository.findWonOpportunitiesWithDate(tenantId);
        double avgCycleDays = wonOpps.stream()
                .mapToLong(o -> ChronoUnit.DAYS.between(o.getCreatedAt().toLocalDate(), o.getWonDate().toLocalDate()))
                .average().orElse(30.0);

        // Pipeline velocity = (total_deals × avg_value × win_rate%) / avg_cycle_days
        BigDecimal velocity = BigDecimal.ZERO;
        if (avgCycleDays > 0 && total > 0) {
            velocity = BigDecimal.valueOf(total)
                    .multiply(avgDealSize)
                    .multiply(winRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(avgCycleDays), 2, RoundingMode.HALF_UP);
        }

        // Per-rep performance
        List<Object[]> repData = opportunityRepository.getPerAssigneeStageBreakdown(tenantId);
        Map<String, PipelinePerformance.RepPerformance.RepPerformanceBuilder> repMap = new LinkedHashMap<>();
        for (Object[] row : repData) {
            String assignee = row[0].toString();
            Opportunity.OpportunityStage stage = (Opportunity.OpportunityStage) row[1];
            long count = (Long) row[2];
            BigDecimal amount = (BigDecimal) row[3];

            PipelinePerformance.RepPerformance.RepPerformanceBuilder builder = repMap.computeIfAbsent(assignee,
                    k -> PipelinePerformance.RepPerformance.builder().userId(k).totalDeals(0).wonDeals(0).lostDeals(0).openDeals(0)
                            .totalRevenue(BigDecimal.ZERO).avgDealSize(BigDecimal.ZERO).winRate(BigDecimal.ZERO).quotaAttainment(BigDecimal.ZERO));

            PipelinePerformance.RepPerformance temp = builder.build();
            long newTotal = temp.getTotalDeals() + count;
            BigDecimal newRevenue = temp.getTotalRevenue().add(amount);
            builder.totalDeals(newTotal).totalRevenue(newRevenue);

            if (stage == Opportunity.OpportunityStage.CLOSED_WON) {
                builder.wonDeals(temp.getWonDeals() + count);
            } else if (stage == Opportunity.OpportunityStage.CLOSED_LOST) {
                builder.lostDeals(temp.getLostDeals() + count);
            } else {
                builder.openDeals(temp.getOpenDeals() + count);
            }
        }

        List<PipelinePerformance.RepPerformance> repPerformances = new ArrayList<>();
        for (var entry : repMap.entrySet()) {
            PipelinePerformance.RepPerformance rep = entry.getValue().build();
            if (rep.getTotalDeals() > 0) {
                rep.setAvgDealSize(rep.getTotalRevenue().divide(BigDecimal.valueOf(rep.getTotalDeals()), 2, RoundingMode.HALF_UP));
            }
            if (rep.getWonDeals() + rep.getLostDeals() > 0) {
                rep.setWinRate(BigDecimal.valueOf(rep.getWonDeals() * 100).divide(
                        BigDecimal.valueOf(rep.getWonDeals() + rep.getLostDeals()), 2, RoundingMode.HALF_UP));
            }
            // Quota attainment
            salesQuotaRepository.findCurrentQuota(tenantId, entry.getKey(), LocalDate.now())
                    .ifPresent(q -> rep.setQuotaAttainment(q.getAttainmentPct()));
            repPerformances.add(rep);
        }

        return PipelinePerformance.builder()
                .pipelineVelocity(velocity).avgDealSize(avgDealSize).winRate(winRate)
                .avgCycleDays(avgCycleDays).totalDeals(total)
                .repPerformances(repPerformances).build();
    }

    // ─── 19. Pipeline View (grouped by stage) ───────────────────────
    @Transactional(readOnly = true)
    public Map<String, List<OpportunityResponse>> getPipelineView() {
        String tenantId = TenantContext.getTenantId();
        List<Opportunity> all = opportunityRepository.findAllForPipeline(tenantId);
        return all.stream()
                .map(opportunityMapper::toResponse)
                .collect(Collectors.groupingBy(o -> o.getStage().name(), LinkedHashMap::new, Collectors.toList()));
    }

    // ─── Import / Export ───────────────────────────────────────────
    @Transactional
    @CacheEvict(value = "opportunities", allEntries = true)
    public Map<String, Object> importOpportunitiesFromFile(MultipartFile file, String userId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Importing opportunities from CSV for tenant: {}", tenantId);
        int imported = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return Map.of("imported", 0);
            String[] headers = headerLine.trim().split(",");
            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colMap.put(headers[i].trim().toLowerCase().replaceAll("[^a-z0-9_]", ""), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] vals = line.trim().split(",", -1);
                    if (vals.length < 1 || vals[0].isBlank()) continue;

                    Opportunity opp = new Opportunity();
                    opp.setTenantId(tenantId);

                    String name = getCsvCol(vals, colMap, "name");
                    if (name == null || name.isBlank()) continue;
                    opp.setName(name);

                    String amount = getCsvCol(vals, colMap, "amount");
                    if (amount != null) opp.setAmount(new BigDecimal(amount));

                    String stage = getCsvCol(vals, colMap, "stage");
                    if (stage != null) {
                        try { opp.setStage(Opportunity.OpportunityStage.valueOf(stage.toUpperCase())); }
                        catch (IllegalArgumentException e) { opp.setStage(Opportunity.OpportunityStage.PROSPECTING); }
                    }

                    String prob = getCsvCol(vals, colMap, "probability");
                    if (prob != null) opp.setProbability(Integer.parseInt(prob));

                    String closeDate = getCsvCol(vals, colMap, "closedate", "close_date");
                    if (closeDate != null) opp.setCloseDate(LocalDate.parse(closeDate));

                    opp.setDescription(getCsvCol(vals, colMap, "description"));
                    opp.setCurrency(getCsvCol(vals, colMap, "currency"));
                    if (opp.getCurrency() == null) opp.setCurrency("USD");
                    opp.setNextStep(getCsvCol(vals, colMap, "nextstep", "next_step"));
                    opp.setLeadSource(getCsvCol(vals, colMap, "leadsource", "lead_source", "source"));

                    opportunityRepository.save(opp);
                    imported++;
                } catch (Exception e) {
                    log.warn("Skipping CSV row: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file", e);
        }
        log.info("Imported {} opportunities", imported);
        return Map.of("imported", imported);
    }

    @Transactional(readOnly = true)
    public String exportOpportunitiesToCsv() {
        String tenantId = TenantContext.getTenantId();
        List<Opportunity> opps = opportunityRepository.findByTenantIdAndDeletedFalse(tenantId);
        StringBuilder sb = new StringBuilder();
        sb.append("name,amount,stage,probability,close_date,description,currency,next_step,lead_source\n");
        for (Opportunity o : opps) {
            sb.append(csvEscape(o.getName())).append(",")
              .append(o.getAmount() != null ? o.getAmount() : "").append(",")
              .append(o.getStage() != null ? o.getStage().name() : "").append(",")
              .append(o.getProbability() != null ? o.getProbability() : "").append(",")
              .append(o.getCloseDate() != null ? o.getCloseDate() : "").append(",")
              .append(csvEscape(o.getDescription())).append(",")
              .append(csvEscape(o.getCurrency())).append(",")
              .append(csvEscape(o.getNextStep())).append(",")
              .append(csvEscape(o.getLeadSource())).append("\n");
        }
        return sb.toString();
    }

    private String getCsvCol(String[] vals, Map<String, Integer> colMap, String... keys) {
        for (String key : keys) {
            Integer idx = colMap.get(key);
            if (idx != null && idx < vals.length) {
                String v = vals[idx].trim();
                if (!v.isEmpty()) return v;
            }
        }
        return null;
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ─── Helpers ─────────────────────────────────────────────────────
    private Opportunity findOpp(UUID id, String tenantId) {
        return opportunityRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));
    }

    private void recordActivity(UUID opportunityId, String tenantId, String type, String description, String userId) {
        try {
            activityRepository.save(OpportunityActivity.builder()
                    .opportunityId(opportunityId)
                    .activityType(type)
                    .description(description)
                    .tenantId(tenantId)
                    .createdBy(userId)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to record activity: {}", e.getMessage());
        }
    }

    private PagedResponse<OpportunityResponse> buildPaged(Page<Opportunity> p) {
        return PagedResponse.<OpportunityResponse>builder()
                .content(p.getContent().stream().map(this::toResponseWithExpectedRevenue).toList())
                .pageNumber(p.getNumber()).pageSize(p.getSize())
                .totalElements(p.getTotalElements()).totalPages(p.getTotalPages())
                .last(p.isLast()).first(p.isFirst()).build();
    }

    private int getStageProbabilityWeight(Opportunity.OpportunityStage stage) {
        return switch (stage) {
            case PROSPECTING -> 10;
            case QUALIFICATION -> 25;
            case NEEDS_ANALYSIS -> 40;
            case PROPOSAL -> 60;
            case NEGOTIATION -> 80;
            case CLOSED_WON -> 100;
            case CLOSED_LOST -> 0;
        };
    }

    private OpportunityResponse toResponseWithExpectedRevenue(Opportunity opp) {
        OpportunityResponse resp = opportunityMapper.toResponse(opp);
        if (opp.getAmount() != null && opp.getProbability() != null) {
            resp.setExpectedRevenue(opp.getAmount()
                    .multiply(BigDecimal.valueOf(opp.getProbability()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        return resp;
    }
}
