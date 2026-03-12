package com.crm.integration.service;

import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.integration.dto.*;
import com.crm.integration.entity.*;
import com.crm.integration.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationManagementService {

    private final ApiEndpointRepository apiEndpointRepo;
    private final WebhookConfigRepository webhookRepo;
    private final ThirdPartyIntegrationRepository integrationRepo;
    private final DataSyncRepository dataSyncRepo;
    private final ExternalConnectorRepository connectorRepo;
    private final ApiAuthConfigRepository authConfigRepo;
    private final IntegrationHealthRepository healthRepo;
    private final IntegrationErrorRepository errorRepo;
    private final ObjectMapper objectMapper;

    // ---- API Endpoints ----

    @Transactional(readOnly = true)
    public List<ApiEndpointResponse> getApiEndpoints() {
        String tenantId = TenantContext.getTenantId();
        return apiEndpointRepo.findByTenantIdOrderByNameAsc(tenantId)
                .stream().map(this::toApiEndpointResponse).toList();
    }

    @Transactional
    public ApiEndpointResponse createApiEndpoint(ApiEndpointRequest request) {
        String tenantId = TenantContext.getTenantId();
        ApiEndpoint entity = ApiEndpoint.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .path(request.getPath())
                .method(request.getMethod())
                .description(request.getDescription())
                .authRequired(request.isAuthRequired())
                .rateLimit(request.getRateLimit())
                .enabled(request.isEnabled())
                .version(request.getVersion())
                .build();
        return toApiEndpointResponse(apiEndpointRepo.save(entity));
    }

    @Transactional
    public ApiEndpointResponse updateApiEndpoint(UUID id, ApiEndpointRequest request) {
        String tenantId = TenantContext.getTenantId();
        ApiEndpoint entity = apiEndpointRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("API endpoint not found: " + id));
        entity.setName(request.getName());
        entity.setPath(request.getPath());
        entity.setMethod(request.getMethod());
        entity.setDescription(request.getDescription());
        entity.setAuthRequired(request.isAuthRequired());
        entity.setRateLimit(request.getRateLimit());
        entity.setEnabled(request.isEnabled());
        entity.setVersion(request.getVersion());
        return toApiEndpointResponse(apiEndpointRepo.save(entity));
    }

    @Transactional
    public void deleteApiEndpoint(UUID id) {
        String tenantId = TenantContext.getTenantId();
        ApiEndpoint entity = apiEndpointRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("API endpoint not found: " + id));
        apiEndpointRepo.delete(entity);
    }

    // ---- Webhooks ----

    @Transactional(readOnly = true)
    public List<WebhookConfigResponse> getWebhooks() {
        String tenantId = TenantContext.getTenantId();
        return webhookRepo.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toWebhookResponse).toList();
    }

    @Transactional
    public WebhookConfigResponse createWebhook(WebhookConfigRequest request) {
        String tenantId = TenantContext.getTenantId();
        WebhookConfig entity = WebhookConfig.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .url(request.getUrl())
                .events(toJson(request.getEvents()))
                .active(request.isActive())
                .retryCount(request.getRetryCount())
                .retryDelayMs(request.getRetryDelayMs())
                .build();
        return toWebhookResponse(webhookRepo.save(entity));
    }

    @Transactional
    public WebhookConfigResponse updateWebhook(UUID id, WebhookConfigRequest request) {
        String tenantId = TenantContext.getTenantId();
        WebhookConfig entity = webhookRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook not found: " + id));
        entity.setName(request.getName());
        entity.setUrl(request.getUrl());
        entity.setEvents(toJson(request.getEvents()));
        entity.setActive(request.isActive());
        entity.setRetryCount(request.getRetryCount());
        entity.setRetryDelayMs(request.getRetryDelayMs());
        return toWebhookResponse(webhookRepo.save(entity));
    }

    @Transactional
    public void deleteWebhook(UUID id) {
        String tenantId = TenantContext.getTenantId();
        WebhookConfig entity = webhookRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook not found: " + id));
        webhookRepo.delete(entity);
    }

    // ---- Third-Party Integrations ----

    @Transactional(readOnly = true)
    public List<IntegrationResponse> getIntegrations() {
        String tenantId = TenantContext.getTenantId();
        return integrationRepo.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toIntegrationResponse).toList();
    }

    @Transactional
    public IntegrationResponse createIntegration(IntegrationRequest request) {
        String tenantId = TenantContext.getTenantId();
        ThirdPartyIntegration entity = ThirdPartyIntegration.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .provider(request.getProvider())
                .type(request.getType())
                .description(request.getDescription())
                .authType(request.getAuthType())
                .enabled(request.isEnabled())
                .status(request.isEnabled() ? "ACTIVE" : "INACTIVE")
                .build();
        return toIntegrationResponse(integrationRepo.save(entity));
    }

    @Transactional
    public IntegrationResponse updateIntegration(UUID id, IntegrationRequest request) {
        String tenantId = TenantContext.getTenantId();
        ThirdPartyIntegration entity = integrationRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Integration not found: " + id));
        entity.setName(request.getName());
        entity.setProvider(request.getProvider());
        entity.setType(request.getType());
        entity.setDescription(request.getDescription());
        entity.setAuthType(request.getAuthType());
        entity.setEnabled(request.isEnabled());
        entity.setStatus(request.isEnabled() ? "ACTIVE" : "INACTIVE");
        return toIntegrationResponse(integrationRepo.save(entity));
    }

    @Transactional
    public void deleteIntegration(UUID id) {
        String tenantId = TenantContext.getTenantId();
        ThirdPartyIntegration entity = integrationRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Integration not found: " + id));
        integrationRepo.delete(entity);
    }

    // ---- Data Syncs ----

    @Transactional(readOnly = true)
    public List<DataSyncResponse> getDataSyncs() {
        String tenantId = TenantContext.getTenantId();
        return dataSyncRepo.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toDataSyncResponse).toList();
    }

    @Transactional
    public DataSyncResponse createDataSync(DataSyncRequest request) {
        String tenantId = TenantContext.getTenantId();
        ThirdPartyIntegration integration = integrationRepo.findByIdAndTenantId(request.getIntegrationId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Integration not found: " + request.getIntegrationId()));
        DataSync entity = DataSync.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .integration(integration)
                .entityType(request.getEntityType())
                .direction(request.getDirection())
                .schedule(request.getSchedule())
                .fieldMapping(request.getFieldMapping() != null ? toJson(request.getFieldMapping()) : null)
                .enabled(request.isEnabled())
                .build();
        return toDataSyncResponse(dataSyncRepo.save(entity));
    }

    @Transactional
    public DataSyncResponse updateDataSync(UUID id, DataSyncRequest request) {
        String tenantId = TenantContext.getTenantId();
        DataSync entity = dataSyncRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Data sync not found: " + id));
        entity.setName(request.getName());
        entity.setEntityType(request.getEntityType());
        entity.setDirection(request.getDirection());
        entity.setSchedule(request.getSchedule());
        entity.setFieldMapping(request.getFieldMapping() != null ? toJson(request.getFieldMapping()) : null);
        entity.setEnabled(request.isEnabled());
        return toDataSyncResponse(dataSyncRepo.save(entity));
    }

    @Transactional
    public void deleteDataSync(UUID id) {
        String tenantId = TenantContext.getTenantId();
        DataSync entity = dataSyncRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Data sync not found: " + id));
        dataSyncRepo.delete(entity);
    }

    // ---- External Connectors ----

    @Transactional(readOnly = true)
    public List<ConnectorResponse> getConnectors() {
        String tenantId = TenantContext.getTenantId();
        return connectorRepo.findByTenantIdOrderByNameAsc(tenantId)
                .stream().map(this::toConnectorResponse).toList();
    }

    @Transactional
    public ConnectorResponse createConnector(ConnectorRequest request) {
        String tenantId = TenantContext.getTenantId();
        ExternalConnector entity = ExternalConnector.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .type(request.getType())
                .host(request.getHost())
                .port(request.getPort())
                .databaseName(request.getDatabaseName())
                .baseUrl(request.getBaseUrl())
                .connectionString(request.getConnectionString())
                .enabled(request.isEnabled())
                .status(request.isEnabled() ? "ACTIVE" : "INACTIVE")
                .build();
        return toConnectorResponse(connectorRepo.save(entity));
    }

    @Transactional
    public ConnectorResponse updateConnector(UUID id, ConnectorRequest request) {
        String tenantId = TenantContext.getTenantId();
        ExternalConnector entity = connectorRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Connector not found: " + id));
        entity.setName(request.getName());
        entity.setType(request.getType());
        entity.setHost(request.getHost());
        entity.setPort(request.getPort());
        entity.setDatabaseName(request.getDatabaseName());
        entity.setBaseUrl(request.getBaseUrl());
        entity.setConnectionString(request.getConnectionString());
        entity.setEnabled(request.isEnabled());
        entity.setStatus(request.isEnabled() ? "ACTIVE" : "INACTIVE");
        return toConnectorResponse(connectorRepo.save(entity));
    }

    @Transactional
    public void deleteConnector(UUID id) {
        String tenantId = TenantContext.getTenantId();
        ExternalConnector entity = connectorRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Connector not found: " + id));
        connectorRepo.delete(entity);
    }

    // ---- Auth Configs ----

    @Transactional(readOnly = true)
    public List<AuthConfigResponse> getAuthConfigs() {
        String tenantId = TenantContext.getTenantId();
        return authConfigRepo.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toAuthConfigResponse).toList();
    }

    @Transactional
    public AuthConfigResponse createAuthConfig(AuthConfigRequest request) {
        String tenantId = TenantContext.getTenantId();
        ApiAuthConfig entity = ApiAuthConfig.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .authType(request.getAuthType())
                .clientId(request.getClientId())
                .tokenUrl(request.getTokenUrl())
                .scopes(request.getScopes() != null ? toJson(request.getScopes()) : null)
                .active(request.isActive())
                .build();
        return toAuthConfigResponse(authConfigRepo.save(entity));
    }

    @Transactional
    public AuthConfigResponse updateAuthConfig(UUID id, AuthConfigRequest request) {
        String tenantId = TenantContext.getTenantId();
        ApiAuthConfig entity = authConfigRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Auth config not found: " + id));
        entity.setName(request.getName());
        entity.setAuthType(request.getAuthType());
        entity.setClientId(request.getClientId());
        entity.setTokenUrl(request.getTokenUrl());
        entity.setScopes(request.getScopes() != null ? toJson(request.getScopes()) : null);
        entity.setActive(request.isActive());
        return toAuthConfigResponse(authConfigRepo.save(entity));
    }

    @Transactional
    public void deleteAuthConfig(UUID id) {
        String tenantId = TenantContext.getTenantId();
        ApiAuthConfig entity = authConfigRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Auth config not found: " + id));
        authConfigRepo.delete(entity);
    }

    // ---- Health ----

    @Transactional(readOnly = true)
    public List<HealthResponse> getHealthRecords() {
        String tenantId = TenantContext.getTenantId();
        return healthRepo.findByTenantIdOrderByLastCheckedAtDesc(tenantId)
                .stream().map(this::toHealthResponse).toList();
    }

    // ---- Errors ----

    @Transactional(readOnly = true)
    public List<ErrorResponse> getErrors() {
        String tenantId = TenantContext.getTenantId();
        return errorRepo.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toErrorResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ErrorResponse> getErrorsByLevel(String level) {
        String tenantId = TenantContext.getTenantId();
        return errorRepo.findByTenantIdAndLevelOrderByCreatedAtDesc(tenantId, level)
                .stream().map(this::toErrorResponse).toList();
    }

    // ---- Mappers ----

    private ApiEndpointResponse toApiEndpointResponse(ApiEndpoint e) {
        return ApiEndpointResponse.builder()
                .id(e.getId()).name(e.getName()).path(e.getPath()).method(e.getMethod())
                .description(e.getDescription()).authRequired(e.isAuthRequired())
                .rateLimit(e.getRateLimit()).enabled(e.isEnabled()).version(e.getVersion())
                .totalCalls(e.getTotalCalls()).createdAt(e.getCreatedAt()).lastCalledAt(e.getLastCalledAt())
                .build();
    }

    private WebhookConfigResponse toWebhookResponse(WebhookConfig e) {
        return WebhookConfigResponse.builder()
                .id(e.getId()).name(e.getName()).url(e.getUrl())
                .events(parseJsonList(e.getEvents())).active(e.isActive())
                .retryCount(e.getRetryCount()).retryDelayMs(e.getRetryDelayMs())
                .successCount(e.getSuccessCount()).failureCount(e.getFailureCount())
                .createdAt(e.getCreatedAt()).lastTriggeredAt(e.getLastTriggeredAt())
                .build();
    }

    private IntegrationResponse toIntegrationResponse(ThirdPartyIntegration e) {
        return IntegrationResponse.builder()
                .id(e.getId()).name(e.getName()).provider(e.getProvider()).type(e.getType())
                .status(e.getStatus()).description(e.getDescription()).authType(e.getAuthType())
                .enabled(e.isEnabled()).createdAt(e.getCreatedAt()).lastSyncAt(e.getLastSyncAt())
                .build();
    }

    private DataSyncResponse toDataSyncResponse(DataSync e) {
        return DataSyncResponse.builder()
                .id(e.getId()).name(e.getName())
                .integrationId(e.getIntegration().getId())
                .integrationName(e.getIntegration().getName())
                .entityType(e.getEntityType()).direction(e.getDirection())
                .status(e.getStatus()).schedule(e.getSchedule())
                .lastRunAt(e.getLastRunAt()).lastRunDuration(e.getLastRunDuration())
                .recordsSynced(e.getRecordsSynced()).recordsFailed(e.getRecordsFailed())
                .fieldMapping(parseJsonMap(e.getFieldMapping())).enabled(e.isEnabled())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ConnectorResponse toConnectorResponse(ExternalConnector e) {
        return ConnectorResponse.builder()
                .id(e.getId()).name(e.getName()).type(e.getType()).host(e.getHost())
                .port(e.getPort()).databaseName(e.getDatabaseName()).baseUrl(e.getBaseUrl())
                .connectionString(e.getConnectionString()).status(e.getStatus())
                .enabled(e.isEnabled()).lastTestAt(e.getLastTestAt()).createdAt(e.getCreatedAt())
                .build();
    }

    private AuthConfigResponse toAuthConfigResponse(ApiAuthConfig e) {
        return AuthConfigResponse.builder()
                .id(e.getId()).name(e.getName()).authType(e.getAuthType())
                .clientId(e.getClientId()).tokenUrl(e.getTokenUrl())
                .scopes(parseJsonList(e.getScopes())).active(e.isActive())
                .expiresAt(e.getExpiresAt()).lastUsedAt(e.getLastUsedAt()).createdAt(e.getCreatedAt())
                .build();
    }

    private HealthResponse toHealthResponse(IntegrationHealthRecord e) {
        return HealthResponse.builder()
                .id(e.getId()).integrationId(e.getIntegration().getId())
                .integrationName(e.getIntegration().getName()).status(e.getStatus())
                .uptime(e.getUptime()).avgResponseMs(e.getAvgResponseMs())
                .successRate(e.getSuccessRate()).totalRequests(e.getTotalRequests())
                .lastCheckedAt(e.getLastCheckedAt()).alertsCount(e.getAlertsCount())
                .build();
    }

    private ErrorResponse toErrorResponse(IntegrationErrorRecord e) {
        return ErrorResponse.builder()
                .id(e.getId()).integrationId(e.getIntegration().getId())
                .integrationName(e.getIntegration().getName()).level(e.getLevel())
                .message(e.getMessage()).endpoint(e.getEndpoint()).httpStatus(e.getHttpStatus())
                .requestPayload(e.getRequestPayload()).resolvedAt(e.getResolvedAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize to JSON: {}", e.getMessage());
            return "[]";
        }
    }
}
