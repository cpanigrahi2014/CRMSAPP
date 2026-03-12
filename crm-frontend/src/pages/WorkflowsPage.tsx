/* ============================================================
   WorkflowsPage – Workflow Automation (8 Features)
   1. Workflow Rules  2. Automated Task Creation
   3. Automated Email Notifications  4. Record Update Automation
   5. Conditional Workflow Logic  6. Approval Workflows
   7. Multi-Step Automation  8. Workflow Monitoring
   ============================================================ */
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Box, Typography, Tabs, Tab, Button, TextField, Select, MenuItem,
  FormControl, InputLabel, IconButton, Chip, Switch, FormControlLabel,
  Card, CardContent, CardActions, Dialog, DialogTitle, DialogContent,
  DialogActions, Alert, Snackbar, Table, TableHead, TableBody, TableRow,
  TableCell, TablePagination, Paper, Tooltip, Divider, Grid, Stack,
  Accordion, AccordionSummary, AccordionDetails, CircularProgress,
  SelectChangeEvent,
} from '@mui/material';
import {
  Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon,
  PlayArrow as EnableIcon, Pause as DisableIcon, ExpandMore,
  Refresh as RefreshIcon, CheckCircle, Error as ErrorIcon,
  SkipNext as SkipIcon, Email as EmailIcon, TaskAlt as TaskIcon,
  EditNote as UpdateIcon, Notifications as NotifIcon,
  AssignmentInd as AssignIcon, FilterList as FilterIcon,
  Timeline as TimelineIcon, Visibility as ViewIcon,
} from '@mui/icons-material';
import { workflowService } from '../services/workflowService';
import type {
  WorkflowRule, WorkflowAction, WorkflowCondition,
  WorkflowExecutionLog, CreateWorkflowRuleRequest,
  ActionType, ConditionOperator, LogicalOperator, ExecutionStatus,
  PagedData,
} from '../types';
import { PageHeader } from '../components';
/* ── Constants ─────────────────────────────────────────────── */
const ENTITY_TYPES = ['LEAD', 'ACCOUNT', 'CONTACT', 'OPPORTUNITY', 'ACTIVITY'];
const TRIGGER_EVENTS = ['CREATED', 'UPDATED', 'DELETED', 'STATUS_CHANGED', 'STAGE_CHANGED', 'ASSIGNED'];

const ACTION_TYPES: { value: ActionType; label: string; icon: React.ReactNode }[] = [
  { value: 'SEND_EMAIL', label: 'Send Email', icon: <EmailIcon fontSize="small" /> },
  { value: 'CREATE_TASK', label: 'Create Task', icon: <TaskIcon fontSize="small" /> },
  { value: 'UPDATE_FIELD', label: 'Update Field', icon: <UpdateIcon fontSize="small" /> },
  { value: 'SEND_NOTIFICATION', label: 'Send Notification', icon: <NotifIcon fontSize="small" /> },
  { value: 'ASSIGN_TO', label: 'Assign To', icon: <AssignIcon fontSize="small" /> },
];

const CONDITION_OPERATORS: { value: ConditionOperator; label: string }[] = [
  { value: 'EQUALS', label: '=' },
  { value: 'NOT_EQUALS', label: '≠' },
  { value: 'GREATER_THAN', label: '>' },
  { value: 'LESS_THAN', label: '<' },
  { value: 'CONTAINS', label: 'Contains' },
  { value: 'IN', label: 'In' },
  { value: 'IS_NULL', label: 'Is Null' },
  { value: 'IS_NOT_NULL', label: 'Is Not Null' },
];

const ENTITY_FIELDS: Record<string, string[]> = {
  LEAD: ['status', 'source', 'leadScore', 'assignedTo', 'company', 'email', 'territory'],
  ACCOUNT: ['type', 'industry', 'annualRevenue', 'numberOfEmployees', 'lifecycleStage', 'ownerId'],
  CONTACT: ['lifecycleStage', 'segment', 'department', 'accountId', 'emailOptIn'],
  OPPORTUNITY: ['stage', 'amount', 'probability', 'assignedTo', 'forecastCategory', 'closeDate'],
  ACTIVITY: ['type', 'status', 'priority', 'assignedTo', 'dueDate'],
};

/* ── Helper: blank condition/action ────────────────────────── */
const blankCondition = (): Omit<WorkflowCondition, 'id'> => ({
  fieldName: '', operator: 'EQUALS', value: '', logicalOperator: 'AND',
});
const blankAction = (): Omit<WorkflowAction, 'id'> => ({
  actionType: 'SEND_EMAIL', targetField: '', targetValue: '', actionOrder: 0,
});

/* ── Tab Panel ─────────────────────────────────────────────── */
const TabPanel: React.FC<{ value: number; index: number; children: React.ReactNode }> = ({ value, index, children }) =>
  value === index ? <Box sx={{ pt: 2 }}>{children}</Box> : null;

/* ================================================================
   MAIN COMPONENT
   ================================================================ */
const WorkflowsPage: React.FC = () => {
  const [tab, setTab] = useState(0);
  const [rules, setRules] = useState<WorkflowRule[]>([]);
  const [totalRules, setTotalRules] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [loading, setLoading] = useState(false);

  const [logs, setLogs] = useState<WorkflowExecutionLog[]>([]);
  const [totalLogs, setTotalLogs] = useState(0);
  const [logPage, setLogPage] = useState(0);
  const [logRows, setLogRows] = useState(25);

  const [snack, setSnack] = useState<{ open: boolean; msg: string; severity: 'success' | 'error' }>({ open: false, msg: '', severity: 'success' });

  /* ── Rule builder dialog state ───────────────────────────── */
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<WorkflowRule | null>(null);
  const [ruleName, setRuleName] = useState('');
  const [ruleDesc, setRuleDesc] = useState('');
  const [entityType, setEntityType] = useState('LEAD');
  const [triggerEvent, setTriggerEvent] = useState('CREATED');
  const [conditions, setConditions] = useState<Omit<WorkflowCondition, 'id'>[]>([blankCondition()]);
  const [actions, setActions] = useState<Omit<WorkflowAction, 'id'>[]>([blankAction()]);
  const [saving, setSaving] = useState(false);

  /* ── Detail dialog state ─────────────────────────────────── */
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailRule, setDetailRule] = useState<WorkflowRule | null>(null);
  const [detailLogs, setDetailLogs] = useState<WorkflowExecutionLog[]>([]);

  /* ── Filter state (monitoring tab) ───────────────────────── */
  const [logStatusFilter, setLogStatusFilter] = useState<ExecutionStatus | ''>('');

  /* ── Fetch rules ─────────────────────────────────────────── */
  const fetchRules = useCallback(async () => {
    setLoading(true);
    try {
      const res = await workflowService.getAll(page, rowsPerPage);
      setRules(res.data.content);
      setTotalRules(res.data.totalElements);
    } catch {
      setSnack({ open: true, msg: 'Failed to load workflow rules', severity: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage]);

  /* ── Fetch execution logs ────────────────────────────────── */
  const fetchLogs = useCallback(async () => {
    try {
      const res = await workflowService.getExecutionLogs(logPage, logRows);
      setLogs(res.data.content);
      setTotalLogs(res.data.totalElements);
    } catch {
      setSnack({ open: true, msg: 'Failed to load execution logs', severity: 'error' });
    }
  }, [logPage, logRows]);

  useEffect(() => { fetchRules(); }, [fetchRules]);
  useEffect(() => { if (tab === 2) fetchLogs(); }, [tab, fetchLogs]);

  /* ── Filtered logs ───────────────────────────────────────── */
  const filteredLogs = useMemo(
    () => logStatusFilter ? logs.filter((l) => l.status === logStatusFilter) : logs,
    [logs, logStatusFilter],
  );

  /* ── Stats for monitoring ────────────────────────────────── */
  const stats = useMemo(() => ({
    total: logs.length,
    success: logs.filter((l) => l.status === 'SUCCESS').length,
    failed: logs.filter((l) => l.status === 'FAILED').length,
    skipped: logs.filter((l) => l.status === 'SKIPPED').length,
  }), [logs]);

  /* ── Open rule builder (create/edit) ─────────────────────── */
  const openCreateDialog = () => {
    setEditingRule(null);
    setRuleName(''); setRuleDesc(''); setEntityType('LEAD'); setTriggerEvent('CREATED');
    setConditions([blankCondition()]); setActions([blankAction()]);
    setDialogOpen(true);
  };

  const openEditDialog = (rule: WorkflowRule) => {
    setEditingRule(rule);
    setRuleName(rule.name);
    setRuleDesc(rule.description ?? '');
    setEntityType(rule.entityType);
    setTriggerEvent(rule.triggerEvent);
    setConditions(rule.conditions.map(({ fieldName, operator, value, logicalOperator }) => ({
      fieldName, operator, value, logicalOperator,
    })));
    setActions(rule.actions.map(({ actionType, targetField, targetValue, actionOrder }) => ({
      actionType, targetField, targetValue, actionOrder,
    })));
    setDialogOpen(true);
  };

  /* ── Save rule ───────────────────────────────────────────── */
  const handleSave = async () => {
    if (!ruleName.trim()) return;
    setSaving(true);
    try {
      const payload: CreateWorkflowRuleRequest = {
        name: ruleName.trim(), description: ruleDesc.trim() || undefined,
        entityType, triggerEvent,
        conditions: conditions.map((c, i) => ({ ...c, logicalOperator: i === 0 ? 'AND' as LogicalOperator : c.logicalOperator })),
        actions: actions.map((a, i) => ({ ...a, actionOrder: i })),
      };
      if (editingRule) {
        await workflowService.update(editingRule.id, payload);
        setSnack({ open: true, msg: 'Rule updated', severity: 'success' });
      } else {
        await workflowService.create(payload);
        setSnack({ open: true, msg: 'Rule created', severity: 'success' });
      }
      setDialogOpen(false);
      fetchRules();
    } catch {
      setSnack({ open: true, msg: 'Save failed', severity: 'error' });
    } finally {
      setSaving(false);
    }
  };

  /* ── Toggle active ───────────────────────────────────────── */
  const toggleActive = async (rule: WorkflowRule) => {
    try {
      if (rule.active) {
        await workflowService.disable(rule.id);
      } else {
        await workflowService.enable(rule.id);
      }
      fetchRules();
    } catch {
      setSnack({ open: true, msg: 'Toggle failed', severity: 'error' });
    }
  };

  /* ── Delete rule ─────────────────────────────────────────── */
  const handleDelete = async (id: string) => {
    try {
      await workflowService.delete(id);
      setSnack({ open: true, msg: 'Rule deleted', severity: 'success' });
      fetchRules();
    } catch {
      setSnack({ open: true, msg: 'Delete failed', severity: 'error' });
    }
  };

  /* ── View detail + execution logs ────────────────────────── */
  const openDetail = async (rule: WorkflowRule) => {
    setDetailRule(rule);
    setDetailOpen(true);
    try {
      const res = await workflowService.getExecutionLogsByRule(rule.id, 0, 50);
      setDetailLogs(res.data.content);
    } catch { /* ignore */ }
  };

  /* ── Condition helpers ───────────────────────────────────── */
  const updateCondition = (idx: number, field: string, val: string) => {
    setConditions((prev) => prev.map((c, i) => i === idx ? { ...c, [field]: val } : c));
  };
  const addCondition = () => setConditions((prev) => [...prev, blankCondition()]);
  const removeCondition = (idx: number) => setConditions((prev) => prev.filter((_, i) => i !== idx));

  /* ── Action helpers ──────────────────────────────────────── */
  const updateAction = (idx: number, field: string, val: string) => {
    setActions((prev) => prev.map((a, i) => i === idx ? { ...a, [field]: val } : a));
  };
  const addAction = () => setActions((prev) => [...prev, blankAction()]);
  const removeAction = (idx: number) => setActions((prev) => prev.filter((_, i) => i !== idx));

  /* ── Action type icon ────────────────────────────────────── */
  const actionIcon = (type: ActionType) => ACTION_TYPES.find((a) => a.value === type)?.icon ?? null;

  /* ── Status chip ─────────────────────────────────────────── */
  const statusChip = (status: ExecutionStatus) => {
    const map: Record<ExecutionStatus, { color: 'success' | 'error' | 'warning'; icon: React.ReactElement }> = {
      SUCCESS: { color: 'success', icon: <CheckCircle fontSize="small" /> },
      FAILED: { color: 'error', icon: <ErrorIcon fontSize="small" /> },
      SKIPPED: { color: 'warning', icon: <SkipIcon fontSize="small" /> },
    };
    const m = map[status];
    return <Chip label={status} size="small" color={m.color} icon={m.icon} />;
  };

  /* ── Available fields for current entity type ────────────── */
  const availableFields = ENTITY_FIELDS[entityType] ?? [];

  /* ────────────────────────────────────────────────────────── */
  /*               R E N D E R                                  */
  /* ────────────────────────────────────────────────────────── */
  return (
    <Box>
      {/* Header */}
      <PageHeader
        title="Workflow Automation"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Workflows' }]}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
            New Rule
          </Button>
        }
      />

      {/* Tabs */}
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 1 }}>
        <Tab label="Workflow Rules" />
        <Tab label="Rule Builder" />
        <Tab label="Monitoring" />
      </Tabs>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 0: WORKFLOW RULES LIST                           ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={0}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>
        ) : rules.length === 0 ? (
          <Alert severity="info" sx={{ mt: 2 }}>
            No workflow rules yet. Click "New Rule" to create one.
          </Alert>
        ) : (
          <Paper>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Entity</TableCell>
                  <TableCell>Trigger</TableCell>
                  <TableCell align="center">Conditions</TableCell>
                  <TableCell align="center">Actions</TableCell>
                  <TableCell align="center">Active</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rules.map((r) => (
                  <TableRow key={r.id} hover>
                    <TableCell>
                      <Typography fontWeight={600}>{r.name}</Typography>
                      {r.description && (
                        <Typography variant="caption" color="text.secondary">{r.description}</Typography>
                      )}
                    </TableCell>
                    <TableCell><Chip label={r.entityType} size="small" variant="outlined" /></TableCell>
                    <TableCell><Chip label={r.triggerEvent} size="small" /></TableCell>
                    <TableCell align="center">{r.conditions.length}</TableCell>
                    <TableCell align="center">
                      <Stack direction="row" spacing={0.5} justifyContent="center">
                        {r.actions.map((a) => (
                          <Tooltip key={a.id ?? a.actionOrder} title={a.actionType}>
                            <span>{actionIcon(a.actionType)}</span>
                          </Tooltip>
                        ))}
                      </Stack>
                    </TableCell>
                    <TableCell align="center">
                      <Switch checked={r.active} onChange={() => toggleActive(r)} size="small" />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="View Details"><IconButton size="small" onClick={() => openDetail(r)}><ViewIcon fontSize="small" /></IconButton></Tooltip>
                      <Tooltip title="Edit"><IconButton size="small" onClick={() => openEditDialog(r)}><EditIcon fontSize="small" /></IconButton></Tooltip>
                      <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => handleDelete(r.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <TablePagination
              component="div" count={totalRules} page={page} rowsPerPage={rowsPerPage}
              onPageChange={(_, p) => setPage(p)}
              onRowsPerPageChange={(e) => { setRowsPerPage(+e.target.value); setPage(0); }}
            />
          </Paper>
        )}
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 1: RULE BUILDER (visual)                         ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={1}>
        <Alert severity="info" sx={{ mb: 2 }}>
          Build workflow rules visually. Select an entity type, define trigger events,
          add conditions (AND/OR logic), and chain multiple actions in sequence.
        </Alert>

        <Grid container spacing={3}>
          {/* Left: Config */}
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h6" gutterBottom>Rule Configuration</Typography>
                <Stack spacing={2}>
                  <TextField label="Rule Name" fullWidth value={ruleName} onChange={(e) => setRuleName(e.target.value)} />
                  <TextField label="Description" fullWidth multiline rows={2} value={ruleDesc} onChange={(e) => setRuleDesc(e.target.value)} />
                  <FormControl fullWidth>
                    <InputLabel>Entity Type</InputLabel>
                    <Select value={entityType} label="Entity Type" onChange={(e: SelectChangeEvent) => setEntityType(e.target.value)}>
                      {ENTITY_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                    </Select>
                  </FormControl>
                  <FormControl fullWidth>
                    <InputLabel>Trigger Event</InputLabel>
                    <Select value={triggerEvent} label="Trigger Event" onChange={(e: SelectChangeEvent) => setTriggerEvent(e.target.value)}>
                      {TRIGGER_EVENTS.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                    </Select>
                  </FormControl>
                </Stack>
              </CardContent>
            </Card>
          </Grid>

          {/* Right: Preview */}
          <Grid item xs={12} md={6}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>Rule Preview</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  <strong>When</strong> a <Chip label={entityType} size="small" /> is <Chip label={triggerEvent} size="small" />
                </Typography>
                {conditions.filter((c) => c.fieldName).length > 0 && (
                  <>
                    <Typography variant="body2" color="text.secondary"><strong>If</strong></Typography>
                    {conditions.filter((c) => c.fieldName).map((c, i) => (
                      <Typography key={i} variant="body2" sx={{ ml: 2 }}>
                        {i > 0 && <Chip label={c.logicalOperator} size="small" sx={{ mr: 1 }} />}
                        {c.fieldName} {CONDITION_OPERATORS.find((o) => o.value === c.operator)?.label} {c.value}
                      </Typography>
                    ))}
                  </>
                )}
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}><strong>Then</strong></Typography>
                {actions.map((a, i) => (
                  <Typography key={i} variant="body2" sx={{ ml: 2 }}>
                    Step {i + 1}: {actionIcon(a.actionType)} {ACTION_TYPES.find((t) => t.value === a.actionType)?.label}
                    {a.targetField && ` → ${a.targetField}`}
                    {a.targetValue && ` = "${a.targetValue}"`}
                  </Typography>
                ))}
              </CardContent>
              <CardActions>
                <Button variant="contained" onClick={handleSave} disabled={saving || !ruleName.trim()}>
                  {saving ? <CircularProgress size={20} /> : editingRule ? 'Update Rule' : 'Create Rule'}
                </Button>
              </CardActions>
            </Card>
          </Grid>

          {/* Conditions */}
          <Grid item xs={12}>
            <Card variant="outlined">
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6">
                    <FilterIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                    Conditions (Conditional Logic)
                  </Typography>
                  <Button startIcon={<AddIcon />} onClick={addCondition}>Add Condition</Button>
                </Box>
                {conditions.map((c, idx) => (
                  <Box key={idx} sx={{ display: 'flex', gap: 1, mb: 1, alignItems: 'center' }}>
                    {idx > 0 && (
                      <FormControl sx={{ minWidth: 80 }}>
                        <Select size="small" value={c.logicalOperator} onChange={(e) => updateCondition(idx, 'logicalOperator', e.target.value)}>
                          <MenuItem value="AND">AND</MenuItem>
                          <MenuItem value="OR">OR</MenuItem>
                        </Select>
                      </FormControl>
                    )}
                    <FormControl sx={{ minWidth: 140 }}>
                      <Select size="small" value={c.fieldName} displayEmpty onChange={(e) => updateCondition(idx, 'fieldName', e.target.value)}>
                        <MenuItem value="" disabled>Field…</MenuItem>
                        {availableFields.map((f) => <MenuItem key={f} value={f}>{f}</MenuItem>)}
                      </Select>
                    </FormControl>
                    <FormControl sx={{ minWidth: 120 }}>
                      <Select size="small" value={c.operator} onChange={(e) => updateCondition(idx, 'operator', e.target.value)}>
                        {CONDITION_OPERATORS.map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                      </Select>
                    </FormControl>
                    {!['IS_NULL', 'IS_NOT_NULL'].includes(c.operator) && (
                      <TextField size="small" placeholder="Value" value={c.value} onChange={(e) => updateCondition(idx, 'value', e.target.value)} />
                    )}
                    <IconButton size="small" color="error" onClick={() => removeCondition(idx)} disabled={conditions.length <= 1}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Box>
                ))}
              </CardContent>
            </Card>
          </Grid>

          {/* Actions (multi-step) */}
          <Grid item xs={12}>
            <Card variant="outlined">
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6">
                    <TimelineIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                    Actions (Multi-Step Automation)
                  </Typography>
                  <Button startIcon={<AddIcon />} onClick={addAction}>Add Action</Button>
                </Box>
                {actions.map((a, idx) => (
                  <Accordion key={idx} defaultExpanded variant="outlined" sx={{ mb: 1 }}>
                    <AccordionSummary expandIcon={<ExpandMore />}>
                      <Typography>
                        Step {idx + 1}: {actionIcon(a.actionType)}{' '}
                        {ACTION_TYPES.find((t) => t.value === a.actionType)?.label}
                      </Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                      <Stack spacing={2}>
                        <FormControl fullWidth>
                          <InputLabel>Action Type</InputLabel>
                          <Select value={a.actionType} label="Action Type" onChange={(e) => updateAction(idx, 'actionType', e.target.value)}>
                            {ACTION_TYPES.map((t) => (
                              <MenuItem key={t.value} value={t.value}>
                                {t.icon} <span style={{ marginLeft: 8 }}>{t.label}</span>
                              </MenuItem>
                            ))}
                          </Select>
                        </FormControl>

                        {/* Dynamic fields per action type */}
                        {a.actionType === 'SEND_EMAIL' && (
                          <>
                            <TextField label="To (email field or address)" fullWidth value={a.targetField} onChange={(e) => updateAction(idx, 'targetField', e.target.value)} helperText="e.g. email, assignedTo" />
                            <TextField label="Email Template / Subject" fullWidth value={a.targetValue} onChange={(e) => updateAction(idx, 'targetValue', e.target.value)} />
                          </>
                        )}
                        {a.actionType === 'CREATE_TASK' && (
                          <>
                            <TextField label="Task Subject" fullWidth value={a.targetField} onChange={(e) => updateAction(idx, 'targetField', e.target.value)} />
                            <TextField label="Assign To (user ID or field)" fullWidth value={a.targetValue} onChange={(e) => updateAction(idx, 'targetValue', e.target.value)} />
                          </>
                        )}
                        {a.actionType === 'UPDATE_FIELD' && (
                          <>
                            <FormControl fullWidth>
                              <InputLabel>Field to Update</InputLabel>
                              <Select value={a.targetField} label="Field to Update" onChange={(e) => updateAction(idx, 'targetField', e.target.value)}>
                                {availableFields.map((f) => <MenuItem key={f} value={f}>{f}</MenuItem>)}
                              </Select>
                            </FormControl>
                            <TextField label="New Value" fullWidth value={a.targetValue} onChange={(e) => updateAction(idx, 'targetValue', e.target.value)} />
                          </>
                        )}
                        {a.actionType === 'SEND_NOTIFICATION' && (
                          <>
                            <TextField label="Notification Type" fullWidth value={a.targetField} onChange={(e) => updateAction(idx, 'targetField', e.target.value)} helperText="e.g. IN_APP, PUSH" />
                            <TextField label="Message" fullWidth value={a.targetValue} onChange={(e) => updateAction(idx, 'targetValue', e.target.value)} />
                          </>
                        )}
                        {a.actionType === 'ASSIGN_TO' && (
                          <>
                            <TextField label="Assignment Field" fullWidth value={a.targetField} onChange={(e) => updateAction(idx, 'targetField', e.target.value)} helperText="e.g. assignedTo, ownerId" />
                            <TextField label="User ID or Round-Robin Rule" fullWidth value={a.targetValue} onChange={(e) => updateAction(idx, 'targetValue', e.target.value)} />
                          </>
                        )}
                      </Stack>
                      <Box sx={{ mt: 2, textAlign: 'right' }}>
                        <IconButton color="error" onClick={() => removeAction(idx)} disabled={actions.length <= 1}>
                          <DeleteIcon />
                        </IconButton>
                      </Box>
                    </AccordionDetails>
                  </Accordion>
                ))}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  TAB 2: MONITORING (execution logs)                    ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <TabPanel value={tab} index={2}>
        {/* Stats */}
        <Grid container spacing={2} sx={{ mb: 3 }}>
          {[
            { label: 'Total Executions', value: stats.total, color: 'primary.main' },
            { label: 'Successful', value: stats.success, color: 'success.main' },
            { label: 'Failed', value: stats.failed, color: 'error.main' },
            { label: 'Skipped', value: stats.skipped, color: 'warning.main' },
          ].map((s) => (
            <Grid item xs={6} md={3} key={s.label}>
              <Card variant="outlined">
                <CardContent sx={{ textAlign: 'center' }}>
                  <Typography variant="h4" fontWeight={700} color={s.color}>{s.value}</Typography>
                  <Typography variant="body2" color="text.secondary">{s.label}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        {/* Filters + Refresh */}
        <Box sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Status</InputLabel>
            <Select value={logStatusFilter} label="Status" onChange={(e) => setLogStatusFilter(e.target.value as ExecutionStatus | '')}>
              <MenuItem value="">All</MenuItem>
              <MenuItem value="SUCCESS">Success</MenuItem>
              <MenuItem value="FAILED">Failed</MenuItem>
              <MenuItem value="SKIPPED">Skipped</MenuItem>
            </Select>
          </FormControl>
          <Button startIcon={<RefreshIcon />} onClick={fetchLogs}>Refresh</Button>
        </Box>

        {/* Log table */}
        <Paper>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Rule</TableCell>
                <TableCell>Entity Type</TableCell>
                <TableCell>Entity ID</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Details</TableCell>
                <TableCell>Executed At</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredLogs.length === 0 ? (
                <TableRow><TableCell colSpan={6} align="center"><Typography color="text.secondary" sx={{ py: 4 }}>No execution logs found</Typography></TableCell></TableRow>
              ) : filteredLogs.map((l) => (
                <TableRow key={l.id} hover>
                  <TableCell><Typography fontWeight={600}>{l.ruleName}</Typography></TableCell>
                  <TableCell><Chip label={l.triggerEntityType} size="small" variant="outlined" /></TableCell>
                  <TableCell>
                    <Tooltip title={l.triggerEntityId}>
                      <Typography variant="body2" sx={{ maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {l.triggerEntityId.substring(0, 8)}…
                      </Typography>
                    </Tooltip>
                  </TableCell>
                  <TableCell>{statusChip(l.status)}</TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ maxWidth: 250, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {l.executionDetails ?? '—'}
                    </Typography>
                  </TableCell>
                  <TableCell>{new Date(l.executedAt).toLocaleString()}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TablePagination
            component="div" count={totalLogs} page={logPage} rowsPerPage={logRows}
            onPageChange={(_, p) => setLogPage(p)}
            onRowsPerPageChange={(e) => { setLogRows(+e.target.value); setLogPage(0); }}
          />
        </Paper>
      </TabPanel>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  DIALOG: Create / Edit Rule                            ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>{editingRule ? 'Edit Workflow Rule' : 'Create Workflow Rule'}</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth required value={ruleName} onChange={(e) => setRuleName(e.target.value)} />
            <TextField label="Description" fullWidth multiline rows={2} value={ruleDesc} onChange={(e) => setRuleDesc(e.target.value)} />
            <Box sx={{ display: 'flex', gap: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Entity Type</InputLabel>
                <Select value={entityType} label="Entity Type" onChange={(e: SelectChangeEvent) => setEntityType(e.target.value)}>
                  {ENTITY_TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel>Trigger Event</InputLabel>
                <Select value={triggerEvent} label="Trigger Event" onChange={(e: SelectChangeEvent) => setTriggerEvent(e.target.value)}>
                  {TRIGGER_EVENTS.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
                </Select>
              </FormControl>
            </Box>

            <Divider />
            <Typography variant="subtitle1" fontWeight={600}>Conditions</Typography>
            {conditions.map((c, idx) => (
              <Box key={idx} sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                {idx > 0 && (
                  <Select size="small" value={c.logicalOperator} sx={{ minWidth: 80 }}
                    onChange={(e) => updateCondition(idx, 'logicalOperator', e.target.value)}>
                    <MenuItem value="AND">AND</MenuItem>
                    <MenuItem value="OR">OR</MenuItem>
                  </Select>
                )}
                <Select size="small" value={c.fieldName} displayEmpty sx={{ minWidth: 130 }}
                  onChange={(e) => updateCondition(idx, 'fieldName', e.target.value)}>
                  <MenuItem value="" disabled>Field</MenuItem>
                  {(ENTITY_FIELDS[entityType] ?? []).map((f) => <MenuItem key={f} value={f}>{f}</MenuItem>)}
                </Select>
                <Select size="small" value={c.operator} sx={{ minWidth: 100 }}
                  onChange={(e) => updateCondition(idx, 'operator', e.target.value)}>
                  {CONDITION_OPERATORS.map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                </Select>
                {!['IS_NULL', 'IS_NOT_NULL'].includes(c.operator) && (
                  <TextField size="small" placeholder="Value" value={c.value}
                    onChange={(e) => updateCondition(idx, 'value', e.target.value)} />
                )}
                <IconButton size="small" color="error" onClick={() => removeCondition(idx)} disabled={conditions.length <= 1}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Box>
            ))}
            <Button size="small" startIcon={<AddIcon />} onClick={addCondition} sx={{ alignSelf: 'flex-start' }}>
              Add Condition
            </Button>

            <Divider />
            <Typography variant="subtitle1" fontWeight={600}>Actions (executed in order)</Typography>
            {actions.map((a, idx) => (
              <Card key={idx} variant="outlined" sx={{ p: 1.5 }}>
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mb: 1 }}>
                  <Typography variant="body2" fontWeight={600}>Step {idx + 1}</Typography>
                  <FormControl size="small" sx={{ minWidth: 180 }}>
                    <Select value={a.actionType} onChange={(e) => updateAction(idx, 'actionType', e.target.value)}>
                      {ACTION_TYPES.map((t) => <MenuItem key={t.value} value={t.value}>{t.icon} {t.label}</MenuItem>)}
                    </Select>
                  </FormControl>
                  <TextField size="small" placeholder="Target Field" value={a.targetField}
                    onChange={(e) => updateAction(idx, 'targetField', e.target.value)} sx={{ flex: 1 }} />
                  <TextField size="small" placeholder="Target Value" value={a.targetValue}
                    onChange={(e) => updateAction(idx, 'targetValue', e.target.value)} sx={{ flex: 1 }} />
                  <IconButton size="small" color="error" onClick={() => removeAction(idx)} disabled={actions.length <= 1}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Box>
              </Card>
            ))}
            <Button size="small" startIcon={<AddIcon />} onClick={addAction} sx={{ alignSelf: 'flex-start' }}>
              Add Action
            </Button>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave} disabled={saving || !ruleName.trim()}>
            {saving ? <CircularProgress size={20} /> : editingRule ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* ╔═══════════════════════════════════════════════════════╗
         ║  DIALOG: Rule Detail + Execution Logs                  ║
         ╚═══════════════════════════════════════════════════════╝ */}
      <Dialog open={detailOpen} onClose={() => setDetailOpen(false)} maxWidth="md" fullWidth>
        {detailRule && (
          <>
            <DialogTitle>
              {detailRule.name}
              <Chip label={detailRule.active ? 'Active' : 'Inactive'} color={detailRule.active ? 'success' : 'default'} size="small" sx={{ ml: 2 }} />
            </DialogTitle>
            <DialogContent dividers>
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">Entity Type</Typography>
                  <Typography>{detailRule.entityType}</Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">Trigger Event</Typography>
                  <Typography>{detailRule.triggerEvent}</Typography>
                </Grid>
                {detailRule.description && (
                  <Grid item xs={12}>
                    <Typography variant="body2" color="text.secondary">Description</Typography>
                    <Typography>{detailRule.description}</Typography>
                  </Grid>
                )}
              </Grid>

              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>Conditions</Typography>
              {detailRule.conditions.length === 0 ? (
                <Typography variant="body2" color="text.secondary">No conditions (always matches)</Typography>
              ) : detailRule.conditions.map((c, i) => (
                <Typography key={i} variant="body2" sx={{ ml: 1 }}>
                  {i > 0 && <Chip label={c.logicalOperator} size="small" sx={{ mr: 1 }} />}
                  <strong>{c.fieldName}</strong> {CONDITION_OPERATORS.find((o) => o.value === c.operator)?.label} "{c.value}"
                </Typography>
              ))}

              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>Actions</Typography>
              {detailRule.actions
                .slice()
                .sort((a, b) => a.actionOrder - b.actionOrder)
                .map((a, i) => (
                  <Box key={i} sx={{ display: 'flex', alignItems: 'center', gap: 1, ml: 1, mb: 0.5 }}>
                    <Chip label={`Step ${i + 1}`} size="small" />
                    {actionIcon(a.actionType)}
                    <Typography variant="body2">
                      {ACTION_TYPES.find((t) => t.value === a.actionType)?.label}
                      {a.targetField && ` → ${a.targetField}`}
                      {a.targetValue && ` = "${a.targetValue}"`}
                    </Typography>
                  </Box>
                ))}

              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>Recent Executions</Typography>
              {detailLogs.length === 0 ? (
                <Typography variant="body2" color="text.secondary">No executions yet</Typography>
              ) : (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Status</TableCell>
                      <TableCell>Entity ID</TableCell>
                      <TableCell>Details</TableCell>
                      <TableCell>Time</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {detailLogs.slice(0, 20).map((l) => (
                      <TableRow key={l.id}>
                        <TableCell>{statusChip(l.status)}</TableCell>
                        <TableCell>{l.triggerEntityId.substring(0, 8)}…</TableCell>
                        <TableCell>{l.executionDetails ?? '—'}</TableCell>
                        <TableCell>{new Date(l.executedAt).toLocaleString()}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setDetailOpen(false)}>Close</Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Snackbar */}
      <Snackbar open={snack.open} autoHideDuration={4000} onClose={() => setSnack((s) => ({ ...s, open: false }))}>
        <Alert severity={snack.severity} onClose={() => setSnack((s) => ({ ...s, open: false }))}>{snack.msg}</Alert>
      </Snackbar>
    </Box>
  );
};

export default WorkflowsPage;
