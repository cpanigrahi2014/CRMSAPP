/* ============================================================
   ReportsPage – analytics & reporting dashboard (live API data)
   ============================================================ */
import React, { useState, useEffect, useCallback } from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
  Alert,
  LinearProgress,
} from '@mui/material';
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
import {
  PageHeader,
  MetricCard,
  ChartWidget,
  CustomReportBuilder,
  ScheduledReports,
  ExportMenu,
  AutoRefreshControl,
} from '../components';
import type { ExportableData } from '../components/ExportMenu';
import { opportunityService } from '../services';
import {
  EmojiEvents,
  Speed,
  AttachMoney,
  Timeline,
  TrendingUp,
  SwapHoriz,
  CalendarMonth,
  AccountBalance,
  Inventory,
  BarChart as BarChartIcon,
} from '@mui/icons-material';
import type {
  WinLossAnalysis,
  RevenueAnalytics,
  StageConversionAnalytics,
  PipelinePerformance,
} from '../types';

/* ---- helpers ---- */
const fmtCurrency = (v: number) => `$${v.toLocaleString(undefined, { maximumFractionDigits: 0 })}`;
const pct = (v: number) => `${v.toFixed(1)}%`;

const PIE_COLORS = ['#059669', '#dc2626'];
const STAGE_COLORS = ['#1976d2', '#7c3aed', '#0891b2', '#d97706', '#059669', '#dc2626', '#6366f1', '#ec4899'];

interface TabPanelProps { children: React.ReactNode; value: number; index: number; }
const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) =>
  value === index ? <Box sx={{ pt: 3 }}>{children}</Box> : null;

const ReportsPage: React.FC = () => {
  const [tab, setTab] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [winLoss, setWinLoss] = useState<WinLossAnalysis | null>(null);
  const [revenue, setRevenue] = useState<RevenueAnalytics | null>(null);
  const [conversion, setConversion] = useState<StageConversionAnalytics | null>(null);
  const [performance, setPerformance] = useState<PipelinePerformance | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [wlRes, revRes, convRes, perfRes] = await Promise.all([
        opportunityService.getWinLossAnalysis(),
        opportunityService.getRevenueAnalytics(),
        opportunityService.getConversionAnalytics(),
        opportunityService.getPerformance(),
      ]);
      setWinLoss(wlRes.data);
      setRevenue(revRes.data);
      setConversion(convRes.data);
      setPerformance(perfRes.data);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to load reports data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  /* ---- export data builder (must be before early returns to keep hook order stable) ---- */
  const getExportData = useCallback((): ExportableData[] => {
    const datasets: ExportableData[] = [];

    if (winLoss) {
      datasets.push({
        title: 'Win_Loss_Analysis',
        headers: ['Metric', 'Value'],
        rows: [
          ['Total Won', winLoss.totalClosedWon],
          ['Total Lost', winLoss.totalClosedLost],
          ['Win Rate', `${winLoss.winRate}%`],
          ['Won Revenue', winLoss.totalWonRevenue],
          ['Lost Revenue', winLoss.totalLostRevenue],
          ['Avg Won Deal', winLoss.averageWonDealSize],
          ['Avg Lost Deal', winLoss.averageLostDealSize],
          ['Avg Days to Close', winLoss.averageDaysToClose],
        ],
      });
    }

    if (revenue) {
      const stageRows = Object.entries(revenue.revenueByStage || {}).map(([stage, amt]) => [stage, amt] as [string, number]);
      datasets.push({
        title: 'Revenue_By_Stage',
        headers: ['Stage', 'Revenue'],
        rows: stageRows,
      });
      const sourceRows = Object.entries(revenue.revenueByLeadSource || {}).map(([src, amt]) => [src, amt] as [string, number]);
      datasets.push({
        title: 'Revenue_By_Source',
        headers: ['Lead Source', 'Revenue'],
        rows: sourceRows,
      });
    }

    if (conversion) {
      const convRows = Object.entries(conversion.conversionRates || {}).map(([stage, r]) => [
        stage, r.conversionPct, r.transitioned, r.total,
      ] as (string | number)[]);
      datasets.push({
        title: 'Stage_Conversion',
        headers: ['Stage', 'Conversion %', 'Transitioned', 'Total'],
        rows: convRows,
      });
    }

    if (performance?.repPerformances) {
      datasets.push({
        title: 'Rep_Performance',
        headers: ['User', 'Total Deals', 'Won', 'Lost', 'Revenue', 'Avg Deal', 'Win Rate', 'Quota'],
        rows: performance.repPerformances.map((r) => [
          r.userId, r.totalDeals, r.wonDeals, r.lostDeals,
          r.totalRevenue, r.avgDealSize, `${r.winRate}%`, `${r.quotaAttainment}%`,
        ]),
      });
    }

    return datasets;
  }, [winLoss, revenue, conversion, performance]);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress />
      </Box>
    );
  }
  if (error) {
    return <Alert severity="error" sx={{ m: 3 }}>{error}</Alert>;
  }

  /* ---------- derived chart data ---------- */

  // Win / Loss pie
  const winLossPie = winLoss
    ? [
        { name: 'Won', value: winLoss.totalClosedWon },
        { name: 'Lost', value: winLoss.totalClosedLost },
      ]
    : [];

  // Lost reason breakdown bar
  const lostReasons = winLoss
    ? Object.entries(winLoss.lostReasonBreakdown || {}).map(([reason, count]) => ({
        reason: reason || 'Unknown',
        count,
      }))
    : [];

  // Revenue by stage bar
  const revenueByStage = revenue
    ? Object.entries(revenue.revenueByStage || {}).map(([stage, amt]) => ({ stage, amount: amt }))
    : [];

  // Revenue by lead source bar
  const revenueBySource = revenue
    ? Object.entries(revenue.revenueByLeadSource || {}).map(([source, amt]) => ({ source, amount: amt }))
    : [];

  // Stage conversion funnel data
  const conversionRates = conversion
    ? Object.entries(conversion.conversionRates || {}).map(([stage, rate]) => ({
        stage,
        conversionPct: rate.conversionPct,
        transitioned: rate.transitioned,
        total: rate.total,
      }))
    : [];

  // Avg time in stage bar
  const avgTimeData = conversion
    ? Object.entries(conversion.avgTimeInStage || {}).map(([stage, seconds]) => ({
        stage,
        days: Math.round(seconds / 86400 * 10) / 10,
      }))
    : [];

  return (
    <>
      <PageHeader
        title="Reports & Analytics"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Reports' }]}
      />

      {/* Toolbar: Auto-refresh + Export */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <AutoRefreshControl onRefresh={loadData} loading={loading} />
        <ExportMenu getData={getExportData} />
      </Box>

      <Card sx={{ mb: 3 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ px: 2, pt: 1 }} variant="scrollable" scrollButtons="auto">
          <Tab label="Sales Performance" />
          <Tab label="Stage Analytics" />
          <Tab label="Revenue Insights" />
          <Tab label="Custom Reports" />
          <Tab label="Scheduled Reports" />
        </Tabs>
      </Card>

      {/* ═══════ Tab 0: Sales Performance ═══════ */}
      <TabPanel value={tab} index={0}>
        <Grid container spacing={3}>
          {/* KPI cards */}
          <Grid item xs={6} md={3}>
            <MetricCard title="Win Rate" value={pct(winLoss?.winRate ?? 0)} icon={<EmojiEvents />} color="#059669" />
          </Grid>
          <Grid item xs={6} md={3}>
            <MetricCard title="Avg Days to Close" value={`${(winLoss?.averageDaysToClose ?? 0).toFixed(0)}d`} icon={<CalendarMonth />} color="#d97706" />
          </Grid>
          <Grid item xs={6} md={3}>
            <MetricCard title="Pipeline Velocity" value={fmtCurrency(performance?.pipelineVelocity ?? 0)} icon={<Speed />} color="#1976d2" />
          </Grid>
          <Grid item xs={6} md={3}>
            <MetricCard title="Avg Deal Size" value={fmtCurrency(performance?.avgDealSize ?? 0)} icon={<AttachMoney />} color="#7c3aed" />
          </Grid>

          {/* Win/Loss pie */}
          <Grid item xs={12} md={4}>
            <ChartWidget title="Win / Loss Ratio" height={280}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={winLossPie}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={95}
                    paddingAngle={4}
                    dataKey="value"
                    label={({ name, value }) => `${name}: ${value}`}
                  >
                    {winLossPie.map((_, i) => (
                      <Cell key={i} fill={PIE_COLORS[i]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </ChartWidget>
          </Grid>

          {/* Lost reason breakdown */}
          <Grid item xs={12} md={8}>
            <ChartWidget title="Lost Reason Breakdown" height={280}>
              {lostReasons.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={lostReasons} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis type="number" />
                    <YAxis dataKey="reason" type="category" width={120} />
                    <Tooltip />
                    <Bar dataKey="count" fill="#dc2626" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <Box display="flex" alignItems="center" justifyContent="center" height="100%">
                  <Typography color="text.secondary">No lost deals recorded yet</Typography>
                </Box>
              )}
            </ChartWidget>
          </Grid>

          {/* Rep Performance table */}
          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight={600} gutterBottom>
                  Representative Performance
                </Typography>
                {(performance?.repPerformances ?? []).length > 0 ? (
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>User</TableCell>
                          <TableCell align="right">Total Deals</TableCell>
                          <TableCell align="right">Won</TableCell>
                          <TableCell align="right">Lost</TableCell>
                          <TableCell align="right">Revenue</TableCell>
                          <TableCell align="right">Avg Deal</TableCell>
                          <TableCell align="right">Win Rate</TableCell>
                          <TableCell align="right" sx={{ minWidth: 160 }}>Quota Attainment</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {performance!.repPerformances.map((r) => (
                          <TableRow key={r.userId}>
                            <TableCell>{r.userId}</TableCell>
                            <TableCell align="right">{r.totalDeals}</TableCell>
                            <TableCell align="right">{r.wonDeals}</TableCell>
                            <TableCell align="right">{r.lostDeals}</TableCell>
                            <TableCell align="right">{fmtCurrency(r.totalRevenue)}</TableCell>
                            <TableCell align="right">{fmtCurrency(r.avgDealSize)}</TableCell>
                            <TableCell align="right">
                              <Chip
                                label={pct(r.winRate)}
                                size="small"
                                color={r.winRate >= 60 ? 'success' : r.winRate >= 40 ? 'warning' : 'error'}
                                variant="outlined"
                              />
                            </TableCell>
                            <TableCell align="right">
                              <Box display="flex" alignItems="center" gap={1}>
                                <LinearProgress
                                  variant="determinate"
                                  value={Math.min(r.quotaAttainment, 100)}
                                  sx={{ flexGrow: 1, height: 8, borderRadius: 4 }}
                                  color={r.quotaAttainment >= 100 ? 'success' : r.quotaAttainment >= 70 ? 'warning' : 'error'}
                                />
                                <Typography variant="caption" sx={{ minWidth: 40 }}>
                                  {pct(r.quotaAttainment)}
                                </Typography>
                              </Box>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                ) : (
                  <Typography color="text.secondary">No rep performance data yet</Typography>
                )}
              </CardContent>
            </Card>
          </Grid>

          {/* Summary cards */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight={600} gutterBottom>Win/Loss Summary</Typography>
                <Box display="flex" flexDirection="column" gap={1}>
                  <Typography>Won Revenue: <strong>{fmtCurrency(winLoss?.totalWonRevenue ?? 0)}</strong></Typography>
                  <Typography>Lost Revenue: <strong>{fmtCurrency(winLoss?.totalLostRevenue ?? 0)}</strong></Typography>
                  <Typography>Avg Won Deal: <strong>{fmtCurrency(winLoss?.averageWonDealSize ?? 0)}</strong></Typography>
                  <Typography>Avg Lost Deal: <strong>{fmtCurrency(winLoss?.averageLostDealSize ?? 0)}</strong></Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight={600} gutterBottom>Velocity Metrics</Typography>
                <Box display="flex" flexDirection="column" gap={1}>
                  <Typography>Pipeline Velocity: <strong>{fmtCurrency(performance?.pipelineVelocity ?? 0)}</strong> / day</Typography>
                  <Typography>Avg Cycle: <strong>{(performance?.avgCycleDays ?? 0).toFixed(0)} days</strong></Typography>
                  <Typography>Total Deals: <strong>{performance?.totalDeals ?? 0}</strong></Typography>
                  <Typography>Win Rate: <strong>{pct(performance?.winRate ?? 0)}</strong></Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ═══════ Tab 1: Stage Analytics ═══════ */}
      <TabPanel value={tab} index={1}>
        <Grid container spacing={3}>
          {/* Overall conversion rate */}
          <Grid item xs={12} md={4}>
            <MetricCard
              title="Overall Conversion"
              value={pct(conversion?.overallConversionRate ?? 0)}
              icon={<TrendingUp />}
              color="#059669"
            />
          </Grid>
          <Grid item xs={12} md={4}>
            <MetricCard title="Total Deals" value={String(performance?.totalDeals ?? 0)} icon={<Inventory />} color="#1976d2" />
          </Grid>
          <Grid item xs={12} md={4}>
            <MetricCard title="Avg Cycle Days" value={`${(performance?.avgCycleDays ?? 0).toFixed(0)}d`} icon={<Timeline />} color="#d97706" />
          </Grid>

          {/* Conversion rates per stage */}
          <Grid item xs={12} md={6}>
            <ChartWidget title="Stage Conversion Rates (%)" height={300}>
              {conversionRates.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={conversionRates}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="stage" />
                    <YAxis domain={[0, 100]} tickFormatter={(v) => `${v}%`} />
                    <Tooltip formatter={(v: number) => `${v.toFixed(1)}%`} />
                    <Bar dataKey="conversionPct" fill="#1976d2" radius={[4, 4, 0, 0]}>
                      {conversionRates.map((_, i) => (
                        <Cell key={i} fill={STAGE_COLORS[i % STAGE_COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <Box display="flex" alignItems="center" justifyContent="center" height="100%">
                  <Typography color="text.secondary">No stage transition data yet</Typography>
                </Box>
              )}
            </ChartWidget>
          </Grid>

          {/* Avg time in stage */}
          <Grid item xs={12} md={6}>
            <ChartWidget title="Average Time in Stage (days)" height={300}>
              {avgTimeData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={avgTimeData} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis type="number" />
                    <YAxis dataKey="stage" type="category" width={120} />
                    <Tooltip formatter={(v: number) => `${v} days`} />
                    <Bar dataKey="days" fill="#7c3aed" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <Box display="flex" alignItems="center" justifyContent="center" height="100%">
                  <Typography color="text.secondary">No time-in-stage data yet</Typography>
                </Box>
              )}
            </ChartWidget>
          </Grid>

          {/* Stage transitions table */}
          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight={600} gutterBottom>Stage Transitions</Typography>
                {(conversion?.transitions ?? []).length > 0 ? (
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>From Stage</TableCell>
                          <TableCell>To Stage</TableCell>
                          <TableCell align="right">Count</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {conversion!.transitions.map((t, i) => (
                          <TableRow key={i}>
                            <TableCell>
                              <Chip label={t.fromStage} size="small" variant="outlined" />
                            </TableCell>
                            <TableCell>
                              <Chip label={t.toStage} size="small" color="primary" variant="outlined" />
                            </TableCell>
                            <TableCell align="right">{t.count}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                ) : (
                  <Typography color="text.secondary">No stage transitions recorded yet</Typography>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ═══════ Tab 2: Revenue Insights ═══════ */}
      <TabPanel value={tab} index={2}>
        <Grid container spacing={3}>
          {/* KPI row */}
          <Grid item xs={6} md={3}>
            <MetricCard title="Total Revenue" value={fmtCurrency(revenue?.totalRevenue ?? 0)} icon={<AttachMoney />} color="#059669" />
          </Grid>
          <Grid item xs={6} md={3}>
            <MetricCard title="Total Pipeline" value={fmtCurrency(revenue?.totalPipeline ?? 0)} icon={<BarChartIcon />} color="#1976d2" />
          </Grid>
          <Grid item xs={6} md={3}>
            <MetricCard title="Weighted Pipeline" value={fmtCurrency(revenue?.totalWeightedPipeline ?? 0)} icon={<AccountBalance />} color="#7c3aed" />
          </Grid>
          <Grid item xs={6} md={3}>
            <MetricCard title="Avg Deal Size" value={fmtCurrency(revenue?.averageDealSize ?? 0)} icon={<SwapHoriz />} color="#d97706" />
          </Grid>

          {/* Revenue by stage */}
          <Grid item xs={12} md={6}>
            <ChartWidget title="Revenue by Stage" height={300}>
              {revenueByStage.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={revenueByStage}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="stage" />
                    <YAxis tickFormatter={(v) => `$${v / 1000}k`} />
                    <Tooltip formatter={(v: number) => fmtCurrency(v)} />
                    <Bar dataKey="amount" fill="#1976d2" radius={[4, 4, 0, 0]}>
                      {revenueByStage.map((_, i) => (
                        <Cell key={i} fill={STAGE_COLORS[i % STAGE_COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <Box display="flex" alignItems="center" justifyContent="center" height="100%">
                  <Typography color="text.secondary">No revenue data yet</Typography>
                </Box>
              )}
            </ChartWidget>
          </Grid>

          {/* Revenue by lead source */}
          <Grid item xs={12} md={6}>
            <ChartWidget title="Revenue by Lead Source" height={300}>
              {revenueBySource.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={revenueBySource} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis type="number" tickFormatter={(v) => `$${v / 1000}k`} />
                    <YAxis dataKey="source" type="category" width={100} />
                    <Tooltip formatter={(v: number) => fmtCurrency(v)} />
                    <Bar dataKey="amount" fill="#059669" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <Box display="flex" alignItems="center" justifyContent="center" height="100%">
                  <Typography color="text.secondary">No lead source data yet</Typography>
                </Box>
              )}
            </ChartWidget>
          </Grid>

          {/* Opportunity counts by stage & overall stats */}
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight={600} gutterBottom>Opportunities by Stage</Typography>
                {revenue && revenue.countByStage ? (
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Stage</TableCell>
                          <TableCell align="right">Count</TableCell>
                          <TableCell align="right">Revenue</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {Object.entries(revenue.countByStage).map(([stage, count]) => (
                          <TableRow key={stage}>
                            <TableCell>
                              <Chip label={stage} size="small" variant="outlined" />
                            </TableCell>
                            <TableCell align="right">{count}</TableCell>
                            <TableCell align="right">{fmtCurrency(revenue.revenueByStage[stage] ?? 0)}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                ) : (
                  <Typography color="text.secondary">No data</Typography>
                )}
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" fontWeight={600} gutterBottom>Overall Statistics</Typography>
                <Box display="flex" flexDirection="column" gap={1.5}>
                  <Typography>Total Opportunities: <strong>{revenue?.totalOpportunities ?? 0}</strong></Typography>
                  <Typography>Open: <strong>{revenue?.openOpportunities ?? 0}</strong></Typography>
                  <Typography>Closed Won: <strong>{revenue?.closedWonOpportunities ?? 0}</strong></Typography>
                  <Typography>Closed Lost: <strong>{revenue?.closedLostOpportunities ?? 0}</strong></Typography>
                  <Typography>Win Rate: <strong>{pct(revenue?.winRate ?? 0)}</strong></Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ═══════ Tab 3: Custom Reports ═══════ */}
      <TabPanel value={tab} index={3}>
        <CustomReportBuilder />
      </TabPanel>

      {/* ═══════ Tab 4: Scheduled Reports ═══════ */}
      <TabPanel value={tab} index={4}>
        <ScheduledReports />
      </TabPanel>
    </>
  );
};

export default ReportsPage;
