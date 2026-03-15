/* ============================================================
   Campaign Service – connects to campaign-service backend
   ============================================================ */
import api from './api';
import type { ApiResponse, PagedData, Campaign } from '../types';

const BASE = '/api/v1/campaigns';

export const campaignService = {
  getAll: (page = 0, size = 20, status?: string, type?: string, sortBy = 'createdAt', sortDir = 'desc') =>
    api.get<ApiResponse<PagedData<Campaign>>>(BASE, { params: { page, size, status, type, sortBy, sortDir } }).then(r => r.data),

  getById: (id: string) =>
    api.get<ApiResponse<Campaign>>(`${BASE}/${id}`).then(r => r.data),

  create: (data: Partial<Campaign>) =>
    api.post<ApiResponse<Campaign>>(BASE, data).then(r => r.data),

  update: (id: string, data: Partial<Campaign>) =>
    api.put<ApiResponse<Campaign>>(`${BASE}/${id}`, data).then(r => r.data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}`).then(r => r.data),

  addMembers: (campaignId: string, memberIds: string[]) =>
    api.post<ApiResponse<void>>(`${BASE}/${campaignId}/members`, memberIds).then(r => r.data),

  getMembers: (campaignId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<any>>>(`${BASE}/${campaignId}/members`, { params: { page, size } }).then(r => r.data),

  updateMemberStatus: (campaignId: string, memberId: string, status: string) =>
    api.patch<ApiResponse<void>>(`${BASE}/${campaignId}/members/${memberId}/status`, null, { params: { status } }).then(r => r.data),

  getRoi: (campaignId: string) =>
    api.get<ApiResponse<any>>(`${BASE}/${campaignId}/roi`).then(r => r.data),
};
