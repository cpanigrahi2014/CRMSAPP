/* ============================================================
   CampaignsPage – Marketing campaign management + analytics
   ============================================================ */
import React, { useState, useMemo, useEffect, useCallback } from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  TextField,
  MenuItem,
  IconButton,
  Tooltip,
  Stack,
  LinearProgress,
} from '@mui/material';
import { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import { Edit as EditIcon, Delete as DeleteIcon, Visibility as ViewIcon } from '@mui/icons-material';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
} from 'recharts';
import { DataTable, PageHeader, StatusChip, ChartWidget, MetricCard, ConfirmDialog, ModalForm } from '../components';
import { Campaign } from '../types';
import { useSnackbar } from 'notistack';
import { Campaign as CampaignIcon, TrendingUp, AttachMoney, People } from '@mui/icons-material';
import { campaignService } from '../services/campaignService';

const statusOptions = ['PLANNED', 'ACTIVE', 'COMPLETED', 'ABORTED'];
const typeOptions = ['EMAIL', 'SOCIAL', 'WEBINAR', 'EVENT', 'PAID_ADS', 'CONTENT'];

/* ---- fallback mock data (used when API has no data) ---- */
const mockCampaigns: Campaign[] = [
  { id: '1', name: 'Q1 Email Blast', type: 'EMAIL', status: 'COMPLETED', startDate: '2024-01-01', endDate: '2024-01-31', budget: 5000, actualCost: 4200, expectedRevenue: 25000, leads: 120, conversions: 18, description: '' },
  { id: '2', name: 'Product Launch Webinar', type: 'WEBINAR', status: 'ACTIVE', startDate: '2024-02-15', endDate: '2024-03-15', budget: 8000, actualCost: 3500, expectedRevenue: 40000, leads: 85, conversions: 12, description: '' },
  { id: '3', name: 'LinkedIn Ads Campaign', type: 'PAID_ADS', status: 'ACTIVE', startDate: '2024-02-01', endDate: '2024-04-30', budget: 15000, actualCost: 7200, expectedRevenue: 60000, leads: 210, conversions: 32, description: '' },
  { id: '4', name: 'Industry Conference Booth', type: 'EVENT', status: 'PLANNED', startDate: '2024-05-10', endDate: '2024-05-12', budget: 20000, actualCost: 0, expectedRevenue: 80000, leads: 0, conversions: 0, description: '' },
  { id: '5', name: 'Content Marketing Blog Series', type: 'CONTENT', status: 'ACTIVE', startDate: '2024-01-15', endDate: '2024-06-30', budget: 3000, actualCost: 1800, expectedRevenue: 15000, leads: 65, conversions: 8, description: '' },
];

const emptyItem = {
  name: '',
  type: 'EMAIL',
  status: 'PLANNED',
  startDate: '',
  endDate: '',
  budget: 0,
  description: '',
};

const CampaignsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [formData, setFormData] = useState(emptyItem);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const loadCampaigns = useCallback(async () => {
    setLoading(true);
    try {
      const res = await campaignService.getAll(0, 100);
      const data = res?.data;
      const items = data?.content ?? data ?? [];
      setCampaigns(Array.isArray(items) && items.length > 0 ? items : mockCampaigns);
    } catch {
      setCampaigns(mockCampaigns);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadCampaigns(); }, [loadCampaigns]);

  const campaignPerformance = useMemo(() =>
    campaigns.filter(c => (c.leads ?? 0) > 0 || (c.conversions ?? 0) > 0).slice(0, 6).map(c => ({
      name: c.name.length > 15 ? c.name.slice(0, 15) + '…' : c.name,
      leads: c.leads ?? c.leadsGenerated ?? 0,
      conversions: c.conversions ?? 0,
      revenue: c.expectedRevenue ?? 0,
    })),
  [campaigns]);

  const totals = useMemo(() => ({
    totalBudget: campaigns.reduce((s, c) => s + (c.budget ?? 0), 0),
    totalLeads: campaigns.reduce((s, c) => s + (c.leads ?? 0), 0),
    totalConversions: campaigns.reduce((s, c) => s + (c.conversions ?? 0), 0),
    active: campaigns.filter((c) => c.status === 'ACTIVE').length,
  }), [campaigns]);

  const openCreate = () => { setEditingId(null); setFormData(emptyItem); setFormOpen(true); };
  const openEdit = (c: Campaign) => {
    setEditingId(c.id);
    setFormData({
      name: c.name,
      type: c.type ?? 'EMAIL',
      status: c.status,
      startDate: c.startDate ?? '',
      endDate: c.endDate ?? '',
      budget: c.budget ?? 0,
      description: c.description ?? '',
    });
    setFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingId) {
        await campaignService.update(editingId, formData as any);
        enqueueSnackbar('Campaign updated', { variant: 'success' });
      } else {
        await campaignService.create(formData as any);
        enqueueSnackbar('Campaign created', { variant: 'success' });
      }
      setFormOpen(false);
      loadCampaigns();
    } catch {
      // Fallback to local state if API fails
      if (editingId) {
        setCampaigns((prev) => prev.map((c) => (c.id === editingId ? { ...c, ...formData } as Campaign : c)));
        enqueueSnackbar('Campaign updated (local)', { variant: 'success' });
      } else {
        setCampaigns((prev) => [...prev, { id: String(Date.now()), ...formData, leads: 0, conversions: 0, actualCost: 0, expectedRevenue: 0 } as Campaign]);
        enqueueSnackbar('Campaign created (local)', { variant: 'success' });
      }
      setFormOpen(false);
    }
  };

  const handleDelete = async () => {
    try {
      if (deleteId) await campaignService.delete(deleteId);
      enqueueSnackbar('Campaign deleted', { variant: 'success' });
      setDeleteId(null);
      loadCampaigns();
    } catch {
      setCampaigns((prev) => prev.filter((c) => c.id !== deleteId));
      enqueueSnackbar('Campaign deleted (local)', { variant: 'success' });
      setDeleteId(null);
    }
  };

  const handleChange = (f: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFormData((p) => ({ ...p, [f]: e.target.value }));

  const columns = useMemo<GridColDef[]>(() => [
    { field: 'name', headerName: 'Campaign', flex: 1.5, minWidth: 200 },
    { field: 'type', headerName: 'Type', width: 110 },
    {
      field: 'status',
      headerName: 'Status',
      width: 120,
      renderCell: (p: GridRenderCellParams) => <StatusChip status={p.value} />,
    },
    {
      field: 'budget',
      headerName: 'Budget',
      width: 120,
      valueFormatter: (p: any) => `$${Number(p.value ?? 0).toLocaleString()}`,
    },
    { field: 'leads', headerName: 'Leads', width: 90, type: 'number' },
    { field: 'conversions', headerName: 'Conv.', width: 90, type: 'number' },
    {
      field: 'roi',
      headerName: 'ROI',
      width: 100,
      renderCell: (p: GridRenderCellParams<Campaign>) => {
        const c = p.row;
        const cost = c.actualCost || 1;
        const rev = c.expectedRevenue ?? 0;
        const roi = ((rev - cost) / cost * 100).toFixed(0);
        return (
          <Box sx={{ width: '100%' }}>
            <Typography variant="caption" fontWeight={600}>{roi}%</Typography>
            <LinearProgress
              variant="determinate"
              value={Math.min(Number(roi), 100)}
              color={Number(roi) > 100 ? 'success' : 'primary'}
              sx={{ height: 4, borderRadius: 2, mt: 0.3 }}
            />
          </Box>
        );
      },
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 130,
      sortable: false,
      filterable: false,
      renderCell: (p: GridRenderCellParams<Campaign>) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="View"><IconButton size="small"><ViewIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(p.row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => setDeleteId(p.row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Stack>
      ),
    },
  ], []);

  const filtered = useMemo(() => {
    if (!search) return campaigns;
    const q = search.toLowerCase();
    return campaigns.filter((c) => c.name.toLowerCase().includes(q));
  }, [campaigns, search]);

  return (
    <>
      <PageHeader
        title="Marketing Campaigns"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Campaigns' }]}
      />

      {/* KPI cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard title="Active Campaigns" value={totals.active} icon={<CampaignIcon />} color="#7c3aed" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard title="Total Budget" value={`$${totals.totalBudget.toLocaleString()}`} icon={<AttachMoney />} color="#059669" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard title="Total Leads" value={totals.totalLeads} icon={<People />} color="#1976d2" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <MetricCard title="Conversions" value={totals.totalConversions} change={14.2} icon={<TrendingUp />} color="#d97706" />
        </Grid>
      </Grid>

      {/* Performance chart */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12}>
          <ChartWidget title="Campaign Performance" subtitle="Leads & conversions by campaign" height={260}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={campaignPerformance}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <RechartsTooltip />
                <Legend />
                <Bar dataKey="leads" fill="#1976d2" radius={[4, 4, 0, 0]} />
                <Bar dataKey="conversions" fill="#059669" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </ChartWidget>
        </Grid>
      </Grid>

      {/* Table */}
      <DataTable
        title="All Campaigns"
        rows={filtered}
        columns={columns}
        searchValue={search}
        onSearchChange={setSearch}
        onAdd={openCreate}
        addLabel="New Campaign"
      />

      <ModalForm
        open={formOpen}
        title={editingId ? 'Edit Campaign' : 'New Campaign'}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
      >
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid item xs={12}>
            <TextField fullWidth label="Campaign Name" value={formData.name} onChange={handleChange('name')} required />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Type" value={formData.type} onChange={handleChange('type')}>
              {typeOptions.map((t) => <MenuItem key={t} value={t}>{t.replace(/_/g, ' ')}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Status" value={formData.status} onChange={handleChange('status')}>
              {statusOptions.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Start Date" type="date" value={formData.startDate} onChange={handleChange('startDate')} InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="End Date" type="date" value={formData.endDate} onChange={handleChange('endDate')} InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Budget" type="number" value={formData.budget} onChange={handleChange('budget')} />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth multiline rows={3} label="Description" value={formData.description} onChange={handleChange('description')} />
          </Grid>
        </Grid>
      </ModalForm>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete Campaign"
        message="Are you sure you want to delete this campaign?"
        confirmLabel="Delete"
        confirmColor="error"
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </>
  );
};

export default CampaignsPage;
