/* ============================================================
   DataHealthPage – Data Decay Scan & Health Dashboard
   Flags stale/incomplete contact records, shows health score,
   identifies duplicates, and recommends cleanup actions.
   ============================================================ */
import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Grid, Paper, Card, CardContent, Button, Chip, Stack,
  Table, TableHead, TableBody, TableRow, TableCell, LinearProgress,
  Select, MenuItem, FormControl, InputLabel, Alert, IconButton,
  Tooltip, CircularProgress,
} from '@mui/material';
import {
  HealthAndSafety as HealthIcon,
  Warning as WarningIcon,
  CheckCircle as GoodIcon,
  Error as ErrorIcon,
  Refresh as RefreshIcon,
  OpenInNew as OpenIcon,
  Email as EmailIcon,
  Phone as PhoneIcon,
  People as DuplicateIcon,
  Schedule as StaleIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { contactService } from '../services/contactService';
import { PageHeader } from '../components';

interface StaleRecord {
  id: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  updatedAt: string;
  daysSinceUpdate: number;
}

interface Issue {
  category: string;
  description: string;
  count: number;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
}

interface DataHealth {
  totalContacts: number;
  staleContacts: number;
  missingEmail: number;
  missingPhone: number;
  duplicateGroups: number;
  healthScore: number;
  staleRecords: StaleRecord[];
  issues: Issue[];
}

const severityColor = (s: string) => s === 'HIGH' ? 'error' : s === 'MEDIUM' ? 'warning' : 'info';
const scoreColor = (score: number) => score >= 80 ? 'success.main' : score >= 60 ? 'warning.main' : 'error.main';

const DataHealthPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [staleDays, setStaleDays] = useState(30);
  const [data, setData] = useState<DataHealth | null>(null);
  const [error, setError] = useState('');

  const fetchHealth = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await contactService.getDataHealth(staleDays);
      setData(res.data);
    } catch (err: any) {
      setError('Failed to load data health report. Ensure you have ADMIN or MANAGER role.');
      // Generate mock data for demo
      setData({
        totalContacts: 0, staleContacts: 0, missingEmail: 0, missingPhone: 0,
        duplicateGroups: 0, healthScore: 0, staleRecords: [], issues: [],
      });
    } finally {
      setLoading(false);
    }
  }, [staleDays]);

  useEffect(() => { fetchHealth(); }, [fetchHealth]);

  return (
    <Box>
      <PageHeader
        title="Data Health & Decay Scan"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Data Health' }]}
      />

      {/* Controls */}
      <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Stale Threshold</InputLabel>
          <Select value={staleDays} label="Stale Threshold" onChange={(e) => setStaleDays(e.target.value as number)}>
            <MenuItem value={7}>7 days</MenuItem>
            <MenuItem value={14}>14 days</MenuItem>
            <MenuItem value={30}>30 days</MenuItem>
            <MenuItem value={60}>60 days</MenuItem>
            <MenuItem value={90}>90 days</MenuItem>
          </Select>
        </FormControl>
        <Button variant="outlined" startIcon={<RefreshIcon />} onClick={fetchHealth} disabled={loading}>
          {loading ? 'Scanning…' : 'Run Scan'}
        </Button>
      </Stack>

      {error && <Alert severity="warning" sx={{ mb: 2 }}>{error}</Alert>}
      {loading && <LinearProgress sx={{ mb: 2 }} />}

      {data && (
        <>
          {/* Score + Summary Cards */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
            {/* Health Score */}
            <Grid item xs={12} md={3}>
              <Paper sx={{ p: 3, textAlign: 'center', height: '100%' }}>
                <Box sx={{ position: 'relative', display: 'inline-flex', mb: 1 }}>
                  <CircularProgress
                    variant="determinate"
                    value={data.healthScore}
                    size={100}
                    thickness={6}
                    sx={{ color: scoreColor(data.healthScore) }}
                  />
                  <Box sx={{ position: 'absolute', top: 0, left: 0, bottom: 0, right: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Typography variant="h4" fontWeight={700} color={scoreColor(data.healthScore)}>
                      {data.healthScore}
                    </Typography>
                  </Box>
                </Box>
                <Typography variant="h6" fontWeight={600}>Health Score</Typography>
                <Typography variant="body2" color="text.secondary">
                  {data.healthScore >= 80 ? 'Good' : data.healthScore >= 60 ? 'Needs Attention' : 'Critical'}
                </Typography>
              </Paper>
            </Grid>
            {/* Metric Cards */}
            <Grid item xs={6} sm={3} md={2.25}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent sx={{ textAlign: 'center' }}>
                  <StaleIcon sx={{ fontSize: 32, color: 'warning.main', mb: 0.5 }} />
                  <Typography variant="h5" fontWeight={700}>{data.staleContacts}</Typography>
                  <Typography variant="caption" color="text.secondary">Stale Records</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6} sm={3} md={2.25}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent sx={{ textAlign: 'center' }}>
                  <EmailIcon sx={{ fontSize: 32, color: 'error.main', mb: 0.5 }} />
                  <Typography variant="h5" fontWeight={700}>{data.missingEmail}</Typography>
                  <Typography variant="caption" color="text.secondary">Missing Email</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6} sm={3} md={2.25}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent sx={{ textAlign: 'center' }}>
                  <PhoneIcon sx={{ fontSize: 32, color: 'info.main', mb: 0.5 }} />
                  <Typography variant="h5" fontWeight={700}>{data.missingPhone}</Typography>
                  <Typography variant="caption" color="text.secondary">Missing Phone</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6} sm={3} md={2.25}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent sx={{ textAlign: 'center' }}>
                  <DuplicateIcon sx={{ fontSize: 32, color: 'secondary.main', mb: 0.5 }} />
                  <Typography variant="h5" fontWeight={700}>{data.duplicateGroups}</Typography>
                  <Typography variant="caption" color="text.secondary">Duplicate Groups</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Issues Breakdown */}
          {data.issues.length > 0 && (
            <Paper sx={{ p: 3, mb: 3 }}>
              <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>
                <WarningIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
                Issues Found
              </Typography>
              <Stack spacing={1.5}>
                {data.issues.map((issue, i) => (
                  <Card key={i} variant="outlined">
                    <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                      <Stack direction="row" justifyContent="space-between" alignItems="center">
                        <Stack direction="row" spacing={1.5} alignItems="center">
                          <Chip label={issue.severity} color={severityColor(issue.severity)} size="small" />
                          <Box>
                            <Typography variant="subtitle2" fontWeight={600}>{issue.category}</Typography>
                            <Typography variant="body2" color="text.secondary">{issue.description}</Typography>
                          </Box>
                        </Stack>
                        <Chip label={`${issue.count} records`} variant="outlined" />
                      </Stack>
                    </CardContent>
                  </Card>
                ))}
              </Stack>
            </Paper>
          )}

          {data.issues.length === 0 && !loading && (
            <Alert severity="success" icon={<GoodIcon />} sx={{ mb: 3 }}>
              No data quality issues found. Your contact data is in great shape!
            </Alert>
          )}

          {/* Stale Records Table */}
          {data.staleRecords.length > 0 && (
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>
                <StaleIcon sx={{ verticalAlign: 'middle', mr: 1, color: 'warning.main' }} />
                Stale Contacts (Not updated in {staleDays}+ days)
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Name</strong></TableCell>
                    <TableCell><strong>Email</strong></TableCell>
                    <TableCell><strong>Phone</strong></TableCell>
                    <TableCell><strong>Last Updated</strong></TableCell>
                    <TableCell align="right"><strong>Days Stale</strong></TableCell>
                    <TableCell align="center"><strong>Action</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.staleRecords.map((rec) => (
                    <TableRow key={rec.id} hover>
                      <TableCell>{rec.firstName} {rec.lastName}</TableCell>
                      <TableCell>
                        {rec.email || <Chip label="Missing" size="small" color="error" variant="outlined" />}
                      </TableCell>
                      <TableCell>
                        {rec.phone || <Chip label="Missing" size="small" color="warning" variant="outlined" />}
                      </TableCell>
                      <TableCell>{new Date(rec.updatedAt).toLocaleDateString()}</TableCell>
                      <TableCell align="right">
                        <Chip
                          label={`${rec.daysSinceUpdate}d`}
                          size="small"
                          color={rec.daysSinceUpdate > 90 ? 'error' : rec.daysSinceUpdate > 60 ? 'warning' : 'default'}
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Tooltip title="Open contact">
                          <IconButton size="small" onClick={() => navigate(`/contacts/${rec.id}`)}>
                            <OpenIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>
          )}
        </>
      )}
    </Box>
  );
};

export default DataHealthPage;
