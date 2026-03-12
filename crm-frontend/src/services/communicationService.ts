import api from './api';
import type { ApiResponse, PagedData } from '../types';
import type {
  SmsMessage, WhatsAppMessage, CallRecord, UnifiedInboxMessage,
  TranscriptionResult, SentimentAnalysisResult,
} from '../types';

const COMM_BASE = '/api/v1/communications';
const AI_BASE = '/api/v1/ai';

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
  sms: 'crm_comm_sms',
  whatsapp: 'crm_comm_whatsapp',
  calls: 'crm_comm_calls',
  inbox: 'crm_comm_inbox',
};

/* ── Default data ────────────────────────────────────────────── */
const now = new Date().toISOString();
const h = (hours: number) => new Date(Date.now() - hours * 3600000).toISOString();

const DEFAULT_SMS: SmsMessage[] = [
  { id: '1', fromNumber: '+15551234567', toNumber: '+15559876543', body: 'Hi John, following up on our discussion about the enterprise plan. Would you like to schedule a demo?', direction: 'OUTBOUND', status: 'DELIVERED', tenantId: 'default', sentAt: h(2), deliveredAt: h(2), createdAt: h(2), updatedAt: h(2) },
  { id: '2', fromNumber: '+15559876543', toNumber: '+15551234567', body: 'Yes, that would be great! How about Thursday at 2pm?', direction: 'INBOUND', status: 'RECEIVED', tenantId: 'default', createdAt: h(1.5), updatedAt: h(1.5) },
  { id: '3', fromNumber: '+15551234567', toNumber: '+15558765432', body: 'Your account renewal is coming up on March 15. Please contact us for special pricing.', direction: 'OUTBOUND', status: 'SENT', tenantId: 'default', sentAt: h(5), createdAt: h(5), updatedAt: h(5) },
  { id: '4', fromNumber: '+15557654321', toNumber: '+15551234567', body: 'I received the proposal. Let me review it with my team and get back to you by Friday.', direction: 'INBOUND', status: 'RECEIVED', tenantId: 'default', createdAt: h(0.5), updatedAt: h(0.5) },
];

const DEFAULT_WHATSAPP: WhatsAppMessage[] = [
  { id: '1', fromNumber: '+15551234567', toNumber: '+15559876543', body: 'Hi Sarah! Sharing the product brochure for the Pro tier.', messageType: 'DOCUMENT', mediaUrl: '/files/pro-brochure.pdf', mediaType: 'application/pdf', direction: 'OUTBOUND', status: 'READ', tenantId: 'default', readAt: h(3), createdAt: h(4), updatedAt: h(3) },
  { id: '2', fromNumber: '+15559876543', toNumber: '+15551234567', body: 'Thanks! This looks great. Can you also send pricing for 50+ seats?', messageType: 'TEXT', direction: 'INBOUND', status: 'DELIVERED', tenantId: 'default', createdAt: h(3), updatedAt: h(3) },
  { id: '3', fromNumber: '+15551234567', toNumber: '+15558765432', body: 'Meeting reminder: Product demo tomorrow at 10am EST', messageType: 'TEXT', direction: 'OUTBOUND', status: 'DELIVERED', tenantId: 'default', createdAt: h(8), updatedAt: h(8) },
  { id: '4', fromNumber: '+15558765432', toNumber: '+15551234567', body: '', messageType: 'IMAGE', mediaUrl: '/files/screenshot.png', mediaType: 'image/png', direction: 'INBOUND', status: 'DELIVERED', tenantId: 'default', createdAt: h(6), updatedAt: h(6) },
];

const DEFAULT_CALLS: CallRecord[] = [
  { id: '1', fromNumber: '+15551234567', toNumber: '+15559876543', direction: 'OUTBOUND', status: 'COMPLETED', durationSeconds: 845, callOutcome: 'Positive - client interested in enterprise plan', notes: 'Discussed pricing for 100 seats. Client will review internally.', startedAt: h(3), answeredAt: h(3), endedAt: h(2.8), tenantId: 'default', createdAt: h(3), updatedAt: h(2.8) },
  { id: '2', fromNumber: '+15558765432', toNumber: '+15551234567', direction: 'INBOUND', status: 'COMPLETED', durationSeconds: 320, recordingUrl: '/recordings/call-002.mp3', recordingDurationSeconds: 320, callOutcome: 'Support inquiry resolved', notes: 'Client had billing question about invoice #1234', startedAt: h(6), answeredAt: h(6), endedAt: h(5.9), tenantId: 'default', createdAt: h(6), updatedAt: h(5.9) },
  { id: '3', fromNumber: '+15551234567', toNumber: '+15557654321', direction: 'OUTBOUND', status: 'NO_ANSWER', startedAt: h(1), endedAt: h(1), tenantId: 'default', createdAt: h(1), updatedAt: h(1) },
  { id: '4', fromNumber: '+15551234567', toNumber: '+15556543210', direction: 'OUTBOUND', status: 'VOICEMAIL', durationSeconds: 45, voicemailUrl: '/recordings/vm-004.mp3', callOutcome: 'Left voicemail', startedAt: h(4), endedAt: h(3.99), tenantId: 'default', createdAt: h(4), updatedAt: h(3.99) },
];

const DEFAULT_INBOX: UnifiedInboxMessage[] = [
  { id: '1', channel: 'SMS', direction: 'INBOUND', sender: '+15559876543', recipient: '+15551234567', body: 'Yes, that would be great! How about Thursday at 2pm?', status: 'RECEIVED', sourceId: '2', tenantId: 'default', createdAt: h(1.5) },
  { id: '2', channel: 'WHATSAPP', direction: 'INBOUND', sender: '+15559876543', recipient: '+15551234567', body: 'Thanks! This looks great. Can you also send pricing for 50+ seats?', status: 'DELIVERED', sourceId: '2', tenantId: 'default', createdAt: h(3) },
  { id: '3', channel: 'CALL', direction: 'INBOUND', sender: '+15558765432', recipient: '+15551234567', body: 'Call completed (320s)', status: 'COMPLETED', sourceId: '2', tenantId: 'default', createdAt: h(6) },
  { id: '4', channel: 'EMAIL', direction: 'INBOUND', sender: 'john@acme.com', recipient: 'sales@company.com', subject: 'Re: Enterprise Plan Inquiry', body: 'Hi, I wanted to follow up on our recent conversation...', status: 'RECEIVED', sourceId: 'em1', tenantId: 'default', createdAt: h(0.5) },
  { id: '5', channel: 'SMS', direction: 'OUTBOUND', sender: '+15551234567', recipient: '+15558765432', body: 'Your account renewal is coming up on March 15.', status: 'SENT', sourceId: '3', tenantId: 'default', createdAt: h(5) },
  { id: '6', channel: 'WHATSAPP', direction: 'OUTBOUND', sender: '+15551234567', recipient: '+15558765432', body: 'Meeting reminder: Product demo tomorrow at 10am EST', status: 'DELIVERED', sourceId: '3', tenantId: 'default', createdAt: h(8) },
  { id: '7', channel: 'CALL', direction: 'OUTBOUND', sender: '+15551234567', recipient: '+15559876543', body: 'Call completed (845s)', status: 'COMPLETED', sourceId: '1', tenantId: 'default', createdAt: h(3) },
  { id: '8', channel: 'SMS', direction: 'INBOUND', sender: '+15557654321', recipient: '+15551234567', body: 'I received the proposal. Let me review it with my team.', status: 'RECEIVED', sourceId: '4', tenantId: 'default', createdAt: h(0.5) },
];

/* ── SMS ─────────────────────────────────────────────────────── */
export function getSmsMessages(): SmsMessage[] { return loadJson(K.sms, DEFAULT_SMS); }
export async function fetchSmsMessages(): Promise<SmsMessage[]> {
  return tryApi(async () => {
    const r = await api.get<ApiResponse<PagedData<SmsMessage>>>(`${COMM_BASE}/sms`);
    const data = r.data?.data?.content;
    return Array.isArray(data) && data.length > 0 ? data : DEFAULT_SMS;
  }, getSmsMessages());
}
export async function sendSms(toNumber: string, body: string): Promise<SmsMessage> {
  return tryApi(async () => {
    const r = await api.post<ApiResponse<SmsMessage>>(`${COMM_BASE}/sms/send`, { toNumber, body });
    return r.data?.data;
  }, { id: Date.now().toString(), fromNumber: '+15551234567', toNumber, body, direction: 'OUTBOUND' as const, status: 'SENT' as const, tenantId: 'default', createdAt: now, updatedAt: now });
}
export function saveSmsMessages(data: SmsMessage[]): void { saveJson(K.sms, data); }

/* ── WhatsApp ────────────────────────────────────────────────── */
export function getWhatsAppMessages(): WhatsAppMessage[] { return loadJson(K.whatsapp, DEFAULT_WHATSAPP); }
export async function fetchWhatsAppMessages(): Promise<WhatsAppMessage[]> {
  return tryApi(async () => {
    const r = await api.get<ApiResponse<PagedData<WhatsAppMessage>>>(`${COMM_BASE}/whatsapp`);
    const data = r.data?.data?.content;
    return Array.isArray(data) && data.length > 0 ? data : DEFAULT_WHATSAPP;
  }, getWhatsAppMessages());
}
export async function sendWhatsApp(toNumber: string, body: string, messageType: string = 'TEXT'): Promise<WhatsAppMessage> {
  return tryApi(async () => {
    const r = await api.post<ApiResponse<WhatsAppMessage>>(`${COMM_BASE}/whatsapp/send`, { toNumber, body, messageType });
    return r.data?.data;
  }, { id: Date.now().toString(), fromNumber: '+15551234567', toNumber, body, messageType: messageType as WhatsAppMessage['messageType'], direction: 'OUTBOUND' as const, status: 'SENT' as const, tenantId: 'default', createdAt: now, updatedAt: now });
}
export function saveWhatsAppMessages(data: WhatsAppMessage[]): void { saveJson(K.whatsapp, data); }

/* ── Calls ───────────────────────────────────────────────────── */
export function getCallRecords(): CallRecord[] { return loadJson(K.calls, DEFAULT_CALLS); }
export async function fetchCallRecords(): Promise<CallRecord[]> {
  return tryApi(async () => {
    const r = await api.get<ApiResponse<PagedData<CallRecord>>>(`${COMM_BASE}/calls`);
    const data = r.data?.data?.content;
    return Array.isArray(data) && data.length > 0 ? data : DEFAULT_CALLS;
  }, getCallRecords());
}
export async function initiateCall(toNumber: string): Promise<CallRecord> {
  return tryApi(async () => {
    const r = await api.post<ApiResponse<CallRecord>>(`${COMM_BASE}/calls/initiate`, { toNumber });
    return r.data?.data;
  }, { id: Date.now().toString(), fromNumber: '+15551234567', toNumber, direction: 'OUTBOUND' as const, status: 'INITIATED' as const, startedAt: now, tenantId: 'default', createdAt: now, updatedAt: now });
}
export async function endCall(callId: string): Promise<CallRecord> {
  return tryApi(async () => {
    const r = await api.post<ApiResponse<CallRecord>>(`${COMM_BASE}/calls/${callId}/end`);
    return r.data?.data;
  }, { id: callId, fromNumber: '', toNumber: '', direction: 'OUTBOUND' as const, status: 'COMPLETED' as const, tenantId: 'default', createdAt: now, updatedAt: now });
}
export function saveCallRecords(data: CallRecord[]): void { saveJson(K.calls, data); }

/* ── Unified Inbox ───────────────────────────────────────────── */
export function getUnifiedInbox(): UnifiedInboxMessage[] { return loadJson(K.inbox, DEFAULT_INBOX); }
export async function fetchUnifiedInbox(channel?: string): Promise<UnifiedInboxMessage[]> {
  const url = channel ? `${COMM_BASE}/inbox/channel/${channel}` : `${COMM_BASE}/inbox`;
  return tryApi(async () => {
    const r = await api.get<ApiResponse<PagedData<UnifiedInboxMessage>>>(url);
    const data = r.data?.data?.content;
    return Array.isArray(data) && data.length > 0 ? data : DEFAULT_INBOX;
  }, getUnifiedInbox());
}
export function saveUnifiedInbox(data: UnifiedInboxMessage[]): void { saveJson(K.inbox, data); }

/* ── AI Transcription ────────────────────────────────────────── */
export async function transcribeContent(content: string, sourceType: string = 'CONVERSATION', speakers?: string[]): Promise<TranscriptionResult> {
  const fallback: TranscriptionResult = {
    id: Date.now().toString(), sourceType, fullTranscript: content,
    segments: [{ speaker: 'Speaker 1', text: content, timestamp: '' }],
    keyTopics: ['General discussion'], summary: 'Transcript of conversation content.',
    language: 'en', createdAt: now,
  };
  return tryApi(async () => {
    const r = await api.post<ApiResponse<TranscriptionResult>>(`${AI_BASE}/transcribe`, { content, sourceType, speakers });
    return r.data?.data ?? fallback;
  }, fallback);
}

/* ── AI Sentiment Analysis ───────────────────────────────────── */
export async function analyzeSentiment(content: string, sourceType: string = 'CONVERSATION', contactName?: string): Promise<SentimentAnalysisResult> {
  const fallback: SentimentAnalysisResult = {
    id: Date.now().toString(), sourceType, overallSentiment: 'NEUTRAL', sentimentScore: 0,
    confidence: 0.5, summary: 'Sentiment analysis unavailable - using defaults.',
    emotions: [{ emotion: 'neutral', score: 1.0 }], keyPhrases: [], concerns: [],
    positiveIndicators: [], recommendation: 'Follow up with the customer.', createdAt: now,
  };
  return tryApi(async () => {
    const r = await api.post<ApiResponse<SentimentAnalysisResult>>(`${AI_BASE}/sentiment-analysis`, { content, sourceType, contactName });
    return r.data?.data ?? fallback;
  }, fallback);
}
