/* ============================================================
   Contact Service – full-featured CRUD + 10 features
   ============================================================ */
import api from './api';
import type {
  ApiResponse, PagedData, Contact, CreateContactRequest,
  UpdateConsentRequest, ContactCommunication, CreateCommunicationRequest,
  ContactActivity, ContactTag, DuplicateContactGroup, ContactAnalytics,
  ContactNote, ContactAttachment,
} from '../types';

export const contactService = {
  // ── Feature 1: CRUD ──
  getAll: (page = 0, size = 20, sortBy = 'createdAt', sortDir = 'desc') =>
    api.get<ApiResponse<PagedData<Contact>>>('/api/v1/contacts', { params: { page, size, sortBy, sortDir } }).then((r) => r.data),

  getById: (id: string) =>
    api.get<ApiResponse<Contact>>(`/api/v1/contacts/${id}`).then((r) => r.data),

  create: (data: CreateContactRequest) =>
    api.post<ApiResponse<Contact>>('/api/v1/contacts', data).then((r) => r.data),

  update: (id: string, data: Partial<CreateContactRequest>) =>
    api.put<ApiResponse<Contact>>(`/api/v1/contacts/${id}`, data).then((r) => r.data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/contacts/${id}`).then((r) => r.data),

  search: (query: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Contact>>>('/api/v1/contacts/search', { params: { query, page, size } }).then((r) => r.data),

  // ── Feature 2: Account linking ──
  getByAccount: (accountId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Contact>>>(`/api/v1/contacts/account/${accountId}`, { params: { page, size } }).then((r) => r.data),

  // ── Feature 3: Communication history ──
  addCommunication: (contactId: string, data: CreateCommunicationRequest) =>
    api.post<ApiResponse<ContactCommunication>>(`/api/v1/contacts/${contactId}/communications`, data).then((r) => r.data),

  getCommunications: (contactId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<ContactCommunication>>>(`/api/v1/contacts/${contactId}/communications`, { params: { page, size } }).then((r) => r.data),

  deleteCommunication: (commId: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/contacts/communications/${commId}`).then((r) => r.data),

  // ── Feature 5: Segmentation ──
  getBySegment: (segment: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Contact>>>(`/api/v1/contacts/segment/${segment}`, { params: { page, size } }).then((r) => r.data),

  getByLifecycle: (stage: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<Contact>>>(`/api/v1/contacts/lifecycle/${stage}`, { params: { page, size } }).then((r) => r.data),

  // ── Feature 6: Marketing consent ──
  updateConsent: (contactId: string, data: UpdateConsentRequest) =>
    api.put<ApiResponse<Contact>>(`/api/v1/contacts/${contactId}/consent`, data).then((r) => r.data),

  // ── Feature 7: Activity timeline ──
  getActivities: (contactId: string, page = 0, size = 50) =>
    api.get<ApiResponse<PagedData<ContactActivity>>>(`/api/v1/contacts/${contactId}/activities`, { params: { page, size } }).then((r) => r.data),

  // ── Feature 8: Tagging ──
  addTag: (contactId: string, tagName: string) =>
    api.post<ApiResponse<ContactTag>>(`/api/v1/contacts/${contactId}/tags`, null, { params: { tagName } }).then((r) => r.data),

  getTags: (contactId: string) =>
    api.get<ApiResponse<ContactTag[]>>(`/api/v1/contacts/${contactId}/tags`).then((r) => r.data),

  removeTag: (contactId: string, tagName: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/contacts/${contactId}/tags`, { params: { tagName } }).then((r) => r.data),

  // ── Feature 9: Duplicate detection ──
  detectDuplicates: () =>
    api.get<ApiResponse<DuplicateContactGroup[]>>('/api/v1/contacts/duplicates').then((r) => r.data),

  mergeContacts: (primaryId: string, duplicateId: string) =>
    api.post<ApiResponse<Contact>>('/api/v1/contacts/merge', null, { params: { primaryId, duplicateId } }).then((r) => r.data),

  // ── Feature 10: Analytics ──
  getAnalytics: () =>
    api.get<ApiResponse<ContactAnalytics>>('/api/v1/contacts/analytics').then((r) => r.data),

  // ── Import / Export ──
  importCsv: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<ApiResponse<{ imported: number }>>('/api/v1/contacts/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data);
  },

  exportCsv: () =>
    api.get<string>('/api/v1/contacts/export', { responseType: 'text' as any }).then((r) => r.data),

  // ── Data Health Scan ──
  getDataHealth: (staleDays = 30) =>
    api.get<ApiResponse<any>>('/api/v1/contacts/data-health', { params: { staleDays } }).then((r) => r.data),

  // ── Notes ──
  addNote: (contactId: string, content: string) =>
    api.post<ApiResponse<ContactNote>>(`/api/v1/contacts/${contactId}/notes`, { content }).then((r) => r.data),

  getNotes: (contactId: string) =>
    api.get<ApiResponse<ContactNote[]>>(`/api/v1/contacts/${contactId}/notes`).then((r) => r.data),

  deleteNote: (noteId: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/contacts/notes/${noteId}`).then((r) => r.data),

  // ── Attachments ──
  addAttachment: (contactId: string, data: { fileName: string; fileUrl: string; fileSize?: number; fileType?: string }) =>
    api.post<ApiResponse<ContactAttachment>>(`/api/v1/contacts/${contactId}/attachments`, data).then((r) => r.data),

  getAttachments: (contactId: string) =>
    api.get<ApiResponse<ContactAttachment[]>>(`/api/v1/contacts/${contactId}/attachments`).then((r) => r.data),

  deleteAttachment: (attachmentId: string) =>
    api.delete<ApiResponse<void>>(`/api/v1/contacts/attachments/${attachmentId}`).then((r) => r.data),
};
