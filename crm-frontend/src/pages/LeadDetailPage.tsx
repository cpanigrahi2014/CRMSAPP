/* ============================================================
   LeadDetailPage – full lead detail with tabs for
   Overview, Notes, Activities/Timeline, Attachments, Tags
   ============================================================ */
import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Typography, Paper, Grid, Chip, Tabs, Tab, Button, TextField, IconButton,
  Tooltip, Stack, Divider, CircularProgress, LinearProgress, Card, CardContent,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Delete as DeleteIcon,
  AttachFile as AttachIcon,
  Upload as UploadIcon,
  Download as DownloadIcon,
  Add as AddIcon,
  Refresh as RefreshIcon,
  AutoAwesome as AiIcon,
} from '@mui/icons-material';
import { PageHeader, StatusChip, VoiceInput } from '../components';
import { leadService } from '../services';
import { aiInsightsService, GeneratedNBA } from '../services/aiInsightsService';
import { Lead, LeadNote, LeadActivity, LeadAttachment, LeadTag } from '../types';
import { useSnackbar } from 'notistack';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}
const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => (
  <div role="tabpanel" hidden={value !== index}>{value === index && <Box sx={{ py: 2 }}>{children}</Box>}</div>
);

const LeadDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [lead, setLead] = useState<Lead | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState(0);

  // Notes
  const [notes, setNotes] = useState<LeadNote[]>([]);
  const [newNote, setNewNote] = useState('');
  const [notesLoading, setNotesLoading] = useState(false);

  // Activities
  const [activities, setActivities] = useState<LeadActivity[]>([]);
  const [activitiesLoading, setActivitiesLoading] = useState(false);

  // Attachments
  const [attachments, setAttachments] = useState<LeadAttachment[]>([]);
  const [attachmentsLoading, setAttachmentsLoading] = useState(false);

  // Tags
  const [allTags, setAllTags] = useState<LeadTag[]>([]);
  const [leadTags, setLeadTags] = useState<LeadTag[]>([]);

  // AI Next Best Actions
  const [nbaActions, setNbaActions] = useState<GeneratedNBA[]>([]);
  const [nbaLoading, setNbaLoading] = useState(false);

  /* ── Fetch lead ───────────────────────────────────────── */
  const fetchLead = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const res = await leadService.getById(id);
      setLead(res.data);
      setLeadTags(res.data.tags ?? []);
    } catch {
      enqueueSnackbar('Failed to load lead', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [id, enqueueSnackbar]);

  const fetchNotes = useCallback(async () => {
    if (!id) return;
    setNotesLoading(true);
    try {
      const res = await leadService.getNotes(id, 0, 100);
      setNotes(Array.isArray(res.data) ? res.data : (res.data as any)?.content ?? []);
    } catch { /* ignore */ } finally { setNotesLoading(false); }
  }, [id]);

  const fetchActivities = useCallback(async () => {
    if (!id) return;
    setActivitiesLoading(true);
    try {
      const res = await leadService.getActivities(id, undefined, 0, 100);
      setActivities(Array.isArray(res.data) ? res.data : (res.data as any)?.content ?? []);
    } catch { /* ignore */ } finally { setActivitiesLoading(false); }
  }, [id]);

  const fetchAttachments = useCallback(async () => {
    if (!id) return;
    setAttachmentsLoading(true);
    try {
      const res = await leadService.getAttachments(id);
      setAttachments(Array.isArray(res.data) ? res.data : []);
    } catch { /* ignore */ } finally { setAttachmentsLoading(false); }
  }, [id]);

  const fetchAllTags = useCallback(async () => {
    try {
      const res = await leadService.getAllTags();
      setAllTags(Array.isArray(res.data) ? res.data : []);
    } catch { /* ignore */ }
  }, []);

  const fetchNba = useCallback(async () => {
    if (!id) return;
    setNbaLoading(true);
    try {
      const res = await aiInsightsService.generateNextBestActions('LEAD', id, {
        name: lead?.firstName ? `${lead.firstName} ${lead.lastName}` : '',
        score: lead?.leadScore ?? 0,
        status: lead?.status ?? '',
      });
      setNbaActions(res.actions ?? []);
    } catch { /* ignore */ } finally { setNbaLoading(false); }
  }, [id, lead]);

  useEffect(() => { fetchLead(); }, [fetchLead]);
  useEffect(() => {
    if (tab === 1) fetchNotes();
    if (tab === 2) fetchActivities();
    if (tab === 3) fetchAttachments();
    if (tab === 4) fetchAllTags();
  }, [tab, fetchNotes, fetchActivities, fetchAttachments, fetchAllTags]);

  /* ── Handlers ─────────────────────────────────────────── */
  const handleAddNote = async () => {
    if (!id || !newNote.trim()) return;
    try {
      await leadService.addNote(id, newNote);
      enqueueSnackbar('Note added', { variant: 'success' });
      setNewNote('');
      fetchNotes();
    } catch { enqueueSnackbar('Failed to add note', { variant: 'error' }); }
  };

  const handleDeleteNote = async (noteId: string) => {
    try {
      await leadService.deleteNote(noteId);
      enqueueSnackbar('Note deleted', { variant: 'success' });
      fetchNotes();
    } catch { enqueueSnackbar('Failed to delete note', { variant: 'error' }); }
  };

  const handleUploadAttachment = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!id || !e.target.files?.[0]) return;
    try {
      await leadService.addAttachment(id, e.target.files[0]);
      enqueueSnackbar('Attachment uploaded', { variant: 'success' });
      fetchAttachments();
    } catch { enqueueSnackbar('Upload failed', { variant: 'error' }); }
  };

  const handleDownloadAttachment = async (att: LeadAttachment) => {
    try {
      const data = await leadService.downloadAttachment(att.id);
      const url = window.URL.createObjectURL(new Blob([data]));
      const a = document.createElement('a');
      a.href = url; a.download = att.fileName; document.body.appendChild(a); a.click(); a.remove();
      window.URL.revokeObjectURL(url);
    } catch { enqueueSnackbar('Download failed', { variant: 'error' }); }
  };

  const handleDeleteAttachment = async (attId: string) => {
    try {
      await leadService.deleteAttachment(attId);
      enqueueSnackbar('Attachment deleted', { variant: 'success' });
      fetchAttachments();
    } catch { enqueueSnackbar('Delete failed', { variant: 'error' }); }
  };

  const handleAddTag = async (tagId: string) => {
    if (!id) return;
    try {
      await leadService.addTagToLead(id, tagId);
      enqueueSnackbar('Tag added', { variant: 'success' });
      fetchLead();
    } catch { enqueueSnackbar('Failed to add tag', { variant: 'error' }); }
  };

  const handleRemoveTag = async (tagId: string) => {
    if (!id) return;
    try {
      await leadService.removeTagFromLead(id, tagId);
      enqueueSnackbar('Tag removed', { variant: 'success' });
      fetchLead();
    } catch { enqueueSnackbar('Failed to remove tag', { variant: 'error' }); }
  };

  const handleRecalculateScore = async () => {
    if (!id) return;
    try {
      const res = await leadService.recalculateScore(id);
      setLead(res.data);
      enqueueSnackbar('Score recalculated', { variant: 'success' });
    } catch { enqueueSnackbar('Failed to recalculate', { variant: 'error' }); }
  };

  if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}><CircularProgress /></Box>;
  if (!lead) return <Typography sx={{ mt: 4, textAlign: 'center' }}>Lead not found</Typography>;

  const fullName = `${lead.firstName} ${lead.lastName}`;

  return (
    <>
      <PageHeader
        title={fullName}
        breadcrumbs={[
          { label: 'Dashboard', to: '/dashboard' },
          { label: 'Leads', to: '/leads' },
          { label: fullName },
        ]}
      />

      {/* Back button + quick actions */}
      <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/leads')}>Back</Button>
        <Button startIcon={<RefreshIcon />} onClick={fetchLead}>Refresh</Button>
        <Button onClick={handleRecalculateScore}>Recalculate Score</Button>
      </Stack>

      {/* Overview card */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Status</Typography>
            <Box><StatusChip status={lead.status} /></Box>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Source</Typography>
            <Box><StatusChip status={lead.source} /></Box>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Lead Score</Typography>
            <Typography variant="h6">{lead.leadScore}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Converted</Typography>
            <Typography variant="h6">{lead.converted ? 'Yes' : 'No'}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Email</Typography>
            <Typography>{lead.email || '—'}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Phone</Typography>
            <Typography>{lead.phone || '—'}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Company</Typography>
            <Typography>{lead.company || '—'}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Title</Typography>
            <Typography>{lead.title || '—'}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Territory</Typography>
            <Typography>{lead.territory || '—'}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">SLA Due</Typography>
            <Typography>{lead.slaDueDate ? new Date(lead.slaDueDate).toLocaleString() : '—'}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Created</Typography>
            <Typography>{new Date(lead.createdAt).toLocaleString()}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">Updated</Typography>
            <Typography>{new Date(lead.updatedAt).toLocaleString()}</Typography>
          </Grid>
          {lead.description && (
            <Grid item xs={12}>
              <Typography variant="caption" color="text.secondary">Description</Typography>
              <Typography>{lead.description}</Typography>
            </Grid>
          )}
          {leadTags.length > 0 && (
            <Grid item xs={12}>
              <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5 }}>Tags</Typography>
              <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5 }}>
                {leadTags.map(t => (
                  <Chip
                    key={t.id}
                    label={t.name}
                    size="small"
                    sx={{ bgcolor: t.color, color: '#fff' }}
                    onDelete={() => handleRemoveTag(t.id)}
                  />
                ))}
              </Stack>
            </Grid>
          )}
        </Grid>
      </Paper>

      {/* Tabs */}
      <Paper>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto">
          <Tab label="Overview" />
          <Tab label="Notes" />
          <Tab label="Timeline" />
          <Tab label="Attachments" />
          <Tab label="Tags" />
        </Tabs>

        {/* Tab 0: Overview – AI Next Best Actions */}
        <TabPanel value={tab} index={0}>
          <Box sx={{ px: 3, pb: 2 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
              <Stack direction="row" spacing={1} alignItems="center">
                <AiIcon color="primary" />
                <Typography variant="h6">AI Recommended Actions</Typography>
              </Stack>
              <Button size="small" startIcon={<AiIcon />} onClick={fetchNba} disabled={nbaLoading}>
                {nbaLoading ? 'Generating…' : 'Get Recommendations'}
              </Button>
            </Stack>
            {nbaLoading && <LinearProgress sx={{ mb: 1 }} />}
            {nbaActions.length > 0 ? nbaActions.map((a, i) => (
              <Card key={i} variant="outlined" sx={{ mb: 1 }}>
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                    <Chip
                      label={a.priority >= 80 ? 'High' : a.priority >= 50 ? 'Medium' : 'Low'}
                      size="small"
                      color={a.priority >= 80 ? 'error' : a.priority >= 50 ? 'warning' : 'success'}
                    />
                    <Typography variant="body2" fontWeight={600}>{a.action}</Typography>
                  </Stack>
                  <Typography variant="body2" color="text.secondary">{a.reason}</Typography>
                </CardContent>
              </Card>
            )) : !nbaLoading && (
              <Typography variant="body2" color="text.secondary">
                Click "Get Recommendations" to have AI suggest the next best actions for this lead.
              </Typography>
            )}
          </Box>
        </TabPanel>

        {/* Tab 1: Notes */}
        <TabPanel value={tab} index={1}>
          <Box sx={{ px: 3 }}>
            <Stack direction="row" spacing={1} sx={{ mb: 2 }} alignItems="flex-start">
              <TextField
                fullWidth size="small" multiline maxRows={4}
                placeholder="Add a note..."
                value={newNote} onChange={e => setNewNote(e.target.value)}
              />
              <VoiceInput onTranscript={(t) => setNewNote(prev => prev ? prev + ' ' + t : t)} tooltip="Dictate note" />
              <Button variant="contained" onClick={handleAddNote} disabled={!newNote.trim()}>Add</Button>
            </Stack>
            {notesLoading && <LinearProgress sx={{ mb: 1 }} />}
            {notes.map(n => (
              <Card key={n.id} variant="outlined" sx={{ mb: 1 }}>
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                    <Box>
                      <Typography variant="body2">{n.content}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {n.createdBy} · {new Date(n.createdAt).toLocaleString()}
                      </Typography>
                    </Box>
                    <IconButton size="small" color="error" onClick={() => handleDeleteNote(n.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                </CardContent>
              </Card>
            ))}
            {!notesLoading && notes.length === 0 && (
              <Typography color="text.secondary" sx={{ mt: 1 }}>No notes yet.</Typography>
            )}
          </Box>
        </TabPanel>

        {/* Tab 2: Timeline / Activities */}
        <TabPanel value={tab} index={2}>
          <Box sx={{ px: 3 }}>
            {activitiesLoading && <LinearProgress sx={{ mb: 1 }} />}
            {activities.map(a => (
              <Card key={a.id} variant="outlined" sx={{ mb: 1 }}>
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Chip label={a.activityType} size="small" color="default" />
                    <Typography variant="body2" fontWeight={600}>{a.title}</Typography>
                  </Stack>
                  {a.description && <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>{a.description}</Typography>}
                  <Typography variant="caption" color="text.secondary">
                    {a.createdBy} · {new Date(a.createdAt).toLocaleString()}
                  </Typography>
                </CardContent>
              </Card>
            ))}
            {!activitiesLoading && activities.length === 0 && (
              <Typography color="text.secondary" sx={{ mt: 1 }}>No activities yet.</Typography>
            )}
          </Box>
        </TabPanel>

        {/* Tab 3: Attachments */}
        <TabPanel value={tab} index={3}>
          <Box sx={{ px: 3 }}>
            <Button variant="outlined" component="label" startIcon={<UploadIcon />} sx={{ mb: 2 }}>
              Upload File
              <input type="file" hidden onChange={handleUploadAttachment} />
            </Button>
            {attachmentsLoading && <LinearProgress sx={{ mb: 1 }} />}
            {attachments.map(att => (
              <Card key={att.id} variant="outlined" sx={{ mb: 1 }}>
                <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Box>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <AttachIcon fontSize="small" />
                        <Typography variant="body2">{att.fileName}</Typography>
                        <Chip label={formatBytes(att.fileSize)} size="small" variant="outlined" />
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {att.createdBy} · {new Date(att.createdAt).toLocaleString()}
                      </Typography>
                    </Box>
                    <Stack direction="row" spacing={0.5}>
                      <Tooltip title="Download">
                        <IconButton size="small" onClick={() => handleDownloadAttachment(att)}>
                          <DownloadIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton size="small" color="error" onClick={() => handleDeleteAttachment(att.id)}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                  </Stack>
                </CardContent>
              </Card>
            ))}
            {!attachmentsLoading && attachments.length === 0 && (
              <Typography color="text.secondary" sx={{ mt: 1 }}>No attachments yet.</Typography>
            )}
          </Box>
        </TabPanel>

        {/* Tab 4: Tags */}
        <TabPanel value={tab} index={4}>
          <Box sx={{ px: 3 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Current Tags</Typography>
            <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mb: 2 }}>
              {leadTags.length === 0 && <Typography variant="body2" color="text.secondary">No tags assigned</Typography>}
              {leadTags.map(t => (
                <Chip
                  key={t.id} label={t.name} size="small"
                  sx={{ bgcolor: t.color, color: '#fff' }}
                  onDelete={() => handleRemoveTag(t.id)}
                />
              ))}
            </Stack>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Available Tags (click to add)</Typography>
            <Stack direction="row" spacing={0.5} flexWrap="wrap">
              {allTags
                .filter(t => !leadTags.some(lt => lt.id === t.id))
                .map(t => (
                  <Chip
                    key={t.id} label={t.name} size="small" variant="outlined"
                    sx={{ borderColor: t.color, color: t.color, cursor: 'pointer' }}
                    onClick={() => handleAddTag(t.id)}
                    icon={<AddIcon />}
                  />
                ))}
              {allTags.filter(t => !leadTags.some(lt => lt.id === t.id)).length === 0 && (
                <Typography variant="body2" color="text.secondary">No more tags to add. Create tags in lead settings.</Typography>
              )}
            </Stack>
          </Box>
        </TabPanel>
      </Paper>
    </>
  );
};

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

export default LeadDetailPage;
