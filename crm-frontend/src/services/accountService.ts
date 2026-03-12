/* ============================================================
   Account Service – Full CRUD + features for accounts
   ============================================================ */
import api from './api';
import type {
  ApiResponse, PagedData, Account, CreateAccountRequest, UpdateAccountRequest,
  AccountNote, AccountTag, AccountAttachment, AccountActivity, AccountAnalytics,
  BulkAccountUpdateRequest,
} from '../types';

export const accountService = {
  // ── CRUD ─────────────────────────────────────────────────
  getAll: (page = 0, size = 20, sortBy = 'createdAt', sortDir = 'desc') =>
    api.get<ApiResponse<PagedData<Account>>>('/api/v1/accounts', { params: { page, size, sortBy, sortDir } }).then((r) => r.data),

  getById: (id: string) =>
    api.get<ApiResponse<Account>>(`/api/v1/accounts/${id}`).then((r) => r.data),

  create: (data: CreateAccountRequest) =>
    api.post<ApiResponse<Account>>('/api/v1/accounts', data).then((r) => r.data),

  update: (id: string, data: Partial<UpdateAccountRequest>) =>
    api.put<ApiResponse<Account>>(`/api/v1/accounts/${id}`, data).then((r) => r.data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/accounts/${id}`).then((r) => r.data),

  search: (query: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Account>>>('/api/v1/accounts/search', { params: { query, page, size } }).then((r) => r.data),

  // ── Hierarchy ────────────────────────────────────────────
  getChildren: (parentId: string) =>
    api.get<ApiResponse<Account[]>>(`/api/v1/accounts/${parentId}/children`).then((r) => r.data),

  // ── Owner assignment ─────────────────────────────────────
  assignOwner: (accountId: string, ownerId: string) =>
    api.put<ApiResponse<Account>>(`/api/v1/accounts/${accountId}/owner`, { ownerId }).then((r) => r.data),

  // ── Territory assignment ─────────────────────────────────
  assignTerritory: (accountId: string, territory: string) =>
    api.put<ApiResponse<Account>>(`/api/v1/accounts/${accountId}/territory`, { territory }).then((r) => r.data),

  // ── Filter endpoints ─────────────────────────────────────
  getByType: (type: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Account>>>('/api/v1/accounts/by-type', { params: { type, page, size } }).then((r) => r.data),

  getByTerritory: (territory: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Account>>>('/api/v1/accounts/by-territory', { params: { territory, page, size } }).then((r) => r.data),

  getBySegment: (segment: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Account>>>('/api/v1/accounts/by-segment', { params: { segment, page, size } }).then((r) => r.data),

  getByLifecycle: (stage: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Account>>>('/api/v1/accounts/by-lifecycle', { params: { stage, page, size } }).then((r) => r.data),

  getByOwner: (ownerId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Account>>>('/api/v1/accounts/by-owner', { params: { ownerId, page, size } }).then((r) => r.data),

  // ── Notes ────────────────────────────────────────────────
  addNote: (accountId: string, content: string) =>
    api.post<ApiResponse<AccountNote>>(`/api/v1/accounts/${accountId}/notes`, { content }).then((r) => r.data),

  getNotes: (accountId: string) =>
    api.get<ApiResponse<AccountNote[]>>(`/api/v1/accounts/${accountId}/notes`).then((r) => r.data),

  deleteNote: (noteId: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/accounts/notes/${noteId}`).then((r) => r.data),

  // ── Attachments ──────────────────────────────────────────
  addAttachment: (accountId: string, data: { fileName: string; fileUrl: string; fileSize?: number; fileType?: string }) =>
    api.post<ApiResponse<AccountAttachment>>(`/api/v1/accounts/${accountId}/attachments`, data).then((r) => r.data),

  getAttachments: (accountId: string) =>
    api.get<ApiResponse<AccountAttachment[]>>(`/api/v1/accounts/${accountId}/attachments`).then((r) => r.data),

  deleteAttachment: (attachmentId: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/accounts/attachments/${attachmentId}`).then((r) => r.data),

  // ── Activities / Timeline ────────────────────────────────
  getActivities: (accountId: string) =>
    api.get<ApiResponse<AccountActivity[]>>(`/api/v1/accounts/${accountId}/activities`).then((r) => r.data),

  // ── Tags ─────────────────────────────────────────────────
  createTag: (name: string, color?: string) =>
    api.post<ApiResponse<AccountTag>>('/api/v1/accounts/tags', { name, color }).then((r) => r.data),

  getAllTags: () =>
    api.get<ApiResponse<AccountTag[]>>('/api/v1/accounts/tags').then((r) => r.data),

  getAccountTags: (accountId: string) =>
    api.get<ApiResponse<AccountTag[]>>(`/api/v1/accounts/${accountId}/tags`).then((r) => r.data),

  addTagToAccount: (accountId: string, tagId: string) =>
    api.post<ApiResponse<void>>(`/api/v1/accounts/${accountId}/tags/${tagId}`).then((r) => r.data),

  removeTagFromAccount: (accountId: string, tagId: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/accounts/${accountId}/tags/${tagId}`).then((r) => r.data),

  // ── Duplicate Detection ──────────────────────────────────
  detectDuplicates: (name?: string, phone?: string, website?: string) =>
    api.get<ApiResponse<Account[]>>('/api/v1/accounts/duplicates', { params: { name, phone, website } }).then((r) => r.data),

  // ── Health Score / Engagement ────────────────────────────
  updateHealthScore: (accountId: string, healthScore: number) =>
    api.put<ApiResponse<Account>>(`/api/v1/accounts/${accountId}/health-score`, { healthScore }).then((r) => r.data),

  updateEngagementScore: (accountId: string, engagementScore: number) =>
    api.put<ApiResponse<Account>>(`/api/v1/accounts/${accountId}/engagement-score`, { engagementScore }).then((r) => r.data),

  // ── Import / Export ──────────────────────────────────────
  importCsv: (csvContent: string) =>
    api.post<ApiResponse<{ imported: number }>>('/api/v1/accounts/import', csvContent, {
      headers: { 'Content-Type': 'text/plain' },
    }).then((r) => r.data),

  exportCsv: () =>
    api.get<string>('/api/v1/accounts/export', { responseType: 'text' as any }).then((r) => r.data),

  // ── Bulk Operations ──────────────────────────────────────
  bulkUpdate: (data: BulkAccountUpdateRequest) =>
    api.put<ApiResponse<{ updated: number }>>('/api/v1/accounts/bulk-update', data).then((r) => r.data),

  bulkDelete: (accountIds: string[]) =>
    api.post<ApiResponse<{ deleted: number }>>('/api/v1/accounts/bulk-delete', accountIds).then((r) => r.data),

  // ── Analytics / Reporting / Dashboard ────────────────────
  getAnalytics: () =>
    api.get<ApiResponse<AccountAnalytics>>('/api/v1/accounts/analytics').then((r) => r.data),
};
