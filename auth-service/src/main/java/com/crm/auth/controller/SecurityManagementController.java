package com.crm.auth.controller;

import com.crm.auth.dto.*;
import com.crm.auth.service.SecurityManagementService;
import com.crm.common.dto.ApiResponse;
import com.crm.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/security")
@RequiredArgsConstructor
@Tag(name = "Security Management", description = "Roles, permissions, field security, SSO, MFA, and audit log APIs")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityManagementController {

    private final SecurityManagementService securityService;

    // ── Roles ────────────────────────────────────────────────

    @GetMapping("/roles")
    @Operation(summary = "Get all roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.getRoles(principal.getTenantId())));
    }

    @PostMapping("/roles")
    @Operation(summary = "Create a new role")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody RoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                securityService.createRole(request, principal.getTenantId()), "Role created"));
    }

    @DeleteMapping("/roles/{id}")
    @Operation(summary = "Delete a role")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        securityService.deleteRole(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted"));
    }

    // ── Permissions ──────────────────────────────────────────

    @GetMapping("/permissions")
    @Operation(summary = "Get all permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.getPermissions(principal.getTenantId())));
    }

    @PostMapping("/permissions")
    @Operation(summary = "Create a permission")
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @Valid @RequestBody PermissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                securityService.createPermission(request, principal.getTenantId()), "Permission created"));
    }

    @PutMapping("/permissions/{id}")
    @Operation(summary = "Update a permission")
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody PermissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.updatePermission(id, request, principal.getTenantId())));
    }

    @DeleteMapping("/permissions/{id}")
    @Operation(summary = "Delete a permission")
    public ResponseEntity<ApiResponse<Void>> deletePermission(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        securityService.deletePermission(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(null, "Permission deleted"));
    }

    // ── Field Security ───────────────────────────────────────

    @GetMapping("/field-security")
    @Operation(summary = "Get all field security rules")
    public ResponseEntity<ApiResponse<List<FieldSecurityResponse>>> getFieldSecurityRules(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.getFieldSecurityRules(principal.getTenantId())));
    }

    @PostMapping("/field-security")
    @Operation(summary = "Create field security rule")
    public ResponseEntity<ApiResponse<FieldSecurityResponse>> createFieldSecurityRule(
            @Valid @RequestBody FieldSecurityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                securityService.createFieldSecurityRule(request, principal.getTenantId())));
    }

    @PutMapping("/field-security/{id}")
    @Operation(summary = "Update field security rule")
    public ResponseEntity<ApiResponse<FieldSecurityResponse>> updateFieldSecurityRule(
            @PathVariable UUID id,
            @Valid @RequestBody FieldSecurityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.updateFieldSecurityRule(id, request, principal.getTenantId())));
    }

    @DeleteMapping("/field-security/{id}")
    @Operation(summary = "Delete field security rule")
    public ResponseEntity<ApiResponse<Void>> deleteFieldSecurityRule(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        securityService.deleteFieldSecurityRule(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(null, "Field security rule deleted"));
    }

    // ── SSO Providers ────────────────────────────────────────

    @GetMapping("/sso")
    @Operation(summary = "Get all SSO providers")
    public ResponseEntity<ApiResponse<List<SsoProviderResponse>>> getSsoProviders(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.getSsoProviders(principal.getTenantId())));
    }

    @PostMapping("/sso")
    @Operation(summary = "Create SSO provider")
    public ResponseEntity<ApiResponse<SsoProviderResponse>> createSsoProvider(
            @Valid @RequestBody SsoProviderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                securityService.createSsoProvider(request, principal.getTenantId())));
    }

    @PutMapping("/sso/{id}")
    @Operation(summary = "Update SSO provider")
    public ResponseEntity<ApiResponse<SsoProviderResponse>> updateSsoProvider(
            @PathVariable UUID id,
            @Valid @RequestBody SsoProviderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.updateSsoProvider(id, request, principal.getTenantId())));
    }

    @DeleteMapping("/sso/{id}")
    @Operation(summary = "Delete SSO provider")
    public ResponseEntity<ApiResponse<Void>> deleteSsoProvider(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        securityService.deleteSsoProvider(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(null, "SSO provider deleted"));
    }

    // ── MFA ──────────────────────────────────────────────────

    @GetMapping("/mfa")
    @Operation(summary = "Get all MFA configurations")
    public ResponseEntity<ApiResponse<List<MfaConfigResponse>>> getMfaConfigs(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.getMfaConfigs(principal.getTenantId())));
    }

    @PostMapping("/mfa")
    @Operation(summary = "Setup or update MFA for a user")
    public ResponseEntity<ApiResponse<MfaConfigResponse>> setupMfa(
            @Valid @RequestBody MfaConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.setupMfa(request, principal.getTenantId())));
    }

    @DeleteMapping("/mfa/{id}")
    @Operation(summary = "Remove MFA configuration")
    public ResponseEntity<ApiResponse<Void>> deleteMfa(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        securityService.deleteMfaConfig(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(null, "MFA configuration removed"));
    }

    // ── Audit Logs ───────────────────────────────────────────

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs (paginated)")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.getAuditLogs(principal.getTenantId(), page, size)));
    }

    @GetMapping("/audit-logs/user/{userId}")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.getAuditLogsByUser(userId, principal.getTenantId(), page, size)));
    }

    // ── Users ────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "Get all users for security management")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                securityService.getUsers(principal.getTenantId())));
    }
}
