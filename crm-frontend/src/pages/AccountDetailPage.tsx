/* ============================================================
   AccountDetailPage – Full account detail with tabs
   ============================================================ */
import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Typography, Paper, Grid, Chip, Tabs, Tab, TextField, Button, IconButton,
  Stack, Divider, List, ListItem, ListItemText, ListItemSecondaryAction, Tooltip,
  LinearProgress, Card, CardContent, Dialog, DialogTitle, DialogContent, DialogActions,
  Autocomplete,
} from '@mui/material';
import {
  ArrowBack as BackIcon, Edit as EditIcon, Delete as DeleteIcon,
  NoteAdd as NoteIcon, AttachFile as AttachIcon, Timeline as TimelineIcon,
  LocalOffer as TagIcon, Save as SaveIcon,
} from '@mui/icons-material';
import { PageHeader } from '../components';
import { accountService, contactService } from '../services';
import { Account, AccountNote, AccountAttachment, AccountActivity, AccountTag, Contact } from '../types';
import { useSnackbar } from 'notistack';

interface TabPanelProps { children?: React.ReactNode; index: number; value: number; }
const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => (
  <div role="tabpanel" hidden={value !== index} style={{ paddingTop: 16 }}>
    {value === index && <>{children}</>}
  </div>
);

const AccountDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [account, setAccount] = useState<Account | null>(null);
  const [loading, setLoading] = useState(true);
  const [tabValue, setTabValue] = useState(0);

  // Notes
  const [notes, setNotes] = useState<AccountNote[]>([]);
  const [newNote, setNewNote] = useState('');
  const [noteSaving, setNoteSaving] = useState(false);

  // Attachments
  const [attachments, setAttachments] = useState<AccountAttachment[]>([]);
  const [attachDialog, setAttachDialog] = useState(false);
  const [attachForm, setAttachForm] = useState({ fileName: '', fileUrl: '', fileType: '' });

  // Activities
  const [activities, setActivities] = useState<AccountActivity[]>([]);

  // Tags
  const [tags, setTags] = useState<AccountTag[]>([]);
  const [allTags, setAllTags] = useState<AccountTag[]>([]);
  const [newTagName, setNewTagName] = useState('');
  const [newTagColor, setNewTagColor] = useState('#1976d2');

  // Related contacts
  const [contacts, setContacts] = useState<Contact[]>([]);

  // Edit mode
  const [editing, setEditing] = useState(false);
  const [editData, setEditData] = useState<Partial<Account>>({});

  const fetchAccount = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const res = await accountService.getById(id);
      setAccount(res.data);
      setEditData(res.data);
    } catch {
      enqueueSnackbar('Failed to load account', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [id, enqueueSnackbar]);

  const fetchNotes = useCallback(async () => {
    if (!id) return;
    try { const res = await accountService.getNotes(id); setNotes(res.data ?? []); } catch { /* */ }
  }, [id]);

  const fetchAttachments = useCallback(async () => {
    if (!id) return;
    try { const res = await accountService.getAttachments(id); setAttachments(res.data ?? []); } catch { /* */ }
  }, [id]);

  const fetchActivities = useCallback(async () => {
    if (!id) return;
    try { const res = await accountService.getActivities(id); setActivities(res.data ?? []); } catch { /* */ }
  }, [id]);

  const fetchTags = useCallback(async () => {
    if (!id) return;
    try {
      const [acctTags, all] = await Promise.all([accountService.getAccountTags(id), accountService.getAllTags()]);
      setTags(acctTags.data ?? []);
      setAllTags(all.data ?? []);
    } catch { /* */ }
  }, [id]);

  const fetchContacts = useCallback(async () => {
    if (!id) return;
    try {
      const res = await contactService.getByAccount(id);
      setContacts(Array.isArray(res.data) ? res.data : []);
    } catch { /* */ }
  }, [id]);

  useEffect(() => { fetchAccount(); }, [fetchAccount]);
  useEffect(() => {
    if (tabValue === 1) fetchNotes();
    if (tabValue === 2) fetchActivities();
    if (tabValue === 3) fetchAttachments();
    if (tabValue === 4) fetchTags();
    if (tabValue === 5) fetchContacts();
  }, [tabValue, fetchNotes, fetchActivities, fetchAttachments, fetchTags, fetchContacts]);

  // ── Handlers ────────────────────────────────────────────
  const handleSave = async () => {
    if (!id) return;
    try {
      const res = await accountService.update(id, editData as any);
      setAccount(res.data);
      setEditing(false);
      enqueueSnackbar('Account updated', { variant: 'success' });
    } catch {
      enqueueSnackbar('Update failed', { variant: 'error' });
    }
  };

  const handleAddNote = async () => {
    if (!id || !newNote.trim()) return;
    setNoteSaving(true);
    try {
      await accountService.addNote(id, newNote);
      setNewNote('');
      fetchNotes();
      enqueueSnackbar('Note added', { variant: 'success' });
    } catch {
      enqueueSnackbar('Failed to add note', { variant: 'error' });
    }
    setNoteSaving(false);
  };

  const handleDeleteNote = async (noteId: string) => {
    try { await accountService.deleteNote(noteId); fetchNotes(); } catch { enqueueSnackbar('Delete failed', { variant: 'error' }); }
  };

  const handleAddAttachment = async () => {
    if (!id || !attachForm.fileName || !attachForm.fileUrl) return;
    try {
      await accountService.addAttachment(id, attachForm);
      setAttachDialog(false);
      setAttachForm({ fileName: '', fileUrl: '', fileType: '' });
      fetchAttachments();
      enqueueSnackbar('Attachment added', { variant: 'success' });
    } catch {
      enqueueSnackbar('Failed', { variant: 'error' });
    }
  };

  const handleDeleteAttachment = async (attId: string) => {
    try { await accountService.deleteAttachment(attId); fetchAttachments(); } catch { enqueueSnackbar('Delete failed', { variant: 'error' }); }
  };

  const handleCreateAndAddTag = async () => {
    if (!id || !newTagName.trim()) return;
    try {
      const tagRes = await accountService.createTag(newTagName, newTagColor);
      if (tagRes.data) await accountService.addTagToAccount(id, tagRes.data.id);
      setNewTagName('');
      fetchTags();
      enqueueSnackbar('Tag added', { variant: 'success' });
    } catch {
      enqueueSnackbar('Failed', { variant: 'error' });
    }
  };

  const handleAddExistingTag = async (tagId: string) => {
    if (!id) return;
    try { await accountService.addTagToAccount(id, tagId); fetchTags(); } catch { enqueueSnackbar('Failed', { variant: 'error' }); }
  };

  const handleRemoveTag = async (tagId: string) => {
    if (!id) return;
    try { await accountService.removeTagFromAccount(id, tagId); fetchTags(); } catch { enqueueSnackbar('Failed', { variant: 'error' }); }
  };

  if (loading) return <LinearProgress />;
  if (!account) return <Typography>Account not found</Typography>;

  const healthColor = (account.healthScore ?? 50) >= 70 ? 'success' : (account.healthScore ?? 50) >= 40 ? 'warning' : 'error';
  const engagementColor = (account.engagementScore ?? 0) >= 70 ? 'success' : (account.engagementScore ?? 0) >= 40 ? 'warning' : 'error';

  return (
    <>
      <PageHeader
        title={account.name}
        breadcrumbs={[
          { label: 'Dashboard', to: '/dashboard' },
          { label: 'Accounts', to: '/accounts' },
          { label: account.name },
        ]}
      />

      <Box sx={{ mb: 2, display: 'flex', gap: 1 }}>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/accounts')} size="small">Back</Button>
        {!editing ? (
          <Button startIcon={<EditIcon />} variant="contained" size="small" onClick={() => setEditing(true)}>Edit</Button>
        ) : (
          <Button startIcon={<SaveIcon />} variant="contained" color="success" size="small" onClick={handleSave}>Save</Button>
        )}
      </Box>

      <Paper sx={{ mb: 2 }}>
        <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)} variant="scrollable" scrollButtons="auto">
          <Tab label="Overview" />
          <Tab label="Notes" />
          <Tab label="Timeline" />
          <Tab label="Attachments" />
          <Tab label="Tags" />
          <Tab label="Related" />
        </Tabs>
      </Paper>

      {/* ── Overview Tab ──────────────────────────────────── */}
      <TabPanel value={tabValue} index={0}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={8}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>Account Details</Typography>
              <Grid container spacing={2}>
                {([
                  ['Name', 'name'], ['Industry', 'industry'], ['Phone', 'phone'],
                  ['Website', 'website'], ['Type', 'type'], ['Territory', 'territory'],
                  ['Lifecycle Stage', 'lifecycleStage'], ['Segment', 'segment'],
                  ['Owner ID', 'ownerId'], ['Billing Address', 'billingAddress'],
                  ['Shipping Address', 'shippingAddress'], ['Description', 'description'],
                ] as [string, keyof Account][]).map(([label, field]) => (
                  <Grid item xs={12} sm={6} key={field}>
                    {editing ? (
                      <TextField fullWidth label={label} size="small"
                        value={(editData as any)[field] ?? ''}
                        onChange={(e) => setEditData((p) => ({ ...p, [field]: e.target.value }))} />
                    ) : (
                      <Box>
                        <Typography variant="caption" color="text.secondary">{label}</Typography>
                        <Typography>{(account as any)[field] ?? '—'}</Typography>
                      </Box>
                    )}
                  </Grid>
                ))}
                <Grid item xs={6} sm={3}>
                  {editing ? (
                    <TextField fullWidth label="Revenue" type="number" size="small"
                      value={editData.annualRevenue ?? ''}
                      onChange={(e) => setEditData((p) => ({ ...p, annualRevenue: Number(e.target.value) }))} />
                  ) : (
                    <Box>
                      <Typography variant="caption" color="text.secondary">Revenue</Typography>
                      <Typography>${account.annualRevenue?.toLocaleString() ?? '—'}</Typography>
                    </Box>
                  )}
                </Grid>
                <Grid item xs={6} sm={3}>
                  {editing ? (
                    <TextField fullWidth label="Employees" type="number" size="small"
                      value={editData.numberOfEmployees ?? ''}
                      onChange={(e) => setEditData((p) => ({ ...p, numberOfEmployees: Number(e.target.value) }))} />
                  ) : (
                    <Box>
                      <Typography variant="caption" color="text.secondary">Employees</Typography>
                      <Typography>{account.numberOfEmployees ?? '—'}</Typography>
                    </Box>
                  )}
                </Grid>
              </Grid>
            </Paper>
          </Grid>

          <Grid item xs={12} md={4}>
            <Stack spacing={2}>
              {/* Health Score */}
              <Card>
                <CardContent>
                  <Typography variant="subtitle2" color="text.secondary">Health Score</Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                    <Box sx={{ flexGrow: 1 }}>
                      <LinearProgress variant="determinate" value={account.healthScore ?? 50} color={healthColor as any} sx={{ height: 10, borderRadius: 5 }} />
                    </Box>
                    <Typography variant="h6">{account.healthScore ?? 50}</Typography>
                  </Box>
                </CardContent>
              </Card>

              {/* Engagement Score */}
              <Card>
                <CardContent>
                  <Typography variant="subtitle2" color="text.secondary">Engagement Score</Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                    <Box sx={{ flexGrow: 1 }}>
                      <LinearProgress variant="determinate" value={account.engagementScore ?? 0} color={engagementColor as any} sx={{ height: 10, borderRadius: 5 }} />
                    </Box>
                    <Typography variant="h6">{account.engagementScore ?? 0}</Typography>
                  </Box>
                </CardContent>
              </Card>

              {/* Quick Info */}
              <Card>
                <CardContent>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>Quick Info</Typography>
                  <Stack spacing={0.5}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2">Type</Typography>
                      <Chip label={account.type ?? 'PROSPECT'} size="small" color="primary" variant="outlined" />
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2">Stage</Typography>
                      <Chip label={account.lifecycleStage ?? 'NEW'} size="small" color="info" variant="outlined" />
                    </Box>
                    {account.tags && account.tags.length > 0 && (
                      <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 1 }}>
                        {account.tags.map((t) => <Chip key={t.id} label={t.name} size="small" sx={{ bgcolor: t.color, color: '#fff' }} />)}
                      </Box>
                    )}
                  </Stack>
                </CardContent>
              </Card>
            </Stack>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ── Notes Tab ─────────────────────────────────────── */}
      <TabPanel value={tabValue} index={1}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>Notes</Typography>
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <TextField fullWidth multiline rows={2} placeholder="Add a note..." value={newNote}
              onChange={(e) => setNewNote(e.target.value)} size="small" />
            <Button variant="contained" onClick={handleAddNote} disabled={noteSaving || !newNote.trim()}
              startIcon={<NoteIcon />} sx={{ alignSelf: 'flex-end' }}>Add</Button>
          </Box>
          <Divider sx={{ mb: 2 }} />
          <List>
            {notes.map((note) => (
              <ListItem key={note.id} divider>
                <ListItemText
                  primary={note.content}
                  secondary={`${note.createdBy ?? 'System'} · ${new Date(note.createdAt).toLocaleString()}`}
                />
                <ListItemSecondaryAction>
                  <IconButton edge="end" size="small" onClick={() => handleDeleteNote(note.id)}><DeleteIcon fontSize="small" /></IconButton>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
            {notes.length === 0 && <Typography color="text.secondary" sx={{ py: 2 }}>No notes yet</Typography>}
          </List>
        </Paper>
      </TabPanel>

      {/* ── Timeline Tab ──────────────────────────────────── */}
      <TabPanel value={tabValue} index={2}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>Activity Timeline</Typography>
          <List>
            {activities.map((act) => (
              <ListItem key={act.id} divider>
                <ListItemText
                  primary={<><Chip label={act.type} size="small" sx={{ mr: 1 }} />{act.description}</>}
                  secondary={`${act.performedBy ?? 'System'} · ${new Date(act.createdAt).toLocaleString()}`}
                />
              </ListItem>
            ))}
            {activities.length === 0 && <Typography color="text.secondary" sx={{ py: 2 }}>No activities yet</Typography>}
          </List>
        </Paper>
      </TabPanel>

      {/* ── Attachments Tab ───────────────────────────────── */}
      <TabPanel value={tabValue} index={3}>
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="h6">Attachments</Typography>
            <Button variant="contained" size="small" startIcon={<AttachIcon />} onClick={() => setAttachDialog(true)}>Add</Button>
          </Box>
          <List>
            {attachments.map((att) => (
              <ListItem key={att.id} divider>
                <ListItemText
                  primary={att.fileName}
                  secondary={`${att.fileType ?? ''} · ${att.fileSize ? (att.fileSize / 1024).toFixed(1) + ' KB' : ''} · ${new Date(att.createdAt).toLocaleString()}`}
                />
                <ListItemSecondaryAction>
                  <IconButton edge="end" size="small" onClick={() => handleDeleteAttachment(att.id)}><DeleteIcon fontSize="small" /></IconButton>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
            {attachments.length === 0 && <Typography color="text.secondary" sx={{ py: 2 }}>No attachments</Typography>}
          </List>
        </Paper>

        <Dialog open={attachDialog} onClose={() => setAttachDialog(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Add Attachment</DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ mt: 1 }}>
              <TextField fullWidth label="File Name" value={attachForm.fileName}
                onChange={(e) => setAttachForm((p) => ({ ...p, fileName: e.target.value }))} />
              <TextField fullWidth label="File URL" value={attachForm.fileUrl}
                onChange={(e) => setAttachForm((p) => ({ ...p, fileUrl: e.target.value }))} />
              <TextField fullWidth label="File Type" value={attachForm.fileType}
                onChange={(e) => setAttachForm((p) => ({ ...p, fileType: e.target.value }))} />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setAttachDialog(false)}>Cancel</Button>
            <Button variant="contained" onClick={handleAddAttachment}>Add</Button>
          </DialogActions>
        </Dialog>
      </TabPanel>

      {/* ── Tags Tab ──────────────────────────────────────── */}
      <TabPanel value={tabValue} index={4}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>Tags</Typography>
          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mb: 2 }}>
            {tags.map((tag) => (
              <Chip key={tag.id} label={tag.name} onDelete={() => handleRemoveTag(tag.id)}
                sx={{ bgcolor: tag.color, color: '#fff' }} />
            ))}
            {tags.length === 0 && <Typography color="text.secondary">No tags</Typography>}
          </Box>
          <Divider sx={{ mb: 2 }} />

          <Typography variant="subtitle2" gutterBottom>Add Existing Tag</Typography>
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <Autocomplete
              options={allTags.filter((t) => !tags.find((at) => at.id === t.id))}
              getOptionLabel={(o) => o.name}
              onChange={(_, v) => v && handleAddExistingTag(v.id)}
              renderInput={(params) => <TextField {...params} size="small" label="Select tag" />}
              sx={{ minWidth: 250 }}
            />
          </Box>

          <Typography variant="subtitle2" gutterBottom>Create New Tag</Typography>
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <TextField size="small" label="Tag name" value={newTagName} onChange={(e) => setNewTagName(e.target.value)} />
            <TextField size="small" type="color" value={newTagColor} onChange={(e) => setNewTagColor(e.target.value)} sx={{ width: 60 }} />
            <Button variant="outlined" size="small" onClick={handleCreateAndAddTag} disabled={!newTagName.trim()}>Create & Add</Button>
          </Box>
        </Paper>
      </TabPanel>

      {/* ── Related Tab (Contacts) ────────────────────────── */}
      <TabPanel value={tabValue} index={5}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>Related Contacts</Typography>
          <List>
            {contacts.map((c) => (
              <ListItem key={c.id} divider>
                <ListItemText
                  primary={`${c.firstName} ${c.lastName}`}
                  secondary={`${c.email} · ${c.phone ?? ''} · ${c.title ?? ''}`}
                />
              </ListItem>
            ))}
            {contacts.length === 0 && <Typography color="text.secondary" sx={{ py: 2 }}>No related contacts</Typography>}
          </List>
        </Paper>
      </TabPanel>
    </>
  );
};

export default AccountDetailPage;
