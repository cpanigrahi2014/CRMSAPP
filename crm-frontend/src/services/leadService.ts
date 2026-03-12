/* ============================================================
   Lead Service – full-featured lead management
   ============================================================ */
import api from './api';
import type {
  ApiResponse, PagedData, Lead, CreateLeadRequest, UpdateLeadRequest, ConvertLeadRequest,
  LeadNote, LeadTag, LeadAttachment, LeadActivity, AssignmentRule, ScoringRule,
  BulkUpdateRequest, LeadAnalytics, WebForm,
} from '../types';

const BASE = '/api/v1/leads';

export const leadService = {
  // ── Core CRUD ──────────────────────────────────────────
  getAll: (page = 0, size = 20, sortBy = 'createdAt', sortDir = 'desc') =>
    api.get<ApiResponse<PagedData<Lead>>>(BASE, { params: { page, size, sortBy, sortDir } }).then(r => r.data),

  getById: (id: string) =>
    api.get<ApiResponse<Lead>>(`${BASE}/${id}`).then(r => r.data),

  create: (data: CreateLeadRequest) =>
    api.post<ApiResponse<Lead>>(BASE, data).then(r => r.data),

  update: (id: string, data: UpdateLeadRequest) =>
    api.put<ApiResponse<Lead>>(`${BASE}/${id}`, data).then(r => r.data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}`).then(r => r.data),

  search: (query: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Lead>>>(`${BASE}/search`, { params: { query, page, size } }).then(r => r.data),

  assign: (id: string, assigneeId: string) =>
    api.patch<ApiResponse<Lead>>(`${BASE}/${id}/assign`, null, { params: { assigneeId } }).then(r => r.data),

  convert: (id: string, data: ConvertLeadRequest) =>
    api.post<ApiResponse<Lead>>(`${BASE}/${id}/convert`, data).then(r => r.data),

  // ── Notes ──────────────────────────────────────────────
  addNote: (leadId: string, content: string) =>
    api.post<ApiResponse<LeadNote>>(`${BASE}/${leadId}/notes`, { content }).then(r => r.data),

  getNotes: (leadId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<LeadNote>>>(`${BASE}/${leadId}/notes`, { params: { page, size } }).then(r => r.data),

  deleteNote: (noteId: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/notes/${noteId}`).then(r => r.data),

  // ── Tags ───────────────────────────────────────────────
  getAllTags: () =>
    api.get<ApiResponse<LeadTag[]>>(`${BASE}/tags`).then(r => r.data),

  createTag: (name: string, color?: string) =>
    api.post<ApiResponse<LeadTag>>(`${BASE}/tags`, { name, color }).then(r => r.data),

  getLeadTags: (leadId: string) =>
    api.get<ApiResponse<LeadTag[]>>(`${BASE}/${leadId}/tags`).then(r => r.data),

  addTagToLead: (leadId: string, tagId: string) =>
    api.post<ApiResponse<void>>(`${BASE}/${leadId}/tags/${tagId}`).then(r => r.data),

  removeTagFromLead: (leadId: string, tagId: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${leadId}/tags/${tagId}`).then(r => r.data),

  // ── Attachments ────────────────────────────────────────
  addAttachment: (leadId: string, file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return api.post<ApiResponse<LeadAttachment>>(`${BASE}/${leadId}/attachments`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },

  getAttachments: (leadId: string) =>
    api.get<ApiResponse<LeadAttachment[]>>(`${BASE}/${leadId}/attachments`).then(r => r.data),

  downloadAttachment: (attachmentId: string) =>
    api.get(`${BASE}/attachments/${attachmentId}/download`, { responseType: 'blob' }).then(r => r.data),

  deleteAttachment: (attachmentId: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/attachments/${attachmentId}`).then(r => r.data),

  // ── Activities / Timeline ──────────────────────────────
  getActivities: (leadId: string, type?: string, page = 0, size = 50) =>
    api.get<ApiResponse<PagedData<LeadActivity>>>(`${BASE}/${leadId}/activities`, {
      params: { type: type || undefined, page, size },
    }).then(r => r.data),

  // ── Duplicate Detection ────────────────────────────────
  findDuplicates: (email?: string, phone?: string) =>
    api.get<ApiResponse<Lead[]>>(`${BASE}/duplicates`, { params: { email, phone } }).then(r => r.data),

  // ── Bulk Operations ────────────────────────────────────
  bulkUpdate: (data: BulkUpdateRequest) =>
    api.post<ApiResponse<{ affected: number }>>(`${BASE}/bulk`, data).then(r => r.data),

  // ── Import / Export ────────────────────────────────────
  importCSV: (file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return api.post<ApiResponse<{ imported: number; errors: number }>>(`${BASE}/import`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },

  exportCSV: () =>
    api.get(`${BASE}/export`, { responseType: 'blob' }).then(r => r.data),

  // ── Assignment Rules ───────────────────────────────────
  getAssignmentRules: () =>
    api.get<ApiResponse<AssignmentRule[]>>(`${BASE}/assignment-rules`).then(r => r.data),

  createAssignmentRule: (data: Omit<AssignmentRule, 'id' | 'createdAt' | 'updatedAt'>) =>
    api.post<ApiResponse<AssignmentRule>>(`${BASE}/assignment-rules`, data).then(r => r.data),

  deleteAssignmentRule: (id: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/assignment-rules/${id}`).then(r => r.data),

  // ── Scoring Rules ──────────────────────────────────────
  getScoringRules: () =>
    api.get<ApiResponse<ScoringRule[]>>(`${BASE}/scoring-rules`).then(r => r.data),

  createScoringRule: (data: Partial<ScoringRule>) =>
    api.post<ApiResponse<ScoringRule>>(`${BASE}/scoring-rules`, data).then(r => r.data),

  deleteScoringRule: (id: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/scoring-rules/${id}`).then(r => r.data),

  recalculateScore: (leadId: string) =>
    api.post<ApiResponse<Lead>>(`${BASE}/${leadId}/recalculate-score`).then(r => r.data),

  // ── Analytics ──────────────────────────────────────────
  getAnalytics: () =>
    api.get<ApiResponse<LeadAnalytics>>(`${BASE}/analytics`).then(r => r.data),

  // ── SLA ────────────────────────────────────────────────
  getSlaBreached: () =>
    api.get<ApiResponse<Lead[]>>(`${BASE}/sla-breached`).then(r => r.data),

  // ── Campaign ───────────────────────────────────────────
  getLeadsByCampaign: (campaignId: string) =>
    api.get<ApiResponse<Lead[]>>(`${BASE}/campaign/${campaignId}`).then(r => r.data),

  // ── Web Forms ──────────────────────────────────────────
  getWebForms: () =>
    api.get<ApiResponse<WebForm[]>>(`${BASE}/web-forms`).then(r => r.data),

  createWebForm: (data: Partial<WebForm>) =>
    api.post<ApiResponse<WebForm>>(`${BASE}/web-forms`, data).then(r => r.data),

  submitWebForm: (formId: string, data: CreateLeadRequest) =>
    api.post<ApiResponse<Lead>>(`${BASE}/web-forms/${formId}/submit`, data).then(r => r.data),

  // ── Email Capture ──────────────────────────────────────
  captureEmail: (email: string, source?: string) =>
    api.post<ApiResponse<Lead>>(`${BASE}/capture-email`, null, { params: { email, source } }).then(r => r.data),

  // ── Territory ──────────────────────────────────────────
  getLeadsByTerritory: (territory: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Lead>>>(`${BASE}/territory/${territory}`, { params: { page, size } }).then(r => r.data),
};
