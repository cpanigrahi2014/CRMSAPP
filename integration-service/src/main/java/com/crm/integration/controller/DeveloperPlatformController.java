package com.crm.integration.controller;

import com.crm.common.dto.ApiResponse;
import com.crm.integration.dto.*;
import com.crm.integration.service.DeveloperPlatformService;
import com.crm.integration.service.WebhookDeliveryService;
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
@RequestMapping("/api/v1/developer")
@RequiredArgsConstructor
@Tag(name = "Developer Platform", description = "Developer tools, API keys, marketplace, widgets, and custom apps")
public class DeveloperPlatformController {

    private final DeveloperPlatformService platformService;
    private final WebhookDeliveryService webhookDeliveryService;

    // ---- API Keys ----

    @GetMapping("/api-keys")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all API keys")
    public ResponseEntity<ApiResponse<List<DeveloperApiKeyResponse>>> getApiKeys() {
        log.info("REST request to get API keys");
        return ResponseEntity.ok(ApiResponse.success(platformService.getApiKeys(), "API keys retrieved"));
    }

    @PostMapping("/api-keys")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate new API key")
    public ResponseEntity<ApiResponse<DeveloperApiKeyResponse>> createApiKey(
            @Valid @RequestBody DeveloperApiKeyRequest request) {
        log.info("REST request to create API key: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(platformService.createApiKey(request), "API key created"));
    }

    @PostMapping("/api-keys/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Revoke an API key")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(@PathVariable UUID id) {
        log.info("REST request to revoke API key: {}", id);
        platformService.revokeApiKey(id);
        return ResponseEntity.ok(ApiResponse.success(null, "API key revoked"));
    }

    @DeleteMapping("/api-keys/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete API key")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(@PathVariable UUID id) {
        log.info("REST request to delete API key: {}", id);
        platformService.deleteApiKey(id);
        return ResponseEntity.ok(ApiResponse.success(null, "API key deleted"));
    }

    // ---- Webhook Delivery Logs ----

    @GetMapping("/webhooks/deliveries")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all webhook delivery logs")
    public ResponseEntity<ApiResponse<List<WebhookDeliveryLogResponse>>> getDeliveryLogs() {
        log.info("REST request to get webhook delivery logs");
        return ResponseEntity.ok(ApiResponse.success(webhookDeliveryService.getDeliveryLogs(), "Delivery logs retrieved"));
    }

    @GetMapping("/webhooks/{webhookId}/deliveries")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get delivery logs for a specific webhook")
    public ResponseEntity<ApiResponse<List<WebhookDeliveryLogResponse>>> getDeliveryLogsByWebhook(
            @PathVariable UUID webhookId) {
        log.info("REST request to get delivery logs for webhook: {}", webhookId);
        return ResponseEntity.ok(ApiResponse.success(
                webhookDeliveryService.getDeliveryLogsByWebhook(webhookId), "Delivery logs retrieved"));
    }

    @PostMapping("/webhooks/{webhookId}/test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send test webhook delivery")
    public ResponseEntity<ApiResponse<WebhookDeliveryLogResponse>> testWebhook(@PathVariable UUID webhookId) {
        log.info("REST request to test webhook: {}", webhookId);
        return ResponseEntity.ok(ApiResponse.success(
                webhookDeliveryService.testWebhook(webhookId), "Test webhook sent"));
    }

    // ---- Marketplace ----

    @GetMapping("/marketplace")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Browse marketplace plugins")
    public ResponseEntity<ApiResponse<List<MarketplacePluginResponse>>> getMarketplacePlugins() {
        log.info("REST request to browse marketplace");
        return ResponseEntity.ok(ApiResponse.success(platformService.getPublishedPlugins(), "Marketplace plugins retrieved"));
    }

    @GetMapping("/marketplace/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Browse marketplace by category")
    public ResponseEntity<ApiResponse<List<MarketplacePluginResponse>>> getPluginsByCategory(
            @PathVariable String category) {
        log.info("REST request to browse marketplace category: {}", category);
        return ResponseEntity.ok(ApiResponse.success(
                platformService.getPluginsByCategory(category), "Plugins retrieved"));
    }

    @PostMapping("/marketplace")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create marketplace plugin")
    public ResponseEntity<ApiResponse<MarketplacePluginResponse>> createPlugin(
            @Valid @RequestBody MarketplacePluginRequest request) {
        log.info("REST request to create plugin: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(platformService.createPlugin(request), "Plugin created"));
    }

    @PostMapping("/marketplace/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish marketplace plugin")
    public ResponseEntity<ApiResponse<MarketplacePluginResponse>> publishPlugin(@PathVariable UUID id) {
        log.info("REST request to publish plugin: {}", id);
        return ResponseEntity.ok(ApiResponse.success(platformService.publishPlugin(id), "Plugin published"));
    }

    // ---- Plugin Installations ----

    @GetMapping("/plugins/installed")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get installed plugins")
    public ResponseEntity<ApiResponse<List<PluginInstallationResponse>>> getInstalledPlugins() {
        log.info("REST request to get installed plugins");
        return ResponseEntity.ok(ApiResponse.success(platformService.getInstalledPlugins(), "Installed plugins retrieved"));
    }

    @PostMapping("/plugins/{pluginId}/install")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Install a plugin")
    public ResponseEntity<ApiResponse<PluginInstallationResponse>> installPlugin(
            @PathVariable UUID pluginId, @RequestBody(required = false) PluginInstallationRequest request) {
        log.info("REST request to install plugin: {}", pluginId);
        if (request == null) request = new PluginInstallationRequest();
        return ResponseEntity.ok(ApiResponse.success(
                platformService.installPlugin(pluginId, request), "Plugin installed"));
    }

    @PostMapping("/plugins/{pluginId}/uninstall")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Uninstall a plugin")
    public ResponseEntity<ApiResponse<Void>> uninstallPlugin(@PathVariable UUID pluginId) {
        log.info("REST request to uninstall plugin: {}", pluginId);
        platformService.uninstallPlugin(pluginId);
        return ResponseEntity.ok(ApiResponse.success(null, "Plugin uninstalled"));
    }

    // ---- Embeddable Widgets ----

    @GetMapping("/widgets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all embeddable widgets")
    public ResponseEntity<ApiResponse<List<EmbeddableWidgetResponse>>> getWidgets() {
        log.info("REST request to get widgets");
        return ResponseEntity.ok(ApiResponse.success(platformService.getWidgets(), "Widgets retrieved"));
    }

    @PostMapping("/widgets")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create embeddable widget")
    public ResponseEntity<ApiResponse<EmbeddableWidgetResponse>> createWidget(
            @Valid @RequestBody EmbeddableWidgetRequest request) {
        log.info("REST request to create widget: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(platformService.createWidget(request), "Widget created"));
    }

    @PutMapping("/widgets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update embeddable widget")
    public ResponseEntity<ApiResponse<EmbeddableWidgetResponse>> updateWidget(
            @PathVariable UUID id, @Valid @RequestBody EmbeddableWidgetRequest request) {
        log.info("REST request to update widget: {}", id);
        return ResponseEntity.ok(ApiResponse.success(platformService.updateWidget(id, request), "Widget updated"));
    }

    @DeleteMapping("/widgets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete embeddable widget")
    public ResponseEntity<ApiResponse<Void>> deleteWidget(@PathVariable UUID id) {
        log.info("REST request to delete widget: {}", id);
        platformService.deleteWidget(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Widget deleted"));
    }

    // ---- Custom Apps ----

    @GetMapping("/apps")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all custom apps")
    public ResponseEntity<ApiResponse<List<CustomAppResponse>>> getCustomApps() {
        log.info("REST request to get custom apps");
        return ResponseEntity.ok(ApiResponse.success(platformService.getCustomApps(), "Custom apps retrieved"));
    }

    @PostMapping("/apps")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create custom app")
    public ResponseEntity<ApiResponse<CustomAppResponse>> createCustomApp(
            @Valid @RequestBody CustomAppRequest request) {
        log.info("REST request to create custom app: {}", request.getName());
        return ResponseEntity.ok(ApiResponse.success(platformService.createCustomApp(request), "Custom app created"));
    }

    @PutMapping("/apps/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update custom app")
    public ResponseEntity<ApiResponse<CustomAppResponse>> updateCustomApp(
            @PathVariable UUID id, @Valid @RequestBody CustomAppRequest request) {
        log.info("REST request to update custom app: {}", id);
        return ResponseEntity.ok(ApiResponse.success(platformService.updateCustomApp(id, request), "Custom app updated"));
    }

    @PostMapping("/apps/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish custom app")
    public ResponseEntity<ApiResponse<CustomAppResponse>> publishCustomApp(@PathVariable UUID id) {
        log.info("REST request to publish custom app: {}", id);
        return ResponseEntity.ok(ApiResponse.success(platformService.publishCustomApp(id), "Custom app published"));
    }

    @DeleteMapping("/apps/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete custom app")
    public ResponseEntity<ApiResponse<Void>> deleteCustomApp(@PathVariable UUID id) {
        log.info("REST request to delete custom app: {}", id);
        platformService.deleteCustomApp(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Custom app deleted"));
    }
}
