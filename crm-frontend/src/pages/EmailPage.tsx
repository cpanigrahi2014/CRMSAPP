/* ============================================================
   EmailPage – Full Email Integration (8 features)
   Features: Gmail/Outlook integration, Email tracking, Templates,
   Automation triggers, Conversation logging, Scheduling, Analytics
   ============================================================ */
import React, { useEffect, useState, useCallback } from 'react';
import {
  GridColDef,
  GridRenderCellParams,
} from '@mui/x-data-grid';
import {
  TextField, Grid, IconButton, Tooltip, Stack, Box, Typography, Chip,
  Button, Tab, Tabs, Dialog, DialogTitle, DialogContent, DialogActions,
  Divider, FormControlLabel, Switch, Select, MenuItem, FormControl,
  InputLabel, Paper, Alert,
} from '@mui/material';
import {
  Edit as EditIcon, Delete as DeleteIcon, Send as SendIcon,
  Add as AddIcon, Email as EmailIcon, Schedule as ScheduleIcon,
  Analytics as AnalyticsIcon, Drafts as TemplateIcon,
  AccountCircle as AccountIcon, Visibility as ViewIcon,
  Cancel as CancelIcon, LinkOff as DisconnectIcon,
  Star as DefaultIcon, OpenInNew as OpenIcon,
  TrendingUp as TrendingUpIcon, MarkEmailRead as ReadIcon,
  Mouse as ClickIcon, ErrorOutline as BounceIcon,
  CheckCircle as DeliveredIcon,
} from '@mui/icons-material';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip as RTooltip, ResponsiveContainer, Legend,
  BarChart, Bar,
} from 'recharts';
import { DataTable, PageHeader, ConfirmDialog, MetricCard } from '../components';
import { emailService } from '../services';
import type {
  EmailAccount, CreateEmailAccountRequest,
  EmailTemplate, CreateEmailTemplateRequest, UpdateEmailTemplateRequest,
  EmailMessage, SendEmailRequest,
  EmailSchedule, EmailAnalytics, EmailTrackingEvent,
} from '../types';
import { useSnackbar } from 'notistack';

/* ── Constants ────────────────────────────────────────────── */
const STATUS_COLORS: Record<string, 'success' | 'warning' | 'error' | 'info' | 'default'> = {
  SENT: 'success', DELIVERED: 'success', QUEUED: 'warning',
  SENDING: 'info', FAILED: 'error', DRAFT: 'default', RECEIVED: 'info',
  PENDING: 'warning', CANCELLED: 'default',
};

/* ── Tab Panel helper ─────────────────────────────────────── */
function TabPanel({ children, value, index }: { children: React.ReactNode; value: number; index: number }) {
  return value === index ? <Box sx={{ pt: 2 }}>{children}</Box> : null;
}

/* ── Empty forms ──────────────────────────────────────────── */
const emptySmtpForm = (): CreateEmailAccountRequest => ({
  provider: 'SMTP', email: '', displayName: '', isDefault: false,
  smtpHost: '', smtpPort: 587, smtpUsername: '', smtpPassword: '',
});

const emptyTemplateForm = (): CreateEmailTemplateRequest => ({
  name: '', subject: '', bodyHtml: '', bodyText: '', category: '',
});

const emptySendForm = (): SendEmailRequest => ({
  to: '', cc: '', bcc: '', subject: '', bodyHtml: '', bodyText: '',
  trackOpens: true, trackClicks: true,
});

/* ============================================================ */
const EmailPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [tab, setTab] = useState(0);

  /* ── State: Accounts ─────────────────────── */
  const [accounts, setAccounts] = useState<EmailAccount[]>([]);
  const [accountDlg, setAccountDlg] = useState(false);
  const [smtpForm, setSmtpForm] = useState(emptySmtpForm());

  /* ── State: Templates ────────────────────── */
  const [templates, setTemplates] = useState<EmailTemplate[]>([]);
  const [totalTemplates, setTotalTemplates] = useState(0);
  const [templateDlg, setTemplateDlg] = useState(false);
  const [editTemplateId, setEditTemplateId] = useState<string | null>(null);
  const [templateForm, setTemplateForm] = useState(emptyTemplateForm());
  const [previewDlg, setPreviewDlg] = useState(false);
  const [previewHtml, setPreviewHtml] = useState('');

  /* ── State: Messages ─────────────────────── */
  const [messages, setMessages] = useState<EmailMessage[]>([]);
  const [totalMessages, setTotalMessages] = useState(0);
  const [sendDlg, setSendDlg] = useState(false);
  const [sendForm, setSendForm] = useState(emptySendForm());
  const [viewDlg, setViewDlg] = useState(false);
  const [selectedMsg, setSelectedMsg] = useState<EmailMessage | null>(null);
  const [trackingEvents, setTrackingEvents] = useState<EmailTrackingEvent[]>([]);

  /* ── State: Schedules ────────────────────── */
  const [schedules, setSchedules] = useState<EmailSchedule[]>([]);
  const [totalSchedules, setTotalSchedules] = useState(0);

  /* ── State: Analytics ────────────────────── */
  const [analytics, setAnalytics] = useState<EmailAnalytics | null>(null);

  /* ── State: Misc ─────────────────────────── */
  const [loading, setLoading] = useState(false);
  const [confirmDlg, setConfirmDlg] = useState<{ open: boolean; title: string; message: string; onConfirm: () => void }>({
    open: false, title: '', message: '', onConfirm: () => {},
  });

  /* ── Loaders ────────────────────────────────────────────── */
  const loadAccounts = useCallback(async () => {
    try { setAccounts(await emailService.getAccounts()); }
    catch { enqueueSnackbar('Failed to load accounts', { variant: 'error' }); }
  }, [enqueueSnackbar]);

  const loadTemplates = useCallback(async () => {
    try {
      const d = await emailService.getTemplates(0, 100);
      setTemplates(d.content || []); setTotalTemplates(d.totalElements || 0);
    } catch { enqueueSnackbar('Failed to load templates', { variant: 'error' }); }
  }, [enqueueSnackbar]);

  const loadMessages = useCallback(async () => {
    try {
      const d = await emailService.getSentMessages(0, 100);
      setMessages(d.content || []); setTotalMessages(d.totalElements || 0);
    } catch { enqueueSnackbar('Failed to load messages', { variant: 'error' }); }
  }, [enqueueSnackbar]);

  const loadSchedules = useCallback(async () => {
    try {
      const d = await emailService.getSchedules(0, 100);
      setSchedules(d.content || []); setTotalSchedules(d.totalElements || 0);
    } catch { enqueueSnackbar('Failed to load schedules', { variant: 'error' }); }
  }, [enqueueSnackbar]);

  const loadAnalytics = useCallback(async () => {
    try { setAnalytics(await emailService.getAnalytics()); }
    catch { enqueueSnackbar('Failed to load analytics', { variant: 'error' }); }
  }, [enqueueSnackbar]);

  useEffect(() => {
    setLoading(true);
    Promise.all([loadAccounts(), loadTemplates(), loadMessages(), loadSchedules(), loadAnalytics()])
      .finally(() => setLoading(false));
  }, [loadAccounts, loadTemplates, loadMessages, loadSchedules, loadAnalytics]);

  /* ── Account Actions ────────────────────────────────────── */
  const handleCreateSmtp = async () => {
    try {
      await emailService.createSmtpAccount(smtpForm);
      enqueueSnackbar('SMTP account connected', { variant: 'success' });
      setAccountDlg(false); setSmtpForm(emptySmtpForm()); loadAccounts();
    } catch { enqueueSnackbar('Failed to create account', { variant: 'error' }); }
  };

  const handleConnectGmail = async () => {
    try {
      const res = await emailService.getGmailAuthUrl();
      window.open(res.authorizationUrl, '_blank');
      enqueueSnackbar('Gmail authorization page opened', { variant: 'info' });
    } catch (e: unknown) {
      enqueueSnackbar(e instanceof Error ? e.message : 'Gmail not configured', { variant: 'warning' });
    }
  };

  const handleConnectOutlook = async () => {
    try {
      const res = await emailService.getOutlookAuthUrl();
      window.open(res.authorizationUrl, '_blank');
      enqueueSnackbar('Outlook authorization page opened', { variant: 'info' });
    } catch (e: unknown) {
      enqueueSnackbar(e instanceof Error ? e.message : 'Outlook not configured', { variant: 'warning' });
    }
  };

  const handleDisconnectAccount = (id: string) => {
    setConfirmDlg({
      open: true, title: 'Disconnect Account', message: 'Disconnect this email account?',
      onConfirm: async () => {
        try { await emailService.disconnectAccount(id); loadAccounts(); enqueueSnackbar('Disconnected', { variant: 'success' }); }
        catch { enqueueSnackbar('Failed', { variant: 'error' }); }
        setConfirmDlg(p => ({ ...p, open: false }));
      }
    });
  };

  const handleDeleteAccount = (id: string) => {
    setConfirmDlg({
      open: true, title: 'Delete Account', message: 'Delete this email account?',
      onConfirm: async () => {
        try { await emailService.deleteAccount(id); loadAccounts(); enqueueSnackbar('Deleted', { variant: 'success' }); }
        catch { enqueueSnackbar('Failed', { variant: 'error' }); }
        setConfirmDlg(p => ({ ...p, open: false }));
      }
    });
  };

  /* ── Template Actions ───────────────────────────────────── */
  const openCreateTemplate = () => { setEditTemplateId(null); setTemplateForm(emptyTemplateForm()); setTemplateDlg(true); };
  const openEditTemplate = (t: EmailTemplate) => {
    setEditTemplateId(t.id);
    setTemplateForm({ name: t.name, subject: t.subject, bodyHtml: t.bodyHtml || '', bodyText: t.bodyText || '', category: t.category || '' });
    setTemplateDlg(true);
  };

  const handleSaveTemplate = async () => {
    try {
      if (editTemplateId) {
        await emailService.updateTemplate(editTemplateId, templateForm as UpdateEmailTemplateRequest);
        enqueueSnackbar('Template updated', { variant: 'success' });
      } else {
        await emailService.createTemplate(templateForm);
        enqueueSnackbar('Template created', { variant: 'success' });
      }
      setTemplateDlg(false); loadTemplates();
    } catch { enqueueSnackbar('Failed to save template', { variant: 'error' }); }
  };

  const handleDeleteTemplate = (id: string) => {
    setConfirmDlg({
      open: true, title: 'Delete Template', message: 'Delete this email template?',
      onConfirm: async () => {
        try { await emailService.deleteTemplate(id); loadTemplates(); enqueueSnackbar('Deleted', { variant: 'success' }); }
        catch { enqueueSnackbar('Failed', { variant: 'error' }); }
        setConfirmDlg(p => ({ ...p, open: false }));
      }
    });
  };

  const handlePreviewTemplate = async (id: string) => {
    try {
      const r = await emailService.previewTemplate(id);
      setPreviewHtml(`<h3>${r.subject}</h3><hr/>${r.bodyHtml}`);
      setPreviewDlg(true);
    } catch { enqueueSnackbar('Preview failed', { variant: 'error' }); }
  };

  /* ── Send Email Actions ─────────────────────────────────── */
  const openSendDlg = () => { setSendForm(emptySendForm()); setSendDlg(true); };

  const handleSend = async () => {
    try {
      await emailService.sendEmail(sendForm);
      enqueueSnackbar('Email sent!', { variant: 'success' });
      setSendDlg(false); loadMessages(); loadAnalytics();
    } catch { enqueueSnackbar('Failed to send email', { variant: 'error' }); }
  };

  /* ── View Message + Tracking ────────────────────────────── */
  const handleViewMessage = async (msg: EmailMessage) => {
    setSelectedMsg(msg);
    try { setTrackingEvents(await emailService.getTrackingEvents(msg.id)); }
    catch { setTrackingEvents([]); }
    setViewDlg(true);
  };

  /* ── Schedule Actions ───────────────────────────────────── */
  const handleCancelSchedule = (id: string) => {
    setConfirmDlg({
      open: true, title: 'Cancel Schedule', message: 'Cancel this scheduled email?',
      onConfirm: async () => {
        try { await emailService.cancelSchedule(id); loadSchedules(); enqueueSnackbar('Cancelled', { variant: 'success' }); }
        catch { enqueueSnackbar('Failed', { variant: 'error' }); }
        setConfirmDlg(p => ({ ...p, open: false }));
      }
    });
  };

  /* ── Grid Column Definitions ────────────────────────────── */

  const accountCols: GridColDef[] = [
    { field: 'provider', headerName: 'Provider', width: 120, renderCell: (p: GridRenderCellParams) =>
      <Chip label={p.value} color={p.value === 'GMAIL' ? 'error' : p.value === 'OUTLOOK' ? 'primary' : 'default'} size="small" /> },
    { field: 'email', headerName: 'Email', flex: 1 },
    { field: 'displayName', headerName: 'Display Name', width: 180 },
    { field: 'connected', headerName: 'Status', width: 120, renderCell: (p: GridRenderCellParams) =>
      <Chip label={p.value ? 'Connected' : 'Disconnected'} color={p.value ? 'success' : 'default'} size="small" /> },
    { field: 'isDefault', headerName: 'Default', width: 100, renderCell: (p: GridRenderCellParams) =>
      p.value ? <DefaultIcon color="warning" /> : null },
    {
      field: 'actions', headerName: 'Actions', width: 150, sortable: false,
      renderCell: (p: GridRenderCellParams) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="Disconnect"><IconButton size="small" onClick={() => handleDisconnectAccount(p.row.id)}><DisconnectIcon /></IconButton></Tooltip>
          <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => handleDeleteAccount(p.row.id)}><DeleteIcon /></IconButton></Tooltip>
        </Stack>
      ),
    },
  ];

  const templateCols: GridColDef[] = [
    { field: 'name', headerName: 'Name', flex: 1 },
    { field: 'subject', headerName: 'Subject', flex: 1.5 },
    { field: 'category', headerName: 'Category', width: 130 },
    { field: 'isActive', headerName: 'Active', width: 90, renderCell: (p: GridRenderCellParams) =>
      <Chip label={p.value ? 'Yes' : 'No'} color={p.value ? 'success' : 'default'} size="small" /> },
    { field: 'usageCount', headerName: 'Uses', width: 80 },
    {
      field: 'actions', headerName: 'Actions', width: 180, sortable: false,
      renderCell: (p: GridRenderCellParams) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="Preview"><IconButton size="small" onClick={() => handlePreviewTemplate(p.row.id)}><ViewIcon /></IconButton></Tooltip>
          <Tooltip title="Edit"><IconButton size="small" onClick={() => openEditTemplate(p.row)}><EditIcon /></IconButton></Tooltip>
          <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => handleDeleteTemplate(p.row.id)}><DeleteIcon /></IconButton></Tooltip>
        </Stack>
      ),
    },
  ];

  const messageCols: GridColDef[] = [
    { field: 'toAddresses', headerName: 'To', flex: 1 },
    { field: 'subject', headerName: 'Subject', flex: 1.5 },
    { field: 'status', headerName: 'Status', width: 120, renderCell: (p: GridRenderCellParams) =>
      <Chip label={p.value} color={STATUS_COLORS[p.value as string] || 'default'} size="small" /> },
    { field: 'opened', headerName: 'Opened', width: 90, renderCell: (p: GridRenderCellParams) =>
      p.value ? <Chip label={`${p.row.openCount}x`} color="info" size="small" icon={<ReadIcon />} /> : <Chip label="No" size="small" /> },
    { field: 'clickCount', headerName: 'Clicks', width: 80 },
    { field: 'sentAt', headerName: 'Sent At', width: 170, valueFormatter: (value: string) =>
      value ? new Date(value).toLocaleString() : '—' },
    {
      field: 'actions', headerName: '', width: 60, sortable: false,
      renderCell: (p: GridRenderCellParams) => (
        <Tooltip title="View"><IconButton size="small" onClick={() => handleViewMessage(p.row)}><ViewIcon /></IconButton></Tooltip>
      ),
    },
  ];

  const scheduleCols: GridColDef[] = [
    { field: 'toAddresses', headerName: 'To', flex: 1 },
    { field: 'subject', headerName: 'Subject', flex: 1.5 },
    { field: 'scheduledAt', headerName: 'Scheduled For', width: 170, valueFormatter: (value: string) =>
      value ? new Date(value).toLocaleString() : '—' },
    { field: 'status', headerName: 'Status', width: 120, renderCell: (p: GridRenderCellParams) =>
      <Chip label={p.value} color={STATUS_COLORS[p.value as string] || 'default'} size="small" /> },
    {
      field: 'actions', headerName: '', width: 80, sortable: false,
      renderCell: (p: GridRenderCellParams) =>
        p.row.status === 'PENDING' ? (
          <Tooltip title="Cancel"><IconButton size="small" color="warning" onClick={() => handleCancelSchedule(p.row.id)}><CancelIcon /></IconButton></Tooltip>
        ) : null,
    },
  ];

  /* ── Analytics chart data ───────────────────────────────── */
  const chartData = analytics
    ? Object.entries(analytics.sentByDay).map(([day, sent]) => ({
        day: day.slice(5), // MM-DD
        Sent: sent,
        Opens: analytics.opensByDay[day] || 0,
        Clicks: analytics.clicksByDay[day] || 0,
      }))
    : [];

  /* ============================================================
     Render
     ============================================================ */
  return (
    <Box>
      <PageHeader
        title="Email Integration"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Email' }]}
      />

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab icon={<AccountIcon />} label="Accounts" iconPosition="start" />
          <Tab icon={<TemplateIcon />} label="Templates" iconPosition="start" />
          <Tab icon={<EmailIcon />} label="Sent Mail" iconPosition="start" />
          <Tab icon={<ScheduleIcon />} label="Scheduled" iconPosition="start" />
          <Tab icon={<AnalyticsIcon />} label="Analytics" iconPosition="start" />
        </Tabs>
      </Box>

      {/* ── Tab 0: Accounts ──────────────────────────────── */}
      <TabPanel value={tab} index={0}>
        <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAccountDlg(true)}>Add SMTP</Button>
          <Button variant="outlined" color="error" onClick={handleConnectGmail}>Connect Gmail</Button>
          <Button variant="outlined" color="primary" onClick={handleConnectOutlook}>Connect Outlook</Button>
        </Stack>
        <DataTable title="Connected Accounts" rows={accounts} columns={accountCols} loading={loading} />
      </TabPanel>

      {/* ── Tab 1: Templates ─────────────────────────────── */}
      <TabPanel value={tab} index={1}>
        <DataTable
          title="Email Templates"
          rows={templates}
          columns={templateCols}
          loading={loading}
          onAdd={openCreateTemplate}
          addLabel="New Template"
          rowCount={totalTemplates}
        />
      </TabPanel>

      {/* ── Tab 2: Sent Mail ─────────────────────────────── */}
      <TabPanel value={tab} index={2}>
        <DataTable
          title="Sent Emails"
          rows={messages}
          columns={messageCols}
          loading={loading}
          onAdd={openSendDlg}
          addLabel="Compose"
          rowCount={totalMessages}
        />
      </TabPanel>

      {/* ── Tab 3: Scheduled ─────────────────────────────── */}
      <TabPanel value={tab} index={3}>
        <DataTable
          title="Scheduled Emails"
          rows={schedules}
          columns={scheduleCols}
          loading={loading}
          rowCount={totalSchedules}
        />
      </TabPanel>

      {/* ── Tab 4: Analytics ─────────────────────────────── */}
      <TabPanel value={tab} index={4}>
        {analytics ? (
          <>
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12} sm={6} md={2}>
                <MetricCard title="Sent" value={analytics.totalSent} icon={<SendIcon />} color="#1976d2" />
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <MetricCard title="Delivered" value={analytics.totalDelivered} icon={<DeliveredIcon />} color="#2e7d32" />
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <MetricCard title="Opened" value={analytics.totalOpened} icon={<ReadIcon />} color="#ed6c02" />
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <MetricCard title="Clicked" value={analytics.totalClicked} icon={<ClickIcon />} color="#9c27b0" />
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <MetricCard title="Bounced" value={analytics.totalBounced} icon={<BounceIcon />} color="#d32f2f" />
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <MetricCard title="Failed" value={analytics.totalFailed} icon={<BounceIcon />} color="#616161" />
              </Grid>
            </Grid>

            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12} sm={6} md={3}>
                <Paper sx={{ p: 2, textAlign: 'center' }}>
                  <Typography variant="h4" color="primary">{analytics.openRate}%</Typography>
                  <Typography variant="body2" color="text.secondary">Open Rate</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Paper sx={{ p: 2, textAlign: 'center' }}>
                  <Typography variant="h4" color="secondary">{analytics.clickRate}%</Typography>
                  <Typography variant="body2" color="text.secondary">Click Rate</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Paper sx={{ p: 2, textAlign: 'center' }}>
                  <Typography variant="h4" color="error">{analytics.bounceRate}%</Typography>
                  <Typography variant="body2" color="text.secondary">Bounce Rate</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Paper sx={{ p: 2, textAlign: 'center' }}>
                  <Typography variant="h4" color="success.main">{analytics.deliveryRate}%</Typography>
                  <Typography variant="body2" color="text.secondary">Delivery Rate</Typography>
                </Paper>
              </Grid>
            </Grid>

            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>Email Activity (Last 30 Days)</Typography>
              <ResponsiveContainer width="100%" height={350}>
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="day" />
                  <YAxis />
                  <RTooltip />
                  <Legend />
                  <Line type="monotone" dataKey="Sent" stroke="#1976d2" strokeWidth={2} />
                  <Line type="monotone" dataKey="Opens" stroke="#ed6c02" strokeWidth={2} />
                  <Line type="monotone" dataKey="Clicks" stroke="#9c27b0" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </Paper>
          </>
        ) : (
          <Alert severity="info">Loading analytics...</Alert>
        )}
      </TabPanel>

      {/* ═══════════════════════════════════════════════════════
          Dialogs
          ═══════════════════════════════════════════════════════ */}

      {/* ── SMTP Account Dialog ───────────────────────────── */}
      <Dialog open={accountDlg} onClose={() => setAccountDlg(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add SMTP Account</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Email" fullWidth value={smtpForm.email}
              onChange={e => setSmtpForm(f => ({ ...f, email: e.target.value }))} />
            <TextField label="Display Name" fullWidth value={smtpForm.displayName}
              onChange={e => setSmtpForm(f => ({ ...f, displayName: e.target.value }))} />
            <TextField label="SMTP Host" fullWidth value={smtpForm.smtpHost}
              onChange={e => setSmtpForm(f => ({ ...f, smtpHost: e.target.value }))} />
            <TextField label="SMTP Port" type="number" fullWidth value={smtpForm.smtpPort}
              onChange={e => setSmtpForm(f => ({ ...f, smtpPort: Number(e.target.value) }))} />
            <TextField label="Username" fullWidth value={smtpForm.smtpUsername}
              onChange={e => setSmtpForm(f => ({ ...f, smtpUsername: e.target.value }))} />
            <TextField label="Password" type="password" fullWidth value={smtpForm.smtpPassword}
              onChange={e => setSmtpForm(f => ({ ...f, smtpPassword: e.target.value }))} />
            <FormControlLabel control={<Switch checked={smtpForm.isDefault} onChange={e => setSmtpForm(f => ({ ...f, isDefault: e.target.checked }))} />} label="Set as Default" />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAccountDlg(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateSmtp} disabled={!smtpForm.email}>Save</Button>
        </DialogActions>
      </Dialog>

      {/* ── Template Dialog ───────────────────────────────── */}
      <Dialog open={templateDlg} onClose={() => setTemplateDlg(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editTemplateId ? 'Edit Template' : 'New Template'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth value={templateForm.name}
              onChange={e => setTemplateForm(f => ({ ...f, name: e.target.value }))} />
            <TextField label="Subject" fullWidth value={templateForm.subject}
              onChange={e => setTemplateForm(f => ({ ...f, subject: e.target.value }))} />
            <TextField label="Category" fullWidth value={templateForm.category}
              onChange={e => setTemplateForm(f => ({ ...f, category: e.target.value }))} />
            <TextField label="Body (HTML)" fullWidth multiline rows={10} value={templateForm.bodyHtml}
              onChange={e => setTemplateForm(f => ({ ...f, bodyHtml: e.target.value }))}
              helperText="Use {{variableName}} for template variables" />
            <TextField label="Body (Plain Text)" fullWidth multiline rows={4} value={templateForm.bodyText}
              onChange={e => setTemplateForm(f => ({ ...f, bodyText: e.target.value }))} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTemplateDlg(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveTemplate} disabled={!templateForm.name || !templateForm.subject}>Save</Button>
        </DialogActions>
      </Dialog>

      {/* ── Template Preview Dialog ───────────────────────── */}
      <Dialog open={previewDlg} onClose={() => setPreviewDlg(false)} maxWidth="md" fullWidth>
        <DialogTitle>Template Preview</DialogTitle>
        <DialogContent>
          <Box dangerouslySetInnerHTML={{ __html: previewHtml }} sx={{ mt: 1 }} />
        </DialogContent>
        <DialogActions><Button onClick={() => setPreviewDlg(false)}>Close</Button></DialogActions>
      </Dialog>

      {/* ── Compose / Send Dialog ─────────────────────────── */}
      <Dialog open={sendDlg} onClose={() => setSendDlg(false)} maxWidth="md" fullWidth>
        <DialogTitle>Compose Email</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="To" fullWidth required value={sendForm.to}
              onChange={e => setSendForm(f => ({ ...f, to: e.target.value }))} />
            <TextField label="CC" fullWidth value={sendForm.cc}
              onChange={e => setSendForm(f => ({ ...f, cc: e.target.value }))} />
            <TextField label="BCC" fullWidth value={sendForm.bcc}
              onChange={e => setSendForm(f => ({ ...f, bcc: e.target.value }))} />
            <TextField label="Subject" fullWidth required value={sendForm.subject}
              onChange={e => setSendForm(f => ({ ...f, subject: e.target.value }))} />
            <TextField label="Body (HTML)" fullWidth multiline rows={8} value={sendForm.bodyHtml}
              onChange={e => setSendForm(f => ({ ...f, bodyHtml: e.target.value }))} />
            <Divider />
            <Stack direction="row" spacing={2}>
              <FormControlLabel control={<Switch checked={sendForm.trackOpens} onChange={e => setSendForm(f => ({ ...f, trackOpens: e.target.checked }))} />} label="Track Opens" />
              <FormControlLabel control={<Switch checked={sendForm.trackClicks} onChange={e => setSendForm(f => ({ ...f, trackClicks: e.target.checked }))} />} label="Track Clicks" />
            </Stack>
            <TextField label="Schedule (optional)" type="datetime-local" fullWidth
              InputLabelProps={{ shrink: true }}
              value={sendForm.scheduledAt || ''}
              onChange={e => setSendForm(f => ({ ...f, scheduledAt: e.target.value || undefined }))} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSendDlg(false)}>Cancel</Button>
          <Button variant="contained" startIcon={<SendIcon />} onClick={handleSend}
            disabled={!sendForm.to || !sendForm.subject}>
            {sendForm.scheduledAt ? 'Schedule' : 'Send'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* ── Message View + Tracking Dialog ────────────────── */}
      <Dialog open={viewDlg} onClose={() => setViewDlg(false)} maxWidth="md" fullWidth>
        <DialogTitle>Email Details</DialogTitle>
        <DialogContent>
          {selectedMsg && (
            <Stack spacing={2} sx={{ mt: 1 }}>
              <Typography><strong>To:</strong> {selectedMsg.toAddresses}</Typography>
              {selectedMsg.ccAddresses && <Typography><strong>CC:</strong> {selectedMsg.ccAddresses}</Typography>}
              <Typography><strong>Subject:</strong> {selectedMsg.subject}</Typography>
              <Typography><strong>Status:</strong>{' '}
                <Chip label={selectedMsg.status} color={STATUS_COLORS[selectedMsg.status] || 'default'} size="small" />
              </Typography>
              <Typography><strong>Sent:</strong> {selectedMsg.sentAt ? new Date(selectedMsg.sentAt).toLocaleString() : 'Not yet'}</Typography>
              <Divider />
              <Typography variant="subtitle2">Tracking</Typography>
              <Stack direction="row" spacing={2}>
                <Chip icon={<ReadIcon />} label={`Opens: ${selectedMsg.openCount}`} color="info" />
                <Chip icon={<ClickIcon />} label={`Clicks: ${selectedMsg.clickCount}`} color="secondary" />
                {selectedMsg.firstOpenedAt && (
                  <Chip label={`First opened: ${new Date(selectedMsg.firstOpenedAt).toLocaleString()}`} size="small" />
                )}
              </Stack>

              {trackingEvents.length > 0 && (
                <>
                  <Typography variant="subtitle2" sx={{ mt: 1 }}>Event Log</Typography>
                  {trackingEvents.map(ev => (
                    <Typography key={ev.id} variant="body2" sx={{ ml: 1 }}>
                      • {ev.eventType} at {new Date(ev.createdAt).toLocaleString()}
                      {ev.linkUrl && ` → ${ev.linkUrl}`}
                    </Typography>
                  ))}
                </>
              )}

              <Divider />
              <Typography variant="subtitle2">Body</Typography>
              <Paper variant="outlined" sx={{ p: 2, maxHeight: 300, overflow: 'auto' }}>
                {selectedMsg.bodyHtml
                  ? <Box dangerouslySetInnerHTML={{ __html: selectedMsg.bodyHtml }} />
                  : <Typography>{selectedMsg.bodyText}</Typography>}
              </Paper>
            </Stack>
          )}
        </DialogContent>
        <DialogActions><Button onClick={() => setViewDlg(false)}>Close</Button></DialogActions>
      </Dialog>

      {/* ── Confirm Dialog ────────────────────────────────── */}
      <ConfirmDialog
        open={confirmDlg.open}
        title={confirmDlg.title}
        message={confirmDlg.message}
        onConfirm={confirmDlg.onConfirm}
        onCancel={() => setConfirmDlg(p => ({ ...p, open: false }))}
      />
    </Box>
  );
};

export default EmailPage;
