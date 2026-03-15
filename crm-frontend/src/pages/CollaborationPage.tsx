/* ============================================================
   CollaborationPage – Real-Time Collaboration Hub
   Tabs: Deal Chat | Mentions | Shared Notes | Activity Stream | Approvals | Comments
   ============================================================ */
import React, { useEffect, useState, useCallback } from 'react';
import {
  Box, Typography, Tab, Tabs, Paper, TextField, Button, Stack, Chip, Avatar,
  List, ListItem, ListItemAvatar, ListItemText, IconButton, Divider, Badge,
  Card, CardContent, CardActions, MenuItem, Select, FormControl, InputLabel,
  Dialog, DialogTitle, DialogContent, DialogActions, Tooltip, Alert, LinearProgress,
} from '@mui/material';
import {
  Send as SendIcon, Chat as ChatIcon, AlternateEmail as MentionIcon,
  Note as NoteIcon, Stream as StreamIcon, Approval as ApprovalIcon,
  Comment as CommentIcon, Delete as DeleteIcon, CheckCircle, Cancel,
  PushPin as PinIcon, MarkChatRead, DoneAll, Schedule, PriorityHigh,
  ArrowUpward, ArrowDownward, Reply as ReplyIcon, Edit as EditIcon,
  FiberManualRecord as DotIcon,
} from '@mui/icons-material';
import { collaborationService } from '../services/collaborationService';
import type {
  DealChatMessage, MentionRecord, DealApproval, RecordComment,
  ActivityStreamEvent, PagedData,
} from '../types';
import { useSnackbar } from 'notistack';
import { PageHeader } from '../components';

/* ── Helpers ─────────────────────────────────────────────────── */
const TabPanel: React.FC<{ value: number; index: number; children: React.ReactNode }> = ({ value, index, children }) => (
  <Box role="tabpanel" hidden={value !== index} sx={{ pt: 2 }}>{value === index && children}</Box>
);
const fmtTime = (d: string) => new Date(d).toLocaleString();
const ago = (d: string) => {
  const diff = Date.now() - new Date(d).getTime();
  if (diff < 60000) return 'just now';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
  return `${Math.floor(diff / 86400000)}d ago`;
};
const initials = (name: string) => name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
const statusColor = (s: string) => {
  switch (s) { case 'APPROVED': return 'success'; case 'REJECTED': return 'error'; case 'PENDING': return 'warning'; case 'EXPIRED': return 'default'; default: return 'default'; }
};
const priorityIcon = (p: string) => {
  switch (p) { case 'URGENT': return <PriorityHigh sx={{ color: 'error.main' }} />; case 'HIGH': return <ArrowUpward sx={{ color: 'warning.main' }} />; default: return null; }
};
const eventIcon = (type: string) => {
  if (type.includes('CHAT')) return <ChatIcon fontSize="small" color="primary" />;
  if (type.includes('APPROVAL')) return <ApprovalIcon fontSize="small" color="warning" />;
  if (type.includes('COMMENT') || type.includes('NOTE')) return <CommentIcon fontSize="small" color="info" />;
  if (type.includes('STAGE')) return <StreamIcon fontSize="small" color="secondary" />;
  if (type.includes('WON')) return <CheckCircle fontSize="small" color="success" />;
  return <DotIcon fontSize="small" color="action" />;
};

const CollaborationPage: React.FC = () => {
  const [tab, setTab] = useState(0);
  const { enqueueSnackbar } = useSnackbar();

  /* ── Deal Chat state ───────────────────────────────────────── */
  const [chatMessages, setChatMessages] = useState<DealChatMessage[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [chatDealId, setChatDealId] = useState('');

  /* ── Mentions state ────────────────────────────────────────── */
  const [mentions, setMentions] = useState<MentionRecord[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);

  /* ── Approvals state ───────────────────────────────────────── */
  const [approvals, setApprovals] = useState<DealApproval[]>([]);
  const [approvalDialog, setApprovalDialog] = useState(false);
  const [approvalForm, setApprovalForm] = useState({ opportunityId: '', approverId: '', approverName: '', approvalType: 'DISCOUNT', title: '', description: '', currentValue: '', requestedValue: '', priority: 'NORMAL' });
  const [decisionDialog, setDecisionDialog] = useState<{ open: boolean; id: string }>({ open: false, id: '' });
  const [decisionComment, setDecisionComment] = useState('');

  /* ── Comments state ────────────────────────────────────────── */
  const [comments, setComments] = useState<RecordComment[]>([]);
  const [commentInput, setCommentInput] = useState('');
  const [commentRecordType, setCommentRecordType] = useState('OPPORTUNITY');
  const [commentRecordId, setCommentRecordId] = useState('');
  const [replyTo, setReplyTo] = useState<string | undefined>(undefined);

  /* ── Activity Stream state ─────────────────────────────────── */
  const [streamEvents, setStreamEvents] = useState<ActivityStreamEvent[]>([]);

  /* ── Loaders ───────────────────────────────────────────────── */
  const loadChat = useCallback(async () => {
    try {
      const res = await collaborationService.getChatMessages(chatDealId);
      setChatMessages(res.content || []);
    } catch { /* silent */ }
  }, [chatDealId]);

  const loadMentions = useCallback(async () => {
    try {
      const res = await collaborationService.getMyMentions();
      setMentions(res.content || []);
      const count = await collaborationService.getUnreadMentionCount();
      setUnreadCount(count);
    } catch { /* silent */ }
  }, []);

  const loadApprovals = useCallback(async () => {
    try {
      const res = await collaborationService.getAllApprovals();
      setApprovals(res.content || []);
    } catch { /* silent */ }
  }, []);

  const loadComments = useCallback(async () => {
    try {
      const res = await collaborationService.getComments(commentRecordType, commentRecordId);
      setComments(res.content || []);
    } catch { /* silent */ }
  }, [commentRecordType, commentRecordId]);

  const loadStream = useCallback(async () => {
    try {
      const res = await collaborationService.getActivityStream();
      setStreamEvents(res.content || []);
    } catch { /* silent */ }
  }, []);

  useEffect(() => { loadChat(); }, [loadChat]);
  useEffect(() => { loadMentions(); }, [loadMentions]);
  useEffect(() => { loadApprovals(); }, [loadApprovals]);
  useEffect(() => { loadComments(); }, [loadComments]);
  useEffect(() => { loadStream(); }, [loadStream]);

  // Poll for real-time updates
  useEffect(() => {
    const interval = setInterval(() => {
      if (tab === 0) loadChat();
      if (tab === 3) loadStream();
    }, 10000);
    return () => clearInterval(interval);
  }, [tab, loadChat, loadStream]);

  /* ── Handlers ──────────────────────────────────────────────── */
  const handleSendChat = async () => {
    if (!chatInput.trim()) return;
    await collaborationService.sendChatMessage(chatDealId, chatInput);
    setChatInput('');
    loadChat();
  };

  const handleMarkMentionRead = async (id: string) => {
    await collaborationService.markMentionAsRead(id);
    loadMentions();
  };

  const handleMarkAllRead = async () => {
    await collaborationService.markAllMentionsAsRead();
    loadMentions();
    enqueueSnackbar('All mentions marked as read', { variant: 'success' });
  };

  const handleCreateApproval = async () => {
    await collaborationService.createApproval(approvalForm);
    setApprovalDialog(false);
    setApprovalForm({ opportunityId: 'opp-1', approverId: 'user-5', approverName: 'VP Sarah Chen', approvalType: 'DISCOUNT', title: '', description: '', currentValue: '', requestedValue: '', priority: 'NORMAL' });
    loadApprovals();
    enqueueSnackbar('Approval request created', { variant: 'success' });
  };

  const handleDecision = async (decision: string) => {
    await collaborationService.decideApproval(decisionDialog.id, decision, decisionComment);
    setDecisionDialog({ open: false, id: '' });
    setDecisionComment('');
    loadApprovals();
    enqueueSnackbar(`Approval ${decision.toLowerCase()}`, { variant: decision === 'APPROVED' ? 'success' : 'warning' });
  };

  const handleAddComment = async () => {
    if (!commentInput.trim()) return;
    await collaborationService.addComment(commentRecordType, commentRecordId, commentInput, replyTo);
    setCommentInput('');
    setReplyTo(undefined);
    loadComments();
  };

  const handlePinComment = async (id: string, currentPinned: boolean) => {
    await collaborationService.updateComment(id, { isPinned: !currentPinned });
    loadComments();
  };

  const handleDeleteComment = async (id: string) => {
    await collaborationService.deleteComment(id);
    loadComments();
  };

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="Collaboration Hub"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Collaboration' }]}
      />
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Real-time collaboration for your sales team — chat, mentions, approvals, comments & activity stream.
      </Typography>

      <Paper sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto">
          <Tab icon={<ChatIcon />} label="Deal Chat" iconPosition="start" />
          <Tab icon={<Badge badgeContent={unreadCount} color="error"><MentionIcon /></Badge>} label="Mentions" iconPosition="start" />
          <Tab icon={<NoteIcon />} label="Shared Notes" iconPosition="start" />
          <Tab icon={<StreamIcon />} label="Activity Stream" iconPosition="start" />
          <Tab icon={<ApprovalIcon />} label="Approvals" iconPosition="start" />
          <Tab icon={<CommentIcon />} label="Comments" iconPosition="start" />
        </Tabs>
      </Paper>

      {/* ═══════════════════ TAB 0: Deal Chat ═══════════════════ */}
      <TabPanel value={tab} index={0}>
        <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
          <TextField label="Deal ID" size="small" value={chatDealId} onChange={e => setChatDealId(e.target.value)} sx={{ width: 200 }} />
          <Button variant="outlined" onClick={loadChat}>Load Chat</Button>
        </Stack>
        <Paper sx={{ maxHeight: 500, overflow: 'auto', p: 2, mb: 2, bgcolor: 'grey.50' }}>
          {chatMessages.length === 0 ? (
            <Typography color="text.secondary" textAlign="center" py={4}>No messages yet. Start the conversation!</Typography>
          ) : (
            <List disablePadding>
              {[...chatMessages].sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()).map((msg) => (
                <ListItem key={msg.id} alignItems="flex-start" sx={{ px: 0 }}>
                  <ListItemAvatar>
                    <Avatar sx={{ bgcolor: `hsl(${msg.senderId.charCodeAt(msg.senderId.length - 1) * 50}, 60%, 50%)`, width: 36, height: 36, fontSize: 14 }}>
                      {initials(msg.senderName)}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Typography variant="subtitle2" fontWeight={600}>{msg.senderName}</Typography>
                        <Typography variant="caption" color="text.secondary">{ago(msg.createdAt)}</Typography>
                        {msg.isEdited && <Chip label="edited" size="small" variant="outlined" sx={{ height: 18, fontSize: 10 }} />}
                      </Stack>
                    }
                    secondary={
                      <Typography variant="body2" sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>
                        {msg.message.replace(/@\[(.*?)\]\((.*?)\)/g, '@$1')}
                      </Typography>
                    }
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Paper>
        <Stack direction="row" spacing={1}>
          <TextField fullWidth placeholder="Type a message... (use @[Name](userId) for mentions)" size="small"
            value={chatInput} onChange={e => setChatInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSendChat(); } }}
          />
          <Button variant="contained" endIcon={<SendIcon />} onClick={handleSendChat}>Send</Button>
        </Stack>
      </TabPanel>

      {/* ═══════════════════ TAB 1: Mentions ═══════════════════ */}
      <TabPanel value={tab} index={1}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
          <Typography variant="h6">Your Mentions ({mentions.length})</Typography>
          <Button variant="outlined" startIcon={<DoneAll />} onClick={handleMarkAllRead} disabled={unreadCount === 0}>
            Mark All Read
          </Button>
        </Stack>
        {mentions.length === 0 ? (
          <Alert severity="info">No mentions yet. When someone @tags you, it will appear here.</Alert>
        ) : (
          <List>
            {mentions.map((m) => (
              <React.Fragment key={m.id}>
                <ListItem secondaryAction={
                  !m.isRead && (
                    <IconButton edge="end" onClick={() => handleMarkMentionRead(m.id)} title="Mark as read">
                      <MarkChatRead />
                    </IconButton>
                  )
                }>
                  <ListItemAvatar>
                    <Avatar sx={{ bgcolor: m.isRead ? 'grey.400' : 'primary.main', width: 36, height: 36, fontSize: 14 }}>
                      {initials(m.mentionedByName)}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Typography variant="subtitle2" fontWeight={m.isRead ? 400 : 700}>
                          {m.mentionedByName} mentioned you
                        </Typography>
                        {!m.isRead && <Chip label="NEW" size="small" color="error" sx={{ height: 18, fontSize: 10 }} />}
                      </Stack>
                    }
                    secondary={
                      <Stack spacing={0.5} sx={{ mt: 0.5 }}>
                        <Typography variant="caption" color="text.secondary">
                          In {m.sourceType.toLowerCase()} on {m.recordType.toLowerCase()} • {ago(m.createdAt)}
                        </Typography>
                      </Stack>
                    }
                  />
                </ListItem>
                <Divider variant="inset" component="li" />
              </React.Fragment>
            ))}
          </List>
        )}
      </TabPanel>

      {/* ═══════════════════ TAB 2: Shared Notes ═══════════════ */}
      <TabPanel value={tab} index={2}>
        <Alert severity="info" sx={{ mb: 2 }}>
          Shared notes are available on each deal's detail page. Navigate to an opportunity and use the Notes tab to add, pin, and manage shared deal notes collaboratively.
        </Alert>
        <Typography variant="h6" gutterBottom>Quick Add Note</Typography>
        <Stack spacing={2}>
          <Stack direction="row" spacing={2}>
            <FormControl size="small" sx={{ width: 200 }}>
              <InputLabel>Record Type</InputLabel>
              <Select value={commentRecordType} label="Record Type" onChange={e => setCommentRecordType(e.target.value)}>
                <MenuItem value="OPPORTUNITY">Opportunity</MenuItem>
                <MenuItem value="ACCOUNT">Account</MenuItem>
                <MenuItem value="LEAD">Lead</MenuItem>
                <MenuItem value="CONTACT">Contact</MenuItem>
              </Select>
            </FormControl>
            <TextField size="small" label="Record ID" value={commentRecordId} onChange={e => setCommentRecordId(e.target.value)} sx={{ width: 250 }} />
          </Stack>
          <TextField multiline rows={3} placeholder="Write a shared note... Use @[Name](userId) to mention teammates" fullWidth
            value={commentInput} onChange={e => setCommentInput(e.target.value)} />
          <Button variant="contained" onClick={handleAddComment} disabled={!commentInput.trim()}>Add Note</Button>
        </Stack>

        <Divider sx={{ my: 3 }} />
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          Notes for {commentRecordType} ({comments.length})
        </Typography>
        {comments.filter(c => c.isPinned).length > 0 && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="caption" color="primary" fontWeight={600} sx={{ mb: 1, display: 'block' }}>📌 Pinned</Typography>
            {comments.filter(c => c.isPinned).map(c => (
              <Card key={c.id} variant="outlined" sx={{ mb: 1, borderColor: 'primary.light' }}>
                <CardContent sx={{ py: 1, '&:last-child': { pb: 1 } }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="subtitle2">{c.authorName} • {ago(c.createdAt)}</Typography>
                    <IconButton size="small" onClick={() => handlePinComment(c.id, c.isPinned)}><PinIcon fontSize="small" color="primary" /></IconButton>
                  </Stack>
                  <Typography variant="body2" sx={{ mt: 0.5 }}>{c.content.replace(/@\[(.*?)\]\((.*?)\)/g, '@$1')}</Typography>
                </CardContent>
              </Card>
            ))}
          </Box>
        )}
        {comments.filter(c => !c.isPinned).map(c => (
          <Card key={c.id} variant="outlined" sx={{ mb: 1 }}>
            <CardContent sx={{ py: 1, '&:last-child': { pb: 1 } }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="subtitle2">{c.authorName} • {ago(c.createdAt)}
                  {c.isEdited && <Chip label="edited" size="small" variant="outlined" sx={{ ml: 1, height: 18, fontSize: 10 }} />}
                </Typography>
                <Stack direction="row">
                  <IconButton size="small" onClick={() => handlePinComment(c.id, c.isPinned)}><PinIcon fontSize="small" /></IconButton>
                  <IconButton size="small" onClick={() => handleDeleteComment(c.id)}><DeleteIcon fontSize="small" /></IconButton>
                </Stack>
              </Stack>
              <Typography variant="body2" sx={{ mt: 0.5 }}>{c.content.replace(/@\[(.*?)\]\((.*?)\)/g, '@$1')}</Typography>
              {c.replies && c.replies.length > 0 && (
                <Box sx={{ ml: 3, mt: 1, borderLeft: '2px solid', borderColor: 'divider', pl: 2 }}>
                  {c.replies.map(r => (
                    <Box key={r.id} sx={{ mb: 1 }}>
                      <Typography variant="caption" fontWeight={600}>{r.authorName}</Typography>
                      <Typography variant="caption" color="text.secondary"> • {ago(r.createdAt)}</Typography>
                      <Typography variant="body2">{r.content.replace(/@\[(.*?)\]\((.*?)\)/g, '@$1')}</Typography>
                    </Box>
                  ))}
                </Box>
              )}
            </CardContent>
          </Card>
        ))}
      </TabPanel>

      {/* ═══════════════════ TAB 3: Activity Stream ═══════════ */}
      <TabPanel value={tab} index={3}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
          <Typography variant="h6">Real-Time Activity Feed</Typography>
          <Button variant="outlined" onClick={loadStream}>Refresh</Button>
        </Stack>
        <Alert severity="info" sx={{ mb: 2 }}>
          Activity stream auto-refreshes every 10 seconds. SSE (Server-Sent Events) real-time push is available when the backend is running.
        </Alert>
        {streamEvents.length === 0 ? (
          <Typography color="text.secondary" textAlign="center" py={4}>No activity events yet.</Typography>
        ) : (
          <List>
            {streamEvents.map((evt) => (
              <React.Fragment key={evt.id}>
                <ListItem alignItems="flex-start">
                  <ListItemAvatar>
                    <Avatar sx={{ bgcolor: 'background.default', border: '1px solid', borderColor: 'divider', width: 36, height: 36 }}>
                      {eventIcon(evt.eventType)}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Typography variant="subtitle2" fontWeight={600}>{evt.performedByName}</Typography>
                        <Chip label={evt.eventType.replace(/_/g, ' ')} size="small" variant="outlined" sx={{ height: 20, fontSize: 10 }} />
                        <Typography variant="caption" color="text.secondary">{ago(evt.createdAt)}</Typography>
                      </Stack>
                    }
                    secondary={
                      <Stack spacing={0.5} sx={{ mt: 0.5 }}>
                        <Typography variant="body2">{evt.description}</Typography>
                        {evt.entityName && (
                          <Typography variant="caption" color="text.secondary">
                            {evt.entityType}: {evt.entityName}
                          </Typography>
                        )}
                      </Stack>
                    }
                  />
                </ListItem>
                <Divider variant="inset" component="li" />
              </React.Fragment>
            ))}
          </List>
        )}
      </TabPanel>

      {/* ═══════════════════ TAB 4: Approvals ═══════════════════ */}
      <TabPanel value={tab} index={4}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
          <Typography variant="h6">Deal Approvals</Typography>
          <Button variant="contained" onClick={() => setApprovalDialog(true)}>Request Approval</Button>
        </Stack>

        {approvals.length === 0 ? (
          <Alert severity="info">No approval requests yet.</Alert>
        ) : (
          <Stack spacing={2}>
            {approvals.map(a => (
              <Card key={a.id} variant="outlined">
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                    <Box>
                      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                        {priorityIcon(a.priority)}
                        <Typography variant="subtitle1" fontWeight={600}>{a.title}</Typography>
                        <Chip label={a.status} size="small" color={statusColor(a.status) as any} />
                        <Chip label={a.approvalType.replace(/_/g, ' ')} size="small" variant="outlined" />
                      </Stack>
                      {a.description && <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>{a.description}</Typography>}
                      <Stack direction="row" spacing={3} sx={{ mt: 1 }}>
                        <Typography variant="caption"><strong>Requested by:</strong> {a.requestedByName}</Typography>
                        <Typography variant="caption"><strong>Approver:</strong> {a.approverName}</Typography>
                        {a.currentValue && <Typography variant="caption"><strong>Current:</strong> {a.currentValue}</Typography>}
                        {a.requestedValue && <Typography variant="caption"><strong>Requested:</strong> {a.requestedValue}</Typography>}
                      </Stack>
                      {a.approverComment && (
                        <Alert severity={a.status === 'APPROVED' ? 'success' : 'warning'} sx={{ mt: 1, py: 0 }}>
                          <Typography variant="body2">{a.approverComment}</Typography>
                        </Alert>
                      )}
                      <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                        Created {ago(a.createdAt)}
                        {a.dueDate && ` • Due ${fmtTime(a.dueDate)}`}
                        {a.decidedAt && ` • Decided ${ago(a.decidedAt)}`}
                      </Typography>
                    </Box>
                  </Stack>
                </CardContent>
                {a.status === 'PENDING' && (
                  <CardActions>
                    <Button size="small" color="success" startIcon={<CheckCircle />}
                      onClick={() => { setDecisionDialog({ open: true, id: a.id }); }}>
                      Approve
                    </Button>
                    <Button size="small" color="error" startIcon={<Cancel />}
                      onClick={() => { setDecisionDialog({ open: true, id: a.id }); }}>
                      Reject
                    </Button>
                  </CardActions>
                )}
              </Card>
            ))}
          </Stack>
        )}

        {/* Create Approval Dialog */}
        <Dialog open={approvalDialog} onClose={() => setApprovalDialog(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Request Deal Approval</DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ mt: 1 }}>
              <TextField label="Title" fullWidth required value={approvalForm.title}
                onChange={e => setApprovalForm({ ...approvalForm, title: e.target.value })} />
              <TextField label="Description" fullWidth multiline rows={2} value={approvalForm.description}
                onChange={e => setApprovalForm({ ...approvalForm, description: e.target.value })} />
              <Stack direction="row" spacing={2}>
                <FormControl fullWidth size="small">
                  <InputLabel>Type</InputLabel>
                  <Select value={approvalForm.approvalType} label="Type"
                    onChange={e => setApprovalForm({ ...approvalForm, approvalType: e.target.value })}>
                    <MenuItem value="DISCOUNT">Discount</MenuItem>
                    <MenuItem value="STAGE_CHANGE">Stage Change</MenuItem>
                    <MenuItem value="CLOSE_DEAL">Close Deal</MenuItem>
                    <MenuItem value="PRICING">Pricing</MenuItem>
                    <MenuItem value="CUSTOM">Custom</MenuItem>
                  </Select>
                </FormControl>
                <FormControl fullWidth size="small">
                  <InputLabel>Priority</InputLabel>
                  <Select value={approvalForm.priority} label="Priority"
                    onChange={e => setApprovalForm({ ...approvalForm, priority: e.target.value })}>
                    <MenuItem value="LOW">Low</MenuItem>
                    <MenuItem value="NORMAL">Normal</MenuItem>
                    <MenuItem value="HIGH">High</MenuItem>
                    <MenuItem value="URGENT">Urgent</MenuItem>
                  </Select>
                </FormControl>
              </Stack>
              <Stack direction="row" spacing={2}>
                <TextField label="Current Value" size="small" fullWidth value={approvalForm.currentValue}
                  onChange={e => setApprovalForm({ ...approvalForm, currentValue: e.target.value })} />
                <TextField label="Requested Value" size="small" fullWidth value={approvalForm.requestedValue}
                  onChange={e => setApprovalForm({ ...approvalForm, requestedValue: e.target.value })} />
              </Stack>
              <TextField label="Approver Name" size="small" fullWidth value={approvalForm.approverName}
                onChange={e => setApprovalForm({ ...approvalForm, approverName: e.target.value })} />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setApprovalDialog(false)}>Cancel</Button>
            <Button variant="contained" onClick={handleCreateApproval} disabled={!approvalForm.title}>Submit</Button>
          </DialogActions>
        </Dialog>

        {/* Decision Dialog */}
        <Dialog open={decisionDialog.open} onClose={() => setDecisionDialog({ open: false, id: '' })} maxWidth="sm" fullWidth>
          <DialogTitle>Approval Decision</DialogTitle>
          <DialogContent>
            <TextField label="Comment (optional)" fullWidth multiline rows={3} sx={{ mt: 1 }}
              value={decisionComment} onChange={e => setDecisionComment(e.target.value)} />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDecisionDialog({ open: false, id: '' })}>Cancel</Button>
            <Button variant="contained" color="error" onClick={() => handleDecision('REJECTED')}>Reject</Button>
            <Button variant="contained" color="success" onClick={() => handleDecision('APPROVED')}>Approve</Button>
          </DialogActions>
        </Dialog>
      </TabPanel>

      {/* ═══════════════════ TAB 5: Comments ═══════════════════ */}
      <TabPanel value={tab} index={5}>
        <Typography variant="h6" gutterBottom>Internal Comments</Typography>
        <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
          <FormControl size="small" sx={{ width: 180 }}>
            <InputLabel>Record Type</InputLabel>
            <Select value={commentRecordType} label="Record Type" onChange={e => { setCommentRecordType(e.target.value); }}>
              <MenuItem value="OPPORTUNITY">Opportunity</MenuItem>
              <MenuItem value="ACCOUNT">Account</MenuItem>
              <MenuItem value="LEAD">Lead</MenuItem>
              <MenuItem value="CONTACT">Contact</MenuItem>
            </Select>
          </FormControl>
          <TextField size="small" label="Record ID" value={commentRecordId}
            onChange={e => setCommentRecordId(e.target.value)} sx={{ width: 220 }} />
          <Button variant="outlined" onClick={loadComments}>Load</Button>
        </Stack>

        {replyTo && (
          <Alert severity="info" sx={{ mb: 1 }}
            action={<Button size="small" onClick={() => setReplyTo(undefined)}>Cancel</Button>}>
            Replying to comment...
          </Alert>
        )}
        <Stack direction="row" spacing={1} sx={{ mb: 3 }}>
          <TextField fullWidth size="small" placeholder="Write a comment... (use @[Name](userId) for mentions)"
            value={commentInput} onChange={e => setCommentInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleAddComment(); } }}
          />
          <Button variant="contained" onClick={handleAddComment} disabled={!commentInput.trim()}>
            {replyTo ? 'Reply' : 'Comment'}
          </Button>
        </Stack>

        {comments.length === 0 ? (
          <Alert severity="info">No comments for this record yet.</Alert>
        ) : (
          <Stack spacing={1}>
            {comments.map(c => (
              <Card key={c.id} variant="outlined" sx={{ borderLeft: c.isPinned ? '3px solid' : undefined, borderLeftColor: c.isPinned ? 'primary.main' : undefined }}>
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Avatar sx={{ width: 28, height: 28, fontSize: 12, bgcolor: `hsl(${c.authorId.charCodeAt(c.authorId.length - 1) * 50}, 60%, 50%)` }}>
                        {initials(c.authorName)}
                      </Avatar>
                      <Typography variant="subtitle2" fontWeight={600}>{c.authorName}</Typography>
                      <Typography variant="caption" color="text.secondary">{ago(c.createdAt)}</Typography>
                      {c.isEdited && <Chip label="edited" size="small" variant="outlined" sx={{ height: 18, fontSize: 10 }} />}
                      {c.isPinned && <PinIcon sx={{ fontSize: 14, color: 'primary.main' }} />}
                    </Stack>
                    <Stack direction="row">
                      <Tooltip title="Reply"><IconButton size="small" onClick={() => setReplyTo(c.id)}><ReplyIcon fontSize="small" /></IconButton></Tooltip>
                      <Tooltip title={c.isPinned ? 'Unpin' : 'Pin'}><IconButton size="small" onClick={() => handlePinComment(c.id, c.isPinned)}><PinIcon fontSize="small" /></IconButton></Tooltip>
                      <Tooltip title="Delete"><IconButton size="small" onClick={() => handleDeleteComment(c.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
                    </Stack>
                  </Stack>
                  <Typography variant="body2" sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>
                    {c.content.replace(/@\[(.*?)\]\((.*?)\)/g, '@$1')}
                  </Typography>
                  {c.replies && c.replies.length > 0 && (
                    <Box sx={{ ml: 4, mt: 1.5, borderLeft: '2px solid', borderColor: 'divider', pl: 2 }}>
                      {c.replies.map(r => (
                        <Box key={r.id} sx={{ mb: 1 }}>
                          <Stack direction="row" spacing={1} alignItems="center">
                            <Avatar sx={{ width: 22, height: 22, fontSize: 10 }}>{initials(r.authorName)}</Avatar>
                            <Typography variant="caption" fontWeight={600}>{r.authorName}</Typography>
                            <Typography variant="caption" color="text.secondary">{ago(r.createdAt)}</Typography>
                          </Stack>
                          <Typography variant="body2" sx={{ ml: 3.5 }}>{r.content.replace(/@\[(.*?)\]\((.*?)\)/g, '@$1')}</Typography>
                        </Box>
                      ))}
                    </Box>
                  )}
                </CardContent>
              </Card>
            ))}
          </Stack>
        )}
      </TabPanel>
    </Box>
  );
};

export default CollaborationPage;
