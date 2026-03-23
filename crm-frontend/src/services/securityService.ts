import api from './api';
import type {
  ApiResponse,
  UserProfile,
  RoleDefinition,
  PermissionSet,
  FieldSecurity,
  RecordAccessRule,
  SsoProvider,
  MfaSetup,
  AuditLogEntry,
  UserManagement,
} from '../types';

const AUTH = '/api/v1/auth';
const SEC  = '/api/v1/auth/security';

/* ── Helpers for localStorage fallback ──────────────────────── */
function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch { return fallback; }
}
function saveJson<T>(key: string, data: T): void {
  localStorage.setItem(key, JSON.stringify(data));
}

/* ── Default data (fallback when backend is unavailable) ────── */
const DEFAULT_ROLES: RoleDefinition[] = [
  { id: '1', name: 'ADMIN', description: 'Full access to all features', level: 100, permissions: ['*'], tenantId: 'default' },
  { id: '2', name: 'MANAGER', description: 'Manage team and view reports', level: 75, permissions: ['leads:*', 'accounts:*', 'contacts:*', 'opportunities:*', 'activities:*', 'reports:read', 'workflows:read'], tenantId: 'default' },
  { id: '3', name: 'USER', description: 'Standard CRM user access', level: 50, permissions: ['leads:read,create,update', 'accounts:read', 'contacts:read,create,update', 'opportunities:read,create', 'activities:*'], tenantId: 'default' },
  { id: '4', name: 'VIEWER', description: 'Read-only access', level: 10, permissions: ['leads:read', 'accounts:read', 'contacts:read', 'opportunities:read'], tenantId: 'default' },
];

const DEFAULT_PERMISSION_SETS: PermissionSet[] = [
  { id: '1', name: 'Sales Rep', description: 'Standard sales representative', permissions: [
    { resource: 'leads', actions: ['create', 'read', 'update'] },
    { resource: 'contacts', actions: ['create', 'read', 'update'] },
    { resource: 'accounts', actions: ['read'] },
    { resource: 'opportunities', actions: ['create', 'read', 'update'] },
    { resource: 'activities', actions: ['create', 'read', 'update', 'delete'] },
  ]},
  { id: '2', name: 'Sales Manager', description: 'Full sales management access', permissions: [
    { resource: 'leads', actions: ['create', 'read', 'update', 'delete'] },
    { resource: 'contacts', actions: ['create', 'read', 'update', 'delete'] },
    { resource: 'accounts', actions: ['create', 'read', 'update', 'delete'] },
    { resource: 'opportunities', actions: ['create', 'read', 'update', 'delete'] },
    { resource: 'activities', actions: ['create', 'read', 'update', 'delete'] },
    { resource: 'reports', actions: ['read'] },
    { resource: 'workflows', actions: ['create', 'read', 'update'] },
  ]},
  { id: '3', name: 'Read Only', description: 'View access to all modules', permissions: [
    { resource: 'leads', actions: ['read'] },
    { resource: 'contacts', actions: ['read'] },
    { resource: 'accounts', actions: ['read'] },
    { resource: 'opportunities', actions: ['read'] },
    { resource: 'activities', actions: ['read'] },
  ]},
];

const DEFAULT_FIELD_SECURITY: FieldSecurity[] = [
  { id: '1', entityType: 'LEAD', fieldName: 'leadScore', role: 'VIEWER', visible: false, editable: false },
  { id: '2', entityType: 'OPPORTUNITY', fieldName: 'amount', role: 'VIEWER', visible: true, editable: false },
  { id: '3', entityType: 'OPPORTUNITY', fieldName: 'probability', role: 'USER', visible: true, editable: false },
  { id: '4', entityType: 'CONTACT', fieldName: 'phone', role: 'VIEWER', visible: false, editable: false },
  { id: '5', entityType: 'ACCOUNT', fieldName: 'annualRevenue', role: 'USER', visible: true, editable: false },
];

const DEFAULT_ACCESS_RULES: RecordAccessRule[] = [
  { id: '1', entityType: 'LEAD', accessType: 'OWNER', canRead: true, canEdit: true, canDelete: true },
  { id: '2', entityType: 'LEAD', accessType: 'TEAM', canRead: true, canEdit: true, canDelete: false },
  { id: '3', entityType: 'LEAD', accessType: 'ROLE', role: 'MANAGER', canRead: true, canEdit: true, canDelete: false },
  { id: '4', entityType: 'OPPORTUNITY', accessType: 'OWNER', canRead: true, canEdit: true, canDelete: true },
  { id: '5', entityType: 'OPPORTUNITY', accessType: 'TEAM', canRead: true, canEdit: false, canDelete: false },
  { id: '6', entityType: 'ACCOUNT', accessType: 'PUBLIC', canRead: true, canEdit: false, canDelete: false },
];

const SAMPLE_AUDIT_LOGS: AuditLogEntry[] = [
  { id: '1', userId: 'u1', userEmail: 'testadmin@crm.com', action: 'LOGIN', entityType: 'User', entityId: 'u1', details: 'Successful login', ipAddress: '127.0.0.1', timestamp: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', userId: 'u1', userEmail: 'testadmin@crm.com', action: 'CREATE', entityType: 'Lead', entityId: 'l1', details: 'Created lead "John Doe"', ipAddress: '127.0.0.1', timestamp: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', userId: 'u1', userEmail: 'testadmin@crm.com', action: 'UPDATE', entityType: 'Opportunity', entityId: 'o1', details: 'Updated stage to NEGOTIATION', ipAddress: '127.0.0.1', timestamp: new Date(Date.now() - 10800000).toISOString() },
  { id: '4', userId: 'u1', userEmail: 'testadmin@crm.com', action: 'ROLE_ASSIGN', entityType: 'User', entityId: 'u2', details: 'Assigned MANAGER role', ipAddress: '127.0.0.1', timestamp: new Date(Date.now() - 14400000).toISOString() },
  { id: '5', userId: 'u1', userEmail: 'testadmin@crm.com', action: 'DELETE', entityType: 'Activity', entityId: 'a5', details: 'Deleted task "Follow up call"', ipAddress: '127.0.0.1', timestamp: new Date(Date.now() - 18000000).toISOString() },
  { id: '6', userId: 'u1', userEmail: 'testadmin@crm.com', action: 'EXPORT', entityType: 'Report', entityId: 'r1', details: 'Exported Sales Report CSV', ipAddress: '127.0.0.1', timestamp: new Date(Date.now() - 21600000).toISOString() },
  { id: '7', userId: 'u1', userEmail: 'testadmin@crm.com', action: 'PASSWORD_RESET', entityType: 'User', entityId: 'u1', details: 'Password changed', ipAddress: '127.0.0.1', timestamp: new Date(Date.now() - 86400000).toISOString() },
  { id: '8', userId: 'u1', userEmail: 'testadmin@crm.com', action: 'MFA_ENABLE', entityType: 'User', entityId: 'u1', details: 'MFA enabled (TOTP)', ipAddress: '127.0.0.1', timestamp: new Date(Date.now() - 172800000).toISOString() },
];

const STORAGE_KEYS = {
  roles: 'crm_security_roles',
  permSets: 'crm_security_permissions',
  fieldSec: 'crm_security_fields',
  accessRules: 'crm_security_access',
  ssoProviders: 'crm_security_sso',
  mfa: 'crm_security_mfa',
  auditLogs: 'crm_security_audit',
};

/* ── Async helper: try API, fall back to localStorage ──────── */
async function tryApi<T>(apiFn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    const result = await apiFn();
    return result ?? fallback;
  } catch { return fallback; }
}

export const securityService = {
  // ── Profile (real API) ──────────────────────────────────
  getProfile: () =>
    api.get<ApiResponse<UserProfile>>(`${AUTH}/me`).then((r) => r.data),

  assignRole: (userId: string, roleName: string) =>
    api.post<ApiResponse<void>>(`${AUTH}/users/${userId}/roles`, null, {
      params: { roleName },
    }).then((r) => r.data),

  // ── Roles (API with localStorage fallback) ──────────────
  getRoles: (): RoleDefinition[] => loadJson(STORAGE_KEYS.roles, DEFAULT_ROLES),
  fetchRoles: (): Promise<RoleDefinition[]> =>
    tryApi(
      () => api.get<ApiResponse<RoleDefinition[]>>(`${SEC}/roles`).then(r => r.data.data!),
      loadJson(STORAGE_KEYS.roles, DEFAULT_ROLES),
    ),
  saveRoles: (roles: RoleDefinition[]) => saveJson(STORAGE_KEYS.roles, roles),

  // ── Permission Sets ─────────────────────────────────────
  getPermissionSets: (): PermissionSet[] => loadJson(STORAGE_KEYS.permSets, DEFAULT_PERMISSION_SETS),
  fetchPermissions: (): Promise<PermissionSet[]> =>
    tryApi(
      () => api.get<ApiResponse<PermissionSet[]>>(`${SEC}/permissions`).then(r => r.data.data!),
      loadJson(STORAGE_KEYS.permSets, DEFAULT_PERMISSION_SETS),
    ),
  savePermissionSets: (sets: PermissionSet[]) => saveJson(STORAGE_KEYS.permSets, sets),

  // ── Field-level Security ────────────────────────────────
  getFieldSecurity: (): FieldSecurity[] => loadJson(STORAGE_KEYS.fieldSec, DEFAULT_FIELD_SECURITY),
  fetchFieldSecurity: (): Promise<FieldSecurity[]> =>
    tryApi(
      () => api.get<ApiResponse<FieldSecurity[]>>(`${SEC}/field-security`).then(r => r.data.data!),
      loadJson(STORAGE_KEYS.fieldSec, DEFAULT_FIELD_SECURITY),
    ),
  saveFieldSecurity: (rules: FieldSecurity[]) => saveJson(STORAGE_KEYS.fieldSec, rules),

  // ── Record-level Access ─────────────────────────────────
  getAccessRules: (): RecordAccessRule[] => loadJson(STORAGE_KEYS.accessRules, DEFAULT_ACCESS_RULES),
  saveAccessRules: (rules: RecordAccessRule[]) => saveJson(STORAGE_KEYS.accessRules, rules),

  // ── SSO ─────────────────────────────────────────────────
  getSsoProviders: (): SsoProvider[] => loadJson(STORAGE_KEYS.ssoProviders, []),
  fetchSsoProviders: (): Promise<SsoProvider[]> =>
    tryApi(
      () => api.get<ApiResponse<SsoProvider[]>>(`${SEC}/sso`).then(r => r.data.data!),
      loadJson(STORAGE_KEYS.ssoProviders, []),
    ),
  saveSsoProviders: (providers: SsoProvider[]) => saveJson(STORAGE_KEYS.ssoProviders, providers),

  // ── MFA ─────────────────────────────────────────────────
  getMfaSetup: (): MfaSetup => loadJson(STORAGE_KEYS.mfa, { enabled: false, method: 'TOTP' as const }),
  saveMfaSetup: (setup: MfaSetup) => saveJson(STORAGE_KEYS.mfa, setup),
  enableMfaApi: async (userId: string): Promise<any> => {
    try {
      const res = await api.post<ApiResponse<any>>(`${SEC}/mfa`, { userId, mfaType: 'TOTP', enabled: true });
      return res.data.data;
    } catch { return null; }
  },
  disableMfaApi: async (id: string): Promise<void> => {
    try { await api.delete(`${SEC}/mfa/${id}`); } catch { /* ignore */ }
  },

  // ── Audit Logs ──────────────────────────────────────────
  getAuditLogs: (): AuditLogEntry[] => loadJson(STORAGE_KEYS.auditLogs, SAMPLE_AUDIT_LOGS),
  fetchAuditLogs: (): Promise<AuditLogEntry[]> =>
    tryApi(
      () => api.get<ApiResponse<AuditLogEntry[]>>(`${SEC}/audit-logs`).then(r => r.data.data!),
      loadJson(STORAGE_KEYS.auditLogs, SAMPLE_AUDIT_LOGS),
    ),

  // ── Users ───────────────────────────────────────────────
  fetchUsers: (): Promise<UserManagement[]> =>
    tryApi(
      () => api.get<ApiResponse<UserManagement[]>>(`${SEC}/users`).then(r => r.data.data!),
      [],
    ),
};
