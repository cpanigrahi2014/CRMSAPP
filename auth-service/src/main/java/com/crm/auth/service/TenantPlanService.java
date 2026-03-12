package com.crm.auth.service;

import com.crm.auth.dto.TenantPlanResponse;
import com.crm.auth.entity.TenantPlan;
import com.crm.auth.repository.TenantPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPlanService {

    private final TenantPlanRepository tenantPlanRepository;

    /**
     * Get current plan for a tenant. Auto-creates FREE plan if none exists.
     */
    @Transactional
    public TenantPlanResponse getPlan(String tenantId) {
        TenantPlan plan = tenantPlanRepository.findByTenantId(tenantId)
                .orElseGet(() -> createFreePlan(tenantId));
        return toResponse(plan);
    }

    /**
     * Upgrade tenant to a paid plan.
     */
    @Transactional
    public TenantPlanResponse upgradePlan(String tenantId, String planName) {
        TenantPlan plan = tenantPlanRepository.findByTenantId(tenantId)
                .orElseGet(() -> createFreePlan(tenantId));

        switch (planName.toUpperCase()) {
            case "STARTER" -> applyStarterLimits(plan);
            case "PROFESSIONAL" -> applyProfessionalLimits(plan);
            case "ENTERPRISE" -> applyEnterpriseLimits(plan);
            default -> applyFreeLimits(plan);
        }

        plan.setPlanName(planName.toUpperCase());
        TenantPlan saved = tenantPlanRepository.save(plan);
        log.info("Tenant {} upgraded to plan {}", tenantId, planName);
        return toResponse(saved);
    }

    /**
     * Auto-create FREE plan for new tenants (called during registration).
     */
    @Transactional
    public TenantPlan ensurePlanExists(String tenantId) {
        return tenantPlanRepository.findByTenantId(tenantId)
                .orElseGet(() -> createFreePlan(tenantId));
    }

    private TenantPlan createFreePlan(String tenantId) {
        TenantPlan plan = TenantPlan.builder()
                .tenantId(tenantId)
                .build(); // defaults to FREE with all free-tier limits
        return tenantPlanRepository.save(plan);
    }

    private void applyFreeLimits(TenantPlan plan) {
        plan.setMaxUsers(3);
        plan.setMaxCustomObjects(2);
        plan.setMaxWorkflows(3);
        plan.setMaxDashboards(1);
        plan.setMaxPipelines(1);
        plan.setMaxRoles(2);
        plan.setMaxRecordsPerObject(100);
        plan.setAiConfigEnabled(true);
        plan.setAiInsightsEnabled(false);
        plan.setEmailTrackingEnabled(false);
        plan.setApiAccessEnabled(false);
        plan.setIntegrationsEnabled(false);
    }

    private void applyStarterLimits(TenantPlan plan) {
        plan.setMaxUsers(5);
        plan.setMaxCustomObjects(5);
        plan.setMaxWorkflows(10);
        plan.setMaxDashboards(3);
        plan.setMaxPipelines(3);
        plan.setMaxRoles(5);
        plan.setMaxRecordsPerObject(1000);
        plan.setAiConfigEnabled(true);
        plan.setAiInsightsEnabled(false);
        plan.setEmailTrackingEnabled(true);
        plan.setApiAccessEnabled(false);
        plan.setIntegrationsEnabled(false);
    }

    private void applyProfessionalLimits(TenantPlan plan) {
        plan.setMaxUsers(25);
        plan.setMaxCustomObjects(50);
        plan.setMaxWorkflows(100);
        plan.setMaxDashboards(20);
        plan.setMaxPipelines(10);
        plan.setMaxRoles(20);
        plan.setMaxRecordsPerObject(50000);
        plan.setAiConfigEnabled(true);
        plan.setAiInsightsEnabled(true);
        plan.setEmailTrackingEnabled(true);
        plan.setApiAccessEnabled(true);
        plan.setIntegrationsEnabled(true);
    }

    private void applyEnterpriseLimits(TenantPlan plan) {
        plan.setMaxUsers(999);
        plan.setMaxCustomObjects(999);
        plan.setMaxWorkflows(999);
        plan.setMaxDashboards(999);
        plan.setMaxPipelines(999);
        plan.setMaxRoles(999);
        plan.setMaxRecordsPerObject(999999);
        plan.setAiConfigEnabled(true);
        plan.setAiInsightsEnabled(true);
        plan.setEmailTrackingEnabled(true);
        plan.setApiAccessEnabled(true);
        plan.setIntegrationsEnabled(true);
    }

    private TenantPlanResponse toResponse(TenantPlan plan) {
        return TenantPlanResponse.builder()
                .tenantId(plan.getTenantId())
                .planName(plan.getPlanName())
                .maxUsers(plan.getMaxUsers())
                .maxCustomObjects(plan.getMaxCustomObjects())
                .maxWorkflows(plan.getMaxWorkflows())
                .maxDashboards(plan.getMaxDashboards())
                .maxPipelines(plan.getMaxPipelines())
                .maxRoles(plan.getMaxRoles())
                .maxRecordsPerObject(plan.getMaxRecordsPerObject())
                .aiConfigEnabled(plan.isAiConfigEnabled())
                .aiInsightsEnabled(plan.isAiInsightsEnabled())
                .emailTrackingEnabled(plan.isEmailTrackingEnabled())
                .apiAccessEnabled(plan.isApiAccessEnabled())
                .integrationsEnabled(plan.isIntegrationsEnabled())
                .startedAt(plan.getStartedAt())
                .expiresAt(plan.getExpiresAt())
                .active(plan.isActive())
                .build();
    }
}
