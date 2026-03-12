/* ============================================================
   OpportunityDetailPage – Multi-tab detail view
   Tabs: Details | Products | Competitors | Timeline | Collaboration | Reminders | Analytics
   ============================================================ */
import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Typography, Grid, Paper, Chip, Button, TextField, Stack, Avatar,
  Divider, IconButton, Tooltip, Tab, Tabs, Dialog, DialogTitle, DialogContent,
  DialogActions, Table, TableHead, TableRow, TableCell, TableBody, Card,
  CardContent, LinearProgress, MenuItem, List, ListItem, ListItemText,
  ListItemIcon, ListItemSecondaryAction, Checkbox,
} from '@mui/material';
import {
  ArrowBack, Edit as EditIcon, Save as SaveIcon, Cancel as CancelIcon,
  Add as AddIcon, Delete as DeleteIcon, TrendingUp, Warning as WarningIcon,
  CheckCircle, Schedule, Person, Note as NoteIcon, EmojiEvents,
  ShowChart, PieChart as PieChartIcon, AttachMoney, Speed, CalendarToday,
  Insights, Assessment, TrackChanges,
} from '@mui/icons-material';
import { PageHeader, ConfirmDialog, StatusChip, MetricCard } from '../components';
import { opportunityService } from '../services';
import type {
  Opportunity, OpportunityProduct, OpportunityCompetitor, OpportunityActivity,
  OpportunityCollaborator, OpportunityNote, OpportunityReminder, OpportunityStage,
  ForecastSummary, RevenueAnalytics, WinLossAnalysis, OpportunityAlert,
  CreateProductRequest, CreateCompetitorRequest, CreateNoteRequest, CreateReminderRequest,
} from '../types';
import { useSnackbar } from 'notistack';

/* ---- helpers ---- */
const TabPanel: React.FC<{ value: number; index: number; children: React.ReactNode }> = ({ value, index, children }) => (
  <Box role="tabpanel" hidden={value !== index} sx={{ pt: 2 }}>{value === index && children}</Box>
);

const fmt$ = (v: number | null | undefined) => v != null ? `$${Number(v).toLocaleString()}` : '-';
const fmtDate = (d: string | null | undefined) => d ? new Date(d).toLocaleDateString() : '-';
const fmtDateTime = (d: string | null | undefined) => d ? new Date(d).toLocaleString() : '-';

const STAGES: OpportunityStage[] = ['PROSPECTING', 'QUALIFICATION', 'NEEDS_ANALYSIS', 'PROPOSAL', 'NEGOTIATION', 'CLOSED_WON', 'CLOSED_LOST'];
const LEAD_SOURCES = ['WEB', 'PHONE', 'EMAIL', 'REFERRAL', 'SOCIAL_MEDIA', 'TRADE_SHOW', 'EVENT', 'PARTNER', 'OTHER'];
const THREAT_LEVELS = ['LOW', 'MEDIUM', 'HIGH'];
const REMINDER_TYPES = ['FOLLOW_UP', 'MEETING', 'DEADLINE', 'REVIEW', 'OTHER'];

const OpportunityDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  /* ---- state ---- */
  const [opp, setOpp] = useState<Opportunity | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState(0);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState<Record<string, any>>({});

  // Sub-resources
  const [products, setProducts] = useState<OpportunityProduct[]>([]);
  const [competitors, setCompetitors] = useState<OpportunityCompetitor[]>([]);
  const [activities, setActivities] = useState<OpportunityActivity[]>([]);
  const [collaborators, setCollaborators] = useState<OpportunityCollaborator[]>([]);
  const [notes, setNotes] = useState<OpportunityNote[]>([]);
  const [reminders, setReminders] = useState<OpportunityReminder[]>([]);
  const [forecast, setForecast] = useState<ForecastSummary | null>(null);
  const [analytics, setAnalytics] = useState<RevenueAnalytics | null>(null);
  const [winLoss, setWinLoss] = useState<WinLossAnalysis | null>(null);
  const [alerts, setAlerts] = useState<OpportunityAlert[]>([]);

  // Dialogs
  const [productDialog, setProductDialog] = useState(false);
  const [productForm, setProductForm] = useState<CreateProductRequest>({ productName: '', quantity: 1, unitPrice: 0, discount: 0 });
  const [competitorDialog, setCompetitorDialog] = useState(false);
  const [competitorForm, setCompetitorForm] = useState<CreateCompetitorRequest>({ competitorName: '', threatLevel: 'MEDIUM' });
  const [noteDialog, setNoteDialog] = useState(false);
  const [noteForm, setNoteForm] = useState<CreateNoteRequest>({ content: '', isPinned: false });
  const [reminderDialog, setReminderDialog] = useState(false);
  const [reminderForm, setReminderForm] = useState<CreateReminderRequest>({ reminderType: 'FOLLOW_UP', message: '', remindAt: '' });
  const [collabDialog, setCollabDialog] = useState(false);
  const [collabForm, setCollabForm] = useState({ userId: '', role: 'MEMBER' });
  const [deleteConfirm, setDeleteConfirm] = useState<{ open: boolean; type: string; id: string }>({ open: false, type: '', id: '' });

  /* ── Loaders ── */
  const fetchOpp = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const res = await opportunityService.getById(id);
      setOpp(res.data);
    } catch { enqueueSnackbar('Failed to load opportunity', { variant: 'error' }); }
    finally { setLoading(false); }
  }, [id, enqueueSnackbar]);

  const fetchProducts = useCallback(async () => {
    if (!id) return;
    try {
      const res = await opportunityService.getProducts(id);
      setProducts(Array.isArray(res.data) ? res.data : (res.data as any)?.content ?? []);
    } catch { /* silent */ }
  }, [id]);

  const fetchCompetitors = useCallback(async () => {
    if (!id) return;
    try {
      const res = await opportunityService.getCompetitors(id);
      setCompetitors(Array.isArray(res.data) ? res.data : (res.data as any)?.content ?? []);
    } catch { /* silent */ }
  }, [id]);

  const fetchActivities = useCallback(async () => {
    if (!id) return;
    try {
      const res = await opportunityService.getActivities(id);
      setActivities(Array.isArray(res.data) ? res.data : (res.data as any)?.content ?? []);
    } catch { /* silent */ }
  }, [id]);

  const fetchCollaborators = useCallback(async () => {
    if (!id) return;
    try {
      const res = await opportunityService.getCollaborators(id);
      setCollaborators(Array.isArray(res.data) ? res.data : (res.data as any) ?? []);
    } catch { /* silent */ }
  }, [id]);

  const fetchNotes = useCallback(async () => {
    if (!id) return;
    try {
      const res = await opportunityService.getNotes(id);
      setNotes(Array.isArray(res.data) ? res.data : (res.data as any)?.content ?? []);
    } catch { /* silent */ }
  }, [id]);

  const fetchReminders = useCallback(async () => {
    if (!id) return;
    try {
      const res = await opportunityService.getReminders(id);
      setReminders(Array.isArray(res.data) ? res.data : (res.data as any)?.content ?? []);
    } catch { /* silent */ }
  }, [id]);

  const fetchAnalytics = useCallback(async () => {
    try {
      const [fRes, aRes, wRes, alRes] = await Promise.all([
        opportunityService.getForecast(),
        opportunityService.getRevenueAnalytics(),
        opportunityService.getWinLossAnalysis(),
        opportunityService.getAlerts(),
      ]);
      setForecast(fRes.data);
      setAnalytics(aRes.data);
      setWinLoss(wRes.data);
      setAlerts(Array.isArray(alRes.data) ? alRes.data : []);
    } catch { /* silent */ }
  }, []);

  useEffect(() => { fetchOpp(); }, [fetchOpp]);

  // Lazy-load tab data
  useEffect(() => {
    if (tab === 1) fetchProducts();
    if (tab === 2) fetchCompetitors();
    if (tab === 3) fetchActivities();
    if (tab === 4) { fetchCollaborators(); fetchNotes(); }
    if (tab === 5) fetchReminders();
    if (tab === 6) fetchAnalytics();
  }, [tab, fetchProducts, fetchCompetitors, fetchActivities, fetchCollaborators, fetchNotes, fetchReminders, fetchAnalytics]);

  /* ── Edit handlers ── */
  const startEdit = () => {
    if (!opp) return;
    setFormData({
      name: opp.name, accountId: opp.accountId ?? '', contactId: opp.contactId ?? '',
      amount: opp.amount, probability: opp.probability, closeDate: opp.closeDate?.split('T')[0] ?? '',
      description: opp.description ?? '', assignedTo: opp.assignedTo ?? '', stage: opp.stage,
      currency: opp.currency ?? 'USD', nextStep: opp.nextStep ?? '', leadSource: opp.leadSource ?? '',
      ownerId: opp.ownerId ?? '',
    });
    setEditing(true);
  };

  const cancelEdit = () => { setEditing(false); setFormData({}); };

  const saveEdit = async () => {
    if (!id) return;
    try {
      const res = await opportunityService.update(id, formData);
      setOpp(res.data);
      setEditing(false);
      enqueueSnackbar('Opportunity updated', { variant: 'success' });
    } catch { enqueueSnackbar('Update failed', { variant: 'error' }); }
  };

  const handleField = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFormData((p: Record<string, any>) => ({ ...p, [field]: e.target.value }));

  /* ── Stage change ── */
  const handleStageChange = async (stage: OpportunityStage) => {
    if (!id) return;
    try {
      const res = await opportunityService.updateStage(id, stage);
      setOpp(res.data);
      enqueueSnackbar(`Stage updated to ${stage.replace(/_/g, ' ')}`, { variant: 'success' });
    } catch { enqueueSnackbar('Stage update failed', { variant: 'error' }); }
  };

  /* ── Predict ── */
  const handlePredict = async () => {
    if (!id) return;
    try {
      const res = await opportunityService.predictCloseDate(id);
      setOpp(res.data);
      enqueueSnackbar(`Predicted close: ${res.data.predictedCloseDate ?? 'N/A'} (confidence: ${res.data.confidenceScore ?? 0}%)`, { variant: 'info' });
    } catch { enqueueSnackbar('Prediction failed', { variant: 'error' }); }
  };

  /* ── Product CRUD ── */
  const handleAddProduct = async () => {
    if (!id) return;
    try {
      await opportunityService.addProduct(id, productForm);
      setProductDialog(false);
      setProductForm({ productName: '', quantity: 1, unitPrice: 0, discount: 0 });
      fetchProducts();
      enqueueSnackbar('Product added', { variant: 'success' });
    } catch { enqueueSnackbar('Failed to add product', { variant: 'error' }); }
  };

  /* ── Competitor CRUD ── */
  const handleAddCompetitor = async () => {
    if (!id) return;
    try {
      await opportunityService.addCompetitor(id, competitorForm);
      setCompetitorDialog(false);
      setCompetitorForm({ competitorName: '', threatLevel: 'MEDIUM' });
      fetchCompetitors();
      enqueueSnackbar('Competitor added', { variant: 'success' });
    } catch { enqueueSnackbar('Failed to add competitor', { variant: 'error' }); }
  };

  /* ── Note CRUD ── */
  const handleAddNote = async () => {
    if (!id) return;
    try {
      await opportunityService.addNote(id, noteForm);
      setNoteDialog(false);
      setNoteForm({ content: '', isPinned: false });
      fetchNotes();
      enqueueSnackbar('Note added', { variant: 'success' });
    } catch { enqueueSnackbar('Failed to add note', { variant: 'error' }); }
  };

  /* ── Reminder CRUD ── */
  const handleAddReminder = async () => {
    if (!id) return;
    try {
      await opportunityService.addReminder(id, reminderForm);
      setReminderDialog(false);
      setReminderForm({ reminderType: 'FOLLOW_UP', message: '', remindAt: '' });
      fetchReminders();
      enqueueSnackbar('Reminder added', { variant: 'success' });
    } catch { enqueueSnackbar('Failed to add reminder', { variant: 'error' }); }
  };

  const handleCompleteReminder = async (reminderId: string) => {
    if (!id) return;
    try {
      await opportunityService.completeReminder(id, reminderId);
      fetchReminders();
      enqueueSnackbar('Reminder completed', { variant: 'success' });
    } catch { enqueueSnackbar('Failed', { variant: 'error' }); }
  };

  /* ── Collaborator CRUD ── */
  const handleAddCollaborator = async () => {
    if (!id) return;
    try {
      await opportunityService.addCollaborator(id, collabForm.userId, collabForm.role);
      setCollabDialog(false);
      setCollabForm({ userId: '', role: 'MEMBER' });
      fetchCollaborators();
      enqueueSnackbar('Collaborator added', { variant: 'success' });
    } catch { enqueueSnackbar('Failed to add collaborator', { variant: 'error' }); }
  };

  /* ── Delete handler ── */
  const handleDelete = async () => {
    const { type, id: targetId } = deleteConfirm;
    if (!id) return;
    try {
      if (type === 'product') await opportunityService.deleteProduct(id, targetId);
      else if (type === 'competitor') await opportunityService.deleteCompetitor(id, targetId);
      else if (type === 'note') await opportunityService.deleteNote(id, targetId);
      else if (type === 'reminder') await opportunityService.deleteReminder(id, targetId);
      else if (type === 'collaborator') await opportunityService.removeCollaborator(id, targetId);
      setDeleteConfirm({ open: false, type: '', id: '' });
      if (type === 'product') fetchProducts();
      else if (type === 'competitor') fetchCompetitors();
      else if (type === 'note') fetchNotes();
      else if (type === 'reminder') fetchReminders();
      else if (type === 'collaborator') fetchCollaborators();
      enqueueSnackbar('Deleted', { variant: 'success' });
    } catch { enqueueSnackbar('Delete failed', { variant: 'error' }); }
  };

  if (loading) return <LinearProgress />;
  if (!opp) return <Typography sx={{ p: 3 }}>Opportunity not found</Typography>;

  /* ============================================================
     RENDER
     ============================================================ */
  return (
    <>
      <PageHeader
        title={opp.name}
        breadcrumbs={[
          { label: 'Dashboard', to: '/dashboard' },
          { label: 'Opportunities', to: '/opportunities' },
          { label: opp.name },
        ]}
        action={
          <Stack direction="row" spacing={1}>
            <Button startIcon={<ArrowBack />} onClick={() => navigate('/opportunities')}>Back</Button>
            {!editing ? (
              <Button variant="contained" startIcon={<EditIcon />} onClick={startEdit}>Edit</Button>
            ) : (
              <>
                <Button variant="contained" color="success" startIcon={<SaveIcon />} onClick={saveEdit}>Save</Button>
                <Button startIcon={<CancelIcon />} onClick={cancelEdit}>Cancel</Button>
              </>
            )}
          </Stack>
        }
      />

      {/* ---- Stage progress ---- */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center">
          <Typography variant="subtitle2" sx={{ mr: 1 }}>Stage:</Typography>
          {STAGES.map((s) => (
            <Chip
              key={s}
              label={s.replace(/_/g, ' ')}
              color={opp.stage === s ? 'primary' : 'default'}
              variant={opp.stage === s ? 'filled' : 'outlined'}
              onClick={() => handleStageChange(s)}
              size="small"
              sx={{ cursor: 'pointer' }}
            />
          ))}
          <Divider orientation="vertical" flexItem sx={{ mx: 1 }} />
          <Tooltip title="AI-predicted close date">
            <Button size="small" startIcon={<TrendingUp />} onClick={handlePredict}>Predict Close</Button>
          </Tooltip>
        </Stack>
      </Paper>

      {/* ---- Summary cards ---- */}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={6} sm={3}><MetricCard title="Amount" value={fmt$(opp.amount)} icon={<AttachMoney />} color="#1976d2" /></Grid>
        <Grid item xs={6} sm={3}><MetricCard title="Probability" value={`${opp.probability ?? 0}%`} icon={<Speed />} color="#7c3aed" /></Grid>
        <Grid item xs={6} sm={3}><MetricCard title="Close Date" value={fmtDate(opp.closeDate)} icon={<CalendarToday />} color="#d97706" /></Grid>
        <Grid item xs={6} sm={3}>
          <MetricCard
            title="Predicted Close"
            value={opp.predictedCloseDate ? `${fmtDate(opp.predictedCloseDate)} (${opp.confidenceScore ?? 0}%)` : 'N/A'}
            icon={<Insights />}
            color="#0891b2"
          />
        </Grid>
      </Grid>

      {/* ---- Alerts banner ---- */}
      {alerts.filter((a) => a.opportunityId === id).length > 0 && (
        <Paper sx={{ p: 1.5, mb: 2, bgcolor: 'warning.light' }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <WarningIcon color="warning" />
            <Typography variant="body2">
              {alerts.filter((a) => a.opportunityId === id).map((a) => a.message).join(' | ')}
            </Typography>
          </Stack>
        </Paper>
      )}

      {/* ---- Tabs ---- */}
      <Paper sx={{ mb: 2 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto">
          <Tab label="Details" />
          <Tab label="Products" />
          <Tab label="Competitors" />
          <Tab label="Timeline" />
          <Tab label="Collaboration" />
          <Tab label="Reminders" />
          <Tab label="Analytics" />
        </Tabs>
      </Paper>

      {/* ============ TAB 0: Details ============ */}
      <TabPanel value={tab} index={0}>
        <Paper sx={{ p: 3 }}>
          <Grid container spacing={2}>
            {editing ? (
              <>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="Name" value={formData.name ?? ''} onChange={handleField('name')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="Account ID" value={formData.accountId ?? ''} onChange={handleField('accountId')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="Contact ID" value={formData.contactId ?? ''} onChange={handleField('contactId')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth type="number" label="Amount" value={formData.amount ?? 0} onChange={handleField('amount')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth type="number" label="Probability" value={formData.probability ?? 0} onChange={handleField('probability')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth type="date" label="Close Date" value={formData.closeDate ?? ''} onChange={handleField('closeDate')} InputLabelProps={{ shrink: true }} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth select label="Stage" value={formData.stage ?? 'PROSPECTING'} onChange={handleField('stage')}>
                    {STAGES.map((s) => <MenuItem key={s} value={s}>{s.replace(/_/g, ' ')}</MenuItem>)}
                  </TextField>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="Currency" value={formData.currency ?? 'USD'} onChange={handleField('currency')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth select label="Lead Source" value={formData.leadSource ?? ''} onChange={handleField('leadSource')}>
                    <MenuItem value="">None</MenuItem>
                    {LEAD_SOURCES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
                  </TextField>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="Assigned To" value={formData.assignedTo ?? ''} onChange={handleField('assignedTo')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="Owner ID" value={formData.ownerId ?? ''} onChange={handleField('ownerId')} />
                </Grid>
                <Grid item xs={12}>
                  <TextField fullWidth label="Next Step" value={formData.nextStep ?? ''} onChange={handleField('nextStep')} />
                </Grid>
                <Grid item xs={12}>
                  <TextField fullWidth multiline rows={3} label="Description" value={formData.description ?? ''} onChange={handleField('description')} />
                </Grid>
              </>
            ) : (
              <>
                {[
                  ['Name', opp.name],
                  ['Stage', opp.stage?.replace(/_/g, ' ')],
                  ['Amount', fmt$(opp.amount)],
                  ['Currency', opp.currency ?? 'USD'],
                  ['Probability', `${opp.probability ?? 0}%`],
                  ['Forecast Category', opp.forecastCategory ?? '-'],
                  ['Close Date', fmtDate(opp.closeDate)],
                  ['Predicted Close', opp.predictedCloseDate ? `${fmtDate(opp.predictedCloseDate)} (${opp.confidenceScore ?? 0}% confidence)` : '-'],
                  ['Lead Source', opp.leadSource ?? '-'],
                  ['Next Step', opp.nextStep ?? '-'],
                  ['Account ID', opp.accountId ?? '-'],
                  ['Contact ID', opp.contactId ?? '-'],
                  ['Assigned To', opp.assignedTo ?? '-'],
                  ['Owner', opp.ownerId ?? '-'],
                  ['Won Date', fmtDate(opp.wonDate)],
                  ['Lost Date', fmtDate(opp.lostDate)],
                  ['Lost Reason', opp.lostReason ?? '-'],
                  ['Created', fmtDateTime(opp.createdAt)],
                  ['Updated', fmtDateTime(opp.updatedAt)],
                ].map(([label, value]) => (
                  <Grid item xs={12} sm={6} key={label}>
                    <Typography variant="caption" color="text.secondary">{label}</Typography>
                    <Typography variant="body1">{value}</Typography>
                  </Grid>
                ))}
                <Grid item xs={12}>
                  <Typography variant="caption" color="text.secondary">Description</Typography>
                  <Typography variant="body1">{opp.description ?? '-'}</Typography>
                </Grid>
              </>
            )}
          </Grid>
        </Paper>
      </TabPanel>

      {/* ============ TAB 1: Products ============ */}
      <TabPanel value={tab} index={1}>
        <Paper sx={{ p: 2 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography variant="h6">Products</Typography>
            <Button startIcon={<AddIcon />} variant="contained" size="small" onClick={() => setProductDialog(true)}>Add Product</Button>
          </Stack>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Product</TableCell>
                <TableCell>Code</TableCell>
                <TableCell align="right">Qty</TableCell>
                <TableCell align="right">Unit Price</TableCell>
                <TableCell align="right">Discount %</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell width={60} />
              </TableRow>
            </TableHead>
            <TableBody>
              {products.map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.productName}</TableCell>
                  <TableCell>{p.productCode ?? '-'}</TableCell>
                  <TableCell align="right">{p.quantity}</TableCell>
                  <TableCell align="right">{fmt$(p.unitPrice)}</TableCell>
                  <TableCell align="right">{p.discount}%</TableCell>
                  <TableCell align="right">{fmt$(p.totalPrice)}</TableCell>
                  <TableCell>
                    <IconButton size="small" color="error" onClick={() => setDeleteConfirm({ open: true, type: 'product', id: p.id })}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {products.length === 0 && (
                <TableRow><TableCell colSpan={7} align="center">No products yet</TableCell></TableRow>
              )}
              {products.length > 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="right"><strong>Total</strong></TableCell>
                  <TableCell align="right"><strong>{fmt$(products.reduce((s, p) => s + (p.totalPrice ?? 0), 0))}</strong></TableCell>
                  <TableCell />
                </TableRow>
              )}
            </TableBody>
          </Table>
        </Paper>
      </TabPanel>

      {/* ============ TAB 2: Competitors ============ */}
      <TabPanel value={tab} index={2}>
        <Paper sx={{ p: 2 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography variant="h6">Competitors</Typography>
            <Button startIcon={<AddIcon />} variant="contained" size="small" onClick={() => setCompetitorDialog(true)}>Add Competitor</Button>
          </Stack>
          <Grid container spacing={2}>
            {competitors.map((c) => (
              <Grid item xs={12} sm={6} md={4} key={c.id}>
                <Card variant="outlined">
                  <CardContent>
                    <Stack direction="row" justifyContent="space-between" alignItems="center">
                      <Typography variant="h6">{c.competitorName}</Typography>
                      <Stack direction="row" spacing={0.5}>
                        <Chip size="small" label={c.threatLevel} color={c.threatLevel === 'HIGH' ? 'error' : c.threatLevel === 'MEDIUM' ? 'warning' : 'success'} />
                        <IconButton size="small" color="error" onClick={() => setDeleteConfirm({ open: true, type: 'competitor', id: c.id })}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Stack>
                    </Stack>
                    <Divider sx={{ my: 1 }} />
                    <Typography variant="caption" color="text.secondary">Strengths</Typography>
                    <Typography variant="body2" sx={{ mb: 0.5 }}>{c.strengths || '-'}</Typography>
                    <Typography variant="caption" color="text.secondary">Weaknesses</Typography>
                    <Typography variant="body2" sx={{ mb: 0.5 }}>{c.weaknesses || '-'}</Typography>
                    <Typography variant="caption" color="text.secondary">Strategy</Typography>
                    <Typography variant="body2">{c.strategy || '-'}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
            {competitors.length === 0 && (
              <Grid item xs={12}><Typography align="center" color="text.secondary">No competitors tracked</Typography></Grid>
            )}
          </Grid>
        </Paper>
      </TabPanel>

      {/* ============ TAB 3: Timeline ============ */}
      <TabPanel value={tab} index={3}>
        <Paper sx={{ p: 2 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>Activity Timeline</Typography>
          <List dense>
            {activities.map((a) => (
              <ListItem key={a.id} divider>
                <ListItemIcon>
                  <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main', fontSize: 14 }}>
                    {a.activityType?.charAt(0) ?? '?'}
                  </Avatar>
                </ListItemIcon>
                <ListItemText
                  primary={a.description}
                  secondary={`${a.activityType} • ${fmtDateTime(a.createdAt)} • by ${a.createdBy ?? '-'}`}
                />
              </ListItem>
            ))}
            {activities.length === 0 && (
              <ListItem><ListItemText primary="No activities yet" /></ListItem>
            )}
          </List>
        </Paper>
      </TabPanel>

      {/* ============ TAB 4: Collaboration ============ */}
      <TabPanel value={tab} index={4}>
        <Grid container spacing={2}>
          {/* Collaborators */}
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 2 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                <Typography variant="h6">Collaborators</Typography>
                <Button startIcon={<AddIcon />} size="small" variant="contained" onClick={() => setCollabDialog(true)}>Add</Button>
              </Stack>
              <List dense>
                {collaborators.map((c) => (
                  <ListItem key={c.id} divider>
                    <ListItemIcon><Person /></ListItemIcon>
                    <ListItemText primary={c.userId} secondary={`Role: ${c.role} • Added: ${fmtDate(c.createdAt)}`} />
                    <ListItemSecondaryAction>
                      <IconButton size="small" color="error" onClick={() => setDeleteConfirm({ open: true, type: 'collaborator', id: c.userId })}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </ListItemSecondaryAction>
                  </ListItem>
                ))}
                {collaborators.length === 0 && <ListItem><ListItemText primary="No collaborators" /></ListItem>}
              </List>
            </Paper>
          </Grid>

          {/* Notes */}
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 2 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                <Typography variant="h6">Notes</Typography>
                <Button startIcon={<AddIcon />} size="small" variant="contained" onClick={() => setNoteDialog(true)}>Add Note</Button>
              </Stack>
              <List dense>
                {notes.map((n) => (
                  <ListItem key={n.id} divider>
                    <ListItemIcon>{n.isPinned ? <EmojiEvents color="warning" /> : <NoteIcon />}</ListItemIcon>
                    <ListItemText
                      primary={n.content}
                      secondary={`${fmtDateTime(n.createdAt)} • ${n.createdBy ?? ''} ${n.isPinned ? '📌 Pinned' : ''}`}
                    />
                    <ListItemSecondaryAction>
                      <IconButton size="small" color="error" onClick={() => setDeleteConfirm({ open: true, type: 'note', id: n.id })}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </ListItemSecondaryAction>
                  </ListItem>
                ))}
                {notes.length === 0 && <ListItem><ListItemText primary="No notes" /></ListItem>}
              </List>
            </Paper>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ============ TAB 5: Reminders ============ */}
      <TabPanel value={tab} index={5}>
        <Paper sx={{ p: 2 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography variant="h6">Reminders</Typography>
            <Button startIcon={<AddIcon />} variant="contained" size="small" onClick={() => setReminderDialog(true)}>Add Reminder</Button>
          </Stack>
          <List>
            {reminders.map((r) => (
              <ListItem key={r.id} divider>
                <ListItemIcon>
                  <Checkbox
                    edge="start"
                    checked={r.isCompleted}
                    onChange={() => !r.isCompleted && handleCompleteReminder(r.id)}
                    disabled={r.isCompleted}
                  />
                </ListItemIcon>
                <ListItemText
                  primary={r.message}
                  secondary={`${r.reminderType} • Due: ${fmtDateTime(r.remindAt)} ${r.isCompleted ? `• Completed: ${fmtDateTime(r.completedAt)}` : ''}`}
                  sx={{ textDecoration: r.isCompleted ? 'line-through' : 'none', opacity: r.isCompleted ? 0.6 : 1 }}
                />
                <ListItemSecondaryAction>
                  <IconButton size="small" color="error" onClick={() => setDeleteConfirm({ open: true, type: 'reminder', id: r.id })}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
            {reminders.length === 0 && <ListItem><ListItemText primary="No reminders" /></ListItem>}
          </List>
        </Paper>
      </TabPanel>

      {/* ============ TAB 6: Analytics ============ */}
      <TabPanel value={tab} index={6}>
        <Grid container spacing={2}>
          {/* Forecast */}
          {forecast && (
            <>
              <Grid item xs={6} sm={3}><MetricCard title="Pipeline" value={fmt$(forecast.pipelineValue)} icon={<ShowChart />} color="#1976d2" /></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Best Case" value={fmt$(forecast.bestCaseValue)} icon={<TrendingUp />} color="#059669" /></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Commit" value={fmt$(forecast.commitValue)} icon={<CheckCircle />} color="#7c3aed" /></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Weighted Pipeline" value={fmt$(forecast.weightedPipeline)} icon={<Assessment />} color="#d97706" /></Grid>
            </>
          )}

          {/* Revenue Analytics */}
          {analytics && (
            <>
              <Grid item xs={6} sm={3}><MetricCard title="Total Opps" value={String(analytics.totalOpportunities)} icon={<TrackChanges />} color="#1976d2" /></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Win Rate" value={`${(analytics.winRate * 100).toFixed(1)}%`} icon={<EmojiEvents />} color="#059669" /></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Avg Deal Size" value={fmt$(analytics.averageDealSize)} icon={<AttachMoney />} color="#7c3aed" /></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Total Revenue" value={fmt$(analytics.totalRevenue)} icon={<ShowChart />} color="#d97706" /></Grid>
              {analytics.revenueByStage && Object.keys(analytics.revenueByStage).length > 0 && (
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" sx={{ mb: 1 }}>Revenue by Stage</Typography>
                    <Table size="small">
                      <TableHead><TableRow><TableCell>Stage</TableCell><TableCell align="right">Revenue</TableCell></TableRow></TableHead>
                      <TableBody>
                        {Object.entries(analytics.revenueByStage).map(([stage, rev]) => (
                          <TableRow key={stage}><TableCell>{stage.replace(/_/g, ' ')}</TableCell><TableCell align="right">{fmt$(Number(rev))}</TableCell></TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </Paper>
                </Grid>
              )}
              {analytics.revenueByLeadSource && Object.keys(analytics.revenueByLeadSource).length > 0 && (
                <Grid item xs={12} md={6}>
                  <Paper sx={{ p: 2 }}>
                    <Typography variant="h6" sx={{ mb: 1 }}>Revenue by Lead Source</Typography>
                    <Table size="small">
                      <TableHead><TableRow><TableCell>Source</TableCell><TableCell align="right">Revenue</TableCell></TableRow></TableHead>
                      <TableBody>
                        {Object.entries(analytics.revenueByLeadSource).map(([src, rev]) => (
                          <TableRow key={src}><TableCell>{src}</TableCell><TableCell align="right">{fmt$(Number(rev))}</TableCell></TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </Paper>
                </Grid>
              )}
            </>
          )}

          {/* Win/Loss */}
          {winLoss && (
            <>
              <Grid item xs={12}><Divider sx={{ my: 1 }}><Typography variant="subtitle2">Win / Loss Analysis</Typography></Divider></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Won" value={String(winLoss.totalClosedWon)} icon={<EmojiEvents />} color="#059669" /></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Lost" value={String(winLoss.totalClosedLost)} icon={<WarningIcon />} color="#dc2626" /></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Avg Days to Close" value={String(winLoss.averageDaysToClose)} icon={<Schedule />} color="#0891b2" /></Grid>
              <Grid item xs={6} sm={3}><MetricCard title="Avg Won Deal" value={fmt$(winLoss.averageWonDealSize)} icon={<AttachMoney />} color="#059669" /></Grid>
            </>
          )}

          {/* Alerts */}
          {alerts.length > 0 && (
            <Grid item xs={12}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" sx={{ mb: 1 }}>Alerts</Typography>
                <List dense>
                  {alerts.map((a, i) => (
                    <ListItem key={i} divider>
                      <ListItemIcon>
                        <WarningIcon color={a.severity === 'HIGH' ? 'error' : a.severity === 'MEDIUM' ? 'warning' : 'info'} />
                      </ListItemIcon>
                      <ListItemText primary={a.message} secondary={`${a.type} • ${a.opportunityName}`} />
                    </ListItem>
                  ))}
                </List>
              </Paper>
            </Grid>
          )}
        </Grid>
      </TabPanel>

      {/* ============ DIALOGS ============ */}

      {/* Add Product Dialog */}
      <Dialog open={productDialog} onClose={() => setProductDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Product</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0 }}>
            <Grid item xs={12}><TextField fullWidth label="Product Name" value={productForm.productName} onChange={(e) => setProductForm((p) => ({ ...p, productName: e.target.value }))} required /></Grid>
            <Grid item xs={6}><TextField fullWidth label="Product Code" value={productForm.productCode ?? ''} onChange={(e) => setProductForm((p) => ({ ...p, productCode: e.target.value }))} /></Grid>
            <Grid item xs={6}><TextField fullWidth type="number" label="Quantity" value={productForm.quantity} onChange={(e) => setProductForm((p) => ({ ...p, quantity: Number(e.target.value) }))} /></Grid>
            <Grid item xs={6}><TextField fullWidth type="number" label="Unit Price" value={productForm.unitPrice} onChange={(e) => setProductForm((p) => ({ ...p, unitPrice: Number(e.target.value) }))} /></Grid>
            <Grid item xs={6}><TextField fullWidth type="number" label="Discount %" value={productForm.discount} onChange={(e) => setProductForm((p) => ({ ...p, discount: Number(e.target.value) }))} /></Grid>
            <Grid item xs={12}><TextField fullWidth label="Description" multiline rows={2} value={productForm.description ?? ''} onChange={(e) => setProductForm((p) => ({ ...p, description: e.target.value }))} /></Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setProductDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddProduct} disabled={!productForm.productName}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Add Competitor Dialog */}
      <Dialog open={competitorDialog} onClose={() => setCompetitorDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Competitor</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0 }}>
            <Grid item xs={8}><TextField fullWidth label="Competitor Name" value={competitorForm.competitorName} onChange={(e) => setCompetitorForm((p) => ({ ...p, competitorName: e.target.value }))} required /></Grid>
            <Grid item xs={4}>
              <TextField fullWidth select label="Threat Level" value={competitorForm.threatLevel ?? 'MEDIUM'} onChange={(e) => setCompetitorForm((p) => ({ ...p, threatLevel: e.target.value }))}>
                {THREAT_LEVELS.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={12}><TextField fullWidth multiline rows={2} label="Strengths" value={competitorForm.strengths ?? ''} onChange={(e) => setCompetitorForm((p) => ({ ...p, strengths: e.target.value }))} /></Grid>
            <Grid item xs={12}><TextField fullWidth multiline rows={2} label="Weaknesses" value={competitorForm.weaknesses ?? ''} onChange={(e) => setCompetitorForm((p) => ({ ...p, weaknesses: e.target.value }))} /></Grid>
            <Grid item xs={12}><TextField fullWidth multiline rows={2} label="Strategy" value={competitorForm.strategy ?? ''} onChange={(e) => setCompetitorForm((p) => ({ ...p, strategy: e.target.value }))} /></Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCompetitorDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddCompetitor} disabled={!competitorForm.competitorName}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Add Note Dialog */}
      <Dialog open={noteDialog} onClose={() => setNoteDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Note</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0 }}>
            <Grid item xs={12}><TextField fullWidth multiline rows={4} label="Content" value={noteForm.content} onChange={(e) => setNoteForm((p) => ({ ...p, content: e.target.value }))} required /></Grid>
            <Grid item xs={12}>
              <Button variant={noteForm.isPinned ? 'contained' : 'outlined'} size="small" onClick={() => setNoteForm((p) => ({ ...p, isPinned: !p.isPinned }))}>
                {noteForm.isPinned ? '📌 Pinned' : 'Pin this note'}
              </Button>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNoteDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddNote} disabled={!noteForm.content}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Add Reminder Dialog */}
      <Dialog open={reminderDialog} onClose={() => setReminderDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Reminder</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0 }}>
            <Grid item xs={6}>
              <TextField fullWidth select label="Type" value={reminderForm.reminderType} onChange={(e) => setReminderForm((p) => ({ ...p, reminderType: e.target.value }))}>
                {REMINDER_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth type="datetime-local" label="Remind At" value={reminderForm.remindAt} onChange={(e) => setReminderForm((p) => ({ ...p, remindAt: e.target.value }))} InputLabelProps={{ shrink: true }} />
            </Grid>
            <Grid item xs={12}><TextField fullWidth label="Message" multiline rows={2} value={reminderForm.message} onChange={(e) => setReminderForm((p) => ({ ...p, message: e.target.value }))} required /></Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReminderDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddReminder} disabled={!reminderForm.message || !reminderForm.remindAt}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Add Collaborator Dialog */}
      <Dialog open={collabDialog} onClose={() => setCollabDialog(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Add Collaborator</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0 }}>
            <Grid item xs={12}><TextField fullWidth label="User ID" value={collabForm.userId} onChange={(e) => setCollabForm((p) => ({ ...p, userId: e.target.value }))} required /></Grid>
            <Grid item xs={12}>
              <TextField fullWidth select label="Role" value={collabForm.role} onChange={(e) => setCollabForm((p) => ({ ...p, role: e.target.value }))}>
                {['MEMBER', 'APPROVER', 'REVIEWER', 'OBSERVER'].map((r) => <MenuItem key={r} value={r}>{r}</MenuItem>)}
              </TextField>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCollabDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddCollaborator} disabled={!collabForm.userId}>Add</Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={deleteConfirm.open}
        title={`Delete ${deleteConfirm.type}?`}
        message="This action cannot be undone."
        onConfirm={handleDelete}
        onCancel={() => setDeleteConfirm({ open: false, type: '', id: '' })}
      />
    </>
  );
};

export default OpportunityDetailPage;
