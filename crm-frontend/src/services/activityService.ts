/* ============================================================
   Activity Service – CRUD + timeline, calendar, analytics
   ============================================================ */
import api from './api';
import type {
  ApiResponse,
  PagedData,
  Activity,
  CreateActivityRequest,
  UpdateActivityRequest,
  ActivityAnalytics,
} from '../types';

const BASE = '/api/v1/activities';

export const activityService = {
  /* ---- CRUD ---- */
  getAll: (page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Activity>>>(BASE, { params: { page, size } }).then((r) => r.data),

  getById: (id: string) =>
    api.get<ApiResponse<Activity>>(`${BASE}/${id}`).then((r) => r.data),

  create: (data: CreateActivityRequest) =>
    api.post<ApiResponse<Activity>>(BASE, data).then((r) => r.data),

  update: (id: string, data: UpdateActivityRequest) =>
    api.put<ApiResponse<Activity>>(`${BASE}/${id}`, data).then((r) => r.data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}`).then((r) => r.data),

  /* ---- Status ---- */
  complete: (id: string) =>
    api.patch<ApiResponse<Activity>>(`${BASE}/${id}/complete`).then((r) => r.data),

  /* ---- Queries ---- */
  getByType: (type: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Activity>>>(`${BASE}/type/${type}`, { params: { page, size } }).then((r) => r.data),

  getByStatus: (status: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Activity>>>(`${BASE}/status/${status}`, { params: { page, size } }).then((r) => r.data),

  getByAssignee: (assignedTo: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Activity>>>(`${BASE}/assignee/${assignedTo}`, { params: { page, size } }).then((r) => r.data),

  getByEntity: (entityId: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Activity>>>(`${BASE}/entity/${entityId}`, { params: { page, size } }).then((r) => r.data),

  search: (q: string, page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Activity>>>(`${BASE}/search`, { params: { q, page, size } }).then((r) => r.data),

  getOverdue: (page = 0, size = 100) =>
    api.get<ApiResponse<PagedData<Activity>>>(`${BASE}/overdue`, { params: { page, size } }).then((r) => r.data),

  /* ---- Timeline ---- */
  getTimeline: () =>
    api.get<ApiResponse<Activity[]>>(`${BASE}/timeline`).then((r) => r.data),

  getEntityTimeline: (relatedEntityId: string) =>
    api.get<ApiResponse<Activity[]>>(`${BASE}/timeline/${relatedEntityId}`).then((r) => r.data),

  /* ---- Calendar / Upcoming ---- */
  getUpcoming: (days = 7) =>
    api.get<ApiResponse<Activity[]>>(`${BASE}/upcoming`, { params: { days } }).then((r) => r.data),

  /* ---- Analytics ---- */
  getAnalytics: () =>
    api.get<ApiResponse<ActivityAnalytics>>(`${BASE}/analytics`).then((r) => r.data),
};
