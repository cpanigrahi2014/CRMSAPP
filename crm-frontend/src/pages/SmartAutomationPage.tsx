/* ============================================================
   SmartAutomationPage – Smart Automation Hub (6 features)
   ============================================================ */
import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Tabs, Tab, Paper, Card, CardContent, CardActions,
  Button, Chip, Grid, IconButton, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Table, TableHead, TableRow, TableCell,
  TableBody, Alert, LinearProgress, Tooltip, Badge, Divider, Stack,
  Select, MenuItem, FormControl, InputLabel, CircularProgress,
} from '@mui/material';
import {
  AccountTree as BuilderIcon,
  Psychology as AiIcon,
  AltRoute as RoutingIcon,
  NotificationsActive as ReminderIcon,
  Description as ProposalIcon,
  Gavel as ContractIcon,
  Add as AddIcon,
  Send as SendIcon,
  Check as CheckIcon,
  Close as CloseIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  ThumbUp as AcceptIcon,
  ThumbDown as RejectIcon,
  AutoFixHigh as AutoIcon,
  TrendingUp as TrendIcon,
  Refresh as RefreshIcon,
  ContentCopy as CopyIcon,
  Draw as SignIcon,
  PlayArrow as ExecuteIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';
import { automationService } from '../services/automationService';
import type {
  WorkflowTemplate, WorkflowSuggestion, Proposal, Contract,
  CreateProposalRequest, CreateContractRequest, SignContractRequest,
} from '../types';
import { PageHeader } from '../components';
/* ── Tab Panel helper ──────────────────────────────────────── */
function TabPanel({ children, value, index }: { children: React.ReactNode; value: number; index: number }) {
  return value === index ? <Box sx={{ pt: 3 }}>{children}</Box> : null;
}

/* ── Status chip colors ────────────────────────────────────── */
const proposalStatusColor: Record<string, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  DRAFT: 'default', SENT: 'info', VIEWED: 'warning', ACCEPTED: 'success', REJECTED: 'error', EXPIRED: 'error',
};
const contractStatusColor: Record<string, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  DRAFT: 'default', SENT: 'info', VIEWED: 'warning', SIGNED: 'success', EXECUTED: 'success', EXPIRED: 'error', CANCELLED: 'error',
};
const suggestionTypeColor: Record<string, 'primary' | 'secondary' | 'warning'> = {
  BEST_PRACTICE: 'primary', PATTERN_DETECTED: 'secondary', OPTIMIZATION: 'warning',
};

/* ================================================================
   MAIN COMPONENT
   ================================================================ */
const SmartAutomationPage: React.FC = () => {
  const [tab, setTab] = useState(0);
  const [loading, setLoading] = useState(false);

  /* ── Workflow Templates ─────────────────────────────────── */
  const [templates, setTemplates] = useState<WorkflowTemplate[]>([]);

  const loadTemplates = useCallback(async () => {
    setLoading(true);
    try { setTemplates(await automationService.getTemplates()); } finally { setLoading(false); }
  }, []);

  /* ── AI Suggestions ─────────────────────────────────────── */
  const [suggestions, setSuggestions] = useState<WorkflowSuggestion[]>([]);
  const pendingSuggestions = suggestions.filter(s => s.status === 'PENDING');

  const loadSuggestions = useCallback(async () => {
    setLoading(true);
    try { setSuggestions(await automationService.getSuggestions()); } finally { setLoading(false); }
  }, []);

  const handleAccept = async (id: string) => {
    await automationService.acceptSuggestion(id);
    await loadSuggestions();
  };
  const handleDismiss = async (id: string) => {
    await automationService.dismissSuggestion(id);
    await loadSuggestions();
  };
  const handleGenerate = async () => {
    await automationService.generateSuggestions();
    await loadSuggestions();
  };

  /* ── Proposals ──────────────────────────────────────────── */
  const [proposals, setProposals] = useState<Proposal[]>([]);
  const [proposalDialog, setProposalDialog] = useState(false);
  const [viewProposal, setViewProposal] = useState<Proposal | null>(null);
  const [proposalForm, setProposalForm] = useState<CreateProposalRequest>({
    opportunityId: '', title: '', recipientEmail: '', recipientName: '',
    lineItems: [{ productName: '', quantity: 1, unitPrice: 0 }],
  });

  const loadProposals = useCallback(async () => {
    setLoading(true);
    try { setProposals(await automationService.getProposals()); } finally { setLoading(false); }
  }, []);

  const handleCreateProposal = async () => {
    await automationService.createProposal(proposalForm);
    setProposalDialog(false);
    setProposalForm({ opportunityId: '', title: '', recipientEmail: '', recipientName: '', lineItems: [{ productName: '', quantity: 1, unitPrice: 0 }] });
    await loadProposals();
  };

  /* ── Contracts ──────────────────────────────────────────── */
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [contractDialog, setContractDialog] = useState(false);
  const [signDialog, setSignDialog] = useState<string | null>(null);
  const [viewContract, setViewContract] = useState<Contract | null>(null);
  const [contractForm, setContractForm] = useState<CreateContractRequest>({
    opportunityId: '', title: '', amount: 0,
  });
  const [signForm, setSignForm] = useState<SignContractRequest>({
    signerName: '', signerEmail: '', signatureData: '',
  });

  const loadContracts = useCallback(async () => {
    setLoading(true);
    try { setContracts(await automationService.getContracts()); } finally { setLoading(false); }
  }, []);

  const handleCreateContract = async () => {
    await automationService.createContract(contractForm);
    setContractDialog(false);
    setContractForm({ opportunityId: '', title: '', amount: 0 });
    await loadContracts();
  };
  const handleSignContract = async () => {
    if (signDialog) {
      await automationService.signContract(signDialog, signForm);
      setSignDialog(null);
      setSignForm({ signerName: '', signerEmail: '', signatureData: '' });
      await loadContracts();
    }
  };

  /* ── Load data on tab change ────────────────────────────── */
  useEffect(() => {
    switch (tab) {
      case 0: loadTemplates(); break;
      case 1: loadSuggestions(); break;
      case 2: break; // Lead routing — info only
      case 3: break; // Reminders — info only
      case 4: loadProposals(); break;
      case 5: loadContracts(); break;
    }
  }, [tab, loadTemplates, loadSuggestions, loadProposals, loadContracts]);

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="Smart Automation"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Automation' }]}
      />
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="body2" color="text.secondary">
            Visual workflows, AI suggestions, automated proposals & contracts
        </Typography>
        <Stack direction="row" spacing={1}>
          <Chip icon={<AutoIcon />} label={`${templates.length} Templates`} color="primary" variant="outlined" />
          <Badge badgeContent={pendingSuggestions.length} color="warning">
            <Chip icon={<AiIcon />} label="AI Suggestions" color="secondary" variant="outlined" />
          </Badge>
        </Stack>
      </Box>

      {loading && <LinearProgress sx={{ mb: 2 }} />}

      <Paper sx={{ borderBottom: 1, borderColor: 'divider', mb: 0 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto">
          <Tab icon={<BuilderIcon />} iconPosition="start" label="Visual Builder" />
          <Tab icon={<AiIcon />} iconPosition="start" label={
            <Badge badgeContent={pendingSuggestions.length} color="warning">AI Suggestions</Badge>
          } />
          <Tab icon={<RoutingIcon />} iconPosition="start" label="Lead Routing" />
          <Tab icon={<ReminderIcon />} iconPosition="start" label="Follow-Up Reminders" />
          <Tab icon={<ProposalIcon />} iconPosition="start" label="Proposals" />
          <Tab icon={<ContractIcon />} iconPosition="start" label="Contracts" />
        </Tabs>
      </Paper>

      {/* ════════════════════════════════════════════════════════
          TAB 0 – Visual Workflow Builder
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={0}>
        <Alert severity="info" sx={{ mb: 3 }}>
          Use workflow templates to quickly create automation rules. Each template includes a pre-built canvas layout with trigger, condition, and action nodes.
        </Alert>
        <Grid container spacing={3}>
          {templates.map(tpl => {
            let nodes = 0;
            try { nodes = JSON.parse(tpl.canvasLayout || '{}').nodes?.length ?? 0; } catch { /* ignore */ }
            return (
              <Grid item xs={12} md={6} lg={4} key={tpl.id}>
                <Card variant="outlined" sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                  <CardContent sx={{ flex: 1 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Chip label={tpl.category.replace(/_/g, ' ')} size="small" color="primary" variant="outlined" />
                      {tpl.isSystem && <Chip label="System" size="small" color="default" />}
                    </Box>
                    <Typography variant="h6" gutterBottom>{tpl.name}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{tpl.description}</Typography>
                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                      <Chip size="small" label={`Entity: ${tpl.entityType}`} />
                      <Chip size="small" label={`Trigger: ${tpl.triggerEvent}`} />
                      <Chip size="small" label={`${nodes} nodes`} variant="outlined" />
                    </Stack>
                    <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                      <TrendIcon fontSize="small" color="action" />
                      <Typography variant="caption" color="text.secondary">
                        Popularity: {tpl.popularity}%
                      </Typography>
                      <LinearProgress variant="determinate" value={tpl.popularity} sx={{ flex: 1, ml: 1 }} />
                    </Box>
                  </CardContent>
                  <CardActions>
                    <Button size="small" startIcon={<CopyIcon />}>Use Template</Button>
                    <Button size="small" startIcon={<ViewIcon />}>Preview</Button>
                  </CardActions>
                </Card>
              </Grid>
            );
          })}
        </Grid>

        {/* Canvas preview placeholder */}
        <Paper variant="outlined" sx={{ mt: 4, p: 4, textAlign: 'center', bgcolor: 'grey.50', border: '2px dashed', borderColor: 'grey.300' }}>
          <BuilderIcon sx={{ fontSize: 64, color: 'grey.400', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">Visual Workflow Canvas</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Select a template above to load it into the visual builder, or drag & drop nodes to create a workflow from scratch.
          </Typography>
          <Stack direction="row" spacing={2} justifyContent="center">
            <Chip label="Trigger" color="primary" />
            <Typography variant="h6" color="text.secondary">&rarr;</Typography>
            <Chip label="Condition" color="warning" />
            <Typography variant="h6" color="text.secondary">&rarr;</Typography>
            <Chip label="Action" color="success" />
          </Stack>
        </Paper>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 1 – AI Workflow Suggestions
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={1}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6">AI-Powered Workflow Suggestions</Typography>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleGenerate}>
            Analyze & Generate
          </Button>
        </Box>

        {pendingSuggestions.length > 0 && (
          <Alert severity="info" sx={{ mb: 3 }}>
            You have <strong>{pendingSuggestions.length}</strong> pending suggestion(s). Review and accept to automate your workflows.
          </Alert>
        )}

        <Grid container spacing={3}>
          {suggestions.map(sug => (
            <Grid item xs={12} md={6} key={sug.id}>
              <Card variant="outlined" sx={{
                borderLeft: 4,
                borderLeftColor: sug.status === 'PENDING' ? 'warning.main' : sug.status === 'ACCEPTED' ? 'success.main' : 'grey.400',
              }}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Chip
                      label={sug.suggestionType.replace(/_/g, ' ')}
                      size="small"
                      color={suggestionTypeColor[sug.suggestionType] || 'default'}
                    />
                    <Chip
                      label={sug.status}
                      size="small"
                      color={sug.status === 'PENDING' ? 'warning' : sug.status === 'ACCEPTED' ? 'success' : 'default'}
                    />
                  </Box>
                  <Typography variant="h6" gutterBottom>{sug.name}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{sug.description}</Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <Typography variant="caption" color="text.secondary">Confidence:</Typography>
                    <LinearProgress
                      variant="determinate"
                      value={sug.confidence * 100}
                      sx={{ flex: 1 }}
                      color={sug.confidence > 0.85 ? 'success' : sug.confidence > 0.7 ? 'warning' : 'error'}
                    />
                    <Typography variant="caption" fontWeight={700}>{Math.round(sug.confidence * 100)}%</Typography>
                  </Box>
                  {sug.reason && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1, fontStyle: 'italic' }}>
                      {sug.reason}
                    </Typography>
                  )}
                </CardContent>
                {sug.status === 'PENDING' && (
                  <CardActions>
                    <Button size="small" color="success" startIcon={<AcceptIcon />} onClick={() => handleAccept(sug.id)}>
                      Accept & Create Rule
                    </Button>
                    <Button size="small" color="error" startIcon={<RejectIcon />} onClick={() => handleDismiss(sug.id)}>
                      Dismiss
                    </Button>
                  </CardActions>
                )}
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 2 – Lead Routing
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={2}>
        <Alert severity="success" sx={{ mb: 3 }}>
          Lead routing rules are managed through <strong>Leads &gt; Assignment Rules</strong>. Both direct assignment and round-robin rotation are supported.
        </Alert>

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  <RoutingIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                  Direct Assignment
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Route leads to a specific team member based on criteria like territory, source, company, or status.
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Typography variant="subtitle2" gutterBottom>How it works:</Typography>
                <Typography variant="body2" component="ul" sx={{ pl: 2 }}>
                  <li>Set criteria (e.g., territory = "West Coast")</li>
                  <li>Assign a specific user to handle matching leads</li>
                  <li>Rules are evaluated by priority (highest first)</li>
                  <li>First matching rule wins</li>
                </Typography>
              </CardContent>
              <CardActions>
                <Button size="small" href="/leads">Go to Leads &rarr;</Button>
              </CardActions>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  <AutoIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                  Round-Robin Assignment
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Distribute leads evenly across a team. The system automatically rotates through team members.
                </Typography>
                <Divider sx={{ mb: 2 }} />
                <Typography variant="subtitle2" gutterBottom>How it works:</Typography>
                <Typography variant="body2" component="ul" sx={{ pl: 2 }}>
                  <li>Set criteria to match incoming leads</li>
                  <li>Define a list of team members for rotation</li>
                  <li>Each new matching lead goes to the next person</li>
                  <li>Index resets after the last member</li>
                </Typography>
              </CardContent>
              <CardActions>
                <Button size="small" href="/leads">Configure Rules &rarr;</Button>
              </CardActions>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 3 – Follow-Up Reminders
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={3}>
        <Alert severity="success" sx={{ mb: 3 }}>
          Follow-up reminders are managed through <strong>Activities</strong>. Set a reminder date on any task, call, meeting, or email and get notified automatically.
        </Alert>

        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <Card variant="outlined">
              <CardContent sx={{ textAlign: 'center' }}>
                <ReminderIcon sx={{ fontSize: 48, color: 'primary.main', mb: 1 }} />
                <Typography variant="h6">Automatic Reminders</Typography>
                <Typography variant="body2" color="text.secondary">
                  Set a reminder date on any activity and the system will notify you and the assignee when it's due.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card variant="outlined">
              <CardContent sx={{ textAlign: 'center' }}>
                <AutoIcon sx={{ fontSize: 48, color: 'warning.main', mb: 1 }} />
                <Typography variant="h6">Recurring Tasks</Typography>
                <Typography variant="body2" color="text.secondary">
                  Create recurring activities (daily, weekly, biweekly, monthly) that automatically spawn the next occurrence when completed.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card variant="outlined">
              <CardContent sx={{ textAlign: 'center' }}>
                <AiIcon sx={{ fontSize: 48, color: 'secondary.main', mb: 1 }} />
                <Typography variant="h6">Smart Notifications</Typography>
                <Typography variant="body2" color="text.secondary">
                  Both the activity creator and assignee receive notifications. Reminder events are published via Kafka for real-time delivery.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        <Paper variant="outlined" sx={{ mt: 3, p: 3 }}>
          <Typography variant="h6" gutterBottom>Reminder Flow</Typography>
          <Stack direction="row" spacing={2} alignItems="center" justifyContent="center" sx={{ py: 2 }}>
            <Chip label="1. Create Activity" color="primary" />
            <Typography>&rarr;</Typography>
            <Chip label="2. Set Reminder Date" color="warning" />
            <Typography>&rarr;</Typography>
            <Chip label="3. System Checks Every 60s" color="info" />
            <Typography>&rarr;</Typography>
            <Chip label="4. Notification Sent" color="success" />
          </Stack>
        </Paper>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 4 – Proposals
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={4}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6">Automated Proposals</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setProposalDialog(true)}>
            New Proposal
          </Button>
        </Box>

        <Grid container spacing={3}>
          {proposals.map(p => (
            <Grid item xs={12} md={6} lg={4} key={p.id}>
              <Card variant="outlined">
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Chip label={p.status} size="small" color={proposalStatusColor[p.status] || 'default'} />
                    <Typography variant="caption" color="text.secondary">v{p.version}</Typography>
                  </Box>
                  <Typography variant="h6" noWrap>{p.title}</Typography>
                  <Typography variant="h5" color="primary.main" fontWeight={700} sx={{ my: 1 }}>
                    ${p.amount.toLocaleString()}
                  </Typography>
                  {p.recipientName && (
                    <Typography variant="body2" color="text.secondary">To: {p.recipientName}</Typography>
                  )}
                  {p.recipientEmail && (
                    <Typography variant="caption" color="text.secondary">{p.recipientEmail}</Typography>
                  )}
                  {p.lineItems.length > 0 && (
                    <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                      {p.lineItems.length} line item(s)
                    </Typography>
                  )}
                  {p.validUntil && (
                    <Typography variant="caption" display="block" color="text.secondary">
                      Valid until: {new Date(p.validUntil).toLocaleDateString()}
                    </Typography>
                  )}
                </CardContent>
                <CardActions>
                  <IconButton size="small" onClick={() => setViewProposal(p)}><ViewIcon /></IconButton>
                  {p.status === 'DRAFT' && (
                    <IconButton size="small" color="primary" onClick={() => automationService.sendProposal(p.id).then(loadProposals)}>
                      <SendIcon />
                    </IconButton>
                  )}
                  {p.status === 'SENT' && (
                    <>
                      <IconButton size="small" color="success" onClick={() => automationService.acceptProposal(p.id).then(loadProposals)}>
                        <AcceptIcon />
                      </IconButton>
                      <IconButton size="small" color="error" onClick={() => automationService.rejectProposal(p.id).then(loadProposals)}>
                        <RejectIcon />
                      </IconButton>
                    </>
                  )}
                  {p.status === 'DRAFT' && (
                    <IconButton size="small" color="error" onClick={() => automationService.deleteProposal(p.id).then(loadProposals)}>
                      <DeleteIcon />
                    </IconButton>
                  )}
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          TAB 5 – Contracts
          ════════════════════════════════════════════════════════ */}
      <TabPanel value={tab} index={5}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6">Automated Contracts</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setContractDialog(true)}>
            New Contract
          </Button>
        </Box>

        <Grid container spacing={3}>
          {contracts.map(c => (
            <Grid item xs={12} md={6} lg={4} key={c.id}>
              <Card variant="outlined">
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Chip label={c.status} size="small" color={contractStatusColor[c.status] || 'default'} />
                    <Typography variant="caption" color="text.secondary">v{c.version}</Typography>
                  </Box>
                  <Typography variant="h6" noWrap>{c.title}</Typography>
                  <Typography variant="h5" color="primary.main" fontWeight={700} sx={{ my: 1 }}>
                    ${c.amount.toLocaleString()}
                  </Typography>
                  {c.signerName && (
                    <Typography variant="body2" color="text.secondary">Signer: {c.signerName}</Typography>
                  )}
                  {c.startDate && c.endDate && (
                    <Typography variant="caption" display="block" color="text.secondary">
                      {new Date(c.startDate).toLocaleDateString()} – {new Date(c.endDate).toLocaleDateString()}
                    </Typography>
                  )}
                </CardContent>
                <CardActions>
                  <IconButton size="small" onClick={() => setViewContract(c)}><ViewIcon /></IconButton>
                  {c.status === 'DRAFT' && (
                    <IconButton size="small" color="primary" onClick={() => automationService.sendContract(c.id).then(loadContracts)}>
                      <SendIcon />
                    </IconButton>
                  )}
                  {c.status === 'SENT' && (
                    <Tooltip title="Sign Contract">
                      <IconButton size="small" color="success" onClick={() => setSignDialog(c.id)}>
                        <SignIcon />
                      </IconButton>
                    </Tooltip>
                  )}
                  {c.status === 'SIGNED' && (
                    <Tooltip title="Execute Contract">
                      <IconButton size="small" color="primary" onClick={() => automationService.executeContract(c.id).then(loadContracts)}>
                        <ExecuteIcon />
                      </IconButton>
                    </Tooltip>
                  )}
                  {(c.status === 'DRAFT' || c.status === 'SENT') && (
                    <IconButton size="small" color="error" onClick={() => automationService.cancelContract(c.id).then(loadContracts)}>
                      <CancelIcon />
                    </IconButton>
                  )}
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      {/* ════════════════════════════════════════════════════════
          DIALOGS
          ════════════════════════════════════════════════════════ */}

      {/* ── Create Proposal Dialog ──────────────────────────── */}
      <Dialog open={proposalDialog} onClose={() => setProposalDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create Proposal</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField fullWidth label="Title" value={proposalForm.title}
                onChange={e => setProposalForm(f => ({ ...f, title: e.target.value }))} />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="Recipient Name" value={proposalForm.recipientName}
                onChange={e => setProposalForm(f => ({ ...f, recipientName: e.target.value }))} />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="Recipient Email" value={proposalForm.recipientEmail}
                onChange={e => setProposalForm(f => ({ ...f, recipientEmail: e.target.value }))} />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Opportunity ID" value={proposalForm.opportunityId}
                onChange={e => setProposalForm(f => ({ ...f, opportunityId: e.target.value }))} />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Line Items</Typography>
              {(proposalForm.lineItems ?? []).map((li, idx) => (
                <Stack key={idx} direction="row" spacing={1} sx={{ mb: 1 }}>
                  <TextField size="small" label="Product" value={li.productName}
                    onChange={e => {
                      const items = [...(proposalForm.lineItems ?? [])];
                      items[idx] = { ...items[idx], productName: e.target.value };
                      setProposalForm(f => ({ ...f, lineItems: items }));
                    }} />
                  <TextField size="small" label="Qty" type="number" sx={{ width: 80 }} value={li.quantity}
                    onChange={e => {
                      const items = [...(proposalForm.lineItems ?? [])];
                      items[idx] = { ...items[idx], quantity: Number(e.target.value) };
                      setProposalForm(f => ({ ...f, lineItems: items }));
                    }} />
                  <TextField size="small" label="Unit Price" type="number" sx={{ width: 120 }} value={li.unitPrice}
                    onChange={e => {
                      const items = [...(proposalForm.lineItems ?? [])];
                      items[idx] = { ...items[idx], unitPrice: Number(e.target.value) };
                      setProposalForm(f => ({ ...f, lineItems: items }));
                    }} />
                  <IconButton color="error" onClick={() => {
                    const items = (proposalForm.lineItems ?? []).filter((_, i) => i !== idx);
                    setProposalForm(f => ({ ...f, lineItems: items }));
                  }}><DeleteIcon /></IconButton>
                </Stack>
              ))}
              <Button size="small" startIcon={<AddIcon />} onClick={() =>
                setProposalForm(f => ({ ...f, lineItems: [...(f.lineItems ?? []), { productName: '', quantity: 1, unitPrice: 0 }] }))
              }>Add Item</Button>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setProposalDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateProposal} disabled={!proposalForm.title}>Create</Button>
        </DialogActions>
      </Dialog>

      {/* ── View Proposal Dialog ────────────────────────────── */}
      <Dialog open={!!viewProposal} onClose={() => setViewProposal(null)} maxWidth="md" fullWidth>
        <DialogTitle>{viewProposal?.title}</DialogTitle>
        <DialogContent>
          {viewProposal && (
            <Box>
              <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <Chip label={viewProposal.status} color={proposalStatusColor[viewProposal.status] || 'default'} />
                <Typography variant="h5" color="primary.main" fontWeight={700}>
                  ${viewProposal.amount.toLocaleString()}
                </Typography>
              </Box>
              {viewProposal.lineItems.length > 0 && (
                <Table size="small" sx={{ mb: 2 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Product</TableCell>
                      <TableCell align="right">Qty</TableCell>
                      <TableCell align="right">Price</TableCell>
                      <TableCell align="right">Discount</TableCell>
                      <TableCell align="right">Total</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {viewProposal.lineItems.map((li, idx) => (
                      <TableRow key={idx}>
                        <TableCell>{li.productName}</TableCell>
                        <TableCell align="right">{li.quantity}</TableCell>
                        <TableCell align="right">${li.unitPrice.toLocaleString()}</TableCell>
                        <TableCell align="right">{li.discount}%</TableCell>
                        <TableCell align="right">${li.totalPrice.toLocaleString()}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
              {viewProposal.content && (
                <Paper variant="outlined" sx={{ p: 2, whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 13 }}>
                  {viewProposal.content}
                </Paper>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewProposal(null)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* ── Create Contract Dialog ──────────────────────────── */}
      <Dialog open={contractDialog} onClose={() => setContractDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Contract</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField fullWidth label="Title" value={contractForm.title}
                onChange={e => setContractForm(f => ({ ...f, title: e.target.value }))} />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Opportunity ID" value={contractForm.opportunityId}
                onChange={e => setContractForm(f => ({ ...f, opportunityId: e.target.value }))} />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="Amount" type="number" value={contractForm.amount}
                onChange={e => setContractForm(f => ({ ...f, amount: Number(e.target.value) }))} />
            </Grid>
            <Grid item xs={3}>
              <TextField fullWidth label="Start Date" type="date" InputLabelProps={{ shrink: true }}
                value={contractForm.startDate ?? ''}
                onChange={e => setContractForm(f => ({ ...f, startDate: e.target.value }))} />
            </Grid>
            <Grid item xs={3}>
              <TextField fullWidth label="End Date" type="date" InputLabelProps={{ shrink: true }}
                value={contractForm.endDate ?? ''}
                onChange={e => setContractForm(f => ({ ...f, endDate: e.target.value }))} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setContractDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateContract} disabled={!contractForm.title}>Create</Button>
        </DialogActions>
      </Dialog>

      {/* ── View Contract Dialog ────────────────────────────── */}
      <Dialog open={!!viewContract} onClose={() => setViewContract(null)} maxWidth="md" fullWidth>
        <DialogTitle>{viewContract?.title}</DialogTitle>
        <DialogContent>
          {viewContract && (
            <Box>
              <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <Chip label={viewContract.status} color={contractStatusColor[viewContract.status] || 'default'} />
                <Typography variant="h5" color="primary.main" fontWeight={700}>
                  ${viewContract.amount.toLocaleString()}
                </Typography>
              </Box>
              {viewContract.signerName && (
                <Typography variant="body2" sx={{ mb: 1 }}>
                  Signed by: {viewContract.signerName} ({viewContract.signerEmail})
                </Typography>
              )}
              {viewContract.content && (
                <Paper variant="outlined" sx={{ p: 2, whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 13 }}>
                  {viewContract.content}
                </Paper>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewContract(null)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* ── Sign Contract Dialog ────────────────────────────── */}
      <Dialog open={!!signDialog} onClose={() => setSignDialog(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Sign Contract</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={6}>
              <TextField fullWidth label="Signer Name" value={signForm.signerName}
                onChange={e => setSignForm(f => ({ ...f, signerName: e.target.value }))} />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="Signer Email" value={signForm.signerEmail}
                onChange={e => setSignForm(f => ({ ...f, signerEmail: e.target.value }))} />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth multiline rows={3} label="Signature (type your name as signature)"
                value={signForm.signatureData}
                onChange={e => setSignForm(f => ({ ...f, signatureData: e.target.value }))} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSignDialog(null)}>Cancel</Button>
          <Button variant="contained" color="success" startIcon={<SignIcon />}
            onClick={handleSignContract} disabled={!signForm.signerName || !signForm.signerEmail}>
            Sign
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SmartAutomationPage;
