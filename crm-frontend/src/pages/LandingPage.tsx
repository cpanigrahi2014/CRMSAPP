/* ============================================================
   LandingPage – Public marketing page with app details,
   features, pricing plans, and login/register CTAs
   ============================================================ */
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  Grid,
  Chip,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Divider,
  AppBar,
  Toolbar,
  Container,
  IconButton,
  Stack,
  Avatar,
  useTheme,
  useMediaQuery,
  Drawer,
  Fab,
} from '@mui/material';
import {
  Hub as LogoIcon,
  Check as CheckIcon,
  Close as CloseIcon,
  Star as StarIcon,
  Rocket as RocketIcon,
  WorkspacePremium as PremiumIcon,
  Diamond as DiamondIcon,
  Psychology as AiIcon,
  Speed as SpeedIcon,
  Security as SecurityIcon,
  TrendingUp as GrowthIcon,
  AutoFixHigh as ConfigIcon,
  Dashboard as DashboardIcon,
  AccountTree as WorkflowIcon,
  People as PeopleIcon,
  Email as EmailIcon,
  BarChart as ReportsIcon,
  Extension as IntegrationIcon,
  DataObject as ObjectIcon,
  Menu as MenuIcon,
  KeyboardArrowUp as ScrollTopIcon,
  FormatQuote as QuoteIcon,
  SmartToy as SmartToyIcon,
  Bolt as BoltIcon,
  Chat as ChatIcon,
  Insights as InsightsIcon,
  Storage as StorageIcon,
  Timeline as TimelineIcon,
  PieChart as PieChartIcon,
  ArrowForward as ArrowIcon,
  PlayArrow as PlayIcon,
  Terminal as TerminalIcon,
  SupportAgent as CasesIcon,
  Campaign as CampaignIcon,
  Forum as CollabIcon,
  Notifications as NotifIcon,
  Task as TaskIcon,
  Code as DevIcon,
  UploadFile as ImportIcon,
  ViewKanban as KanbanIcon,
  HealthAndSafety as DataHealthIcon,
  Api as ApiIcon,
  Language as LanguageIcon,
  Groups as TeamsIcon,
} from '@mui/icons-material';
import { alpha } from '@mui/material/styles';

/* ─── Plan Definitions ──────────────────────────────────────────────────── */
const PLANS = [
  {
    name: 'FREE',
    price: '$0',
    priceDetail: 'forever',
    icon: <StarIcon fontSize="large" />,
    color: '#78909c',
    features: [
      { label: 'Up to 3 users', included: true },
      { label: '2 custom objects', included: true },
      { label: '3 workflows', included: true },
      { label: '1 dashboard', included: true },
      { label: '100 records per object', included: true },
      { label: 'AI Config (plain English setup)', included: true },
      { label: 'AI Insights & Predictions', included: false },
      { label: 'Email open/click tracking', included: false },
      { label: 'REST API access', included: false },
      { label: 'Third-party integrations', included: false },
    ],
    cta: 'Get Started Free',
  },
  {
    name: 'STARTER',
    price: '$29',
    priceDetail: 'per user / month',
    icon: <RocketIcon fontSize="large" />,
    color: '#1976d2',
    features: [
      { label: 'Up to 5 users', included: true },
      { label: '5 custom objects', included: true },
      { label: '10 workflows', included: true },
      { label: '3 dashboards', included: true },
      { label: '1,000 records per object', included: true },
      { label: 'AI Config (plain English setup)', included: true },
      { label: 'AI Insights & Predictions', included: false },
      { label: 'Email open/click tracking', included: true },
      { label: 'REST API access', included: false },
      { label: 'Third-party integrations', included: false },
    ],
    cta: 'Start 14-Day Trial',
  },
  {
    name: 'PROFESSIONAL',
    price: '$59',
    priceDetail: 'per user / month',
    icon: <PremiumIcon fontSize="large" />,
    color: '#7b1fa2',
    popular: true,
    features: [
      { label: 'Up to 25 users', included: true },
      { label: '50 custom objects', included: true },
      { label: '100 workflows', included: true },
      { label: '20 dashboards', included: true },
      { label: '50,000 records per object', included: true },
      { label: 'AI Config (plain English setup)', included: true },
      { label: 'AI Insights & Predictions', included: true },
      { label: 'Email open/click tracking', included: true },
      { label: 'REST API access', included: true },
      { label: 'Third-party integrations', included: true },
    ],
    cta: 'Start 14-Day Trial',
  },
  {
    name: 'ENTERPRISE',
    price: '$99',
    priceDetail: 'per user / month',
    icon: <DiamondIcon fontSize="large" />,
    color: '#f57c00',
    features: [
      { label: 'Unlimited users', included: true },
      { label: 'Unlimited custom objects', included: true },
      { label: 'Unlimited workflows', included: true },
      { label: 'Unlimited dashboards', included: true },
      { label: 'Unlimited records', included: true },
      { label: 'AI Config (plain English setup)', included: true },
      { label: 'AI Insights & Predictions', included: true },
      { label: 'Email open/click tracking', included: true },
      { label: 'REST API access', included: true },
      { label: 'Third-party integrations', included: true },
    ],
    cta: 'Contact Sales',
  },
];

/* ─── AI Quick Actions ───────────────────────────────────────────────────── */
const AI_QUICK_ACTIONS = [
  {
    icon: <SmartToyIcon />,
    label: 'Build CRM from Scratch',
    example: '"Set up a real estate CRM with properties, agents, and showings"',
    color: '#1976d2',
    time: '~30 sec',
  },
  {
    icon: <ObjectIcon />,
    label: 'Create Custom Object',
    example: '"Create a Patients object with name, DOB, insurance, and diagnosis"',
    color: '#7b1fa2',
    time: '~5 sec',
  },
  {
    icon: <WorkflowIcon />,
    label: 'Automate Workflow',
    example: '"When a lead status changes to Hot, assign to senior rep and send email"',
    color: '#e65100',
    time: '~8 sec',
  },
  {
    icon: <DashboardIcon />,
    label: 'Generate Dashboard',
    example: '"Build a sales dashboard with revenue by month, top reps, and pipeline chart"',
    color: '#2e7d32',
    time: '~10 sec',
  },
  {
    icon: <InsightsIcon />,
    label: 'AI Lead Scoring',
    example: '"Score my leads by engagement, budget, and timeline to close"',
    color: '#c62828',
    time: 'Real-time',
  },
  {
    icon: <BoltIcon />,
    label: 'Smart Predictions',
    example: '"Predict which deals will close this quarter and flag at-risk accounts"',
    color: '#f57c00',
    time: 'Real-time',
  },
  {
    icon: <ImportIcon />,
    label: 'Smart CSV Import',
    example: '"Import real estate listings CSV — auto-detect property fields, MLS numbers, and agent columns"',
    color: '#6d4c41',
    time: '~10 sec',
  },
  {
    icon: <CasesIcon />,
    label: 'Case Auto-Routing',
    example: '"Route support tickets to agents by priority and auto-escalate unresolved cases after 4 hours"',
    color: '#0097a7',
    time: '~5 sec',
  },
];

/* ─── Platform Stats ────────────────────────────────────────────────────── */
const STATS = [
  { value: '14', label: 'Microservices', icon: <StorageIcon /> },
  { value: '25+', label: 'Platform Modules', icon: <DashboardIcon /> },
  { value: '60s', label: 'AI Zero-Config', icon: <SpeedIcon /> },
  { value: '6', label: 'Industry Templates', icon: <LanguageIcon /> },
  { value: '20+', label: 'AI Actions', icon: <SmartToyIcon /> },
  { value: '99.9%', label: 'Uptime SLA', icon: <TimelineIcon /> },
];

/* ─── Feature Highlights (compact) ──────────────────────────────────────── */
const FEATURES = [
  {
    icon: <ConfigIcon sx={{ fontSize: 32 }} />,
    title: 'AI Config Engine',
    desc: 'Describe needs in plain English — AI builds objects, fields, workflows, and dashboards automatically.',
    color: '#1976d2',
  },
  {
    icon: <AiIcon sx={{ fontSize: 32 }} />,
    title: 'AI Insights & Predictions',
    desc: 'Lead scoring, deal predictions, churn alerts, revenue forecasts, and next-best-action recommendations powered by GPT-4o.',
    color: '#7b1fa2',
  },
  {
    icon: <PeopleIcon sx={{ fontSize: 32 }} />,
    title: 'Lead & Contact Management',
    desc: 'Full lifecycle from lead capture to conversion. Accounts, contacts, opportunities — 360° relationship views.',
    color: '#00796b',
  },
  {
    icon: <KanbanIcon sx={{ fontSize: 32 }} />,
    title: 'Pipeline & Kanban Boards',
    desc: 'Visual drag-and-drop deal pipeline with customizable stages, weighted forecasting, and conversion analytics.',
    color: '#1565c0',
  },
  {
    icon: <DashboardIcon sx={{ fontSize: 32 }} />,
    title: 'Real-Time Dashboards',
    desc: 'KPI cards, revenue charts, pipeline analytics, team leaderboards, and sales velocity tracking.',
    color: '#2e7d32',
  },
  {
    icon: <ReportsIcon sx={{ fontSize: 32 }} />,
    title: 'Reports & Analytics',
    desc: 'Custom report builder with conversion funnels, revenue attribution, quota tracking, and data health monitoring.',
    color: '#4527a0',
  },
  {
    icon: <WorkflowIcon sx={{ fontSize: 32 }} />,
    title: 'Workflow Automation',
    desc: 'Build trigger-condition-action rules. Automate lead assignment, emails, tasks, escalations, and stage transitions.',
    color: '#ed6c02',
  },
  {
    icon: <BoltIcon sx={{ fontSize: 32 }} />,
    title: 'Smart Automation (AI)',
    desc: 'AI detects patterns and suggests automations — follow-up reminders, lead routing, email sequences, and SLA alerts.',
    color: '#e65100',
  },
  {
    icon: <EmailIcon sx={{ fontSize: 32 }} />,
    title: 'Email & Communications',
    desc: 'Send emails, track opens/clicks, use templates, SMS notifications, and in-app messaging with full history.',
    color: '#c62828',
  },
  {
    icon: <CasesIcon sx={{ fontSize: 32 }} />,
    title: 'Case Management',
    desc: 'Support ticket system with priority levels, SLA tracking, auto-escalation rules, and resolution workflows.',
    color: '#0097a7',
  },
  {
    icon: <CampaignIcon sx={{ fontSize: 32 }} />,
    title: 'Campaign Management',
    desc: 'Create and manage marketing campaigns, track ROI, measure engagement, and link campaigns to opportunities.',
    color: '#d32f2f',
  },
  {
    icon: <TaskIcon sx={{ fontSize: 32 }} />,
    title: 'Activities & Tasks',
    desc: 'Log calls, meetings, tasks, and notes. Automatic reminders, overdue alerts, and activity timeline per record.',
    color: '#558b2f',
  },
  {
    icon: <CollabIcon sx={{ fontSize: 32 }} />,
    title: 'Team Collaboration',
    desc: '@mentions, deal comments, shared notes, team feeds, and real-time collaboration on opportunities.',
    color: '#00838f',
  },
  {
    icon: <ObjectIcon sx={{ fontSize: 32 }} />,
    title: 'Custom Objects & Fields',
    desc: 'Build any data model — patients, properties, students. Text, number, date, currency, and dropdown field types.',
    color: '#0288d1',
  },
  {
    icon: <ImportIcon sx={{ fontSize: 32 }} />,
    title: 'CSV Import with AI Detection',
    desc: 'Upload CSVs and AI auto-maps columns to CRM fields. Industry-specific field detection for 6+ verticals.',
    color: '#6d4c41',
  },
  {
    icon: <SecurityIcon sx={{ fontSize: 32 }} />,
    title: 'Security & Multi-Tenancy',
    desc: 'Role-based access, tenant isolation, JWT auth, MFA, audit logging, and data encryption.',
    color: '#37474f',
  },
  {
    icon: <DevIcon sx={{ fontSize: 32 }} />,
    title: 'Developer Portal & API',
    desc: 'Full REST API, webhook management, API key generation, and developer documentation for custom integrations.',
    color: '#263238',
  },
  {
    icon: <IntegrationIcon sx={{ fontSize: 32 }} />,
    title: 'Integrations & Webhooks',
    desc: 'Connect Zapier, Slack, email providers, web forms, and build custom integrations via REST API and webhooks.',
    color: '#6a1b9a',
  },
];

/* ─── Use Cases ─────────────────────────────────────────────────────────── */
const USE_CASES = [
  { industry: 'Real Estate', example: '"Create a Properties object with address, price, bedrooms, status, and listing agent fields"', features: 'MLS import, showing scheduler, agent commission tracking' },
  { industry: 'Healthcare', example: '"Build a Patients object with date of birth, insurance provider, diagnosis, and primary doctor"', features: 'Appointment workflows, HIPAA-ready fields, referral tracking' },
  { industry: 'Education', example: '"Set up a Students object with enrollment date, GPA, program, advisor, and graduation status"', features: 'Enrollment pipeline, advisor assignment, graduation tracking' },
  { industry: 'Finance', example: '"Create a Portfolio object with account value, risk level, investment type, and advisor assignment"', features: 'AUM tracking, compliance fields, client risk profiling' },
  { industry: 'Technology / SaaS', example: '"Create a Subscriptions object with plan tier, MRR, renewal date, and churn risk score"', features: 'MRR dashboards, churn prediction, renewal automation' },
  { industry: 'Manufacturing', example: '"Build a Purchase Orders object with supplier, quantity, unit cost, delivery date, and QC status"', features: 'Supplier management, QC workflows, delivery tracking' },
  { industry: 'Professional Services', example: '"Build a Projects object with budget, hours tracked, status, client, and deadline"', features: 'Time tracking, milestone billing, resource allocation' },
  { industry: 'Retail / E-Commerce', example: '"Create an Orders object with order total, shipping status, customer, and return flag"', features: 'Order pipeline, customer segmentation, return workflows' },
];

/* ─── Testimonials ──────────────────────────────────────────────────────── */
const TESTIMONIALS = [
  {
    name: 'Sarah Mitchell',
    role: 'VP Sales, TechFlow Inc.',
    text: 'We set up our entire CRM in 30 minutes using the AI Config. No developer needed. Our team was productive from day one.',
    avatar: 'SM',
  },
  {
    name: 'James Rodriguez',
    role: 'Broker, Skyline Realty',
    text: 'The CSV import detected our MLS columns automatically. Combined with AI insights predicting which deals would close, upgrading was a no-brainer.',
    avatar: 'JR',
  },
  {
    name: 'Dr. Priya Patel',
    role: 'Owner, HealthFirst Clinic',
    text: 'I told the AI "build me a patient management system" and it created everything — objects, fields, workflows — in under a minute.',
    avatar: 'PP',
  },
  {
    name: 'Michael Chen',
    role: 'CTO, DataStream SaaS',
    text: 'The developer portal and REST API made integrating with our product seamless. Webhook events trigger our billing system automatically.',
    avatar: 'MC',
  },
  {
    name: 'Lisa Thompson',
    role: 'Support Lead, CloudServe',
    text: 'Case management with auto-escalation cut our response times by 40%. The AI even suggests which tickets need immediate attention.',
    avatar: 'LT',
  },
  {
    name: 'Raj Patel',
    role: 'Director, EduPath Academy',
    text: 'We imported 5,000 student records via CSV and the AI mapped every column perfectly. The enrollment pipeline view is exactly what we needed.',
    avatar: 'RP',
  },
];

/* ─── FAQ ───────────────────────────────────────────────────────────────── */
const FAQ = [
  { q: 'Do I need a credit card to start?', a: 'No. The Free plan is completely free forever. Paid plans include a 14-day free trial — no credit card required.' },
  { q: 'Can I upgrade or downgrade anytime?', a: 'Yes! Changes take effect immediately. Your data is never deleted when switching plans.' },
  { q: 'How does the AI Config work?', a: 'Just type what you need in plain English. For example: "Create a Projects object with budget, status, and deadline fields". The AI parses your request and builds everything automatically.' },
  { q: 'Is my data secure?', a: 'Absolutely. We use JWT authentication, role-based access control, MFA, full data isolation per tenant, audit logging, and encrypted communications.' },
  { q: 'Can I use it for my specific industry?', a: 'Yes! We have built-in templates for Real Estate, Healthcare, Finance, Technology, Education, and Manufacturing. Or just tell the AI your needs and it adapts.' },
  { q: 'What happens if I exceed my plan limits?', a: 'You\'ll see a friendly upgrade prompt. Existing data is never deleted or restricted — you just can\'t create new items beyond the limit until you upgrade.' },
  { q: 'Can I import data from spreadsheets?', a: 'Yes! Upload any CSV file and our AI automatically detects columns, maps them to CRM fields, and shows a preview before importing. It even recognizes industry-specific fields.' },
  { q: 'Do you have an API for custom integrations?', a: 'Yes. Full REST API with API key management, webhook events, and a Developer Portal with documentation. Connect to Zapier, Slack, or build your own integrations.' },
  { q: 'How does case management work?', a: 'Create support tickets with priority levels. Set SLA rules with auto-escalation. The AI routes cases to the right agents and flags tickets nearing their deadline.' },
  { q: 'Is there team collaboration built in?', a: 'Yes! @mention teammates on deals, share notes, add comments to any record, and use team activity feeds. Everyone stays aligned in real time.' },
];

/* ═══════════════════════════════════════════════════════════════════════════ */

const LandingPage: React.FC = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const scrollTo = (id: string) => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
    setMobileMenuOpen(false);
  };

  const navLinks = [
    { label: 'Features', id: 'features' },
    { label: 'AI', id: 'ai-actions' },
    { label: 'Use Cases', id: 'usecases' },
    { label: 'Pricing', id: 'pricing' },
    { label: 'FAQ', id: 'faq' },
  ];

  return (
    <Box sx={{ bgcolor: '#ffffff' }}>
      {/* ── Top Navigation Bar ─────────────────────────────────────── */}
      <AppBar
        position="sticky"
        elevation={1}
        sx={{ bgcolor: '#fff', color: '#333' }}
      >
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, cursor: 'pointer' }} onClick={() => scrollTo('hero')}>
              <LogoIcon sx={{ color: '#1976d2', fontSize: 32 }} />
              <Typography variant="h6" fontWeight={800} color="primary">
                CRM Platform
              </Typography>
            </Box>

            {isMobile ? (
              <IconButton onClick={() => setMobileMenuOpen(true)}>
                <MenuIcon />
              </IconButton>
            ) : (
              <Stack direction="row" spacing={1} alignItems="center">
                {navLinks.map((l) => (
                  <Button key={l.id} onClick={() => scrollTo(l.id)} sx={{ color: '#555', textTransform: 'none', fontWeight: 600 }}>
                    {l.label}
                  </Button>
                ))}
                <Divider orientation="vertical" flexItem sx={{ mx: 1 }} />
                <Button
                  variant="outlined"
                  onClick={() => navigate('/auth/login')}
                  sx={{ textTransform: 'none', fontWeight: 600, borderRadius: 2 }}
                >
                  Log In
                </Button>
                <Button
                  variant="contained"
                  onClick={() => navigate('/auth/register')}
                  sx={{ textTransform: 'none', fontWeight: 600, borderRadius: 2 }}
                >
                  Sign Up Free
                </Button>
              </Stack>
            )}
          </Toolbar>
        </Container>

        {/* Mobile drawer */}
        <Drawer anchor="right" open={mobileMenuOpen} onClose={() => setMobileMenuOpen(false)}>
          <Box sx={{ width: 250, p: 3 }}>
            <Typography variant="h6" fontWeight={700} color="primary" sx={{ mb: 2 }}>
              CRM Platform
            </Typography>
            {navLinks.map((l) => (
              <Button key={l.id} fullWidth onClick={() => scrollTo(l.id)} sx={{ justifyContent: 'flex-start', mb: 1, textTransform: 'none' }}>
                {l.label}
              </Button>
            ))}
            <Divider sx={{ my: 2 }} />
            <Button fullWidth variant="outlined" onClick={() => navigate('/auth/login')} sx={{ mb: 1, textTransform: 'none' }}>
              Log In
            </Button>
            <Button fullWidth variant="contained" onClick={() => navigate('/auth/register')} sx={{ textTransform: 'none' }}>
              Sign Up Free
            </Button>
          </Box>
        </Drawer>
      </AppBar>

      {/* ── HERO Section ───────────────────────────────────────────── */}
      <Box
        id="hero"
        sx={{
          background: 'linear-gradient(135deg, #1565c0 0%, #7b1fa2 50%, #e65100 100%)',
          color: '#fff',
          py: { xs: 8, md: 12 },
          textAlign: 'center',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Decorative circles */}
        <Box sx={{ position: 'absolute', top: -80, right: -80, width: 300, height: 300, borderRadius: '50%', bgcolor: 'rgba(255,255,255,0.06)' }} />
        <Box sx={{ position: 'absolute', bottom: -60, left: -60, width: 200, height: 200, borderRadius: '50%', bgcolor: 'rgba(255,255,255,0.04)' }} />

        <Container maxWidth="md" sx={{ position: 'relative', zIndex: 1 }}>
          <Chip label="AI-POWERED CRM" sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: '#fff', fontWeight: 700, mb: 3, fontSize: '0.85rem' }} />
          <Typography variant={isMobile ? 'h3' : 'h2'} fontWeight={900} sx={{ mb: 2, lineHeight: 1.15 }}>
            Build Your Perfect CRM<br />
            in Plain English
          </Typography>
          <Typography variant="h6" sx={{ mb: 4, opacity: 0.9, fontWeight: 400, maxWidth: 650, mx: 'auto' }}>
            No coding. No consultants. Just tell the AI what you need —
            custom objects, fields, workflows, dashboards — and it builds everything in seconds.
            Start free, upgrade as you grow.
          </Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="center">
            <Button
              variant="contained"
              size="large"
              onClick={() => navigate('/auth/register')}
              sx={{
                bgcolor: '#fff',
                color: '#1565c0',
                fontWeight: 700,
                fontSize: '1.1rem',
                px: 5,
                py: 1.5,
                borderRadius: 3,
                textTransform: 'none',
                '&:hover': { bgcolor: '#e3f2fd' },
              }}
            >
              Start Free — No Credit Card
            </Button>
            <Button
              variant="outlined"
              size="large"
              onClick={() => navigate('/auth/login')}
              sx={{
                borderColor: 'rgba(255,255,255,0.6)',
                color: '#fff',
                fontWeight: 600,
                fontSize: '1.1rem',
                px: 5,
                py: 1.5,
                borderRadius: 3,
                textTransform: 'none',
                '&:hover': { borderColor: '#fff', bgcolor: 'rgba(255,255,255,0.1)' },
              }}
            >
              Log In
            </Button>
          </Stack>
          <Typography variant="body2" sx={{ mt: 3, opacity: 0.7 }}>
            Free plan includes 3 users, AI Config, custom objects & workflows — forever free
          </Typography>
        </Container>
      </Box>

      {/* ── Stats Bar ──────────────────────────────────────────────── */}
      <Box sx={{ py: 4, bgcolor: '#1a237e' }}>
        <Container maxWidth="md">
          <Grid container spacing={3} justifyContent="center">
            {STATS.map((s, i) => (
              <Grid item xs={4} sm={2} key={i}>
                <Box sx={{ textAlign: 'center', color: '#fff' }}>
                  <Box sx={{ color: '#90caf9', mb: 0.5 }}>{s.icon}</Box>
                  <Typography variant="h4" fontWeight={800}>{s.value}</Typography>
                  <Typography variant="body2" sx={{ opacity: 0.8 }}>{s.label}</Typography>
                </Box>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* ── AI Quick Actions Panel ─────────────────────────────────── */}
      <Box id="ai-actions" sx={{ py: 6, bgcolor: '#f8f9ff' }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: 'center', mb: 4 }}>
            <Chip icon={<SmartToyIcon />} label="AI-POWERED" sx={{ bgcolor: '#e8eaf6', color: '#3949ab', fontWeight: 700, mb: 2 }} />
            <Typography variant="h4" fontWeight={800}>
              What Can the AI Do?
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mt: 1, maxWidth: 500, mx: 'auto' }}>
              Just type what you need — the AI handles the rest. Here are some quick actions:
            </Typography>
          </Box>
          <Grid container spacing={2.5}>
            {AI_QUICK_ACTIONS.map((a, i) => (
              <Grid item xs={12} sm={6} md={4} key={i}>
                <Card
                  elevation={0}
                  sx={{
                    p: 2.5,
                    border: '1px solid',
                    borderColor: alpha(a.color, 0.2),
                    borderRadius: 3,
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    transition: 'all 0.25s',
                    cursor: 'pointer',
                    '&:hover': {
                      borderColor: a.color,
                      bgcolor: alpha(a.color, 0.04),
                      transform: 'translateY(-3px)',
                      boxShadow: `0 6px 24px ${alpha(a.color, 0.15)}`,
                    },
                  }}
                  onClick={() => navigate('/auth/register')}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5 }}>
                    <Avatar sx={{ bgcolor: alpha(a.color, 0.12), color: a.color, width: 40, height: 40 }}>
                      {a.icon}
                    </Avatar>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="subtitle2" fontWeight={700}>{a.label}</Typography>
                      <Chip label={a.time} size="small" sx={{ height: 20, fontSize: '0.65rem', bgcolor: alpha(a.color, 0.1), color: a.color }} />
                    </Box>
                  </Box>
                  <Box sx={{ bgcolor: '#f5f5f5', borderRadius: 2, p: 1.5, flex: 1, display: 'flex', alignItems: 'center' }}>
                    <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                      <TerminalIcon sx={{ fontSize: 16, color: '#9e9e9e', mt: 0.3 }} />
                      <Typography variant="body2" sx={{ fontStyle: 'italic', color: 'text.secondary', fontSize: '0.8rem', lineHeight: 1.5 }}>
                        {a.example}
                      </Typography>
                    </Box>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', mt: 1.5, color: a.color }}>
                    <Typography variant="caption" fontWeight={600}>Try it free</Typography>
                    <ArrowIcon sx={{ fontSize: 14, ml: 0.5 }} />
                  </Box>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* ── AI Live Demo Preview ───────────────────────────────────── */}
      <Box sx={{ py: 6, bgcolor: '#fff' }}>
        <Container maxWidth="md">
          <Grid container spacing={4} alignItems="center">
            <Grid item xs={12} md={6}>
              <Chip icon={<PlayIcon />} label="SEE IT IN ACTION" sx={{ bgcolor: '#e8f5e9', color: '#2e7d32', fontWeight: 700, mb: 2 }} />
              <Typography variant="h5" fontWeight={800} gutterBottom>
                Type a Sentence,<br />Get a Full CRM
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3, lineHeight: 1.7 }}>
                Our AI understands natural language. Describe your business and it creates custom objects, fields, pipelines, workflows, and dashboards — all in one go.
              </Typography>
              <Stack spacing={1.5}>
                {[
                  { icon: <CheckIcon sx={{ color: '#4caf50', fontSize: 18 }} />, text: 'Creates 5-10 custom fields per object automatically' },
                  { icon: <CheckIcon sx={{ color: '#4caf50', fontSize: 18 }} />, text: 'Builds stage-based pipelines with drag-and-drop' },
                  { icon: <CheckIcon sx={{ color: '#4caf50', fontSize: 18 }} />, text: 'Generates dashboard widgets with real charts' },
                  { icon: <CheckIcon sx={{ color: '#4caf50', fontSize: 18 }} />, text: 'Sets up automated workflow rules instantly' },
                ].map((item, i) => (
                  <Box key={i} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {item.icon}
                    <Typography variant="body2" color="text.secondary">{item.text}</Typography>
                  </Box>
                ))}
              </Stack>
            </Grid>
            <Grid item xs={12} md={6}>
              {/* Simulated terminal / chat UI */}
              <Card elevation={3} sx={{ borderRadius: 3, overflow: 'hidden' }}>
                <Box sx={{ bgcolor: '#263238', p: 1.5, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: '#ef5350' }} />
                  <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: '#ffca28' }} />
                  <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: '#66bb6a' }} />
                  <Typography variant="caption" sx={{ color: '#78909c', ml: 1 }}>AI Config Chat</Typography>
                </Box>
                <Box sx={{ bgcolor: '#37474f', p: 2.5, minHeight: 240 }}>
                  {/* User message */}
                  <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
                    <Avatar sx={{ width: 28, height: 28, bgcolor: '#1976d2', fontSize: 12 }}>U</Avatar>
                    <Box sx={{ bgcolor: '#455a64', borderRadius: 2, p: 1.5, maxWidth: '85%' }}>
                      <Typography variant="body2" sx={{ color: '#e0e0e0', fontSize: '0.8rem' }}>
                        Create a Properties object with address, price, bedrooms, bathrooms, status, listing date, and agent fields
                      </Typography>
                    </Box>
                  </Box>
                  {/* AI response */}
                  <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
                    <Avatar sx={{ width: 28, height: 28, bgcolor: '#7b1fa2', fontSize: 12 }}>
                      <SmartToyIcon sx={{ fontSize: 16 }} />
                    </Avatar>
                    <Box sx={{ bgcolor: '#455a64', borderRadius: 2, p: 1.5, maxWidth: '85%' }}>
                      <Typography variant="body2" sx={{ color: '#66bb6a', fontSize: '0.8rem', fontWeight: 600, mb: 0.5 }}>
                        Done! Created "Properties" with 7 fields:
                      </Typography>
                      <Typography variant="body2" sx={{ color: '#b0bec5', fontSize: '0.75rem', lineHeight: 1.8 }}>
                        address (text) &bull; price (currency) &bull; bedrooms (number)<br />
                        bathrooms (number) &bull; status (dropdown) &bull; listing_date (date)<br />
                        agent (text)
                      </Typography>
                    </Box>
                  </Box>
                  {/* Typing indicator */}
                  <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                    <Avatar sx={{ width: 28, height: 28, bgcolor: '#7b1fa2', fontSize: 12 }}>
                      <SmartToyIcon sx={{ fontSize: 16 }} />
                    </Avatar>
                    <Box sx={{ display: 'flex', gap: 0.5 }}>
                      {[0, 1, 2].map((d) => (
                        <Box key={d} sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: '#78909c',
                          animation: 'pulse 1.4s ease-in-out infinite', animationDelay: `${d * 0.2}s`,
                          '@keyframes pulse': { '0%, 80%, 100%': { opacity: 0.3 }, '40%': { opacity: 1 } },
                        }} />
                      ))}
                    </Box>
                  </Box>
                </Box>
              </Card>
            </Grid>
          </Grid>
        </Container>
      </Box>

      {/* ── How It Works ───────────────────────────────────────────── */}
      <Box sx={{ py: 6, bgcolor: '#fafafa' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={800} textAlign="center" gutterBottom>
            How It Works
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 6, maxWidth: 600, mx: 'auto' }}>
            From sign-up to a fully customized CRM in 3 simple steps
          </Typography>
          <Grid container spacing={4}>
            {[
              { step: '1', title: 'Sign Up for Free', desc: 'Create your account with a Tenant ID. Your workspace is instantly ready — no setup, no waiting.', color: '#1976d2' },
              { step: '2', title: 'Tell the AI What You Need', desc: 'Type instructions like "Create a Properties object with price, bedrooms, and status fields". The AI builds it in seconds.', color: '#7b1fa2' },
              { step: '3', title: 'Start Selling', desc: 'Manage leads, track deals, automate workflows, and view dashboards. Upgrade when you need more power.', color: '#e65100' },
            ].map((s) => (
              <Grid item xs={12} md={4} key={s.step}>
                <Box sx={{ textAlign: 'center' }}>
                  <Avatar sx={{ width: 64, height: 64, bgcolor: s.color, fontSize: 28, fontWeight: 800, mx: 'auto', mb: 2 }}>
                    {s.step}
                  </Avatar>
                  <Typography variant="h6" fontWeight={700} gutterBottom>
                    {s.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 320, mx: 'auto' }}>
                    {s.desc}
                  </Typography>
                </Box>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* ── Features Section (medium compact cards) ────────────── */}
      <Box id="features" sx={{ py: 6 }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={800} textAlign="center" gutterBottom>
            Everything You Need to Grow
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 1 }}>
            18 feature modules across sales, marketing, support, and operations
          </Typography>
          <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ mb: 4, maxWidth: 600, mx: 'auto' }}>
            A full-featured, AI-powered CRM platform built on 14 microservices — every module works together seamlessly
          </Typography>
          <Grid container spacing={2}>
            {FEATURES.map((f, i) => (
              <Grid item xs={6} sm={4} md={3} key={i}>
                <Card
                  elevation={0}
                  sx={{
                    height: '100%',
                    p: 2,
                    border: '1px solid #e0e0e0',
                    borderRadius: 2.5,
                    transition: 'all 0.2s',
                    '&:hover': { borderColor: f.color, boxShadow: `0 3px 16px ${alpha(f.color, 0.12)}`, transform: 'translateY(-2px)' },
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
                    <Avatar sx={{ bgcolor: alpha(f.color, 0.1), color: f.color, width: 36, height: 36 }}>
                      {f.icon}
                    </Avatar>
                    <Typography variant="subtitle2" fontWeight={700}>
                      {f.title}
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.5, fontSize: '0.8rem' }}>
                    {f.desc}
                  </Typography>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* ── AI Capabilities Showcase ───────────────────────────────── */}
      <Box sx={{ py: 6, background: 'linear-gradient(135deg, #ede7f6 0%, #e3f2fd 50%, #e8f5e9 100%)' }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: 'center', mb: 4 }}>
            <Chip icon={<AiIcon />} label="ARTIFICIAL INTELLIGENCE" sx={{ bgcolor: '#fff', color: '#7b1fa2', fontWeight: 700, mb: 2, boxShadow: 1 }} />
            <Typography variant="h4" fontWeight={800}>
              AI That Actually Works for You
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mt: 1, maxWidth: 550, mx: 'auto' }}>
              Not just a chatbot — a full AI engine that configures, predicts, and automates your entire CRM
            </Typography>
          </Box>

          {/* Row 1: Two large AI cards */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} md={6}>
              <Card elevation={2} sx={{ borderRadius: 3, overflow: 'hidden', height: '100%' }}>
                <Box sx={{ background: 'linear-gradient(135deg, #1565c0, #1976d2)', p: 3, color: '#fff' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5 }}>
                    <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 44, height: 44 }}>
                      <ConfigIcon />
                    </Avatar>
                    <Box>
                      <Typography variant="h6" fontWeight={700}>AI Configuration Engine</Typography>
                      <Chip label="GPT-4o Powered" size="small" sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: '#fff', height: 20, fontSize: '0.65rem' }} />
                    </Box>
                  </Box>
                  <Typography variant="body2" sx={{ opacity: 0.9, lineHeight: 1.7 }}>
                    Describe your business in one sentence. The AI creates custom objects, fields, pipelines, workflows, dashboards, and roles — all configured and ready to use.
                  </Typography>
                </Box>
                <CardContent sx={{ p: 2.5 }}>
                  <Typography variant="caption" fontWeight={700} color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 1 }}>
                    Example Prompts
                  </Typography>
                  <Stack spacing={1} sx={{ mt: 1.5 }}>
                    {[
                      '"Create a Leads pipeline with stages: New, Contacted, Qualified, Proposal, Won, Lost"',
                      '"Add a urgency dropdown field to Cases with values Low, Medium, High, Critical"',
                      '"Build a workflow: when deal value > $50k, notify the VP of Sales"',
                    ].map((p, i) => (
                      <Box key={i} sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                        <ChatIcon sx={{ fontSize: 14, color: '#1976d2', mt: 0.4 }} />
                        <Typography variant="body2" sx={{ fontSize: '0.78rem', color: 'text.secondary', fontStyle: 'italic' }}>{p}</Typography>
                      </Box>
                    ))}
                  </Stack>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={6}>
              <Card elevation={2} sx={{ borderRadius: 3, overflow: 'hidden', height: '100%' }}>
                <Box sx={{ background: 'linear-gradient(135deg, #6a1b9a, #7b1fa2)', p: 3, color: '#fff' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5 }}>
                    <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)', width: 44, height: 44 }}>
                      <InsightsIcon />
                    </Avatar>
                    <Box>
                      <Typography variant="h6" fontWeight={700}>AI Insights & Predictions</Typography>
                      <Chip label="Real-Time Analysis" size="small" sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: '#fff', height: 20, fontSize: '0.65rem' }} />
                    </Box>
                  </Box>
                  <Typography variant="body2" sx={{ opacity: 0.9, lineHeight: 1.7 }}>
                    Get AI-powered lead scoring, revenue forecasts, deal-close predictions, churn risk alerts, and next-best-action recommendations — all in real time.
                  </Typography>
                </Box>
                <CardContent sx={{ p: 2.5 }}>
                  <Typography variant="caption" fontWeight={700} color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 1 }}>
                    What AI Predicts
                  </Typography>
                  <Grid container spacing={1.5} sx={{ mt: 0.5 }}>
                    {[
                      { label: 'Lead Score', value: '87/100', color: '#4caf50' },
                      { label: 'Close Probability', value: '73%', color: '#1976d2' },
                      { label: 'Churn Risk', value: 'Low', color: '#ff9800' },
                      { label: 'Revenue Forecast', value: '$142K', color: '#7b1fa2' },
                    ].map((m, i) => (
                      <Grid item xs={6} key={i}>
                        <Box sx={{ bgcolor: '#f5f5f5', borderRadius: 2, p: 1.5, textAlign: 'center' }}>
                          <Typography variant="h6" fontWeight={800} sx={{ color: m.color }}>{m.value}</Typography>
                          <Typography variant="caption" color="text.secondary">{m.label}</Typography>
                        </Box>
                      </Grid>
                    ))}
                  </Grid>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Row 2: Four smaller AI capability cards */}
          <Grid container spacing={2}>
            {[
              {
                icon: <SmartToyIcon />,
                title: 'Natural Language Processing',
                desc: 'No forms or wizards — just type what you want in plain English and the AI understands context, intent, and relationships.',
                color: '#1976d2',
                gradient: 'linear-gradient(135deg, #e3f2fd, #bbdefb)',
              },
              {
                icon: <BoltIcon />,
                title: 'Instant Automation',
                desc: 'AI detects patterns and suggests automations: follow-up reminders, lead routing, email sequences, and stage transitions.',
                color: '#e65100',
                gradient: 'linear-gradient(135deg, #fff3e0, #ffe0b2)',
              },
              {
                icon: <PieChartIcon />,
                title: 'Smart Analytics',
                desc: 'AI auto-generates dashboard widgets based on your data — revenue trends, conversion funnels, team leaderboards, and forecasts.',
                color: '#2e7d32',
                gradient: 'linear-gradient(135deg, #e8f5e9, #c8e6c9)',
              },
              {
                icon: <SecurityIcon />,
                title: 'AI-Powered Security',
                desc: 'Anomaly detection on login patterns, intelligent role suggestions based on usage, and automated audit trail analysis.',
                color: '#37474f',
                gradient: 'linear-gradient(135deg, #eceff1, #cfd8dc)',
              },
            ].map((c, i) => (
              <Grid item xs={12} sm={6} md={3} key={i}>
                <Card
                  elevation={0}
                  sx={{
                    height: '100%',
                    p: 2.5,
                    background: c.gradient,
                    border: '1px solid',
                    borderColor: alpha(c.color, 0.15),
                    borderRadius: 3,
                    transition: 'all 0.2s',
                    '&:hover': { transform: 'translateY(-3px)', boxShadow: `0 6px 20px ${alpha(c.color, 0.15)}` },
                  }}
                >
                  <Avatar sx={{ bgcolor: '#fff', color: c.color, width: 40, height: 40, mb: 1.5, boxShadow: 1 }}>
                    {c.icon}
                  </Avatar>
                  <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                    {c.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.78rem', lineHeight: 1.6 }}>
                    {c.desc}
                  </Typography>
                </Card>
              </Grid>
            ))}
          </Grid>

          <Box sx={{ textAlign: 'center', mt: 4 }}>
            <Button
              variant="contained"
              size="large"
              onClick={() => navigate('/auth/register')}
              sx={{ textTransform: 'none', fontWeight: 700, borderRadius: 3, px: 5, py: 1.5, fontSize: '1rem' }}
            >
              Try AI Config Free
            </Button>
          </Box>
        </Container>
      </Box>

      {/* ── Complete Platform Architecture ─────────────────────────── */}
      <Box sx={{ py: 6, bgcolor: '#fff' }}>
        <Container maxWidth="lg">
          <Box sx={{ textAlign: 'center', mb: 4 }}>
            <Chip icon={<StorageIcon />} label="ENTERPRISE ARCHITECTURE" sx={{ bgcolor: '#e8eaf6', color: '#3949ab', fontWeight: 700, mb: 2 }} />
            <Typography variant="h4" fontWeight={800}>
              14 Microservices. One Platform.
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mt: 1, maxWidth: 600, mx: 'auto' }}>
              Built on a modern distributed architecture — each service scales independently for enterprise-grade reliability
            </Typography>
          </Box>
          <Grid container spacing={1.5}>
            {[
              { name: 'Auth Service', desc: 'Login, registration, JWT, MFA, roles', icon: <SecurityIcon />, color: '#1565c0' },
              { name: 'Lead Service', desc: 'Lead capture, scoring, conversion', icon: <GrowthIcon />, color: '#2e7d32' },
              { name: 'Account Service', desc: 'Company records, hierarchies', icon: <TeamsIcon />, color: '#00796b' },
              { name: 'Contact Service', desc: '360° contact profiles', icon: <PeopleIcon />, color: '#0288d1' },
              { name: 'Opportunity Service', desc: 'Deal tracking, pipeline, forecasting', icon: <KanbanIcon />, color: '#7b1fa2' },
              { name: 'Activity Service', desc: 'Tasks, calls, meetings, notes', icon: <TaskIcon />, color: '#558b2f' },
              { name: 'Notification Service', desc: 'Email, SMS, in-app alerts', icon: <NotifIcon />, color: '#e65100' },
              { name: 'Workflow Service', desc: 'Automation rules & triggers', icon: <WorkflowIcon />, color: '#ed6c02' },
              { name: 'AI Service', desc: 'Config engine, insights, predictions', icon: <AiIcon />, color: '#6a1b9a' },
              { name: 'Email Service', desc: 'Templates, tracking, campaigns', icon: <EmailIcon />, color: '#c62828' },
              { name: 'Integration Service', desc: 'REST API, webhooks, web forms', icon: <ApiIcon />, color: '#263238' },
              { name: 'Case Service', desc: 'Tickets, SLA, escalation', icon: <CasesIcon />, color: '#0097a7' },
              { name: 'Campaign Service', desc: 'Marketing campaigns & ROI', icon: <CampaignIcon />, color: '#d32f2f' },
              { name: 'AI Agent', desc: 'Conversational AI assistant', icon: <SmartToyIcon />, color: '#4527a0' },
            ].map((svc, i) => (
              <Grid item xs={6} sm={4} md={3} lg={12/7} key={i}>
                <Box
                  sx={{
                    p: 1.5,
                    border: '1px solid',
                    borderColor: alpha(svc.color, 0.2),
                    borderRadius: 2,
                    textAlign: 'center',
                    transition: 'all 0.2s',
                    '&:hover': { borderColor: svc.color, bgcolor: alpha(svc.color, 0.04), transform: 'translateY(-2px)' },
                  }}
                >
                  <Avatar sx={{ bgcolor: alpha(svc.color, 0.1), color: svc.color, width: 36, height: 36, mx: 'auto', mb: 0.5 }}>
                    {svc.icon}
                  </Avatar>
                  <Typography variant="caption" fontWeight={700} display="block" sx={{ fontSize: '0.7rem' }}>
                    {svc.name}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.6rem', lineHeight: 1.3 }}>
                    {svc.desc}
                  </Typography>
                </Box>
              </Grid>
            ))}
          </Grid>
          <Box sx={{ textAlign: 'center', mt: 3 }}>
            <Stack direction="row" spacing={3} justifyContent="center" flexWrap="wrap" useFlexGap>
              {[
                'PostgreSQL Databases',
                'Redis Caching',
                'Apache Kafka Events',
                'Docker Orchestration',
                'Nginx Load Balancing',
              ].map((tech) => (
                <Chip key={tech} label={tech} variant="outlined" size="small" sx={{ fontWeight: 600, fontSize: '0.75rem' }} />
              ))}
            </Stack>
          </Box>
        </Container>
      </Box>

      {/* ── Use Cases Section ──────────────────────────────────────── */}
      <Box id="usecases" sx={{ py: 8, bgcolor: '#f5f5f5' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={800} textAlign="center" gutterBottom>
            Built for Every Industry
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 5, maxWidth: 600, mx: 'auto' }}>
            Just tell the AI your industry — it builds the perfect CRM for you
          </Typography>
          <Grid container spacing={3}>
            {USE_CASES.map((uc, i) => (
              <Grid item xs={12} sm={6} md={3} key={i}>
                <Card elevation={0} sx={{ height: '100%', p: 2.5, border: '1px solid #e0e0e0', borderRadius: 3, bgcolor: '#fff' }}>
                  <Typography variant="subtitle1" fontWeight={700} gutterBottom color="primary">
                    {uc.industry}
                  </Typography>
                  <Typography
                    variant="body2"
                    sx={{ fontStyle: 'italic', color: 'text.secondary', bgcolor: '#f0f7ff', p: 1.5, borderRadius: 2, lineHeight: 1.6, fontSize: '0.78rem', mb: 1.5 }}
                  >
                    {uc.example}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ lineHeight: 1.6 }}>
                    <strong>Includes:</strong> {uc.features}
                  </Typography>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* ── Pricing Section ────────────────────────────────────────── */}
      <Box id="pricing" sx={{ py: 8 }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={800} textAlign="center" gutterBottom>
            Simple, Transparent Pricing
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 2 }}>
            Start free. Upgrade when you need more power. No hidden fees.
          </Typography>
          <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ mb: 5 }}>
            All paid plans include a <strong>14-day free trial</strong> — no credit card required
          </Typography>

          <Grid container spacing={3} justifyContent="center">
            {PLANS.map((plan) => (
              <Grid item xs={12} sm={6} md={3} key={plan.name}>
                <Card
                  elevation={plan.popular ? 8 : 1}
                  sx={{
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    border: plan.popular ? `2px solid ${plan.color}` : '1px solid #e0e0e0',
                    borderRadius: 3,
                    position: 'relative',
                    transition: 'transform 0.2s',
                    '&:hover': { transform: 'translateY(-4px)' },
                  }}
                >
                  {plan.popular && (
                    <Chip
                      label="MOST POPULAR"
                      size="small"
                      sx={{
                        position: 'absolute',
                        top: -12,
                        left: '50%',
                        transform: 'translateX(-50%)',
                        bgcolor: plan.color,
                        color: '#fff',
                        fontWeight: 700,
                        fontSize: '0.7rem',
                      }}
                    />
                  )}
                  <CardContent sx={{ flexGrow: 1, pt: 3 }}>
                    <Box sx={{ textAlign: 'center', mb: 2 }}>
                      <Box sx={{ color: plan.color, mb: 1 }}>{plan.icon}</Box>
                      <Typography variant="h6" fontWeight={700}>
                        {plan.name}
                      </Typography>
                      <Typography variant="h3" fontWeight={800} sx={{ color: plan.color, my: 1 }}>
                        {plan.price}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {plan.priceDetail}
                      </Typography>
                    </Box>
                    <Divider sx={{ my: 2 }} />
                    <List dense disablePadding>
                      {plan.features.map((feat) => (
                        <ListItem key={feat.label} disableGutters sx={{ py: 0.3 }}>
                          <ListItemIcon sx={{ minWidth: 28 }}>
                            {feat.included ? (
                              <CheckIcon fontSize="small" sx={{ color: '#4caf50' }} />
                            ) : (
                              <CloseIcon fontSize="small" sx={{ color: '#ccc' }} />
                            )}
                          </ListItemIcon>
                          <ListItemText
                            primary={feat.label}
                            primaryTypographyProps={{
                              variant: 'body2',
                              color: feat.included ? 'text.primary' : 'text.disabled',
                            }}
                          />
                        </ListItem>
                      ))}
                    </List>
                  </CardContent>
                  <Box sx={{ p: 2, pt: 0 }}>
                    <Button
                      fullWidth
                      variant={plan.popular ? 'contained' : 'outlined'}
                      onClick={() => navigate('/auth/register')}
                      sx={{
                        textTransform: 'none',
                        fontWeight: 600,
                        borderRadius: 2,
                        ...(plan.popular ? { bgcolor: plan.color, '&:hover': { bgcolor: plan.color, opacity: 0.9 } } : { borderColor: plan.color, color: plan.color }),
                      }}
                    >
                      {plan.cta}
                    </Button>
                  </Box>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* ── Plan Comparison Table ──────────────────────────────────── */}
      <Box sx={{ py: 6, bgcolor: '#fafafa' }}>
        <Container maxWidth="md">
          <Typography variant="h5" fontWeight={700} textAlign="center" gutterBottom>
            Compare Plans at a Glance
          </Typography>
          <Box sx={{ overflowX: 'auto', mt: 3 }}>
            <Box
              component="table"
              sx={{
                width: '100%',
                borderCollapse: 'collapse',
                '& th, & td': { p: 1.5, textAlign: 'center', borderBottom: '1px solid #e0e0e0', fontSize: '0.875rem' },
                '& th': { fontWeight: 700, bgcolor: '#f5f5f5' },
                '& td:first-of-type': { textAlign: 'left', fontWeight: 600 },
              }}
            >
              <thead>
                <tr>
                  <th style={{ textAlign: 'left' }}>Feature</th>
                  <th>Free</th>
                  <th>Starter</th>
                  <th style={{ color: '#7b1fa2' }}>Professional</th>
                  <th>Enterprise</th>
                </tr>
              </thead>
              <tbody>
                {[
                  ['Users', '3', '5', '25', 'Unlimited'],
                  ['Custom Objects', '2', '5', '50', 'Unlimited'],
                  ['Workflows', '3', '10', '100', 'Unlimited'],
                  ['Dashboards', '1', '3', '20', 'Unlimited'],
                  ['Records / Object', '100', '1,000', '50,000', 'Unlimited'],
                  ['AI Config', '\u2705', '\u2705', '\u2705', '\u2705'],
                  ['AI Insights', '\u274c', '\u274c', '\u2705', '\u2705'],
                  ['Email Tracking', '\u274c', '\u2705', '\u2705', '\u2705'],
                  ['REST API', '\u274c', '\u274c', '\u2705', '\u2705'],
                  ['Integrations', '\u274c', '\u274c', '\u2705', '\u2705'],
                  ['Priority Support', '\u274c', '\u274c', '\u274c', '\u2705'],
                ].map((row, i) => (
                  <tr key={i}>
                    {row.map((cell, j) => (
                      j === 0 ? <td key={j}>{cell}</td> : <td key={j}>{cell}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </Box>
          </Box>
        </Container>
      </Box>

      {/* ── Testimonials ───────────────────────────────────────────── */}
      <Box sx={{ py: 8 }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={800} textAlign="center" gutterBottom>
            Trusted by Growing Teams
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 5 }}>
            See why businesses love our AI-powered CRM
          </Typography>
          <Grid container spacing={3}>
            {TESTIMONIALS.map((t, i) => (
              <Grid item xs={12} md={4} key={i}>
                <Card elevation={0} sx={{ height: '100%', p: 3, border: '1px solid #e0e0e0', borderRadius: 3 }}>
                  <QuoteIcon sx={{ color: '#1976d2', fontSize: 32, mb: 1, opacity: 0.5 }} />
                  <Typography variant="body2" sx={{ mb: 3, lineHeight: 1.7, fontStyle: 'italic', color: 'text.secondary' }}>
                    "{t.text}"
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Avatar sx={{ bgcolor: '#1976d2', fontWeight: 700, width: 40, height: 40 }}>{t.avatar}</Avatar>
                    <Box>
                      <Typography variant="subtitle2" fontWeight={700}>{t.name}</Typography>
                      <Typography variant="caption" color="text.secondary">{t.role}</Typography>
                    </Box>
                  </Box>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* ── FAQ Section ────────────────────────────────────────────── */}
      <Box id="faq" sx={{ py: 8, bgcolor: '#fafafa' }}>
        <Container maxWidth="md">
          <Typography variant="h4" fontWeight={800} textAlign="center" gutterBottom>
            Frequently Asked Questions
          </Typography>
          <Box sx={{ mt: 4 }}>
            <Grid container spacing={3}>
              {FAQ.map((item, i) => (
                <Grid item xs={12} md={6} key={i}>
                  <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                    {item.q}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2, lineHeight: 1.6 }}>
                    {item.a}
                  </Typography>
                </Grid>
              ))}
            </Grid>
          </Box>
        </Container>
      </Box>

      {/* ── Final CTA ──────────────────────────────────────────────── */}
      <Box
        sx={{
          py: 8,
          background: 'linear-gradient(135deg, #1565c0 0%, #7b1fa2 100%)',
          color: '#fff',
          textAlign: 'center',
        }}
      >
        <Container maxWidth="md">
          <Typography variant="h4" fontWeight={800} gutterBottom>
            Ready to Transform Your Sales?
          </Typography>
          <Typography variant="h6" sx={{ mb: 4, opacity: 0.9, fontWeight: 400, maxWidth: 550, mx: 'auto' }}>
            Join thousands of teams using AI to build and manage their CRM.
            Start free today — no credit card, no commitment.
          </Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="center">
            <Button
              variant="contained"
              size="large"
              onClick={() => navigate('/auth/register')}
              sx={{
                bgcolor: '#fff',
                color: '#1565c0',
                fontWeight: 700,
                fontSize: '1.1rem',
                px: 5,
                py: 1.5,
                borderRadius: 3,
                textTransform: 'none',
                '&:hover': { bgcolor: '#e3f2fd' },
              }}
            >
              Create Free Account
            </Button>
            <Button
              variant="outlined"
              size="large"
              onClick={() => navigate('/auth/login')}
              sx={{
                borderColor: 'rgba(255,255,255,0.6)',
                color: '#fff',
                fontWeight: 600,
                fontSize: '1.1rem',
                px: 5,
                py: 1.5,
                borderRadius: 3,
                textTransform: 'none',
                '&:hover': { borderColor: '#fff', bgcolor: 'rgba(255,255,255,0.1)' },
              }}
            >
              Log In to Your Account
            </Button>
          </Stack>
        </Container>
      </Box>

      {/* ── Footer ─────────────────────────────────────────────────── */}
      <Box sx={{ py: 4, bgcolor: '#212121', color: '#bdbdbd' }}>
        <Container maxWidth="lg">
          <Grid container spacing={4}>
            <Grid item xs={12} md={4}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
                <LogoIcon sx={{ color: '#90caf9' }} />
                <Typography variant="h6" fontWeight={700} sx={{ color: '#fff' }}>
                  CRM Platform
                </Typography>
              </Box>
              <Typography variant="body2" sx={{ lineHeight: 1.7 }}>
                The AI-powered CRM that adapts to any business.
                Build custom objects, automate workflows, and close more deals.
              </Typography>
            </Grid>
            <Grid item xs={6} md={2}>
              <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#fff', mb: 1 }}>Product</Typography>
              <Typography variant="body2" sx={{ cursor: 'pointer', mb: 0.5, '&:hover': { color: '#fff' } }} onClick={() => scrollTo('features')}>Features</Typography>
              <Typography variant="body2" sx={{ cursor: 'pointer', mb: 0.5, '&:hover': { color: '#fff' } }} onClick={() => scrollTo('pricing')}>Pricing</Typography>
              <Typography variant="body2" sx={{ cursor: 'pointer', mb: 0.5, '&:hover': { color: '#fff' } }} onClick={() => scrollTo('usecases')}>Use Cases</Typography>
              <Typography variant="body2" sx={{ cursor: 'pointer', mb: 0.5, '&:hover': { color: '#fff' } }} onClick={() => scrollTo('faq')}>FAQ</Typography>
            </Grid>
            <Grid item xs={6} md={2}>
              <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#fff', mb: 1 }}>Company</Typography>
              <Typography variant="body2" sx={{ mb: 0.5 }}>About Us</Typography>
              <Typography variant="body2" sx={{ mb: 0.5 }}>Blog</Typography>
              <Typography variant="body2" sx={{ mb: 0.5 }}>Careers</Typography>
              <Typography variant="body2" sx={{ mb: 0.5 }}>Contact</Typography>
            </Grid>
            <Grid item xs={12} md={4}>
              <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#fff', mb: 1 }}>Get Started</Typography>
              <Typography variant="body2" sx={{ mb: 2, lineHeight: 1.7 }}>
                Create your free account and build your CRM in minutes using AI.
              </Typography>
              <Button
                variant="contained"
                size="small"
                onClick={() => navigate('/auth/register')}
                sx={{ textTransform: 'none', borderRadius: 2 }}
              >
                Sign Up Free
              </Button>
            </Grid>
          </Grid>
          <Divider sx={{ my: 3, borderColor: '#424242' }} />
          <Typography variant="body2" textAlign="center" sx={{ opacity: 0.7 }}>
            &copy; {new Date().getFullYear()} CRM Platform. All rights reserved.
          </Typography>
        </Container>
      </Box>

      {/* ── Scroll to Top FAB ──────────────────────────────────────── */}
      <Fab
        size="small"
        onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
        sx={{ position: 'fixed', bottom: 24, right: 24, bgcolor: '#1976d2', color: '#fff', '&:hover': { bgcolor: '#1565c0' } }}
      >
        <ScrollTopIcon />
      </Fab>
    </Box>
  );
};

export default LandingPage;
