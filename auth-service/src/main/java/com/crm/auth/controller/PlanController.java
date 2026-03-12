package com.crm.auth.controller;

import com.crm.auth.dto.TenantPlanResponse;
import com.crm.auth.service.TenantPlanService;
import com.crm.common.dto.ApiResponse;
import com.crm.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/plan")
@RequiredArgsConstructor
@Tag(name = "Tenant Plans", description = "Subscription plan management")
public class PlanController {

    private final TenantPlanService tenantPlanService;

    @GetMapping
    @Operation(summary = "Get current tenant plan and limits")
    public ResponseEntity<ApiResponse<TenantPlanResponse>> getCurrentPlan(
            @AuthenticationPrincipal UserPrincipal principal) {
        TenantPlanResponse plan = tenantPlanService.getPlan(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @PutMapping("/upgrade")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upgrade tenant plan (admin only)")
    public ResponseEntity<ApiResponse<TenantPlanResponse>> upgradePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String plan) {
        TenantPlanResponse response = tenantPlanService.upgradePlan(principal.getTenantId(), plan);
        return ResponseEntity.ok(ApiResponse.success(response, "Plan upgraded to " + plan));
    }
}
