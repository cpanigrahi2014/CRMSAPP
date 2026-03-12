import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Tabs, Tab, Paper, Button, Grid, Card, CardContent,
  Chip, IconButton, TextField, Dialog, DialogTitle, DialogContent,
  DialogActions, Table, TableHead, TableRow, TableCell, TableBody,
  Alert, Tooltip, Select, MenuItem, FormControl, InputLabel, Rating,
  CardActions, Stack, Switch, FormControlLabel, Snackbar,
} from '@mui/material';
import {
  VpnKey as KeyIcon, Webhook as WebhookIcon, Code as CodeIcon,
  Widgets as WidgetsIcon, Store as StoreIcon, Apps as AppsIcon,
  Add as AddIcon, Delete as DeleteIcon, ContentCopy as CopyIcon,
  Send as SendIcon, Download as InstallIcon, Block as RevokeIcon,
  Publish as PublishIcon, CheckCircle, Error as ErrorIcon,
  Refresh as RefreshIcon, Search as SearchIcon, Verified as VerifiedIcon,
} from '@mui/icons-material';
import { developerPlatformService } from '../services/developerPlatformService';
import type {
  DeveloperApiKey, WebhookDeliveryLog, MarketplacePlugin,
  PluginInstallation, EmbeddableWidget, CustomApp,
  CreateApiKeyRequest, CreateWidgetRequest, CreateCustomAppRequest,
} from '../types';
import { PageHeader } from '../components';
interface TabPanelProps { children?: React.ReactNode; index: number; value: number; }
function TabPanel({ children, value, index }: TabPanelProps) {
  return <Box hidden={value !== index} sx={{ pt: 2 }}>{value === index && children}</Box>;
}

export default function DeveloperPortalPage() {
  const [tab, setTab] = useState(0);

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="Developer Portal"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Developer Portal' }]}
      />
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        API keys, webhooks, marketplace plugins, embeddable widgets, and custom app builder
      </Typography>
      <Paper sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto">
          <Tab icon={<CodeIcon />} iconPosition="start" label="API Explorer" />
          <Tab icon={<WebhookIcon />} iconPosition="start" label="Webhooks" />
          <Tab icon={<KeyIcon />} iconPosition="start" label="API Keys" />
          <Tab icon={<AppsIcon />} iconPosition="start" label="App Builder" />
          <Tab icon={<WidgetsIcon />} iconPosition="start" label="Widgets" />
          <Tab icon={<StoreIcon />} iconPosition="start" label="Marketplace" />
        </Tabs>
      </Paper>
      <TabPanel value={tab} index={0}><ApiExplorerTab /></TabPanel>
      <TabPanel value={tab} index={1}><WebhooksTab /></TabPanel>
      <TabPanel value={tab} index={2}><ApiKeysTab /></TabPanel>
      <TabPanel value={tab} index={3}><AppBuilderTab /></TabPanel>
      <TabPanel value={tab} index={4}><WidgetsTab /></TabPanel>
      <TabPanel value={tab} index={5}><MarketplaceTab /></TabPanel>
    </Box>
  );
}

/* ═══════════════════════════════════════════════════════════════
   TAB 0 — API Explorer / SDK
   ═══════════════════════════════════════════════════════════════ */
function ApiExplorerTab() {
  const [lang, setLang] = useState<'curl' | 'javascript' | 'python'>('curl');

  const endpoints = [
    { method: 'GET', path: '/api/v1/leads', desc: 'List all leads' },
    { method: 'POST', path: '/api/v1/leads', desc: 'Create a lead' },
    { method: 'GET', path: '/api/v1/contacts', desc: 'List contacts' },
    { method: 'GET', path: '/api/v1/opportunities', desc: 'List opportunities' },
    { method: 'GET', path: '/api/v1/accounts', desc: 'List accounts' },
    { method: 'POST', path: '/api/v1/webhooks', desc: 'Create webhook config' },
    { method: 'GET', path: '/api/v1/activities', desc: 'List activities' },
    { method: 'GET', path: '/api/v1/workflows/rules', desc: 'List workflow rules' },
  ];

  const methodColor = (m: string) =>
    m === 'GET' ? 'success' : m === 'POST' ? 'primary' : m === 'PUT' ? 'warning' : 'error';

  const sdkExamples: Record<string, string> = {
    curl: `curl -X GET "https://api.yourcrm.com/api/v1/leads" \\
  -H "Authorization: Bearer crm_your_api_key" \\
  -H "Content-Type: application/json"`,
    javascript: `import CrmSDK from '@crm/sdk';

const crm = new CrmSDK({ apiKey: 'crm_your_api_key' });

const leads = await crm.leads.list({ page: 1, size: 20 });
const newLead = await crm.leads.create({
  firstName: 'John', lastName: 'Doe',
  email: 'john@example.com', company: 'Acme Inc'
});`,
    python: `from crm_sdk import CrmClient

crm = CrmClient(api_key='crm_your_api_key')

leads = crm.leads.list(page=1, size=20)
new_lead = crm.leads.create(
    first_name='John', last_name='Doe',
    email='john@example.com', company='Acme Inc'
)`,
  };

  return (
    <Box>
      <Alert severity="info" sx={{ mb: 3 }}>
        All API endpoints support JSON and require Bearer token authentication. OpenAPI/Swagger docs available at each service's /swagger-ui endpoint.
      </Alert>
      <Grid container spacing={3}>
        <Grid item xs={12} md={7}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>REST API Endpoints</Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Method</TableCell>
                  <TableCell>Path</TableCell>
                  <TableCell>Description</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {endpoints.map((ep, i) => (
                  <TableRow key={i}>
                    <TableCell><Chip label={ep.method} color={methodColor(ep.method) as 'success'} size="small" /></TableCell>
                    <TableCell><code>{ep.path}</code></TableCell>
                    <TableCell>{ep.desc}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        </Grid>
        <Grid item xs={12} md={5}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>SDK & Code Examples</Typography>
            <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
              {(['curl', 'javascript', 'python'] as const).map(l => (
                <Button key={l} variant={lang === l ? 'contained' : 'outlined'} size="small" onClick={() => setLang(l)}>
                  {l}
                </Button>
              ))}
            </Stack>
            <Box sx={{ bgcolor: 'grey.900', color: 'grey.100', p: 2, borderRadius: 1, fontFamily: 'monospace', fontSize: 13, whiteSpace: 'pre-wrap', maxHeight: 300, overflow: 'auto' }}>
              {sdkExamples[lang]}
            </Box>
            <Alert severity="info" sx={{ mt: 2 }}>
              Install SDK: <code>npm install @crm/sdk</code> or <code>pip install crm-sdk</code>
            </Alert>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}

/* ═══════════════════════════════════════════════════════════════
   TAB 1 — Webhooks / Delivery Logs
   ═══════════════════════════════════════════════════════════════ */
function WebhooksTab() {
  const [logs, setLogs] = useState<WebhookDeliveryLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [snack, setSnack] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    const data = await developerPlatformService.getDeliveryLogs();
    setLogs(data);
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleTest = async (webhookId: string) => {
    const result = await developerPlatformService.testWebhook(webhookId);
    setSnack(`Test sent! Status: ${result.status}`);
    load();
  };

  const statusColor = (s: string) =>
    s === 'SUCCESS' ? 'success' : s === 'FAILED' ? 'error' : s === 'RETRYING' ? 'warning' : 'default';

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6">Webhook Delivery Logs</Typography>
        <Button startIcon={<RefreshIcon />} onClick={load}>Refresh</Button>
      </Box>
      <Alert severity="info" sx={{ mb: 2 }}>
        Webhooks are triggered by CRM events (lead.created, contact.updated, opportunity.won, etc). Configure webhook endpoints in the Integrations page.
        Supported events: LEAD_CREATED, LEAD_UPDATED, CONTACT_CREATED, CONTACT_UPDATED, OPPORTUNITY_CREATED, OPPORTUNITY_WON, OPPORTUNITY_LOST, ACTIVITY_CREATED, DEAL_CLOSED, WORKFLOW_TRIGGERED, ACCOUNT_CREATED, EMAIL_SENT, NOTE_ADDED
      </Alert>
      {!loading && (
        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Webhook</TableCell>
                <TableCell>Event</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Response</TableCell>
                <TableCell>Attempt</TableCell>
                <TableCell>Time</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {logs.map(l => (
                <TableRow key={l.id}>
                  <TableCell>{l.webhookName}</TableCell>
                  <TableCell><Chip label={l.eventType} size="small" variant="outlined" /></TableCell>
                  <TableCell><Chip label={l.status} color={statusColor(l.status) as 'success'} size="small" /></TableCell>
                  <TableCell>{l.responseStatus > 0 ? l.responseStatus : '-'}</TableCell>
                  <TableCell>{l.attempt}</TableCell>
                  <TableCell>{new Date(l.createdAt).toLocaleString()}</TableCell>
                  <TableCell>
                    <Tooltip title="Resend test">
                      <IconButton size="small" onClick={() => handleTest(l.webhookId)}><SendIcon fontSize="small" /></IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
              {logs.length === 0 && (
                <TableRow><TableCell colSpan={7} align="center">No delivery logs yet</TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </Paper>
      )}
      <Snackbar open={!!snack} autoHideDuration={3000} onClose={() => setSnack('')} message={snack} />
    </Box>
  );
}

/* ═══════════════════════════════════════════════════════════════
   TAB 2 — API Keys
   ═══════════════════════════════════════════════════════════════ */
function ApiKeysTab() {
  const [keys, setKeys] = useState<DeveloperApiKey[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<CreateApiKeyRequest>({ name: '', scopes: ['read', 'write'], rateLimit: 1000 });
  const [snack, setSnack] = useState('');

  const load = useCallback(async () => { setKeys(await developerPlatformService.getApiKeys()); }, []);
  useEffect(() => { load(); }, [load]);

  const handleCreate = async () => {
    if (!form.name.trim()) return;
    const created = await developerPlatformService.createApiKey(form);
    if (created.rawKey) setSnack(`Key created! Raw key (save now): ${created.rawKey}`);
    setDialogOpen(false);
    setForm({ name: '', scopes: ['read', 'write'], rateLimit: 1000 });
    load();
  };

  const handleRevoke = async (id: string) => { await developerPlatformService.revokeApiKey(id); load(); };
  const handleDelete = async (id: string) => { await developerPlatformService.deleteApiKey(id); load(); };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6">API Keys</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>Generate Key</Button>
      </Box>
      <Alert severity="warning" sx={{ mb: 2 }}>
        API keys grant access to your CRM data. Keep them secure and never share them publicly. Raw keys are shown only once at creation.
      </Alert>
      <Grid container spacing={2}>
        {keys.map(k => (
          <Grid item xs={12} md={6} lg={4} key={k.id}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="subtitle1" fontWeight={600}>{k.name}</Typography>
                  <Chip label={k.active ? 'Active' : 'Revoked'} color={k.active ? 'success' : 'error'} size="small" />
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
                  {k.keyPrefix}••••••••
                </Typography>
                <Box sx={{ mt: 1 }}>
                  <Stack direction="row" spacing={0.5} flexWrap="wrap">
                    {k.scopes.map(s => <Chip key={s} label={s} size="small" variant="outlined" />)}
                  </Stack>
                </Box>
                <Box sx={{ mt: 1, display: 'flex', gap: 2 }}>
                  <Typography variant="caption">Rate: {k.rateLimit}/hr</Typography>
                  <Typography variant="caption">Today: {k.callsToday}</Typography>
                  <Typography variant="caption">Total: {k.totalCalls.toLocaleString()}</Typography>
                </Box>
                {k.expiresAt && <Typography variant="caption" color="warning.main">Expires: {new Date(k.expiresAt).toLocaleDateString()}</Typography>}
              </CardContent>
              <CardActions>
                <Button size="small" color="warning" startIcon={<RevokeIcon />} onClick={() => handleRevoke(k.id)} disabled={!k.active}>Revoke</Button>
                <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleDelete(k.id)}>Delete</Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Generate API Key</DialogTitle>
        <DialogContent>
          <TextField label="Key Name" fullWidth sx={{ mt: 1, mb: 2 }} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
          <TextField label="Rate Limit (per hour)" type="number" fullWidth sx={{ mb: 2 }} value={form.rateLimit} onChange={e => setForm({ ...form, rateLimit: parseInt(e.target.value) || 1000 })} />
          <FormControl fullWidth>
            <InputLabel>Scopes</InputLabel>
            <Select multiple value={form.scopes || []} onChange={e => setForm({ ...form, scopes: e.target.value as string[] })} label="Scopes">
              {['read', 'write', 'admin', 'contacts', 'leads', 'opportunities', 'activities', 'analytics'].map(s => (
                <MenuItem key={s} value={s}>{s}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate}>Generate</Button>
        </DialogActions>
      </Dialog>
      <Snackbar open={!!snack} autoHideDuration={10000} onClose={() => setSnack('')} message={snack} />
    </Box>
  );
}

/* ═══════════════════════════════════════════════════════════════
   TAB 3 — Low-Code App Builder
   ═══════════════════════════════════════════════════════════════ */
function AppBuilderTab() {
  const [apps, setApps] = useState<CustomApp[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<CreateCustomAppRequest>({ name: '', description: '', appType: 'DASHBOARD' });
  const [snack, setSnack] = useState('');

  const load = useCallback(async () => { setApps(await developerPlatformService.getCustomApps()); }, []);
  useEffect(() => { load(); }, [load]);

  const handleCreate = async () => {
    if (!form.name.trim()) return;
    await developerPlatformService.createCustomApp(form);
    setDialogOpen(false);
    setForm({ name: '', description: '', appType: 'DASHBOARD' });
    load();
  };

  const handlePublish = async (id: string) => {
    await developerPlatformService.publishCustomApp(id);
    setSnack('App published!');
    load();
  };

  const handleDelete = async (id: string) => {
    await developerPlatformService.deleteCustomApp(id);
    load();
  };

  const typeIcon = (t: string) =>
    t === 'FORM' ? '📝' : t === 'DASHBOARD' ? '📊' : t === 'WORKFLOW' ? '⚙️' : '📄';

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6">Custom Apps (Low-Code Builder)</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>Create App</Button>
      </Box>
      <Alert severity="info" sx={{ mb: 2 }}>
        Build custom forms, dashboards, pages, and workflows without writing code. Configure data sources, layouts, and styling visually.
      </Alert>
      <Grid container spacing={2}>
        {apps.map(app => (
          <Grid item xs={12} md={6} lg={4} key={app.id}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="h6">{typeIcon(app.appType)} {app.name}</Typography>
                  <Chip label={app.status} size="small"
                    color={app.status === 'PUBLISHED' ? 'success' : app.status === 'DRAFT' ? 'warning' : 'default'} />
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>{app.description || 'No description'}</Typography>
                <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                  <Chip label={app.appType} size="small" variant="outlined" />
                  {app.publishedVersion && <Chip label={`v${app.publishedVersion}`} size="small" />}
                </Stack>
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                  /{app.slug} &bull; Created {new Date(app.createdAt).toLocaleDateString()}
                </Typography>
              </CardContent>
              <CardActions>
                {app.status !== 'PUBLISHED' && (
                  <Button size="small" color="primary" startIcon={<PublishIcon />} onClick={() => handlePublish(app.id)}>Publish</Button>
                )}
                <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleDelete(app.id)}>Delete</Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Custom App</DialogTitle>
        <DialogContent>
          <TextField label="App Name" fullWidth sx={{ mt: 1, mb: 2 }} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
          <TextField label="Description" fullWidth multiline rows={2} sx={{ mb: 2 }} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
          <FormControl fullWidth>
            <InputLabel>App Type</InputLabel>
            <Select value={form.appType || 'DASHBOARD'} onChange={e => setForm({ ...form, appType: e.target.value })} label="App Type">
              <MenuItem value="DASHBOARD">Dashboard</MenuItem>
              <MenuItem value="FORM">Form</MenuItem>
              <MenuItem value="PAGE">Page</MenuItem>
              <MenuItem value="WORKFLOW">Workflow</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate}>Create</Button>
        </DialogActions>
      </Dialog>
      <Snackbar open={!!snack} autoHideDuration={3000} onClose={() => setSnack('')} message={snack} />
    </Box>
  );
}

/* ═══════════════════════════════════════════════════════════════
   TAB 4 — Embeddable Widgets
   ═══════════════════════════════════════════════════════════════ */
function WidgetsTab() {
  const [widgets, setWidgets] = useState<EmbeddableWidget[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<CreateWidgetRequest>({ name: '', widgetType: 'CHART', description: '' });
  const [snack, setSnack] = useState('');

  const load = useCallback(async () => { setWidgets(await developerPlatformService.getWidgets()); }, []);
  useEffect(() => { load(); }, [load]);

  const handleCreate = async () => {
    if (!form.name.trim()) return;
    await developerPlatformService.createWidget(form);
    setDialogOpen(false);
    setForm({ name: '', widgetType: 'CHART', description: '' });
    load();
  };

  const copyCode = (code: string) => {
    navigator.clipboard.writeText(code);
    setSnack('Embed code copied!');
  };

  const handleDelete = async (id: string) => { await developerPlatformService.deleteWidget(id); load(); };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6">Embeddable Widgets</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>Create Widget</Button>
      </Box>
      <Alert severity="info" sx={{ mb: 2 }}>
        Create widgets that can be embedded on external websites. Copy the embed code and paste it into any HTML page. Configure allowed domains for security.
      </Alert>
      <Grid container spacing={2}>
        {widgets.map(w => (
          <Grid item xs={12} md={6} key={w.id}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="h6">{w.name}</Typography>
                  <Stack direction="row" spacing={1}>
                    <Chip label={w.widgetType} size="small" color="primary" variant="outlined" />
                    <Chip icon={w.active ? <CheckCircle /> : <ErrorIcon />} label={w.active ? 'Active' : 'Inactive'}
                      size="small" color={w.active ? 'success' : 'default'} />
                  </Stack>
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>{w.description || 'No description'}</Typography>
                <Box sx={{ mt: 2, p: 1.5, bgcolor: 'grey.100', borderRadius: 1, fontFamily: 'monospace', fontSize: 12, wordBreak: 'break-all' }}>
                  {w.embedCode}
                </Box>
                <Stack direction="row" spacing={2} sx={{ mt: 1 }}>
                  <Typography variant="caption">Token: {w.embedToken}</Typography>
                  <Typography variant="caption">Views: {w.viewCount.toLocaleString()}</Typography>
                  {w.allowedDomains && w.allowedDomains.length > 0 && (
                    <Typography variant="caption">Domains: {w.allowedDomains.join(', ')}</Typography>
                  )}
                </Stack>
              </CardContent>
              <CardActions>
                <Button size="small" startIcon={<CopyIcon />} onClick={() => copyCode(w.embedCode)}>Copy Code</Button>
                <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => handleDelete(w.id)}>Delete</Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Widget</DialogTitle>
        <DialogContent>
          <TextField label="Widget Name" fullWidth sx={{ mt: 1, mb: 2 }} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
          <TextField label="Description" fullWidth multiline rows={2} sx={{ mb: 2 }} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
          <FormControl fullWidth>
            <InputLabel>Widget Type</InputLabel>
            <Select value={form.widgetType || 'CHART'} onChange={e => setForm({ ...form, widgetType: e.target.value })} label="Widget Type">
              <MenuItem value="CHART">Chart</MenuItem>
              <MenuItem value="FORM">Form</MenuItem>
              <MenuItem value="TABLE">Table</MenuItem>
              <MenuItem value="METRIC">Metric</MenuItem>
              <MenuItem value="TIMELINE">Timeline</MenuItem>
              <MenuItem value="CUSTOM">Custom</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate}>Create</Button>
        </DialogActions>
      </Dialog>
      <Snackbar open={!!snack} autoHideDuration={3000} onClose={() => setSnack('')} message={snack} />
    </Box>
  );
}

/* ═══════════════════════════════════════════════════════════════
   TAB 5 — Marketplace
   ═══════════════════════════════════════════════════════════════ */
function MarketplaceTab() {
  const [plugins, setPlugins] = useState<MarketplacePlugin[]>([]);
  const [installed, setInstalled] = useState<PluginInstallation[]>([]);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('All');
  const [snack, setSnack] = useState('');

  const load = useCallback(async () => {
    setPlugins(await developerPlatformService.getMarketplacePlugins());
    setInstalled(await developerPlatformService.getInstalledPlugins());
  }, []);
  useEffect(() => { load(); }, [load]);

  const handleInstall = async (pluginId: string) => {
    await developerPlatformService.installPlugin(pluginId);
    setSnack('Plugin installed!');
    load();
  };

  const handleUninstall = async (pluginId: string) => {
    await developerPlatformService.uninstallPlugin(pluginId);
    setSnack('Plugin uninstalled');
    load();
  };

  const categories = ['All', ...new Set(plugins.map(p => p.category))];
  const filtered = plugins.filter(p => {
    if (category !== 'All' && p.category !== category) return false;
    if (search && !p.name.toLowerCase().includes(search.toLowerCase()) && !p.description?.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  return (
    <Box>
      <Typography variant="h6" gutterBottom>Plugin Marketplace</Typography>
      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        <TextField placeholder="Search plugins..." size="small" sx={{ width: 300 }}
          InputProps={{ startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} /> }}
          value={search} onChange={e => setSearch(e.target.value)} />
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <Select value={category} onChange={e => setCategory(e.target.value)}>
            {categories.map(c => <MenuItem key={c} value={c}>{c}</MenuItem>)}
          </Select>
        </FormControl>
        <Chip label={`${installed.length} installed`} color="primary" />
      </Box>

      <Grid container spacing={2}>
        {filtered.map(p => (
          <Grid item xs={12} md={6} lg={4} key={p.id}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <CardContent sx={{ flex: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography variant="h6">{p.name}</Typography>
                      {p.isVerified && <Tooltip title="Verified"><VerifiedIcon color="primary" fontSize="small" /></Tooltip>}
                    </Box>
                    <Typography variant="caption" color="text.secondary">by {p.author} &bull; v{p.version}</Typography>
                  </Box>
                  <Chip label={p.pricing} size="small" color={p.pricing === 'FREE' ? 'success' : p.pricing === 'FREEMIUM' ? 'info' : 'warning'} />
                </Box>
                <Typography variant="body2" sx={{ mt: 1 }}>{p.description}</Typography>
                <Box sx={{ mt: 1.5, display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Rating value={p.rating} precision={0.1} size="small" readOnly />
                    <Typography variant="caption">({p.ratingCount})</Typography>
                  </Box>
                  <Typography variant="caption">{p.installCount.toLocaleString()} installs</Typography>
                </Box>
                <Stack direction="row" spacing={0.5} sx={{ mt: 1 }} flexWrap="wrap">
                  <Chip label={p.category} size="small" variant="outlined" />
                  {p.priceAmount && p.priceAmount > 0 && <Chip label={`$${p.priceAmount}/mo`} size="small" />}
                </Stack>
              </CardContent>
              <CardActions>
                {p.installed ? (
                  <Button size="small" color="error" onClick={() => handleUninstall(p.id)}>Uninstall</Button>
                ) : (
                  <Button size="small" variant="contained" startIcon={<InstallIcon />} onClick={() => handleInstall(p.id)}>Install</Button>
                )}
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>
      <Snackbar open={!!snack} autoHideDuration={3000} onClose={() => setSnack('')} message={snack} />
    </Box>
  );
}
