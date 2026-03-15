package com.crm.campaign.controller;

import com.crm.campaign.dto.*;
import com.crm.campaign.service.CampaignService;
import com.crm.common.dto.ApiResponse;
import com.crm.common.dto.PagedResponse;
import com.crm.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        CampaignResponse response = campaignService.createCampaign(request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Campaign created successfully"));
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable UUID campaignId,
            @Valid @RequestBody UpdateCampaignRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        CampaignResponse response = campaignService.updateCampaign(campaignId, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Campaign updated successfully"));
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaign(@PathVariable UUID campaignId) {
        CampaignResponse response = campaignService.getCampaignById(campaignId);
        return ResponseEntity.ok(ApiResponse.success(response, "Campaign retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CampaignResponse>>> getAllCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        PagedResponse<CampaignResponse> response = campaignService.getAllCampaigns(page, size, status, type, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response, "Campaigns retrieved successfully"));
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<Void>> deleteCampaign(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal UserPrincipal user) {
        campaignService.deleteCampaign(campaignId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Campaign deleted successfully"));
    }

    // ==================== MEMBERS ====================

    @PostMapping("/{campaignId}/members")
    public ResponseEntity<ApiResponse<List<CampaignMemberResponse>>> addMembers(
            @PathVariable UUID campaignId,
            @Valid @RequestBody AddMembersRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        List<CampaignMemberResponse> response = campaignService.addMembers(campaignId, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Members added successfully"));
    }

    @PatchMapping("/{campaignId}/members/{memberId}/status")
    public ResponseEntity<ApiResponse<CampaignMemberResponse>> updateMemberStatus(
            @PathVariable UUID campaignId,
            @PathVariable UUID memberId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal user) {
        String newStatus = body.get("status");
        CampaignMemberResponse response = campaignService.updateMemberStatus(campaignId, memberId, newStatus, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Member status updated"));
    }

    @GetMapping("/{campaignId}/members")
    public ResponseEntity<ApiResponse<PagedResponse<CampaignMemberResponse>>> getMembers(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResponse<CampaignMemberResponse> response = campaignService.getMembers(campaignId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Members retrieved successfully"));
    }

    // ==================== ROI ====================

    @GetMapping("/{campaignId}/roi")
    public ResponseEntity<ApiResponse<CampaignROIResponse>> getCampaignROI(@PathVariable UUID campaignId) {
        CampaignROIResponse response = campaignService.calculateROI(campaignId);
        return ResponseEntity.ok(ApiResponse.success(response, "Campaign ROI calculated successfully"));
    }
}
