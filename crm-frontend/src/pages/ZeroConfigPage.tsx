/* ============================================================
   ZeroConfigPage.tsx – Zero-Configuration CRM (8 Features)
   Covers: NL Config, Auto-Pipelines, CSV Field Detection,
   AI Workflows, Dedup, Enrichment, Smart Dashboards, Onboarding
   ============================================================ */
import React, { useEffect, useState, useCallback } from 'react';
import {
  Box, Typography, Paper, Grid, Tabs, Tab, Card, CardContent,
  CardActions, Button, LinearProgress, Chip, Alert, Table,
  TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, Stepper, Step, StepLabel, StepContent, IconButton,
  Tooltip, Snackbar, CircularProgress, List, ListItem, ListItemIcon,
  ListItemText, Divider, Stack,
} from '@mui/material';
import {
  CheckCircle as CheckIcon,
  RadioButtonUnchecked as UncheckedIcon,
  AutoFixHigh as AiIcon,
  UploadFile as UploadIcon,
  ContactPhone as ContactIcon,
  School as OnboardIcon,
  ArrowForward as ArrowIcon,
  Lightbulb as HintIcon,
  Refresh as RefreshIcon,
  ContentPaste as PasteIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import type {
  CsvFieldDetectionResult, CsvFieldMapping,
  ContactEnrichmentResult,
  OnboardingStatus, OnboardingStep,
  IndustryFieldInfo,
} from '../types';
import {
  detectCsvFields, enrichContact,
  getOnboardingStatus, completeOnboardingStep, resetOnboarding,
  getOnboardingGuidance, getSupportedIndustries,
  DEFAULT_ONBOARDING_STATUS, DEFAULT_CSV_DETECTION, DEFAULT_ENRICHMENT,
} from '../services/zeroConfigService';
import { PageHeader } from '../components';

/* ── Tab panel helper ─────────────────────────────────────── */
function TabPanel({ children, value, index }: { children: React.ReactNode; value: number; index: number }) {
  return value === index ? <Box sx={{ py: 3 }}>{children}</Box> : null;
}

const ZeroConfigPage: React.FC = () => {
  const navigate = useNavigate();
  const [tab, setTab] = useState(0);
  const [snack, setSnack] = useState('');

  /* ---- Onboarding state ---- */
  const [onboarding, setOnboarding] = useState<OnboardingStatus>(DEFAULT_ONBOARDING_STATUS);
  const [loadingOnboarding, setLoadingOnboarding] = useState(false);
  const [guidanceText, setGuidanceText] = useState<Record<string, string>>({});
  const [guidanceLoading, setGuidanceLoading] = useState<string | null>(null);

  /* ---- CSV Detection state ---- */
  const [csvText, setCsvText] = useState('');
  const [csvEntityType, setCsvEntityType] = useState('account');
  const [csvIndustry, setCsvIndustry] = useState('');
  const [csvIndustries, setCsvIndustries] = useState<string[]>([]);
  const [csvResult, setCsvResult] = useState<CsvFieldDetectionResult | null>(null);
  const [csvLoading, setCsvLoading] = useState(false);

  /* ---- Enrichment state ---- */
  const [enrichName, setEnrichName] = useState('');
  const [enrichEmail, setEnrichEmail] = useState('');
  const [enrichCompany, setEnrichCompany] = useState('');
  const [enrichTitle, setEnrichTitle] = useState('');
  const [enrichResult, setEnrichResult] = useState<ContactEnrichmentResult | null>(null);
  const [enrichLoading, setEnrichLoading] = useState(false);

  /* ---- Load onboarding status ---- */
  useEffect(() => {
    (async () => {
      try {
        const status = await getOnboardingStatus();
        if (status && Array.isArray(status.steps)) setOnboarding(status);
      } catch {
        /* keep defaults */
      }
      try {
        const ind = await getSupportedIndustries();
        if (Array.isArray(ind)) setCsvIndustries(ind);
      } catch {
        setCsvIndustries(['real_estate', 'healthcare', 'technology', 'finance', 'education', 'manufacturing']);
      }
    })();
  }, []);

  /* ---- Handlers ---- */

  const handleCompleteStep = useCallback(async (stepId: string) => {
    setLoadingOnboarding(true);
    try {
      const updated = await completeOnboardingStep(stepId);
      if (updated && Array.isArray(updated.steps)) setOnboarding(updated);
      setSnack('Step completed!');
    } catch {
      // optimistic update locally
      setOnboarding(prev => ({
        ...prev,
        steps: prev.steps.map(s => s.id === stepId ? { ...s, completed: true } : s),
        completedSteps: prev.completedSteps + 1,
        progressPercent: Math.round(((prev.completedSteps + 1) / prev.totalSteps) * 100),
      }));
      setSnack('Step marked complete (offline)');
    }
    setLoadingOnboarding(false);
  }, []);

  const handleResetOnboarding = useCallback(async () => {
    try {
      const updated = await resetOnboarding();
      if (updated && Array.isArray(updated.steps)) setOnboarding(updated);
      else setOnboarding(DEFAULT_ONBOARDING_STATUS);
      setSnack('Onboarding reset');
    } catch {
      setOnboarding(DEFAULT_ONBOARDING_STATUS);
      setSnack('Onboarding reset (offline)');
    }
  }, []);

  const handleGetGuidance = useCallback(async (stepId: string) => {
    setGuidanceLoading(stepId);
    try {
      const text = await getOnboardingGuidance(stepId);
      setGuidanceText(prev => ({ ...prev, [stepId]: text }));
    } catch {
      const step = onboarding.steps.find(s => s.id === stepId);
      setGuidanceText(prev => ({ ...prev, [stepId]: step?.aiHint || 'Guidance unavailable' }));
    }
    setGuidanceLoading(null);
  }, [onboarding.steps]);

  const handleDetectCsv = useCallback(async () => {
    if (!csvText.trim()) { setSnack('Paste CSV content first'); return; }
    setCsvLoading(true);
    try {
      const result = await detectCsvFields(csvText, csvEntityType, csvIndustry || undefined);
      if (result && Array.isArray(result.fieldMappings)) setCsvResult(result);
      else setCsvResult(DEFAULT_CSV_DETECTION);
    } catch {
      setCsvResult(DEFAULT_CSV_DETECTION);
      setSnack('Using sample detection (API unavailable)');
    }
    setCsvLoading(false);
  }, [csvText, csvEntityType, csvIndustry]);

  const handleEnrich = useCallback(async () => {
    if (!enrichName.trim() && !enrichEmail.trim()) { setSnack('Enter at least a name or email'); return; }
    setEnrichLoading(true);
    try {
      const result = await enrichContact({
        contactId: 'new-' + Date.now(),
        name: enrichName || undefined,
        email: enrichEmail || undefined,
        company: enrichCompany || undefined,
        title: enrichTitle || undefined,
      });
      if (result && Array.isArray(result.enrichedFields)) setEnrichResult(result);
      else setEnrichResult(DEFAULT_ENRICHMENT);
    } catch {
      setEnrichResult(DEFAULT_ENRICHMENT);
      setSnack('Using sample enrichment (API unavailable)');
    }
    setEnrichLoading(false);
  }, [enrichName, enrichEmail, enrichCompany, enrichTitle]);

  const confidenceColor = (c: number) => c >= 0.85 ? 'success' : c >= 0.6 ? 'warning' : 'error';

  /* ============================================================
     RENDER
     ============================================================ */
  return (
    <Box sx={{ p: { xs: 2, md: 3 }, maxWidth: 1400, mx: 'auto' }}>
      {/* Header */}
      <PageHeader
        title="Zero-Configuration CRM"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Zero Config' }]}
      />
      <Typography color="text.secondary" sx={{ mb: 3 }}>
          8 features that eliminate setup complexity — get started in minutes, not months.
      </Typography>

      {/* Progress banner */}
      <Paper sx={{ p: 2, mb: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
        <OnboardIcon color="primary" />
        <Box sx={{ flex: 1 }}>
          <Typography variant="body2" fontWeight={600}>
            Onboarding Progress: {onboarding.completedSteps}/{onboarding.totalSteps} steps
          </Typography>
          <LinearProgress
            variant="determinate"
            value={onboarding.progressPercent}
            sx={{ mt: 0.5, height: 8, borderRadius: 4 }}
          />
        </Box>
        <Typography variant="h6" color="primary.main" fontWeight={700}>
          {onboarding.progressPercent}%
        </Typography>
      </Paper>

      {/* Tabs */}
      <Tabs
        value={tab}
        onChange={(_, v) => setTab(v)}
        variant="scrollable"
        scrollButtons="auto"
        sx={{ mb: 1, borderBottom: 1, borderColor: 'divider' }}
      >
        <Tab icon={<OnboardIcon />} label="Onboarding" iconPosition="start" />
        <Tab icon={<UploadIcon />} label="CSV Detection" iconPosition="start" />
        <Tab icon={<ContactIcon />} label="Enrichment" iconPosition="start" />
        <Tab icon={<AiIcon />} label="All Features" iconPosition="start" />
      </Tabs>

      {/* ──────── TAB 0: AI Onboarding Assistant ──────── */}
      <TabPanel value={tab} index={0}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">AI Onboarding Checklist</Typography>
          <Button size="small" startIcon={<RefreshIcon />} onClick={handleResetOnboarding}>
            Reset
          </Button>
        </Box>

        {loadingOnboarding && <LinearProgress sx={{ mb: 2 }} />}

        <Alert severity="info" sx={{ mb: 3 }}>
          <strong>Next recommendation:</strong> {onboarding.nextRecommendation}
        </Alert>

        <Stepper orientation="vertical" activeStep={onboarding.steps.findIndex(s => !s.completed)}>
          {onboarding.steps.map((step) => (
            <Step key={step.id} completed={step.completed}>
              <StepLabel
                StepIconComponent={() =>
                  step.completed
                    ? <CheckIcon color="success" />
                    : <UncheckedIcon color="disabled" />
                }
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography fontWeight={step.completed ? 400 : 600}>{step.title}</Typography>
                  <Chip label={step.category} size="small" variant="outlined" />
                </Box>
              </StepLabel>
              <StepContent>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  {step.description}
                </Typography>

                {/* AI Hint */}
                <Alert severity="info" icon={<HintIcon />} sx={{ mb: 1.5 }}>
                  {guidanceText[step.id] || step.aiHint}
                </Alert>

                <Stack direction="row" spacing={1}>
                  {!step.completed && (
                    <Button
                      variant="contained"
                      size="small"
                      onClick={() => navigate(step.actionUrl)}
                      endIcon={<ArrowIcon />}
                    >
                      Go to {step.title.split(' ').slice(-1)[0]}
                    </Button>
                  )}
                  {!step.completed && (
                    <Button
                      variant="outlined"
                      size="small"
                      onClick={() => handleCompleteStep(step.id)}
                    >
                      Mark Complete
                    </Button>
                  )}
                  <Button
                    size="small"
                    startIcon={guidanceLoading === step.id ? <CircularProgress size={16} /> : <AiIcon />}
                    onClick={() => handleGetGuidance(step.id)}
                    disabled={guidanceLoading === step.id}
                  >
                    AI Guide
                  </Button>
                </Stack>
              </StepContent>
            </Step>
          ))}
        </Stepper>
      </TabPanel>

      {/* ──────── TAB 1: CSV Field Auto-Detection ──────── */}
      <TabPanel value={tab} index={1}>
        <Typography variant="h6" gutterBottom>
          AI CSV Field Detection
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Paste your CSV data and let AI automatically map columns to CRM fields — no manual configuration needed.
        </Typography>

        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={12} md={9}>
            <TextField
              fullWidth
              multiline
              rows={6}
              label="Paste CSV content here"
              placeholder={"Company Name,Industry,Website URL,Phone Number,Annual Rev,# Employees\nAcme Corp,Technology,https://acme.com,+1-555-0123,$5000000,250"}
              value={csvText}
              onChange={(e) => setCsvText(e.target.value)}
              variant="outlined"
            />
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField
              select
              fullWidth
              label="Entity Type"
              value={csvEntityType}
              onChange={(e) => setCsvEntityType(e.target.value)}
              SelectProps={{ native: true }}
              sx={{ mb: 2 }}
            >
              <option value="account">Account</option>
              <option value="contact">Contact</option>
              <option value="lead">Lead</option>
            </TextField>
            <TextField
              select
              fullWidth
              label="Industry (optional)"
              value={csvIndustry}
              onChange={(e) => setCsvIndustry(e.target.value)}
              SelectProps={{ native: true }}
              sx={{ mb: 2 }}
              helperText="Shows industry-specific fields"
            >
              <option value="">None (default fields)</option>
              {csvIndustries.map(ind => (
                <option key={ind} value={ind}>
                  {ind.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())}
                </option>
              ))}
            </TextField>
            <Button
              fullWidth
              variant="contained"
              startIcon={csvLoading ? <CircularProgress size={20} color="inherit" /> : <AiIcon />}
              onClick={handleDetectCsv}
              disabled={csvLoading}
              sx={{ height: 48 }}
            >
              {csvLoading ? 'Detecting...' : 'Detect Fields'}
            </Button>
            <Button
              fullWidth
              variant="outlined"
              startIcon={<PasteIcon />}
              sx={{ mt: 1 }}
              onClick={() => {
                setCsvText('Company Name,Industry,Website URL,Phone Number,Annual Rev,# Employees,Custom Field 1,Internal Code\nAcme Corp,Technology,https://acme.com,+1-555-0123,$5000000,250,custom1,IC001');
                setSnack('Sample CSV loaded');
              }}
            >
              Load Sample
            </Button>
          </Grid>
        </Grid>

        {csvResult && (
          <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
              <Chip
                label={`${csvResult.mappedColumns}/${csvResult.totalColumns} columns mapped`}
                color="success"
              />
              {csvResult.unmappedColumns.length > 0 && (
                <Chip
                  label={`${csvResult.unmappedColumns.length} unmapped`}
                  color="warning"
                  variant="outlined"
                />
              )}
            </Box>

            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell><strong>CSV Header</strong></TableCell>
                    <TableCell><strong>CRM Field</strong></TableCell>
                    <TableCell><strong>Source</strong></TableCell>
                    <TableCell><strong>Data Type</strong></TableCell>
                    <TableCell><strong>Confidence</strong></TableCell>
                    <TableCell><strong>Sample</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {csvResult.fieldMappings.map((m: CsvFieldMapping, i: number) => (
                    <TableRow key={i}>
                      <TableCell>{m.csvHeader}</TableCell>
                      <TableCell>
                        <Chip label={m.crmField} size="small" color="primary" variant="outlined" />
                      </TableCell>
                      <TableCell>
                        {m.isIndustryField ? (
                          <Chip label="Industry" size="small" color="secondary" />
                        ) : m.isCustomField ? (
                          <Chip label="Custom" size="small" color="info" />
                        ) : (
                          <Chip label="Standard" size="small" variant="outlined" />
                        )}
                      </TableCell>
                      <TableCell>{m.dataType}</TableCell>
                      <TableCell>
                        <Chip
                          label={`${Math.round(m.confidence * 100)}%`}
                          size="small"
                          color={confidenceColor(m.confidence) as any}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">{m.sampleValue}</Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            {csvResult.unmappedColumns.length > 0 && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                <strong>Unmapped columns:</strong> {csvResult.unmappedColumns.join(', ')}
                <br />
                These columns don&apos;t match known CRM fields. You can create custom fields for them.
              </Alert>
            )}

            {csvResult.industryFields && csvResult.industryFields.length > 0 && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Available Industry Fields ({csvResult.industry?.replace(/_/g, ' ').replace(/\b\w/g, (c: string) => c.toUpperCase())})
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {csvResult.industryFields.map((f: IndustryFieldInfo) => (
                    <Chip
                      key={f.fieldName}
                      label={f.label}
                      size="small"
                      color="secondary"
                      variant="outlined"
                    />
                  ))}
                </Box>
              </Box>
            )}
          </Paper>
        )}
      </TabPanel>

      {/* ──────── TAB 2: Contact Enrichment ──────── */}
      <TabPanel value={tab} index={2}>
        <Typography variant="h6" gutterBottom>
          AI Contact Enrichment
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Enter basic contact info and let AI enrich it with inferred data — industry, seniority, department, and more.
        </Typography>

        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={12} md={3}>
            <TextField fullWidth label="Name" value={enrichName} onChange={e => setEnrichName(e.target.value)} />
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField fullWidth label="Email" value={enrichEmail} onChange={e => setEnrichEmail(e.target.value)} />
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField fullWidth label="Company" value={enrichCompany} onChange={e => setEnrichCompany(e.target.value)} />
          </Grid>
          <Grid item xs={12} md={2}>
            <TextField fullWidth label="Title" value={enrichTitle} onChange={e => setEnrichTitle(e.target.value)} />
          </Grid>
          <Grid item xs={12} md={1}>
            <Button
              fullWidth
              variant="contained"
              onClick={handleEnrich}
              disabled={enrichLoading}
              sx={{ height: 56 }}
            >
              {enrichLoading ? <CircularProgress size={24} color="inherit" /> : <AiIcon />}
            </Button>
          </Grid>
        </Grid>

        {enrichResult && (
          <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
              <Typography variant="subtitle1" fontWeight={600}>Enrichment Results</Typography>
              <Chip
                label={`Overall confidence: ${Math.round(enrichResult.overallConfidence * 100)}%`}
                color={confidenceColor(enrichResult.overallConfidence) as any}
              />
              <Chip label={enrichResult.enrichmentSource} size="small" variant="outlined" />
            </Box>

            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Field</strong></TableCell>
                    <TableCell><strong>Current</strong></TableCell>
                    <TableCell><strong>Suggested Value</strong></TableCell>
                    <TableCell><strong>Confidence</strong></TableCell>
                    <TableCell><strong>Source</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {enrichResult.enrichedFields.map((f, i) => (
                    <TableRow key={i}>
                      <TableCell>
                        <Chip label={f.field} size="small" variant="outlined" />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {f.currentValue || '—'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight={600}>{f.suggestedValue}</Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={`${Math.round(f.confidence * 100)}%`}
                          size="small"
                          color={confidenceColor(f.confidence) as any}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary">{f.source}</Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
        )}
      </TabPanel>

      {/* ──────── TAB 3: All 8 Features Overview ──────── */}
      <TabPanel value={tab} index={3}>
        <Typography variant="h6" gutterBottom>
          All Zero-Configuration Features
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Your CRM comes pre-configured with 8 intelligent features that remove setup complexity.
        </Typography>

        <Grid container spacing={2}>
          {[
            {
              title: 'Natural Language Configuration',
              desc: 'Configure your CRM using plain English — create fields, objects, pipelines, and workflows by just describing what you need.',
              status: 'Active',
              link: '/ai-config',
            },
            {
              title: 'Auto-Create Pipelines',
              desc: 'Describe your sales process and the AI automatically creates pipeline stages with the right probabilities and settings.',
              status: 'Active',
              link: '/ai-config',
            },
            {
              title: 'CSV Field Auto-Detection',
              desc: 'Upload a CSV and AI intelligently maps columns to CRM fields using fuzzy matching — no manual mapping required.',
              status: 'Active',
              link: '',
              tabIndex: 1,
            },
            {
              title: 'AI Workflow Automation',
              desc: 'Describe automations in natural language and the AI creates triggers, conditions, and actions automatically.',
              status: 'Active',
              link: '/ai-config',
            },
            {
              title: 'Automatic Deduplication',
              desc: 'Contacts, leads, and accounts are automatically checked for duplicates by email, phone, and name with one-click merging.',
              status: 'Active',
              link: '/contacts',
            },
            {
              title: 'AI Contact Enrichment',
              desc: 'Enter basic info and AI enriches contacts with inferred industry, seniority, department, timezone, and more.',
              status: 'Active',
              link: '',
              tabIndex: 2,
            },
            {
              title: 'Smart Default Dashboards',
              desc: 'Pipeline analytics, revenue charts, forecasts, and conversion rates are pre-configured and ready from day one.',
              status: 'Active',
              link: '/dashboard',
            },
            {
              title: 'AI Onboarding Assistant',
              desc: '10-step guided onboarding with AI hints, contextual guidance, and progress tracking to get you up and running fast.',
              status: 'Active',
              link: '',
              tabIndex: 0,
            },
          ].map((feature, i) => (
            <Grid item xs={12} md={6} lg={3} key={i}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flex: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="subtitle2" fontWeight={700}>{feature.title}</Typography>
                    <Chip label={feature.status} size="small" color="success" />
                  </Box>
                  <Typography variant="body2" color="text.secondary">{feature.desc}</Typography>
                </CardContent>
                <CardActions>
                  <Button
                    size="small"
                    endIcon={<ArrowIcon />}
                    onClick={() => {
                      if (feature.link) navigate(feature.link);
                      else if (feature.tabIndex !== undefined) setTab(feature.tabIndex);
                    }}
                  >
                    Try it
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      <Snackbar
        open={!!snack}
        autoHideDuration={3000}
        onClose={() => setSnack('')}
        message={snack}
      />
    </Box>
  );
};

export default ZeroConfigPage;
