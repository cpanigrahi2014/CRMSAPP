/* ============================================================
   Opportunity Service – Full CRUD + pipeline + analytics
   ============================================================ */
import api from './api';
import type {
  ApiResponse,
  PagedData,
  Opportunity,
  CreateOpportunityRequest,
  UpdateOpportunityRequest,
  OpportunityStage,
  OpportunityProduct,
  CreateProductRequest,
  OpportunityCompetitor,
  CreateCompetitorRequest,
  OpportunityActivity,
  OpportunityCollaborator,
  OpportunityNote,
  CreateNoteRequest,
  OpportunityReminder,
  CreateReminderRequest,
  ForecastSummary,
  RevenueAnalytics,
  WinLossAnalysis,
  OpportunityAlert,
  StageConversionAnalytics,
  PipelineDashboard,
  PipelinePerformance,
  SalesQuota,
  CreateSalesQuotaRequest,
} from '../types';

const BASE = '/api/v1/opportunities';

export const opportunityService = {
  /* ---- CRUD ---- */
  getAll: (page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Opportunity>>>(BASE, { params: { page, size } }).then((r) => r.data),

  getById: (id: string) =>
    api.get<ApiResponse<Opportunity>>(`${BASE}/${id}`).then((r) => r.data),

  create: (data: CreateOpportunityRequest) =>
    api.post<ApiResponse<Opportunity>>(BASE, data).then((r) => r.data),

  update: (id: string, data: UpdateOpportunityRequest) =>
    api.put<ApiResponse<Opportunity>>(`${BASE}/${id}`, data).then((r) => r.data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}`).then((r) => r.data),

  /* ---- Stage management ---- */
  updateStage: (id: string, stage: OpportunityStage) =>
    api.patch<ApiResponse<Opportunity>>(`${BASE}/${id}/stage`, null, { params: { stage } }).then((r) => r.data),

  getByStage: (stage: OpportunityStage, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Opportunity>>>(`${BASE}/stage/${stage}`, { params: { page, size } }).then((r) => r.data),

  /* ---- Queries ---- */
  getByAccount: (accountId: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Opportunity>>>(`${BASE}/account/${accountId}`, { params: { page, size } }).then((r) => r.data),

  getByAssignee: (assignedTo: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Opportunity>>>(`${BASE}/assignee/${assignedTo}`, { params: { page, size } }).then((r) => r.data),

  search: (q: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Opportunity>>>(`${BASE}/search`, { params: { q, page, size } }).then((r) => r.data),

  getByDateRange: (start: string, end: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Opportunity>>>(`${BASE}/date-range`, { params: { start, end, page, size } }).then((r) => r.data),

  /* ---- Products ---- */
  addProduct: (id: string, data: CreateProductRequest) =>
    api.post<ApiResponse<OpportunityProduct>>(`${BASE}/${id}/products`, data).then((r) => r.data),

  getProducts: (id: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<OpportunityProduct>>>(`${BASE}/${id}/products`, { params: { page, size } }).then((r) => r.data),

  deleteProduct: (id: string, productId: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}/products/${productId}`).then((r) => r.data),

  /* ---- Competitors ---- */
  addCompetitor: (id: string, data: CreateCompetitorRequest) =>
    api.post<ApiResponse<OpportunityCompetitor>>(`${BASE}/${id}/competitors`, data).then((r) => r.data),

  getCompetitors: (id: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<OpportunityCompetitor>>>(`${BASE}/${id}/competitors`, { params: { page, size } }).then((r) => r.data),

  deleteCompetitor: (id: string, competitorId: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}/competitors/${competitorId}`).then((r) => r.data),

  /* ---- Activity Timeline ---- */
  getActivities: (id: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<OpportunityActivity>>>(`${BASE}/${id}/activities`, { params: { page, size } }).then((r) => r.data),

  /* ---- Collaborators ---- */
  addCollaborator: (id: string, userId: string, role = 'MEMBER') =>
    api.post<ApiResponse<OpportunityCollaborator>>(`${BASE}/${id}/collaborators`, null, { params: { userId, role } }).then((r) => r.data),

  getCollaborators: (id: string) =>
    api.get<ApiResponse<OpportunityCollaborator[]>>(`${BASE}/${id}/collaborators`).then((r) => r.data),

  removeCollaborator: (id: string, userId: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}/collaborators/${userId}`).then((r) => r.data),

  /* ---- Notes ---- */
  addNote: (id: string, data: CreateNoteRequest) =>
    api.post<ApiResponse<OpportunityNote>>(`${BASE}/${id}/notes`, data).then((r) => r.data),

  getNotes: (id: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<OpportunityNote>>>(`${BASE}/${id}/notes`, { params: { page, size } }).then((r) => r.data),

  deleteNote: (id: string, noteId: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}/notes/${noteId}`).then((r) => r.data),

  /* ---- Reminders ---- */
  addReminder: (id: string, data: CreateReminderRequest) =>
    api.post<ApiResponse<OpportunityReminder>>(`${BASE}/${id}/reminders`, data).then((r) => r.data),

  getReminders: (id: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<OpportunityReminder>>>(`${BASE}/${id}/reminders`, { params: { page, size } }).then((r) => r.data),

  completeReminder: (id: string, reminderId: string) =>
    api.patch<ApiResponse<OpportunityReminder>>(`${BASE}/${id}/reminders/${reminderId}/complete`).then((r) => r.data),

  deleteReminder: (id: string, reminderId: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}/reminders/${reminderId}`).then((r) => r.data),

  getDueReminders: () =>
    api.get<ApiResponse<OpportunityReminder[]>>(`${BASE}/reminders/due`).then((r) => r.data),

  /* ---- Prediction ---- */
  predictCloseDate: (id: string) =>
    api.get<ApiResponse<Opportunity>>(`${BASE}/${id}/predict-close`).then((r) => r.data),

  /* ---- Forecasting ---- */
  getForecast: () =>
    api.get<ApiResponse<ForecastSummary>>(`${BASE}/forecast`).then((r) => r.data),

  /* ---- Analytics ---- */
  getRevenueAnalytics: () =>
    api.get<ApiResponse<RevenueAnalytics>>(`${BASE}/analytics/revenue`).then((r) => r.data),

  getWinLossAnalysis: () =>
    api.get<ApiResponse<WinLossAnalysis>>(`${BASE}/analytics/win-loss`).then((r) => r.data),

  /* ---- Alerts ---- */
  getAlerts: () =>
    api.get<ApiResponse<OpportunityAlert[]>>(`${BASE}/alerts`).then((r) => r.data),

  /* ---- Stage Conversion Analytics ---- */
  getConversionAnalytics: () =>
    api.get<ApiResponse<StageConversionAnalytics>>(`${BASE}/analytics/conversion`).then((r) => r.data),

  /* ---- Pipeline Dashboard ---- */
  getDashboard: () =>
    api.get<ApiResponse<PipelineDashboard>>(`${BASE}/dashboard`).then((r) => r.data),

  /* ---- Pipeline Performance ---- */
  getPerformance: () =>
    api.get<ApiResponse<PipelinePerformance>>(`${BASE}/analytics/performance`).then((r) => r.data),

  /* ---- Pipeline View ---- */
  getPipelineView: () =>
    api.get<ApiResponse<Record<string, Opportunity[]>>>(`${BASE}/pipeline`).then((r) => r.data),

  /* ---- Sales Quotas ---- */
  getQuotas: () =>
    api.get<ApiResponse<SalesQuota[]>>('/api/v1/quotas').then((r) => r.data),

  getActiveQuotas: () =>
    api.get<ApiResponse<SalesQuota[]>>('/api/v1/quotas/active').then((r) => r.data),

  createQuota: (data: CreateSalesQuotaRequest) =>
    api.post<ApiResponse<SalesQuota>>('/api/v1/quotas', data).then((r) => r.data),

  updateQuota: (id: string, data: Partial<CreateSalesQuotaRequest>) =>
    api.put<ApiResponse<SalesQuota>>(`/api/v1/quotas/${id}`, data).then((r) => r.data),

  deleteQuota: (id: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/quotas/${id}`).then((r) => r.data),

  recalculateQuotas: () =>
    api.post<ApiResponse<void>>('/api/v1/quotas/recalculate').then((r) => r.data),
};
