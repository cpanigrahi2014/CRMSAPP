package com.crm.opportunity.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.opportunity.dto.SalesQuotaRequest;
import com.crm.opportunity.dto.SalesQuotaResponse;
import com.crm.opportunity.service.SalesQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotas")
@RequiredArgsConstructor
public class SalesQuotaController {

    private final SalesQuotaService salesQuotaService;

    @PostMapping
    public ResponseEntity<ApiResponse<SalesQuotaResponse>> create(
            @RequestBody SalesQuotaRequest request,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(salesQuotaService.createQuota(request, tenantId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesQuotaResponse>> update(
            @PathVariable UUID id,
            @RequestBody SalesQuotaRequest request,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(salesQuotaService.updateQuota(id, request, tenantId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesQuotaResponse>> getById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(salesQuotaService.getQuota(id, tenantId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalesQuotaResponse>>> getAll(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(salesQuotaService.getAllQuotas(tenantId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<SalesQuotaResponse>>> getByUser(
            @PathVariable String userId,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(salesQuotaService.getQuotasByUser(userId, tenantId)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SalesQuotaResponse>>> getActive(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(salesQuotaService.getActiveQuotas(tenantId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {
        salesQuotaService.deleteQuota(id, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/recalculate")
    public ResponseEntity<ApiResponse<Void>> recalculate(
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "default") String tenantId) {
        salesQuotaService.recalculateAllQuotas(tenantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
