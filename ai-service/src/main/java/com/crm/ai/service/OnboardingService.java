package com.crm.ai.service;

import com.crm.ai.dto.OnboardingActionRequest;
import com.crm.ai.dto.OnboardingStatusResponse;
import com.crm.ai.dto.OnboardingStatusResponse.OnboardingStep;
import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final LlmService llmService;
    private final StringRedisTemplate redisTemplate;

    private static final String ONBOARDING_KEY_PREFIX = "onboarding:";

    private static final List<OnboardingStep> DEFAULT_STEPS = List.of(
            OnboardingStep.builder()
                    .id("create_pipeline").title("Create Your Sales Pipeline")
                    .description("Set up your first pipeline with stages that match your sales process")
                    .category("Setup").completed(false).actionUrl("/ai-config")
                    .aiHint("Tell the AI: 'Create a pipeline called Main Sales with stages: Prospecting, Discovery, Proposal, Negotiation, Closed Won, Closed Lost'")
                    .build(),
            OnboardingStep.builder()
                    .id("add_first_lead").title("Add Your First Lead")
                    .description("Create a new lead or import from CSV")
                    .category("Data").completed(false).actionUrl("/leads")
                    .aiHint("Go to Leads and click 'Add Lead', or use CSV import for bulk upload")
                    .build(),
            OnboardingStep.builder()
                    .id("add_first_contact").title("Add Your First Contact")
                    .description("Create a contact record for a key person")
                    .category("Data").completed(false).actionUrl("/contacts")
                    .aiHint("Go to Contacts and add a key contact from your network")
                    .build(),
            OnboardingStep.builder()
                    .id("add_first_account").title("Create Your First Account")
                    .description("Set up a company account to track")
                    .category("Data").completed(false).actionUrl("/accounts")
                    .aiHint("Go to Accounts and add a prospect company you're working with")
                    .build(),
            OnboardingStep.builder()
                    .id("create_opportunity").title("Create an Opportunity")
                    .description("Track a deal in your pipeline")
                    .category("Sales").completed(false).actionUrl("/opportunities")
                    .aiHint("Go to Opportunities and create a deal linked to your account")
                    .build(),
            OnboardingStep.builder()
                    .id("setup_workflow").title("Set Up an Automation")
                    .description("Create a workflow to automate a repetitive task")
                    .category("Automation").completed(false).actionUrl("/ai-config")
                    .aiHint("Tell the AI: 'Create a workflow that sends an email notification when a lead status changes to Qualified'")
                    .build(),
            OnboardingStep.builder()
                    .id("explore_dashboard").title("Explore Your Dashboard")
                    .description("Check out the smart default dashboard with pipeline and revenue insights")
                    .category("Insights").completed(false).actionUrl("/dashboard")
                    .aiHint("Visit the Dashboard to see pipeline stages, revenue charts, and forecasts")
                    .build(),
            OnboardingStep.builder()
                    .id("try_ai_insights").title("Try AI Insights")
                    .description("Explore AI-powered lead scoring, forecasts, and recommendations")
                    .category("AI").completed(false).actionUrl("/ai-insights")
                    .aiHint("Go to AI Insights to see lead scores, win probabilities, and next best actions")
                    .build(),
            OnboardingStep.builder()
                    .id("invite_team").title("Invite Your Team")
                    .description("Add team members for collaboration")
                    .category("Team").completed(false).actionUrl("/settings")
                    .aiHint("Go to Settings to manage user roles and invite team members")
                    .build(),
            OnboardingStep.builder()
                    .id("configure_integrations").title("Connect Integrations")
                    .description("Connect email, calendar, or other tools")
                    .category("Integrations").completed(false).actionUrl("/integrations")
                    .aiHint("Go to Integrations to connect your email provider, calendar, or other CRM tools")
                    .build()
    );

    public OnboardingStatusResponse getStatus() {
        String tenantId = TenantContext.getTenantId();
        Set<String> completedStepIds = getCompletedSteps(tenantId);

        List<OnboardingStep> steps = DEFAULT_STEPS.stream()
                .map(s -> OnboardingStep.builder()
                        .id(s.getId())
                        .title(s.getTitle())
                        .description(s.getDescription())
                        .category(s.getCategory())
                        .completed(completedStepIds.contains(s.getId()))
                        .actionUrl(s.getActionUrl())
                        .aiHint(s.getAiHint())
                        .build())
                .toList();

        int completed = (int) steps.stream().filter(OnboardingStep::isCompleted).count();
        int total = steps.size();
        int percent = total > 0 ? (completed * 100) / total : 0;

        String nextRec = steps.stream()
                .filter(s -> !s.isCompleted())
                .findFirst()
                .map(s -> s.getAiHint())
                .orElse("You're all set! Your CRM is fully configured.");

        return OnboardingStatusResponse.builder()
                .completedSteps(completed)
                .totalSteps(total)
                .progressPercent(percent)
                .steps(steps)
                .nextRecommendation(nextRec)
                .build();
    }

    public OnboardingStatusResponse completeStep(OnboardingActionRequest request) {
        String tenantId = TenantContext.getTenantId();
        String key = ONBOARDING_KEY_PREFIX + tenantId;

        try {
            redisTemplate.opsForSet().add(key, request.getStepId());
        } catch (Exception e) {
            log.warn("Redis unavailable for onboarding tracking: {}", e.getMessage());
        }

        return getStatus();
    }

    public OnboardingStatusResponse resetOnboarding() {
        String tenantId = TenantContext.getTenantId();
        String key = ONBOARDING_KEY_PREFIX + tenantId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis unavailable for onboarding reset: {}", e.getMessage());
        }
        return getStatus();
    }

    public String getAiGuidance(String stepId) {
        OnboardingStep step = DEFAULT_STEPS.stream()
                .filter(s -> s.getId().equals(stepId))
                .findFirst()
                .orElse(null);

        if (step == null) {
            return "Step not found. Please check the step ID.";
        }

        String prompt = "You are a friendly CRM onboarding assistant. The user needs help with this step:\n\n"
                + "Step: " + step.getTitle() + "\n"
                + "Description: " + step.getDescription() + "\n"
                + "Category: " + step.getCategory() + "\n\n"
                + "Provide a brief, encouraging 2-3 sentence guide on how to complete this step. "
                + "Be specific and actionable. Mention the relevant page URL: " + step.getActionUrl();

        try {
            LlmRequest llmRequest = LlmRequest.builder()
                    .prompt(prompt)
                    .maxTokens(256)
                    .temperature(0.7)
                    .build();
            LlmResponse response = llmService.call(llmRequest);
            return response.getContent();
        } catch (Exception e) {
            log.warn("AI guidance generation failed: {}", e.getMessage());
            return step.getAiHint();
        }
    }

    private Set<String> getCompletedSteps(String tenantId) {
        String key = ONBOARDING_KEY_PREFIX + tenantId;
        try {
            Set<String> members = redisTemplate.opsForSet().members(key);
            return members != null ? members : Set.of();
        } catch (Exception e) {
            log.warn("Redis unavailable for onboarding status: {}", e.getMessage());
            return Set.of();
        }
    }
}
