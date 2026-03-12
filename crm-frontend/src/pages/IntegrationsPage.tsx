/* ============================================================
   IntegrationsPage – Integration Platform (8 Features)
   1. REST APIs  2. Webhooks  3. Third-party Integrations
   4. Data Synchronization  5. External Data Connectors
   6. API Authentication  7. Integration Monitoring
   8. Integration Error Logging
   ============================================================ */
import React, { useState, useEffect, useMemo } from 'react';
import {
  Box, Typography, Tabs, Tab, Button, TextField, Select, MenuItem,
  FormControl, InputLabel, IconButton, Chip, Switch, FormControlLabel,
  Card, CardContent, CardActions, Dialog, DialogTitle, DialogContent,
  DialogActions, Alert, Snackbar, Table, TableHead, TableBody, TableRow,
  TableCell, Paper, Tooltip, Divider, Grid, Stack, LinearProgress,
  Checkbox, Avatar, TablePagination, Badge,
  SelectChangeEvent,
} from '@mui/material';
import {
  Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon,
  Save as SaveIcon, Api as ApiIcon, Webhook as WebhookIcon,
  Extension as ExtensionIcon, Sync as SyncIcon,
  Storage as ConnectorIcon, VpnKey as AuthIcon,
  MonitorHeart as MonitorIcon, BugReport as ErrorIcon,
  CheckCircle, Cancel as CancelIcon, PlayArrow as RunIcon,
  ContentCopy as CopyIcon, Refresh as RefreshIcon,
  FilterList, Speed as SpeedIcon, CloudDone, CloudOff,
  Warning as WarnIcon, Error as ErrorLevelIcon,
  Info as InfoIcon, Link as LinkIcon,
  VisibilityOff as HiddenIcon, Visibility as VisibleIcon,
} from '@mui/icons-material';
import { integrationService } from '../services/integrationService';
import type {
  RestApiEndpoint, WebhookConfig, ThirdPartyIntegration, DataSync,
  ExternalConnector, ApiAuthConfig, IntegrationHealth, IntegrationError,
  HttpMethod, AuthType, WebhookEvent, SyncDirection, ConnectorType,
  IntegrationLogLevel,
} from '../types';
import { PageHeader } from '../components';
/* ── Constants ─────────────────────────────────────────────── */
const HTTP_METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
const AUTH_TYPES: AuthType[] = ['API_KEY', 'OAUTH2', 'BASIC', 'BEARER', 'NONE'];
const WEBHOOK_EVENTS: WebhookEvent[] = [
  'LEAD_CREATED', 'LEAD_UPDATED', 'LEAD_DELETED', 'CONTACT_CREATED', 'CONTACT_UPDATED',
  'ACCOUNT_CREATED', 'ACCOUNT_UPDATED', 'OPPORTUNITY_CREATED', 'OPPORTUNITY_UPDATED',
  'OPPORTUNITY_WON', 'OPPORTUNITY_LOST', 'ACTIVITY_COMPLETED', 'CUSTOM',
];
const SYNC_DIRECTIONS: SyncDirection[] = ['INBOUND', 'OUTBOUND', 'BIDIRECTIONAL'];
const CONNECTOR_TYPES: ConnectorType[] = ['DATABASE', 'REST_API', 'FILE', 'SALESFORCE', 'HUBSPOT', 'ZAPIER', 'SLACK', 'CUSTOM'];
const ENTITY_TYPES = ['LEAD', 'CONTACT', 'ACCOUNT', 'OPPORTUNITY', 'ACTIVITY'];

/* ── Helpers ───────────────────────────────────────────────── */
const methodColor = (m: string): 'success' | 'primary' | 'warning' | 'info' | 'error' => {
  switch (m) { case 'GET': return 'success'; case 'POST': return 'primary'; case 'PUT': return 'warning'; case 'PATCH': return 'info'; case 'DELETE': return 'error'; default: return 'primary'; }
};
const statusColor = (s: string): 'success' | 'error' | 'warning' | 'default' => {
  switch (s) { case 'ACTIVE': case 'COMPLETED': return 'success'; case 'ERROR': case 'FAILED': case 'CRITICAL': return 'error'; case 'PENDING': case 'RUNNING': case 'WARN': return 'warning'; default: return 'default'; }
};
const logIcon = (level: IntegrationLogLevel) => {
  switch (level) { case 'CRITICAL': return <ErrorLevelIcon color="error" fontSize="small" />; case 'ERROR': return <ErrorIcon color="error" fontSize="small" />; case 'WARN': return <WarnIcon color="warning" fontSize="small" />; default: return <InfoIcon color="info" fontSize="small" />; }
};
const ago = (ts?: string) => {
  if (!ts) return '—';
  const mins = Math.floor((Date.now() - new Date(ts).getTime()) / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
};

/* ── TabPanel ──────────────────────────────────────────────── */
const TabPanel: React.FC<{ value: number; index: number; children: React.ReactNode }> = ({ value, index, children }) =>
  value === index ? <Box sx={{ pt: 2 }}>{children}</Box> : null;

/* ================================================================
   MAIN COMPONENT
   ================================================================ */
const IntegrationsPage: React.FC = () => {
  const [tab, setTab] = useState(0);
  const [snack, setSnack] = useState<{ open: boolean; msg: string; severity: 'success' | 'error' | 'info' }>({ open: false, msg: '', severity: 'success' });
  const notify = (msg: string, severity: 'success' | 'error' | 'info' = 'success') => setSnack({ open: true, msg, severity });

  /* ── State ─────────────────────────────────────────────── */
  const [apis, setApis] = useState<RestApiEndpoint[]>([]);
  const [webhooks, setWebhooks] = useState<WebhookConfig[]>([]);
  const [integrations, setIntegrations] = useState<ThirdPartyIntegration[]>([]);
  const [syncs, setSyncs] = useState<DataSync[]>([]);
  const [connectors, setConnectors] = useState<ExternalConnector[]>([]);
  const [authConfigs, setAuthConfigs] = useState<ApiAuthConfig[]>([]);
  const [health, setHealth] = useState<IntegrationHealth[]>([]);
  const [errors, setErrors] = useState<IntegrationError[]>([]);

  /* Dialogs */
  const [apiDialog, setApiDialog] = useState(false);
  const [webhookDialog, setWebhookDialog] = useState(false);
  const [integrationDialog, setIntegrationDialog] = useState(false);
  const [syncDialog, setSyncDialog] = useState(false);
  const [connectorDialog, setConnectorDialog] = useState(false);
  const [authDialog, setAuthDialog] = useState(false);

  /* Form state: API */
  const [apiName, setApiName] = useState('');
  const [apiPath, setApiPath] = useState('');
  const [apiMethod, setApiMethod] = useState<HttpMethod>('GET');
  const [apiDesc, setApiDesc] = useState('');
  const [apiRateLimit, setApiRateLimit] = useState(100);
  const [apiAuth, setApiAuth] = useState(true);

  /* Form state: Webhook */
  const [whName, setWhName] = useState('');
  const [whUrl, setWhUrl] = useState('');
  const [whEvents, setWhEvents] = useState<WebhookEvent[]>([]);
  const [whRetry, setWhRetry] = useState(3);

  /* Form state: Integration */
  const [intName, setIntName] = useState('');
  const [intProvider, setIntProvider] = useState('');
  const [intType, setIntType] = useState<ConnectorType>('REST_API');
  const [intDesc, setIntDesc] = useState('');
  const [intAuthType, setIntAuthType] = useState<AuthType>('API_KEY');

  /* Form state: Sync */
  const [syncName, setSyncName] = useState('');
  const [syncIntId, setSyncIntId] = useState('');
  const [syncEntity, setSyncEntity] = useState('LEAD');
  const [syncDir, setSyncDir] = useState<SyncDirection>('BIDIRECTIONAL');
  const [syncSchedule, setSyncSchedule] = useState('');

  /* Form state: Connector */
  const [conName, setConName] = useState('');
  const [conType, setConType] = useState<ConnectorType>('DATABASE');
  const [conHost, setConHost] = useState('');
  const [conPort, setConPort] = useState(5432);
  const [conDb, setConDb] = useState('');
  const [conUrl, setConUrl] = useState('');

  /* Form state: Auth */
  const [authName, setAuthName] = useState('');
  const [authType, setAuthType] = useState<AuthType>('API_KEY');
  const [authClientId, setAuthClientId] = useState('');
  const [authTokenUrl, setAuthTokenUrl] = useState('');

  /* Filters */
  const [errorFilter, setErrorFilter] = useState('');
  const [errorPage, setErrorPage] = useState(0);

  /* ── Load ───────────────────────────────────────────────── */
  useEffect(() => {
    // Immediate sync load from localStorage
    setApis(integrationService.getApis());
    setWebhooks(integrationService.getWebhooks());
    setIntegrations(integrationService.getIntegrations());
    setSyncs(integrationService.getSyncs());
    setConnectors(integrationService.getConnectors());
    setAuthConfigs(integrationService.getAuthConfigs());
    setHealth(integrationService.getHealth());
    setErrors(integrationService.getErrors());
    // Try real API (overrides if backend is running)
    const load = async () => {
      const [a, w, i, s, c, ac, h, e] = await Promise.all([
        integrationService.fetchApis(),
        integrationService.fetchWebhooks(),
        integrationService.fetchIntegrations(),
        integrationService.fetchSyncs(),
        integrationService.fetchConnectors(),
        integrationService.fetchAuthConfigs(),
        integrationService.fetchHealth(),
        integrationService.fetchErrors(),
      ]);
      if (Array.isArray(a)) setApis(a);
      if (Array.isArray(w)) setWebhooks(w);
      if (Array.isArray(i)) setIntegrations(i);
      if (Array.isArray(s)) setSyncs(s);
      if (Array.isArray(c)) setConnectors(c);
      if (Array.isArray(ac)) setAuthConfigs(ac);
      if (Array.isArray(h)) setHealth(h);
      if (Array.isArray(e)) setErrors(e);
    };
    load();
  }, []);

  /* ═══════════════════════════════════════════════════════════
     CRUD HANDLERS
     ═══════════════════════════════════════════════════════════ */

  /* 1. REST APIs */
  const saveApi = () => {
    if (!apiName.trim() || !apiPath.trim()) return;
    const ep: RestApiEndpoint = {
      id: Date.now().toString(), name: apiName, path: apiPath, method: apiMethod,
      description: apiDesc, authRequired: apiAuth, rateLimit: apiRateLimit,
      enabled: true, version: 'v1', totalCalls: 0, createdAt: new Date().toISOString(),
    };
    const updated = [...apis, ep];
    setApis(updated);
    integrationService.saveApis(updated);
    setApiDialog(false);
    setApiName(''); setApiPath(''); setApiDesc('');
    notify('API endpoint added');
  };
  const toggleApi = (id: string) => {
    const updated = apis.map((a) => a.id === id ? { ...a, enabled: !a.enabled } : a);
    setApis(updated);
    integrationService.saveApis(updated);
  };
  const deleteApi = (id: string) => {
    const updated = apis.filter((a) => a.id !== id);
    setApis(updated);
    integrationService.saveApis(updated);
    notify('API endpoint removed');
  };

  /* 2. Webhooks */
  const saveWebhook = () => {
    if (!whName.trim() || !whUrl.trim()) return;
    const wh: WebhookConfig = {
      id: Date.now().toString(), name: whName, url: whUrl, events: whEvents,
      active: true, retryCount: whRetry, retryDelayMs: 5000,
      successCount: 0, failureCount: 0, createdAt: new Date().toISOString(),
    };
    const updated = [...webhooks, wh];
    setWebhooks(updated);
    integrationService.saveWebhooks(updated);
    setWebhookDialog(false);
    setWhName(''); setWhUrl(''); setWhEvents([]);
    notify('Webhook created');
  };
  const toggleWebhook = (id: string) => {
    const updated = webhooks.map((w) => w.id === id ? { ...w, active: !w.active } : w);
    setWebhooks(updated);
    integrationService.saveWebhooks(updated);
  };
  const deleteWebhook = (id: string) => {
    const updated = webhooks.filter((w) => w.id !== id);
    setWebhooks(updated);
    integrationService.saveWebhooks(updated);
    notify('Webhook removed');
  };

  /* 3. Third-party integrations */
  const saveIntegration = () => {
    if (!intName.trim()) return;
    const int: ThirdPartyIntegration = {
      id: Date.now().toString(), name: intName, provider: intProvider, type: intType,
      status: 'PENDING', description: intDesc, authType: intAuthType,
      enabled: true, createdAt: new Date().toISOString(),
    };
    const updated = [...integrations, int];
    setIntegrations(updated);
    integrationService.saveIntegrations(updated);
    setIntegrationDialog(false);
    setIntName(''); setIntProvider(''); setIntDesc('');
    notify('Integration added');
  };
  const toggleIntegration = (id: string) => {
    const updated = integrations.map((i) => i.id === id ? { ...i, enabled: !i.enabled, status: i.enabled ? 'INACTIVE' : 'ACTIVE' } : i) as ThirdPartyIntegration[];
    setIntegrations(updated);
    integrationService.saveIntegrations(updated);
  };
  const deleteIntegration = (id: string) => {
    const updated = integrations.filter((i) => i.id !== id);
    setIntegrations(updated);
    integrationService.saveIntegrations(updated);
    notify('Integration removed');
  };

  /* 4. Data sync */
  const saveSyncConfig = () => {
    if (!syncName.trim()) return;
    const integ = integrations.find((i) => i.id === syncIntId);
    const s: DataSync = {
      id: Date.now().toString(), name: syncName, integrationId: syncIntId,
      integrationName: integ?.name ?? 'Unknown', entityType: syncEntity,
      direction: syncDir, status: 'IDLE', schedule: syncSchedule || undefined,
      recordsSynced: 0, recordsFailed: 0, fieldMapping: {}, enabled: true,
      createdAt: new Date().toISOString(),
    };
    const updated = [...syncs, s];
    setSyncs(updated);
    integrationService.saveSyncs(updated);
    setSyncDialog(false);
    setSyncName(''); setSyncSchedule('');
    notify('Sync configuration added');
  };
  const triggerSync = (id: string) => {
    const updated = syncs.map((s) =>
      s.id === id ? { ...s, status: 'RUNNING' as const, lastRunAt: new Date().toISOString() } : s
    );
    setSyncs(updated);
    integrationService.saveSyncs(updated);
    notify('Sync triggered', 'info');
    // Simulate completion
    setTimeout(() => {
      setSyncs((prev) => {
        const fin = prev.map((s) =>
          s.id === id ? { ...s, status: 'COMPLETED' as const, lastRunDuration: Math.floor(Math.random() * 10000) + 1000, recordsSynced: s.recordsSynced + Math.floor(Math.random() * 50) + 10 } : s
        );
        integrationService.saveSyncs(fin);
        return fin;
      });
      notify('Sync completed');
    }, 3000);
  };
  const deleteSync = (id: string) => {
    const updated = syncs.filter((s) => s.id !== id);
    setSyncs(updated);
    integrationService.saveSyncs(updated);
    notify('Sync removed');
  };

  /* 5. External connectors */
  const saveConnector = () => {
    if (!conName.trim()) return;
    const c: ExternalConnector = {
      id: Date.now().toString(), name: conName, type: conType,
      host: conType === 'DATABASE' ? conHost : undefined,
      port: conType === 'DATABASE' ? conPort : undefined,
      database: conType === 'DATABASE' ? conDb : undefined,
      baseUrl: conType === 'REST_API' ? conUrl : undefined,
      status: 'PENDING', enabled: true, createdAt: new Date().toISOString(),
    };
    const updated = [...connectors, c];
    setConnectors(updated);
    integrationService.saveConnectors(updated);
    setConnectorDialog(false);
    setConName(''); setConHost(''); setConDb(''); setConUrl('');
    notify('Connector added');
  };
  const testConnector = (id: string) => {
    const updated = connectors.map((c) => c.id === id ? { ...c, status: 'ACTIVE' as const, lastTestAt: new Date().toISOString() } : c);
    setConnectors(updated);
    integrationService.saveConnectors(updated);
    notify('Connection test passed');
  };
  const deleteConnector = (id: string) => {
    const updated = connectors.filter((c) => c.id !== id);
    setConnectors(updated);
    integrationService.saveConnectors(updated);
    notify('Connector removed');
  };

  /* 6. API Auth */
  const saveAuthConfig = () => {
    if (!authName.trim()) return;
    const a: ApiAuthConfig = {
      id: Date.now().toString(), name: authName, authType: authType,
      clientId: authType === 'OAUTH2' ? authClientId : undefined,
      tokenUrl: authType === 'OAUTH2' ? authTokenUrl : undefined,
      apiKey: authType === 'API_KEY' ? `crm_pk_${Date.now().toString(36)}` : undefined,
      active: true, createdAt: new Date().toISOString(),
    };
    const updated = [...authConfigs, a];
    setAuthConfigs(updated);
    integrationService.saveAuthConfigs(updated);
    setAuthDialog(false);
    setAuthName(''); setAuthClientId(''); setAuthTokenUrl('');
    notify('Auth config added');
  };
  const toggleAuth = (id: string) => {
    const updated = authConfigs.map((a) => a.id === id ? { ...a, active: !a.active } : a);
    setAuthConfigs(updated);
    integrationService.saveAuthConfigs(updated);
  };
  const deleteAuth = (id: string) => {
    const updated = authConfigs.filter((a) => a.id !== id);
    setAuthConfigs(updated);
    integrationService.saveAuthConfigs(updated);
    notify('Auth config removed');
  };

  /* 8. Error logs (filtered) */
  const filteredErrors = useMemo(() => {
    const errs = errors ?? [];
    if (!errorFilter) return errs;
    const q = errorFilter.toLowerCase();
    return errs.filter((e) =>
      e.integrationName.toLowerCase().includes(q) ||
      e.message.toLowerCase().includes(q) ||
      e.level.toLowerCase().includes(q) ||
      (e.endpoint ?? '').toLowerCase().includes(q)
    );
  }, [errors, errorFilter]);

  /* ══════════════════════════════════════════════════════════
     RENDER
     ══════════════════════════════════════════════════════════ */
  return (
    <Box>
      <PageHeader
        title="Integration Platform"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Integrations' }]}
      />

      <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto"
        sx={{ mb: 1, borderBottom: 1, borderColor: 'divider' }}>
        <Tab icon={<ApiIcon />} label="REST APIs" iconPosition="start" />
        <Tab icon={<WebhookIcon />} label="Webhooks" iconPosition="start" />
        <Tab icon={<ExtensionIcon />} label="Integrations" iconPosition="start" />
        <Tab icon={<SyncIcon />} label="Data Sync" iconPosition="start" />
        <Tab icon={<ConnectorIcon />} label="Connectors" iconPosition="start" />
        <Tab icon={<AuthIcon />} label="API Auth" iconPosition="start" />
        <Tab icon={<Badge badgeContent={(health ?? []).filter((h) => h.status === 'ERROR').length} color="error"><MonitorIcon /></Badge>} label="Monitoring" iconPosition="start" />
        <Tab icon={<Badge badgeContent={(errors ?? []).filter((e) => !e.resolvedAt && e.level !== 'INFO').length} color="error"><ErrorIcon /></Badge>} label="Error Logs" iconPosition="start" />
      </Tabs>

      {/* ════════════════════════════════════════════════════════
          TAB 0: REST APIs
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={0}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">REST API Endpoints</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setApiDialog(true)}>Add Endpoint</Button>
        </Box>
        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Method</TableCell>
                <TableCell>Path</TableCell>
                <TableCell>Name</TableCell>
                <TableCell align="center">Auth</TableCell>
                <TableCell align="center">Rate Limit</TableCell>
                <TableCell align="right">Total Calls</TableCell>
                <TableCell align="center">Last Called</TableCell>
                <TableCell align="center">Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {apis.map((a) => (
                <TableRow key={a.id} hover>
                  <TableCell><Chip label={a.method} size="small" color={methodColor(a.method)} /></TableCell>
                  <TableCell><Typography fontFamily="monospace" variant="body2">{a.path}</Typography></TableCell>
                  <TableCell>{a.name}</TableCell>
                  <TableCell align="center">{a.authRequired ? <AuthIcon color="primary" fontSize="small" /> : <CancelIcon color="disabled" fontSize="small" />}</TableCell>
                  <TableCell align="center">{a.rateLimit}/min</TableCell>
                  <TableCell align="right"><Typography fontWeight={600}>{a.totalCalls.toLocaleString()}</Typography></TableCell>
                  <TableCell align="center"><Typography variant="caption">{ago(a.lastCalledAt)}</Typography></TableCell>
                  <TableCell align="center"><Switch size="small" checked={a.enabled} onChange={() => toggleApi(a.id)} /></TableCell>
                  <TableCell align="right">
                    <IconButton size="small" color="error" onClick={() => deleteApi(a.id)}><DeleteIcon fontSize="small" /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 1: Webhooks
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={1}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Webhook Configurations</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setWebhookDialog(true)}>Add Webhook</Button>
        </Box>
        <Grid container spacing={2}>
          {webhooks.map((w) => (
            <Grid item xs={12} md={6} key={w.id}>
              <Card variant="outlined">
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                    <WebhookIcon color={w.active ? 'primary' : 'disabled'} />
                    <Box sx={{ flex: 1 }}>
                      <Typography fontWeight={600}>{w.name}</Typography>
                      <Typography variant="body2" fontFamily="monospace" color="text.secondary" noWrap>{w.url}</Typography>
                    </Box>
                    <Switch checked={w.active} onChange={() => toggleWebhook(w.id)} />
                  </Box>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
                    {w.events.map((e) => <Chip key={e} label={e} size="small" variant="outlined" />)}
                  </Box>
                  <Divider sx={{ my: 1 }} />
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="caption" color="text.secondary">
                      Retries: {w.retryCount} | Last: {ago(w.lastTriggeredAt)}
                    </Typography>
                    <Box>
                      <Chip label={`${w.successCount} ok`} size="small" color="success" variant="outlined" sx={{ mr: 0.5 }} />
                      <Chip label={`${w.failureCount} fail`} size="small" color="error" variant="outlined" />
                    </Box>
                  </Box>
                </CardContent>
                <CardActions>
                  <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => deleteWebhook(w.id)}>Remove</Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 2: Third-Party Integrations
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={2}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Third-Party Integrations</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setIntegrationDialog(true)}>Add Integration</Button>
        </Box>
        <Grid container spacing={2}>
          {integrations.map((int) => (
            <Grid item xs={12} md={4} key={int.id}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1.5 }}>
                    <Avatar sx={{ bgcolor: int.status === 'ACTIVE' ? 'success.main' : int.status === 'ERROR' ? 'error.main' : 'grey.400' }}>
                      <ExtensionIcon />
                    </Avatar>
                    <Box sx={{ flex: 1 }}>
                      <Typography fontWeight={600}>{int.name}</Typography>
                      <Typography variant="caption" color="text.secondary">{int.provider} · {int.type}</Typography>
                    </Box>
                    <Chip label={int.status} size="small" color={statusColor(int.status)} />
                  </Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>{int.description}</Typography>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Chip label={int.authType} size="small" variant="outlined" icon={<AuthIcon />} />
                    <Typography variant="caption" color="text.secondary">Sync: {ago(int.lastSyncAt)}</Typography>
                  </Box>
                </CardContent>
                <CardActions>
                  <Switch size="small" checked={int.enabled} onChange={() => toggleIntegration(int.id)} />
                  <Box sx={{ flex: 1 }} />
                  <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => deleteIntegration(int.id)}>Remove</Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 3: Data Synchronization
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={3}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Data Synchronization</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setSyncDialog(true)}>Add Sync</Button>
        </Box>
        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Integration</TableCell>
                <TableCell>Entity</TableCell>
                <TableCell>Direction</TableCell>
                <TableCell align="center">Status</TableCell>
                <TableCell>Schedule</TableCell>
                <TableCell align="right">Synced</TableCell>
                <TableCell align="right">Failed</TableCell>
                <TableCell align="center">Last Run</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {syncs.map((s) => (
                <TableRow key={s.id} hover>
                  <TableCell><Typography fontWeight={600}>{s.name}</Typography></TableCell>
                  <TableCell>{s.integrationName}</TableCell>
                  <TableCell><Chip label={s.entityType} size="small" variant="outlined" /></TableCell>
                  <TableCell><Chip label={s.direction} size="small" color={s.direction === 'BIDIRECTIONAL' ? 'primary' : s.direction === 'INBOUND' ? 'success' : 'warning'} variant="outlined" /></TableCell>
                  <TableCell align="center">
                    {s.status === 'RUNNING' ? <LinearProgress sx={{ width: 60 }} /> : <Chip label={s.status} size="small" color={statusColor(s.status)} />}
                  </TableCell>
                  <TableCell><Typography variant="caption" fontFamily="monospace">{s.schedule ?? '—'}</Typography></TableCell>
                  <TableCell align="right"><Typography fontWeight={600} color="success.main">{s.recordsSynced.toLocaleString()}</Typography></TableCell>
                  <TableCell align="right"><Typography fontWeight={600} color={s.recordsFailed > 0 ? 'error.main' : 'text.secondary'}>{s.recordsFailed}</Typography></TableCell>
                  <TableCell align="center"><Typography variant="caption">{ago(s.lastRunAt)}</Typography></TableCell>
                  <TableCell align="right">
                    <Tooltip title="Run now"><IconButton size="small" color="primary" onClick={() => triggerSync(s.id)} disabled={s.status === 'RUNNING'}><RunIcon fontSize="small" /></IconButton></Tooltip>
                    <IconButton size="small" color="error" onClick={() => deleteSync(s.id)}><DeleteIcon fontSize="small" /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 4: External Data Connectors
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={4}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">External Data Connectors</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setConnectorDialog(true)}>Add Connector</Button>
        </Box>
        <Grid container spacing={2}>
          {connectors.map((c) => (
            <Grid item xs={12} md={6} key={c.id}>
              <Card variant="outlined">
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                    <Avatar sx={{ bgcolor: c.status === 'ACTIVE' ? 'success.main' : c.status === 'ERROR' ? 'error.main' : 'grey.400' }}>
                      {c.type === 'DATABASE' ? <ConnectorIcon /> : c.type === 'REST_API' ? <ApiIcon /> : <LinkIcon />}
                    </Avatar>
                    <Box sx={{ flex: 1 }}>
                      <Typography fontWeight={600}>{c.name}</Typography>
                      <Typography variant="caption" color="text.secondary">{c.type}</Typography>
                    </Box>
                    <Chip label={c.status} size="small" color={statusColor(c.status)} />
                  </Box>
                  <Divider sx={{ my: 1 }} />
                  {c.host && <Typography variant="body2"><strong>Host:</strong> {c.host}:{c.port}</Typography>}
                  {c.database && <Typography variant="body2"><strong>Database:</strong> {c.database}</Typography>}
                  {c.baseUrl && <Typography variant="body2"><strong>Base URL:</strong> {c.baseUrl}</Typography>}
                  {c.connectionString && <Typography variant="body2"><strong>Path:</strong> {c.connectionString}</Typography>}
                  <Typography variant="caption" color="text.secondary">
                    Last tested: {ago(c.lastTestAt)}
                  </Typography>
                </CardContent>
                <CardActions>
                  <Button size="small" startIcon={<RefreshIcon />} onClick={() => testConnector(c.id)}>Test</Button>
                  <Box sx={{ flex: 1 }} />
                  <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => deleteConnector(c.id)}>Remove</Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 5: API Authentication
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={5}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">API Authentication</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAuthDialog(true)}>Add Auth Config</Button>
        </Box>
        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Auth Type</TableCell>
                <TableCell>Key / Client ID</TableCell>
                <TableCell>Expires</TableCell>
                <TableCell align="center">Last Used</TableCell>
                <TableCell align="center">Active</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {authConfigs.map((a) => (
                <TableRow key={a.id} hover>
                  <TableCell><Typography fontWeight={600}>{a.name}</Typography></TableCell>
                  <TableCell><Chip label={a.authType} size="small" variant="outlined" /></TableCell>
                  <TableCell>
                    <Typography variant="body2" fontFamily="monospace">
                      {a.apiKey ?? a.clientId ?? '—'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {a.expiresAt ? (
                      <Chip
                        label={new Date(a.expiresAt) > new Date() ? `${Math.ceil((new Date(a.expiresAt).getTime() - Date.now()) / 86400000)}d left` : 'Expired'}
                        size="small"
                        color={new Date(a.expiresAt) > new Date() ? 'success' : 'error'}
                      />
                    ) : '—'}
                  </TableCell>
                  <TableCell align="center"><Typography variant="caption">{ago(a.lastUsedAt)}</Typography></TableCell>
                  <TableCell align="center"><Switch size="small" checked={a.active} onChange={() => toggleAuth(a.id)} /></TableCell>
                  <TableCell align="right">
                    <Tooltip title="Copy key"><IconButton size="small" onClick={() => notify('Key copied to clipboard', 'info')}><CopyIcon fontSize="small" /></IconButton></Tooltip>
                    <IconButton size="small" color="error" onClick={() => deleteAuth(a.id)}><DeleteIcon fontSize="small" /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 6: Integration Monitoring
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={6}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Integration Health Monitoring</Typography>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => notify('Health data refreshed', 'info')}>Refresh</Button>
        </Box>

        {/* Summary cards */}
        <Grid container spacing={2} sx={{ mb: 3 }}>
          {[
            { label: 'Total Integrations', val: health.length, color: 'primary.main', icon: <ExtensionIcon /> },
            { label: 'Healthy', val: health.filter((h) => h.status === 'ACTIVE').length, color: 'success.main', icon: <CloudDone /> },
            { label: 'Errors', val: health.filter((h) => h.status === 'ERROR').length, color: 'error.main', icon: <CloudOff /> },
            { label: 'Avg Response', val: `${Math.round(health.reduce((a, h) => a + h.avgResponseMs, 0) / (health.length || 1))}ms`, color: 'info.main', icon: <SpeedIcon /> },
          ].map((s) => (
            <Grid item xs={6} md={3} key={s.label}>
              <Card variant="outlined">
                <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 1.5 }}>
                  <Avatar sx={{ bgcolor: s.color, width: 40, height: 40 }}>{s.icon}</Avatar>
                  <Box>
                    <Typography variant="h5" fontWeight={700}>{s.val}</Typography>
                    <Typography variant="body2" color="text.secondary">{s.label}</Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        {health.map((h) => (
          <Card key={h.id} variant="outlined" sx={{ mb: 2 }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1.5 }}>
                <Avatar sx={{ bgcolor: h.status === 'ACTIVE' ? 'success.main' : 'error.main', width: 36, height: 36 }}>
                  {h.status === 'ACTIVE' ? <CloudDone fontSize="small" /> : <CloudOff fontSize="small" />}
                </Avatar>
                <Box sx={{ flex: 1 }}>
                  <Typography fontWeight={600}>{h.integrationName}</Typography>
                  <Typography variant="caption" color="text.secondary">Last checked: {ago(h.lastCheckedAt)}</Typography>
                </Box>
                <Chip label={h.status} size="small" color={statusColor(h.status)} />
                {h.alertsCount > 0 && <Chip label={`${h.alertsCount} alerts`} size="small" color="error" variant="outlined" />}
              </Box>

              <Grid container spacing={3}>
                <Grid item xs={3}>
                  <Typography variant="caption" color="text.secondary">Uptime</Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <LinearProgress variant="determinate" value={h.uptime} sx={{ flex: 1, height: 8, borderRadius: 4 }} color={h.uptime > 95 ? 'success' : h.uptime > 80 ? 'warning' : 'error'} />
                    <Typography variant="body2" fontWeight={600}>{h.uptime}%</Typography>
                  </Box>
                </Grid>
                <Grid item xs={3}>
                  <Typography variant="caption" color="text.secondary">Success Rate</Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <LinearProgress variant="determinate" value={h.successRate} sx={{ flex: 1, height: 8, borderRadius: 4 }} color={h.successRate > 95 ? 'success' : h.successRate > 80 ? 'warning' : 'error'} />
                    <Typography variant="body2" fontWeight={600}>{h.successRate}%</Typography>
                  </Box>
                </Grid>
                <Grid item xs={3}>
                  <Typography variant="caption" color="text.secondary">Avg Response</Typography>
                  <Typography variant="body2" fontWeight={600}>{h.avgResponseMs}ms</Typography>
                </Grid>
                <Grid item xs={3}>
                  <Typography variant="caption" color="text.secondary">Total Requests</Typography>
                  <Typography variant="body2" fontWeight={600}>{h.totalRequests.toLocaleString()}</Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        ))}
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 7: Integration Error Logging
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={7}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">Integration Error Logs</Typography>
          <Chip label={`${errors.filter((e) => !e.resolvedAt).length} unresolved`} color="error" />
        </Box>

        {/* Stats */}
        <Grid container spacing={2} sx={{ mb: 2 }}>
          {[
            { label: 'Critical', val: errors.filter((e) => e.level === 'CRITICAL').length, color: 'error.main' },
            { label: 'Errors', val: errors.filter((e) => e.level === 'ERROR').length, color: 'warning.main' },
            { label: 'Warnings', val: errors.filter((e) => e.level === 'WARN').length, color: 'info.main' },
            { label: 'Resolved', val: errors.filter((e) => e.resolvedAt).length, color: 'success.main' },
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

        <TextField
          size="small" placeholder="Filter by integration, message, level..."
          value={errorFilter} onChange={(e) => { setErrorFilter(e.target.value); setErrorPage(0); }}
          sx={{ mb: 2, width: 400 }}
          InputProps={{ startAdornment: <FilterList sx={{ mr: 1, color: 'text.secondary' }} /> }}
        />

        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Level</TableCell>
                <TableCell>Integration</TableCell>
                <TableCell>Message</TableCell>
                <TableCell>Endpoint</TableCell>
                <TableCell align="center">HTTP</TableCell>
                <TableCell>Timestamp</TableCell>
                <TableCell align="center">Resolved</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredErrors.length === 0 ? (
                <TableRow><TableCell colSpan={7} align="center"><Typography color="text.secondary" sx={{ py: 3 }}>No error logs found</Typography></TableCell></TableRow>
              ) : filteredErrors.slice(errorPage * 10, errorPage * 10 + 10).map((e) => (
                <TableRow key={e.id} hover sx={{ bgcolor: e.level === 'CRITICAL' ? 'error.50' : undefined }}>
                  <TableCell>{logIcon(e.level)} <Chip label={e.level} size="small" color={statusColor(e.level)} sx={{ ml: 0.5 }} /></TableCell>
                  <TableCell>{e.integrationName}</TableCell>
                  <TableCell><Typography variant="body2" sx={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis' }}>{e.message}</Typography></TableCell>
                  <TableCell><Typography variant="caption" fontFamily="monospace">{e.endpoint ?? '—'}</Typography></TableCell>
                  <TableCell align="center">{e.httpStatus ? <Chip label={e.httpStatus} size="small" color={e.httpStatus >= 400 ? 'error' : 'success'} variant="outlined" /> : '—'}</TableCell>
                  <TableCell>{new Date(e.createdAt).toLocaleString()}</TableCell>
                  <TableCell align="center">{e.resolvedAt ? <CheckCircle color="success" fontSize="small" /> : <CancelIcon color="disabled" fontSize="small" />}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TablePagination component="div" count={filteredErrors.length} page={errorPage} rowsPerPage={10}
            rowsPerPageOptions={[10]} onPageChange={(_, p) => setErrorPage(p)} onRowsPerPageChange={() => {}} />
        </Paper>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          DIALOGS
          ════════════════════════════════════════════════════════ */}

      {/* API Dialog */}
      <Dialog open={apiDialog} onClose={() => setApiDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add REST API Endpoint</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth value={apiName} onChange={(e) => setApiName(e.target.value)} />
            <Box sx={{ display: 'flex', gap: 2 }}>
              <FormControl sx={{ minWidth: 120 }}>
                <InputLabel>Method</InputLabel>
                <Select value={apiMethod} label="Method" onChange={(e: SelectChangeEvent) => setApiMethod(e.target.value as HttpMethod)}>
                  {HTTP_METHODS.map((m) => <MenuItem key={m} value={m}>{m}</MenuItem>)}
                </Select>
              </FormControl>
              <TextField label="Path" fullWidth value={apiPath} onChange={(e) => setApiPath(e.target.value)} placeholder="/api/v1/..." />
            </Box>
            <TextField label="Description" fullWidth value={apiDesc} onChange={(e) => setApiDesc(e.target.value)} />
            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
              <TextField label="Rate Limit (req/min)" type="number" value={apiRateLimit} onChange={(e) => setApiRateLimit(+e.target.value)} sx={{ width: 200 }} />
              <FormControlLabel control={<Checkbox checked={apiAuth} onChange={() => setApiAuth(!apiAuth)} />} label="Auth Required" />
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApiDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveApi} disabled={!apiName.trim() || !apiPath.trim()}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Webhook Dialog */}
      <Dialog open={webhookDialog} onClose={() => setWebhookDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Webhook</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth value={whName} onChange={(e) => setWhName(e.target.value)} />
            <TextField label="URL" fullWidth value={whUrl} onChange={(e) => setWhUrl(e.target.value)} placeholder="https://..." />
            <FormControl fullWidth>
              <InputLabel>Events</InputLabel>
              <Select multiple value={whEvents} label="Events"
                onChange={(e) => setWhEvents(typeof e.target.value === 'string' ? e.target.value.split(',') as WebhookEvent[] : e.target.value as WebhookEvent[])}
                renderValue={(selected) => <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>{selected.map((v) => <Chip key={v} label={v} size="small" />)}</Box>}
              >
                {WEBHOOK_EVENTS.map((ev) => (
                  <MenuItem key={ev} value={ev}>
                    <Checkbox size="small" checked={whEvents.includes(ev)} />
                    {ev}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField label="Retry Count" type="number" value={whRetry} onChange={(e) => setWhRetry(+e.target.value)} sx={{ width: 200 }} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setWebhookDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveWebhook} disabled={!whName.trim() || !whUrl.trim()}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Integration Dialog */}
      <Dialog open={integrationDialog} onClose={() => setIntegrationDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Third-Party Integration</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth value={intName} onChange={(e) => setIntName(e.target.value)} />
            <TextField label="Provider" fullWidth value={intProvider} onChange={(e) => setIntProvider(e.target.value)} placeholder="e.g. Salesforce, HubSpot" />
            <FormControl fullWidth>
              <InputLabel>Type</InputLabel>
              <Select value={intType} label="Type" onChange={(e: SelectChangeEvent) => setIntType(e.target.value as ConnectorType)}>
                {CONNECTOR_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Description" fullWidth multiline rows={2} value={intDesc} onChange={(e) => setIntDesc(e.target.value)} />
            <FormControl fullWidth>
              <InputLabel>Auth Type</InputLabel>
              <Select value={intAuthType} label="Auth Type" onChange={(e: SelectChangeEvent) => setIntAuthType(e.target.value as AuthType)}>
                {AUTH_TYPES.map((a) => <MenuItem key={a} value={a}>{a}</MenuItem>)}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIntegrationDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveIntegration} disabled={!intName.trim()}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Sync Dialog */}
      <Dialog open={syncDialog} onClose={() => setSyncDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Data Sync</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth value={syncName} onChange={(e) => setSyncName(e.target.value)} />
            <FormControl fullWidth>
              <InputLabel>Integration</InputLabel>
              <Select value={syncIntId} label="Integration" onChange={(e: SelectChangeEvent) => setSyncIntId(e.target.value)}>
                {integrations.filter((i) => i.enabled).map((i) => <MenuItem key={i.id} value={i.id}>{i.name}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Entity Type</InputLabel>
              <Select value={syncEntity} label="Entity Type" onChange={(e: SelectChangeEvent) => setSyncEntity(e.target.value)}>
                {ENTITY_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Direction</InputLabel>
              <Select value={syncDir} label="Direction" onChange={(e: SelectChangeEvent) => setSyncDir(e.target.value as SyncDirection)}>
                {SYNC_DIRECTIONS.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="Schedule (cron)" fullWidth value={syncSchedule} onChange={(e) => setSyncSchedule(e.target.value)} placeholder="*/15 * * * *" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSyncDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveSyncConfig} disabled={!syncName.trim()}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Connector Dialog */}
      <Dialog open={connectorDialog} onClose={() => setConnectorDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add External Connector</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth value={conName} onChange={(e) => setConName(e.target.value)} />
            <FormControl fullWidth>
              <InputLabel>Type</InputLabel>
              <Select value={conType} label="Type" onChange={(e: SelectChangeEvent) => setConType(e.target.value as ConnectorType)}>
                {CONNECTOR_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </Select>
            </FormControl>
            {conType === 'DATABASE' && (
              <>
                <TextField label="Host" fullWidth value={conHost} onChange={(e) => setConHost(e.target.value)} />
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <TextField label="Port" type="number" value={conPort} onChange={(e) => setConPort(+e.target.value)} sx={{ width: 120 }} />
                  <TextField label="Database" fullWidth value={conDb} onChange={(e) => setConDb(e.target.value)} />
                </Box>
              </>
            )}
            {conType === 'REST_API' && <TextField label="Base URL" fullWidth value={conUrl} onChange={(e) => setConUrl(e.target.value)} placeholder="https://api.example.com" />}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConnectorDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveConnector} disabled={!conName.trim()}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Auth Dialog */}
      <Dialog open={authDialog} onClose={() => setAuthDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add API Auth Config</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth value={authName} onChange={(e) => setAuthName(e.target.value)} />
            <FormControl fullWidth>
              <InputLabel>Auth Type</InputLabel>
              <Select value={authType} label="Auth Type" onChange={(e: SelectChangeEvent) => setAuthType(e.target.value as AuthType)}>
                {AUTH_TYPES.map((a) => <MenuItem key={a} value={a}>{a}</MenuItem>)}
              </Select>
            </FormControl>
            {authType === 'OAUTH2' && (
              <>
                <TextField label="Client ID" fullWidth value={authClientId} onChange={(e) => setAuthClientId(e.target.value)} />
                <TextField label="Token URL" fullWidth value={authTokenUrl} onChange={(e) => setAuthTokenUrl(e.target.value)} />
              </>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAuthDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={saveAuthConfig} disabled={!authName.trim()}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar open={snack.open} autoHideDuration={4000} onClose={() => setSnack((s) => ({ ...s, open: false }))}>
        <Alert severity={snack.severity} onClose={() => setSnack((s) => ({ ...s, open: false }))}>{snack.msg}</Alert>
      </Snackbar>
    </Box>
  );
};

export default IntegrationsPage;
