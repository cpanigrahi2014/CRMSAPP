import api from './api';
import type { ApiResponse, PagedData } from '../types';
import type {
  DealChatMessage, MentionRecord, DealApproval, RecordComment, ActivityStreamEvent,
} from '../types';

const COLLAB_BASE = '/api/v1/collaboration';
const ACTIVITY_BASE = '/api/v1/activities';

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
  chat: 'crm_collab_chat',
  mentions: 'crm_collab_mentions',
  approvals: 'crm_collab_approvals',
  comments: 'crm_collab_comments',
  stream: 'crm_collab_stream',
};

/* ── Default data ────────────────────────────────────────────── */
const now = new Date().toISOString();
const h = (hours: number) => new Date(Date.now() - hours * 3600000).toISOString();
const m = (mins: number) => new Date(Date.now() - mins * 60000).toISOString();

const DEFAULT_CHAT: DealChatMessage[] = [
  { id: '1', opportunityId: 'opp-1', senderId: 'user-1', senderName: 'Alice Johnson', message: 'Hey team, just got off a call with Acme Corp. They want to increase the deal size to $500K.', messageType: 'TEXT', isEdited: false, createdAt: h(2), updatedAt: h(2) },
  { id: '2', opportunityId: 'opp-1', senderId: 'user-2', senderName: 'Bob Smith', message: 'That\'s great news! Do we need approval for that discount tier?', messageType: 'TEXT', isEdited: false, createdAt: h(1.8), updatedAt: h(1.8) },
  { id: '3', opportunityId: 'opp-1', senderId: 'user-3', senderName: 'Carol Davis', message: '@[Alice Johnson](user-1) Yes, anything above $400K with >15% discount needs VP approval.', messageType: 'TEXT', isEdited: false, createdAt: h(1.5), updatedAt: h(1.5) },
  { id: '4', opportunityId: 'opp-1', senderId: 'user-1', senderName: 'Alice Johnson', message: 'I\'ll submit the approval request now. @[David Wilson](user-4) can you review the pricing structure?', messageType: 'TEXT', isEdited: false, createdAt: h(1), updatedAt: h(1) },
  { id: '5', opportunityId: 'opp-1', senderId: 'user-4', senderName: 'David Wilson', message: 'On it! I\'ll prepare the revised proposal by end of day.', messageType: 'TEXT', isEdited: false, createdAt: m(45), updatedAt: m(45) },
  { id: '6', opportunityId: 'opp-1', senderId: 'user-2', senderName: 'Bob Smith', message: 'Quick update — their CTO confirmed they want the enterprise license bundled.', messageType: 'TEXT', isEdited: true, createdAt: m(20), updatedAt: m(15) },
];

const DEFAULT_MENTIONS: MentionRecord[] = [
  { id: '1', recordType: 'OPPORTUNITY', recordId: 'opp-1', sourceType: 'CHAT', sourceId: 'msg-3', mentionedUserId: 'user-1', mentionedUserName: 'Alice Johnson', mentionedById: 'user-3', mentionedByName: 'Carol Davis', isRead: true, createdAt: h(1.5) },
  { id: '2', recordType: 'OPPORTUNITY', recordId: 'opp-1', sourceType: 'CHAT', sourceId: 'msg-4', mentionedUserId: 'user-4', mentionedUserName: 'David Wilson', mentionedById: 'user-1', mentionedByName: 'Alice Johnson', isRead: false, createdAt: h(1) },
  { id: '3', recordType: 'ACCOUNT', recordId: 'acc-1', sourceType: 'COMMENT', sourceId: 'cmt-1', mentionedUserId: 'user-2', mentionedUserName: 'Bob Smith', mentionedById: 'user-3', mentionedByName: 'Carol Davis', isRead: false, createdAt: m(30) },
  { id: '4', recordType: 'LEAD', recordId: 'lead-1', sourceType: 'COMMENT', sourceId: 'cmt-2', mentionedUserId: 'user-1', mentionedUserName: 'Alice Johnson', mentionedById: 'user-2', mentionedByName: 'Bob Smith', isRead: false, createdAt: m(10) },
];

const DEFAULT_APPROVALS: DealApproval[] = [
  { id: '1', opportunityId: 'opp-1', requestedById: 'user-1', requestedByName: 'Alice Johnson', approverId: 'user-5', approverName: 'VP Sarah Chen', approvalType: 'DISCOUNT', status: 'PENDING', title: 'Acme Corp: 20% Enterprise Discount', description: 'Request to increase discount from 10% to 20% for enterprise license bundle. Deal value $500K.', currentValue: '10%', requestedValue: '20%', priority: 'HIGH', dueDate: new Date(Date.now() + 86400000).toISOString(), createdAt: h(1), updatedAt: h(1) },
  { id: '2', opportunityId: 'opp-2', requestedById: 'user-2', requestedByName: 'Bob Smith', approverId: 'user-5', approverName: 'VP Sarah Chen', approvalType: 'CLOSE_DEAL', status: 'APPROVED', title: 'TechStart: Close at $180K', description: 'Closing deal at $180K with standard terms. Customer agreed to 2-year commitment.', currentValue: '$200K', requestedValue: '$180K', approverComment: 'Approved — good margin and long-term commitment.', priority: 'NORMAL', decidedAt: h(3), createdAt: h(8), updatedAt: h(3) },
  { id: '3', opportunityId: 'opp-3', requestedById: 'user-3', requestedByName: 'Carol Davis', approverId: 'user-5', approverName: 'VP Sarah Chen', approvalType: 'PRICING', status: 'REJECTED', title: 'Global Inc: Custom Pricing Tier', description: 'Request for custom pricing tier — $75/seat instead of $95/seat for 200+ seats.', currentValue: '$95/seat', requestedValue: '$75/seat', approverComment: 'Margin too thin. Counter-offer at $85/seat.', priority: 'NORMAL', decidedAt: h(5), createdAt: h(12), updatedAt: h(5) },
  { id: '4', opportunityId: 'opp-4', requestedById: 'user-4', requestedByName: 'David Wilson', approverId: 'user-6', approverName: 'Director Mike Lee', approvalType: 'STAGE_CHANGE', status: 'PENDING', title: 'MegaCorp: Move to Negotiation', description: 'Requesting to advance deal to Negotiation stage. Budget confirmed, procurement involved.', currentValue: 'PROPOSAL', requestedValue: 'NEGOTIATION', priority: 'URGENT', dueDate: new Date(Date.now() + 43200000).toISOString(), createdAt: m(30), updatedAt: m(30) },
];

const DEFAULT_COMMENTS: RecordComment[] = [
  { id: '1', recordType: 'OPPORTUNITY', recordId: 'opp-1', authorId: 'user-1', authorName: 'Alice Johnson', content: 'Customer budget confirmed at $500K. Key decision maker is their VP of Engineering.', isInternal: true, isEdited: false, isPinned: true, createdAt: h(24), updatedAt: h(24), replies: [
    { id: '1a', recordType: 'OPPORTUNITY', recordId: 'opp-1', authorId: 'user-2', authorName: 'Bob Smith', content: 'I have a contact at their company — can intro you to the VP.', isInternal: true, isEdited: false, isPinned: false, createdAt: h(23), updatedAt: h(23) },
    { id: '1b', recordType: 'OPPORTUNITY', recordId: 'opp-1', authorId: 'user-1', authorName: 'Alice Johnson', content: 'That would be amazing, thanks @[Bob Smith](user-2)!', isInternal: true, isEdited: false, isPinned: false, createdAt: h(22), updatedAt: h(22) },
  ]},
  { id: '2', recordType: 'OPPORTUNITY', recordId: 'opp-1', authorId: 'user-3', authorName: 'Carol Davis', content: 'Competitive analysis: They\'re also evaluating Salesforce and HubSpot. Our edge is AI features and pricing.', isInternal: true, isEdited: false, isPinned: true, createdAt: h(20), updatedAt: h(20) },
  { id: '3', recordType: 'ACCOUNT', recordId: 'acc-1', authorId: 'user-2', authorName: 'Bob Smith', content: 'Annual review meeting went well. They want to expand to 3 more departments. @[Carol Davis](user-3) please prepare the expansion proposal.', isInternal: true, isEdited: false, isPinned: false, createdAt: h(6), updatedAt: h(6) },
  { id: '4', recordType: 'LEAD', recordId: 'lead-1', authorId: 'user-4', authorName: 'David Wilson', content: 'Hot lead from the webinar. They downloaded the enterprise whitepaper twice this week.', isInternal: true, isEdited: false, isPinned: false, createdAt: h(3), updatedAt: h(3) },
];

const DEFAULT_STREAM: ActivityStreamEvent[] = [
  { id: '1', eventType: 'DEAL_UPDATED', entityType: 'Opportunity', entityId: 'opp-1', entityName: 'Acme Corp Enterprise Deal', description: 'Deal amount updated from $300K to $500K', performedBy: 'user-1', performedByName: 'Alice Johnson', createdAt: m(5) },
  { id: '2', eventType: 'COMMENT_ADDED', entityType: 'Opportunity', entityId: 'opp-1', entityName: 'Acme Corp Enterprise Deal', description: 'New comment added by Bob Smith', performedBy: 'user-2', performedByName: 'Bob Smith', createdAt: m(10) },
  { id: '3', eventType: 'APPROVAL_REQUESTED', entityType: 'DealApproval', entityId: 'appr-1', entityName: 'Acme Corp: 20% Enterprise Discount', description: 'Discount approval requested for VP Sarah Chen', performedBy: 'user-1', performedByName: 'Alice Johnson', createdAt: m(15) },
  { id: '4', eventType: 'CHAT_MESSAGE_SENT', entityType: 'DealChat', entityId: 'opp-1', entityName: 'Acme Corp Enterprise Deal', description: 'David Wilson sent a message in deal chat', performedBy: 'user-4', performedByName: 'David Wilson', createdAt: m(20) },
  { id: '5', eventType: 'STAGE_CHANGED', entityType: 'Opportunity', entityId: 'opp-2', entityName: 'TechStart Pro License', description: 'Stage changed from Proposal to Negotiation', performedBy: 'user-2', performedByName: 'Bob Smith', createdAt: m(30) },
  { id: '6', eventType: 'APPROVAL_APPROVED', entityType: 'DealApproval', entityId: 'appr-2', entityName: 'TechStart: Close at $180K', description: 'Approval granted by VP Sarah Chen', performedBy: 'user-5', performedByName: 'VP Sarah Chen', createdAt: m(45) },
  { id: '7', eventType: 'LEAD_CREATED', entityType: 'Lead', entityId: 'lead-5', entityName: 'Enterprise Webinar Lead', description: 'New lead captured from Enterprise Webinar', performedBy: 'user-4', performedByName: 'David Wilson', createdAt: h(1) },
  { id: '8', eventType: 'COLLABORATOR_ADDED', entityType: 'Opportunity', entityId: 'opp-3', entityName: 'Global Inc Platform Deal', description: 'Carol Davis added as collaborator', performedBy: 'user-3', performedByName: 'Carol Davis', createdAt: h(1.5) },
  { id: '9', eventType: 'NOTE_ADDED', entityType: 'Account', entityId: 'acc-2', entityName: 'MegaCorp Industries', description: 'New shared note added about Q4 renewal', performedBy: 'user-1', performedByName: 'Alice Johnson', createdAt: h(2) },
  { id: '10', eventType: 'DEAL_WON', entityType: 'Opportunity', entityId: 'opp-5', entityName: 'CloudTech Migration', description: 'Deal closed won at $120K!', performedBy: 'user-2', performedByName: 'Bob Smith', createdAt: h(3) },
];

/* ═══════════════════════════════════════════════════════════════
   Service
   ═══════════════════════════════════════════════════════════════ */
export const collaborationService = {

  /* ── Deal Chat ─────────────────────────────────────────────── */
  getChatMessages: async (opportunityId: string, page = 0, size = 50) => {
    const local = loadJson<DealChatMessage[]>(K.chat, DEFAULT_CHAT);
    const filtered = local.filter(m => m.opportunityId === opportunityId);
    return tryApi(
      () => api.get<ApiResponse<PagedData<DealChatMessage>>>(`${COLLAB_BASE}/chat/${opportunityId}`, { params: { page, size } }).then(r => r.data.data),
      { content: filtered, pageNumber: 0, pageSize: size, totalElements: filtered.length, totalPages: 1, last: true, first: true } as PagedData<DealChatMessage>,
    );
  },

  sendChatMessage: async (opportunityId: string, message: string, parentMessageId?: string) => {
    const local = loadJson<DealChatMessage[]>(K.chat, DEFAULT_CHAT);
    const newMsg: DealChatMessage = {
      id: crypto.randomUUID(), opportunityId, senderId: 'user-1', senderName: 'Current User',
      message, messageType: 'TEXT', parentMessageId, isEdited: false,
      createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
    };
    local.push(newMsg);
    saveJson(K.chat, local);
    return tryApi(
      () => api.post<ApiResponse<DealChatMessage>>(`${COLLAB_BASE}/chat`, { opportunityId, message, messageType: 'TEXT', parentMessageId }).then(r => r.data.data),
      newMsg,
    );
  },

  deleteChatMessage: async (messageId: string) => {
    const local = loadJson<DealChatMessage[]>(K.chat, DEFAULT_CHAT);
    saveJson(K.chat, local.filter(m => m.id !== messageId));
    return tryApi(
      () => api.delete<ApiResponse<void>>(`${COLLAB_BASE}/chat/${messageId}`).then(r => r.data),
      undefined as any,
    );
  },

  /* ── Mentions ──────────────────────────────────────────────── */
  getMyMentions: async (page = 0, size = 20) => {
    const local = loadJson<MentionRecord[]>(K.mentions, DEFAULT_MENTIONS);
    return tryApi(
      () => api.get<ApiResponse<PagedData<MentionRecord>>>(`${COLLAB_BASE}/mentions`, { params: { page, size } }).then(r => r.data.data),
      { content: local, pageNumber: 0, pageSize: size, totalElements: local.length, totalPages: 1, last: true, first: true } as PagedData<MentionRecord>,
    );
  },

  getUnreadMentions: async (page = 0, size = 20) => {
    const local = loadJson<MentionRecord[]>(K.mentions, DEFAULT_MENTIONS).filter(m => !m.isRead);
    return tryApi(
      () => api.get<ApiResponse<PagedData<MentionRecord>>>(`${COLLAB_BASE}/mentions/unread`, { params: { page, size } }).then(r => r.data.data),
      { content: local, pageNumber: 0, pageSize: size, totalElements: local.length, totalPages: 1, last: true, first: true } as PagedData<MentionRecord>,
    );
  },

  getUnreadMentionCount: async () => {
    const local = loadJson<MentionRecord[]>(K.mentions, DEFAULT_MENTIONS).filter(m => !m.isRead).length;
    return tryApi(
      () => api.get<ApiResponse<number>>(`${COLLAB_BASE}/mentions/unread-count`).then(r => r.data.data),
      local,
    );
  },

  markMentionAsRead: async (mentionId: string) => {
    const local = loadJson<MentionRecord[]>(K.mentions, DEFAULT_MENTIONS);
    const idx = local.findIndex(m => m.id === mentionId);
    if (idx >= 0) { local[idx].isRead = true; saveJson(K.mentions, local); }
    return tryApi(
      () => api.patch<ApiResponse<void>>(`${COLLAB_BASE}/mentions/${mentionId}/read`).then(r => r.data),
      undefined as any,
    );
  },

  markAllMentionsAsRead: async () => {
    const local = loadJson<MentionRecord[]>(K.mentions, DEFAULT_MENTIONS);
    local.forEach(m => m.isRead = true);
    saveJson(K.mentions, local);
    return tryApi(
      () => api.patch<ApiResponse<void>>(`${COLLAB_BASE}/mentions/read-all`).then(r => r.data),
      undefined as any,
    );
  },

  /* ── Deal Approvals ────────────────────────────────────────── */
  getApprovalsByDeal: async (opportunityId: string, page = 0, size = 20) => {
    const local = loadJson<DealApproval[]>(K.approvals, DEFAULT_APPROVALS).filter(a => a.opportunityId === opportunityId);
    return tryApi(
      () => api.get<ApiResponse<PagedData<DealApproval>>>(`${COLLAB_BASE}/approvals/opportunity/${opportunityId}`, { params: { page, size } }).then(r => r.data.data),
      { content: local, pageNumber: 0, pageSize: size, totalElements: local.length, totalPages: 1, last: true, first: true } as PagedData<DealApproval>,
    );
  },

  getPendingApprovals: async (page = 0, size = 20) => {
    const local = loadJson<DealApproval[]>(K.approvals, DEFAULT_APPROVALS).filter(a => a.status === 'PENDING');
    return tryApi(
      () => api.get<ApiResponse<PagedData<DealApproval>>>(`${COLLAB_BASE}/approvals/pending`, { params: { page, size } }).then(r => r.data.data),
      { content: local, pageNumber: 0, pageSize: size, totalElements: local.length, totalPages: 1, last: true, first: true } as PagedData<DealApproval>,
    );
  },

  getAllApprovals: async (page = 0, size = 20) => {
    const local = loadJson<DealApproval[]>(K.approvals, DEFAULT_APPROVALS);
    return tryApi(
      () => api.get<ApiResponse<PagedData<DealApproval>>>(`${COLLAB_BASE}/approvals/my-approvals`, { params: { page, size } }).then(r => r.data.data),
      { content: local, pageNumber: 0, pageSize: size, totalElements: local.length, totalPages: 1, last: true, first: true } as PagedData<DealApproval>,
    );
  },

  createApproval: async (data: { opportunityId: string; approverId: string; approverName?: string; approvalType: string; title: string; description?: string; currentValue?: string; requestedValue?: string; priority?: string; dueDate?: string }) => {
    const local = loadJson<DealApproval[]>(K.approvals, DEFAULT_APPROVALS);
    const newApproval: DealApproval = {
      id: crypto.randomUUID(), ...data, requestedById: 'user-1', requestedByName: 'Current User',
      approverName: data.approverName || 'Approver', approvalType: data.approvalType as any,
      status: 'PENDING', priority: (data.priority || 'NORMAL') as any, title: data.title,
      createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
    };
    local.push(newApproval);
    saveJson(K.approvals, local);
    return tryApi(
      () => api.post<ApiResponse<DealApproval>>(`${COLLAB_BASE}/approvals`, data).then(r => r.data.data),
      newApproval,
    );
  },

  decideApproval: async (approvalId: string, decision: string, comment?: string) => {
    const local = loadJson<DealApproval[]>(K.approvals, DEFAULT_APPROVALS);
    const idx = local.findIndex(a => a.id === approvalId);
    if (idx >= 0) {
      local[idx].status = decision as any;
      local[idx].approverComment = comment;
      local[idx].decidedAt = new Date().toISOString();
      saveJson(K.approvals, local);
    }
    return tryApi(
      () => api.post<ApiResponse<DealApproval>>(`${COLLAB_BASE}/approvals/${approvalId}/decide`, { decision, comment }).then(r => r.data.data),
      local[idx],
    );
  },

  /* ── Record Comments ───────────────────────────────────────── */
  getComments: async (recordType: string, recordId: string, page = 0, size = 50) => {
    const local = loadJson<RecordComment[]>(K.comments, DEFAULT_COMMENTS)
      .filter(c => c.recordType === recordType && c.recordId === recordId);
    return tryApi(
      () => api.get<ApiResponse<PagedData<RecordComment>>>(`${COLLAB_BASE}/comments/${recordType}/${recordId}`, { params: { page, size } }).then(r => r.data.data),
      { content: local, pageNumber: 0, pageSize: size, totalElements: local.length, totalPages: 1, last: true, first: true } as PagedData<RecordComment>,
    );
  },

  addComment: async (recordType: string, recordId: string, content: string, parentCommentId?: string) => {
    const local = loadJson<RecordComment[]>(K.comments, DEFAULT_COMMENTS);
    const newComment: RecordComment = {
      id: crypto.randomUUID(), recordType, recordId, authorId: 'user-1', authorName: 'Current User',
      content, parentCommentId, isInternal: true, isEdited: false, isPinned: false,
      createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
    };
    local.push(newComment);
    saveJson(K.comments, local);
    return tryApi(
      () => api.post<ApiResponse<RecordComment>>(`${COLLAB_BASE}/comments`, { recordType, recordId, content, parentCommentId, isInternal: true }).then(r => r.data.data),
      newComment,
    );
  },

  updateComment: async (commentId: string, data: { content?: string; isPinned?: boolean }) => {
    const local = loadJson<RecordComment[]>(K.comments, DEFAULT_COMMENTS);
    const idx = local.findIndex(c => c.id === commentId);
    if (idx >= 0) {
      if (data.content) { local[idx].content = data.content; local[idx].isEdited = true; }
      if (data.isPinned !== undefined) local[idx].isPinned = data.isPinned;
      saveJson(K.comments, local);
    }
    return tryApi(
      () => api.put<ApiResponse<RecordComment>>(`${COLLAB_BASE}/comments/${commentId}`, data).then(r => r.data.data),
      local[idx],
    );
  },

  deleteComment: async (commentId: string) => {
    const local = loadJson<RecordComment[]>(K.comments, DEFAULT_COMMENTS);
    saveJson(K.comments, local.filter(c => c.id !== commentId));
    return tryApi(
      () => api.delete<ApiResponse<void>>(`${COLLAB_BASE}/comments/${commentId}`).then(r => r.data),
      undefined as any,
    );
  },

  /* ── Activity Stream ───────────────────────────────────────── */
  getActivityStream: async (page = 0, size = 50) => {
    const local = loadJson<ActivityStreamEvent[]>(K.stream, DEFAULT_STREAM);
    return tryApi(
      () => api.get<ApiResponse<PagedData<ActivityStreamEvent>>>(`${ACTIVITY_BASE}/stream`, { params: { page, size } }).then(r => r.data.data),
      { content: local, pageNumber: 0, pageSize: size, totalElements: local.length, totalPages: 1, last: true, first: true } as PagedData<ActivityStreamEvent>,
    );
  },

  getEntityStream: async (entityType: string, entityId: string, page = 0, size = 50) => {
    const local = loadJson<ActivityStreamEvent[]>(K.stream, DEFAULT_STREAM)
      .filter(e => e.entityType === entityType && e.entityId === entityId);
    return tryApi(
      () => api.get<ApiResponse<PagedData<ActivityStreamEvent>>>(`${ACTIVITY_BASE}/stream/entity/${entityType}/${entityId}`, { params: { page, size } }).then(r => r.data.data),
      { content: local, pageNumber: 0, pageSize: size, totalElements: local.length, totalPages: 1, last: true, first: true } as PagedData<ActivityStreamEvent>,
    );
  },
};
