/* ============================================================
   Calendar Sync Service – Integration-service calendar APIs
   ============================================================ */
import api from './api';
import type { ApiResponse, CalendarSyncConfig } from '../types';

const BASE = '/api/v1/integrations/calendar';

export const calendarSyncService = {
  getConnections: () =>
    api.get<ApiResponse<CalendarSyncConfig[]>>(`${BASE}/connections`).then((r) => r.data),

  getSyncStatus: (provider: string) =>
    api.get<ApiResponse<CalendarSyncConfig>>(`${BASE}/status/${provider}`).then((r) => r.data),

  getGoogleAuthUrl: () =>
    api.get<ApiResponse<{ authUrl: string }>>(`${BASE}/google/auth-url`).then((r) => r.data),

  handleGoogleCallback: (code: string) =>
    api.post<ApiResponse<CalendarSyncConfig>>(`${BASE}/google/callback`, null, { params: { code } }).then((r) => r.data),

  updateConfig: (id: string, data: Partial<Pick<CalendarSyncConfig, 'calendarId' | 'syncDirection' | 'syncIntervalMinutes' | 'enabled'>>) =>
    api.put<ApiResponse<CalendarSyncConfig>>(`${BASE}/${id}`, data).then((r) => r.data),

  disconnect: (id: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}`).then((r) => r.data),
};
