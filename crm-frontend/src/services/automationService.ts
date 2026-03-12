import api from './api';
import type { ApiResponse, PagedData } from '../types';
import type {
  WorkflowTemplate, WorkflowSuggestion, ProposalTemplate,
  Proposal, Contract, CreateProposalRequest, CreateContractRequest, SignContractRequest,
} from '../types';

const BASE = '/api/v1/automation';

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
async function tryApi<T>(apiFn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    const result = await apiFn();
    return result ?? fallback;
  } catch { return fallback; }
}

/* ── Storage keys ────────────────────────────────────────────── */
const K = {
  templates: 'crm_auto_templates',
  suggestions: 'crm_auto_suggestions',
  proposals: 'crm_auto_proposals',
  contracts: 'crm_auto_contracts',
};

/* ── Default data ────────────────────────────────────────────── */
const now = new Date().toISOString();
const h = (hours: number) => new Date(Date.now() - hours * 3600000).toISOString();

const DEFAULT_TEMPLATES: WorkflowTemplate[] = [
  { id: 'tpl-1', name: 'Auto-Assign New Leads', description: 'Automatically assign new leads to sales reps based on territory', category: 'LEAD_MANAGEMENT', entityType: 'LEAD', triggerEvent: 'CREATED', conditionsJson: '[{"fieldName":"territory","operator":"IS_NOT_NULL","value":""}]', actionsJson: '[{"actionType":"ASSIGN_TO","targetField":"assignedTo","targetValue":"auto","actionOrder":1}]', canvasLayout: '{"nodes":[{"id":"trigger","type":"trigger","x":100,"y":100},{"id":"condition","type":"condition","x":300,"y":100},{"id":"action","type":"action","x":500,"y":100}]}', popularity: 85, isSystem: true, createdAt: h(720), updatedAt: h(48) },
  { id: 'tpl-2', name: 'Welcome Email on Lead Creation', description: 'Send a welcome email when a new lead is created', category: 'COMMUNICATION', entityType: 'LEAD', triggerEvent: 'CREATED', conditionsJson: '[{"fieldName":"email","operator":"IS_NOT_NULL","value":""}]', actionsJson: '[{"actionType":"SEND_EMAIL","targetField":"email","targetValue":"welcome_template","actionOrder":1}]', canvasLayout: '{"nodes":[{"id":"trigger","type":"trigger","x":100,"y":100},{"id":"action","type":"action","x":400,"y":100}]}', popularity: 72, isSystem: true, createdAt: h(720), updatedAt: h(96) },
  { id: 'tpl-3', name: 'Stage Change Notification', description: 'Notify manager when opportunity stage changes', category: 'NOTIFICATION', entityType: 'OPPORTUNITY', triggerEvent: 'STAGE_CHANGED', conditionsJson: '[]', actionsJson: '[{"actionType":"SEND_NOTIFICATION","targetField":"manager","targetValue":"Stage changed","actionOrder":1}]', canvasLayout: '{"nodes":[{"id":"trigger","type":"trigger","x":100,"y":100},{"id":"action","type":"action","x":400,"y":100}]}', popularity: 64, isSystem: true, createdAt: h(720), updatedAt: h(168) },
  { id: 'tpl-4', name: 'Follow-Up Task on Qualification', description: 'Create a follow-up task when lead is qualified', category: 'TASK_AUTOMATION', entityType: 'LEAD', triggerEvent: 'STATUS_CHANGED', conditionsJson: '[{"fieldName":"status","operator":"EQUALS","value":"QUALIFIED"}]', actionsJson: '[{"actionType":"CREATE_TASK","targetField":"","targetValue":"Follow up with qualified lead","actionOrder":1}]', canvasLayout: '{"nodes":[{"id":"trigger","type":"trigger","x":100,"y":100},{"id":"condition","type":"condition","x":300,"y":100},{"id":"action","type":"action","x":500,"y":100}]}', popularity: 58, isSystem: true, createdAt: h(720), updatedAt: h(240) },
  { id: 'tpl-5', name: 'Stale Lead Escalation', description: 'Escalate leads that have been untouched for 7+ days', category: 'ESCALATION', entityType: 'LEAD', triggerEvent: 'SCHEDULED', conditionsJson: '[{"fieldName":"daysSinceUpdate","operator":"GREATER_THAN","value":"7"}]', actionsJson: '[{"actionType":"SEND_NOTIFICATION","targetField":"manager","targetValue":"Stale lead requires attention","actionOrder":1},{"actionType":"UPDATE_FIELD","targetField":"priority","targetValue":"HIGH","actionOrder":2}]', canvasLayout: '{"nodes":[{"id":"trigger","type":"trigger","x":100,"y":100},{"id":"condition","type":"condition","x":300,"y":100},{"id":"action1","type":"action","x":500,"y":50},{"id":"action2","type":"action","x":500,"y":200}]}', popularity: 45, isSystem: true, createdAt: h(720), updatedAt: h(336) },
];

const DEFAULT_SUGGESTIONS: WorkflowSuggestion[] = [
  { id: 'sug-1', suggestionType: 'BEST_PRACTICE', name: 'Auto-assign leads by territory', description: 'Based on your lead volume, we recommend setting up automatic territory-based lead routing to reduce response time by ~40%.', entityType: 'LEAD', triggerEvent: 'CREATED', conditionsJson: '[{"fieldName":"territory","operator":"IS_NOT_NULL","value":""}]', actionsJson: '[{"actionType":"ASSIGN_TO","targetField":"assignedTo","targetValue":"round_robin","actionOrder":1}]', confidence: 0.92, reason: 'High lead volume with manual assignment detected', status: 'PENDING', tenantId: 'default', createdAt: h(24), updatedAt: h(24) },
  { id: 'sug-2', suggestionType: 'PATTERN_DETECTED', name: 'Follow-up reminder for proposals', description: 'We noticed 65% of won deals had a follow-up within 48 hours of sending a proposal. Adding an automated reminder could improve win rates.', entityType: 'OPPORTUNITY', triggerEvent: 'STAGE_CHANGED', conditionsJson: '[{"fieldName":"stage","operator":"EQUALS","value":"PROPOSAL"}]', actionsJson: '[{"actionType":"CREATE_TASK","targetField":"","targetValue":"Follow up on proposal within 48h","actionOrder":1}]', confidence: 0.85, reason: 'Pattern: 65% of won deals had timely follow-up', status: 'PENDING', tenantId: 'default', createdAt: h(12), updatedAt: h(12) },
  { id: 'sug-3', suggestionType: 'OPTIMIZATION', name: 'Reduce notification noise', description: 'Your "Stage Change Notification" workflow fires on every stage change. Consider limiting to key stages (PROPOSAL, NEGOTIATION, CLOSED_WON) to reduce notification fatigue.', entityType: 'OPPORTUNITY', triggerEvent: 'STAGE_CHANGED', conditionsJson: '[{"fieldName":"stage","operator":"IN","value":"PROPOSAL,NEGOTIATION,CLOSED_WON"}]', actionsJson: '[{"actionType":"SEND_NOTIFICATION","targetField":"manager","targetValue":"Key stage reached","actionOrder":1}]', confidence: 0.78, reason: 'High notification volume detected from current workflow', status: 'PENDING', tenantId: 'default', createdAt: h(6), updatedAt: h(6) },
  { id: 'sug-4', suggestionType: 'BEST_PRACTICE', name: 'Contact enrichment on creation', description: 'Automatically enrich contact data when a new contact is added. This can improve data quality and save ~15 min per contact.', entityType: 'CONTACT', triggerEvent: 'CREATED', conditionsJson: '[]', actionsJson: '[{"actionType":"UPDATE_FIELD","targetField":"enrichment","targetValue":"auto_enrich","actionOrder":1}]', confidence: 0.88, reason: 'Many contacts have incomplete data fields', status: 'ACCEPTED', acceptedRuleId: 'rule-10', tenantId: 'default', createdAt: h(72), updatedAt: h(48) },
];

const DEFAULT_PROPOSALS: Proposal[] = [
  { id: 'prop-1', opportunityId: 'opp-1', title: 'Enterprise CRM License — Acme Corp', content: '# Proposal: Enterprise CRM License\n\n**Prepared for:** Acme Corp\n**Date:** ' + new Date().toLocaleDateString() + '\n\n## Executive Summary\nWe are pleased to present our enterprise CRM solution tailored to Acme Corp\'s needs.\n\n## Pricing\n| Item | Qty | Price | Total |\n|------|-----|-------|-------|\n| Enterprise License | 50 | $200 | $10,000 |\n| Premium Support | 1 | $5,000 | $5,000 |\n| Training Package | 1 | $3,000 | $3,000 |\n\n**Total: $18,000**', status: 'SENT', amount: 18000, validUntil: new Date(Date.now() + 30 * 86400000).toISOString(), sentAt: h(48), recipientEmail: 'john@acme.com', recipientName: 'John Smith', version: 1, lineItems: [{ productName: 'Enterprise License', quantity: 50, unitPrice: 200, discount: 0, totalPrice: 10000, sortOrder: 1 }, { productName: 'Premium Support', quantity: 1, unitPrice: 5000, discount: 0, totalPrice: 5000, sortOrder: 2 }, { productName: 'Training Package', quantity: 1, unitPrice: 3000, discount: 0, totalPrice: 3000, sortOrder: 3 }], tenantId: 'default', createdAt: h(72), updatedAt: h(48) },
  { id: 'prop-2', opportunityId: 'opp-2', title: 'Startup Package — TechFlow Inc', content: '# Proposal: Startup Package\n\n**Prepared for:** TechFlow Inc\n\n## Pricing\n| Item | Qty | Price | Total |\n|------|-----|-------|-------|\n| Starter License | 10 | $100 | $1,000 |\n| Onboarding | 1 | $500 | $500 |\n\n**Total: $1,500**', status: 'DRAFT', amount: 1500, version: 1, lineItems: [{ productName: 'Starter License', quantity: 10, unitPrice: 100, discount: 0, totalPrice: 1000, sortOrder: 1 }, { productName: 'Onboarding', quantity: 1, unitPrice: 500, discount: 0, totalPrice: 500, sortOrder: 2 }], tenantId: 'default', createdAt: h(24), updatedAt: h(24) },
  { id: 'prop-3', opportunityId: 'opp-3', title: 'Premium Renewal — GlobalTrade Ltd', content: '# Renewal Proposal\n\n**Prepared for:** GlobalTrade Ltd\n\n**Total: $25,000**', status: 'ACCEPTED', amount: 25000, validUntil: h(-720), sentAt: h(168), viewedAt: h(144), respondedAt: h(120), recipientEmail: 'sarah@globaltrade.com', recipientName: 'Sarah Johnson', version: 2, lineItems: [{ productName: 'Enterprise Renewal', quantity: 100, unitPrice: 200, discount: 20, totalPrice: 16000, sortOrder: 1 }, { productName: 'Advanced Analytics', quantity: 1, unitPrice: 9000, discount: 0, totalPrice: 9000, sortOrder: 2 }], tenantId: 'default', createdAt: h(240), updatedAt: h(120) },
];

const DEFAULT_CONTRACTS: Contract[] = [
  { id: 'ctr-1', opportunityId: 'opp-3', proposalId: 'prop-3', title: 'Service Agreement — GlobalTrade Ltd', content: '# Service Agreement\n\n**Parties:** CRM Platform Inc. and GlobalTrade Ltd.\n\n## Terms\n- Duration: 12 months\n- Amount: $25,000/year\n- Payment: Net 30\n\n## Signatures\n- [x] Provider signed\n- [x] Client signed', status: 'EXECUTED', amount: 25000, startDate: h(-24), endDate: new Date(Date.now() + 365 * 86400000).toISOString(), sentAt: h(96), viewedAt: h(72), signedAt: h(48), executedAt: h(24), signerName: 'Sarah Johnson', signerEmail: 'sarah@globaltrade.com', version: 1, tenantId: 'default', createdAt: h(120), updatedAt: h(24) },
  { id: 'ctr-2', opportunityId: 'opp-1', title: 'Master Agreement — Acme Corp', content: '# Master Service Agreement\n\n**Parties:** CRM Platform Inc. and Acme Corp.\n\nAwaiting signature...', status: 'SENT', amount: 18000, startDate: now, endDate: new Date(Date.now() + 365 * 86400000).toISOString(), sentAt: h(24), signerEmail: 'john@acme.com', version: 1, tenantId: 'default', createdAt: h(48), updatedAt: h(24) },
];

/* ── Service ─────────────────────────────────────────────────── */
export const automationService = {

  /* ── Templates ──────────────────────────────────────────── */
  getTemplates: async (): Promise<WorkflowTemplate[]> => {
    const local = loadJson<WorkflowTemplate[]>(K.templates, DEFAULT_TEMPLATES);
    return tryApi(
      () => api.get<ApiResponse<WorkflowTemplate[]>>(`${BASE}/templates`).then(r => {
        const d = r.data.data as any;
        return Array.isArray(d) ? d : Array.isArray(d?.content) ? d.content : local;
      }),
      local,
    );
  },
  getTemplatesByEntityType: async (entityType: string): Promise<WorkflowTemplate[]> => {
    const all = await automationService.getTemplates();
    return all.filter(t => t.entityType === entityType);
  },

  /* ── Suggestions ────────────────────────────────────────── */
  getSuggestions: async (): Promise<WorkflowSuggestion[]> => {
    const local = loadJson<WorkflowSuggestion[]>(K.suggestions, DEFAULT_SUGGESTIONS);
    return tryApi(
      () => api.get<ApiResponse<WorkflowSuggestion[]>>(`${BASE}/suggestions`).then(r => {
        const d = r.data.data as any;
        return Array.isArray(d) ? d : Array.isArray(d?.content) ? d.content : local;
      }),
      local,
    );
  },
  getPendingSuggestions: async (): Promise<WorkflowSuggestion[]> => {
    const all = await automationService.getSuggestions();
    return all.filter(s => s.status === 'PENDING');
  },
  getPendingCount: async (): Promise<number> => {
    const pending = await automationService.getPendingSuggestions();
    return pending.length;
  },
  acceptSuggestion: async (id: string): Promise<void> => {
    const local = loadJson<WorkflowSuggestion[]>(K.suggestions, DEFAULT_SUGGESTIONS);
    const updated = local.map(s => s.id === id ? { ...s, status: 'ACCEPTED' as const, updatedAt: now } : s);
    saveJson(K.suggestions, updated);
    try { await api.post(`${BASE}/suggestions/${id}/accept`); } catch { /* local fallback */ }
  },
  dismissSuggestion: async (id: string): Promise<void> => {
    const local = loadJson<WorkflowSuggestion[]>(K.suggestions, DEFAULT_SUGGESTIONS);
    const updated = local.map(s => s.id === id ? { ...s, status: 'DISMISSED' as const, updatedAt: now } : s);
    saveJson(K.suggestions, updated);
    try { await api.post(`${BASE}/suggestions/${id}/dismiss`); } catch { /* local fallback */ }
  },
  generateSuggestions: async (): Promise<void> => {
    try { await api.post(`${BASE}/suggestions/generate`); } catch { /* ok */ }
  },

  /* ── Proposals ──────────────────────────────────────────── */
  getProposals: async (): Promise<Proposal[]> => {
    const local = loadJson<Proposal[]>(K.proposals, DEFAULT_PROPOSALS);
    return tryApi(
      () => api.get<ApiResponse<Proposal[]>>(`${BASE}/proposals`).then(r => {
        const d = r.data.data as any;
        return Array.isArray(d) ? d : Array.isArray(d?.content) ? d.content : local;
      }),
      local,
    );
  },
  getProposal: async (id: string): Promise<Proposal | null> => {
    const all = await automationService.getProposals();
    return all.find(p => p.id === id) ?? null;
  },
  getProposalsByOpportunity: async (opportunityId: string): Promise<Proposal[]> => {
    const all = await automationService.getProposals();
    return all.filter(p => p.opportunityId === opportunityId);
  },
  createProposal: async (data: CreateProposalRequest): Promise<Proposal> => {
    const lineItems = (data.lineItems ?? []).map((li, idx) => ({
      productName: li.productName, quantity: li.quantity, unitPrice: li.unitPrice,
      discount: li.discount ?? 0, totalPrice: li.quantity * li.unitPrice * (1 - (li.discount ?? 0) / 100),
      sortOrder: li.sortOrder ?? idx + 1,
    }));
    const amount = lineItems.reduce((sum, li) => sum + li.totalPrice, 0);
    const proposal: Proposal = {
      id: `prop-${Date.now()}`, ...data, status: 'DRAFT', amount, version: 1,
      lineItems, tenantId: 'default', createdAt: now, updatedAt: now,
    };
    const local = loadJson<Proposal[]>(K.proposals, DEFAULT_PROPOSALS);
    saveJson(K.proposals, [...local, proposal]);
    try {
      const resp = await api.post<ApiResponse<Proposal>>(`${BASE}/proposals`, data);
      if (resp.data?.data) return resp.data.data;
    } catch { /* local fallback */ }
    return proposal;
  },
  sendProposal: async (id: string): Promise<void> => {
    const local = loadJson<Proposal[]>(K.proposals, DEFAULT_PROPOSALS);
    const updated = local.map(p => p.id === id ? { ...p, status: 'SENT' as const, sentAt: now, updatedAt: now } : p);
    saveJson(K.proposals, updated);
    try { await api.post(`${BASE}/proposals/${id}/send`); } catch { /* local fallback */ }
  },
  acceptProposal: async (id: string): Promise<void> => {
    const local = loadJson<Proposal[]>(K.proposals, DEFAULT_PROPOSALS);
    const updated = local.map(p => p.id === id ? { ...p, status: 'ACCEPTED' as const, respondedAt: now, updatedAt: now } : p);
    saveJson(K.proposals, updated);
    try { await api.post(`${BASE}/proposals/${id}/accept`); } catch { /* local fallback */ }
  },
  rejectProposal: async (id: string): Promise<void> => {
    const local = loadJson<Proposal[]>(K.proposals, DEFAULT_PROPOSALS);
    const updated = local.map(p => p.id === id ? { ...p, status: 'REJECTED' as const, respondedAt: now, updatedAt: now } : p);
    saveJson(K.proposals, updated);
    try { await api.post(`${BASE}/proposals/${id}/reject`); } catch { /* local fallback */ }
  },
  deleteProposal: async (id: string): Promise<void> => {
    const local = loadJson<Proposal[]>(K.proposals, DEFAULT_PROPOSALS);
    saveJson(K.proposals, local.filter(p => p.id !== id));
    try { await api.delete(`${BASE}/proposals/${id}`); } catch { /* local fallback */ }
  },

  /* ── Contracts ──────────────────────────────────────────── */
  getContracts: async (): Promise<Contract[]> => {
    const local = loadJson<Contract[]>(K.contracts, DEFAULT_CONTRACTS);
    return tryApi(
      () => api.get<ApiResponse<Contract[]>>(`${BASE}/contracts`).then(r => {
        const d = r.data.data as any;
        return Array.isArray(d) ? d : Array.isArray(d?.content) ? d.content : local;
      }),
      local,
    );
  },
  getContract: async (id: string): Promise<Contract | null> => {
    const all = await automationService.getContracts();
    return all.find(c => c.id === id) ?? null;
  },
  getContractsByOpportunity: async (opportunityId: string): Promise<Contract[]> => {
    const all = await automationService.getContracts();
    return all.filter(c => c.opportunityId === opportunityId);
  },
  createContract: async (data: CreateContractRequest): Promise<Contract> => {
    const contract: Contract = {
      id: `ctr-${Date.now()}`, ...data, status: 'DRAFT', version: 1,
      tenantId: 'default', createdAt: now, updatedAt: now,
    };
    const local = loadJson<Contract[]>(K.contracts, DEFAULT_CONTRACTS);
    saveJson(K.contracts, [...local, contract]);
    try {
      const resp = await api.post<ApiResponse<Contract>>(`${BASE}/contracts`, data);
      if (resp.data?.data) return resp.data.data;
    } catch { /* local fallback */ }
    return contract;
  },
  sendContract: async (id: string): Promise<void> => {
    const local = loadJson<Contract[]>(K.contracts, DEFAULT_CONTRACTS);
    const updated = local.map(c => c.id === id ? { ...c, status: 'SENT' as const, sentAt: now, updatedAt: now } : c);
    saveJson(K.contracts, updated);
    try { await api.post(`${BASE}/contracts/${id}/send`); } catch { /* local fallback */ }
  },
  signContract: async (id: string, data: SignContractRequest): Promise<void> => {
    const local = loadJson<Contract[]>(K.contracts, DEFAULT_CONTRACTS);
    const updated = local.map(c => c.id === id ? { ...c, status: 'SIGNED' as const, signedAt: now, signerName: data.signerName, signerEmail: data.signerEmail, updatedAt: now } : c);
    saveJson(K.contracts, updated);
    try { await api.post(`${BASE}/contracts/${id}/sign`, data); } catch { /* local fallback */ }
  },
  executeContract: async (id: string): Promise<void> => {
    const local = loadJson<Contract[]>(K.contracts, DEFAULT_CONTRACTS);
    const updated = local.map(c => c.id === id ? { ...c, status: 'EXECUTED' as const, executedAt: now, updatedAt: now } : c);
    saveJson(K.contracts, updated);
    try { await api.post(`${BASE}/contracts/${id}/execute`); } catch { /* local fallback */ }
  },
  cancelContract: async (id: string): Promise<void> => {
    const local = loadJson<Contract[]>(K.contracts, DEFAULT_CONTRACTS);
    const updated = local.map(c => c.id === id ? { ...c, status: 'CANCELLED' as const, updatedAt: now } : c);
    saveJson(K.contracts, updated);
    try { await api.post(`${BASE}/contracts/${id}/cancel`); } catch { /* local fallback */ }
  },
  deleteContract: async (id: string): Promise<void> => {
    const local = loadJson<Contract[]>(K.contracts, DEFAULT_CONTRACTS);
    saveJson(K.contracts, local.filter(c => c.id !== id));
    try { await api.delete(`${BASE}/contracts/${id}`); } catch { /* local fallback */ }
  },
};
