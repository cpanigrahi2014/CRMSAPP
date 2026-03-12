package com.crm.integration.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.integration.dto.*;
import com.crm.integration.service.IntegrationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Tag(name = "Integrations", description = "Integration management APIs")
public class IntegrationController {

    private final IntegrationManagementService service;

    // ---- API Endpoints ----

    @GetMapping("/apis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all API endpoints")
    public ResponseEntity<ApiResponse<List<ApiEndpointResponse>>> getApiEndpoints() {
        log.info("REST request to get API endpoints");
        return ResponseEntity.ok(ApiResponse.success(service.getApiEndpoints(), "API endpoints retrieved"));
    }

    @PostMapping("/apis")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create API endpoint")
    public ResponseEntity<ApiResponse<ApiEndpointResponse>> createApiEndpoint(
            @Valid @RequestBody ApiEndpointRequest request) {
        log.info("REST request to create API endpoint: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(service.createApiEndpoint(request), "API endpoint created"));
    }

    @PutMapping("/apis/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update API endpoint")
    public ResponseEntity<ApiResponse<ApiEndpointResponse>> updateApiEndpoint(
            @PathVariable UUID id, @Valid @RequestBody ApiEndpointRequest request) {
        log.info("REST request to update API endpoint: {}", id);
        return ResponseEntity.ok(ApiResponse.success(service.updateApiEndpoint(id, request), "API endpoint updated"));
    }

    @DeleteMapping("/apis/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete API endpoint")
    public ResponseEntity<ApiResponse<Void>> deleteApiEndpoint(@PathVariable UUID id) {
        log.info("REST request to delete API endpoint: {}", id);
        service.deleteApiEndpoint(id);
        return ResponseEntity.ok(ApiResponse.success(null, "API endpoint deleted"));
    }

    // ---- Webhooks ----

    @GetMapping("/webhooks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all webhook configurations")
    public ResponseEntity<ApiResponse<List<WebhookConfigResponse>>> getWebhooks() {
        log.info("REST request to get webhooks");
        return ResponseEntity.ok(ApiResponse.success(service.getWebhooks(), "Webhooks retrieved"));
    }

    @PostMapping("/webhooks")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create webhook")
    public ResponseEntity<ApiResponse<WebhookConfigResponse>> createWebhook(
            @Valid @RequestBody WebhookConfigRequest request) {
        log.info("REST request to create webhook: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(service.createWebhook(request), "Webhook created"));
    }

    @PutMapping("/webhooks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update webhook")
    public ResponseEntity<ApiResponse<WebhookConfigResponse>> updateWebhook(
            @PathVariable UUID id, @Valid @RequestBody WebhookConfigRequest request) {
        log.info("REST request to update webhook: {}", id);
        return ResponseEntity.ok(ApiResponse.success(service.updateWebhook(id, request), "Webhook updated"));
    }

    @DeleteMapping("/webhooks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete webhook")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(@PathVariable UUID id) {
        log.info("REST request to delete webhook: {}", id);
        service.deleteWebhook(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook deleted"));
    }

    // ---- Third-Party Integrations ----

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all third-party integrations")
    public ResponseEntity<ApiResponse<List<IntegrationResponse>>> getIntegrations() {
        log.info("REST request to get integrations");
        return ResponseEntity.ok(ApiResponse.success(service.getIntegrations(), "Integrations retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create integration")
    public ResponseEntity<ApiResponse<IntegrationResponse>> createIntegration(
            @Valid @RequestBody IntegrationRequest request) {
        log.info("REST request to create integration: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(service.createIntegration(request), "Integration created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update integration")
    public ResponseEntity<ApiResponse<IntegrationResponse>> updateIntegration(
            @PathVariable UUID id, @Valid @RequestBody IntegrationRequest request) {
        log.info("REST request to update integration: {}", id);
        return ResponseEntity.ok(ApiResponse.success(service.updateIntegration(id, request), "Integration updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete integration")
    public ResponseEntity<ApiResponse<Void>> deleteIntegration(@PathVariable UUID id) {
        log.info("REST request to delete integration: {}", id);
        service.deleteIntegration(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Integration deleted"));
    }

    // ---- Data Syncs ----

    @GetMapping("/syncs")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all data sync configurations")
    public ResponseEntity<ApiResponse<List<DataSyncResponse>>> getDataSyncs() {
        log.info("REST request to get data syncs");
        return ResponseEntity.ok(ApiResponse.success(service.getDataSyncs(), "Data syncs retrieved"));
    }

    @PostMapping("/syncs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create data sync")
    public ResponseEntity<ApiResponse<DataSyncResponse>> createDataSync(
            @Valid @RequestBody DataSyncRequest request) {
        log.info("REST request to create data sync: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(service.createDataSync(request), "Data sync created"));
    }

    @PutMapping("/syncs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update data sync")
    public ResponseEntity<ApiResponse<DataSyncResponse>> updateDataSync(
            @PathVariable UUID id, @Valid @RequestBody DataSyncRequest request) {
        log.info("REST request to update data sync: {}", id);
        return ResponseEntity.ok(ApiResponse.success(service.updateDataSync(id, request), "Data sync updated"));
    }

    @DeleteMapping("/syncs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete data sync")
    public ResponseEntity<ApiResponse<Void>> deleteDataSync(@PathVariable UUID id) {
        log.info("REST request to delete data sync: {}", id);
        service.deleteDataSync(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Data sync deleted"));
    }

    // ---- External Connectors ----

    @GetMapping("/connectors")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all external connectors")
    public ResponseEntity<ApiResponse<List<ConnectorResponse>>> getConnectors() {
        log.info("REST request to get connectors");
        return ResponseEntity.ok(ApiResponse.success(service.getConnectors(), "Connectors retrieved"));
    }

    @PostMapping("/connectors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create connector")
    public ResponseEntity<ApiResponse<ConnectorResponse>> createConnector(
            @Valid @RequestBody ConnectorRequest request) {
        log.info("REST request to create connector: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(service.createConnector(request), "Connector created"));
    }

    @PutMapping("/connectors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update connector")
    public ResponseEntity<ApiResponse<ConnectorResponse>> updateConnector(
            @PathVariable UUID id, @Valid @RequestBody ConnectorRequest request) {
        log.info("REST request to update connector: {}", id);
        return ResponseEntity.ok(ApiResponse.success(service.updateConnector(id, request), "Connector updated"));
    }

    @DeleteMapping("/connectors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete connector")
    public ResponseEntity<ApiResponse<Void>> deleteConnector(@PathVariable UUID id) {
        log.info("REST request to delete connector: {}", id);
        service.deleteConnector(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Connector deleted"));
    }

    // ---- Auth Configs ----

    @GetMapping("/auth-configs")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all API auth configurations")
    public ResponseEntity<ApiResponse<List<AuthConfigResponse>>> getAuthConfigs() {
        log.info("REST request to get auth configs");
        return ResponseEntity.ok(ApiResponse.success(service.getAuthConfigs(), "Auth configs retrieved"));
    }

    @PostMapping("/auth-configs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create auth config")
    public ResponseEntity<ApiResponse<AuthConfigResponse>> createAuthConfig(
            @Valid @RequestBody AuthConfigRequest request) {
        log.info("REST request to create auth config: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(service.createAuthConfig(request), "Auth config created"));
    }

    @PutMapping("/auth-configs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update auth config")
    public ResponseEntity<ApiResponse<AuthConfigResponse>> updateAuthConfig(
            @PathVariable UUID id, @Valid @RequestBody AuthConfigRequest request) {
        log.info("REST request to update auth config: {}", id);
        return ResponseEntity.ok(ApiResponse.success(service.updateAuthConfig(id, request), "Auth config updated"));
    }

    @DeleteMapping("/auth-configs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete auth config")
    public ResponseEntity<ApiResponse<Void>> deleteAuthConfig(@PathVariable UUID id) {
        log.info("REST request to delete auth config: {}", id);
        service.deleteAuthConfig(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Auth config deleted"));
    }

    // ---- Health & Errors ----

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get integration health records")
    public ResponseEntity<ApiResponse<List<HealthResponse>>> getHealth() {
        log.info("REST request to get integration health");
        return ResponseEntity.ok(ApiResponse.success(service.getHealthRecords(), "Health records retrieved"));
    }

    @GetMapping("/errors")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get integration error logs")
    public ResponseEntity<ApiResponse<List<ErrorResponse>>> getErrors() {
        log.info("REST request to get integration errors");
        return ResponseEntity.ok(ApiResponse.success(service.getErrors(), "Error logs retrieved"));
    }

    @GetMapping("/errors/level/{level}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get integration errors by severity level")
    public ResponseEntity<ApiResponse<List<ErrorResponse>>> getErrorsByLevel(@PathVariable String level) {
        log.info("REST request to get integration errors by level: {}", level);
        return ResponseEntity.ok(ApiResponse.success(service.getErrorsByLevel(level), "Error logs retrieved"));
    }
}
