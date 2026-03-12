/* ============================================================
   ScheduledReports – manage scheduled report delivery
   Uses localStorage for schedule persistence + shows UI for
   CRUD of scheduled report configurations.
   ============================================================ */
import React, { useState, useEffect, useCallback } from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  Alert,
  Switch,
  FormControlLabel,
  Stack,
} from '@mui/material';
import {
  Add,
  Delete,
  Edit,
  Schedule,
  Email,
  Description,
} from '@mui/icons-material';

/* ---- Types ---- */
export interface ScheduledReport {
  id: string;
  name: string;
  reportType: 'sales_performance' | 'pipeline_overview' | 'revenue_insights' | 'conversion_analytics' | 'forecast';
  frequency: 'daily' | 'weekly' | 'monthly';
  recipients: string;
  format: 'csv' | 'pdf';
  enabled: boolean;
  createdAt: string;
  lastRun: string | null;
  nextRun: string;
}

const STORAGE_KEY = 'crm-scheduled-reports';

const REPORT_TYPE_LABELS: Record<string, string> = {
  sales_performance: 'Sales Performance',
  pipeline_overview: 'Pipeline Overview',
  revenue_insights: 'Revenue Insights',
  conversion_analytics: 'Conversion Analytics',
  forecast: 'Forecast Report',
};

const FREQUENCY_LABELS: Record<string, string> = {
  daily: 'Daily',
  weekly: 'Weekly',
  monthly: 'Monthly',
};

function computeNextRun(frequency: string): string {
  const now = new Date();
  switch (frequency) {
    case 'daily':
      now.setDate(now.getDate() + 1);
      now.setHours(8, 0, 0, 0);
      break;
    case 'weekly':
      now.setDate(now.getDate() + (7 - now.getDay() + 1));
      now.setHours(8, 0, 0, 0);
      break;
    case 'monthly':
      now.setMonth(now.getMonth() + 1, 1);
      now.setHours(8, 0, 0, 0);
      break;
  }
  return now.toISOString();
}

function generateId(): string {
  return `sr_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

const emptyForm: Omit<ScheduledReport, 'id' | 'createdAt' | 'lastRun' | 'nextRun'> = {
  name: '',
  reportType: 'sales_performance',
  frequency: 'weekly',
  recipients: '',
  format: 'csv',
  enabled: true,
};

const ScheduledReports: React.FC = () => {
  const [schedules, setSchedules] = useState<ScheduledReport[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);

  // Persist to localStorage
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(schedules));
  }, [schedules]);

  const openCreate = () => {
    setEditingId(null);
    setForm({ ...emptyForm });
    setDialogOpen(true);
    setError(null);
  };

  const openEdit = (schedule: ScheduledReport) => {
    setEditingId(schedule.id);
    setForm({
      name: schedule.name,
      reportType: schedule.reportType,
      frequency: schedule.frequency,
      recipients: schedule.recipients,
      format: schedule.format,
      enabled: schedule.enabled,
    });
    setDialogOpen(true);
    setError(null);
  };

  const handleSave = () => {
    if (!form.name.trim()) {
      setError('Report name is required');
      return;
    }
    if (!form.recipients.trim()) {
      setError('At least one recipient email is required');
      return;
    }

    if (editingId) {
      setSchedules((prev) =>
        prev.map((s) =>
          s.id === editingId
            ? { ...s, ...form, nextRun: computeNextRun(form.frequency) }
            : s,
        ),
      );
    } else {
      const newSchedule: ScheduledReport = {
        id: generateId(),
        ...form,
        createdAt: new Date().toISOString(),
        lastRun: null,
        nextRun: computeNextRun(form.frequency),
      };
      setSchedules((prev) => [...prev, newSchedule]);
    }
    setDialogOpen(false);
  };

  const handleDelete = (id: string) => {
    setSchedules((prev) => prev.filter((s) => s.id !== id));
  };

  const handleToggle = (id: string) => {
    setSchedules((prev) =>
      prev.map((s) => (s.id === id ? { ...s, enabled: !s.enabled } : s)),
    );
  };

  const formatDate = (iso: string | null) => {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h6" fontWeight={600}>
            Scheduled Reports
          </Typography>
          <Button variant="contained" startIcon={<Add />} onClick={openCreate}>
            New Schedule
          </Button>
        </Box>
      </Grid>

      {schedules.length === 0 ? (
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Box display="flex" flexDirection="column" alignItems="center" py={4}>
                <Schedule sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                <Typography variant="h6" color="text.secondary">No Scheduled Reports</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Set up automated report delivery to your inbox.
                </Typography>
                <Button variant="outlined" startIcon={<Add />} onClick={openCreate}>
                  Create First Schedule
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      ) : (
        <Grid item xs={12}>
          <Card>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Report Name</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Frequency</TableCell>
                    <TableCell>Format</TableCell>
                    <TableCell>Recipients</TableCell>
                    <TableCell>Next Run</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {schedules.map((s) => (
                    <TableRow key={s.id}>
                      <TableCell>
                        <Typography variant="body2" fontWeight={600}>{s.name}</Typography>
                      </TableCell>
                      <TableCell>
                        <Chip label={REPORT_TYPE_LABELS[s.reportType]} size="small" variant="outlined" />
                      </TableCell>
                      <TableCell>
                        <Chip label={FREQUENCY_LABELS[s.frequency]} size="small" color="info" variant="outlined" />
                      </TableCell>
                      <TableCell>
                        <Chip label={s.format.toUpperCase()} size="small" />
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" noWrap sx={{ maxWidth: 180, display: 'block' }}>
                          {s.recipients}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption">{formatDate(s.nextRun)}</Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={s.enabled ? 'Active' : 'Paused'}
                          size="small"
                          color={s.enabled ? 'success' : 'default'}
                          onClick={() => handleToggle(s.id)}
                          sx={{ cursor: 'pointer' }}
                        />
                      </TableCell>
                      <TableCell align="right">
                        <IconButton size="small" onClick={() => openEdit(s)}><Edit fontSize="small" /></IconButton>
                        <IconButton size="small" color="error" onClick={() => handleDelete(s.id)}><Delete fontSize="small" /></IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Card>
        </Grid>
      )}

      {/* Create / Edit Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingId ? 'Edit Schedule' : 'New Scheduled Report'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Report Name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              fullWidth
              size="small"
            />
            <FormControl fullWidth size="small">
              <InputLabel>Report Type</InputLabel>
              <Select
                value={form.reportType}
                label="Report Type"
                onChange={(e) => setForm({ ...form, reportType: e.target.value as any })}
              >
                {Object.entries(REPORT_TYPE_LABELS).map(([key, label]) => (
                  <MenuItem key={key} value={key}>{label}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>Frequency</InputLabel>
              <Select
                value={form.frequency}
                label="Frequency"
                onChange={(e) => setForm({ ...form, frequency: e.target.value as any })}
              >
                <MenuItem value="daily">Daily (8:00 AM)</MenuItem>
                <MenuItem value="weekly">Weekly (Monday 8:00 AM)</MenuItem>
                <MenuItem value="monthly">Monthly (1st at 8:00 AM)</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel>Export Format</InputLabel>
              <Select
                value={form.format}
                label="Export Format"
                onChange={(e) => setForm({ ...form, format: e.target.value as any })}
              >
                <MenuItem value="csv">CSV</MenuItem>
                <MenuItem value="pdf">PDF</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Recipients (comma-separated emails)"
              value={form.recipients}
              onChange={(e) => setForm({ ...form, recipients: e.target.value })}
              fullWidth
              size="small"
              placeholder="user@example.com, manager@example.com"
            />
            <FormControlLabel
              control={
                <Switch
                  checked={form.enabled}
                  onChange={(e) => setForm({ ...form, enabled: e.target.checked })}
                />
              }
              label="Enabled"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave}>
            {editingId ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Grid>
  );
};

export default ScheduledReports;
