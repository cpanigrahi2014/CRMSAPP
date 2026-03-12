/* ============================================================
   Email Service – accounts, templates, messages, tracking,
   schedules, analytics
   ============================================================ */
import api from './api';
import type {
  EmailAccount,
  CreateEmailAccountRequest,
  EmailTemplate,
  CreateEmailTemplateRequest,
  UpdateEmailTemplateRequest,
  EmailMessage,
  SendEmailRequest,
  EmailTrackingEvent,
  EmailSchedule,
  EmailAnalytics,
  OAuthConnectResponse,
} from '../types';

const BASE = '/api/v1/email';

/* ────────────────────── Spring returns raw objects for email (no ApiResponse wrapper) */

export const emailService = {
  /* ── Accounts ───────────────────────────────────────────── */
  getAccounts: () =>
    api.get<EmailAccount[]>(`${BASE}/accounts`).then((r) => r.data),

  createSmtpAccount: (data: CreateEmailAccountRequest) =>
    api.post<EmailAccount>(`${BASE}/accounts/smtp`, data).then((r) => r.data),

  getGmailAuthUrl: () =>
    api.get<OAuthConnectResponse>(`${BASE}/accounts/oauth/gmail/url`).then((r) => r.data),

  connectGmail: (code: string) =>
    api.post<EmailAccount>(`${BASE}/accounts/oauth/gmail/callback`, null, { params: { code } }).then((r) => r.data),

  getOutlookAuthUrl: () =>
    api.get<OAuthConnectResponse>(`${BASE}/accounts/oauth/outlook/url`).then((r) => r.data),

  connectOutlook: (code: string) =>
    api.post<EmailAccount>(`${BASE}/accounts/oauth/outlook/callback`, null, { params: { code } }).then((r) => r.data),

  setDefaultAccount: (id: string) =>
    api.put<EmailAccount>(`${BASE}/accounts/${id}/default`).then((r) => r.data),

  disconnectAccount: (id: string) =>
    api.post<void>(`${BASE}/accounts/${id}/disconnect`).then((r) => r.data),

  deleteAccount: (id: string) =>
    api.delete<void>(`${BASE}/accounts/${id}`).then((r) => r.data),

  /* ── Templates ──────────────────────────────────────────── */
  getTemplates: (page = 0, size = 100) =>
    api.get<{ content: EmailTemplate[]; totalElements: number }>(`${BASE}/templates`, { params: { page, size } }).then((r) => r.data),

  getActiveTemplates: () =>
    api.get<EmailTemplate[]>(`${BASE}/templates/active`).then((r) => r.data),

  getTemplate: (id: string) =>
    api.get<EmailTemplate>(`${BASE}/templates/${id}`).then((r) => r.data),

  createTemplate: (data: CreateEmailTemplateRequest) =>
    api.post<EmailTemplate>(`${BASE}/templates`, data).then((r) => r.data),

  updateTemplate: (id: string, data: UpdateEmailTemplateRequest) =>
    api.put<EmailTemplate>(`${BASE}/templates/${id}`, data).then((r) => r.data),

  deleteTemplate: (id: string) =>
    api.delete<void>(`${BASE}/templates/${id}`).then((r) => r.data),

  previewTemplate: (id: string, sampleData?: Record<string, string>) =>
    api.post<{ subject: string; bodyHtml: string }>(`${BASE}/templates/${id}/preview`, sampleData || {}).then((r) => r.data),

  /* ── Messages ───────────────────────────────────────────── */
  sendEmail: (data: SendEmailRequest) =>
    api.post<EmailMessage>(`${BASE}/messages/send`, data).then((r) => r.data),

  getMessages: (page = 0, size = 20) =>
    api.get<{ content: EmailMessage[]; totalElements: number }>(`${BASE}/messages`, { params: { page, size } }).then((r) => r.data),

  getSentMessages: (page = 0, size = 20) =>
    api.get<{ content: EmailMessage[]; totalElements: number }>(`${BASE}/messages/sent`, { params: { page, size } }).then((r) => r.data),

  getMessage: (id: string) =>
    api.get<EmailMessage>(`${BASE}/messages/${id}`).then((r) => r.data),

  getThread: (threadId: string) =>
    api.get<EmailMessage[]>(`${BASE}/messages/thread/${threadId}`).then((r) => r.data),

  getByEntity: (entityType: string, entityId: string) =>
    api.get<EmailMessage[]>(`${BASE}/messages/entity/${entityType}/${entityId}`).then((r) => r.data),

  searchMessages: (query: string, page = 0, size = 20) =>
    api.get<{ content: EmailMessage[]; totalElements: number }>(`${BASE}/messages/search`, { params: { query, page, size } }).then((r) => r.data),

  /* ── Tracking ───────────────────────────────────────────── */
  getTrackingEvents: (messageId: string) =>
    api.get<EmailTrackingEvent[]>(`${BASE}/track/events/${messageId}`).then((r) => r.data),

  /* ── Schedules ──────────────────────────────────────────── */
  getSchedules: (page = 0, size = 20) =>
    api.get<{ content: EmailSchedule[]; totalElements: number }>(`${BASE}/schedules`, { params: { page, size } }).then((r) => r.data),

  cancelSchedule: (id: string) =>
    api.post<EmailSchedule>(`${BASE}/schedules/${id}/cancel`).then((r) => r.data),

  deleteSchedule: (id: string) =>
    api.delete<void>(`${BASE}/schedules/${id}`).then((r) => r.data),

  /* ── Analytics ──────────────────────────────────────────── */
  getAnalytics: () =>
    api.get<EmailAnalytics>(`${BASE}/analytics`).then((r) => r.data),
};
