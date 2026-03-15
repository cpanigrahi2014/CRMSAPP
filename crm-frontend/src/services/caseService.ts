/* ============================================================
   Case Service – connects to case-service backend
   ============================================================ */
import api from './api';
import type { ApiResponse, PagedData, SupportCase } from '../types';

const BASE = '/api/v1/cases';

export const caseService = {
  getAll: (page = 0, size = 20, status?: string, priority?: string, sortBy = 'createdAt', sortDir = 'desc') =>
    api.get<ApiResponse<PagedData<SupportCase>>>(BASE, { params: { page, size, status, priority, sortBy, sortDir } }).then(r => r.data),

  getById: (id: string) =>
    api.get<ApiResponse<SupportCase>>(`${BASE}/${id}`).then(r => r.data),

  getByCaseNumber: (caseNumber: string) =>
    api.get<ApiResponse<SupportCase>>(`${BASE}/number/${caseNumber}`).then(r => r.data),

  create: (data: Partial<SupportCase>) =>
    api.post<ApiResponse<SupportCase>>(BASE, data).then(r => r.data),

  update: (id: string, data: Partial<SupportCase>) =>
    api.put<ApiResponse<SupportCase>>(`${BASE}/${id}`, data).then(r => r.data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}`).then(r => r.data),

  resolve: (id: string, resolutionNotes?: string) =>
    api.patch<ApiResponse<SupportCase>>(`${BASE}/${id}/resolve`, null, { params: { resolutionNotes } }).then(r => r.data),

  close: (id: string) =>
    api.patch<ApiResponse<SupportCase>>(`${BASE}/${id}/close`).then(r => r.data),

  escalate: (id: string) =>
    api.patch<ApiResponse<SupportCase>>(`${BASE}/${id}/escalate`).then(r => r.data),

  submitCsat: (id: string, score: number, comment?: string) =>
    api.post<ApiResponse<void>>(`${BASE}/${id}/csat`, { score, comment }).then(r => r.data),

  getAnalytics: () =>
    api.get<ApiResponse<any>>(`${BASE}/analytics`).then(r => r.data),
};
