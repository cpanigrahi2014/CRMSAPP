package com.crm.ai.controller;

import com.crm.ai.dto.*;
import com.crm.ai.service.AiInsightsService;
import com.crm.ai.service.AutoLeadService;
import com.crm.ai.service.EmailReplyService;
import com.crm.ai.service.MeetingSummaryService;
import com.crm.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/insights")
@RequiredArgsConstructor
@Tag(name = "AI Insights", description = "AI-powered analytics and insights APIs")
public class AiInsightsController {

    private final AiInsightsService aiInsightsService;
    private final EmailReplyService emailReplyService;
    private final MeetingSummaryService meetingSummaryService;
    private final AutoLeadService autoLeadService;

    // ---- Lead Scores ----

    @GetMapping("/lead-scores")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get predictive lead scores")
    public ResponseEntity<ApiResponse<List<PredictiveLeadScoreResponse>>> getLeadScores() {
        log.info("REST request to get predictive lead scores");
        List<PredictiveLeadScoreResponse> result = aiInsightsService.getLeadScores();
        return ResponseEntity.ok(ApiResponse.success(result, "Lead scores retrieved successfully"));
    }

    // ---- Win Probability ----

    @GetMapping("/win-probability")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get win probability analysis for all opportunities")
    public ResponseEntity<ApiResponse<List<WinProbabilityResponse>>> getWinProbabilities() {
        log.info("REST request to get win probabilities");
        List<WinProbabilityResponse> result = aiInsightsService.getWinProbabilities();
        return ResponseEntity.ok(ApiResponse.success(result, "Win probabilities retrieved successfully"));
    }

    @GetMapping("/win-probability/{opportunityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get win probability for a specific opportunity")
    public ResponseEntity<ApiResponse<WinProbabilityResponse>> getWinProbabilityByOpportunity(
            @PathVariable String opportunityId) {
        log.info("REST request to get win probability for opportunity: {}", opportunityId);
        WinProbabilityResponse result = aiInsightsService.getWinProbabilityByOpportunity(opportunityId);
        return ResponseEntity.ok(ApiResponse.success(result, "Win probability retrieved successfully"));
    }

    // ---- Sales Forecasts ----

    @GetMapping("/forecasts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get AI-powered sales forecasts")
    public ResponseEntity<ApiResponse<List<SalesForecastResponse>>> getSalesForecasts() {
        log.info("REST request to get sales forecasts");
        List<SalesForecastResponse> result = aiInsightsService.getSalesForecasts();
        return ResponseEntity.ok(ApiResponse.success(result, "Sales forecasts retrieved successfully"));
    }

    // ---- Churn Predictions ----

    @GetMapping("/churn-predictions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get churn predictions for all accounts")
    public ResponseEntity<ApiResponse<List<ChurnPredictionResponse>>> getChurnPredictions() {
        log.info("REST request to get churn predictions");
        List<ChurnPredictionResponse> result = aiInsightsService.getChurnPredictions();
        return ResponseEntity.ok(ApiResponse.success(result, "Churn predictions retrieved successfully"));
    }

    @GetMapping("/churn-predictions/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get churn prediction for a specific account")
    public ResponseEntity<ApiResponse<ChurnPredictionResponse>> getChurnPredictionByAccount(
            @PathVariable String accountId) {
        log.info("REST request to get churn prediction for account: {}", accountId);
        ChurnPredictionResponse result = aiInsightsService.getChurnPredictionByAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success(result, "Churn prediction retrieved successfully"));
    }

    // ---- AI Report Insights ----

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get AI-generated report insights")
    public ResponseEntity<ApiResponse<List<AiReportInsightResponse>>> getReportInsights() {
        log.info("REST request to get AI report insights");
        List<AiReportInsightResponse> result = aiInsightsService.getReportInsights();
        return ResponseEntity.ok(ApiResponse.success(result, "Report insights retrieved successfully"));
    }

    @GetMapping("/reports/type/{insightType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get AI report insights filtered by type")
    public ResponseEntity<ApiResponse<List<AiReportInsightResponse>>> getReportInsightsByType(
            @PathVariable String insightType) {
        log.info("REST request to get AI report insights by type: {}", insightType);
        List<AiReportInsightResponse> result = aiInsightsService.getReportInsightsByType(insightType);
        return ResponseEntity.ok(ApiResponse.success(result, "Report insights retrieved successfully"));
    }

    // ---- Data Entry Suggestions ----

    @GetMapping("/suggestions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get pending data entry suggestions")
    public ResponseEntity<ApiResponse<List<DataEntrySuggestionResponse>>> getPendingSuggestions() {
        log.info("REST request to get pending data entry suggestions");
        List<DataEntrySuggestionResponse> result = aiInsightsService.getPendingSuggestions();
        return ResponseEntity.ok(ApiResponse.success(result, "Data suggestions retrieved successfully"));
    }

    @PostMapping("/suggestions/action")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Accept or reject a data entry suggestion")
    public ResponseEntity<ApiResponse<DataEntrySuggestionResponse>> actionSuggestion(
            @Valid @RequestBody SuggestionActionRequest request) {
        log.info("REST request to action suggestion: {}", request.getSuggestionId());
        DataEntrySuggestionResponse result = aiInsightsService.actionSuggestion(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Suggestion updated successfully"));
    }

    // ---- Sales Insights ----

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get AI sales insights")
    public ResponseEntity<ApiResponse<List<AiSalesInsightResponse>>> getSalesInsights() {
        log.info("REST request to get AI sales insights");
        List<AiSalesInsightResponse> result = aiInsightsService.getSalesInsights();
        return ResponseEntity.ok(ApiResponse.success(result, "Sales insights retrieved successfully"));
    }

    @GetMapping("/sales/type/{insightType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get AI sales insights filtered by type")
    public ResponseEntity<ApiResponse<List<AiSalesInsightResponse>>> getSalesInsightsByType(
            @PathVariable String insightType) {
        log.info("REST request to get AI sales insights by type: {}", insightType);
        List<AiSalesInsightResponse> result = aiInsightsService.getSalesInsightsByType(insightType);
        return ResponseEntity.ok(ApiResponse.success(result, "Sales insights retrieved successfully"));
    }

    // ---- Email Replies ----

    @GetMapping("/email-replies")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get recent AI-generated email replies")
    public ResponseEntity<ApiResponse<List<EmailReplyResponse>>> getEmailReplies() {
        log.info("REST request to get email replies");
        List<EmailReplyResponse> result = emailReplyService.getRecentReplies();
        return ResponseEntity.ok(ApiResponse.success(result, "Email replies retrieved successfully"));
    }

    // ---- Meeting Summaries ----

    @GetMapping("/meeting-summaries")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get recent AI meeting summaries")
    public ResponseEntity<ApiResponse<List<MeetingSummaryResponse>>> getMeetingSummaries() {
        log.info("REST request to get meeting summaries");
        List<MeetingSummaryResponse> result = meetingSummaryService.getRecentSummaries();
        return ResponseEntity.ok(ApiResponse.success(result, "Meeting summaries retrieved successfully"));
    }

    // ---- Auto-Generated Leads ----

    @GetMapping("/auto-leads")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get auto-extracted leads")
    public ResponseEntity<ApiResponse<List<AutoLeadResponse>>> getAutoLeads() {
        log.info("REST request to get auto-extracted leads");
        List<AutoLeadResponse> result = autoLeadService.getAllAutoLeads();
        return ResponseEntity.ok(ApiResponse.success(result, "Auto-leads retrieved successfully"));
    }

    @GetMapping("/auto-leads/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get pending auto-extracted leads")
    public ResponseEntity<ApiResponse<List<AutoLeadResponse>>> getPendingAutoLeads() {
        log.info("REST request to get pending auto-leads");
        List<AutoLeadResponse> result = autoLeadService.getPendingLeads();
        return ResponseEntity.ok(ApiResponse.success(result, "Pending auto-leads retrieved successfully"));
    }
}
