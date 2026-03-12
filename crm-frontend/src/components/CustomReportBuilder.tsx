/* ============================================================
   CustomReportBuilder – configurable report with selectable
   metrics, dimensions, chart type, and date filters
   ============================================================ */
import React, { useState, useCallback } from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  CircularProgress,
  Alert,
  Chip,
  Stack,
  TextField,
} from '@mui/material';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { PlayArrow, RestartAlt } from '@mui/icons-material';
import { opportunityService } from '../services';
import type {
  RevenueAnalytics,
  WinLossAnalysis,
  StageConversionAnalytics,
  PipelinePerformance,
  PipelineDashboard,
  ForecastSummary,
} from '../types';

/* ---- constants ---- */
type ReportType = 'revenue' | 'winloss' | 'conversion' | 'performance' | 'pipeline' | 'forecast';
type ChartType = 'bar' | 'pie' | 'line' | 'table';
type Dimension = 'stage' | 'source' | 'rep' | 'reason' | 'category';

interface ReportConfig {
  label: string;
  dimensions: { value: Dimension; label: string }[];
  metrics: string[];
}

const REPORT_CONFIGS: Record<ReportType, ReportConfig> = {
  revenue: {
    label: 'Revenue Analysis',
    dimensions: [
      { value: 'stage', label: 'By Stage' },
      { value: 'source', label: 'By Lead Source' },
    ],
    metrics: ['Revenue', 'Deal Count', 'Avg Deal Size'],
  },
  winloss: {
    label: 'Win/Loss Analysis',
    dimensions: [
      { value: 'reason', label: 'By Lost Reason' },
    ],
    metrics: ['Win Rate', 'Revenue', 'Deal Count'],
  },
  conversion: {
    label: 'Conversion Analytics',
    dimensions: [
      { value: 'stage', label: 'By Stage' },
    ],
    metrics: ['Conversion Rate', 'Avg Time in Stage', 'Transitions'],
  },
  performance: {
    label: 'Sales Performance',
    dimensions: [
      { value: 'rep', label: 'By Representative' },
    ],
    metrics: ['Revenue', 'Win Rate', 'Deal Count', 'Quota Attainment'],
  },
  pipeline: {
    label: 'Pipeline Overview',
    dimensions: [
      { value: 'stage', label: 'By Stage' },
      { value: 'source', label: 'By Lead Source' },
      { value: 'category', label: 'By Forecast Category' },
    ],
    metrics: ['Pipeline Value', 'Deal Count', 'Weighted Value'],
  },
  forecast: {
    label: 'Forecast Report',
    dimensions: [
      { value: 'category', label: 'By Category' },
      { value: 'stage', label: 'By Stage' },
    ],
    metrics: ['Forecast Value', 'Deal Count'],
  },
};

const CHART_COLORS = ['#1976d2', '#7c3aed', '#059669', '#d97706', '#dc2626', '#0891b2', '#8b5cf6', '#ec4899'];
const fmtCurrency = (v: number) => `$${v.toLocaleString(undefined, { maximumFractionDigits: 0 })}`;

interface ChartDataPoint {
  name: string;
  value: number;
  [key: string]: string | number;
}

const CustomReportBuilder: React.FC = () => {
  const [reportType, setReportType] = useState<ReportType>('revenue');
  const [chartType, setChartType] = useState<ChartType>('bar');
  const [dimension, setDimension] = useState<Dimension>('stage');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [reportTitle, setReportTitle] = useState('');
  const [generated, setGenerated] = useState(false);

  const buildChartData = useCallback(
    (type: ReportType, dim: Dimension, data: any): ChartDataPoint[] => {
      switch (type) {
        case 'revenue': {
          const rev = data as RevenueAnalytics;
          if (dim === 'source') {
            return Object.entries(rev.revenueByLeadSource || {}).map(([name, value]) => ({ name, value: value as number }));
          }
          return Object.entries(rev.revenueByStage || {}).map(([name, value]) => ({ name: name.replace(/_/g, ' '), value: value as number }));
        }
        case 'winloss': {
          const wl = data as WinLossAnalysis;
          if (dim === 'reason') {
            return Object.entries(wl.lostReasonBreakdown || {}).map(([name, value]) => ({ name: name || 'Unknown', value: value as number }));
          }
          return [
            { name: 'Won', value: wl.totalClosedWon },
            { name: 'Lost', value: wl.totalClosedLost },
          ];
        }
        case 'conversion': {
          const conv = data as StageConversionAnalytics;
          return Object.entries(conv.conversionRates || {}).map(([name, rate]) => ({
            name: name.replace(/_/g, ' '),
            value: Math.round(rate.conversionPct * 10) / 10,
          }));
        }
        case 'performance': {
          const perf = data as PipelinePerformance;
          return (perf.repPerformances || []).map((r) => ({
            name: r.userId,
            value: r.totalRevenue,
            winRate: r.winRate,
            deals: r.totalDeals,
            quota: r.quotaAttainment,
          }));
        }
        case 'pipeline': {
          const dash = data as PipelineDashboard;
          if (dim === 'source') {
            return Object.entries(dash.revenueByLeadSource || {}).map(([name, value]) => ({ name, value: value as number }));
          }
          if (dim === 'category') {
            return [
              { name: 'Pipeline', value: dash.forecastPipeline || 0 },
              { name: 'Best Case', value: dash.forecastBestCase || 0 },
              { name: 'Commit', value: dash.forecastCommit || 0 },
              { name: 'Closed', value: dash.forecastClosed || 0 },
            ].filter((d) => d.value > 0);
          }
          return (dash.stageBreakdown || []).map((s) => ({
            name: s.stage.replace(/_/g, ' '),
            value: s.totalAmount,
            count: s.count,
          }));
        }
        case 'forecast': {
          const fc = data as ForecastSummary;
          if (dim === 'stage') {
            return (fc.byStage || []).map((s) => ({
              name: s.stage.replace(/_/g, ' '),
              value: s.totalAmount,
              count: s.count,
            }));
          }
          return [
            { name: 'Pipeline', value: fc.pipelineValue },
            { name: 'Best Case', value: fc.bestCaseValue },
            { name: 'Commit', value: fc.commitValue },
            { name: 'Closed', value: fc.closedValue },
            { name: 'Weighted', value: fc.weightedPipeline },
          ].filter((d) => d.value > 0);
        }
        default:
          return [];
      }
    },
    [],
  );

  const fetchData = useCallback(
    async (type: ReportType): Promise<any> => {
      switch (type) {
        case 'revenue':
          return (await opportunityService.getRevenueAnalytics()).data;
        case 'winloss':
          return (await opportunityService.getWinLossAnalysis()).data;
        case 'conversion':
          return (await opportunityService.getConversionAnalytics()).data;
        case 'performance':
          return (await opportunityService.getPerformance()).data;
        case 'pipeline':
          return (await opportunityService.getDashboard()).data;
        case 'forecast':
          return (await opportunityService.getForecast()).data;
      }
    },
    [],
  );

  const handleGenerate = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchData(reportType);
      const points = buildChartData(reportType, dimension, data);
      setChartData(points);
      setReportTitle(
        `${REPORT_CONFIGS[reportType].label} – ${REPORT_CONFIGS[reportType].dimensions.find((d) => d.value === dimension)?.label || dimension}`,
      );
      setGenerated(true);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to generate report');
    } finally {
      setLoading(false);
    }
  }, [reportType, dimension, buildChartData, fetchData]);

  const handleReset = () => {
    setChartData([]);
    setGenerated(false);
    setError(null);
    setReportTitle('');
  };

  // Update dimension when report type changes
  const handleReportTypeChange = (type: ReportType) => {
    setReportType(type);
    setDimension(REPORT_CONFIGS[type].dimensions[0].value);
    setGenerated(false);
  };

  /* ---- Render chart ---- */
  const renderChart = () => {
    if (chartData.length === 0) {
      return (
        <Box display="flex" alignItems="center" justifyContent="center" height={300}>
          <Typography color="text.secondary">No data available for this selection</Typography>
        </Box>
      );
    }

    switch (chartType) {
      case 'bar':
        return (
          <ResponsiveContainer width="100%" height={350}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" tick={{ fontSize: 11 }} />
              <YAxis tickFormatter={(v) => (v >= 1000 ? `$${v / 1000}k` : String(v))} />
              <Tooltip formatter={(v: number) => (v >= 100 ? fmtCurrency(v) : v)} />
              <Legend />
              <Bar dataKey="value" fill="#1976d2" radius={[4, 4, 0, 0]}>
                {chartData.map((_, i) => (
                  <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        );

      case 'pie':
        return (
          <ResponsiveContainer width="100%" height={350}>
            <PieChart>
              <Pie
                data={chartData}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={120}
                paddingAngle={4}
                dataKey="value"
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              >
                {chartData.map((_, i) => (
                  <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip formatter={(v: number) => (v >= 100 ? fmtCurrency(v) : v)} />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        );

      case 'line':
        return (
          <ResponsiveContainer width="100%" height={350}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" tick={{ fontSize: 11 }} />
              <YAxis tickFormatter={(v) => (v >= 1000 ? `$${v / 1000}k` : String(v))} />
              <Tooltip formatter={(v: number) => (v >= 100 ? fmtCurrency(v) : v)} />
              <Legend />
              <Line type="monotone" dataKey="value" stroke="#1976d2" strokeWidth={2} dot={{ r: 5 }} />
            </LineChart>
          </ResponsiveContainer>
        );

      case 'table':
        return (
          <Box sx={{ maxHeight: 400, overflow: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e0e0e0' }}>
                  <th style={{ textAlign: 'left', padding: '8px 12px' }}>Name</th>
                  <th style={{ textAlign: 'right', padding: '8px 12px' }}>Value</th>
                </tr>
              </thead>
              <tbody>
                {chartData.map((d, i) => (
                  <tr key={i} style={{ borderBottom: '1px solid #f0f0f0' }}>
                    <td style={{ padding: '8px 12px' }}>{d.name}</td>
                    <td style={{ textAlign: 'right', padding: '8px 12px', fontWeight: 600 }}>
                      {d.value >= 100 ? fmtCurrency(d.value) : d.value}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Box>
        );
    }
  };

  const availableDimensions = REPORT_CONFIGS[reportType].dimensions;

  return (
    <Grid container spacing={3}>
      {/* Configuration Panel */}
      <Grid item xs={12}>
        <Card>
          <CardContent>
            <Typography variant="h6" fontWeight={600} gutterBottom>
              Build Your Report
            </Typography>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={3}>
                <FormControl fullWidth size="small">
                  <InputLabel>Report Type</InputLabel>
                  <Select
                    value={reportType}
                    label="Report Type"
                    onChange={(e) => handleReportTypeChange(e.target.value as ReportType)}
                  >
                    {Object.entries(REPORT_CONFIGS).map(([key, cfg]) => (
                      <MenuItem key={key} value={key}>{cfg.label}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={3}>
                <FormControl fullWidth size="small">
                  <InputLabel>Dimension</InputLabel>
                  <Select
                    value={dimension}
                    label="Dimension"
                    onChange={(e) => { setDimension(e.target.value as Dimension); setGenerated(false); }}
                  >
                    {availableDimensions.map((d) => (
                      <MenuItem key={d.value} value={d.value}>{d.label}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={3}>
                <FormControl fullWidth size="small">
                  <InputLabel>Chart Type</InputLabel>
                  <Select
                    value={chartType}
                    label="Chart Type"
                    onChange={(e) => setChartType(e.target.value as ChartType)}
                  >
                    <MenuItem value="bar">Bar Chart</MenuItem>
                    <MenuItem value="pie">Pie Chart</MenuItem>
                    <MenuItem value="line">Line Chart</MenuItem>
                    <MenuItem value="table">Data Table</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={3}>
                <Stack direction="row" spacing={1}>
                  <Button
                    variant="contained"
                    startIcon={loading ? <CircularProgress size={18} color="inherit" /> : <PlayArrow />}
                    onClick={handleGenerate}
                    disabled={loading}
                    fullWidth
                  >
                    Generate
                  </Button>
                  {generated && (
                    <Button variant="outlined" startIcon={<RestartAlt />} onClick={handleReset}>
                      Reset
                    </Button>
                  )}
                </Stack>
              </Grid>
            </Grid>
            {/* Available metrics hint */}
            <Box sx={{ mt: 2 }}>
              <Typography variant="caption" color="text.secondary">
                Available metrics:{' '}
                {REPORT_CONFIGS[reportType].metrics.map((m) => (
                  <Chip key={m} label={m} size="small" variant="outlined" sx={{ mr: 0.5, mb: 0.5 }} />
                ))}
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>

      {/* Error */}
      {error && (
        <Grid item xs={12}>
          <Alert severity="error">{error}</Alert>
        </Grid>
      )}

      {/* Generated Report */}
      {generated && (
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>
                {reportTitle}
              </Typography>
              {renderChart()}
            </CardContent>
          </Card>
        </Grid>
      )}
    </Grid>
  );
};

export default CustomReportBuilder;
