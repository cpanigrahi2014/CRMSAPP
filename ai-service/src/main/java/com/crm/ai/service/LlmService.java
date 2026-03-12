package com.crm.ai.service;

import com.crm.ai.dto.LlmRequest;
import com.crm.ai.dto.LlmResponse;
import com.crm.ai.entity.AiRequestLog;
import com.crm.ai.repository.AiRequestLogRepository;
import com.crm.common.security.TenantContext;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final WebClient llmWebClient;
    private final AiRequestLogRepository aiRequestLogRepository;

    @Value("${ai.llm.default-model:gpt-4}")
    private String defaultModel;

    @Value("${ai.llm.timeout-seconds:60}")
    private int timeoutSeconds;

    @CircuitBreaker(name = "llmService", fallbackMethod = "llmFallback")
    @Retry(name = "llmService", fallbackMethod = "llmRetryFallback")
    public LlmResponse call(LlmRequest request) {
        String tenantId = TenantContext.getTenantId();
        String model = request.getModel() != null ? request.getModel() : defaultModel;
        log.info("Calling LLM API for tenant: {}, model: {}, maxTokens: {}", tenantId, model, request.getMaxTokens());

        Instant start = Instant.now();

        Map<String, Object> apiBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", request.getPrompt())
                ),
                "max_tokens", request.getMaxTokens(),
                "temperature", request.getTemperature()
        );

        Map<String, Object> responseBody = llmWebClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(apiBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        long latencyMs = Duration.between(start, Instant.now()).toMillis();

        String content = extractContent(responseBody);
        int tokensUsed = extractTokensUsed(responseBody);

        LlmResponse llmResponse = LlmResponse.builder()
                .content(content)
                .tokensUsed(tokensUsed)
                .build();

        logRequest(tenantId, "LLM_CALL", request.getPrompt(), content, model, tokensUsed, latencyMs);

        log.info("LLM API response received for tenant: {}, tokens: {}, latency: {}ms", tenantId, tokensUsed, latencyMs);
        return llmResponse;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return "";
        }
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract content from LLM response: {}", e.getMessage());
        }
        return responseBody.toString();
    }

    @SuppressWarnings("unchecked")
    private int extractTokensUsed(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return 0;
        }
        try {
            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
            if (usage != null && usage.get("total_tokens") != null) {
                return ((Number) usage.get("total_tokens")).intValue();
            }
        } catch (Exception e) {
            log.warn("Failed to extract token usage from LLM response: {}", e.getMessage());
        }
        return 0;
    }

    private void logRequest(String tenantId, String requestType, String input, String output,
                            String model, int tokensUsed, long latencyMs) {
        try {
            AiRequestLog requestLog = AiRequestLog.builder()
                    .tenantId(tenantId)
                    .requestType(requestType)
                    .inputData(truncate(input, 4000))
                    .outputData(truncate(output, 4000))
                    .model(model)
                    .tokensUsed(tokensUsed)
                    .latencyMs(latencyMs)
                    .build();
            aiRequestLogRepository.save(requestLog);
        } catch (Exception e) {
            log.error("Failed to log AI request: {}", e.getMessage(), e);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    /**
     * Fallback when circuit breaker is open or call fails.
     */
    public LlmResponse llmFallback(LlmRequest request, Throwable t) {
        log.error("LLM circuit breaker fallback triggered for model: {}. Error: {}", request.getModel(), t.getMessage());
        String tenantId = TenantContext.getTenantId();
        logRequest(tenantId != null ? tenantId : "UNKNOWN", "LLM_CALL_FALLBACK",
                request.getPrompt(), "CIRCUIT_BREAKER_OPEN", request.getModel(), 0, 0);

        return LlmResponse.builder()
                .content("AI service is temporarily unavailable. Please try again later.")
                .tokensUsed(0)
                .build();
    }

    /**
     * Fallback after all retries are exhausted.
     */
    public LlmResponse llmRetryFallback(LlmRequest request, Throwable t) {
        log.error("LLM retry fallback triggered for model: {}. Error: {}", request.getModel(), t.getMessage());
        String tenantId = TenantContext.getTenantId();
        logRequest(tenantId != null ? tenantId : "UNKNOWN", "LLM_CALL_RETRY_EXHAUSTED",
                request.getPrompt(), "RETRIES_EXHAUSTED: " + t.getMessage(), request.getModel(), 0, 0);

        return LlmResponse.builder()
                .content("AI service is temporarily unavailable after multiple attempts. Please try again later.")
                .tokensUsed(0)
                .build();
    }
}
