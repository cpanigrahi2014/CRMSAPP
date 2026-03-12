/* ============================================================
   DashboardPage – KPI cards + charts (real API data)
   Drag-and-drop widget rearrangement + auto-refresh
   ============================================================ */
import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { Grid, Card, CardContent, Typography, Chip, Box, CircularProgress, Alert } from '@mui/material';
import {
  TrendingUp as RevenueIcon,
  AttachMoney as MoneyIcon,
  Warning as AlertIcon,
  Speed as VelocityIcon,
  SmartToy as AiIcon,
  Psychology as InsightsIcon,
  AutoFixHigh as AutomationIcon,
  Settings as ConfigIcon,
  Assessment as ReportIcon,
  Bolt as BoltIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { MetricCard, ChartWidget, PageHeader, AutoRefreshControl } from '../components';
import DraggableWidgetGrid from '../components/DraggableWidgetGrid';
import type { DashboardWidget } from '../components/DraggableWidgetGrid';
import { opportunityService } from '../services';
import type { PipelineDashboard, StageConversionAnalytics, SalesQuota } from '../types';

const PIE_COLORS = ['#1976d2', '#7c3aed', '#059669', '#d97706', '#dc2626', '#0891b2', '#8b5cf6'];

const fmt = (n: number) => {
  if (n >= 1_000_000) return `$${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `$${(n / 1_000).toFixed(0)}K`;
  return `$${n.toLocaleString()}`;
};

const aiQuickActions = [
  { label: 'AI Insights', desc: 'Predictive analytics & scores', icon: InsightsIcon, path: '/ai-insights', color: '#7c3aed' },
  { label: 'AI Config', desc: 'Configure AI models & prompts', icon: ConfigIcon, path: '/ai-config', color: '#1976d2' },
  { label: 'Smart Automation', desc: 'AI-driven workflow rules', icon: AutomationIcon, path: '/automation', color: '#059669' },
  { label: 'AI Reports', desc: 'Generate reports with AI', icon: ReportIcon, path: '/reports', color: '#d97706' },
  { label: 'AI Chat Agent', desc: 'Ask anything about your CRM', icon: AiIcon, path: '/ai-config', color: '#dc2626' },
  { label: 'Quick Actions', desc: 'AI-powered bulk operations', icon: BoltIcon, path: '/ai-config', color: '#0891b2' },
];

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<PipelineDashboard | null>(null);
  const [conversion, setConversion] = useState<StageConversionAnalytics | null>(null);
  const [quotas, setQuotas] = useState<SalesQuota[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [dashRes, convRes, quotaRes] = await Promise.all([
        opportunityService.getDashboard(),
        opportunityService.getConversionAnalytics(),
        opportunityService.getActiveQuotas(),
      ]);
      setDashboard(dashRes.data);
      setConversion(convRes.data);
      setQuotas(Array.isArray(quotaRes.data) ? quotaRes.data : []);
    } catch (e: any) {
      setError(e?.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const d = dashboard;

  const totalAlerts = d ? (d.overdueDeals || 0) + (d.closingSoonDeals || 0) + (d.staleDeals || 0) : 0;

  /* ---- Define draggable widgets ---- */
  const dashboardWidgets: DashboardWidget[] = useMemo(() => {
    if (!d) return [];

    const pipelineStageData = (d.stageBreakdown || []).map((s) => ({
      stage: s.stage.replace(/_/g, ' '),
      value: s.totalAmount,
      count: s.count,
    }));

    const leadSourceData = Object.entries(d.revenueByLeadSource || {}).map(([name, value]) => ({
      name, value,
    }));

    const forecastData = [
      { name: 'Pipeline', value: d.forecastPipeline || 0 },
      { name: 'Best Case', value: d.forecastBestCase || 0 },
      { name: 'Commit', value: d.forecastCommit || 0 },
      { name: 'Closed', value: d.forecastClosed || 0 },
    ].filter((f) => f.value > 0);

    const convRates = conversion ? Object.values(conversion.conversionRates || {}).map((r) => ({
      stage: r.fromStage.replace(/_/g, ' '),
      rate: Math.round(r.conversionPct * 10) / 10,
      count: r.transitioned,
    })) : [];

    return [
    {
      id: 'pipeline-by-stage',
      content: (
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={12} md={7}>
            <ChartWidget title="Pipeline by Stage" subtitle="Revenue per pipeline stage" height={170}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={pipelineStageData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                  <XAxis dataKey="stage" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => fmt(v)} />
                  <Tooltip formatter={(v: number) => `$${v.toLocaleString()}`} />
                  <Bar dataKey="value" fill="#7c3aed" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartWidget>
          </Grid>
          <Grid item xs={12} md={5}>
            <ChartWidget title="Revenue by Source" subtitle="Lead source distribution" height={170}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={leadSourceData}
                    cx="50%"
                    cy="50%"
                    innerRadius={28}
                    outerRadius={50}
                    paddingAngle={4}
                    dataKey="value"
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  >
                    {leadSourceData.map((_, idx) => (
                      <Cell key={idx} fill={PIE_COLORS[idx % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v: number) => `$${v.toLocaleString()}`} />
                </PieChart>
              </ResponsiveContainer>
            </ChartWidget>
          </Grid>
        </Grid>
      ),
    },
    {
      id: 'forecast-conversion',
      content: (
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={12} md={6}>
            <ChartWidget title="Forecast Categories" subtitle="Pipeline forecast breakdown" height={160}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={forecastData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                  <XAxis type="number" tickFormatter={(v) => fmt(v)} />
                  <YAxis type="category" dataKey="name" width={80} tick={{ fontSize: 12 }} />
                  <Tooltip formatter={(v: number) => `$${v.toLocaleString()}`} />
                  <Bar dataKey="value" fill="#059669" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartWidget>
          </Grid>
          <Grid item xs={12} md={6}>
            <ChartWidget title="Stage Conversion Rates" subtitle="Percentage moving to next stage" height={160}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={convRates}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                  <XAxis dataKey="stage" tick={{ fontSize: 10 }} />
                  <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => `${v}%`} />
                  <Tooltip formatter={(v: number) => `${v}%`} />
                  <Bar dataKey="rate" fill="#1976d2" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartWidget>
          </Grid>
        </Grid>
      ),
    },
    {
      id: 'summary-cards',
      content: (
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight={600} gutterBottom>Pipeline Summary</Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Weighted Pipeline</Typography>
                    <Typography variant="body2" fontWeight={600}>{fmt(d.weightedPipeline)}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Avg Deal Size</Typography>
                    <Typography variant="body2" fontWeight={600}>{fmt(d.avgDealSize)}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Won Deals</Typography>
                    <Typography variant="body2" fontWeight={600}>{d.totalClosedWon}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Lost Deals</Typography>
                    <Typography variant="body2" fontWeight={600}>{d.totalClosedLost}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Total Revenue</Typography>
                    <Typography variant="body2" fontWeight={600}>{fmt(d.totalRevenue)}</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight={600} gutterBottom>Alerts</Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="body2">Overdue Deals</Typography>
                    <Chip label={d.overdueDeals} size="small" color={d.overdueDeals > 0 ? 'error' : 'success'} variant="outlined" />
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="body2">Closing Soon (7d)</Typography>
                    <Chip label={d.closingSoonDeals} size="small" color={d.closingSoonDeals > 0 ? 'warning' : 'success'} variant="outlined" />
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="body2">Stale Deals (30d+)</Typography>
                    <Chip label={d.staleDeals} size="small" color={d.staleDeals > 0 ? 'warning' : 'success'} variant="outlined" />
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="body2">Active Reminders</Typography>
                    <Chip label={d.activeReminders} size="small" color="info" variant="outlined" />
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight={600} gutterBottom>
                  Sales Quota {quotas.length > 0 ? '' : '(No quotas set)'}
                </Typography>
                {quotas.length > 0 ? (
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    {quotas.slice(0, 5).map((q) => (
                      <Box key={q.id} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="body2" noWrap sx={{ maxWidth: 120 }}>{q.userId}</Typography>
                        <Chip
                          label={`${q.attainmentPct}%`} size="small"
                          color={q.attainmentPct >= 100 ? 'success' : q.attainmentPct >= 75 ? 'warning' : 'error'}
                          variant="outlined"
                        />
                      </Box>
                    ))}
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    Set up sales quotas for rep tracking.
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      ),
    },
  ];
  }, [d, conversion, quotas]);

  if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>;
  if (error) return <Alert severity="error" sx={{ m: 3 }}>{error}</Alert>;
  if (!d) return null;

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
        <PageHeader title="Pipeline Dashboard" breadcrumbs={[{ label: 'Dashboard' }]} />
        <AutoRefreshControl onRefresh={load} loading={loading} />
      </Box>

      {/* KPI Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard title="Pipeline Value" value={fmt(d.totalPipelineValue)} icon={<MoneyIcon />} color="#059669" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard title="Open Deals" value={d.totalOpenDeals} icon={<RevenueIcon />} color="#1976d2" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard title="Win Rate" value={`${d.winRate}%`} icon={<VelocityIcon />} color="#7c3aed" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard title="Alerts" value={totalAlerts} icon={<AlertIcon />} color={totalAlerts > 0 ? '#dc2626' : '#059669'} />
        </Grid>
      </Grid>

      {/* AI Quick Actions */}
      <Typography variant="h6" fontWeight={700} sx={{ mb: 1.5, display: 'flex', alignItems: 'center', gap: 1 }}>
        <AiIcon sx={{ color: '#7c3aed' }} /> AI Quick Actions
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {aiQuickActions.map((a) => (
          <Grid item xs={6} sm={4} md={2} key={a.label}>
            <Card
              sx={{
                cursor: 'pointer',
                textAlign: 'center',
                py: 2,
                px: 1,
                transition: 'all .2s',
                border: '1px solid',
                borderColor: 'divider',
                '&:hover': { borderColor: a.color, transform: 'translateY(-2px)', boxShadow: 3 },
              }}
              onClick={() => navigate(a.path)}
            >
              <a.icon sx={{ fontSize: 32, color: a.color, mb: 0.5 }} />
              <Typography variant="subtitle2" fontWeight={700}>{a.label}</Typography>
              <Typography variant="caption" color="text.secondary" sx={{ lineHeight: 1.2 }}>{a.desc}</Typography>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Draggable widget area */}
      <DraggableWidgetGrid widgets={dashboardWidgets} />
    </>
  );
};

export default DashboardPage;
