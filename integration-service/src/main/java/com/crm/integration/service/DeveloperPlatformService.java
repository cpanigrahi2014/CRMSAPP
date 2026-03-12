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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeveloperPlatformService {

    private final DeveloperApiKeyRepository apiKeyRepo;
    private final MarketplacePluginRepository pluginRepo;
    private final PluginInstallationRepository installationRepo;
    private final EmbeddableWidgetRepository widgetRepo;
    private final CustomAppRepository customAppRepo;
    private final ObjectMapper objectMapper;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ---- API Keys ----

    @Transactional(readOnly = true)
    public List<DeveloperApiKeyResponse> getApiKeys() {
        String tenantId = TenantContext.getTenantId();
        return apiKeyRepo.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toApiKeyResponse).toList();
    }

    @Transactional
    public DeveloperApiKeyResponse createApiKey(DeveloperApiKeyRequest request) {
        String tenantId = TenantContext.getTenantId();
        String rawKey = generateApiKey();
        String prefix = rawKey.substring(0, 8);
        String hash = sha256(rawKey);

        DeveloperApiKey entity = DeveloperApiKey.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .keyPrefix(prefix)
                .keyHash(hash)
                .scopes(toJson(request.getScopes() != null ? request.getScopes() : List.of("read", "write")))
                .rateLimit(request.getRateLimit() > 0 ? request.getRateLimit() : 1000)
                .active(true)
                .expiresAt(request.getExpiresAt())
                .createdBy(tenantId)
                .build();
        entity = apiKeyRepo.save(entity);

        DeveloperApiKeyResponse response = toApiKeyResponse(entity);
        response.setRawKey(rawKey); // Only returned on creation
        return response;
    }

    @Transactional
    public void revokeApiKey(UUID id) {
        String tenantId = TenantContext.getTenantId();
        DeveloperApiKey entity = apiKeyRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found: " + id));
        entity.setActive(false);
        apiKeyRepo.save(entity);
    }

    @Transactional
    public void deleteApiKey(UUID id) {
        String tenantId = TenantContext.getTenantId();
        DeveloperApiKey entity = apiKeyRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found: " + id));
        apiKeyRepo.delete(entity);
    }

    // ---- Marketplace Plugins ----

    @Transactional(readOnly = true)
    public List<MarketplacePluginResponse> getPublishedPlugins() {
        String tenantId = TenantContext.getTenantId();
        List<MarketplacePlugin> plugins = pluginRepo.findByStatusOrderByInstallCountDesc("PUBLISHED");
        Set<UUID> installedPluginIds = getInstalledPluginIds(tenantId);
        return plugins.stream().map(p -> toPluginResponse(p, installedPluginIds.contains(p.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<MarketplacePluginResponse> getPluginsByCategory(String category) {
        String tenantId = TenantContext.getTenantId();
        List<MarketplacePlugin> plugins = pluginRepo.findByCategoryAndStatusOrderByInstallCountDesc(category, "PUBLISHED");
        Set<UUID> installedPluginIds = getInstalledPluginIds(tenantId);
        return plugins.stream().map(p -> toPluginResponse(p, installedPluginIds.contains(p.getId()))).toList();
    }

    @Transactional
    public MarketplacePluginResponse createPlugin(MarketplacePluginRequest request) {
        String slug = request.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        MarketplacePlugin entity = MarketplacePlugin.builder()
                .tenantId(TenantContext.getTenantId())
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .longDescription(request.getLongDescription())
                .category(request.getCategory())
                .author(request.getAuthor())
                .version(request.getVersion() != null ? request.getVersion() : "1.0.0")
                .iconUrl(request.getIconUrl())
                .screenshots(toJson(request.getScreenshots()))
                .downloadUrl(request.getDownloadUrl())
                .documentationUrl(request.getDocumentationUrl())
                .pricing(request.getPricing() != null ? request.getPricing() : "FREE")
                .priceAmount(request.getPriceAmount())
                .requiredScopes(toJson(request.getRequiredScopes()))
                .configSchema(toJson(request.getConfigSchema()))
                .status("DRAFT")
                .build();
        return toPluginResponse(pluginRepo.save(entity), false);
    }

    @Transactional
    public MarketplacePluginResponse publishPlugin(UUID id) {
        MarketplacePlugin entity = pluginRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plugin not found: " + id));
        entity.setStatus("PUBLISHED");
        return toPluginResponse(pluginRepo.save(entity), false);
    }

    // ---- Plugin Installations ----

    @Transactional(readOnly = true)
    public List<PluginInstallationResponse> getInstalledPlugins() {
        String tenantId = TenantContext.getTenantId();
        return installationRepo.findByTenantIdAndStatus(tenantId, "ACTIVE")
                .stream().map(this::toInstallationResponse).toList();
    }

    @Transactional
    public PluginInstallationResponse installPlugin(UUID pluginId, PluginInstallationRequest request) {
        String tenantId = TenantContext.getTenantId();
        MarketplacePlugin plugin = pluginRepo.findById(pluginId)
                .orElseThrow(() -> new ResourceNotFoundException("Plugin not found: " + pluginId));

        Optional<PluginInstallation> existing = installationRepo.findByPlugin_IdAndTenantId(pluginId, tenantId);
        if (existing.isPresent()) {
            PluginInstallation inst = existing.get();
            inst.setStatus("ACTIVE");
            inst.setConfig(request.getConfig() != null ? toJson(request.getConfig()) : inst.getConfig());
            return toInstallationResponse(installationRepo.save(inst));
        }

        PluginInstallation entity = PluginInstallation.builder()
                .tenantId(tenantId)
                .plugin(plugin)
                .status("ACTIVE")
                .config(request.getConfig() != null ? toJson(request.getConfig()) : "{}")
                .installedBy(tenantId)
                .build();
        entity = installationRepo.save(entity);

        plugin.setInstallCount(plugin.getInstallCount() + 1);
        pluginRepo.save(plugin);

        return toInstallationResponse(entity);
    }

    @Transactional
    public void uninstallPlugin(UUID pluginId) {
        String tenantId = TenantContext.getTenantId();
        PluginInstallation inst = installationRepo.findByPlugin_IdAndTenantId(pluginId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Plugin installation not found"));
        inst.setStatus("UNINSTALLED");
        installationRepo.save(inst);
    }

    // ---- Embeddable Widgets ----

    @Transactional(readOnly = true)
    public List<EmbeddableWidgetResponse> getWidgets() {
        String tenantId = TenantContext.getTenantId();
        return widgetRepo.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toWidgetResponse).toList();
    }

    @Transactional
    public EmbeddableWidgetResponse createWidget(EmbeddableWidgetRequest request) {
        String tenantId = TenantContext.getTenantId();
        String embedToken = UUID.randomUUID().toString().replace("-", "");

        EmbeddableWidget entity = EmbeddableWidget.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .widgetType(request.getWidgetType() != null ? request.getWidgetType() : "CHART")
                .description(request.getDescription())
                .config(toJson(request.getConfig()))
                .embedToken(embedToken)
                .allowedDomains(toJson(request.getAllowedDomains()))
                .active(true)
                .build();
        return toWidgetResponse(widgetRepo.save(entity));
    }

    @Transactional
    public EmbeddableWidgetResponse updateWidget(UUID id, EmbeddableWidgetRequest request) {
        String tenantId = TenantContext.getTenantId();
        EmbeddableWidget entity = widgetRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Widget not found: " + id));
        entity.setName(request.getName());
        if (request.getWidgetType() != null) entity.setWidgetType(request.getWidgetType());
        entity.setDescription(request.getDescription());
        entity.setConfig(toJson(request.getConfig()));
        entity.setAllowedDomains(toJson(request.getAllowedDomains()));
        return toWidgetResponse(widgetRepo.save(entity));
    }

    @Transactional
    public void deleteWidget(UUID id) {
        String tenantId = TenantContext.getTenantId();
        EmbeddableWidget entity = widgetRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Widget not found: " + id));
        widgetRepo.delete(entity);
    }

    // ---- Custom Apps ----

    @Transactional(readOnly = true)
    public List<CustomAppResponse> getCustomApps() {
        String tenantId = TenantContext.getTenantId();
        return customAppRepo.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toCustomAppResponse).toList();
    }

    @Transactional
    public CustomAppResponse createCustomApp(CustomAppRequest request) {
        String tenantId = TenantContext.getTenantId();
        String slug = request.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");

        CustomApp entity = CustomApp.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .appType(request.getAppType() != null ? request.getAppType() : "DASHBOARD")
                .status("DRAFT")
                .layout(toJson(request.getLayout()))
                .dataSource(toJson(request.getDataSource()))
                .style(toJson(request.getStyle()))
                .createdBy(tenantId)
                .build();
        return toCustomAppResponse(customAppRepo.save(entity));
    }

    @Transactional
    public CustomAppResponse updateCustomApp(UUID id, CustomAppRequest request) {
        String tenantId = TenantContext.getTenantId();
        CustomApp entity = customAppRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom app not found: " + id));
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getAppType() != null) entity.setAppType(request.getAppType());
        entity.setLayout(toJson(request.getLayout()));
        entity.setDataSource(toJson(request.getDataSource()));
        entity.setStyle(toJson(request.getStyle()));
        return toCustomAppResponse(customAppRepo.save(entity));
    }

    @Transactional
    public CustomAppResponse publishCustomApp(UUID id) {
        String tenantId = TenantContext.getTenantId();
        CustomApp entity = customAppRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom app not found: " + id));
        entity.setStatus("PUBLISHED");
        entity.setPublishedVersion(entity.getPublishedVersion() != null
                ? incrementVersion(entity.getPublishedVersion()) : "1.0.0");
        return toCustomAppResponse(customAppRepo.save(entity));
    }

    @Transactional
    public void deleteCustomApp(UUID id) {
        String tenantId = TenantContext.getTenantId();
        CustomApp entity = customAppRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom app not found: " + id));
        customAppRepo.delete(entity);
    }

    // ---- Helpers ----

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "crm_" + HexFormat.of().formatHex(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }

    private String incrementVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length == 3) {
            int patch = Integer.parseInt(parts[2]) + 1;
            return parts[0] + "." + parts[1] + "." + patch;
        }
        return "1.0.0";
    }

    private Set<UUID> getInstalledPluginIds(String tenantId) {
        return new HashSet<>(installationRepo.findByTenantIdAndStatus(tenantId, "ACTIVE")
                .stream().map(i -> i.getPlugin().getId()).toList());
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ---- Mappers ----

    private DeveloperApiKeyResponse toApiKeyResponse(DeveloperApiKey e) {
        return DeveloperApiKeyResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .keyPrefix(e.getKeyPrefix())
                .scopes(parseJsonList(e.getScopes()))
                .rateLimit(e.getRateLimit())
                .callsToday(e.getCallsToday())
                .totalCalls(e.getTotalCalls())
                .active(e.isActive())
                .expiresAt(e.getExpiresAt())
                .lastUsedAt(e.getLastUsedAt())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private MarketplacePluginResponse toPluginResponse(MarketplacePlugin e, boolean installed) {
        return MarketplacePluginResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .slug(e.getSlug())
                .description(e.getDescription())
                .longDescription(e.getLongDescription())
                .category(e.getCategory())
                .author(e.getAuthor())
                .version(e.getVersion())
                .iconUrl(e.getIconUrl())
                .screenshots(parseJsonList(e.getScreenshots()))
                .downloadUrl(e.getDownloadUrl())
                .documentationUrl(e.getDocumentationUrl())
                .status(e.getStatus())
                .pricing(e.getPricing())
                .priceAmount(e.getPriceAmount())
                .installCount(e.getInstallCount())
                .rating(e.getRating())
                .ratingCount(e.getRatingCount())
                .requiredScopes(parseJsonList(e.getRequiredScopes()))
                .configSchema(parseJsonMap(e.getConfigSchema()))
                .isVerified(e.isVerified())
                .installed(installed)
                .createdAt(e.getCreatedAt())
                .build();
    }

    private PluginInstallationResponse toInstallationResponse(PluginInstallation e) {
        return PluginInstallationResponse.builder()
                .id(e.getId())
                .pluginId(e.getPlugin().getId())
                .pluginName(e.getPlugin().getName())
                .pluginSlug(e.getPlugin().getSlug())
                .status(e.getStatus())
                .config(parseJsonMap(e.getConfig()))
                .installedBy(e.getInstalledBy())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private EmbeddableWidgetResponse toWidgetResponse(EmbeddableWidget e) {
        String embedCode = String.format(
                "<iframe src=\"/embed/widget/%s\" width=\"100%%\" height=\"400\" frameborder=\"0\"></iframe>",
                e.getEmbedToken());
        return EmbeddableWidgetResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .widgetType(e.getWidgetType())
                .description(e.getDescription())
                .config(parseJsonMap(e.getConfig()))
                .embedToken(e.getEmbedToken())
                .embedCode(embedCode)
                .allowedDomains(parseJsonList(e.getAllowedDomains()))
                .active(e.isActive())
                .viewCount(e.getViewCount())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private CustomAppResponse toCustomAppResponse(CustomApp e) {
        return CustomAppResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .slug(e.getSlug())
                .description(e.getDescription())
                .appType(e.getAppType())
                .status(e.getStatus())
                .layout(parseJsonMap(e.getLayout()))
                .dataSource(parseJsonMap(e.getDataSource()))
                .style(parseJsonMap(e.getStyle()))
                .publishedVersion(e.getPublishedVersion())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
