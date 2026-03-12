import api from './api';
import type {
  WebhookDeliveryLog, DeveloperApiKey, CreateApiKeyRequest,
  MarketplacePlugin, CreatePluginRequest, PluginInstallation,
  EmbeddableWidget, CreateWidgetRequest,
  CustomApp, CreateCustomAppRequest,
} from '../types';

const BASE = '/api/v1/developer';

/* ── localStorage helpers ────────────────────────────────────── */
function loadJson<T>(key: string, fallback: T): T {
  try { const raw = localStorage.getItem(key); return raw ? JSON.parse(raw) : fallback; } catch { return fallback; }
}
function saveJson<T>(key: string, data: T): void { localStorage.setItem(key, JSON.stringify(data)); }
async function tryApi<T>(apiFn: () => Promise<T>, fallback: T): Promise<T> {
  try { const r = await apiFn(); return r ?? fallback; } catch { return fallback; }
}
const now = new Date().toISOString();

/* ── Storage keys ────────────────────────────────────────────── */
const K = {
  apiKeys: 'crm_dev_api_keys',
  deliveryLogs: 'crm_dev_delivery_logs',
  marketplace: 'crm_dev_marketplace',
  installed: 'crm_dev_installed_plugins',
  widgets: 'crm_dev_widgets',
  apps: 'crm_dev_custom_apps',
};

/* ── Default Data ────────────────────────────────────────────── */
const DEFAULT_API_KEYS: DeveloperApiKey[] = [
  { id: '1', name: 'Production API Key', keyPrefix: 'crm_a1b2', scopes: ['read', 'write'], rateLimit: 1000, callsToday: 342, totalCalls: 28450, active: true, createdBy: 'admin', createdAt: new Date(Date.now() - 86400000 * 60).toISOString(), lastUsedAt: new Date(Date.now() - 300000).toISOString() },
  { id: '2', name: 'Analytics Read-Only', keyPrefix: 'crm_c3d4', scopes: ['read'], rateLimit: 5000, callsToday: 1280, totalCalls: 156000, active: true, createdBy: 'admin', createdAt: new Date(Date.now() - 86400000 * 90).toISOString(), lastUsedAt: new Date(Date.now() - 60000).toISOString() },
  { id: '3', name: 'Test Key (Sandbox)', keyPrefix: 'crm_e5f6', scopes: ['read', 'write'], rateLimit: 100, callsToday: 12, totalCalls: 890, active: true, expiresAt: new Date(Date.now() + 86400000 * 30).toISOString(), createdBy: 'admin', createdAt: new Date(Date.now() - 86400000 * 10).toISOString() },
];

const DEFAULT_DELIVERY_LOGS: WebhookDeliveryLog[] = [
  { id: '1', webhookId: '1', webhookName: 'Slack Notifications', eventType: 'LEAD_CREATED', payload: '{"event":"LEAD_CREATED","data":{"name":"John Doe"}}', responseStatus: 200, attempt: 1, status: 'SUCCESS', deliveredAt: new Date(Date.now() - 300000).toISOString(), createdAt: new Date(Date.now() - 300000).toISOString() },
  { id: '2', webhookId: '2', webhookName: 'Zapier Automation', eventType: 'CONTACT_CREATED', payload: '{"event":"CONTACT_CREATED","data":{"email":"jane@co.com"}}', responseStatus: 200, attempt: 1, status: 'SUCCESS', deliveredAt: new Date(Date.now() - 600000).toISOString(), createdAt: new Date(Date.now() - 600000).toISOString() },
  { id: '3', webhookId: '1', webhookName: 'Slack Notifications', eventType: 'OPPORTUNITY_WON', payload: '{"event":"OPPORTUNITY_WON","data":{"name":"Enterprise Deal"}}', responseStatus: 0, attempt: 3, status: 'FAILED', errorMessage: 'Connection timed out', createdAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '4', webhookId: '2', webhookName: 'Zapier Automation', eventType: 'LEAD_UPDATED', payload: '{"event":"LEAD_UPDATED","data":{"id":"lead-5"}}', responseStatus: 200, attempt: 1, status: 'SUCCESS', deliveredAt: new Date(Date.now() - 7200000).toISOString(), createdAt: new Date(Date.now() - 7200000).toISOString() },
];

const DEFAULT_MARKETPLACE: MarketplacePlugin[] = [
  { id: '1', name: 'Slack Integration', slug: 'slack-integration', description: 'Send CRM notifications to Slack channels in real-time', category: 'Communication', author: 'CRM Team', version: '2.1.0', iconUrl: '', status: 'PUBLISHED', pricing: 'FREE', installCount: 2450, rating: 4.7, ratingCount: 189, isVerified: true, installed: true, requiredScopes: ['read', 'notifications'], createdAt: new Date(Date.now() - 86400000 * 180).toISOString() },
  { id: '2', name: 'Mailchimp Sync', slug: 'mailchimp-sync', description: 'Bi-directional sync of contacts and campaigns with Mailchimp', category: 'Email Marketing', author: 'CRM Team', version: '1.5.0', iconUrl: '', status: 'PUBLISHED', pricing: 'FREEMIUM', priceAmount: 29, installCount: 1820, rating: 4.5, ratingCount: 142, isVerified: true, installed: false, requiredScopes: ['read', 'write', 'contacts'], createdAt: new Date(Date.now() - 86400000 * 150).toISOString() },
  { id: '3', name: 'Google Calendar', slug: 'google-calendar', description: 'Sync activities and meetings with Google Calendar', category: 'Productivity', author: 'CRM Team', version: '1.3.0', iconUrl: '', status: 'PUBLISHED', pricing: 'FREE', installCount: 3100, rating: 4.8, ratingCount: 256, isVerified: true, installed: true, requiredScopes: ['read', 'write', 'activities'], createdAt: new Date(Date.now() - 86400000 * 200).toISOString() },
  { id: '4', name: 'Stripe Payments', slug: 'stripe-payments', description: 'Track payments and invoices from Stripe within your CRM', category: 'Finance', author: 'FinTech Labs', version: '1.0.2', iconUrl: '', status: 'PUBLISHED', pricing: 'PAID', priceAmount: 49, installCount: 890, rating: 4.3, ratingCount: 67, isVerified: true, installed: false, requiredScopes: ['read', 'write', 'opportunities'], createdAt: new Date(Date.now() - 86400000 * 90).toISOString() },
  { id: '5', name: 'HubSpot Import', slug: 'hubspot-import', description: 'One-click migration from HubSpot to your CRM', category: 'Migration', author: 'DataBridge Inc', version: '1.1.0', iconUrl: '', status: 'PUBLISHED', pricing: 'PAID', priceAmount: 99, installCount: 340, rating: 4.1, ratingCount: 28, isVerified: false, installed: false, requiredScopes: ['read', 'write', 'contacts', 'leads'], createdAt: new Date(Date.now() - 86400000 * 60).toISOString() },
  { id: '6', name: 'AI Lead Scoring', slug: 'ai-lead-scoring', description: 'Machine learning powered lead scoring and prioritization', category: 'AI & Analytics', author: 'CRM Team', version: '2.0.0', iconUrl: '', status: 'PUBLISHED', pricing: 'FREEMIUM', priceAmount: 79, installCount: 1540, rating: 4.6, ratingCount: 112, isVerified: true, installed: true, requiredScopes: ['read', 'leads', 'analytics'], createdAt: new Date(Date.now() - 86400000 * 120).toISOString() },
];

const DEFAULT_INSTALLED: PluginInstallation[] = [
  { id: '1', pluginId: '1', pluginName: 'Slack Integration', pluginSlug: 'slack-integration', status: 'ACTIVE', config: { channel: '#sales', notifyOnLead: true }, installedBy: 'admin', createdAt: new Date(Date.now() - 86400000 * 90).toISOString() },
  { id: '2', pluginId: '3', pluginName: 'Google Calendar', pluginSlug: 'google-calendar', status: 'ACTIVE', config: { calendarId: 'primary', syncInterval: 15 }, installedBy: 'admin', createdAt: new Date(Date.now() - 86400000 * 60).toISOString() },
  { id: '3', pluginId: '6', pluginName: 'AI Lead Scoring', pluginSlug: 'ai-lead-scoring', status: 'ACTIVE', config: { model: 'v2', threshold: 0.7 }, installedBy: 'admin', createdAt: new Date(Date.now() - 86400000 * 30).toISOString() },
];

const DEFAULT_WIDGETS: EmbeddableWidget[] = [
  { id: '1', name: 'Sales Pipeline Chart', widgetType: 'CHART', description: 'Visual representation of the sales pipeline', config: { chartType: 'funnel', dataSource: 'opportunities' }, embedToken: 'wgt_a1b2c3d4e5f6', embedCode: '<iframe src="/embed/widget/wgt_a1b2c3d4e5f6" width="100%" height="400" frameborder="0"></iframe>', allowedDomains: ['*.company.com'], active: true, viewCount: 1250, createdAt: new Date(Date.now() - 86400000 * 30).toISOString() },
  { id: '2', name: 'Lead Capture Form', widgetType: 'FORM', description: 'Embeddable lead capture form for websites', config: { fields: ['name', 'email', 'company', 'phone'] }, embedToken: 'wgt_g7h8i9j0k1l2', embedCode: '<iframe src="/embed/widget/wgt_g7h8i9j0k1l2" width="100%" height="500" frameborder="0"></iframe>', allowedDomains: ['*.company.com', 'landing.company.com'], active: true, viewCount: 3400, createdAt: new Date(Date.now() - 86400000 * 45).toISOString() },
  { id: '3', name: 'Revenue Metric', widgetType: 'METRIC', description: 'Live revenue KPI display', config: { metric: 'totalRevenue', period: 'month' }, embedToken: 'wgt_m3n4o5p6q7r8', embedCode: '<iframe src="/embed/widget/wgt_m3n4o5p6q7r8" width="300" height="200" frameborder="0"></iframe>', allowedDomains: ['*'], active: true, viewCount: 890, createdAt: new Date(Date.now() - 86400000 * 20).toISOString() },
];

const DEFAULT_APPS: CustomApp[] = [
  { id: '1', name: 'Sales Dashboard', slug: 'sales-dashboard', description: 'Custom dashboard showing key sales metrics', appType: 'DASHBOARD', status: 'PUBLISHED', layout: { widgets: ['pipeline', 'revenue', 'activities'] }, dataSource: { primary: 'opportunities', secondary: 'activities' }, publishedVersion: '1.2.0', createdBy: 'admin', createdAt: new Date(Date.now() - 86400000 * 40).toISOString() },
  { id: '2', name: 'Onboarding Workflow', slug: 'onboarding-workflow', description: 'Customer onboarding process automation', appType: 'WORKFLOW', status: 'PUBLISHED', layout: { steps: ['welcome', 'setup', 'training', 'go-live'] }, dataSource: { primary: 'contacts' }, publishedVersion: '1.0.0', createdBy: 'admin', createdAt: new Date(Date.now() - 86400000 * 25).toISOString() },
  { id: '3', name: 'Lead Intake Form', slug: 'lead-intake-form', description: 'Custom lead intake form for trade shows', appType: 'FORM', status: 'DRAFT', layout: { fields: ['name', 'company', 'interest', 'budget'] }, dataSource: { primary: 'leads' }, createdBy: 'admin', createdAt: new Date(Date.now() - 86400000 * 5).toISOString() },
];

/* ── Service ─────────────────────────────────────────────────── */
export const developerPlatformService = {

  /* ── API Keys ── */
  getApiKeys: async (): Promise<DeveloperApiKey[]> =>
    tryApi(async () => { const r = await api.get<{ data: DeveloperApiKey[] }>(`${BASE}/api-keys`); return r.data.data; }, loadJson(K.apiKeys, DEFAULT_API_KEYS)),

  createApiKey: async (req: CreateApiKeyRequest): Promise<DeveloperApiKey> => {
    const key: DeveloperApiKey = { id: `ak-${Date.now()}`, keyPrefix: 'crm_new', rawKey: 'crm_xxxx...xxxx (shown once)', scopes: req.scopes || ['read', 'write'], rateLimit: req.rateLimit || 1000, callsToday: 0, totalCalls: 0, active: true, expiresAt: req.expiresAt, createdBy: 'admin', createdAt: now, ...req };
    const local = loadJson<DeveloperApiKey[]>(K.apiKeys, DEFAULT_API_KEYS);
    saveJson(K.apiKeys, [key, ...local]);
    try { const r = await api.post<{ data: DeveloperApiKey }>(`${BASE}/api-keys`, req); return r.data.data; } catch { return key; }
  },

  revokeApiKey: async (id: string): Promise<void> => {
    const local = loadJson<DeveloperApiKey[]>(K.apiKeys, DEFAULT_API_KEYS);
    saveJson(K.apiKeys, local.map(k => k.id === id ? { ...k, active: false } : k));
    try { await api.post(`${BASE}/api-keys/${id}/revoke`); } catch { /* local fallback */ }
  },

  deleteApiKey: async (id: string): Promise<void> => {
    const local = loadJson<DeveloperApiKey[]>(K.apiKeys, DEFAULT_API_KEYS);
    saveJson(K.apiKeys, local.filter(k => k.id !== id));
    try { await api.delete(`${BASE}/api-keys/${id}`); } catch { /* local fallback */ }
  },

  /* ── Webhook Delivery Logs ── */
  getDeliveryLogs: async (): Promise<WebhookDeliveryLog[]> =>
    tryApi(async () => { const r = await api.get<{ data: WebhookDeliveryLog[] }>(`${BASE}/webhooks/deliveries`); return r.data.data; }, loadJson(K.deliveryLogs, DEFAULT_DELIVERY_LOGS)),

  getDeliveryLogsByWebhook: async (webhookId: string): Promise<WebhookDeliveryLog[]> =>
    tryApi(async () => { const r = await api.get<{ data: WebhookDeliveryLog[] }>(`${BASE}/webhooks/${webhookId}/deliveries`); return r.data.data; }, loadJson<WebhookDeliveryLog[]>(K.deliveryLogs, DEFAULT_DELIVERY_LOGS).filter(l => l.webhookId === webhookId)),

  testWebhook: async (webhookId: string): Promise<WebhookDeliveryLog> => {
    const log: WebhookDeliveryLog = { id: `dl-${Date.now()}`, webhookId, webhookName: 'Test', eventType: 'webhook.test', payload: '{"test":true}', responseStatus: 200, attempt: 1, status: 'SUCCESS', deliveredAt: now, createdAt: now };
    const local = loadJson<WebhookDeliveryLog[]>(K.deliveryLogs, DEFAULT_DELIVERY_LOGS);
    saveJson(K.deliveryLogs, [log, ...local]);
    try { const r = await api.post<{ data: WebhookDeliveryLog }>(`${BASE}/webhooks/${webhookId}/test`); return r.data.data; } catch { return log; }
  },

  /* ── Marketplace ── */
  getMarketplacePlugins: async (): Promise<MarketplacePlugin[]> =>
    tryApi(async () => { const r = await api.get<{ data: MarketplacePlugin[] }>(`${BASE}/marketplace`); return r.data.data; }, loadJson(K.marketplace, DEFAULT_MARKETPLACE)),

  getPluginsByCategory: async (category: string): Promise<MarketplacePlugin[]> =>
    tryApi(async () => { const r = await api.get<{ data: MarketplacePlugin[] }>(`${BASE}/marketplace/category/${category}`); return r.data.data; }, loadJson<MarketplacePlugin[]>(K.marketplace, DEFAULT_MARKETPLACE).filter(p => p.category === category)),

  createPlugin: async (req: CreatePluginRequest): Promise<MarketplacePlugin> => {
    const slug = req.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
    const plugin: MarketplacePlugin = { id: `pl-${Date.now()}`, slug, status: 'DRAFT', pricing: (req.pricing || 'FREE') as MarketplacePlugin['pricing'], installCount: 0, rating: 0, ratingCount: 0, isVerified: false, installed: false, createdAt: now, name: req.name, description: req.description, longDescription: req.longDescription, category: req.category || 'General', author: req.author || 'admin', version: req.version || '1.0.0', iconUrl: req.iconUrl, screenshots: req.screenshots, downloadUrl: req.downloadUrl, documentationUrl: req.documentationUrl, priceAmount: req.priceAmount, requiredScopes: req.requiredScopes, configSchema: req.configSchema };
    const local = loadJson<MarketplacePlugin[]>(K.marketplace, DEFAULT_MARKETPLACE);
    saveJson(K.marketplace, [plugin, ...local]);
    try { const r = await api.post<{ data: MarketplacePlugin }>(`${BASE}/marketplace`, req); return r.data.data; } catch { return plugin; }
  },

  /* ── Plugin Installations ── */
  getInstalledPlugins: async (): Promise<PluginInstallation[]> =>
    tryApi(async () => { const r = await api.get<{ data: PluginInstallation[] }>(`${BASE}/plugins/installed`); return r.data.data; }, loadJson(K.installed, DEFAULT_INSTALLED)),

  installPlugin: async (pluginId: string, config?: Record<string, unknown>): Promise<PluginInstallation> => {
    const plugins = loadJson<MarketplacePlugin[]>(K.marketplace, DEFAULT_MARKETPLACE);
    const plug = plugins.find(p => p.id === pluginId);
    const inst: PluginInstallation = { id: `inst-${Date.now()}`, pluginId, pluginName: plug?.name || 'Plugin', pluginSlug: plug?.slug || 'plugin', status: 'ACTIVE', config, installedBy: 'admin', createdAt: now };
    const local = loadJson<PluginInstallation[]>(K.installed, DEFAULT_INSTALLED);
    saveJson(K.installed, [inst, ...local]);
    saveJson(K.marketplace, plugins.map(p => p.id === pluginId ? { ...p, installed: true, installCount: p.installCount + 1 } : p));
    try { const r = await api.post<{ data: PluginInstallation }>(`${BASE}/plugins/${pluginId}/install`, { config }); return r.data.data; } catch { return inst; }
  },

  uninstallPlugin: async (pluginId: string): Promise<void> => {
    const local = loadJson<PluginInstallation[]>(K.installed, DEFAULT_INSTALLED);
    saveJson(K.installed, local.filter(i => i.pluginId !== pluginId));
    const plugins = loadJson<MarketplacePlugin[]>(K.marketplace, DEFAULT_MARKETPLACE);
    saveJson(K.marketplace, plugins.map(p => p.id === pluginId ? { ...p, installed: false } : p));
    try { await api.post(`${BASE}/plugins/${pluginId}/uninstall`); } catch { /* local fallback */ }
  },

  /* ── Embeddable Widgets ── */
  getWidgets: async (): Promise<EmbeddableWidget[]> =>
    tryApi(async () => { const r = await api.get<{ data: EmbeddableWidget[] }>(`${BASE}/widgets`); return r.data.data; }, loadJson(K.widgets, DEFAULT_WIDGETS)),

  createWidget: async (req: CreateWidgetRequest): Promise<EmbeddableWidget> => {
    const token = `wgt_${Date.now().toString(36)}`;
    const widget: EmbeddableWidget = { id: `w-${Date.now()}`, embedToken: token, embedCode: `<iframe src="/embed/widget/${token}" width="100%" height="400" frameborder="0"></iframe>`, widgetType: (req.widgetType || 'CHART') as EmbeddableWidget['widgetType'], active: true, viewCount: 0, createdAt: now, name: req.name, description: req.description, config: req.config, allowedDomains: req.allowedDomains };
    const local = loadJson<EmbeddableWidget[]>(K.widgets, DEFAULT_WIDGETS);
    saveJson(K.widgets, [widget, ...local]);
    try { const r = await api.post<{ data: EmbeddableWidget }>(`${BASE}/widgets`, req); return r.data.data; } catch { return widget; }
  },

  updateWidget: async (id: string, req: CreateWidgetRequest): Promise<EmbeddableWidget> => {
    const local = loadJson<EmbeddableWidget[]>(K.widgets, DEFAULT_WIDGETS);
    const updated = local.map(w => w.id === id ? { ...w, ...req, widgetType: (req.widgetType || w.widgetType) as EmbeddableWidget['widgetType'] } : w);
    saveJson(K.widgets, updated);
    try { const r = await api.put<{ data: EmbeddableWidget }>(`${BASE}/widgets/${id}`, req); return r.data.data; } catch { return updated.find(w => w.id === id)!; }
  },

  deleteWidget: async (id: string): Promise<void> => {
    const local = loadJson<EmbeddableWidget[]>(K.widgets, DEFAULT_WIDGETS);
    saveJson(K.widgets, local.filter(w => w.id !== id));
    try { await api.delete(`${BASE}/widgets/${id}`); } catch { /* local fallback */ }
  },

  /* ── Custom Apps (Low-Code Builder) ── */
  getCustomApps: async (): Promise<CustomApp[]> =>
    tryApi(async () => { const r = await api.get<{ data: CustomApp[] }>(`${BASE}/apps`); return r.data.data; }, loadJson(K.apps, DEFAULT_APPS)),

  createCustomApp: async (req: CreateCustomAppRequest): Promise<CustomApp> => {
    const slug = req.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
    const app: CustomApp = { id: `app-${Date.now()}`, slug, appType: (req.appType || 'DASHBOARD') as CustomApp['appType'], status: 'DRAFT', createdBy: 'admin', createdAt: now, name: req.name, description: req.description, layout: req.layout, dataSource: req.dataSource, style: req.style };
    const local = loadJson<CustomApp[]>(K.apps, DEFAULT_APPS);
    saveJson(K.apps, [app, ...local]);
    try { const r = await api.post<{ data: CustomApp }>(`${BASE}/apps`, req); return r.data.data; } catch { return app; }
  },

  updateCustomApp: async (id: string, req: CreateCustomAppRequest): Promise<CustomApp> => {
    const local = loadJson<CustomApp[]>(K.apps, DEFAULT_APPS);
    const updated = local.map(a => a.id === id ? { ...a, ...req, appType: (req.appType || a.appType) as CustomApp['appType'] } : a);
    saveJson(K.apps, updated);
    try { const r = await api.put<{ data: CustomApp }>(`${BASE}/apps/${id}`, req); return r.data.data; } catch { return updated.find(a => a.id === id)!; }
  },

  publishCustomApp: async (id: string): Promise<CustomApp> => {
    const local = loadJson<CustomApp[]>(K.apps, DEFAULT_APPS);
    const updated = local.map(a => a.id === id ? { ...a, status: 'PUBLISHED' as const, publishedVersion: a.publishedVersion ? incrementVersion(a.publishedVersion) : '1.0.0' } : a);
    saveJson(K.apps, updated);
    try { const r = await api.post<{ data: CustomApp }>(`${BASE}/apps/${id}/publish`); return r.data.data; } catch { return updated.find(a => a.id === id)!; }
  },

  deleteCustomApp: async (id: string): Promise<void> => {
    const local = loadJson<CustomApp[]>(K.apps, DEFAULT_APPS);
    saveJson(K.apps, local.filter(a => a.id !== id));
    try { await api.delete(`${BASE}/apps/${id}`); } catch { /* local fallback */ }
  },
};

function incrementVersion(v: string): string {
  const parts = v.split('.');
  if (parts.length === 3) { const p = parseInt(parts[2]) + 1; return `${parts[0]}.${parts[1]}.${p}`; }
  return '1.0.0';
}
