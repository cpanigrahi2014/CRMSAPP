/* ============================================================
   ContactDetailPage – 8-tab detail view for a contact
   Tabs: Details | Communications | Notes | Timeline | Tags | Attachments | Social & Consent | Analytics
   ============================================================ */
import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Typography, Grid, Paper, Chip, Button, TextField, Stack, Avatar,
  Divider, IconButton, Tooltip, Tab, Tabs, Switch, FormControlLabel,
  Select, MenuItem, InputLabel, FormControl, Dialog, DialogTitle, DialogContent,
  DialogActions, Table, TableHead, TableRow, TableCell, TableBody, Card,
  CardContent, LinearProgress,
} from '@mui/material';
import {
  ArrowBack, Edit as EditIcon, Save as SaveIcon, Cancel as CancelIcon,
  Email as EmailIcon, Phone as PhoneIcon, LinkedIn, Facebook, Twitter,
  Add as AddIcon, Delete as DeleteIcon, Send as SendIcon, CallMade, CallReceived,
  Timeline as TimelineIcon, Label as LabelIcon, Analytics as AnalyticsIcon,
  NoteAdd as NoteAddIcon, AttachFile as AttachFileIcon,
} from '@mui/icons-material';
import { PageHeader, ConfirmDialog } from '../components';
import { contactService } from '../services';
import type {
  Contact, ContactCommunication, ContactActivity, ContactTag, ContactAnalytics,
  ContactNote, ContactAttachment,
} from '../types';
import { useSnackbar } from 'notistack';

/* ---- Tab panel helper ---- */
const TabPanel: React.FC<{ value: number; index: number; children: React.ReactNode }> = ({ value, index, children }) => (
  <Box role="tabpanel" hidden={value !== index} sx={{ pt: 2 }}>
    {value === index && children}
  </Box>
);

const lifecycleOptions = ['SUBSCRIBER', 'LEAD', 'MQL', 'SQL', 'OPPORTUNITY', 'CUSTOMER', 'EVANGELIST', 'OTHER'];
const segmentOptions = ['Enterprise', 'Mid-Market', 'SMB', 'Startup', 'Government', 'Non-Profit', 'Other'];
const leadSourceOptions = ['Website', 'Referral', 'Campaign', 'Social Media', 'Trade Show', 'Cold Call', 'Partner', 'Other'];
const commTypes = ['EMAIL', 'CALL', 'MEETING', 'NOTE', 'SMS'];
const commDirections = ['OUTBOUND', 'INBOUND'];

const ContactDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [contact, setContact] = useState<Contact | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState(0);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState<Record<string, any>>({});

  // Communications
  const [comms, setComms] = useState<ContactCommunication[]>([]);
  const [commDialogOpen, setCommDialogOpen] = useState(false);
  const [commForm, setCommForm] = useState({ commType: 'EMAIL', subject: '', body: '', direction: 'OUTBOUND', status: 'COMPLETED' });

  // Timeline
  const [activities, setActivities] = useState<ContactActivity[]>([]);

  // Tags
  const [tags, setTags] = useState<ContactTag[]>([]);
  const [newTag, setNewTag] = useState('');

  // Analytics
  const [analytics, setAnalytics] = useState<ContactAnalytics | null>(null);

  // Notes
  const [notes, setNotes] = useState<ContactNote[]>([]);
  const [newNote, setNewNote] = useState('');

  // Attachments
  const [attachments, setAttachments] = useState<ContactAttachment[]>([]);
  const [attachDialog, setAttachDialog] = useState(false);
  const [attachForm, setAttachForm] = useState({ fileName: '', fileUrl: '', fileType: '' });

  /* ── Loaders ── */
  const fetchContact = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const res = await contactService.getById(id);
      setContact(res.data);
    } catch { enqueueSnackbar('Failed to load contact', { variant: 'error' }); }
    finally { setLoading(false); }
  }, [id, enqueueSnackbar]);

  const fetchComms = useCallback(async () => {
    if (!id) return;
    try {
      const res = await contactService.getCommunications(id, 0, 100);
      const d = res.data as any;
      setComms(Array.isArray(d) ? d : d.content ?? []);
    } catch { /* silent */ }
  }, [id]);

  const fetchActivities = useCallback(async () => {
    if (!id) return;
    try {
      const res = await contactService.getActivities(id, 0, 100);
      const d = res.data as any;
      setActivities(Array.isArray(d) ? d : d.content ?? []);
    } catch { /* silent */ }
  }, [id]);

  const fetchTags = useCallback(async () => {
    if (!id) return;
    try {
      const res = await contactService.getTags(id);
      setTags(Array.isArray(res.data) ? res.data : []);
    } catch { /* silent */ }
  }, [id]);

  const fetchAnalytics = useCallback(async () => {
    try {
      const res = await contactService.getAnalytics();
      setAnalytics(res.data);
    } catch { /* silent */ }
  }, []);

  const fetchNotes = useCallback(async () => {
    if (!id) return;
    try {
      const res = await contactService.getNotes(id);
      setNotes(Array.isArray(res.data) ? res.data : []);
    } catch { /* silent */ }
  }, [id]);

  const fetchAttachments = useCallback(async () => {
    if (!id) return;
    try {
      const res = await contactService.getAttachments(id);
      setAttachments(Array.isArray(res.data) ? res.data : []);
    } catch { /* silent */ }
  }, [id]);

  useEffect(() => { fetchContact(); }, [fetchContact]);

  useEffect(() => {
    if (tab === 1) fetchComms();
    if (tab === 2) fetchNotes();
    if (tab === 3) fetchActivities();
    if (tab === 4) fetchTags();
    if (tab === 5) fetchAttachments();
    if (tab === 7) fetchAnalytics();
  }, [tab, fetchComms, fetchNotes, fetchActivities, fetchTags, fetchAttachments, fetchAnalytics]);

  /* ── Edit mode ── */
  const startEdit = () => {
    if (!contact) return;
    setFormData({
      firstName: contact.firstName ?? '',
      lastName: contact.lastName ?? '',
      email: contact.email ?? '',
      phone: contact.phone ?? '',
      mobilePhone: contact.mobilePhone ?? '',
      title: contact.title ?? '',
      department: contact.department ?? '',
      accountId: contact.accountId ?? '',
      mailingAddress: contact.mailingAddress ?? '',
      description: contact.description ?? '',
      linkedinUrl: contact.linkedinUrl ?? '',
      twitterUrl: contact.twitterUrl ?? '',
      facebookUrl: contact.facebookUrl ?? '',
      otherSocialUrl: contact.otherSocialUrl ?? '',
      leadSource: contact.leadSource ?? '',
      lifecycleStage: contact.lifecycleStage ?? '',
      segment: contact.segment ?? '',
      emailOptIn: contact.emailOptIn ?? false,
      smsOptIn: contact.smsOptIn ?? false,
      phoneOptIn: contact.phoneOptIn ?? false,
      doNotCall: contact.doNotCall ?? false,
      consentSource: contact.consentSource ?? '',
    });
    setEditing(true);
  };

  const saveEdit = async () => {
    if (!id) return;
    try {
      await contactService.update(id, formData);
      enqueueSnackbar('Contact updated', { variant: 'success' });
      setEditing(false);
      fetchContact();
    } catch { enqueueSnackbar('Update failed', { variant: 'error' }); }
  };

  const handleField = (field: string) => (e: any) => {
    const val = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setFormData((p: Record<string, any>) => ({ ...p, [field]: val }));
  };

  /* ── Communication ── */
  const addComm = async () => {
    if (!id) return;
    try {
      await contactService.addCommunication(id, commForm);
      enqueueSnackbar('Communication logged', { variant: 'success' });
      setCommDialogOpen(false);
      setCommForm({ commType: 'EMAIL', subject: '', body: '', direction: 'OUTBOUND', status: 'COMPLETED' });
      fetchComms();
    } catch { enqueueSnackbar('Failed to log communication', { variant: 'error' }); }
  };

  /* ── Tags ── */
  const addTag = async () => {
    if (!id || !newTag.trim()) return;
    try {
      await contactService.addTag(id, newTag.trim());
      setNewTag('');
      fetchTags();
    } catch { enqueueSnackbar('Failed to add tag', { variant: 'error' }); }
  };

  const removeTag = async (tagName: string) => {
    if (!id) return;
    try {
      await contactService.removeTag(id, tagName);
      fetchTags();
    } catch { enqueueSnackbar('Failed to remove tag', { variant: 'error' }); }
  };

  /* ── Notes ── */
  const addNote = async () => {
    if (!id || !newNote.trim()) return;
    try {
      await contactService.addNote(id, newNote.trim());
      setNewNote('');
      enqueueSnackbar('Note added', { variant: 'success' });
      fetchNotes();
    } catch { enqueueSnackbar('Failed to add note', { variant: 'error' }); }
  };

  const deleteNote = async (noteId: string) => {
    try {
      await contactService.deleteNote(noteId);
      fetchNotes();
    } catch { enqueueSnackbar('Failed to delete note', { variant: 'error' }); }
  };

  /* ── Attachments ── */
  const addAttachment = async () => {
    if (!id || !attachForm.fileName.trim() || !attachForm.fileUrl.trim()) return;
    try {
      await contactService.addAttachment(id, attachForm);
      setAttachDialog(false);
      setAttachForm({ fileName: '', fileUrl: '', fileType: '' });
      enqueueSnackbar('Attachment added', { variant: 'success' });
      fetchAttachments();
    } catch { enqueueSnackbar('Failed to add attachment', { variant: 'error' }); }
  };

  const deleteAttachment = async (attachmentId: string) => {
    try {
      await contactService.deleteAttachment(attachmentId);
      fetchAttachments();
    } catch { enqueueSnackbar('Failed to delete attachment', { variant: 'error' }); }
  };

  if (loading) return <LinearProgress />;
  if (!contact) return <Typography>Contact not found</Typography>;

  const fullName = `${contact.firstName} ${contact.lastName}`;

  /* ──────────────────────────── RENDER ──────────────────────────── */
  return (
    <>
      <PageHeader
        title={fullName}
        breadcrumbs={[
          { label: 'Dashboard', to: '/dashboard' },
          { label: 'Contacts', to: '/contacts' },
          { label: fullName },
        ]}
      />

      {/* Header card */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Stack direction="row" spacing={3} alignItems="center">
          <Avatar sx={{ width: 64, height: 64, fontSize: 24, bgcolor: 'primary.main' }}>
            {(contact.firstName?.[0] ?? '') + (contact.lastName?.[0] ?? '')}
          </Avatar>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h5" fontWeight={700}>{fullName}</Typography>
            <Typography variant="body2" color="text.secondary">{contact.title} {contact.department ? `· ${contact.department}` : ''}</Typography>
            <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
              {contact.segment && <Chip label={contact.segment} size="small" color="primary" variant="outlined" />}
              {contact.lifecycleStage && <Chip label={contact.lifecycleStage} size="small" color="secondary" variant="outlined" />}
              {contact.emailOptIn && <Chip label="Email Opt-In" size="small" color="success" />}
              {contact.doNotCall && <Chip label="Do Not Call" size="small" color="error" />}
            </Stack>
          </Box>
          <Stack direction="row" spacing={1}>
            <Button startIcon={<ArrowBack />} onClick={() => navigate('/contacts')}>Back</Button>
            {!editing ? (
              <Button variant="contained" startIcon={<EditIcon />} onClick={startEdit}>Edit</Button>
            ) : (
              <>
                <Button variant="contained" color="success" startIcon={<SaveIcon />} onClick={saveEdit}>Save</Button>
                <Button startIcon={<CancelIcon />} onClick={() => setEditing(false)}>Cancel</Button>
              </>
            )}
          </Stack>
        </Stack>
      </Paper>

      {/* Tabs */}
      <Paper sx={{ mb: 2 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto">
          <Tab label="Details" />
          <Tab label="Communications" icon={<EmailIcon />} iconPosition="start" />
          <Tab label="Notes" icon={<NoteAddIcon />} iconPosition="start" />
          <Tab label="Timeline" icon={<TimelineIcon />} iconPosition="start" />
          <Tab label="Tags" icon={<LabelIcon />} iconPosition="start" />
          <Tab label="Attachments" icon={<AttachFileIcon />} iconPosition="start" />
          <Tab label="Social & Consent" />
          <Tab label="Analytics" icon={<AnalyticsIcon />} iconPosition="start" />
        </Tabs>
      </Paper>

      {/* ── Tab 0: Details ── */}
      <TabPanel value={tab} index={0}>
        <Paper sx={{ p: 3 }}>
          <Grid container spacing={2}>
            {editing ? (
              <>
                <Grid item xs={12} sm={6}><TextField fullWidth label="First Name" value={formData.firstName} onChange={handleField('firstName')} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth label="Last Name" value={formData.lastName} onChange={handleField('lastName')} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth label="Email" value={formData.email} onChange={handleField('email')} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth label="Phone" value={formData.phone} onChange={handleField('phone')} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth label="Mobile Phone" value={formData.mobilePhone} onChange={handleField('mobilePhone')} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth label="Job Title" value={formData.title} onChange={handleField('title')} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth label="Department" value={formData.department} onChange={handleField('department')} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth label="Account ID" value={formData.accountId} onChange={handleField('accountId')} /></Grid>
                <Grid item xs={12}><TextField fullWidth label="Mailing Address" value={formData.mailingAddress} onChange={handleField('mailingAddress')} /></Grid>
                <Grid item xs={12}><TextField fullWidth multiline rows={2} label="Description" value={formData.description} onChange={handleField('description')} /></Grid>
                <Grid item xs={12}><Divider>Segmentation</Divider></Grid>
                <Grid item xs={12} sm={4}>
                  <FormControl fullWidth><InputLabel>Lifecycle Stage</InputLabel>
                    <Select value={formData.lifecycleStage} label="Lifecycle Stage" onChange={handleField('lifecycleStage')}>
                      {lifecycleOptions.map((o) => <MenuItem key={o} value={o}>{o}</MenuItem>)}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <FormControl fullWidth><InputLabel>Segment</InputLabel>
                    <Select value={formData.segment} label="Segment" onChange={handleField('segment')}>
                      {segmentOptions.map((o) => <MenuItem key={o} value={o}>{o}</MenuItem>)}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <FormControl fullWidth><InputLabel>Lead Source</InputLabel>
                    <Select value={formData.leadSource} label="Lead Source" onChange={handleField('leadSource')}>
                      {leadSourceOptions.map((o) => <MenuItem key={o} value={o}>{o}</MenuItem>)}
                    </Select>
                  </FormControl>
                </Grid>
              </>
            ) : (
              <>
                {[
                  ['Email', contact.email],
                  ['Phone', contact.phone],
                  ['Mobile', contact.mobilePhone],
                  ['Title', contact.title],
                  ['Department', contact.department],
                  ['Account ID', contact.accountId],
                  ['Mailing Address', contact.mailingAddress],
                  ['Lead Source', contact.leadSource],
                  ['Lifecycle Stage', contact.lifecycleStage],
                  ['Segment', contact.segment],
                  ['Created', contact.createdAt ? new Date(contact.createdAt).toLocaleString() : ''],
                  ['Updated', contact.updatedAt ? new Date(contact.updatedAt).toLocaleString() : ''],
                ].map(([label, value]) => (
                  <Grid item xs={12} sm={6} key={label as string}>
                    <Typography variant="caption" color="text.secondary">{label}</Typography>
                    <Typography variant="body1">{(value as string) || '—'}</Typography>
                  </Grid>
                ))}
                {contact.description && (
                  <Grid item xs={12}>
                    <Typography variant="caption" color="text.secondary">Description</Typography>
                    <Typography variant="body1">{contact.description}</Typography>
                  </Grid>
                )}
              </>
            )}
          </Grid>
        </Paper>
      </TabPanel>

      {/* ── Tab 1: Communications ── */}
      <TabPanel value={tab} index={1}>
        <Paper sx={{ p: 3 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography variant="h6">Communication History</Typography>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCommDialogOpen(true)}>Log Communication</Button>
          </Stack>
          {comms.length === 0 ? (
            <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>No communications yet</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Type</TableCell>
                  <TableCell>Direction</TableCell>
                  <TableCell>Subject</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {comms.map((c) => (
                  <TableRow key={c.id} hover>
                    <TableCell><Chip label={c.commType} size="small" /></TableCell>
                    <TableCell>
                      {c.direction === 'OUTBOUND'
                        ? <Chip icon={<CallMade />} label="Outbound" size="small" color="primary" variant="outlined" />
                        : <Chip icon={<CallReceived />} label="Inbound" size="small" color="secondary" variant="outlined" />}
                    </TableCell>
                    <TableCell>{c.subject || '—'}</TableCell>
                    <TableCell>{c.status}</TableCell>
                    <TableCell>{c.communicationDate ? new Date(c.communicationDate).toLocaleString() : ''}</TableCell>
                    <TableCell>
                      <IconButton size="small" color="error" onClick={async () => {
                        try { await contactService.deleteCommunication(c.id); fetchComms(); } catch { /* */ }
                      }}><DeleteIcon fontSize="small" /></IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Paper>

        {/* Communication dialog */}
        <Dialog open={commDialogOpen} onClose={() => setCommDialogOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Log Communication</DialogTitle>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0 }}>
              <Grid item xs={6}>
                <FormControl fullWidth><InputLabel>Type</InputLabel>
                  <Select value={commForm.commType} label="Type" onChange={(e) => setCommForm((p) => ({ ...p, commType: e.target.value }))}>
                    {commTypes.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={6}>
                <FormControl fullWidth><InputLabel>Direction</InputLabel>
                  <Select value={commForm.direction} label="Direction" onChange={(e) => setCommForm((p) => ({ ...p, direction: e.target.value }))}>
                    {commDirections.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}><TextField fullWidth label="Subject" value={commForm.subject} onChange={(e) => setCommForm((p) => ({ ...p, subject: e.target.value }))} /></Grid>
              <Grid item xs={12}><TextField fullWidth multiline rows={3} label="Body / Notes" value={commForm.body} onChange={(e) => setCommForm((p) => ({ ...p, body: e.target.value }))} /></Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCommDialogOpen(false)}>Cancel</Button>
            <Button variant="contained" startIcon={<SendIcon />} onClick={addComm}>Save</Button>
          </DialogActions>
        </Dialog>
      </TabPanel>

      {/* ── Tab 2: Notes ── */}
      <TabPanel value={tab} index={2}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>Notes</Typography>
          <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
            <TextField
              fullWidth
              size="small"
              label="Add a note"
              multiline
              minRows={2}
              value={newNote}
              onChange={(e) => setNewNote(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && e.ctrlKey) addNote(); }}
            />
            <Button variant="contained" startIcon={<AddIcon />} onClick={addNote} sx={{ alignSelf: 'flex-start' }}>Add</Button>
          </Stack>
          {notes.length === 0 ? (
            <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>No notes yet</Typography>
          ) : (
            <Stack spacing={1}>
              {notes.map((n) => (
                <Paper key={n.id} variant="outlined" sx={{ p: 2 }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>{n.content}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {n.createdBy ? `By ${n.createdBy} • ` : ''}{n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}
                      </Typography>
                    </Box>
                    <IconButton size="small" color="error" onClick={() => deleteNote(n.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                </Paper>
              ))}
            </Stack>
          )}
        </Paper>
      </TabPanel>

      {/* ── Tab 3: Activity Timeline ── */}
      <TabPanel value={tab} index={3}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>Activity Timeline</Typography>
          {activities.length === 0 ? (
            <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>No activities yet</Typography>
          ) : (
            <Stack spacing={1}>
              {activities.map((a) => (
                <Paper key={a.id} variant="outlined" sx={{ p: 2 }}>
                  <Stack direction="row" justifyContent="space-between">
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Chip label={a.activityType} size="small" color="info" />
                      <Typography variant="body2">{a.description}</Typography>
                    </Stack>
                    <Typography variant="caption" color="text.secondary">
                      {a.createdAt ? new Date(a.createdAt).toLocaleString() : ''}
                    </Typography>
                  </Stack>
                </Paper>
              ))}
            </Stack>
          )}
        </Paper>
      </TabPanel>

      {/* ── Tab 4: Tags ── */}
      <TabPanel value={tab} index={4}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>Tags</Typography>
          <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
            <TextField size="small" label="New tag" value={newTag} onChange={(e) => setNewTag(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') addTag(); }} />
            <Button variant="contained" startIcon={<AddIcon />} onClick={addTag}>Add</Button>
          </Stack>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            {tags.map((t) => (
              <Chip key={t.id} label={t.tagName} onDelete={() => removeTag(t.tagName)} color="primary" variant="outlined" />
            ))}
            {tags.length === 0 && <Typography color="text.secondary">No tags</Typography>}
          </Stack>
        </Paper>
      </TabPanel>

      {/* ── Tab 5: Attachments ── */}
      <TabPanel value={tab} index={5}>
        <Paper sx={{ p: 3 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography variant="h6">Attachments</Typography>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAttachDialog(true)}>Add Attachment</Button>
          </Stack>
          {attachments.length === 0 ? (
            <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>No attachments yet</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>File Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Size</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {attachments.map((a) => (
                  <TableRow key={a.id} hover>
                    <TableCell>
                      <Typography
                        variant="body2"
                        component="a"
                        href={a.fileUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        sx={{ color: 'primary.main', textDecoration: 'none' }}
                      >
                        {a.fileName}
                      </Typography>
                    </TableCell>
                    <TableCell>{a.fileType || '—'}</TableCell>
                    <TableCell>{a.fileSize ? `${(a.fileSize / 1024).toFixed(1)} KB` : '—'}</TableCell>
                    <TableCell>{a.createdAt ? new Date(a.createdAt).toLocaleString() : ''}</TableCell>
                    <TableCell>
                      <IconButton size="small" color="error" onClick={() => deleteAttachment(a.id)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Paper>

        {/* Attachment dialog */}
        <Dialog open={attachDialog} onClose={() => setAttachDialog(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Add Attachment</DialogTitle>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0 }}>
              <Grid item xs={12}>
                <TextField fullWidth label="File Name" value={attachForm.fileName}
                  onChange={(e) => setAttachForm((p) => ({ ...p, fileName: e.target.value }))} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="File URL" value={attachForm.fileUrl}
                  onChange={(e) => setAttachForm((p) => ({ ...p, fileUrl: e.target.value }))} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="File Type (e.g. PDF, DOCX)" value={attachForm.fileType}
                  onChange={(e) => setAttachForm((p) => ({ ...p, fileType: e.target.value }))} />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setAttachDialog(false)}>Cancel</Button>
            <Button variant="contained" onClick={addAttachment}>Save</Button>
          </DialogActions>
        </Dialog>
      </TabPanel>

      {/* ── Tab 6: Social & Consent ── */}
      <TabPanel value={tab} index={6}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" sx={{ mb: 2 }}>Social Profiles</Typography>
              {editing ? (
                <Stack spacing={2}>
                  <TextField fullWidth label="LinkedIn URL" value={formData.linkedinUrl} onChange={handleField('linkedinUrl')} />
                  <TextField fullWidth label="Twitter URL" value={formData.twitterUrl} onChange={handleField('twitterUrl')} />
                  <TextField fullWidth label="Facebook URL" value={formData.facebookUrl} onChange={handleField('facebookUrl')} />
                  <TextField fullWidth label="Other Social URL" value={formData.otherSocialUrl} onChange={handleField('otherSocialUrl')} />
                </Stack>
              ) : (
                <Stack spacing={1.5}>
                  {[
                    { icon: <LinkedIn color="primary" />, label: 'LinkedIn', value: contact.linkedinUrl },
                    { icon: <Twitter sx={{ color: '#1DA1F2' }} />, label: 'Twitter', value: contact.twitterUrl },
                    { icon: <Facebook sx={{ color: '#4267B2' }} />, label: 'Facebook', value: contact.facebookUrl },
                  ].map((s) => (
                    <Stack key={s.label} direction="row" spacing={1} alignItems="center">
                      {s.icon}
                      {s.value ? (
                        <Typography variant="body2" component="a" href={s.value} target="_blank" rel="noopener" sx={{ color: 'primary.main' }}>
                          {s.value}
                        </Typography>
                      ) : (
                        <Typography variant="body2" color="text.secondary">Not set</Typography>
                      )}
                    </Stack>
                  ))}
                  {contact.otherSocialUrl && (
                    <Typography variant="body2">Other: <a href={contact.otherSocialUrl} target="_blank" rel="noopener">{contact.otherSocialUrl}</a></Typography>
                  )}
                </Stack>
              )}
            </Paper>
          </Grid>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" sx={{ mb: 2 }}>Marketing Consent</Typography>
              {editing ? (
                <Stack spacing={1}>
                  <FormControlLabel control={<Switch checked={formData.emailOptIn} onChange={handleField('emailOptIn')} />} label="Email Opt-In" />
                  <FormControlLabel control={<Switch checked={formData.smsOptIn} onChange={handleField('smsOptIn')} />} label="SMS Opt-In" />
                  <FormControlLabel control={<Switch checked={formData.phoneOptIn} onChange={handleField('phoneOptIn')} />} label="Phone Opt-In" />
                  <FormControlLabel control={<Switch checked={formData.doNotCall} onChange={handleField('doNotCall')} />} label="Do Not Call" />
                  <TextField fullWidth label="Consent Source" value={formData.consentSource} onChange={handleField('consentSource')} />
                </Stack>
              ) : (
                <Stack spacing={1}>
                  <Stack direction="row" spacing={1}>
                    <Chip label={`Email: ${contact.emailOptIn ? 'Yes' : 'No'}`} color={contact.emailOptIn ? 'success' : 'default'} size="small" />
                    <Chip label={`SMS: ${contact.smsOptIn ? 'Yes' : 'No'}`} color={contact.smsOptIn ? 'success' : 'default'} size="small" />
                    <Chip label={`Phone: ${contact.phoneOptIn ? 'Yes' : 'No'}`} color={contact.phoneOptIn ? 'success' : 'default'} size="small" />
                    <Chip label={`Do Not Call: ${contact.doNotCall ? 'Yes' : 'No'}`} color={contact.doNotCall ? 'error' : 'default'} size="small" />
                  </Stack>
                  {contact.consentDate && (
                    <Typography variant="body2" color="text.secondary">
                      Last consent update: {new Date(contact.consentDate).toLocaleString()} ({contact.consentSource || 'N/A'})
                    </Typography>
                  )}
                </Stack>
              )}
            </Paper>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ── Tab 7: Analytics ── */}
      <TabPanel value={tab} index={7}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>Contact Analytics (Tenant-Wide)</Typography>
          {analytics ? (
            <Grid container spacing={3}>
              {[
                { label: 'Total Contacts', value: analytics.totalContacts },
                { label: 'With Email', value: analytics.contactsWithEmail },
                { label: 'With Phone', value: analytics.contactsWithPhone },
                { label: 'Linked to Account', value: analytics.contactsWithAccount },
                { label: 'Email Opt-In', value: analytics.emailOptInCount },
                { label: 'SMS Opt-In', value: analytics.smsOptInCount },
                { label: 'Do Not Call', value: analytics.doNotCallCount },
              ].map((kpi) => (
                <Grid item xs={6} sm={3} key={kpi.label}>
                  <Card variant="outlined">
                    <CardContent sx={{ textAlign: 'center' }}>
                      <Typography variant="h4" fontWeight={700}>{kpi.value}</Typography>
                      <Typography variant="caption" color="text.secondary">{kpi.label}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
              {/* Segment breakdown */}
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1 }}>By Segment</Typography>
                {analytics.bySegment && Object.entries(analytics.bySegment).map(([k, v]) => (
                  <Stack key={k} direction="row" justifyContent="space-between" sx={{ py: 0.5 }}>
                    <Typography variant="body2">{k}</Typography>
                    <Chip label={v} size="small" />
                  </Stack>
                ))}
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1 }}>By Lifecycle Stage</Typography>
                {analytics.byLifecycleStage && Object.entries(analytics.byLifecycleStage).map(([k, v]) => (
                  <Stack key={k} direction="row" justifyContent="space-between" sx={{ py: 0.5 }}>
                    <Typography variant="body2">{k}</Typography>
                    <Chip label={v} size="small" />
                  </Stack>
                ))}
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1 }}>By Lead Source</Typography>
                {analytics.byLeadSource && Object.entries(analytics.byLeadSource).map(([k, v]) => (
                  <Stack key={k} direction="row" justifyContent="space-between" sx={{ py: 0.5 }}>
                    <Typography variant="body2">{k}</Typography>
                    <Chip label={v} size="small" />
                  </Stack>
                ))}
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1 }}>By Department</Typography>
                {analytics.byDepartment && Object.entries(analytics.byDepartment).map(([k, v]) => (
                  <Stack key={k} direction="row" justifyContent="space-between" sx={{ py: 0.5 }}>
                    <Typography variant="body2">{k}</Typography>
                    <Chip label={v} size="small" />
                  </Stack>
                ))}
              </Grid>
            </Grid>
          ) : (
            <Typography color="text.secondary">Loading analytics…</Typography>
          )}
        </Paper>
      </TabPanel>
    </>
  );
};

export default ContactDetailPage;
