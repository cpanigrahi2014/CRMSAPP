import api from './api';
import type { ApiResponse } from '../types';
import type {
  RestApiEndpoint, WebhookConfig, ThirdPartyIntegration, DataSync,
  ExternalConnector, ApiAuthConfig, IntegrationHealth, IntegrationError,
} from '../types';

const BASE = '/api/v1/integrations';

/* ── localStorage helpers ────────────────────────────────────── */
function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch { return fallback; }
}
function saveJson<T>(key: string, data: T): void {
  localStorage.setItem(key, JSON.stringify(data));
}

/* ── Async helper: try API, fall back to localStorage ──────── */
async function tryApi<T>(apiFn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    const result = await apiFn();
    return result ?? fallback;
  } catch { return fallback; }
}

/* ── Storage keys ────────────────────────────────────────────── */
const K = {
  apis: 'crm_integration_apis',
  webhooks: 'crm_integration_webhooks',
  integrations: 'crm_integration_thirdparty',
  syncs: 'crm_integration_syncs',
  connectors: 'crm_integration_connectors',
  auth: 'crm_integration_auth',
  health: 'crm_integration_health',
  errors: 'crm_integration_errors',
};

/* ── Default REST API Endpoints ──────────────────────────────── */
const DEFAULT_APIS: RestApiEndpoint[] = [
  { id: '1', name: 'Get Leads', path: '/api/v1/leads', method: 'GET', description: 'Retrieve paginated leads', authRequired: true, rateLimit: 100, enabled: true, version: 'v1', totalCalls: 12450, createdAt: new Date(Date.now() - 86400000 * 90).toISOString(), lastCalledAt: new Date(Date.now() - 300000).toISOString() },
  { id: '2', name: 'Create Lead', path: '/api/v1/leads', method: 'POST', description: 'Create a new lead', authRequired: true, rateLimit: 50, enabled: true, version: 'v1', totalCalls: 3420, createdAt: new Date(Date.now() - 86400000 * 90).toISOString(), lastCalledAt: new Date(Date.now() - 600000).toISOString() },
  { id: '3', name: 'Get Contacts', path: '/api/v1/contacts', method: 'GET', description: 'Retrieve paginated contacts', authRequired: true, rateLimit: 100, enabled: true, version: 'v1', totalCalls: 8900, createdAt: new Date(Date.now() - 86400000 * 90).toISOString(), lastCalledAt: new Date(Date.now() - 180000).toISOString() },
  { id: '4', name: 'Get Opportunities', path: '/api/v1/opportunities', method: 'GET', description: 'Retrieve pipeline opportunities', authRequired: true, rateLimit: 100, enabled: true, version: 'v1', totalCalls: 6780, createdAt: new Date(Date.now() - 86400000 * 60).toISOString() },
  { id: '5', name: 'Get Accounts', path: '/api/v1/accounts', method: 'GET', description: 'Retrieve company accounts', authRequired: true, rateLimit: 100, enabled: true, version: 'v1', totalCalls: 5430, createdAt: new Date(Date.now() - 86400000 * 60).toISOString() },
  { id: '6', name: 'Update Lead', path: '/api/v1/leads/{id}', method: 'PUT', description: 'Update a lead by ID', authRequired: true, rateLimit: 50, enabled: true, version: 'v1', totalCalls: 2340, createdAt: new Date(Date.now() - 86400000 * 60).toISOString() },
  { id: '7', name: 'Webhook Ingest', path: '/api/v1/webhooks/ingest', method: 'POST', description: 'Receive external webhook payloads', authRequired: false, rateLimit: 200, enabled: true, version: 'v1', totalCalls: 18900, createdAt: new Date(Date.now() - 86400000 * 30).toISOString() },
  { id: '8', name: 'Bulk Import', path: '/api/v1/import/bulk', method: 'POST', description: 'Bulk import records via CSV/JSON', authRequired: true, rateLimit: 10, enabled: true, version: 'v1', totalCalls: 150, createdAt: new Date(Date.now() - 86400000 * 20).toISOString() },
];

/* ── Default Webhooks ────────────────────────────────────────── */
const DEFAULT_WEBHOOKS: WebhookConfig[] = [
  { id: '1', name: 'Slack Notifications', url: 'https://hooks.slack.com/services/T00/B00/xxx', events: ['LEAD_CREATED', 'OPPORTUNITY_WON'], active: true, retryCount: 3, retryDelayMs: 5000, successCount: 245, failureCount: 3, createdAt: new Date(Date.now() - 86400000 * 30).toISOString(), lastTriggeredAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', name: 'Zapier Automation', url: 'https://hooks.zapier.com/hooks/catch/123/abc', events: ['CONTACT_CREATED', 'LEAD_UPDATED'], active: true, retryCount: 5, retryDelayMs: 10000, successCount: 1890, failureCount: 12, createdAt: new Date(Date.now() - 86400000 * 60).toISOString(), lastTriggeredAt: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', name: 'Analytics Pipeline', url: 'https://analytics.company.com/api/events', events: ['OPPORTUNITY_CREATED', 'OPPORTUNITY_UPDATED', 'OPPORTUNITY_WON', 'OPPORTUNITY_LOST'], active: false, retryCount: 3, retryDelayMs: 5000, successCount: 560, failureCount: 45, createdAt: new Date(Date.now() - 86400000 * 45).toISOString() },
];

/* ── Default Third-Party Integrations ────────────────────────── */
const DEFAULT_INTEGRATIONS: ThirdPartyIntegration[] = [
  { id: '1', name: 'Salesforce Sync', provider: 'Salesforce', type: 'SALESFORCE', status: 'ACTIVE', description: 'Bidirectional lead and contact sync with Salesforce', authType: 'OAUTH2', enabled: true, createdAt: new Date(Date.now() - 86400000 * 120).toISOString(), lastSyncAt: new Date(Date.now() - 1800000).toISOString() },
  { id: '2', name: 'HubSpot Marketing', provider: 'HubSpot', type: 'HUBSPOT', status: 'ACTIVE', description: 'Import marketing leads and campaign data', authType: 'API_KEY', enabled: true, createdAt: new Date(Date.now() - 86400000 * 90).toISOString(), lastSyncAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '3', name: 'Slack Notifications', provider: 'Slack', type: 'SLACK', status: 'ACTIVE', description: 'Send deal alerts and activity notifications to Slack', authType: 'OAUTH2', enabled: true, createdAt: new Date(Date.now() - 86400000 * 60).toISOString() },
  { id: '4', name: 'Zapier Workflows', provider: 'Zapier', type: 'ZAPIER', status: 'INACTIVE', description: 'Connect CRM events to 5000+ apps via Zapier', authType: 'API_KEY', enabled: false, createdAt: new Date(Date.now() - 86400000 * 30).toISOString() },
  { id: '5', name: 'Custom Data Lake', provider: 'AWS', type: 'REST_API', status: 'ERROR', description: 'Push analytics data to S3 data lake', authType: 'BEARER', enabled: true, createdAt: new Date(Date.now() - 86400000 * 15).toISOString() },
];

/* ── Default Data Syncs ──────────────────────────────────────── */
const DEFAULT_SYNCS: DataSync[] = [
  { id: '1', name: 'SF Lead Sync', integrationId: '1', integrationName: 'Salesforce Sync', entityType: 'LEAD', direction: 'BIDIRECTIONAL', status: 'COMPLETED', schedule: '*/15 * * * *', lastRunAt: new Date(Date.now() - 900000).toISOString(), lastRunDuration: 12400, recordsSynced: 342, recordsFailed: 2, fieldMapping: { firstName: 'FirstName', lastName: 'LastName', email: 'Email', company: 'Company', phone: 'Phone' }, enabled: true, createdAt: new Date(Date.now() - 86400000 * 120).toISOString() },
  { id: '2', name: 'HubSpot Contact Import', integrationId: '2', integrationName: 'HubSpot Marketing', entityType: 'CONTACT', direction: 'INBOUND', status: 'IDLE', schedule: '0 */6 * * *', lastRunAt: new Date(Date.now() - 21600000).toISOString(), lastRunDuration: 8500, recordsSynced: 156, recordsFailed: 0, fieldMapping: { firstname: 'firstName', lastname: 'lastName', email: 'email', phone: 'phone' }, enabled: true, createdAt: new Date(Date.now() - 86400000 * 90).toISOString() },
  { id: '3', name: 'Opportunity Export', integrationId: '5', integrationName: 'Custom Data Lake', entityType: 'OPPORTUNITY', direction: 'OUTBOUND', status: 'FAILED', schedule: '0 2 * * *', lastRunAt: new Date(Date.now() - 86400000).toISOString(), lastRunDuration: 2100, recordsSynced: 0, recordsFailed: 89, fieldMapping: { name: 'deal_name', amount: 'deal_amount', stage: 'deal_stage' }, enabled: true, createdAt: new Date(Date.now() - 86400000 * 15).toISOString() },
];

/* ── Default Connectors ──────────────────────────────────────── */
const DEFAULT_CONNECTORS: ExternalConnector[] = [
  { id: '1', name: 'Production PostgreSQL', type: 'DATABASE', host: 'db.company.com', port: 5432, database: 'crm_prod', status: 'ACTIVE', enabled: true, lastTestAt: new Date(Date.now() - 3600000).toISOString(), createdAt: new Date(Date.now() - 86400000 * 180).toISOString() },
  { id: '2', name: 'Reporting Data Warehouse', type: 'DATABASE', host: 'dw.company.com', port: 5432, database: 'analytics', status: 'ACTIVE', enabled: true, lastTestAt: new Date(Date.now() - 7200000).toISOString(), createdAt: new Date(Date.now() - 86400000 * 90).toISOString() },
  { id: '3', name: 'Salesforce REST', type: 'REST_API', baseUrl: 'https://company.my.salesforce.com', status: 'ACTIVE', enabled: true, lastTestAt: new Date(Date.now() - 1800000).toISOString(), createdAt: new Date(Date.now() - 86400000 * 120).toISOString() },
  { id: '4', name: 'CSV Import Source', type: 'FILE', connectionString: '/imports/', status: 'INACTIVE', enabled: false, createdAt: new Date(Date.now() - 86400000 * 60).toISOString() },
];

/* ── Default API Auth Configs ────────────────────────────────── */
const DEFAULT_AUTH: ApiAuthConfig[] = [
  { id: '1', name: 'CRM Public API Key', authType: 'API_KEY', apiKey: 'crm_pk_live_a1b2c3...', active: true, createdAt: new Date(Date.now() - 86400000 * 90).toISOString(), lastUsedAt: new Date(Date.now() - 600000).toISOString() },
  { id: '2', name: 'Salesforce OAuth', authType: 'OAUTH2', clientId: 'sf_client_id_xxx', tokenUrl: 'https://login.salesforce.com/services/oauth2/token', scopes: ['api', 'refresh_token'], active: true, expiresAt: new Date(Date.now() + 86400000 * 30).toISOString(), createdAt: new Date(Date.now() - 86400000 * 120).toISOString(), lastUsedAt: new Date(Date.now() - 1800000).toISOString() },
  { id: '3', name: 'HubSpot API Key', authType: 'API_KEY', apiKey: 'hubspot_ak_xxx...', active: true, createdAt: new Date(Date.now() - 86400000 * 90).toISOString(), lastUsedAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '4', name: 'Slack Bot Token', authType: 'BEARER', active: true, createdAt: new Date(Date.now() - 86400000 * 60).toISOString(), lastUsedAt: new Date(Date.now() - 7200000).toISOString() },
];

/* ── Default Health ──────────────────────────────────────────── */
const DEFAULT_HEALTH: IntegrationHealth[] = [
  { id: '1', integrationId: '1', integrationName: 'Salesforce Sync', status: 'ACTIVE', uptime: 99.8, avgResponseMs: 245, successRate: 99.4, totalRequests: 45600, lastCheckedAt: new Date(Date.now() - 60000).toISOString(), alertsCount: 1 },
  { id: '2', integrationId: '2', integrationName: 'HubSpot Marketing', status: 'ACTIVE', uptime: 99.9, avgResponseMs: 180, successRate: 100, totalRequests: 12400, lastCheckedAt: new Date(Date.now() - 60000).toISOString(), alertsCount: 0 },
  { id: '3', integrationId: '3', integrationName: 'Slack Notifications', status: 'ACTIVE', uptime: 99.5, avgResponseMs: 120, successRate: 98.8, totalRequests: 8900, lastCheckedAt: new Date(Date.now() - 60000).toISOString(), alertsCount: 2 },
  { id: '4', integrationId: '5', integrationName: 'Custom Data Lake', status: 'ERROR', uptime: 87.3, avgResponseMs: 890, successRate: 72.1, totalRequests: 3400, lastCheckedAt: new Date(Date.now() - 60000).toISOString(), alertsCount: 15 },
];

/* ── Default Error Logs ──────────────────────────────────────── */
const DEFAULT_ERRORS: IntegrationError[] = [
  { id: '1', integrationId: '5', integrationName: 'Custom Data Lake', level: 'CRITICAL', message: 'Connection refused: Unable to reach S3 endpoint', endpoint: 'PUT /data/opportunities', httpStatus: 503, createdAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', integrationId: '5', integrationName: 'Custom Data Lake', level: 'ERROR', message: 'Authentication token expired', endpoint: 'POST /auth/refresh', httpStatus: 401, createdAt: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', integrationId: '1', integrationName: 'Salesforce Sync', level: 'WARN', message: 'Rate limit approaching: 95% of quota used', endpoint: 'GET /services/data/v58.0/query', httpStatus: 200, createdAt: new Date(Date.now() - 14400000).toISOString() },
  { id: '4', integrationId: '3', integrationName: 'Slack Notifications', level: 'ERROR', message: 'Webhook delivery failed: channel_not_found', endpoint: 'POST /services/hooks', httpStatus: 404, createdAt: new Date(Date.now() - 21600000).toISOString(), resolvedAt: new Date(Date.now() - 18000000).toISOString() },
  { id: '5', integrationId: '2', integrationName: 'HubSpot Marketing', level: 'INFO', message: 'Sync completed successfully: 156 contacts imported', endpoint: 'GET /contacts/v3/search', httpStatus: 200, createdAt: new Date(Date.now() - 21600000).toISOString() },
  { id: '6', integrationId: '5', integrationName: 'Custom Data Lake', level: 'CRITICAL', message: 'Data corruption detected in batch upload', endpoint: 'POST /data/batch', httpStatus: 500, requestPayload: '{"records": 89, "format": "json"}', createdAt: new Date(Date.now() - 86400000).toISOString() },
  { id: '7', integrationId: '1', integrationName: 'Salesforce Sync', level: 'WARN', message: 'Field mapping mismatch: "leadScore" not found in target', endpoint: 'POST /services/data/v58.0/sobjects/Lead', httpStatus: 400, createdAt: new Date(Date.now() - 86400000 * 2).toISOString(), resolvedAt: new Date(Date.now() - 86400000).toISOString() },
  { id: '8', integrationId: '3', integrationName: 'Slack Notifications', level: 'INFO', message: 'Connection test successful', endpoint: 'POST /api.test', httpStatus: 200, createdAt: new Date(Date.now() - 86400000 * 3).toISOString() },
];

/* ── Service ─────────────────────────────────────────────────── */
export const integrationService = {
  // REST APIs
  getApis: (): RestApiEndpoint[] => loadJson(K.apis, DEFAULT_APIS),
  fetchApis: (): Promise<RestApiEndpoint[]> =>
    tryApi(() => api.get<ApiResponse<RestApiEndpoint[]>>(`${BASE}/apis`).then(r => r.data.data!), loadJson(K.apis, DEFAULT_APIS)),
  saveApis: (d: RestApiEndpoint[]) => saveJson(K.apis, d),

  // Webhooks
  getWebhooks: (): WebhookConfig[] => loadJson(K.webhooks, DEFAULT_WEBHOOKS),
  fetchWebhooks: (): Promise<WebhookConfig[]> =>
    tryApi(() => api.get<ApiResponse<WebhookConfig[]>>(`${BASE}/webhooks`).then(r => r.data.data!), loadJson(K.webhooks, DEFAULT_WEBHOOKS)),
  saveWebhooks: (d: WebhookConfig[]) => saveJson(K.webhooks, d),

  // Third-party integrations
  getIntegrations: (): ThirdPartyIntegration[] => loadJson(K.integrations, DEFAULT_INTEGRATIONS),
  fetchIntegrations: (): Promise<ThirdPartyIntegration[]> =>
    tryApi(() => api.get<ApiResponse<ThirdPartyIntegration[]>>(`${BASE}`).then(r => r.data.data!), loadJson(K.integrations, DEFAULT_INTEGRATIONS)),
  saveIntegrations: (d: ThirdPartyIntegration[]) => saveJson(K.integrations, d),

  // Data syncs
  getSyncs: (): DataSync[] => loadJson(K.syncs, DEFAULT_SYNCS),
  fetchSyncs: (): Promise<DataSync[]> =>
    tryApi(() => api.get<ApiResponse<DataSync[]>>(`${BASE}/syncs`).then(r => r.data.data!), loadJson(K.syncs, DEFAULT_SYNCS)),
  saveSyncs: (d: DataSync[]) => saveJson(K.syncs, d),

  // External connectors
  getConnectors: (): ExternalConnector[] => loadJson(K.connectors, DEFAULT_CONNECTORS),
  fetchConnectors: (): Promise<ExternalConnector[]> =>
    tryApi(() => api.get<ApiResponse<ExternalConnector[]>>(`${BASE}/connectors`).then(r => r.data.data!), loadJson(K.connectors, DEFAULT_CONNECTORS)),
  saveConnectors: (d: ExternalConnector[]) => saveJson(K.connectors, d),

  // API authentication
  getAuthConfigs: (): ApiAuthConfig[] => loadJson(K.auth, DEFAULT_AUTH),
  fetchAuthConfigs: (): Promise<ApiAuthConfig[]> =>
    tryApi(() => api.get<ApiResponse<ApiAuthConfig[]>>(`${BASE}/auth-configs`).then(r => r.data.data!), loadJson(K.auth, DEFAULT_AUTH)),
  saveAuthConfigs: (d: ApiAuthConfig[]) => saveJson(K.auth, d),

  // Monitoring
  getHealth: (): IntegrationHealth[] => loadJson(K.health, DEFAULT_HEALTH),
  fetchHealth: (): Promise<IntegrationHealth[]> =>
    tryApi(() => api.get<ApiResponse<IntegrationHealth[]>>(`${BASE}/health`).then(r => r.data.data!), loadJson(K.health, DEFAULT_HEALTH)),
  saveHealth: (d: IntegrationHealth[]) => saveJson(K.health, d),

  // Error logs
  getErrors: (): IntegrationError[] => loadJson(K.errors, DEFAULT_ERRORS),
  fetchErrors: (): Promise<IntegrationError[]> =>
    tryApi(() => api.get<ApiResponse<IntegrationError[]>>(`${BASE}/errors`).then(r => r.data.data!), loadJson(K.errors, DEFAULT_ERRORS)),
  saveErrors: (d: IntegrationError[]) => saveJson(K.errors, d),
};
