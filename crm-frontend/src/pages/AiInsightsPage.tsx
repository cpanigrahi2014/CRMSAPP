/* ============================================================
   AiInsightsPage – AI & Intelligence (11 Features)
   1. Predictive Lead Scoring  2. Opportunity Win Probability
   3. Sales Forecasting AI     4. Customer Churn Prediction
   5. Next Best Action         6. AI-powered Reporting
   7. Automated Data Entry     8. AI Sales Insights
   9. Email Reply Generation  10. Meeting Summary & CRM Update
  11. Auto-Create Leads
   ============================================================ */
import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Tabs, Tab, Card, CardContent, CardActions,
  Button, Chip, Table, TableHead, TableBody, TableRow, TableCell,
  Paper, Grid, Stack, LinearProgress, Tooltip, IconButton,
  Dialog, DialogTitle, DialogContent, DialogActions, Alert,
  Snackbar, Divider, TextField, Select, MenuItem, FormControl,
  InputLabel, Avatar,
  type SelectChangeEvent,
} from '@mui/material';
import {
  TrendingUp, TrendingDown, TrendingFlat,
  CheckCircle, Cancel, Refresh as RefreshIcon,
  EmojiEvents as WinIcon, Warning as WarnIcon,
  Psychology as AiIcon, Lightbulb as IdeaIcon,
  AutoFixHigh as MagicIcon, Speed as SpeedIcon,
  AssessmentOutlined as ReportIcon,
  NotificationsActive as AlertIcon,
  Phone as CallIcon, Email as EmailIcon,
  Event as MeetingIcon, Description as ProposalIcon,
  Loyalty as RetentionIcon, TrendingUp as UpsellIcon,
  FollowTheSigns as FollowUpIcon,
  ThumbUp, ThumbDown, Done as DoneIcon,
  ShowChart as ChartIcon, BubbleChart as BubbleIcon,
  FilterList as FilterIcon,
  Reply as ReplyIcon,
  Summarize as SummarizeIcon,
  PersonAdd as PersonAddIcon,
} from '@mui/icons-material';
import { aiInsightsService, GeneratedMeetingSummary, GeneratedEmailDraft } from '../services/aiInsightsService';
import type {
  PredictiveLeadScore, WinProbability, SalesForecast, ChurnPrediction,
  NextBestAction, AiReportInsight, DataEntrySuggestion, AiSalesInsight,
  EmailReply, MeetingSummary, AutoLead,
} from '../types';
import { PageHeader } from '../components';
/* ── Helpers ─────────────────────────────────────────────────── */
const pct = (v: number) => `${(v * 100).toFixed(0)}%`;
const usd = (v: number) => `$${v.toLocaleString()}`;
const ago = (ts?: string) => {
  if (!ts) return '—';
  const mins = Math.floor((Date.now() - new Date(ts).getTime()) / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
};

const trendIcon = (t: string) => {
  switch (t) {
    case 'RISING': return <TrendingUp fontSize="small" color="success" />;
    case 'DECLINING': return <TrendingDown fontSize="small" color="error" />;
    default: return <TrendingFlat fontSize="small" color="action" />;
  }
};
const riskColor = (r: string): 'error' | 'warning' | 'info' | 'success' => {
  switch (r) { case 'CRITICAL': return 'error'; case 'HIGH': return 'error'; case 'MEDIUM': return 'warning'; case 'LOW': return 'success'; default: return 'info'; }
};
const insightIcon = (t: string) => {
  switch (t) { case 'TREND': return <ChartIcon color="primary" />; case 'ANOMALY': return <BubbleIcon color="warning" />; case 'PREDICTION': return <AiIcon color="info" />; case 'ALERT': return <AlertIcon color="error" />; default: return <IdeaIcon color="success" />; }
};
const actionCategoryIcon = (c: string) => {
  switch (c) { case 'CALL': return <CallIcon fontSize="small" />; case 'EMAIL': return <EmailIcon fontSize="small" />; case 'MEETING': return <MeetingIcon fontSize="small" />; case 'PROPOSAL': return <ProposalIcon fontSize="small" />; case 'RETENTION': return <RetentionIcon fontSize="small" />; case 'UPSELL': return <UpsellIcon fontSize="small" />; case 'FOLLOW_UP': return <FollowUpIcon fontSize="small" />; default: return <IdeaIcon fontSize="small" />; }
};
const severityColor = (s: string): 'error' | 'warning' | 'info' => {
  switch (s) { case 'high': return 'error'; case 'medium': return 'warning'; default: return 'info'; }
};

/* ── TabPanel ────────────────────────────────────────────────── */
const TabPanel: React.FC<{ value: number; index: number; children: React.ReactNode }> = ({ value, index, children }) =>
  value === index ? <Box sx={{ pt: 2 }}>{children}</Box> : null;

/* ================================================================
   MAIN COMPONENT
   ================================================================ */
const AiInsightsPage: React.FC = () => {
  const [tab, setTab] = useState(0);
  const [snack, setSnack] = useState<{ open: boolean; msg: string; severity: 'success' | 'error' | 'info' }>({ open: false, msg: '', severity: 'success' });

  /* data */
  const [leadScores, setLeadScores] = useState<PredictiveLeadScore[]>([]);
  const [winProbs, setWinProbs] = useState<WinProbability[]>([]);
  const [forecasts, setForecasts] = useState<SalesForecast[]>([]);
  const [churnPreds, setChurnPreds] = useState<ChurnPrediction[]>([]);
  const [actions, setActions] = useState<NextBestAction[]>([]);
  const [reportInsights, setReportInsights] = useState<AiReportInsight[]>([]);
  const [suggestions, setSuggestions] = useState<DataEntrySuggestion[]>([]);
  const [salesInsights, setSalesInsights] = useState<AiSalesInsight[]>([]);
  const [emailReplies, setEmailReplies] = useState<EmailReply[]>([]);
  const [meetingSummaries, setMeetingSummaries] = useState<MeetingSummary[]>([]);
  const [autoLeads, setAutoLeads] = useState<AutoLead[]>([]);

  /* detail dialogs */
  const [selectedLead, setSelectedLead] = useState<PredictiveLeadScore | null>(null);
  const [selectedOpp, setSelectedOpp] = useState<WinProbability | null>(null);
  const [selectedChurn, setSelectedChurn] = useState<ChurnPrediction | null>(null);
  const [selectedInsight, setSelectedInsight] = useState<AiSalesInsight | null>(null);

  /* filters */
  const [actionFilter, setActionFilter] = useState<string>('ALL');
  const [insightFilter, setInsightFilter] = useState<string>('ALL');

  /* Interactive AI forms */
  const [meetingInput, setMeetingInput] = useState({ title: '', notes: '' });
  const [meetingResult, setMeetingResult] = useState<GeneratedMeetingSummary | null>(null);
  const [meetingGenLoading, setMeetingGenLoading] = useState(false);
  const [emailInput, setEmailInput] = useState({ to: '', subjectContext: '', tone: 'professional' });
  const [emailResult, setEmailResult] = useState<GeneratedEmailDraft | null>(null);
  const [emailGenLoading, setEmailGenLoading] = useState(false);

  useEffect(() => {
    // Immediate sync load from localStorage
    setLeadScores(aiInsightsService.getLeadScores());
    setWinProbs(aiInsightsService.getWinProbabilities());
    setForecasts(aiInsightsService.getForecasts());
    setChurnPreds(aiInsightsService.getChurnPredictions());
    setActions(aiInsightsService.getNextBestActions());
    setReportInsights(aiInsightsService.getReportInsights());
    setSuggestions(aiInsightsService.getSuggestions());
    setSalesInsights(aiInsightsService.getSalesInsights());
    setEmailReplies(aiInsightsService.getEmailReplies());
    setMeetingSummaries(aiInsightsService.getMeetingSummaries());
    setAutoLeads(aiInsightsService.getAutoLeads());
    // Try real API (overrides if backend is running)
    const load = async () => {
      const [ls, wp, fc, cp, ri, sg, si, er, ms, al] = await Promise.all([
        aiInsightsService.fetchLeadScores(),
        aiInsightsService.fetchWinProbabilities(),
        aiInsightsService.fetchForecasts(),
        aiInsightsService.fetchChurnPredictions(),
        aiInsightsService.fetchReportInsights(),
        aiInsightsService.fetchSuggestions(),
        aiInsightsService.fetchSalesInsights(),
        aiInsightsService.fetchEmailReplies(),
        aiInsightsService.fetchMeetingSummaries(),
        aiInsightsService.fetchAutoLeads(),
      ]);
      if (Array.isArray(ls)) setLeadScores(ls);
      if (Array.isArray(wp)) setWinProbs(wp);
      if (Array.isArray(fc)) setForecasts(fc);
      if (Array.isArray(cp)) setChurnPreds(cp);
      if (Array.isArray(ri)) setReportInsights(ri);
      if (Array.isArray(sg)) setSuggestions(sg);
      if (Array.isArray(si)) setSalesInsights(si);
      if (Array.isArray(er)) setEmailReplies(er);
      if (Array.isArray(ms)) setMeetingSummaries(ms);
      if (Array.isArray(al)) setAutoLeads(al);
    };
    load();
  }, []);

  const notify = (msg: string, severity: 'success' | 'error' | 'info' = 'success') =>
    setSnack({ open: true, msg, severity });

  /* ── Action helpers ──────────────────────────────────────── */
  const completeAction = (id: string) => {
    const updated = actions.map(a => a.id === id ? { ...a, completed: true } : a);
    setActions(updated);
    aiInsightsService.saveNextBestActions(updated);
    notify('Action marked complete');
  };

  const acceptSuggestion = (id: string) => {
    const updated = suggestions.filter(s => s.id !== id);
    setSuggestions(updated);
    aiInsightsService.saveSuggestions(updated);
    notify('Suggestion accepted and applied');
  };
  const dismissSuggestion = (id: string) => {
    const updated = suggestions.filter(s => s.id !== id);
    setSuggestions(updated);
    aiInsightsService.saveSuggestions(updated);
    notify('Suggestion dismissed', 'info');
  };

  const refreshScores = () => {
    setLeadScores(aiInsightsService.getLeadScores());
    notify('Lead scores refreshed', 'info');
  };

  const filteredActions = actionFilter === 'ALL' ? actions : actions.filter(a => a.category === actionFilter);
  const filteredInsights = insightFilter === 'ALL' ? salesInsights : salesInsights.filter(i => i.insightType === insightFilter);

  /* ================================================================
     RENDER
     ================================================================ */
  return (
    <Box>
      <PageHeader
        title="AI & Intelligence"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'AI Insights' }]}
        action={<Chip icon={<AiIcon />} label="Powered by AI" color="primary" variant="outlined" />}
      />

      <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto" sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="Lead Scoring" icon={<SpeedIcon />} iconPosition="start" />
        <Tab label="Win Probability" icon={<WinIcon />} iconPosition="start" />
        <Tab label="Forecasting" icon={<ChartIcon />} iconPosition="start" />
        <Tab label="Churn Prediction" icon={<WarnIcon />} iconPosition="start" />
        <Tab label="Next Best Action" icon={<IdeaIcon />} iconPosition="start" />
        <Tab label="AI Reports" icon={<ReportIcon />} iconPosition="start" />
        <Tab label="Data Suggestions" icon={<MagicIcon />} iconPosition="start" />
        <Tab label="Sales Insights" icon={<AiIcon />} iconPosition="start" />
        <Tab label="Email Reply" icon={<ReplyIcon />} iconPosition="start" />
        <Tab label="Meeting Summary" icon={<SummarizeIcon />} iconPosition="start" />
        <Tab label="Auto-Lead" icon={<PersonAddIcon />} iconPosition="start" />
      </Tabs>

      {/* ── 1. Predictive Lead Scoring ─────────────────────────── */}
      <TabPanel value={tab} index={0}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h6">Predictive Lead Scoring</Typography>
          <Button startIcon={<RefreshIcon />} onClick={refreshScores}>Refresh Scores</Button>
        </Stack>
        <Grid container spacing={2}>
          {leadScores.map(l => (
            <Grid item xs={12} md={6} lg={4} key={l.id}>
              <Card variant="outlined" sx={{ cursor: 'pointer' }} onClick={() => setSelectedLead(l)}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle1" fontWeight={600}>{l.leadName}</Typography>
                    {trendIcon(l.trend)}
                  </Stack>
                  <Typography variant="body2" color="text.secondary">{l.company} · {l.email}</Typography>
                  <Box mt={2}>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2">Current Score</Typography>
                      <Typography variant="body2" fontWeight={600}>{l.currentScore}</Typography>
                    </Stack>
                    <LinearProgress variant="determinate" value={l.currentScore} sx={{ my: 0.5, height: 8, borderRadius: 4 }} color={l.currentScore >= 70 ? 'success' : l.currentScore >= 40 ? 'warning' : 'error'} />
                    <Stack direction="row" justifyContent="space-between" mt={1}>
                      <Typography variant="body2">Predicted Score</Typography>
                      <Typography variant="body2" fontWeight={600}>{l.predictedScore}</Typography>
                    </Stack>
                    <LinearProgress variant="determinate" value={l.predictedScore} sx={{ my: 0.5, height: 8, borderRadius: 4 }} color="primary" />
                  </Box>
                  <Divider sx={{ my: 1.5 }} />
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">Conversion Probability</Typography>
                    <Chip label={pct(l.conversionProbability)} size="small" color={l.conversionProbability >= 0.7 ? 'success' : l.conversionProbability >= 0.4 ? 'warning' : 'error'} />
                  </Stack>
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>Updated {ago(l.lastUpdated)}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        {/* Lead Score Detail */}
        <Dialog open={!!selectedLead} onClose={() => setSelectedLead(null)} maxWidth="sm" fullWidth>
          {selectedLead && (
            <>
              <DialogTitle>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  {selectedLead.leadName}
                  <Chip label={selectedLead.trend} size="small" icon={trendIcon(selectedLead.trend)} />
                </Stack>
              </DialogTitle>
              <DialogContent dividers>
                <Typography variant="body2" gutterBottom><strong>Company:</strong> {selectedLead.company}</Typography>
                <Typography variant="body2" gutterBottom><strong>Email:</strong> {selectedLead.email}</Typography>
                <Typography variant="body2" gutterBottom><strong>Score:</strong> {selectedLead.currentScore} → {selectedLead.predictedScore}</Typography>
                <Typography variant="body2" gutterBottom><strong>Conversion Prob:</strong> {pct(selectedLead.conversionProbability)}</Typography>
                <Divider sx={{ my: 2 }} />
                <Typography variant="subtitle2" gutterBottom>Top Scoring Factors</Typography>
                {selectedLead.topFactors.map((f, i) => (
                  <Stack key={i} direction="row" alignItems="center" spacing={1} sx={{ mb: 0.5 }}>
                    {f.direction === 'positive' ? <ThumbUp fontSize="small" color="success" /> : <ThumbDown fontSize="small" color="error" />}
                    <Typography variant="body2">{f.factor}</Typography>
                    <Chip label={`${f.impact > 0 ? '+' : ''}${f.impact}`} size="small" color={f.direction === 'positive' ? 'success' : 'error'} variant="outlined" />
                  </Stack>
                ))}
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setSelectedLead(null)}>Close</Button>
              </DialogActions>
            </>
          )}
        </Dialog>
      </TabPanel>

      {/* ── 2. Win Probability ─────────────────────────────────── */}
      <TabPanel value={tab} index={1}>
        <Typography variant="h6" mb={2}>Opportunity Win Probability</Typography>
        <Grid container spacing={2}>
          {winProbs.map(w => (
            <Grid item xs={12} md={6} key={w.id}>
              <Card variant="outlined" sx={{ cursor: 'pointer' }} onClick={() => setSelectedOpp(w)}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="subtitle1" fontWeight={600}>{w.opportunityName}</Typography>
                    <Avatar sx={{ bgcolor: w.winProbability >= 70 ? 'success.main' : w.winProbability >= 40 ? 'warning.main' : 'error.main', width: 48, height: 48, fontSize: 16, fontWeight: 700 }}>
                      {w.winProbability}%
                    </Avatar>
                  </Stack>
                  <Typography variant="body2" color="text.secondary">{w.accountName} · {usd(w.amount)}</Typography>
                  <Stack direction="row" spacing={1} mt={1.5}>
                    <Chip label={w.stage.replace(/_/g, ' ')} size="small" variant="outlined" />
                    <Chip label={`${w.daysInStage}d in stage`} size="small" variant="outlined" />
                    <Chip label={`Hist: ${w.historicalWinRate}%`} size="small" variant="outlined" />
                  </Stack>
                  <Box mt={2}>
                    <LinearProgress variant="determinate" value={w.winProbability} sx={{ height: 10, borderRadius: 5 }} color={w.winProbability >= 70 ? 'success' : w.winProbability >= 40 ? 'warning' : 'error'} />
                  </Box>
                  {w.riskFactors.length > 0 && (
                    <Alert severity="warning" sx={{ mt: 1.5 }} variant="outlined">
                      {w.riskFactors[0]}
                    </Alert>
                  )}
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        {/* Win Prob Detail */}
        <Dialog open={!!selectedOpp} onClose={() => setSelectedOpp(null)} maxWidth="sm" fullWidth>
          {selectedOpp && (
            <>
              <DialogTitle>{selectedOpp.opportunityName}</DialogTitle>
              <DialogContent dividers>
                <Typography variant="body2" gutterBottom><strong>Account:</strong> {selectedOpp.accountName}</Typography>
                <Typography variant="body2" gutterBottom><strong>Amount:</strong> {usd(selectedOpp.amount)}</Typography>
                <Typography variant="body2" gutterBottom><strong>Win Probability:</strong> {selectedOpp.winProbability}%</Typography>
                <Typography variant="body2" gutterBottom><strong>Historical Win Rate:</strong> {selectedOpp.historicalWinRate}%</Typography>
                <Divider sx={{ my: 2 }} />
                <Typography variant="subtitle2" color="error" gutterBottom>Risk Factors</Typography>
                {selectedOpp.riskFactors.map((r, i) => <Typography key={i} variant="body2" sx={{ mb: 0.5 }}>• {r}</Typography>)}
                <Typography variant="subtitle2" color="success.main" sx={{ mt: 2 }} gutterBottom>Positive Signals</Typography>
                {selectedOpp.positiveSignals.map((s, i) => <Typography key={i} variant="body2" sx={{ mb: 0.5 }}>✓ {s}</Typography>)}
                <Divider sx={{ my: 2 }} />
                <Alert severity="info" icon={<IdeaIcon />}>{selectedOpp.recommendation}</Alert>
              </DialogContent>
              <DialogActions><Button onClick={() => setSelectedOpp(null)}>Close</Button></DialogActions>
            </>
          )}
        </Dialog>
      </TabPanel>

      {/* ── 3. Sales Forecasting ──────────────────────────────── */}
      <TabPanel value={tab} index={2}>
        <Typography variant="h6" mb={2}>Sales Forecasting AI</Typography>
        <Grid container spacing={2}>
          {forecasts.map(f => (
            <Grid item xs={12} md={6} key={f.id}>
              <Card variant="outlined">
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="h6">{f.periodLabel}</Typography>
                    <Chip label={f.confidence} size="small" color={f.confidence === 'HIGH' ? 'success' : f.confidence === 'MEDIUM' ? 'warning' : 'error'} />
                  </Stack>
                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">Predicted Revenue</Typography>
                      <Typography variant="h5" fontWeight={700} color="primary">{usd(f.predictedRevenue)}</Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">Quota</Typography>
                      <Typography variant="h5" fontWeight={700}>{usd(f.quota)}</Typography>
                    </Grid>
                    <Grid item xs={4}>
                      <Typography variant="caption" color="text.secondary">Best Case</Typography>
                      <Typography variant="body2" color="success.main" fontWeight={600}>{usd(f.bestCase)}</Typography>
                    </Grid>
                    <Grid item xs={4}>
                      <Typography variant="caption" color="text.secondary">Worst Case</Typography>
                      <Typography variant="body2" color="error" fontWeight={600}>{usd(f.worstCase)}</Typography>
                    </Grid>
                    <Grid item xs={4}>
                      <Typography variant="caption" color="text.secondary">Closed to Date</Typography>
                      <Typography variant="body2" fontWeight={600}>{usd(f.closedToDate)}</Typography>
                    </Grid>
                  </Grid>
                  <Box mt={2}>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2">Pipeline Coverage</Typography>
                      <Typography variant="body2" fontWeight={600}>{usd(f.pipelineValue)} ({(f.pipelineValue / f.quota).toFixed(1)}x)</Typography>
                    </Stack>
                    <LinearProgress variant="determinate" value={Math.min(100, (f.predictedRevenue / f.quota) * 100)} sx={{ height: 8, borderRadius: 4, mt: 0.5 }} color={f.predictedRevenue >= f.quota ? 'success' : f.predictedRevenue >= f.quota * 0.8 ? 'warning' : 'error'} />
                  </Box>
                  {f.attainmentPct > 0 && (
                    <Chip label={`${f.attainmentPct}% Attainment`} size="small" sx={{ mt: 1.5 }} color={f.attainmentPct >= 100 ? 'success' : f.attainmentPct >= 70 ? 'warning' : 'error'} />
                  )}
                  <Divider sx={{ my: 1.5 }} />
                  <Typography variant="subtitle2" gutterBottom>Key Factors</Typography>
                  {f.factors.map((fct, i) => <Typography key={i} variant="body2" color="text.secondary">• {fct}</Typography>)}
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      {/* ── 4. Customer Churn Prediction ──────────────────────── */}
      <TabPanel value={tab} index={3}>
        <Typography variant="h6" mb={2}>Customer Churn Prediction</Typography>
        <Grid container spacing={2}>
          {churnPreds.sort((a, b) => b.churnProbability - a.churnProbability).map(c => (
            <Grid item xs={12} md={6} lg={4} key={c.id}>
              <Card variant="outlined" sx={{ borderLeft: 4, borderColor: c.riskLevel === 'CRITICAL' ? 'error.main' : c.riskLevel === 'HIGH' ? 'error.light' : c.riskLevel === 'MEDIUM' ? 'warning.main' : 'success.main', cursor: 'pointer' }} onClick={() => setSelectedChurn(c)}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="subtitle1" fontWeight={600}>{c.accountName}</Typography>
                    <Chip label={c.riskLevel} size="small" color={riskColor(c.riskLevel)} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary">{c.industry} · ARR: {usd(c.annualRevenue)}</Typography>
                  <Box mt={2}>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2">Churn Probability</Typography>
                      <Typography variant="body2" fontWeight={600} color="error">{pct(c.churnProbability)}</Typography>
                    </Stack>
                    <LinearProgress variant="determinate" value={c.churnProbability * 100} sx={{ height: 8, borderRadius: 4, mt: 0.5 }} color="error" />
                  </Box>
                  <Stack direction="row" justifyContent="space-between" mt={1.5}>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Health Score</Typography>
                      <Typography variant="body2" fontWeight={600}>{c.healthScore}/100</Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">Last Activity</Typography>
                      <Typography variant="body2" fontWeight={600}>{c.lastActivityDays}d ago</Typography>
                    </Box>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        {/* Churn Detail */}
        <Dialog open={!!selectedChurn} onClose={() => setSelectedChurn(null)} maxWidth="sm" fullWidth>
          {selectedChurn && (
            <>
              <DialogTitle>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  {selectedChurn.accountName}
                  <Chip label={selectedChurn.riskLevel} color={riskColor(selectedChurn.riskLevel)} />
                </Stack>
              </DialogTitle>
              <DialogContent dividers>
                <Typography variant="body2" gutterBottom><strong>Industry:</strong> {selectedChurn.industry}</Typography>
                <Typography variant="body2" gutterBottom><strong>Annual Revenue:</strong> {usd(selectedChurn.annualRevenue)}</Typography>
                <Typography variant="body2" gutterBottom><strong>Churn Probability:</strong> {pct(selectedChurn.churnProbability)}</Typography>
                <Typography variant="body2" gutterBottom><strong>Health Score:</strong> {selectedChurn.healthScore}/100</Typography>
                {selectedChurn.predictedChurnDate && (
                  <Typography variant="body2" gutterBottom><strong>Predicted Churn:</strong> {new Date(selectedChurn.predictedChurnDate).toLocaleDateString()}</Typography>
                )}
                <Divider sx={{ my: 2 }} />
                <Typography variant="subtitle2" color="error" gutterBottom>Risk Factors</Typography>
                {selectedChurn.riskFactors.map((r, i) => <Typography key={i} variant="body2" sx={{ mb: 0.5 }}>⚠ {r}</Typography>)}
                <Divider sx={{ my: 2 }} />
                <Typography variant="subtitle2" color="primary" gutterBottom>Recommended Actions</Typography>
                {selectedChurn.recommendedActions.map((a, i) => <Typography key={i} variant="body2" sx={{ mb: 0.5 }}>→ {a}</Typography>)}
              </DialogContent>
              <DialogActions><Button onClick={() => setSelectedChurn(null)}>Close</Button></DialogActions>
            </>
          )}
        </Dialog>
      </TabPanel>

      {/* ── 5. Next Best Action ───────────────────────────────── */}
      <TabPanel value={tab} index={4}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h6">Next Best Actions</Typography>
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>Category</InputLabel>
            <Select value={actionFilter} label="Category" onChange={(e: SelectChangeEvent) => setActionFilter(e.target.value)}>
              <MenuItem value="ALL">All Categories</MenuItem>
              <MenuItem value="CALL">Call</MenuItem>
              <MenuItem value="EMAIL">Email</MenuItem>
              <MenuItem value="MEETING">Meeting</MenuItem>
              <MenuItem value="PROPOSAL">Proposal</MenuItem>
              <MenuItem value="RETENTION">Retention</MenuItem>
              <MenuItem value="UPSELL">Upsell</MenuItem>
              <MenuItem value="FOLLOW_UP">Follow Up</MenuItem>
            </Select>
          </FormControl>
        </Stack>
        <Stack spacing={2}>
          {filteredActions.sort((a, b) => b.priority - a.priority).map(a => (
            <Card key={a.id} variant="outlined" sx={{ opacity: a.completed ? 0.6 : 1 }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Stack direction="row" spacing={1.5} alignItems="center">
                    <Avatar sx={{ bgcolor: a.completed ? 'grey.400' : 'primary.main', width: 36, height: 36 }}>
                      {actionCategoryIcon(a.category)}
                    </Avatar>
                    <Box>
                      <Typography variant="subtitle1" fontWeight={600} sx={{ textDecoration: a.completed ? 'line-through' : 'none' }}>{a.action}</Typography>
                      <Stack direction="row" spacing={1} mt={0.5}>
                        <Chip label={a.entityType} size="small" variant="outlined" />
                        <Chip label={a.entityName} size="small" />
                        <Chip label={a.category.replace(/_/g, ' ')} size="small" color="primary" variant="outlined" />
                        <Chip label={`Priority: ${a.priority}`} size="small" color={a.priority >= 90 ? 'error' : a.priority >= 70 ? 'warning' : 'info'} />
                      </Stack>
                    </Box>
                  </Stack>
                  {!a.completed && (
                    <Button variant="contained" size="small" startIcon={<DoneIcon />} onClick={() => completeAction(a.id)}>Done</Button>
                  )}
                  {a.completed && <Chip icon={<CheckCircle />} label="Completed" color="success" size="small" />}
                </Stack>
                <Typography variant="body2" color="text.secondary" mt={1}><strong>Why:</strong> {a.reason}</Typography>
                <Typography variant="body2" color="text.secondary"><strong>Expected Impact:</strong> {a.expectedImpact}</Typography>
                {a.dueDate && <Typography variant="caption" color="text.secondary">Due: {new Date(a.dueDate).toLocaleDateString()}</Typography>}
              </CardContent>
            </Card>
          ))}
        </Stack>
      </TabPanel>

      {/* ── 6. AI-powered Reporting ──────────────────────────── */}
      <TabPanel value={tab} index={5}>
        <Typography variant="h6" mb={2}>AI-Powered Report Insights</Typography>
        <Grid container spacing={2}>
          {reportInsights.map(r => (
            <Grid item xs={12} md={6} key={r.id}>
              <Card variant="outlined">
                <CardContent>
                  <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                    {insightIcon(r.insightType)}
                    <Typography variant="subtitle1" fontWeight={600}>{r.title}</Typography>
                  </Stack>
                  <Chip label={r.reportName} size="small" variant="outlined" sx={{ mb: 1 }} />
                  <Typography variant="body2" color="text.secondary" paragraph>{r.description}</Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={4}>
                      <Typography variant="caption" color="text.secondary">Metric</Typography>
                      <Typography variant="body2" fontWeight={600}>{r.metric}</Typography>
                    </Grid>
                    <Grid item xs={4}>
                      <Typography variant="caption" color="text.secondary">Current</Typography>
                      <Typography variant="body2" fontWeight={600}>{typeof r.currentValue === 'number' && r.currentValue > 9999 ? usd(r.currentValue) : r.currentValue}</Typography>
                    </Grid>
                    <Grid item xs={4}>
                      <Typography variant="caption" color="text.secondary">Change</Typography>
                      <Chip label={`${r.changePct > 0 ? '+' : ''}${r.changePct}%`} size="small" color={r.changePct > 0 ? 'success' : 'error'} />
                    </Grid>
                  </Grid>
                  <Alert severity="info" icon={<IdeaIcon />} sx={{ mt: 2 }}>{r.recommendation}</Alert>
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>Generated {ago(r.generatedAt)}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      {/* ── 7. Data Entry Suggestions ─────────────────────────── */}
      <TabPanel value={tab} index={6}>
        <Typography variant="h6" mb={2}>Automated Data Entry Suggestions</Typography>
        {suggestions.length === 0 ? (
          <Alert severity="success">No pending suggestions — all data looks complete!</Alert>
        ) : (
          <Paper variant="outlined">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Entity</TableCell>
                  <TableCell>Field</TableCell>
                  <TableCell>Current Value</TableCell>
                  <TableCell>Suggested Value</TableCell>
                  <TableCell>Confidence</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {suggestions.map(s => (
                  <TableRow key={s.id} hover>
                    <TableCell>
                      <Stack>
                        <Typography variant="body2" fontWeight={600}>{s.entityName}</Typography>
                        <Typography variant="caption" color="text.secondary">{s.entityType}</Typography>
                      </Stack>
                    </TableCell>
                    <TableCell><Chip label={s.field} size="small" variant="outlined" /></TableCell>
                    <TableCell><Typography variant="body2" color="text.secondary">{s.currentValue || '—'}</Typography></TableCell>
                    <TableCell><Typography variant="body2" fontWeight={600}>{s.suggestedValue}</Typography></TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={0.5} alignItems="center">
                        <LinearProgress variant="determinate" value={s.confidence * 100} sx={{ width: 60, height: 6, borderRadius: 3 }} color={s.confidence >= 0.85 ? 'success' : s.confidence >= 0.7 ? 'warning' : 'error'} />
                        <Typography variant="caption">{pct(s.confidence)}</Typography>
                      </Stack>
                    </TableCell>
                    <TableCell><Typography variant="caption" color="text.secondary">{s.source}</Typography></TableCell>
                    <TableCell align="right">
                      <Tooltip title="Accept"><IconButton size="small" color="success" onClick={() => acceptSuggestion(s.id)}><CheckCircle /></IconButton></Tooltip>
                      <Tooltip title="Dismiss"><IconButton size="small" color="error" onClick={() => dismissSuggestion(s.id)}><Cancel /></IconButton></Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        )}
      </TabPanel>

      {/* ── 8. AI Sales Insights ──────────────────────────────── */}
      <TabPanel value={tab} index={7}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h6">AI Sales Insights</Typography>
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>Type</InputLabel>
            <Select value={insightFilter} label="Type" onChange={(e: SelectChangeEvent) => setInsightFilter(e.target.value)}>
              <MenuItem value="ALL">All Types</MenuItem>
              <MenuItem value="TREND">Trends</MenuItem>
              <MenuItem value="ANOMALY">Anomalies</MenuItem>
              <MenuItem value="PREDICTION">Predictions</MenuItem>
              <MenuItem value="RECOMMENDATION">Recommendations</MenuItem>
              <MenuItem value="ALERT">Alerts</MenuItem>
            </Select>
          </FormControl>
        </Stack>
        <Stack spacing={2}>
          {filteredInsights.map(ins => (
            <Card key={ins.id} variant="outlined" sx={{ borderLeft: 4, borderColor: ins.severity === 'high' ? 'error.main' : ins.severity === 'medium' ? 'warning.main' : 'info.main', cursor: 'pointer' }} onClick={() => setSelectedInsight(ins)}>
              <CardContent>
                <Stack direction="row" spacing={1.5} alignItems="center" mb={1}>
                  {insightIcon(ins.insightType)}
                  <Typography variant="subtitle1" fontWeight={600}>{ins.title}</Typography>
                  <Chip label={ins.insightType} size="small" color={severityColor(ins.severity)} variant="outlined" />
                  <Chip label={ins.severity} size="small" color={severityColor(ins.severity)} />
                </Stack>
                <Typography variant="body2" color="text.secondary">{ins.summary}</Typography>
                <Stack direction="row" spacing={1} mt={1.5}>
                  <Chip label={ins.impactArea} size="small" variant="outlined" />
                  {ins.actionable && <Chip label="Actionable" size="small" color="primary" />}
                  {ins.relatedEntities?.map((e, i) => <Chip key={i} label={e.name} size="small" variant="outlined" />)}
                </Stack>
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>Generated {ago(ins.generatedAt)}</Typography>
              </CardContent>
            </Card>
          ))}
        </Stack>

        {/* Insight Detail */}
        <Dialog open={!!selectedInsight} onClose={() => setSelectedInsight(null)} maxWidth="sm" fullWidth>
          {selectedInsight && (
            <>
              <DialogTitle>
                <Stack direction="row" spacing={1} alignItems="center">
                  {insightIcon(selectedInsight.insightType)}
                  <span>{selectedInsight.title}</span>
                </Stack>
              </DialogTitle>
              <DialogContent dividers>
                <Chip label={selectedInsight.insightType} size="small" sx={{ mb: 2 }} color={severityColor(selectedInsight.severity)} />
                <Typography variant="body1" paragraph>{selectedInsight.details}</Typography>
                <Typography variant="body2" color="text.secondary"><strong>Impact Area:</strong> {selectedInsight.impactArea}</Typography>
                {selectedInsight.relatedEntities && selectedInsight.relatedEntities.length > 0 && (
                  <>
                    <Divider sx={{ my: 2 }} />
                    <Typography variant="subtitle2" gutterBottom>Related Entities</Typography>
                    {selectedInsight.relatedEntities.map((e, i) => (
                      <Chip key={i} label={`${e.type}: ${e.name}`} size="small" sx={{ mr: 0.5, mb: 0.5 }} />
                    ))}
                  </>
                )}
              </DialogContent>
              <DialogActions><Button onClick={() => setSelectedInsight(null)}>Close</Button></DialogActions>
            </>
          )}
        </Dialog>
      </TabPanel>

      {/* ── 9. AI Email Reply Generation ──────────────────────── */}
      <TabPanel value={tab} index={8}>
        <Typography variant="h6" mb={2}>AI Email Reply Generation</Typography>

        {/* Interactive generator */}
        <Card variant="outlined" sx={{ mb: 3 }}>
          <CardContent>
            <Stack direction="row" spacing={1} alignItems="center" mb={2}>
              <AiIcon color="primary" />
              <Typography variant="subtitle1" fontWeight={600}>Generate AI Email Draft</Typography>
            </Stack>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth size="small" label="Recipient" value={emailInput.to}
                  onChange={e => setEmailInput(p => ({ ...p, to: e.target.value }))} />
              </Grid>
              <Grid item xs={12} sm={5}>
                <TextField fullWidth size="small" label="Subject / Context" value={emailInput.subjectContext}
                  onChange={e => setEmailInput(p => ({ ...p, subjectContext: e.target.value }))} />
              </Grid>
              <Grid item xs={12} sm={3}>
                <FormControl fullWidth size="small">
                  <InputLabel>Tone</InputLabel>
                  <Select value={emailInput.tone} label="Tone" onChange={(e: SelectChangeEvent) => setEmailInput(p => ({ ...p, tone: e.target.value }))}>
                    <MenuItem value="professional">Professional</MenuItem>
                    <MenuItem value="friendly">Friendly</MenuItem>
                    <MenuItem value="formal">Formal</MenuItem>
                    <MenuItem value="empathetic">Empathetic</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
            <Button variant="contained" startIcon={<AiIcon />} sx={{ mt: 2 }}
              disabled={emailGenLoading || !emailInput.to || !emailInput.subjectContext}
              onClick={async () => {
                setEmailGenLoading(true); setEmailResult(null);
                try {
                  const r = await aiInsightsService.generateEmailDraft(emailInput.to, emailInput.subjectContext, emailInput.tone);
                  setEmailResult(r);
                  setSnack({ open: true, msg: 'Email draft generated!', severity: 'success' });
                } catch { setSnack({ open: true, msg: 'Failed to generate draft', severity: 'error' }); }
                finally { setEmailGenLoading(false); }
              }}>
              {emailGenLoading ? 'Generating…' : 'Generate Draft'}
            </Button>
            {emailResult && (
              <Paper variant="outlined" sx={{ p: 2, mt: 2, bgcolor: 'grey.50' }}>
                <Typography variant="subtitle2" gutterBottom>Subject: {emailResult.subject}</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{emailResult.body}</Typography>
                {emailResult.suggestions.length > 0 && (
                  <Box mt={1}>
                    <Typography variant="caption" fontWeight={600}>Suggestions:</Typography>
                    {emailResult.suggestions.map((s, i) => (
                      <Typography key={i} variant="caption" display="block" color="text.secondary">• {s}</Typography>
                    ))}
                  </Box>
                )}
              </Paper>
            )}
          </CardContent>
        </Card>

        {emailReplies.length === 0 && !emailResult ? (
          <Alert severity="info">Use the form above to generate an AI-powered email draft, or use the AI Draft button in the email compose dialog.</Alert>
        ) : (
          <Stack spacing={2}>
            {emailReplies.map(er => (
              <Card key={er.id} variant="outlined">
                <CardContent>
                  <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                    <ReplyIcon color="primary" />
                    <Typography variant="subtitle1" fontWeight={600}>{er.replySubject}</Typography>
                    <Chip label={er.tone} size="small" variant="outlined" />
                  </Stack>
                  <Typography variant="body2" color="text.secondary" mb={1}>
                    <strong>Original from:</strong> {er.originalFrom} · <strong>Subject:</strong> {er.originalSubject}
                  </Typography>
                  <Paper variant="outlined" sx={{ p: 2, bgcolor: 'grey.50', whiteSpace: 'pre-wrap', mb: 2 }}>
                    <Typography variant="body2">{er.replyBody}</Typography>
                  </Paper>
                  {er.suggestions && er.suggestions.length > 0 && (
                    <>
                      <Typography variant="subtitle2" gutterBottom>Improvement Suggestions</Typography>
                      {er.suggestions.map((s, i) => (
                        <Stack key={i} direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                          <IdeaIcon fontSize="small" color="warning" />
                          <Typography variant="body2">{s}</Typography>
                        </Stack>
                      ))}
                    </>
                  )}
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>Generated {ago(er.createdAt)}</Typography>
                </CardContent>
              </Card>
            ))}
          </Stack>
        )}
      </TabPanel>

      {/* ── 10. AI Meeting Summary & CRM Update ──────────────── */}
      <TabPanel value={tab} index={9}>
        <Typography variant="h6" mb={2}>AI Meeting Summary &amp; CRM Update</Typography>

        {/* Interactive generator */}
        <Card variant="outlined" sx={{ mb: 3 }}>
          <CardContent>
            <Stack direction="row" spacing={1} alignItems="center" mb={2}>
              <SummarizeIcon color="primary" />
              <Typography variant="subtitle1" fontWeight={600}>Generate Meeting Summary from Notes</Typography>
            </Stack>
            <Stack spacing={2}>
              <TextField fullWidth size="small" label="Meeting Title" value={meetingInput.title}
                onChange={e => setMeetingInput(p => ({ ...p, title: e.target.value }))} />
              <TextField fullWidth multiline rows={4} label="Meeting Notes / Transcript"
                placeholder="Paste meeting notes, transcript, or key discussion points…"
                value={meetingInput.notes}
                onChange={e => setMeetingInput(p => ({ ...p, notes: e.target.value }))} />
            </Stack>
            <Button variant="contained" startIcon={<SummarizeIcon />} sx={{ mt: 2 }}
              disabled={meetingGenLoading || !meetingInput.title || !meetingInput.notes}
              onClick={async () => {
                setMeetingGenLoading(true); setMeetingResult(null);
                try {
                  const r = await aiInsightsService.generateMeetingSummary(meetingInput.title, meetingInput.notes);
                  setMeetingResult(r);
                  setSnack({ open: true, msg: 'Meeting summary generated!', severity: 'success' });
                } catch { setSnack({ open: true, msg: 'Failed to generate summary', severity: 'error' }); }
                finally { setMeetingGenLoading(false); }
              }}>
              {meetingGenLoading ? 'Summarizing…' : 'Generate Summary'}
            </Button>
            {meetingResult && (
              <Paper variant="outlined" sx={{ p: 2, mt: 2, bgcolor: 'grey.50' }}>
                <Typography variant="subtitle2" gutterBottom>Summary</Typography>
                <Typography variant="body2" sx={{ mb: 1 }}>{meetingResult.summary}</Typography>
                {meetingResult.actionItems.length > 0 && (
                  <Box mb={1}>
                    <Typography variant="caption" fontWeight={600}>Action Items:</Typography>
                    {meetingResult.actionItems.map((item, i) => (
                      <Typography key={i} variant="body2" display="block">  {i + 1}. {item}</Typography>
                    ))}
                  </Box>
                )}
                {meetingResult.keyDecisions.length > 0 && (
                  <Box>
                    <Typography variant="caption" fontWeight={600}>Key Decisions:</Typography>
                    {meetingResult.keyDecisions.map((d, i) => (
                      <Typography key={i} variant="body2" display="block">  • {d}</Typography>
                    ))}
                  </Box>
                )}
              </Paper>
            )}
          </CardContent>
        </Card>

        {meetingSummaries.length === 0 && !meetingResult ? (
          <Alert severity="info">Use the form above to generate a meeting summary, or use the AI Summarize button in the Activity dialog.</Alert>
        ) : (
          <Stack spacing={2}>
            {meetingSummaries.map(ms => (
              <Card key={ms.id} variant="outlined">
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <SummarizeIcon color="primary" />
                      <Typography variant="subtitle1" fontWeight={600}>{ms.meetingTitle}</Typography>
                    </Stack>
                    {ms.meetingDate && (
                      <Chip label={new Date(ms.meetingDate).toLocaleDateString()} size="small" variant="outlined" icon={<MeetingIcon />} />
                    )}
                  </Stack>
                  {ms.participants && ms.participants.length > 0 && (
                    <Stack direction="row" spacing={0.5} flexWrap="wrap" mb={1}>
                      {ms.participants.map((p, i) => <Chip key={i} label={p} size="small" variant="outlined" />)}
                    </Stack>
                  )}
                  <Paper variant="outlined" sx={{ p: 2, bgcolor: 'grey.50', mb: 2 }}>
                    <Typography variant="body2">{ms.summary}</Typography>
                  </Paper>
                  <Grid container spacing={2}>
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" color="primary" gutterBottom>Action Items</Typography>
                      {(ms.actionItems ?? []).map((item, i) => (
                        <Stack key={i} direction="row" spacing={1} sx={{ mb: 0.5 }}>
                          <CheckCircle fontSize="small" color="action" />
                          <Typography variant="body2">{item}</Typography>
                        </Stack>
                      ))}
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <Typography variant="subtitle2" color="secondary" gutterBottom>Key Decisions</Typography>
                      {(ms.keyDecisions ?? []).map((d, i) => (
                        <Stack key={i} direction="row" spacing={1} sx={{ mb: 0.5 }}>
                          <DoneIcon fontSize="small" color="success" />
                          <Typography variant="body2">{d}</Typography>
                        </Stack>
                      ))}
                    </Grid>
                  </Grid>
                  {ms.crmUpdates && ms.crmUpdates.length > 0 && (
                    <Box mt={2}>
                      <Typography variant="subtitle2" color="warning.main" gutterBottom>Suggested CRM Updates</Typography>
                      <Paper variant="outlined">
                        <Table size="small">
                          <TableHead>
                            <TableRow>
                              <TableCell>Entity</TableCell>
                              <TableCell>Field</TableCell>
                              <TableCell>Value</TableCell>
                              <TableCell>Reason</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {ms.crmUpdates.map((u, i) => (
                              <TableRow key={i}>
                                <TableCell><Chip label={u.entityType} size="small" /></TableCell>
                                <TableCell>{u.field}</TableCell>
                                <TableCell><Typography variant="body2" fontWeight={600}>{u.suggestedValue}</Typography></TableCell>
                                <TableCell><Typography variant="caption">{u.reason}</Typography></TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </Paper>
                    </Box>
                  )}
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>Generated {ago(ms.createdAt)}</Typography>
                </CardContent>
              </Card>
            ))}
          </Stack>
        )}
      </TabPanel>

      {/* ── 11. Auto-Create Leads from Emails/Meetings ────────── */}
      <TabPanel value={tab} index={10}>
        <Typography variant="h6" mb={2}>Auto-Created Leads from Emails &amp; Meetings</Typography>
        {autoLeads.length === 0 ? (
          <Alert severity="info">No auto-extracted leads yet. AI will detect potential leads from incoming emails and meeting notes.</Alert>
        ) : (
          <Paper variant="outlined">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Lead Name</TableCell>
                  <TableCell>Company</TableCell>
                  <TableCell>Email / Phone</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell>Confidence</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {autoLeads.map(al => (
                  <TableRow key={al.id} hover sx={{ opacity: al.status === 'REJECTED' ? 0.5 : 1 }}>
                    <TableCell>
                      <Stack>
                        <Typography variant="body2" fontWeight={600}>{al.leadName}</Typography>
                        {al.title && <Typography variant="caption" color="text.secondary">{al.title}</Typography>}
                      </Stack>
                    </TableCell>
                    <TableCell>{al.company || '—'}</TableCell>
                    <TableCell>
                      <Typography variant="body2">{al.email || '—'}</Typography>
                      {al.phone && <Typography variant="caption" color="text.secondary">{al.phone}</Typography>}
                    </TableCell>
                    <TableCell>
                      <Chip label={al.sourceType} size="small" color={al.sourceType === 'EMAIL' ? 'primary' : 'secondary'} variant="outlined" />
                      {al.sourceReference && (
                        <Typography variant="caption" color="text.secondary" display="block" sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {al.sourceReference}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={0.5} alignItems="center">
                        <LinearProgress variant="determinate" value={al.confidence * 100} sx={{ width: 60, height: 6, borderRadius: 3 }} color={al.confidence >= 0.8 ? 'success' : al.confidence >= 0.6 ? 'warning' : 'error'} />
                        <Typography variant="caption">{pct(al.confidence)}</Typography>
                      </Stack>
                    </TableCell>
                    <TableCell>
                      <Chip label={al.status} size="small"
                        color={al.status === 'APPROVED' ? 'success' : al.status === 'REJECTED' ? 'error' : al.status === 'CREATED' ? 'info' : 'warning'} />
                    </TableCell>
                    <TableCell align="right">
                      {al.status === 'PENDING' && (
                        <>
                          <Tooltip title="Approve & Create Lead">
                            <IconButton size="small" color="success" onClick={() => {
                              const updated = autoLeads.map(a => a.id === al.id ? { ...a, status: 'APPROVED' as const } : a);
                              setAutoLeads(updated);
                              aiInsightsService.saveAutoLeads(updated);
                              notify('Lead approved');
                            }}><CheckCircle /></IconButton>
                          </Tooltip>
                          <Tooltip title="Reject">
                            <IconButton size="small" color="error" onClick={() => {
                              const updated = autoLeads.map(a => a.id === al.id ? { ...a, status: 'REJECTED' as const } : a);
                              setAutoLeads(updated);
                              aiInsightsService.saveAutoLeads(updated);
                              notify('Lead rejected', 'info');
                            }}><Cancel /></IconButton>
                          </Tooltip>
                        </>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        )}
        {autoLeads.filter(a => a.notes).length > 0 && (
          <Box mt={2}>
            <Typography variant="subtitle2" gutterBottom>Lead Notes</Typography>
            <Grid container spacing={1}>
              {autoLeads.filter(a => a.notes).map(al => (
                <Grid item xs={12} md={6} key={al.id}>
                  <Alert severity="info" variant="outlined" sx={{ '.MuiAlert-message': { width: '100%' } }}>
                    <Typography variant="body2" fontWeight={600}>{al.leadName}</Typography>
                    <Typography variant="caption">{al.notes}</Typography>
                  </Alert>
                </Grid>
              ))}
            </Grid>
          </Box>
        )}
      </TabPanel>

      {/* ── Snackbar ──────────────────────────────────────────── */}
      <Snackbar open={snack.open} autoHideDuration={3000} onClose={() => setSnack(s => ({ ...s, open: false }))}>
        <Alert severity={snack.severity} onClose={() => setSnack(s => ({ ...s, open: false }))}>{snack.msg}</Alert>
      </Snackbar>
    </Box>
  );
};

export default AiInsightsPage;
