package com.crm.ai.controller;

import com.crm.ai.dto.*;
import com.crm.ai.service.*;
import com.crm.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI-powered CRM feature APIs")
public class AiController {

    private final LeadScoringService leadScoringService;
    private final NextBestActionService nextBestActionService;
    private final EmailDraftService emailDraftService;
    private final LlmService llmService;
    private final EmailReplyService emailReplyService;
    private final MeetingSummaryService meetingSummaryService;
    private final AutoLeadService autoLeadService;
    private final CsvFieldDetectionService csvFieldDetectionService;
    private final ContactEnrichmentService contactEnrichmentService;
    private final OnboardingService onboardingService;
    private final TranscriptionService transcriptionService;
    private final SentimentAnalysisService sentimentAnalysisService;

    @PostMapping("/lead-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Score a lead using AI analysis")
    public ResponseEntity<ApiResponse<LeadScoreResponse>> scoreLead(
            @Valid @RequestBody LeadScoreRequest request) {
        log.info("REST request to score lead: {}", request.getLeadId());
        LeadScoreResponse response = leadScoringService.scoreLead(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Lead scored successfully"));
    }

    @PostMapping("/next-best-action")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get next best action recommendations")
    public ResponseEntity<ApiResponse<NextBestActionResponse>> getNextBestAction(
            @Valid @RequestBody NextBestActionRequest request) {
        log.info("REST request to get next best actions for entity: {} ({})", request.getEntityId(), request.getEntityType());
        NextBestActionResponse response = nextBestActionService.getNextBestActions(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Next best actions generated successfully"));
    }

    @PostMapping("/email-draft")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Generate an AI-powered email draft")
    public ResponseEntity<ApiResponse<EmailDraftResponse>> generateEmailDraft(
            @Valid @RequestBody EmailDraftRequest request) {
        log.info("REST request to generate email draft for: {}", request.getTo());
        EmailDraftResponse response = emailDraftService.generateEmailDraft(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Email draft generated successfully"));
    }

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Perform generic LLM analysis")
    public ResponseEntity<ApiResponse<LlmResponse>> analyze(
            @Valid @RequestBody AnalyzeRequest request) {
        log.info("REST request for generic AI analysis");
        LlmRequest llmRequest = LlmRequest.builder()
                .model(request.getModel())
                .prompt(request.getPrompt())
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .build();
        LlmResponse response = llmService.call(llmRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Analysis completed successfully"));
    }

    // ---- Email Reply Generation ----

    @PostMapping("/email-reply")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Generate an AI-powered email reply")
    public ResponseEntity<ApiResponse<EmailReplyResponse>> generateEmailReply(
            @Valid @RequestBody EmailReplyRequest request) {
        log.info("REST request to generate email reply for: {}", request.getOriginalFrom());
        EmailReplyResponse response = emailReplyService.generateReply(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Email reply generated successfully"));
    }

    // ---- Meeting Summary ----

    @PostMapping("/meeting-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Generate an AI meeting summary with CRM update suggestions")
    public ResponseEntity<ApiResponse<MeetingSummaryResponse>> summarizeMeeting(
            @Valid @RequestBody MeetingSummaryRequest request) {
        log.info("REST request to summarize meeting: {}", request.getMeetingTitle());
        MeetingSummaryResponse response = meetingSummaryService.summarizeMeeting(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Meeting summary generated successfully"));
    }

    // ---- Auto-Lead Extraction ----

    @PostMapping("/auto-lead")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Extract lead information from email or meeting content")
    public ResponseEntity<ApiResponse<AutoLeadResponse>> extractLead(
            @Valid @RequestBody AutoLeadRequest request) {
        log.info("REST request to extract lead from: {}", request.getSourceType());
        AutoLeadResponse response = autoLeadService.extractLead(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Lead extracted successfully"));
    }

    @PostMapping("/auto-lead/action")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Approve or reject an auto-extracted lead")
    public ResponseEntity<ApiResponse<AutoLeadResponse>> actionAutoLead(
            @Valid @RequestBody AutoLeadActionRequest request) {
        log.info("REST request to action auto-lead: {}", request.getAutoLeadId());
        AutoLeadResponse response = autoLeadService.actionAutoLead(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Auto-lead updated successfully"));
    }

    // ---- CSV Field Detection ----

    @PostMapping("/csv-detect-fields")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Auto-detect and map CSV columns to CRM fields using AI")
    public ResponseEntity<ApiResponse<CsvFieldDetectionResponse>> detectCsvFields(
            @Valid @RequestBody CsvFieldDetectionRequest request) {
        log.info("REST request to detect CSV fields for entity type: {}", request.getEntityType());
        CsvFieldDetectionResponse response = csvFieldDetectionService.detectFields(request);
        return ResponseEntity.ok(ApiResponse.success(response, "CSV fields detected successfully"));
    }

    // ---- Contact Enrichment ----

    @PostMapping("/enrich-contact")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Enrich contact data using AI-powered analysis")
    public ResponseEntity<ApiResponse<ContactEnrichmentResponse>> enrichContact(
            @Valid @RequestBody ContactEnrichmentRequest request) {
        log.info("REST request to enrich contact: {}", request.getContactId());
        ContactEnrichmentResponse response = contactEnrichmentService.enrichContact(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Contact enriched successfully"));
    }

    // ---- AI Onboarding Assistant ----

    @GetMapping("/onboarding/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get onboarding checklist status")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getOnboardingStatus() {
        log.info("REST request to get onboarding status");
        OnboardingStatusResponse response = onboardingService.getStatus();
        return ResponseEntity.ok(ApiResponse.success(response, "Onboarding status retrieved"));
    }

    @PostMapping("/onboarding/complete-step")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Mark an onboarding step as complete")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> completeOnboardingStep(
            @Valid @RequestBody OnboardingActionRequest request) {
        log.info("REST request to complete onboarding step: {}", request.getStepId());
        OnboardingStatusResponse response = onboardingService.completeStep(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Step completed"));
    }

    @PostMapping("/onboarding/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Reset onboarding progress")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> resetOnboarding() {
        log.info("REST request to reset onboarding");
        OnboardingStatusResponse response = onboardingService.resetOnboarding();
        return ResponseEntity.ok(ApiResponse.success(response, "Onboarding reset"));
    }

    @GetMapping("/onboarding/guidance/{stepId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get AI guidance for a specific onboarding step")
    public ResponseEntity<ApiResponse<String>> getOnboardingGuidance(@PathVariable String stepId) {
        log.info("REST request for onboarding guidance: {}", stepId);
        String guidance = onboardingService.getAiGuidance(stepId);
        return ResponseEntity.ok(ApiResponse.success(guidance, "Guidance generated"));
    }

    // ---- AI Transcription ----

    @PostMapping("/transcribe")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Transcribe and structure conversation content using AI")
    public ResponseEntity<ApiResponse<TranscriptionResponse>> transcribe(
            @Valid @RequestBody TranscriptionRequest request) {
        log.info("REST request to transcribe content, sourceType: {}", request.getSourceType());
        TranscriptionResponse response = transcriptionService.transcribe(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Transcription completed successfully"));
    }

    // ---- AI Sentiment Analysis ----

    @PostMapping("/sentiment-analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Analyze sentiment of a call or conversation")
    public ResponseEntity<ApiResponse<SentimentAnalysisResponse>> analyzeSentiment(
            @Valid @RequestBody SentimentAnalysisRequest request) {
        log.info("REST request for sentiment analysis, sourceType: {}", request.getSourceType());
        SentimentAnalysisResponse response = sentimentAnalysisService.analyze(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Sentiment analysis completed successfully"));
    }
}
