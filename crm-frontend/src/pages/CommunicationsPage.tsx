/* ============================================================
   CommunicationsPage.tsx – Unified Communication CRM
   Covers: SMS, WhatsApp, Calling, AI Transcription,
   AI Sentiment Analysis, Multi-Channel Inbox
   ============================================================ */
import React, { useEffect, useState, useCallback } from 'react';
import {
  Box, Typography, Paper, Tabs, Tab, Card, CardContent,
  Button, Chip, Alert, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TextField, IconButton,
  Tooltip, Snackbar, CircularProgress, Stack, Dialog,
  DialogTitle, DialogContent, DialogActions, LinearProgress,
  List, ListItem, ListItemText, ListItemIcon, Divider,
} from '@mui/material';
import {
  Sms as SmsIcon,
  WhatsApp as WhatsAppIcon,
  Phone as PhoneIcon,
  Inbox as InboxIcon,
  Transcribe as TranscribeIcon,
  SentimentSatisfied as SentimentIcon,
  Send as SendIcon,
  CallEnd as CallEndIcon,
  CallMade as OutboundIcon,
  CallReceived as InboundIcon,
  Refresh as RefreshIcon,
  Mic as MicIcon,
  Image as ImageIcon,
  Description as DocIcon,
  ArrowUpward as ArrowUpIcon,
  ArrowDownward as ArrowDownIcon,
} from '@mui/icons-material';
import type {
  SmsMessage, WhatsAppMessage, CallRecord,
  UnifiedInboxMessage, TranscriptionResult, SentimentAnalysisResult,
} from '../types';
import {
  getSmsMessages, fetchSmsMessages, sendSms,
  getWhatsAppMessages, fetchWhatsAppMessages, sendWhatsApp,
  getCallRecords, fetchCallRecords, initiateCall, endCall,
  getUnifiedInbox, fetchUnifiedInbox,
  transcribeContent, analyzeSentiment,
} from '../services/communicationService';
import { PageHeader } from '../components';

/* ── Tab panel helper ─────────────────────────────────────── */
function TabPanel({ children, value, index }: { children: React.ReactNode; value: number; index: number }) {
  return value === index ? <Box sx={{ py: 3 }}>{children}</Box> : null;
}

/* ── Status chip ──────────────────────────────────────────── */
function StatusChip({ status }: { status: string }) {
  const color = (() => {
    const s = status.toUpperCase();
    if (['DELIVERED', 'COMPLETED', 'READ', 'SENT', 'RECEIVED'].includes(s)) return 'success';
    if (['PENDING', 'INITIATED', 'RINGING', 'IN_PROGRESS', 'SENDING'].includes(s)) return 'warning';
    if (['FAILED', 'NO_ANSWER', 'BUSY'].includes(s)) return 'error';
    return 'default';
  })() as 'success' | 'warning' | 'error' | 'default';
  return <Chip label={status} size="small" color={color} />;
}

function DirectionIcon({ direction }: { direction: string }) {
  return direction === 'INBOUND'
    ? <Tooltip title="Inbound"><InboundIcon color="primary" fontSize="small" /></Tooltip>
    : <Tooltip title="Outbound"><OutboundIcon color="action" fontSize="small" /></Tooltip>;
}

function ChannelChip({ channel }: { channel: string }) {
  const color = (() => {
    switch (channel) {
      case 'SMS': return 'primary';
      case 'WHATSAPP': return 'success';
      case 'CALL': return 'secondary';
      case 'EMAIL': return 'info';
      default: return 'default';
    }
  })() as 'primary' | 'success' | 'secondary' | 'info' | 'default';
  const icon = (() => {
    switch (channel) {
      case 'SMS': return <SmsIcon fontSize="small" />;
      case 'WHATSAPP': return <WhatsAppIcon fontSize="small" />;
      case 'CALL': return <PhoneIcon fontSize="small" />;
      default: return undefined;
    }
  })();
  return <Chip icon={icon} label={channel} size="small" color={color} variant="outlined" />;
}

function formatDuration(seconds?: number): string {
  if (!seconds) return '—';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

function fmtDate(d?: string): string {
  if (!d) return '—';
  return new Date(d).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

const CommunicationsPage: React.FC = () => {
  const [tab, setTab] = useState(0);
  const [snack, setSnack] = useState('');
  const [loading, setLoading] = useState(false);

  /* ── SMS state ──────────────────────────────── */
  const [smsList, setSmsList] = useState<SmsMessage[]>([]);
  const [smsTo, setSmsTo] = useState('');
  const [smsBody, setSmsBody] = useState('');

  /* ── WhatsApp state ────────────────────────── */
  const [waList, setWaList] = useState<WhatsAppMessage[]>([]);
  const [waTo, setWaTo] = useState('');
  const [waBody, setWaBody] = useState('');

  /* ── Calls state ───────────────────────────── */
  const [callList, setCallList] = useState<CallRecord[]>([]);
  const [callTo, setCallTo] = useState('');

  /* ── Unified Inbox state ───────────────────── */
  const [inbox, setInbox] = useState<UnifiedInboxMessage[]>([]);
  const [inboxFilter, setInboxFilter] = useState<string>('ALL');

  /* ── AI Transcription state ────────────────── */
  const [transcriptInput, setTranscriptInput] = useState('');
  const [transcriptResult, setTranscriptResult] = useState<TranscriptionResult | null>(null);

  /* ── AI Sentiment state ────────────────────── */
  const [sentimentInput, setSentimentInput] = useState('');
  const [sentimentResult, setSentimentResult] = useState<SentimentAnalysisResult | null>(null);

  /* ── Dialog for call notes ─────────────────── */
  const [notesDialog, setNotesDialog] = useState<CallRecord | null>(null);

  /* ── Load data ─────────────────────────────── */
  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [s, w, c, i] = await Promise.all([
        fetchSmsMessages(), fetchWhatsAppMessages(), fetchCallRecords(), fetchUnifiedInbox(),
      ]);
      setSmsList(Array.isArray(s) ? s : getSmsMessages());
      setWaList(Array.isArray(w) ? w : getWhatsAppMessages());
      setCallList(Array.isArray(c) ? c : getCallRecords());
      setInbox(Array.isArray(i) ? i : getUnifiedInbox());
    } catch {
      setSmsList(getSmsMessages());
      setWaList(getWhatsAppMessages());
      setCallList(getCallRecords());
      setInbox(getUnifiedInbox());
    }
    setLoading(false);
  }, []);

  useEffect(() => { loadAll(); }, [loadAll]);

  /* ── Handlers ──────────────────────────────── */
  const handleSendSms = async () => {
    if (!smsTo.trim() || !smsBody.trim()) return;
    setLoading(true);
    await sendSms(smsTo, smsBody);
    setSmsTo(''); setSmsBody('');
    setSnack('SMS sent!');
    await loadAll();
    setLoading(false);
  };

  const handleSendWhatsApp = async () => {
    if (!waTo.trim() || !waBody.trim()) return;
    setLoading(true);
    await sendWhatsApp(waTo, waBody);
    setWaTo(''); setWaBody('');
    setSnack('WhatsApp message sent!');
    await loadAll();
    setLoading(false);
  };

  const handleInitiateCall = async () => {
    if (!callTo.trim()) return;
    setLoading(true);
    await initiateCall(callTo);
    setCallTo('');
    setSnack('Call initiated!');
    await loadAll();
    setLoading(false);
  };

  const handleEndCall = async (callId: string) => {
    setLoading(true);
    await endCall(callId);
    setSnack('Call ended');
    await loadAll();
    setLoading(false);
  };

  const handleTranscribe = async () => {
    if (!transcriptInput.trim()) return;
    setLoading(true);
    const result = await transcribeContent(transcriptInput, 'CONVERSATION');
    setTranscriptResult(result);
    setSnack('Transcription complete!');
    setLoading(false);
  };

  const handleSentiment = async () => {
    if (!sentimentInput.trim()) return;
    setLoading(true);
    const result = await analyzeSentiment(sentimentInput, 'CONVERSATION');
    setSentimentResult(result);
    setSnack('Sentiment analysis complete!');
    setLoading(false);
  };

  const filteredInbox = inboxFilter === 'ALL' ? inbox : inbox.filter(m => m.channel === inboxFilter);

  return (
    <Box>
      <PageHeader
        title="Communications"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Communications' }]}
        action={<Button startIcon={<RefreshIcon />} onClick={loadAll} disabled={loading}>Refresh</Button>}
      />

      {loading && <LinearProgress sx={{ mb: 2 }} />}

      <Paper sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto">
          <Tab icon={<InboxIcon />} label="Unified Inbox" iconPosition="start" />
          <Tab icon={<SmsIcon />} label="SMS" iconPosition="start" />
          <Tab icon={<WhatsAppIcon />} label="WhatsApp" iconPosition="start" />
          <Tab icon={<PhoneIcon />} label="Calls" iconPosition="start" />
          <Tab icon={<TranscribeIcon />} label="Transcription" iconPosition="start" />
          <Tab icon={<SentimentIcon />} label="Sentiment" iconPosition="start" />
        </Tabs>
      </Paper>

      {/* ── Tab 0: Unified Inbox ─────────────────── */}
      <TabPanel value={tab} index={0}>
        <Stack direction="row" spacing={1} mb={2}>
          {['ALL', 'EMAIL', 'SMS', 'WHATSAPP', 'CALL'].map(f => (
            <Chip key={f} label={f} variant={inboxFilter === f ? 'filled' : 'outlined'}
              color={inboxFilter === f ? 'primary' : 'default'}
              onClick={() => setInboxFilter(f)} />
          ))}
        </Stack>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Channel</TableCell>
                <TableCell>Direction</TableCell>
                <TableCell>From</TableCell>
                <TableCell>To</TableCell>
                <TableCell>Message</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredInbox.length === 0 ? (
                <TableRow><TableCell colSpan={7} align="center"><Typography color="text.secondary">No messages</Typography></TableCell></TableRow>
              ) : filteredInbox.map(m => (
                <TableRow key={m.id} hover>
                  <TableCell><ChannelChip channel={m.channel} /></TableCell>
                  <TableCell><DirectionIcon direction={m.direction} /></TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{m.sender}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{m.recipient}</TableCell>
                  <TableCell sx={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {m.subject ? <strong>{m.subject}: </strong> : null}{m.body}
                  </TableCell>
                  <TableCell><StatusChip status={m.status} /></TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{fmtDate(m.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {/* ── Tab 1: SMS ───────────────────────────── */}
      <TabPanel value={tab} index={1}>
        <Card variant="outlined" sx={{ mb: 3, p: 2 }}>
          <Typography variant="h6" gutterBottom>Send SMS</Typography>
          <Stack direction="row" spacing={2} alignItems="flex-end">
            <TextField label="To Number" value={smsTo} onChange={e => setSmsTo(e.target.value)}
              placeholder="+15559876543" size="small" sx={{ width: 200 }} />
            <TextField label="Message" value={smsBody} onChange={e => setSmsBody(e.target.value)}
              placeholder="Type your message..." size="small" sx={{ flex: 1 }} multiline maxRows={3} />
            <Button variant="contained" startIcon={<SendIcon />} onClick={handleSendSms}
              disabled={loading || !smsTo.trim() || !smsBody.trim()}>Send</Button>
          </Stack>
        </Card>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Direction</TableCell>
                <TableCell>From</TableCell>
                <TableCell>To</TableCell>
                <TableCell>Message</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {smsList.map(s => (
                <TableRow key={s.id} hover>
                  <TableCell><DirectionIcon direction={s.direction} /></TableCell>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{s.fromNumber}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{s.toNumber}</TableCell>
                  <TableCell sx={{ maxWidth: 350, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{s.body}</TableCell>
                  <TableCell><StatusChip status={s.status} /></TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{fmtDate(s.sentAt || s.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {/* ── Tab 2: WhatsApp ──────────────────────── */}
      <TabPanel value={tab} index={2}>
        <Card variant="outlined" sx={{ mb: 3, p: 2 }}>
          <Typography variant="h6" gutterBottom>Send WhatsApp Message</Typography>
          <Stack direction="row" spacing={2} alignItems="flex-end">
            <TextField label="To Number" value={waTo} onChange={e => setWaTo(e.target.value)}
              placeholder="+15559876543" size="small" sx={{ width: 200 }} />
            <TextField label="Message" value={waBody} onChange={e => setWaBody(e.target.value)}
              placeholder="Type your message..." size="small" sx={{ flex: 1 }} multiline maxRows={3} />
            <Button variant="contained" color="success" startIcon={<WhatsAppIcon />} onClick={handleSendWhatsApp}
              disabled={loading || !waTo.trim() || !waBody.trim()}>Send</Button>
          </Stack>
        </Card>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Direction</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>From</TableCell>
                <TableCell>To</TableCell>
                <TableCell>Message / Media</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {waList.map(w => (
                <TableRow key={w.id} hover>
                  <TableCell><DirectionIcon direction={w.direction} /></TableCell>
                  <TableCell>
                    <Chip size="small" variant="outlined" label={w.messageType}
                      icon={w.messageType === 'IMAGE' ? <ImageIcon fontSize="small" /> : w.messageType === 'DOCUMENT' ? <DocIcon fontSize="small" /> : undefined} />
                  </TableCell>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{w.fromNumber}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{w.toNumber}</TableCell>
                  <TableCell sx={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {w.body || (w.mediaUrl ? `[${w.mediaType || 'media'}]` : '—')}
                  </TableCell>
                  <TableCell><StatusChip status={w.status} /></TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{fmtDate(w.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {/* ── Tab 3: Calls ─────────────────────────── */}
      <TabPanel value={tab} index={3}>
        <Card variant="outlined" sx={{ mb: 3, p: 2 }}>
          <Typography variant="h6" gutterBottom>Make a Call</Typography>
          <Stack direction="row" spacing={2} alignItems="flex-end">
            <TextField label="Phone Number" value={callTo} onChange={e => setCallTo(e.target.value)}
              placeholder="+15559876543" size="small" sx={{ width: 250 }} />
            <Button variant="contained" color="secondary" startIcon={<PhoneIcon />} onClick={handleInitiateCall}
              disabled={loading || !callTo.trim()}>Dial</Button>
          </Stack>
        </Card>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Direction</TableCell>
                <TableCell>From</TableCell>
                <TableCell>To</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Duration</TableCell>
                <TableCell>Outcome</TableCell>
                <TableCell>Recording</TableCell>
                <TableCell>Started</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {callList.map(c => (
                <TableRow key={c.id} hover>
                  <TableCell><DirectionIcon direction={c.direction} /></TableCell>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{c.fromNumber}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace' }}>{c.toNumber}</TableCell>
                  <TableCell><StatusChip status={c.status} /></TableCell>
                  <TableCell>{formatDuration(c.durationSeconds)}</TableCell>
                  <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {c.callOutcome || '—'}
                  </TableCell>
                  <TableCell>
                    {c.recordingUrl ? <Chip size="small" icon={<MicIcon />} label="Recording" color="info" variant="outlined" /> : '—'}
                  </TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{fmtDate(c.startedAt || c.createdAt)}</TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.5}>
                      {['INITIATED', 'RINGING', 'IN_PROGRESS'].includes(c.status) && (
                        <Tooltip title="End Call">
                          <IconButton size="small" color="error" onClick={() => handleEndCall(c.id)}>
                            <CallEndIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      {c.notes && (
                        <Tooltip title="View Notes">
                          <IconButton size="small" onClick={() => setNotesDialog(c)}>
                            <DocIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {/* ── Tab 4: AI Transcription ──────────────── */}
      <TabPanel value={tab} index={4}>
        <Card variant="outlined" sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>AI Conversation Transcription</Typography>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Paste call recordings, meeting notes, or conversation content to get a structured transcript with speaker identification, key topics, and summary.
          </Typography>
          <TextField fullWidth label="Conversation Content" value={transcriptInput}
            onChange={e => setTranscriptInput(e.target.value)}
            placeholder="Paste conversation content, call transcript, or meeting notes here..."
            multiline rows={6} sx={{ mb: 2 }} />
          <Button variant="contained" startIcon={<TranscribeIcon />} onClick={handleTranscribe}
            disabled={loading || !transcriptInput.trim()}>Transcribe</Button>
        </Card>
        {transcriptResult && (
          <Card variant="outlined" sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>Transcription Result</Typography>
            <Alert severity="info" sx={{ mb: 2 }}>{transcriptResult.summary}</Alert>
            <Stack direction="row" spacing={1} mb={2} flexWrap="wrap">
              {transcriptResult.keyTopics.map((t, i) => <Chip key={i} label={t} color="primary" variant="outlined" size="small" />)}
            </Stack>
            <Typography variant="subtitle2" gutterBottom>Segments</Typography>
            <List dense>
              {transcriptResult.segments.map((seg, i) => (
                <ListItem key={i} sx={{ alignItems: 'flex-start' }}>
                  <ListItemIcon sx={{ minWidth: 32, mt: 0.5 }}>
                    <MicIcon fontSize="small" color="action" />
                  </ListItemIcon>
                  <ListItemText
                    primary={<Typography variant="subtitle2" color="primary">{seg.speaker}{seg.timestamp ? ` (${seg.timestamp})` : ''}</Typography>}
                    secondary={seg.text}
                  />
                </ListItem>
              ))}
            </List>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle2">Full Transcript</Typography>
            <Paper variant="outlined" sx={{ p: 2, mt: 1, maxHeight: 200, overflow: 'auto', bgcolor: 'grey.50' }}>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{transcriptResult.fullTranscript}</Typography>
            </Paper>
          </Card>
        )}
      </TabPanel>

      {/* ── Tab 5: AI Sentiment Analysis ─────────── */}
      <TabPanel value={tab} index={5}>
        <Card variant="outlined" sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>AI Call & Conversation Sentiment Analysis</Typography>
          <Typography variant="body2" color="text.secondary" mb={2}>
            Paste any conversation content - call transcripts, emails, chat logs - to analyze customer sentiment, detect emotions, and get CRM action recommendations.
          </Typography>
          <TextField fullWidth label="Content to Analyze" value={sentimentInput}
            onChange={e => setSentimentInput(e.target.value)}
            placeholder="Paste call transcript, email thread, or chat conversation here..."
            multiline rows={6} sx={{ mb: 2 }} />
          <Button variant="contained" color="secondary" startIcon={<SentimentIcon />}
            onClick={handleSentiment} disabled={loading || !sentimentInput.trim()}>Analyze Sentiment</Button>
        </Card>
        {sentimentResult && (
          <Card variant="outlined" sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>Sentiment Analysis Result</Typography>
            <Stack direction="row" spacing={2} mb={2} alignItems="center">
              <Chip
                label={sentimentResult.overallSentiment}
                color={sentimentResult.overallSentiment === 'POSITIVE' ? 'success' : sentimentResult.overallSentiment === 'NEGATIVE' ? 'error' : sentimentResult.overallSentiment === 'MIXED' ? 'warning' : 'default'}
                size="medium"
              />
              <Box sx={{ flex: 1 }}>
                <Typography variant="body2" color="text.secondary">
                  Score: {sentimentResult.sentimentScore.toFixed(2)} | Confidence: {(sentimentResult.confidence * 100).toFixed(0)}%
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={(sentimentResult.sentimentScore + 1) * 50}
                  color={sentimentResult.sentimentScore > 0.2 ? 'success' : sentimentResult.sentimentScore < -0.2 ? 'error' : 'warning'}
                  sx={{ mt: 0.5, height: 8, borderRadius: 4 }}
                />
              </Box>
            </Stack>
            <Alert severity="info" sx={{ mb: 2 }}>{sentimentResult.summary}</Alert>
            {sentimentResult.emotions.length > 0 && (
              <Box mb={2}>
                <Typography variant="subtitle2" gutterBottom>Emotions Detected</Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap">
                  {sentimentResult.emotions.map((e, i) => (
                    <Chip key={i} label={`${e.emotion}: ${(e.score * 100).toFixed(0)}%`}
                      size="small" variant="outlined" />
                  ))}
                </Stack>
              </Box>
            )}
            {sentimentResult.keyPhrases.length > 0 && (
              <Box mb={2}>
                <Typography variant="subtitle2" gutterBottom>Key Phrases</Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap">
                  {sentimentResult.keyPhrases.map((p, i) => <Chip key={i} label={p} size="small" color="primary" variant="outlined" />)}
                </Stack>
              </Box>
            )}
            {sentimentResult.concerns.length > 0 && (
              <Box mb={2}>
                <Typography variant="subtitle2" gutterBottom color="error">Concerns</Typography>
                <List dense>
                  {sentimentResult.concerns.map((c, i) => (
                    <ListItem key={i}><ListItemIcon sx={{ minWidth: 28 }}><ArrowDownIcon fontSize="small" color="error" /></ListItemIcon><ListItemText primary={c} /></ListItem>
                  ))}
                </List>
              </Box>
            )}
            {sentimentResult.positiveIndicators.length > 0 && (
              <Box mb={2}>
                <Typography variant="subtitle2" gutterBottom color="success.main">Positive Indicators</Typography>
                <List dense>
                  {sentimentResult.positiveIndicators.map((p, i) => (
                    <ListItem key={i}><ListItemIcon sx={{ minWidth: 28 }}><ArrowUpIcon fontSize="small" color="success" /></ListItemIcon><ListItemText primary={p} /></ListItem>
                  ))}
                </List>
              </Box>
            )}
            {sentimentResult.recommendation && (
              <Alert severity="success" sx={{ mt: 2 }}>
                <Typography variant="subtitle2">Recommendation</Typography>
                {sentimentResult.recommendation}
              </Alert>
            )}
          </Card>
        )}
      </TabPanel>

      {/* ── Call Notes Dialog ────────────────────── */}
      <Dialog open={!!notesDialog} onClose={() => setNotesDialog(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Call Notes</DialogTitle>
        <DialogContent>
          {notesDialog && (
            <Box>
              <Typography variant="body2" color="text.secondary" mb={1}>
                {notesDialog.fromNumber} → {notesDialog.toNumber} | {formatDuration(notesDialog.durationSeconds)}
              </Typography>
              {notesDialog.callOutcome && (
                <Alert severity="info" sx={{ mb: 2 }}>Outcome: {notesDialog.callOutcome}</Alert>
              )}
              <Typography variant="body1">{notesDialog.notes}</Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNotesDialog(null)}>Close</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!snack} autoHideDuration={3000} onClose={() => setSnack('')}
        message={snack} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }} />
    </Box>
  );
};

export default CommunicationsPage;
