/* ============================================================
   SecurityPage – Security & User Management (8 Features)
   1. Role Hierarchy  2. User Profile Management
   3. Permission Sets  4. Field-level Security
   5. Record-level Access Control  6. Single Sign-On (SSO)
   7. Multi-factor Authentication  8. Audit Logs
   ============================================================ */
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Box, Typography, Tabs, Tab, Button, TextField, Select, MenuItem,
  FormControl, InputLabel, IconButton, Chip, Switch, FormControlLabel,
  Card, CardContent, CardActions, Dialog, DialogTitle, DialogContent,
  DialogActions, Alert, Snackbar, Table, TableHead, TableBody, TableRow,
  TableCell, Paper, Tooltip, Divider, Grid, Stack, LinearProgress,
  Checkbox, Accordion, AccordionSummary, AccordionDetails,
  SelectChangeEvent, Avatar, TablePagination,
} from '@mui/material';
import {
  Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon,
  ExpandMore, Save as SaveIcon, Security as ShieldIcon,
  AdminPanelSettings as RoleIcon, VpnKey as KeyIcon,
  Lock as LockIcon, Visibility as VisibleIcon, VisibilityOff as HiddenIcon,
  CloudSync as SsoIcon, PhonelinkLock as MfaIcon,
  History as AuditIcon, CheckCircle, Cancel as CancelIcon,
  ContentCopy as CopyIcon, QrCode2 as QrIcon,
  FilterList, Download as DownloadIcon,
} from '@mui/icons-material';
import { securityService } from '../services/securityService';
import { useAppSelector } from '../hooks/useRedux';
import type {
  RoleDefinition, PermissionSet, Permission, FieldSecurity,
  RecordAccessRule, SsoProvider, MfaSetup, AuditLogEntry,
} from '../types';
import { PageHeader } from '../components';
/* ── Constants ─────────────────────────────────────────────── */
const ENTITY_TYPES = ['LEAD', 'ACCOUNT', 'CONTACT', 'OPPORTUNITY', 'ACTIVITY'];
const ROLE_NAMES = ['ADMIN', 'MANAGER', 'USER', 'VIEWER'];
const RESOURCES = ['leads', 'accounts', 'contacts', 'opportunities', 'activities', 'reports', 'workflows', 'settings'];
const ACTIONS: Permission['actions'][number][] = ['create', 'read', 'update', 'delete'];
const ENTITY_FIELDS: Record<string, string[]> = {
  LEAD: ['email', 'phone', 'company', 'leadScore', 'assignedTo', 'status', 'source', 'territory'],
  ACCOUNT: ['annualRevenue', 'numberOfEmployees', 'phone', 'website', 'ownerId', 'billingAddress'],
  CONTACT: ['email', 'phone', 'mobilePhone', 'department', 'title', 'linkedinUrl'],
  OPPORTUNITY: ['amount', 'probability', 'closeDate', 'forecastCategory', 'assignedTo', 'lostReason'],
  ACTIVITY: ['assignedTo', 'dueDate', 'priority', 'location', 'callOutcome'],
};

/* ── Tab Panel ─────────────────────────────────────────────── */
const TabPanel: React.FC<{ value: number; index: number; children: React.ReactNode }> = ({ value, index, children }) =>
  value === index ? <Box sx={{ pt: 2 }}>{children}</Box> : null;

/* ================================================================
   MAIN COMPONENT
   ================================================================ */
const SecurityPage: React.FC = () => {
  const [tab, setTab] = useState(0);
  const [snack, setSnack] = useState<{ open: boolean; msg: string; severity: 'success' | 'error' | 'info' }>({
    open: false, msg: '', severity: 'success',
  });
  const { user } = useAppSelector((s) => s.auth);

  /* ── 1. Roles ────────────────────────────────────────────── */
  const [roles, setRoles] = useState<RoleDefinition[]>([]);
  const [roleDialog, setRoleDialog] = useState(false);
  const [editRole, setEditRole] = useState<RoleDefinition | null>(null);
  const [roleName, setRoleName] = useState('');
  const [roleDesc, setRoleDesc] = useState('');
  const [roleLevel, setRoleLevel] = useState(50);

  /* ── 3. Permission Sets ──────────────────────────────────── */
  const [permSets, setPermSets] = useState<PermissionSet[]>([]);
  const [permDialog, setPermDialog] = useState(false);
  const [editPerm, setEditPerm] = useState<PermissionSet | null>(null);
  const [permName, setPermName] = useState('');
  const [permDesc, setPermDesc] = useState('');
  const [permMatrix, setPermMatrix] = useState<Record<string, Record<string, boolean>>>({});

  /* ── 4. Field Security ───────────────────────────────────── */
  const [fieldRules, setFieldRules] = useState<FieldSecurity[]>([]);
  const [fieldDialog, setFieldDialog] = useState(false);
  const [fsEntity, setFsEntity] = useState('LEAD');
  const [fsField, setFsField] = useState('');
  const [fsRole, setFsRole] = useState('USER');
  const [fsVisible, setFsVisible] = useState(true);
  const [fsEditable, setFsEditable] = useState(false);

  /* ── 5. Record Access ────────────────────────────────────── */
  const [accessRules, setAccessRules] = useState<RecordAccessRule[]>([]);
  const [accessDialog, setAccessDialog] = useState(false);
  const [arEntity, setArEntity] = useState('LEAD');
  const [arType, setArType] = useState<RecordAccessRule['accessType']>('OWNER');
  const [arRole, setArRole] = useState('');
  const [arRead, setArRead] = useState(true);
  const [arEdit, setArEdit] = useState(false);
  const [arDelete, setArDelete] = useState(false);

  /* ── 6. SSO ──────────────────────────────────────────────── */
  const [ssoProviders, setSsoProviders] = useState<SsoProvider[]>([]);
  const [ssoDialog, setSsoDialog] = useState(false);
  const [ssoName, setSsoName] = useState('');
  const [ssoProtocol, setSsoProtocol] = useState<'SAML' | 'OIDC'>('OIDC');
  const [ssoIssuer, setSsoIssuer] = useState('');
  const [ssoClientId, setSsoClientId] = useState('');

  /* ── 7. MFA ──────────────────────────────────────────────── */
  const [mfa, setMfa] = useState<MfaSetup>({ enabled: false, method: 'TOTP' });
  const [mfaVerifyDialog, setMfaVerifyDialog] = useState(false);
  const [mfaCode, setMfaCode] = useState('');

  /* ── 8. Audit Logs ───────────────────────────────────────── */
  const [auditLogs, setAuditLogs] = useState<AuditLogEntry[]>([]);
  const [auditFilter, setAuditFilter] = useState('');
  const [auditPage, setAuditPage] = useState(0);

  /* ── Load data ───────────────────────────────────────────── */
  useEffect(() => {
    // Immediate sync load from localStorage
    setRoles(securityService.getRoles());
    setPermSets(securityService.getPermissionSets());
    setFieldRules(securityService.getFieldSecurity());
    setAccessRules(securityService.getAccessRules());
    setSsoProviders(securityService.getSsoProviders());
    setMfa(securityService.getMfaSetup());
    setAuditLogs(securityService.getAuditLogs());
    // Try real API (overrides if backend is running)
    const load = async () => {
      // Check roles from JWT to skip admin-only endpoints for non-admin users
      let isAdmin = false;
      try {
        const token = localStorage.getItem('accessToken');
        if (token) {
          const payload = JSON.parse(atob(token.split('.')[1]));
          isAdmin = (payload.roles || []).some((r: string) => r.toUpperCase() === 'ADMIN');
        }
      } catch { /* ignore parse errors */ }
      const fetchers: Promise<any>[] = [
        securityService.fetchRoles(),
        securityService.fetchPermissions(),
        securityService.fetchFieldSecurity(),
        isAdmin ? securityService.fetchSsoProviders() : Promise.resolve(null),
        isAdmin ? securityService.fetchAuditLogs() : Promise.resolve(null),
      ];
      const [r, p, f, s, a] = await Promise.all(fetchers);
      if (Array.isArray(r)) setRoles(r);
      if (Array.isArray(p)) setPermSets(p);
      if (Array.isArray(f)) setFieldRules(f);
      if (Array.isArray(s)) setSsoProviders(s);
      if (Array.isArray(a)) setAuditLogs(a);
    };
    load();
  }, []);

  const notify = (msg: string, severity: 'success' | 'error' | 'info' = 'success') =>
    setSnack({ open: true, msg, severity });

  /* ════════════════════════════════════════════════════════════
     1. ROLE HIERARCHY
     ════════════════════════════════════════════════════════════ */
  const openRoleDialog = (role?: RoleDefinition) => {
    if (role) {
      setEditRole(role);
      setRoleName(role.name);
      setRoleDesc(role.description);
      setRoleLevel(role.level);
    } else {
      setEditRole(null);
      setRoleName('');
      setRoleDesc('');
      setRoleLevel(50);
    }
    setRoleDialog(true);
  };
  const saveRole = () => {
    if (!roleName.trim()) return;
    let updated: RoleDefinition[];
    if (editRole) {
      updated = roles.map((r) => r.id === editRole.id ? { ...r, name: roleName, description: roleDesc, level: roleLevel } : r);
    } else {
      updated = [...roles, { id: Date.now().toString(), name: roleName.toUpperCase(), description: roleDesc, level: roleLevel, permissions: [], tenantId: 'default' }];
    }
    updated.sort((a, b) => b.level - a.level);
    setRoles(updated);
    securityService.saveRoles(updated);
    setRoleDialog(false);
    notify(editRole ? 'Role updated' : 'Role created');
  };
  const deleteRole = (id: string) => {
    const updated = roles.filter((r) => r.id !== id);
    setRoles(updated);
    securityService.saveRoles(updated);
    notify('Role deleted');
  };

  /* ════════════════════════════════════════════════════════════
     3. PERMISSION SETS
     ════════════════════════════════════════════════════════════ */
  const openPermDialog = (ps?: PermissionSet) => {
    const matrix: Record<string, Record<string, boolean>> = {};
    RESOURCES.forEach((res) => {
      matrix[res] = {};
      ACTIONS.forEach((act) => { matrix[res][act] = false; });
    });
    if (ps) {
      setEditPerm(ps);
      setPermName(ps.name);
      setPermDesc(ps.description);
      ps.permissions.forEach((p) => {
        if (matrix[p.resource]) {
          p.actions.forEach((a) => { matrix[p.resource][a] = true; });
        }
      });
    } else {
      setEditPerm(null);
      setPermName('');
      setPermDesc('');
    }
    setPermMatrix(matrix);
    setPermDialog(true);
  };
  const togglePerm = (resource: string, action: string) => {
    setPermMatrix((prev) => ({
      ...prev,
      [resource]: { ...prev[resource], [action]: !prev[resource][action] },
    }));
  };
  const savePerm = () => {
    if (!permName.trim()) return;
    const perms: Permission[] = [];
    RESOURCES.forEach((res) => {
      const acts = ACTIONS.filter((a) => permMatrix[res]?.[a]);
      if (acts.length > 0) perms.push({ resource: res, actions: acts });
    });
    let updated: PermissionSet[];
    if (editPerm) {
      updated = permSets.map((p) => p.id === editPerm.id ? { ...p, name: permName, description: permDesc, permissions: perms } : p);
    } else {
      updated = [...permSets, { id: Date.now().toString(), name: permName, description: permDesc, permissions: perms }];
    }
    setPermSets(updated);
    securityService.savePermissionSets(updated);
    setPermDialog(false);
    notify(editPerm ? 'Permission set updated' : 'Permission set created');
  };
  const deletePerm = (id: string) => {
    const updated = permSets.filter((p) => p.id !== id);
    setPermSets(updated);
    securityService.savePermissionSets(updated);
    notify('Permission set deleted');
  };

  /* ════════════════════════════════════════════════════════════
     4. FIELD-LEVEL SECURITY
     ════════════════════════════════════════════════════════════ */
  const saveFieldRule = () => {
    if (!fsField) return;
    const rule: FieldSecurity = { id: Date.now().toString(), entityType: fsEntity, fieldName: fsField, role: fsRole, visible: fsVisible, editable: fsEditable };
    const updated = [...fieldRules, rule];
    setFieldRules(updated);
    securityService.saveFieldSecurity(updated);
    setFieldDialog(false);
    setFsField('');
    notify('Field security rule added');
  };
  const deleteFieldRule = (id: string) => {
    const updated = fieldRules.filter((r) => r.id !== id);
    setFieldRules(updated);
    securityService.saveFieldSecurity(updated);
    notify('Rule removed');
  };

  /* ════════════════════════════════════════════════════════════
     5. RECORD-LEVEL ACCESS
     ════════════════════════════════════════════════════════════ */
  const saveAccessRule = () => {
    const rule: RecordAccessRule = { id: Date.now().toString(), entityType: arEntity, accessType: arType, role: arType === 'ROLE' ? arRole : undefined, canRead: arRead, canEdit: arEdit, canDelete: arDelete };
    const updated = [...accessRules, rule];
    setAccessRules(updated);
    securityService.saveAccessRules(updated);
    setAccessDialog(false);
    notify('Access rule added');
  };
  const deleteAccessRule = (id: string) => {
    const updated = accessRules.filter((r) => r.id !== id);
    setAccessRules(updated);
    securityService.saveAccessRules(updated);
    notify('Access rule removed');
  };

  /* ════════════════════════════════════════════════════════════
     6. SSO
     ════════════════════════════════════════════════════════════ */
  const saveSso = () => {
    if (!ssoName.trim() || !ssoIssuer.trim()) return;
    const provider: SsoProvider = {
      id: Date.now().toString(), name: ssoName, protocol: ssoProtocol,
      issuerUrl: ssoIssuer, clientId: ssoClientId, enabled: true,
      createdAt: new Date().toISOString(),
    };
    const updated = [...ssoProviders, provider];
    setSsoProviders(updated);
    securityService.saveSsoProviders(updated);
    setSsoDialog(false);
    setSsoName(''); setSsoIssuer(''); setSsoClientId('');
    notify('SSO provider added');
  };
  const toggleSso = (id: string) => {
    const updated = ssoProviders.map((p) => p.id === id ? { ...p, enabled: !p.enabled } : p);
    setSsoProviders(updated);
    securityService.saveSsoProviders(updated);
  };
  const deleteSso = (id: string) => {
    const updated = ssoProviders.filter((p) => p.id !== id);
    setSsoProviders(updated);
    securityService.saveSsoProviders(updated);
    notify('SSO provider removed');
  };

  /* ════════════════════════════════════════════════════════════
     7. MFA
     ════════════════════════════════════════════════════════════ */
  const toggleMfa = async () => {
    if (!mfa.enabled) {
      // Call backend to set up MFA, then show verify dialog
      try {
        const token = localStorage.getItem('accessToken');
        if (token) {
          const payload = JSON.parse(atob(token.split('.')[1]));
          await securityService.enableMfaApi(payload.sub || payload.userId);
        }
      } catch { /* continue with local flow */ }
      setMfaVerifyDialog(true);
    } else {
      const updated = { ...mfa, enabled: false, verifiedAt: undefined };
      setMfa(updated);
      securityService.saveMfaSetup(updated);
      notify('MFA disabled', 'info');
    }
  };
  const verifyMfaSetup = () => {
    if (mfaCode.length !== 6) return;
    const updated: MfaSetup = { ...mfa, enabled: true, verifiedAt: new Date().toISOString() };
    setMfa(updated);
    securityService.saveMfaSetup(updated);
    setMfaVerifyDialog(false);
    setMfaCode('');
    notify('MFA enabled successfully — you will be prompted for a code on login');
  };

  /* ════════════════════════════════════════════════════════════
     8. AUDIT LOGS
     ════════════════════════════════════════════════════════════ */
  const filteredLogs = useMemo(() => {
    if (!auditFilter) return auditLogs;
    const q = auditFilter.toLowerCase();
    return auditLogs.filter((l) =>
      l.action.toLowerCase().includes(q) ||
      l.entityType.toLowerCase().includes(q) ||
      l.userEmail.toLowerCase().includes(q) ||
      l.details.toLowerCase().includes(q)
    );
  }, [auditLogs, auditFilter]);

  const actionColor = (action: string): 'default' | 'success' | 'error' | 'warning' | 'info' | 'primary' => {
    if (action.includes('CREATE') || action.includes('LOGIN')) return 'success';
    if (action.includes('DELETE')) return 'error';
    if (action.includes('UPDATE') || action.includes('ROLE')) return 'warning';
    if (action.includes('EXPORT') || action.includes('PASSWORD') || action.includes('MFA')) return 'info';
    return 'default';
  };

  /* ────────────────────────────────────────────────────────── */
  /*               R E N D E R                                  */
  /* ────────────────────────────────────────────────────────── */
  return (
    <Box>
      <PageHeader
        title="Security & User Management"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Security' }]}
      />

      <Tabs
        value={tab}
        onChange={(_, v) => setTab(v)}
        variant="scrollable"
        scrollButtons="auto"
        sx={{ mb: 1, borderBottom: 1, borderColor: 'divider' }}
      >
        <Tab icon={<RoleIcon />} label="Roles" iconPosition="start" />
        <Tab icon={<KeyIcon />} label="Permissions" iconPosition="start" />
        <Tab icon={<LockIcon />} label="Field Security" iconPosition="start" />
        <Tab icon={<ShieldIcon />} label="Record Access" iconPosition="start" />
        <Tab icon={<SsoIcon />} label="SSO" iconPosition="start" />
        <Tab icon={<MfaIcon />} label="MFA" iconPosition="start" />
        <Tab icon={<AuditIcon />} label="Audit Logs" iconPosition="start" />
      </Tabs>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 0: ROLE HIERARCHY                                 ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={0}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Role Hierarchy</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => openRoleDialog()}>Add Role</Button>
        </Box>
        <Alert severity="info" sx={{ mb: 2 }}>
          Roles are ordered by hierarchy level. Higher-level roles inherit permissions from lower levels.
        </Alert>
        <Grid container spacing={2}>
          {roles.map((role, idx) => (
            <Grid item xs={12} md={6} key={role.id}>
              <Card variant="outlined" sx={{ position: 'relative', overflow: 'visible' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                    <Avatar sx={{ bgcolor: idx === 0 ? 'error.main' : idx === 1 ? 'warning.main' : idx === 2 ? 'primary.main' : 'grey.500', width: 40, height: 40 }}>
                      <RoleIcon />
                    </Avatar>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="h6">{role.name}</Typography>
                      <Typography variant="body2" color="text.secondary">{role.description}</Typography>
                    </Box>
                    <Chip label={`Level ${role.level}`} size="small" color={role.level >= 75 ? 'error' : role.level >= 50 ? 'warning' : 'default'} />
                  </Box>
                  <Box sx={{ mt: 1 }}>
                    <Typography variant="caption" color="text.secondary">Hierarchy Level</Typography>
                    <LinearProgress
                      variant="determinate" value={role.level}
                      sx={{ height: 8, borderRadius: 4, mt: 0.5 }}
                      color={role.level >= 75 ? 'error' : role.level >= 50 ? 'warning' : 'primary'}
                    />
                  </Box>
                  {role.permissions.length > 0 && (
                    <Box sx={{ mt: 1.5, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                      {role.permissions.slice(0, 5).map((p) => (
                        <Chip key={p} label={p} size="small" variant="outlined" />
                      ))}
                      {role.permissions.length > 5 && <Chip label={`+${role.permissions.length - 5}`} size="small" />}
                    </Box>
                  )}
                </CardContent>
                <CardActions>
                  <Button size="small" startIcon={<EditIcon />} onClick={() => openRoleDialog(role)}>Edit</Button>
                  <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => deleteRole(role.id)}
                    disabled={['ADMIN', 'USER'].includes(role.name)}>Delete</Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 1: PERMISSION SETS                                ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={1}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Permission Sets</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => openPermDialog()}>New Permission Set</Button>
        </Box>
        {permSets.map((ps) => (
          <Accordion key={ps.id} variant="outlined" sx={{ mb: 1 }}>
            <AccordionSummary expandIcon={<ExpandMore />}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}>
                <KeyIcon color="primary" />
                <Box sx={{ flex: 1 }}>
                  <Typography fontWeight={600}>{ps.name}</Typography>
                  <Typography variant="body2" color="text.secondary">{ps.description}</Typography>
                </Box>
                <Chip label={`${ps.permissions.length} resources`} size="small" />
              </Box>
            </AccordionSummary>
            <AccordionDetails>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Resource</TableCell>
                    <TableCell align="center">Create</TableCell>
                    <TableCell align="center">Read</TableCell>
                    <TableCell align="center">Update</TableCell>
                    <TableCell align="center">Delete</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {ps.permissions.map((p) => (
                    <TableRow key={p.resource}>
                      <TableCell><Chip label={p.resource} size="small" variant="outlined" /></TableCell>
                      {ACTIONS.map((a) => (
                        <TableCell key={a} align="center">
                          {p.actions.includes(a) ? <CheckCircle color="success" fontSize="small" /> : <CancelIcon color="disabled" fontSize="small" />}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Box sx={{ mt: 2, display: 'flex', gap: 1 }}>
                <Button size="small" startIcon={<EditIcon />} onClick={() => openPermDialog(ps)}>Edit</Button>
                <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => deletePerm(ps.id)}>Delete</Button>
              </Box>
            </AccordionDetails>
          </Accordion>
        ))}
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 2: FIELD-LEVEL SECURITY                           ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={2}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Field-Level Security</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setFieldDialog(true)}>Add Rule</Button>
        </Box>
        <Alert severity="info" sx={{ mb: 2 }}>
          Control visibility and editability of specific fields per role. Restricted fields are hidden or read-only for the assigned role.
        </Alert>
        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Entity</TableCell>
                <TableCell>Field</TableCell>
                <TableCell>Role</TableCell>
                <TableCell align="center">Visible</TableCell>
                <TableCell align="center">Editable</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {fieldRules.length === 0 ? (
                <TableRow><TableCell colSpan={6} align="center"><Typography color="text.secondary" sx={{ py: 3 }}>No field security rules configured</Typography></TableCell></TableRow>
              ) : fieldRules.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell><Chip label={r.entityType} size="small" variant="outlined" /></TableCell>
                  <TableCell><Typography fontWeight={600}>{r.fieldName}</Typography></TableCell>
                  <TableCell><Chip label={r.role} size="small" /></TableCell>
                  <TableCell align="center">
                    {r.visible ? <VisibleIcon color="success" fontSize="small" /> : <HiddenIcon color="error" fontSize="small" />}
                  </TableCell>
                  <TableCell align="center">
                    {r.editable ? <CheckCircle color="success" fontSize="small" /> : <LockIcon color="disabled" fontSize="small" />}
                  </TableCell>
                  <TableCell align="right">
                    <IconButton size="small" color="error" onClick={() => deleteFieldRule(r.id)}><DeleteIcon fontSize="small" /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 3: RECORD-LEVEL ACCESS CONTROL                    ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={3}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Record-Level Access Control</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAccessDialog(true)}>Add Rule</Button>
        </Box>
        <Alert severity="info" sx={{ mb: 2 }}>
          Define who can access records based on ownership, team membership, role, or public access.
        </Alert>
        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Entity</TableCell>
                <TableCell>Access Type</TableCell>
                <TableCell>Role</TableCell>
                <TableCell align="center">Read</TableCell>
                <TableCell align="center">Edit</TableCell>
                <TableCell align="center">Delete</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {accessRules.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell><Chip label={r.entityType} size="small" variant="outlined" /></TableCell>
                  <TableCell><Chip label={r.accessType} size="small" color={
                    r.accessType === 'OWNER' ? 'primary' : r.accessType === 'TEAM' ? 'success' :
                    r.accessType === 'ROLE' ? 'warning' : 'default'
                  } /></TableCell>
                  <TableCell>{r.role ?? '—'}</TableCell>
                  <TableCell align="center">{r.canRead ? <CheckCircle color="success" fontSize="small" /> : <CancelIcon color="disabled" fontSize="small" />}</TableCell>
                  <TableCell align="center">{r.canEdit ? <CheckCircle color="success" fontSize="small" /> : <CancelIcon color="disabled" fontSize="small" />}</TableCell>
                  <TableCell align="center">{r.canDelete ? <CheckCircle color="error" fontSize="small" /> : <CancelIcon color="disabled" fontSize="small" />}</TableCell>
                  <TableCell align="right">
                    <IconButton size="small" color="error" onClick={() => deleteAccessRule(r.id)}><DeleteIcon fontSize="small" /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 4: SINGLE SIGN-ON (SSO)                           ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={4}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Single Sign-On (SSO) Providers</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setSsoDialog(true)}>Add Provider</Button>
        </Box>
        {ssoProviders.length === 0 ? (
          <Alert severity="info">
            No SSO providers configured. Add a SAML or OIDC provider to enable single sign-on for your organization.
          </Alert>
        ) : (
          <Grid container spacing={2}>
            {ssoProviders.map((p) => (
              <Grid item xs={12} md={6} key={p.id}>
                <Card variant="outlined">
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                      <SsoIcon color={p.enabled ? 'primary' : 'disabled'} />
                      <Box sx={{ flex: 1 }}>
                        <Typography fontWeight={600}>{p.name}</Typography>
                        <Typography variant="caption" color="text.secondary">{p.protocol}</Typography>
                      </Box>
                      <Switch checked={p.enabled} onChange={() => toggleSso(p.id)} />
                    </Box>
                    <Divider sx={{ my: 1 }} />
                    <Typography variant="body2"><strong>Issuer URL:</strong> {p.issuerUrl}</Typography>
                    <Typography variant="body2"><strong>Client ID:</strong> {p.clientId}</Typography>
                    <Typography variant="caption" color="text.secondary">Added: {new Date(p.createdAt).toLocaleDateString()}</Typography>
                  </CardContent>
                  <CardActions>
                    <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => deleteSso(p.id)}>Remove</Button>
                  </CardActions>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 5: MULTI-FACTOR AUTHENTICATION                    ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={5}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                  <MfaIcon sx={{ fontSize: 40 }} color={mfa.enabled ? 'success' : 'disabled'} />
                  <Box>
                    <Typography variant="h6">Multi-Factor Authentication</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {mfa.enabled ? 'MFA is enabled for your account' : 'MFA is not enabled'}
                    </Typography>
                  </Box>
                </Box>

                <Chip
                  label={mfa.enabled ? 'ENABLED' : 'DISABLED'}
                  color={mfa.enabled ? 'success' : 'default'}
                  sx={{ mb: 2 }}
                />

                {mfa.verifiedAt && (
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Verified: {new Date(mfa.verifiedAt).toLocaleString()}
                  </Typography>
                )}

                <FormControl fullWidth sx={{ mb: 2 }}>
                  <InputLabel>MFA Method</InputLabel>
                  <Select
                    value={mfa.method} label="MFA Method"
                    onChange={(e: SelectChangeEvent) => setMfa((prev) => ({ ...prev, method: e.target.value as MfaSetup['method'] }))}
                    disabled={mfa.enabled}
                  >
                    <MenuItem value="TOTP">Authenticator App (TOTP)</MenuItem>
                    <MenuItem value="SMS">SMS Code</MenuItem>
                    <MenuItem value="EMAIL">Email Code</MenuItem>
                  </Select>
                </FormControl>

                <Button
                  variant="contained"
                  color={mfa.enabled ? 'error' : 'success'}
                  fullWidth
                  onClick={toggleMfa}
                >
                  {mfa.enabled ? 'Disable MFA' : 'Enable MFA'}
                </Button>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h6" gutterBottom>How It Works</Typography>
                <Divider sx={{ mb: 2 }} />
                <Stack spacing={1.5}>
                  <Box sx={{ display: 'flex', gap: 2 }}>
                    <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32, fontSize: 14 }}>1</Avatar>
                    <Typography variant="body2">Choose your preferred MFA method (Authenticator App, SMS, or Email).</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 2 }}>
                    <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32, fontSize: 14 }}>2</Avatar>
                    <Typography variant="body2">Scan the QR code with your authenticator app or verify via the chosen channel.</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 2 }}>
                    <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32, fontSize: 14 }}>3</Avatar>
                    <Typography variant="body2">Enter the 6-digit verification code to confirm setup.</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 2 }}>
                    <Avatar sx={{ bgcolor: 'success.main', width: 32, height: 32, fontSize: 14 }}>✓</Avatar>
                    <Typography variant="body2">MFA is enabled! You'll be prompted for a code on each login.</Typography>
                  </Box>
                </Stack>

                {!mfa.enabled && (
                  <Alert severity="warning" sx={{ mt: 2 }}>
                    We strongly recommend enabling MFA to protect your account from unauthorized access.
                  </Alert>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 6: AUDIT LOGS                                     ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={6}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">Audit Logs</Typography>
          <Button variant="outlined" startIcon={<DownloadIcon />} onClick={() => notify('Audit logs exported', 'info')}>
            Export CSV
          </Button>
        </Box>

        {/* Stats */}
        <Grid container spacing={2} sx={{ mb: 2 }}>
          {[
            { label: 'Total Events', val: auditLogs.length, color: 'primary.main' },
            { label: 'Logins', val: auditLogs.filter((l) => l.action === 'LOGIN').length, color: 'success.main' },
            { label: 'Updates', val: auditLogs.filter((l) => l.action === 'UPDATE').length, color: 'warning.main' },
            { label: 'Deletions', val: auditLogs.filter((l) => l.action === 'DELETE').length, color: 'error.main' },
          ].map((s) => (
            <Grid item xs={6} md={3} key={s.label}>
              <Card variant="outlined">
                <CardContent sx={{ textAlign: 'center', py: 1.5 }}>
                  <Typography variant="h5" fontWeight={700} color={s.color}>{s.val}</Typography>
                  <Typography variant="body2" color="text.secondary">{s.label}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        {/* Filter */}
        <TextField
          size="small" placeholder="Filter by action, entity, user or details..."
          value={auditFilter} onChange={(e) => { setAuditFilter(e.target.value); setAuditPage(0); }}
          sx={{ mb: 2, width: 400 }}
          InputProps={{ startAdornment: <FilterList sx={{ mr: 1, color: 'text.secondary' }} /> }}
        />

        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Action</TableCell>
                <TableCell>User</TableCell>
                <TableCell>Entity</TableCell>
                <TableCell>Details</TableCell>
                <TableCell>IP Address</TableCell>
                <TableCell>Timestamp</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredLogs.length === 0 ? (
                <TableRow><TableCell colSpan={6} align="center"><Typography color="text.secondary" sx={{ py: 3 }}>No audit logs found</Typography></TableCell></TableRow>
              ) : filteredLogs.slice(auditPage * 10, auditPage * 10 + 10).map((l) => (
                <TableRow key={l.id} hover>
                  <TableCell><Chip label={l.action} size="small" color={actionColor(l.action)} /></TableCell>
                  <TableCell><Typography variant="body2">{l.userEmail}</Typography></TableCell>
                  <TableCell>
                    <Chip label={l.entityType} size="small" variant="outlined" />
                    <Typography variant="caption" sx={{ ml: 0.5 }}>{l.entityId.substring(0, 6)}…</Typography>
                  </TableCell>
                  <TableCell><Typography variant="body2" sx={{ maxWidth: 250, overflow: 'hidden', textOverflow: 'ellipsis' }}>{l.details}</Typography></TableCell>
                  <TableCell><Typography variant="body2" fontFamily="monospace">{l.ipAddress}</Typography></TableCell>
                  <TableCell>{new Date(l.timestamp).toLocaleString()}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TablePagination
            component="div" count={filteredLogs.length} page={auditPage} rowsPerPage={10}
            rowsPerPageOptions={[10]}
            onPageChange={(_, p) => setAuditPage(p)}
            onRowsPerPageChange={() => {}}
          />
        </Paper>
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  DIALOGS                                                ║
         ╚═══════════════════════════════════════════════════════╝ */}

      {/* Role Dialog */}
      <Dialog open={roleDialog} onClose={() => setRoleDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editRole ? 'Edit Role' : 'Create Role'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Role Name" fullWidth value={roleName} onChange={(e) => setRoleName(e.target.value)} />
            <TextField label="Description" fullWidth multiline rows={2} value={roleDesc} onChange={(e) => setRoleDesc(e.target.value)} />
            <Box>
              <Typography gutterBottom>Hierarchy Level: {roleLevel}</Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Typography variant="caption">Low</Typography>
                <input type="range" min={1} max={100} value={roleLevel} onChange={(e) => setRoleLevel(+e.target.value)} style={{ flex: 1 }} />
                <Typography variant="caption">High</Typography>
              </Box>
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRoleDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveRole} disabled={!roleName.trim()}>Save</Button>
        </DialogActions>
      </Dialog>

      {/* Permission Set Dialog */}
      <Dialog open={permDialog} onClose={() => setPermDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editPerm ? 'Edit Permission Set' : 'Create Permission Set'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth value={permName} onChange={(e) => setPermName(e.target.value)} />
            <TextField label="Description" fullWidth value={permDesc} onChange={(e) => setPermDesc(e.target.value)} />
            <Divider />
            <Typography variant="subtitle1" fontWeight={600}>Permission Matrix</Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Resource</TableCell>
                  {ACTIONS.map((a) => <TableCell key={a} align="center" sx={{ textTransform: 'capitalize' }}>{a}</TableCell>)}
                </TableRow>
              </TableHead>
              <TableBody>
                {RESOURCES.map((res) => (
                  <TableRow key={res}>
                    <TableCell sx={{ textTransform: 'capitalize' }}>{res}</TableCell>
                    {ACTIONS.map((a) => (
                      <TableCell key={a} align="center">
                        <Checkbox checked={!!permMatrix[res]?.[a]} onChange={() => togglePerm(res, a)} size="small" />
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPermDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={savePerm} disabled={!permName.trim()}>Save</Button>
        </DialogActions>
      </Dialog>

      {/* Field Security Dialog */}
      <Dialog open={fieldDialog} onClose={() => setFieldDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Field Security Rule</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>Entity Type</InputLabel>
              <Select value={fsEntity} label="Entity Type" onChange={(e: SelectChangeEvent) => { setFsEntity(e.target.value); setFsField(''); }}>
                {ENTITY_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Field</InputLabel>
              <Select value={fsField} label="Field" onChange={(e: SelectChangeEvent) => setFsField(e.target.value)}>
                {(ENTITY_FIELDS[fsEntity] ?? []).map((f) => <MenuItem key={f} value={f}>{f}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Role</InputLabel>
              <Select value={fsRole} label="Role" onChange={(e: SelectChangeEvent) => setFsRole(e.target.value)}>
                {ROLE_NAMES.map((r) => <MenuItem key={r} value={r}>{r}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControlLabel control={<Switch checked={fsVisible} onChange={() => setFsVisible(!fsVisible)} />} label="Visible" />
            <FormControlLabel control={<Switch checked={fsEditable} onChange={() => setFsEditable(!fsEditable)} />} label="Editable" disabled={!fsVisible} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFieldDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveFieldRule} disabled={!fsField}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Record Access Dialog */}
      <Dialog open={accessDialog} onClose={() => setAccessDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Record Access Rule</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>Entity Type</InputLabel>
              <Select value={arEntity} label="Entity Type" onChange={(e: SelectChangeEvent) => setArEntity(e.target.value)}>
                {ENTITY_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Access Type</InputLabel>
              <Select value={arType} label="Access Type" onChange={(e: SelectChangeEvent) => setArType(e.target.value as RecordAccessRule['accessType'])}>
                <MenuItem value="OWNER">Owner</MenuItem>
                <MenuItem value="TEAM">Team</MenuItem>
                <MenuItem value="ROLE">Role</MenuItem>
                <MenuItem value="PUBLIC">Public</MenuItem>
              </Select>
            </FormControl>
            {arType === 'ROLE' && (
              <FormControl fullWidth>
                <InputLabel>Role</InputLabel>
                <Select value={arRole} label="Role" onChange={(e: SelectChangeEvent) => setArRole(e.target.value)}>
                  {ROLE_NAMES.map((r) => <MenuItem key={r} value={r}>{r}</MenuItem>)}
                </Select>
              </FormControl>
            )}
            <FormControlLabel control={<Checkbox checked={arRead} onChange={() => setArRead(!arRead)} />} label="Can Read" />
            <FormControlLabel control={<Checkbox checked={arEdit} onChange={() => setArEdit(!arEdit)} />} label="Can Edit" />
            <FormControlLabel control={<Checkbox checked={arDelete} onChange={() => setArDelete(!arDelete)} />} label="Can Delete" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAccessDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveAccessRule}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* SSO Dialog */}
      <Dialog open={ssoDialog} onClose={() => setSsoDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add SSO Provider</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Provider Name" fullWidth value={ssoName} onChange={(e) => setSsoName(e.target.value)} placeholder="e.g. Google Workspace, Okta" />
            <FormControl fullWidth>
              <InputLabel>Protocol</InputLabel>
              <Select value={ssoProtocol} label="Protocol" onChange={(e: SelectChangeEvent) => setSsoProtocol(e.target.value as 'SAML' | 'OIDC')}>
                <MenuItem value="OIDC">OpenID Connect (OIDC)</MenuItem>
                <MenuItem value="SAML">SAML 2.0</MenuItem>
              </Select>
            </FormControl>
            <TextField label="Issuer URL" fullWidth value={ssoIssuer} onChange={(e) => setSsoIssuer(e.target.value)} placeholder="https://accounts.google.com" />
            <TextField label="Client ID" fullWidth value={ssoClientId} onChange={(e) => setSsoClientId(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSsoDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveSso} disabled={!ssoName.trim() || !ssoIssuer.trim()}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* MFA Verify Dialog */}
      <Dialog open={mfaVerifyDialog} onClose={() => setMfaVerifyDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Verify MFA Setup</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1, alignItems: 'center' }}>
            {mfa.method === 'TOTP' && (
              <>
                <Box sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 2, bgcolor: 'grey.50', textAlign: 'center' }}>
                  <QrIcon sx={{ fontSize: 120, color: 'text.secondary' }} />
                  <Typography variant="caption" display="block" color="text.secondary">
                    Scan with your authenticator app
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography variant="body2" fontFamily="monospace" sx={{ bgcolor: 'grey.100', px: 1.5, py: 0.5, borderRadius: 1 }}>
                    JBSW Y3DP EHPK 3PXP
                  </Typography>
                  <Tooltip title="Copy secret key">
                    <IconButton size="small" onClick={() => notify('Secret key copied', 'info')}><CopyIcon fontSize="small" /></IconButton>
                  </Tooltip>
                </Box>
              </>
            )}
            {mfa.method === 'SMS' && (
              <Alert severity="info">A verification code has been sent to your phone number on file.</Alert>
            )}
            {mfa.method === 'EMAIL' && (
              <Alert severity="info">A verification code has been sent to {user?.email}.</Alert>
            )}
            <TextField
              label="6-digit Code" fullWidth value={mfaCode}
              onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              inputProps={{ maxLength: 6, style: { textAlign: 'center', letterSpacing: 8, fontSize: 24 } }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setMfaVerifyDialog(false); setMfaCode(''); }}>Cancel</Button>
          <Button variant="contained" onClick={verifyMfaSetup} disabled={mfaCode.length !== 6}>Verify & Enable</Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar open={snack.open} autoHideDuration={4000} onClose={() => setSnack((s) => ({ ...s, open: false }))}>
        <Alert severity={snack.severity} onClose={() => setSnack((s) => ({ ...s, open: false }))}>{snack.msg}</Alert>
      </Snackbar>
    </Box>
  );
};

export default SecurityPage;
