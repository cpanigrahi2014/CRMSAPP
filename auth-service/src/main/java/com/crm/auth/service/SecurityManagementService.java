package com.crm.auth.service;

import com.crm.auth.dto.*;
import com.crm.auth.entity.*;
import com.crm.auth.repository.*;
import com.crm.common.exception.BadRequestException;
import com.crm.common.exception.DuplicateResourceException;
import com.crm.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final FieldSecurityRuleRepository fieldSecurityRuleRepository;
    private final SsoProviderRepository ssoProviderRepository;
    private final MfaConfigRepository mfaConfigRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    // ── Roles ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles(String tenantId) {
        return roleRepository.findAll().stream()
                .filter(r -> r.getTenantId().equals(tenantId))
                .map(r -> RoleResponse.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .description(r.getDescription())
                        .permissions(getPermissionNamesForRole(r.getName(), tenantId))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse createRole(RoleRequest request, String tenantId) {
        if (roleRepository.findByNameAndTenantId(request.getName(), tenantId).isPresent()) {
            throw new DuplicateResourceException("Role with name '" + request.getName() + "' already exists");
        }
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tenantId(tenantId)
                .build();
        role = roleRepository.save(role);
        log.info("Created role: {} for tenant: {}", request.getName(), tenantId);
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(List.of())
                .build();
    }

    @Transactional
    public void deleteRole(UUID roleId, String tenantId) {
        Role role = roleRepository.findById(roleId)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        roleRepository.delete(role);
        log.info("Deleted role: {} for tenant: {}", role.getName(), tenantId);
    }

    private List<String> getPermissionNamesForRole(String roleName, String tenantId) {
        return permissionRepository.findByTenantIdOrderByResourceAscNameAsc(tenantId).stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
    }

    // ── Permissions ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions(String tenantId) {
        return permissionRepository.findByTenantIdOrderByResourceAscNameAsc(tenantId).stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PermissionResponse createPermission(PermissionRequest request, String tenantId) {
        if (permissionRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new DuplicateResourceException("Permission with name '" + request.getName() + "' already exists");
        }
        Permission perm = Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .resource(request.getResource())
                .actions(request.getActions())
                .tenantId(tenantId)
                .build();
        perm = permissionRepository.save(perm);
        log.info("Created permission: {} for tenant: {}", request.getName(), tenantId);
        return toPermissionResponse(perm);
    }

    @Transactional
    public PermissionResponse updatePermission(UUID id, PermissionRequest request, String tenantId) {
        Permission perm = permissionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
        perm.setName(request.getName());
        perm.setDescription(request.getDescription());
        perm.setResource(request.getResource());
        perm.setActions(request.getActions());
        perm = permissionRepository.save(perm);
        return toPermissionResponse(perm);
    }

    @Transactional
    public void deletePermission(UUID id, String tenantId) {
        Permission perm = permissionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
        permissionRepository.delete(perm);
    }

    private PermissionResponse toPermissionResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .resource(p.getResource()).actions(p.getActions()).createdAt(p.getCreatedAt())
                .build();
    }

    // ── Field Security ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FieldSecurityResponse> getFieldSecurityRules(String tenantId) {
        return fieldSecurityRuleRepository.findByTenantIdOrderByEntityTypeAscFieldNameAsc(tenantId)
                .stream().map(this::toFieldSecurityResponse).collect(Collectors.toList());
    }

    @Transactional
    public FieldSecurityResponse createFieldSecurityRule(FieldSecurityRequest request, String tenantId) {
        FieldSecurityRule rule = FieldSecurityRule.builder()
                .entityType(request.getEntityType())
                .fieldName(request.getFieldName())
                .roleName(request.getRoleName())
                .accessLevel(request.getAccessLevel() != null ? request.getAccessLevel() : "READ_WRITE")
                .tenantId(tenantId)
                .build();
        rule = fieldSecurityRuleRepository.save(rule);
        return toFieldSecurityResponse(rule);
    }

    @Transactional
    public FieldSecurityResponse updateFieldSecurityRule(UUID id, FieldSecurityRequest request, String tenantId) {
        FieldSecurityRule rule = fieldSecurityRuleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("FieldSecurityRule", "id", id));
        rule.setEntityType(request.getEntityType());
        rule.setFieldName(request.getFieldName());
        rule.setRoleName(request.getRoleName());
        rule.setAccessLevel(request.getAccessLevel() != null ? request.getAccessLevel() : rule.getAccessLevel());
        rule = fieldSecurityRuleRepository.save(rule);
        return toFieldSecurityResponse(rule);
    }

    @Transactional
    public void deleteFieldSecurityRule(UUID id, String tenantId) {
        FieldSecurityRule rule = fieldSecurityRuleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("FieldSecurityRule", "id", id));
        fieldSecurityRuleRepository.delete(rule);
    }

    private FieldSecurityResponse toFieldSecurityResponse(FieldSecurityRule r) {
        return FieldSecurityResponse.builder()
                .id(r.getId()).entityType(r.getEntityType()).fieldName(r.getFieldName())
                .roleName(r.getRoleName()).accessLevel(r.getAccessLevel()).createdAt(r.getCreatedAt())
                .build();
    }

    // ── SSO Providers ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SsoProviderResponse> getSsoProviders(String tenantId) {
        return ssoProviderRepository.findByTenantIdOrderByNameAsc(tenantId)
                .stream().map(this::toSsoResponse).collect(Collectors.toList());
    }

    @Transactional
    public SsoProviderResponse createSsoProvider(SsoProviderRequest request, String tenantId) {
        if (ssoProviderRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new DuplicateResourceException("SsoProvider with name '" + request.getName() + "' already exists");
        }
        SsoProvider provider = SsoProvider.builder()
                .name(request.getName())
                .providerType(request.getProviderType())
                .clientId(request.getClientId())
                .issuerUrl(request.getIssuerUrl())
                .metadataUrl(request.getMetadataUrl())
                .enabled(request.isEnabled())
                .autoProvision(request.isAutoProvision())
                .defaultRole(request.getDefaultRole() != null ? request.getDefaultRole() : "USER")
                .tenantId(tenantId)
                .build();
        provider = ssoProviderRepository.save(provider);
        return toSsoResponse(provider);
    }

    @Transactional
    public SsoProviderResponse updateSsoProvider(UUID id, SsoProviderRequest request, String tenantId) {
        SsoProvider provider = ssoProviderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SsoProvider", "id", id));
        provider.setName(request.getName());
        provider.setProviderType(request.getProviderType());
        provider.setClientId(request.getClientId());
        provider.setIssuerUrl(request.getIssuerUrl());
        provider.setMetadataUrl(request.getMetadataUrl());
        provider.setEnabled(request.isEnabled());
        provider.setAutoProvision(request.isAutoProvision());
        provider.setDefaultRole(request.getDefaultRole());
        provider = ssoProviderRepository.save(provider);
        return toSsoResponse(provider);
    }

    @Transactional
    public void deleteSsoProvider(UUID id, String tenantId) {
        SsoProvider provider = ssoProviderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SsoProvider", "id", id));
        ssoProviderRepository.delete(provider);
    }

    private SsoProviderResponse toSsoResponse(SsoProvider s) {
        return SsoProviderResponse.builder()
                .id(s.getId()).name(s.getName()).providerType(s.getProviderType())
                .clientId(s.getClientId()).issuerUrl(s.getIssuerUrl()).metadataUrl(s.getMetadataUrl())
                .enabled(s.isEnabled()).autoProvision(s.isAutoProvision()).defaultRole(s.getDefaultRole())
                .createdAt(s.getCreatedAt())
                .build();
    }

    // ── MFA ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MfaConfigResponse> getMfaConfigs(String tenantId) {
        return mfaConfigRepository.findAll().stream()
                .filter(m -> m.getTenantId().equals(tenantId))
                .map(this::toMfaResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MfaConfigResponse setupMfa(MfaConfigRequest request, String tenantId) {
        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(request.getUserId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        String mfaType = request.getMfaType() != null ? request.getMfaType() : "TOTP";

        MfaConfig config = mfaConfigRepository
                .findByUserIdAndMfaTypeAndTenantId(request.getUserId(), mfaType, tenantId)
                .orElseGet(() -> MfaConfig.builder()
                        .user(user)
                        .mfaType(mfaType)
                        .tenantId(tenantId)
                        .build());

        config.setEnabled(request.isEnabled());
        if (config.getSecretKey() == null) {
            config.setSecretKey(generateSecret());
        }
        if (config.getBackupCodes() == null && request.isEnabled()) {
            config.setBackupCodes(generateBackupCodes());
        }
        config = mfaConfigRepository.save(config);
        return toMfaResponse(config);
    }

    @Transactional
    public void deleteMfaConfig(UUID id, String tenantId) {
        MfaConfig config = mfaConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MfaConfig", "id", id));
        mfaConfigRepository.delete(config);
    }

    private MfaConfigResponse toMfaResponse(MfaConfig m) {
        return MfaConfigResponse.builder()
                .id(m.getId())
                .userId(m.getUser().getId())
                .userEmail(m.getUser().getEmail())
                .mfaType(m.getMfaType())
                .enabled(m.isEnabled())
                .hasBackupCodes(m.getBackupCodes() != null && !m.getBackupCodes().isEmpty())
                .lastUsedAt(m.getLastUsedAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        return IntStream.range(0, 10)
                .mapToObj(i -> String.format("%08d", random.nextInt(100_000_000)))
                .collect(Collectors.joining(","));
    }

    // ── Audit Logs ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(String tenantId, int page, int size) {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .map(this::toAuditLogResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByUser(UUID userId, String tenantId, int page, int size) {
        return auditLogRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId, PageRequest.of(page, size))
                .map(this::toAuditLogResponse);
    }

    @Transactional
    public void recordAudit(UUID userId, String userEmail, String action, String entityType,
                            String entityId, String details, String ipAddress, String userAgent,
                            String status, String tenantId) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId).userEmail(userEmail).action(action)
                .entityType(entityType).entityId(entityId).details(details)
                .ipAddress(ipAddress).userAgent(userAgent)
                .status(status != null ? status : "SUCCESS")
                .tenantId(tenantId)
                .build();
        auditLogRepository.save(auditLog);
    }

    private AuditLogResponse toAuditLogResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId()).userId(a.getUserId()).userEmail(a.getUserEmail())
                .action(a.getAction()).entityType(a.getEntityType()).entityId(a.getEntityId())
                .details(a.getDetails()).ipAddress(a.getIpAddress()).status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }

    // ── Users list (for security admin) ──────────────────────

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(String tenantId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getTenantId().equals(tenantId) && !u.isDeleted())
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .phone(u.getPhone())
                        .tenantId(u.getTenantId())
                        .enabled(u.isEnabled())
                        .roles(u.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }
}
