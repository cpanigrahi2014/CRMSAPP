/* ============================================================
   ActivitiesPage – Full activity & task management
   Features: Task CRUD, type tabs, reminders, recurring,
   call/email logging, meeting scheduling, timeline,
   analytics dashboard
   ============================================================ */
import React, { useEffect, useState, useMemo, useCallback } from 'react';
import {
  GridColDef,
  GridRenderCellParams,
  GridPaginationModel,
} from '@mui/x-data-grid';
import {
  TextField,
  Grid,
  IconButton,
  Tooltip,
  Stack,
  Box,
  Typography,
  Chip,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Button,
  Tab,
  Tabs,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Divider,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Switch,
  FormControlLabel,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  CheckCircle as CompleteIcon,
  Add as AddIcon,
  Task as TaskIcon,
  Call as CallIcon,
  Event as MeetingIcon,
  Email as EmailIcon,
  Assignment as AssignmentIcon,
  Schedule as ScheduleIcon,
  Analytics as AnalyticsIcon,
  Timeline as TimelineIcon,
  NotificationsActive as ReminderIcon,
  Repeat as RecurringIcon,
  TrendingUp as TrendingUpIcon,
  Cancel as CancelIcon,
  PlayArrow as PlayIcon,
  Sync as SyncIcon,
  ContentCopy as CopyIcon,
  LinkOff as LinkOffIcon,
  AutoAwesome as AiIcon,
} from '@mui/icons-material';
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip as RTooltip, ResponsiveContainer, Legend,
} from 'recharts';
import { DataTable, PageHeader, ConfirmDialog, MetricCard, VoiceInput } from '../components';
import { activityService } from '../services';
import { aiInsightsService, GeneratedMeetingSummary } from '../services/aiInsightsService';
import type {
  Activity,
  ActivityType,
  ActivityStatus,
  ActivityPriority,
  RecurrenceRule,
  ActivityAnalytics,
  CreateActivityRequest,
  CalendarFeedToken,
} from '../types';
import { useSnackbar } from 'notistack';

/* ── Constants ─────────────────────────────────────────────── */
const TYPE_OPTIONS: ActivityType[] = ['TASK', 'CALL', 'MEETING', 'EMAIL'];
const STATUS_OPTIONS: ActivityStatus[] = ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
const PRIORITY_OPTIONS: ActivityPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
const RECURRENCE_OPTIONS: RecurrenceRule[] = ['DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY'];

const typeIcons: Record<ActivityType, React.ReactNode> = {
  TASK: <TaskIcon fontSize="small" />,
  CALL: <CallIcon fontSize="small" />,
  MEETING: <MeetingIcon fontSize="small" />,
  EMAIL: <EmailIcon fontSize="small" />,
};

const statusColors: Record<ActivityStatus, 'default' | 'info' | 'success' | 'error'> = {
  NOT_STARTED: 'default',
  IN_PROGRESS: 'info',
  COMPLETED: 'success',
  CANCELLED: 'error',
};

const priorityColors: Record<ActivityPriority, string> = {
  LOW: '#4caf50',
  MEDIUM: '#ff9800',
  HIGH: '#f44336',
  URGENT: '#9c27b0',
};

const CHART_COLORS = ['#1976d2', '#4caf50', '#ff9800', '#f44336', '#9c27b0', '#00bcd4', '#795548'];

/* ── Helper ────────────────────────────────────────────────── */
const fmtDate = (d: string | null) => (d ? new Date(d).toLocaleDateString() : '—');
const fmtDateTime = (d: string | null) => (d ? new Date(d).toLocaleString() : '—');
const isOverdue = (a: Activity) =>
  a.dueDate && a.status !== 'COMPLETED' && a.status !== 'CANCELLED' && new Date(a.dueDate) < new Date();

/* ── Empty form defaults ───────────────────────────────────── */
const emptyForm = (): Record<string, any> => ({
  type: 'TASK' as ActivityType,
  subject: '',
  description: '',
  status: 'NOT_STARTED' as ActivityStatus,
  priority: 'MEDIUM' as ActivityPriority,
  dueDate: '',
  startTime: '',
  endTime: '',
  assignedTo: '',
  relatedEntityType: '',
  relatedEntityId: '',
  reminderAt: '',
  enableRecurrence: false,
  recurrenceRule: '' as string,
  recurrenceEnd: '',
  location: '',
  callDurationMinutes: '',
  callOutcome: '',
  emailTo: '',
  emailCc: '',
});

/* ── Tab panels ────────────────────────────────────────────── */
interface TabPanelProps { children?: React.ReactNode; index: number; value: number; }
const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => (
  <div role="tabpanel" hidden={value !== index}>{value === index && <Box sx={{ pt: 2 }}>{children}</Box>}</div>
);

/* ================================================================
   Main Component
   ================================================================ */
const ActivitiesPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  /* ---- State ---- */
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<string>('ALL');
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [mainTab, setMainTab] = useState(0); // 0=List, 1=Timeline, 2=Calendar, 3=Analytics

  /* Form state */
  const [formOpen, setFormOpen] = useState(false);
  const [formData, setFormData] = useState(emptyForm());
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  /* AI Summarize state */
  const [aiSummary, setAiSummary] = useState<GeneratedMeetingSummary | null>(null);
  const [aiSummaryLoading, setAiSummaryLoading] = useState(false);

  /* Timeline state */
  const [timeline, setTimeline] = useState<Activity[]>([]);
  const [timelineLoading, setTimelineLoading] = useState(false);

  /* Calendar / upcoming state */
  const [upcoming, setUpcoming] = useState<Activity[]>([]);
  const [upcomingDays, setUpcomingDays] = useState(7);
  const [upcomingLoading, setUpcomingLoading] = useState(false);

  /* Analytics state */
  const [analytics, setAnalytics] = useState<ActivityAnalytics | null>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);

  /* Calendar Sync state */
  const [calTokens, setCalTokens] = useState<CalendarFeedToken[]>([]);
  const [calTokensLoading, setCalTokensLoading] = useState(false);
  const [newTokenName, setNewTokenName] = useState('');
  const [copiedToken, setCopiedToken] = useState<string | null>(null);

  /* ---- Fetch list ---- */
  const fetchActivities = useCallback(async () => {
    setLoading(true);
    try {
      const res = await activityService.getAll(paginationModel.page, paginationModel.pageSize);
      setActivities(Array.isArray(res.data) ? res.data : (res.data as any).content ?? []);
    } catch {
      enqueueSnackbar('Failed to load activities', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [paginationModel, enqueueSnackbar]);

  useEffect(() => { fetchActivities(); }, [fetchActivities]);

  /* ---- Fetch timeline ---- */
  const fetchTimeline = useCallback(async () => {
    setTimelineLoading(true);
    try {
      const res = await activityService.getTimeline();
      setTimeline(Array.isArray(res.data) ? res.data : []);
    } catch {
      enqueueSnackbar('Failed to load timeline', { variant: 'error' });
    } finally {
      setTimelineLoading(false);
    }
  }, [enqueueSnackbar]);

  /* ---- Fetch upcoming ---- */
  const fetchUpcoming = useCallback(async () => {
    setUpcomingLoading(true);
    try {
      const res = await activityService.getUpcoming(upcomingDays);
      setUpcoming(Array.isArray(res.data) ? res.data : []);
    } catch {
      enqueueSnackbar('Failed to load upcoming activities', { variant: 'error' });
    } finally {
      setUpcomingLoading(false);
    }
  }, [upcomingDays, enqueueSnackbar]);

  /* ---- Fetch analytics ---- */
  const fetchAnalytics = useCallback(async () => {
    setAnalyticsLoading(true);
    try {
      const res = await activityService.getAnalytics();
      setAnalytics(res.data);
    } catch {
      enqueueSnackbar('Failed to load analytics', { variant: 'error' });
    } finally {
      setAnalyticsLoading(false);
    }
  }, [enqueueSnackbar]);

  const fetchCalTokens = useCallback(async () => {
    setCalTokensLoading(true);
    try {
      const res = await activityService.getCalendarTokens();
      setCalTokens(Array.isArray(res.data) ? res.data : []);
    } catch {
      // Silently handle - calendar tokens are optional
    } finally {
      setCalTokensLoading(false);
    }
  }, []);

  /* Load data when tab changes */
  useEffect(() => {
    if (mainTab === 1) fetchTimeline();
    if (mainTab === 2) fetchUpcoming();
    if (mainTab === 3) fetchAnalytics();
    if (mainTab === 4) fetchCalTokens();
  }, [mainTab, fetchTimeline, fetchUpcoming, fetchAnalytics, fetchCalTokens]);

  /* ---- Filtering ---- */
  const filtered = useMemo(() => {
    let rows = activities;
    if (typeFilter !== 'ALL') rows = rows.filter((a) => a.type === typeFilter);
    if (statusFilter !== 'ALL') rows = rows.filter((a) => a.status === statusFilter);
    if (priorityFilter !== 'ALL') rows = rows.filter((a) => a.priority === priorityFilter);
    if (search) {
      const q = search.toLowerCase();
      rows = rows.filter((a) =>
        a.subject.toLowerCase().includes(q) ||
        (a.description && a.description.toLowerCase().includes(q)) ||
        (a.assignedTo && a.assignedTo.toLowerCase().includes(q)),
      );
    }
    return rows;
  }, [activities, typeFilter, statusFilter, priorityFilter, search]);

  /* ---- CRUD handlers ---- */
  const openCreate = (type: ActivityType = 'TASK') => {
    setEditingId(null);
    setFormData({ ...emptyForm(), type });
    setFormOpen(true);
  };

  const openEdit = (a: Activity) => {
    setEditingId(a.id);
    setFormData({
      type: a.type,
      subject: a.subject,
      description: a.description ?? '',
      status: a.status,
      priority: a.priority,
      dueDate: a.dueDate ? a.dueDate.substring(0, 16) : '',
      startTime: a.startTime ? a.startTime.substring(0, 16) : '',
      endTime: a.endTime ? a.endTime.substring(0, 16) : '',
      assignedTo: a.assignedTo ?? '',
      relatedEntityType: a.relatedEntityType ?? '',
      relatedEntityId: a.relatedEntityId ?? '',
      reminderAt: a.reminderAt ? a.reminderAt.substring(0, 16) : '',
      enableRecurrence: !!a.recurrenceRule,
      recurrenceRule: a.recurrenceRule ?? '',
      recurrenceEnd: a.recurrenceEnd ?? '',
      location: a.location ?? '',
      callDurationMinutes: a.callDurationMinutes ?? '',
      callOutcome: a.callOutcome ?? '',
      emailTo: a.emailTo ?? '',
      emailCc: a.emailCc ?? '',
    });
    setFormOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const payload: CreateActivityRequest = {
        type: formData.type,
        subject: formData.subject,
        description: formData.description || undefined,
        status: formData.status,
        priority: formData.priority,
        dueDate: formData.dueDate || undefined,
        startTime: formData.startTime || undefined,
        endTime: formData.endTime || undefined,
        assignedTo: formData.assignedTo || undefined,
        relatedEntityType: formData.relatedEntityType || undefined,
        relatedEntityId: formData.relatedEntityId || undefined,
        reminderAt: formData.reminderAt || undefined,
        recurrenceRule: formData.enableRecurrence && formData.recurrenceRule ? formData.recurrenceRule : undefined,
        recurrenceEnd: formData.enableRecurrence && formData.recurrenceEnd ? formData.recurrenceEnd : undefined,
        location: formData.location || undefined,
        callDurationMinutes: formData.callDurationMinutes ? Number(formData.callDurationMinutes) : undefined,
        callOutcome: formData.callOutcome || undefined,
        emailTo: formData.emailTo || undefined,
        emailCc: formData.emailCc || undefined,
      };
      if (editingId) {
        await activityService.update(editingId, payload);
        enqueueSnackbar('Activity updated', { variant: 'success' });
      } else {
        await activityService.create(payload);
        enqueueSnackbar('Activity created', { variant: 'success' });
      }
      setFormOpen(false);
      fetchActivities();
    } catch {
      enqueueSnackbar('Failed to save activity', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      await activityService.delete(deleteId);
      enqueueSnackbar('Activity deleted', { variant: 'success' });
      setDeleteId(null);
      fetchActivities();
    } catch {
      enqueueSnackbar('Failed to delete activity', { variant: 'error' });
    }
  };

  const handleComplete = async (id: string) => {
    try {
      await activityService.complete(id);
      enqueueSnackbar('Activity marked complete', { variant: 'success' });
      fetchActivities();
    } catch {
      enqueueSnackbar('Failed to complete activity', { variant: 'error' });
    }
  };

  const handleAiSummarize = async () => {
    setAiSummaryLoading(true);
    setAiSummary(null);
    try {
      const result = await aiInsightsService.generateMeetingSummary(
        formData.subject || 'Meeting',
        formData.description || '',
        formData.startTime || undefined,
        [],
        formData.relatedEntityType || undefined,
        formData.relatedEntityId || undefined,
      );
      setAiSummary(result);
      enqueueSnackbar('AI summary generated', { variant: 'success' });
    } catch {
      enqueueSnackbar('Failed to generate summary', { variant: 'error' });
    } finally { setAiSummaryLoading(false); }
  };

  /* ── Calendar token handlers ──────────────────────────────── */
  const handleCreateToken = async () => {
    if (!newTokenName.trim()) return;
    try {
      await activityService.createCalendarToken(newTokenName.trim());
      setNewTokenName('');
      enqueueSnackbar('Calendar feed token created', { variant: 'success' });
      fetchCalTokens();
    } catch {
      enqueueSnackbar('Failed to create token', { variant: 'error' });
    }
  };

  const handleRevokeToken = async (id: string) => {
    try {
      await activityService.revokeCalendarToken(id);
      enqueueSnackbar('Token revoked', { variant: 'success' });
      fetchCalTokens();
    } catch {
      enqueueSnackbar('Failed to revoke token', { variant: 'error' });
    }
  };

  const handleCopyFeedUrl = (token: string) => {
    const url = activityService.getCalendarFeedUrl(token);
    navigator.clipboard.writeText(url);
    setCopiedToken(token);
    enqueueSnackbar('Calendar feed URL copied!', { variant: 'success' });
    setTimeout(() => setCopiedToken(null), 3000);
  };

  const handleExportIcs = async () => {
    try {
      const blob = await activityService.exportCalendar();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'activities.ics';
      a.click();
      window.URL.revokeObjectURL(url);
      enqueueSnackbar('Calendar exported', { variant: 'success' });
    } catch {
      enqueueSnackbar('Failed to export calendar', { variant: 'error' });
    }
  };

  const ff = (field: string) => (e: any) => setFormData((p: any) => ({ ...p, [field]: e.target.value }));

  /* ── DataGrid columns ──────────────────────────────────────── */
  const columns: GridColDef[] = useMemo(() => [
    {
      field: 'type',
      headerName: 'Type',
      width: 110,
      renderCell: (params: GridRenderCellParams) => (
        <Chip
          icon={typeIcons[params.row.type as ActivityType] as React.ReactElement}
          label={params.row.type}
          size="small"
          variant="outlined"
        />
      ),
    },
    { field: 'subject', headerName: 'Subject', flex: 1, minWidth: 200 },
    {
      field: 'status',
      headerName: 'Status',
      width: 130,
      renderCell: (params: GridRenderCellParams) => (
        <Chip
          label={params.row.status?.replace('_', ' ')}
          color={statusColors[params.row.status as ActivityStatus] ?? 'default'}
          size="small"
        />
      ),
    },
    {
      field: 'priority',
      headerName: 'Priority',
      width: 110,
      renderCell: (params: GridRenderCellParams) => (
        <Chip
          label={params.row.priority}
          size="small"
          sx={{ bgcolor: priorityColors[params.row.priority as ActivityPriority] + '20', color: priorityColors[params.row.priority as ActivityPriority], fontWeight: 600 }}
        />
      ),
    },
    {
      field: 'dueDate',
      headerName: 'Due Date',
      width: 130,
      renderCell: (params: GridRenderCellParams) => {
        const overdue = isOverdue(params.row);
        return (
          <Typography variant="body2" color={overdue ? 'error' : 'textPrimary'} fontWeight={overdue ? 600 : 400}>
            {fmtDate(params.row.dueDate)}
          </Typography>
        );
      },
    },
    { field: 'assignedTo', headerName: 'Assigned To', width: 160, valueFormatter: (value: string) => value || '—' },
    {
      field: 'reminderAt',
      headerName: 'Reminder',
      width: 120,
      renderCell: (params: GridRenderCellParams) =>
        params.row.reminderAt ? (
          <Tooltip title={fmtDateTime(params.row.reminderAt)}>
            <ReminderIcon fontSize="small" color={params.row.reminderSent ? 'disabled' : 'warning'} />
          </Tooltip>
        ) : null,
    },
    {
      field: 'recurrenceRule',
      headerName: 'Recurring',
      width: 110,
      renderCell: (params: GridRenderCellParams) =>
        params.row.recurrenceRule ? (
          <Chip icon={<RecurringIcon />} label={params.row.recurrenceRule} size="small" variant="outlined" />
        ) : null,
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      sortable: false,
      filterable: false,
      renderCell: (params: GridRenderCellParams) => (
        <Stack direction="row" spacing={0.5}>
          {params.row.status !== 'COMPLETED' && params.row.status !== 'CANCELLED' && (
            <Tooltip title="Mark Complete">
              <IconButton size="small" color="success" onClick={() => handleComplete(params.row.id)}>
                <CompleteIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => openEdit(params.row)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton size="small" color="error" onClick={() => setDeleteId(params.row.id)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ], []);

  /* ================================================================
     Render
     ================================================================ */
  return (
    <Box>
      <PageHeader
        title="Activities & Tasks"
        breadcrumbs={[{ label: 'Activities' }]}
        action={
          <Stack direction="row" spacing={1}>
            <Button variant="contained" startIcon={<TaskIcon />} onClick={() => openCreate('TASK')}>
              New Task
            </Button>
            <Button variant="outlined" startIcon={<CallIcon />} onClick={() => openCreate('CALL')}>
              Log Call
            </Button>
            <Button variant="outlined" startIcon={<MeetingIcon />} onClick={() => openCreate('MEETING')}>
              Schedule Meeting
            </Button>
            <Button variant="outlined" startIcon={<EmailIcon />} onClick={() => openCreate('EMAIL')}>
              Log Email
            </Button>
          </Stack>
        }
      />

      {/* ---- Main Tabs ---- */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={mainTab} onChange={(_, v) => setMainTab(v)}>
          <Tab icon={<AssignmentIcon />} iconPosition="start" label="All Activities" />
          <Tab icon={<TimelineIcon />} iconPosition="start" label="Timeline" />
          <Tab icon={<ScheduleIcon />} iconPosition="start" label="Calendar" />
          <Tab icon={<AnalyticsIcon />} iconPosition="start" label="Analytics" />
          <Tab icon={<SyncIcon />} iconPosition="start" label="Calendar Sync" />
        </Tabs>
      </Box>

      {/* ================================================================
         TAB 0 – Activity List with Filters
         ================================================================ */}
      <TabPanel value={mainTab} index={0}>
        {/* Filters */}
        <Stack direction="row" spacing={2} sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
          <TextField
            size="small"
            placeholder="Search activities…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            sx={{ minWidth: 220 }}
          />
          <FormControl size="small" sx={{ minWidth: 130 }}>
            <InputLabel>Type</InputLabel>
            <Select value={typeFilter} label="Type" onChange={(e) => setTypeFilter(e.target.value)}>
              <MenuItem value="ALL">All Types</MenuItem>
              {TYPE_OPTIONS.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel>Status</InputLabel>
            <Select value={statusFilter} label="Status" onChange={(e) => setStatusFilter(e.target.value)}>
              <MenuItem value="ALL">All Status</MenuItem>
              {STATUS_OPTIONS.map((s) => <MenuItem key={s} value={s}>{s.replace('_', ' ')}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 130 }}>
            <InputLabel>Priority</InputLabel>
            <Select value={priorityFilter} label="Priority" onChange={(e) => setPriorityFilter(e.target.value)}>
              <MenuItem value="ALL">All Priority</MenuItem>
              {PRIORITY_OPTIONS.map((p) => <MenuItem key={p} value={p}>{p}</MenuItem>)}
            </Select>
          </FormControl>
        </Stack>

        <DataTable
          title="Activities"
          rows={filtered}
          columns={columns}
          loading={loading}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
        />
      </TabPanel>

      {/* ================================================================
         TAB 1 – Timeline
         ================================================================ */}
      <TabPanel value={mainTab} index={1}>
        {timelineLoading ? (
          <Typography>Loading timeline…</Typography>
        ) : timeline.length === 0 ? (
          <Typography color="text.secondary">No timeline entries found.</Typography>
        ) : (
          <Paper variant="outlined" sx={{ p: 2 }}>
            <List>
              {timeline.map((a, i) => (
                <React.Fragment key={a.id}>
                  <ListItem alignItems="flex-start">
                    <ListItemIcon sx={{ mt: 1 }}>{typeIcons[a.type]}</ListItemIcon>
                    <ListItemText
                      primary={
                        <Stack direction="row" spacing={1} alignItems="center">
                          <Typography fontWeight={600}>{a.subject}</Typography>
                          <Chip label={a.status.replace('_', ' ')} color={statusColors[a.status]} size="small" />
                          <Chip label={a.priority} size="small" sx={{ bgcolor: priorityColors[a.priority] + '20', color: priorityColors[a.priority] }} />
                          {a.recurrenceRule && <Chip icon={<RecurringIcon />} label={a.recurrenceRule} size="small" variant="outlined" />}
                        </Stack>
                      }
                      secondary={
                        <Box sx={{ mt: 0.5 }}>
                          {a.description && <Typography variant="body2" color="text.secondary">{a.description}</Typography>}
                          <Typography variant="caption" color="text.secondary">
                            {fmtDateTime(a.createdAt)}
                            {a.assignedTo && ` · Assigned: ${a.assignedTo}`}
                            {a.dueDate && ` · Due: ${fmtDate(a.dueDate)}`}
                            {a.location && ` · ${a.location}`}
                            {a.callDurationMinutes && ` · ${a.callDurationMinutes} min`}
                          </Typography>
                        </Box>
                      }
                    />
                  </ListItem>
                  {i < timeline.length - 1 && <Divider variant="inset" component="li" />}
                </React.Fragment>
              ))}
            </List>
          </Paper>
        )}
      </TabPanel>

      {/* ================================================================
         TAB 2 – Calendar / Upcoming
         ================================================================ */}
      <TabPanel value={mainTab} index={2}>
        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
          <Typography variant="h6">Upcoming Activities</Typography>
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Days</InputLabel>
            <Select
              value={upcomingDays}
              label="Days"
              onChange={(e) => { setUpcomingDays(Number(e.target.value)); }}
            >
              {[3, 7, 14, 30].map((d) => <MenuItem key={d} value={d}>Next {d} days</MenuItem>)}
            </Select>
          </FormControl>
          <Button size="small" variant="outlined" onClick={fetchUpcoming}>Refresh</Button>
        </Stack>

        {upcomingLoading ? (
          <Typography>Loading…</Typography>
        ) : upcoming.length === 0 ? (
          <Typography color="text.secondary">No upcoming activities in the next {upcomingDays} days.</Typography>
        ) : (
          <Grid container spacing={2}>
            {upcoming.map((a) => (
              <Grid item xs={12} sm={6} md={4} key={a.id}>
                <Paper variant="outlined" sx={{ p: 2, borderLeft: `4px solid ${priorityColors[a.priority]}` }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    {typeIcons[a.type]}
                    <Typography fontWeight={600} noWrap>{a.subject}</Typography>
                  </Stack>
                  <Typography variant="body2" color="text.secondary" noWrap>{a.description || 'No description'}</Typography>
                  <Stack direction="row" spacing={1} sx={{ mt: 1 }} flexWrap="wrap" useFlexGap>
                    <Chip label={a.type} size="small" variant="outlined" />
                    <Chip label={a.status.replace('_', ' ')} color={statusColors[a.status]} size="small" />
                    {a.dueDate && (
                      <Chip
                        label={`Due: ${fmtDate(a.dueDate)}`}
                        size="small"
                        color={isOverdue(a) ? 'error' : 'default'}
                      />
                    )}
                  </Stack>
                  {a.location && <Typography variant="caption" sx={{ mt: 0.5, display: 'block' }}>📍 {a.location}</Typography>}
                  {a.assignedTo && <Typography variant="caption" color="text.secondary">Assigned: {a.assignedTo}</Typography>}
                </Paper>
              </Grid>
            ))}
          </Grid>
        )}
      </TabPanel>

      {/* ================================================================
         TAB 3 – Analytics
         ================================================================ */}
      <TabPanel value={mainTab} index={3}>
        {analyticsLoading || !analytics ? (
          <Typography>Loading analytics…</Typography>
        ) : (
          <Box>
            {/* KPI Cards */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12} sm={6} md={3}>
                <MetricCard title="Total Activities" value={analytics.totalActivities} icon={<AssignmentIcon />} color="#1976d2" />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <MetricCard title="Completed" value={analytics.completedActivities} icon={<CompleteIcon />} color="#4caf50" />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <MetricCard title="Overdue" value={analytics.overdueActivities} icon={<ScheduleIcon />} color="#f44336" />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <MetricCard
                  title="Completion Rate"
                  value={`${analytics.completionRate.toFixed(1)}%`}
                  icon={<TrendingUpIcon />}
                  color="#9c27b0"
                />
              </Grid>
            </Grid>

            {/* Charts */}
            <Grid container spacing={3}>
              {/* By Type Pie */}
              <Grid item xs={12} md={6}>
                <Paper variant="outlined" sx={{ p: 2 }}>
                  <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1 }}>Activities by Type</Typography>
                  <ResponsiveContainer width="100%" height={280}>
                    <PieChart>
                      <Pie
                        data={Object.entries(analytics.countByType).map(([name, value]) => ({ name, value }))}
                        cx="50%" cy="50%" outerRadius={100} dataKey="value" label
                      >
                        {Object.keys(analytics.countByType).map((_, i) => (
                          <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                        ))}
                      </Pie>
                      <RTooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>

              {/* By Status Pie */}
              <Grid item xs={12} md={6}>
                <Paper variant="outlined" sx={{ p: 2 }}>
                  <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1 }}>Activities by Status</Typography>
                  <ResponsiveContainer width="100%" height={280}>
                    <PieChart>
                      <Pie
                        data={Object.entries(analytics.countByStatus).map(([name, value]) => ({ name: name.replace('_', ' '), value }))}
                        cx="50%" cy="50%" outerRadius={100} dataKey="value" label
                      >
                        {Object.keys(analytics.countByStatus).map((_, i) => (
                          <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                        ))}
                      </Pie>
                      <RTooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>

              {/* By Priority Bar */}
              <Grid item xs={12} md={6}>
                <Paper variant="outlined" sx={{ p: 2 }}>
                  <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1 }}>Activities by Priority</Typography>
                  <ResponsiveContainer width="100%" height={280}>
                    <BarChart data={Object.entries(analytics.countByPriority).map(([name, value]) => ({ name, value }))}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" />
                      <YAxis />
                      <RTooltip />
                      <Bar dataKey="value" fill="#1976d2" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>

              {/* Avg Completion Days */}
              <Grid item xs={12} md={6}>
                <Paper variant="outlined" sx={{ p: 2, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                  <Typography variant="subtitle1" fontWeight={600}>Avg. Completion Time</Typography>
                  <Typography variant="h2" color="primary" sx={{ mt: 2 }}>
                    {analytics.avgCompletionDays.toFixed(1)}
                  </Typography>
                  <Typography variant="h6" color="text.secondary">days</Typography>
                </Paper>
              </Grid>

              {/* Activities by Assignee */}
              <Grid item xs={12}>
                <Paper variant="outlined" sx={{ p: 2 }}>
                  <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1 }}>Activities by Assignee</Typography>
                  {analytics.countByAssignee && Object.keys(analytics.countByAssignee).length > 0 ? (
                    <ResponsiveContainer width="100%" height={Math.max(200, Object.keys(analytics.countByAssignee).length * 36)}>
                      <BarChart
                        data={Object.entries(analytics.countByAssignee)
                          .sort(([, a], [, b]) => b - a)
                          .map(([name, value]) => ({ name: name.split('@')[0] || name, value }))}
                        layout="vertical"
                      >
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis type="number" />
                        <YAxis dataKey="name" type="category" width={120} />
                        <RTooltip />
                        <Bar dataKey="value" fill="#1976d2" radius={[0, 4, 4, 0]}>
                          {Object.keys(analytics.countByAssignee).map((_, i) => (
                            <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                          ))}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  ) : (
                    <Typography color="text.secondary" variant="body2">No assignee data available</Typography>
                  )}
                </Paper>
              </Grid>
            </Grid>
          </Box>
        )}
      </TabPanel>

      {/* ================================================================
         TAB 4 – Calendar Sync
         ================================================================ */}
      <TabPanel value={mainTab} index={4}>
        <Grid container spacing={3}>
          {/* iCal Feed Subscription */}
          <Grid item xs={12} md={6}>
            <Paper variant="outlined" sx={{ p: 3 }}>
              <Typography variant="h6" sx={{ mb: 1 }}>📅 iCal Feed Subscription</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Generate a subscription URL to sync your CRM activities with Google Calendar, Outlook, or Apple Calendar.
                Add this URL as a &quot;Subscribe to calendar&quot; in your calendar app.
              </Typography>

              {/* Create a new token */}
              <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
                <TextField
                  size="small"
                  label="Feed Name"
                  placeholder="e.g., Work CRM"
                  value={newTokenName}
                  onChange={(e) => setNewTokenName(e.target.value)}
                  sx={{ flex: 1 }}
                />
                <Button variant="contained" onClick={handleCreateToken} disabled={!newTokenName.trim()}>
                  Generate
                </Button>
              </Stack>

              {/* Existing tokens */}
              {calTokensLoading ? (
                <Typography color="text.secondary">Loading…</Typography>
              ) : calTokens.length === 0 ? (
                <Typography color="text.secondary" variant="body2">No feed tokens yet. Create one above to get started.</Typography>
              ) : (
                <List dense disablePadding>
                  {calTokens.map((t) => (
                    <ListItem
                      key={t.id}
                      secondaryAction={
                        <Stack direction="row" spacing={0.5}>
                          <Tooltip title={copiedToken === t.token ? 'Copied!' : 'Copy feed URL'}>
                            <IconButton size="small" onClick={() => handleCopyFeedUrl(t.token)}>
                              <CopyIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Revoke token">
                            <IconButton size="small" color="error" onClick={() => handleRevokeToken(t.id)}>
                              <LinkOffIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      }
                    >
                      <ListItemIcon><SyncIcon /></ListItemIcon>
                      <ListItemText
                        primary={t.name}
                        secondary={`Created: ${new Date(t.createdAt).toLocaleDateString()}${t.lastAccessedAt ? ` · Last used: ${new Date(t.lastAccessedAt).toLocaleDateString()}` : ''}`}
                      />
                    </ListItem>
                  ))}
                </List>
              )}
            </Paper>
          </Grid>

          {/* Export / Download */}
          <Grid item xs={12} md={6}>
            <Paper variant="outlined" sx={{ p: 3 }}>
              <Typography variant="h6" sx={{ mb: 1 }}>⬇️ Export Calendar</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Download all your activities as an .ics file that you can import into any calendar application.
              </Typography>
              <Button variant="outlined" startIcon={<ScheduleIcon />} onClick={handleExportIcs}>
                Download .ics File
              </Button>
            </Paper>

            <Paper variant="outlined" sx={{ p: 3, mt: 2 }}>
              <Typography variant="h6" sx={{ mb: 1 }}>🔗 How to Subscribe</Typography>
              <Typography variant="body2" color="text.secondary" component="div">
                <ol style={{ margin: 0, paddingLeft: 20 }}>
                  <li><strong>Google Calendar:</strong> Settings → Add calendar → From URL → Paste the feed URL</li>
                  <li><strong>Outlook:</strong> Add calendar → Subscribe from web → Paste the feed URL</li>
                  <li><strong>Apple Calendar:</strong> File → New Calendar Subscription → Paste the feed URL</li>
                </ol>
              </Typography>
            </Paper>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ================================================================
         Create / Edit Dialog
         ================================================================ */}
      <Dialog open={formOpen} onClose={() => setFormOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          <Stack direction="row" spacing={1} alignItems="center">
            {typeIcons[formData.type as ActivityType]}
            <span>{editingId ? 'Edit' : 'Create'} {formData.type === 'CALL' ? 'Call Log' : formData.type === 'EMAIL' ? 'Email Log' : formData.type === 'MEETING' ? 'Meeting' : 'Task'}</span>
          </Stack>
        </DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 1 }}>
            {/* Common fields */}
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth size="small">
                <InputLabel>Type</InputLabel>
                <Select value={formData.type} label="Type" onChange={ff('type')}>
                  {TYPE_OPTIONS.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth size="small" label="Subject" value={formData.subject} onChange={ff('subject')} required />
            </Grid>
            <Grid item xs={12}>
              <Stack direction="row" spacing={1} alignItems="flex-start">
                <TextField fullWidth size="small" label="Description" value={formData.description} onChange={ff('description')} multiline rows={2} />
                <VoiceInput onTranscript={(t) => setFormData(prev => ({ ...prev, description: prev.description ? prev.description + ' ' + t : t }))} tooltip="Dictate description" />
              </Stack>
            </Grid>
            <Grid item xs={12} sm={4}>
              <FormControl fullWidth size="small">
                <InputLabel>Status</InputLabel>
                <Select value={formData.status} label="Status" onChange={ff('status')}>
                  {STATUS_OPTIONS.map((s) => <MenuItem key={s} value={s}>{s.replace('_', ' ')}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={4}>
              <FormControl fullWidth size="small">
                <InputLabel>Priority</InputLabel>
                <Select value={formData.priority} label="Priority" onChange={ff('priority')}>
                  {PRIORITY_OPTIONS.map((p) => <MenuItem key={p} value={p}>{p}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField fullWidth size="small" label="Assigned To" value={formData.assignedTo} onChange={ff('assignedTo')} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth size="small" label="Due Date" type="datetime-local" value={formData.dueDate} onChange={ff('dueDate')} InputLabelProps={{ shrink: true }} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth size="small" label="Reminder At" type="datetime-local" value={formData.reminderAt} onChange={ff('reminderAt')} InputLabelProps={{ shrink: true }} />
            </Grid>

            {/* Related entity */}
            <Grid item xs={12} sm={6}>
              <TextField fullWidth size="small" label="Related Entity Type" value={formData.relatedEntityType} onChange={ff('relatedEntityType')} placeholder="e.g., Lead, Account" />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth size="small" label="Related Entity ID" value={formData.relatedEntityId} onChange={ff('relatedEntityId')} />
            </Grid>

            {/* Meeting-specific fields */}
            {(formData.type === 'MEETING' || formData.type === 'CALL') && (
              <>
                <Grid item xs={12}><Divider><Chip label={formData.type === 'MEETING' ? 'Meeting Details' : 'Call Details'} size="small" /></Divider></Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth size="small" label="Start Time" type="datetime-local" value={formData.startTime} onChange={ff('startTime')} InputLabelProps={{ shrink: true }} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth size="small" label="End Time" type="datetime-local" value={formData.endTime} onChange={ff('endTime')} InputLabelProps={{ shrink: true }} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth size="small" label="Location" value={formData.location} onChange={ff('location')} />
                </Grid>
              </>
            )}

            {/* Call-specific fields */}
            {formData.type === 'CALL' && (
              <>
                <Grid item xs={12} sm={3}>
                  <TextField fullWidth size="small" label="Duration (min)" type="number" value={formData.callDurationMinutes} onChange={ff('callDurationMinutes')} />
                </Grid>
                <Grid item xs={12} sm={3}>
                  <TextField fullWidth size="small" label="Call Outcome" value={formData.callOutcome} onChange={ff('callOutcome')} placeholder="e.g., Connected, Voicemail" />
                </Grid>
              </>
            )}

            {/* Email-specific fields */}
            {formData.type === 'EMAIL' && (
              <>
                <Grid item xs={12}><Divider><Chip label="Email Details" size="small" /></Divider></Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth size="small" label="To" value={formData.emailTo} onChange={ff('emailTo')} placeholder="recipient@example.com" />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth size="small" label="CC" value={formData.emailCc} onChange={ff('emailCc')} placeholder="cc@example.com" />
                </Grid>
              </>
            )}

            {/* Recurrence */}
            <Grid item xs={12}><Divider><Chip label="Recurrence" size="small" /></Divider></Grid>
            <Grid item xs={12} sm={4}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.enableRecurrence}
                    onChange={(e) => setFormData((p: any) => ({ ...p, enableRecurrence: e.target.checked }))}
                  />
                }
                label="Recurring"
              />
            </Grid>
            {formData.enableRecurrence && (
              <>
                <Grid item xs={12} sm={4}>
                  <FormControl fullWidth size="small">
                    <InputLabel>Frequency</InputLabel>
                    <Select value={formData.recurrenceRule} label="Frequency" onChange={ff('recurrenceRule')}>
                      {RECURRENCE_OPTIONS.map((r) => <MenuItem key={r} value={r}>{r}</MenuItem>)}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField fullWidth size="small" label="Recurrence End" type="date" value={formData.recurrenceEnd} onChange={ff('recurrenceEnd')} InputLabelProps={{ shrink: true }} />
                </Grid>
              </>
            )}
          </Grid>

          {/* AI Summary Result */}
          {aiSummary && (
            <Paper variant="outlined" sx={{ mt: 2, p: 2, bgcolor: 'action.hover' }}>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                <AiIcon color="primary" fontSize="small" />
                <Typography variant="subtitle2">AI Meeting Summary</Typography>
              </Stack>
              <Typography variant="body2" sx={{ mb: 1 }}>{aiSummary.summary}</Typography>
              {aiSummary.actionItems.length > 0 && (
                <>
                  <Typography variant="caption" fontWeight={600}>Action Items:</Typography>
                  <List dense disablePadding>
                    {aiSummary.actionItems.map((item, i) => (
                      <ListItem key={i} disableGutters sx={{ py: 0 }}>
                        <ListItemIcon sx={{ minWidth: 24 }}><Typography variant="caption">{i + 1}.</Typography></ListItemIcon>
                        <ListItemText primary={<Typography variant="body2">{item}</Typography>} />
                      </ListItem>
                    ))}
                  </List>
                </>
              )}
              {aiSummary.keyDecisions.length > 0 && (
                <>
                  <Typography variant="caption" fontWeight={600} sx={{ mt: 1 }}>Key Decisions:</Typography>
                  <List dense disablePadding>
                    {aiSummary.keyDecisions.map((dec, i) => (
                      <ListItem key={i} disableGutters sx={{ py: 0 }}>
                        <ListItemIcon sx={{ minWidth: 24 }}><Typography variant="caption">•</Typography></ListItemIcon>
                        <ListItemText primary={<Typography variant="body2">{dec}</Typography>} />
                      </ListItem>
                    ))}
                  </List>
                </>
              )}
            </Paper>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setFormOpen(false); setAiSummary(null); }}>Cancel</Button>
          {(formData.type === 'MEETING' || formData.type === 'CALL') && (
            <Button startIcon={<AiIcon />} onClick={handleAiSummarize} disabled={aiSummaryLoading || !formData.subject}>
              {aiSummaryLoading ? 'Summarizing…' : 'AI Summarize'}
            </Button>
          )}
          <Button variant="contained" onClick={handleSave} disabled={saving || !formData.subject}>
            {saving ? 'Saving…' : editingId ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deleteId}
        title="Delete Activity"
        message="Are you sure you want to delete this activity? This action cannot be undone."
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </Box>
  );
};

export default ActivitiesPage;
