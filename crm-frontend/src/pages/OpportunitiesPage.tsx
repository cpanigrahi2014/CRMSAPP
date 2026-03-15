/* ============================================================
   OpportunitiesPage – Kanban pipeline + list toggle
   ============================================================ */
import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  ToggleButtonGroup,
  ToggleButton,
  Tooltip,
  IconButton,
  Grid,
  TextField,
  MenuItem,
  Stack,
  Link,
  Button,
} from '@mui/material';
import {
  ViewKanban as KanbanIcon,
  ViewList as ListIcon,
  Add as AddIcon,
  FileUpload as ImportIcon,
  FileDownload as ExportIcon,
} from '@mui/icons-material';
import { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import { PageHeader, StatusChip, DataTable, ModalForm } from '../components';
import KanbanBoard, { KanbanColumn } from '../components/KanbanBoard';
import { opportunityService } from '../services';
import { Opportunity, OpportunityStage } from '../types';

const STAGES: OpportunityStage[] = [
  'PROSPECTING',
  'QUALIFICATION',
  'NEEDS_ANALYSIS',
  'PROPOSAL',
  'NEGOTIATION',
  'CLOSED_WON',
  'CLOSED_LOST',
];

const STAGE_COLORS: Record<string, string> = {
  PROSPECTING: '#1976d2',
  QUALIFICATION: '#7c3aed',
  NEEDS_ANALYSIS: '#8b5cf6',
  PROPOSAL: '#d97706',
  NEGOTIATION: '#0891b2',
  CLOSED_WON: '#059669',
  CLOSED_LOST: '#dc2626',
};

const emptyOpp = {
  name: '',
  accountId: '',
  amount: 0,
  stage: 'PROSPECTING' as OpportunityStage,
  closeDate: '',
  probability: 10,
  description: '',
  currency: 'USD',
  leadSource: '',
  nextStep: '',
};

const OpportunitiesPage: React.FC = () => {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [view, setView] = useState<'kanban' | 'list'>('kanban');

  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  // filters
  const [filterStage, setFilterStage] = useState<string>('ALL');
  const [filterSource, setFilterSource] = useState<string>('ALL');
  const [filterDateFrom, setFilterDateFrom] = useState<string>('');
  const [filterDateTo, setFilterDateTo] = useState<string>('');

  // form
  const [formOpen, setFormOpen] = useState(false);
  const [formData, setFormData] = useState(emptyOpp);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const importInputRef = React.useRef<HTMLInputElement>(null);

  /* ---- fetch ---- */
  const fetchOpps = useCallback(async () => {
    setLoading(true);
    try {
      const res = await opportunityService.getAll(0, 200);
      setOpportunities(Array.isArray(res.data) ? res.data : (res.data as any).content ?? []);
    } catch {
      enqueueSnackbar('Failed to load opportunities', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [enqueueSnackbar]);

  useEffect(() => { fetchOpps(); }, [fetchOpps]);

  /* ---- filtered ---- */
  const filtered = useMemo(() => {
    let result = opportunities;
    if (search) {
      const q = search.toLowerCase();
      result = result.filter((o) => o.name.toLowerCase().includes(q));
    }
    if (filterStage !== 'ALL') {
      result = result.filter((o) => o.stage === filterStage);
    }
    if (filterSource !== 'ALL') {
      result = result.filter((o) => o.leadSource === filterSource);
    }
    if (filterDateFrom) {
      result = result.filter((o) => o.closeDate >= filterDateFrom);
    }
    if (filterDateTo) {
      result = result.filter((o) => o.closeDate <= filterDateTo);
    }
    return result;
  }, [opportunities, search, filterStage, filterSource, filterDateFrom, filterDateTo]);

  /* ---- Kanban columns ---- */
  const kanbanColumns = useMemo<KanbanColumn[]>(() => {
    return STAGES.map((stage) => ({
      id: stage,
      title: stage.replace(/_/g, ' '),
      color: STAGE_COLORS[stage],
      items: filtered
        .filter((o) => o.stage === stage)
        .map((o) => ({
          id: o.id,
          title: o.name,
          subtitle: o.accountName ?? o.accountId ?? undefined,
          value: `$${(o.amount ?? 0).toLocaleString()}`,
          tag: `${o.probability ?? 0}%`,
          tagColor: o.probability && o.probability >= 70 ? ('success' as const) : ('info' as const),
        })),
    }));
  }, [filtered]);

  /* ---- drag handler ---- */
  const handleDragEnd = async (itemId: string, _from: string, to: string) => {
    // optimistic update
    setOpportunities((prev) =>
      prev.map((o) => (o.id === itemId ? { ...o, stage: to as OpportunityStage } : o)),
    );
    try {
      await opportunityService.updateStage(itemId, to as OpportunityStage);
      enqueueSnackbar('Stage updated', { variant: 'success' });
    } catch {
      enqueueSnackbar('Failed to update stage', { variant: 'error' });
      fetchOpps(); // rollback
    }
  };

  /* ---- form ---- */
  const openCreate = () => {
    setEditingId(null);
    setFormData(emptyOpp);
    setFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editingId) {
        await opportunityService.update(editingId, formData);
        enqueueSnackbar('Opportunity updated', { variant: 'success' });
      } else {
        await opportunityService.create(formData);
        enqueueSnackbar('Opportunity created', { variant: 'success' });
      }
      setFormOpen(false);
      fetchOpps();
    } catch {
      enqueueSnackbar('Operation failed', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFormData((p) => ({ ...p, [field]: e.target.value }));

  const handleExport = async () => {
    try {
      const csv = await opportunityService.exportCsv();
      const blob = new Blob([csv as any], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'opportunities.csv'; a.click();
      URL.revokeObjectURL(url);
      enqueueSnackbar('Export complete', { variant: 'success' });
    } catch {
      enqueueSnackbar('Export failed', { variant: 'error' });
    }
  };

  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res = await opportunityService.importCsv(file);
      enqueueSnackbar(`Imported ${(res.data as any)?.imported ?? 0} opportunities`, { variant: 'success' });
      fetchOpps();
    } catch {
      enqueueSnackbar('Import failed', { variant: 'error' });
    }
    if (importInputRef.current) importInputRef.current.value = '';
  };

  /* ---- list columns ---- */
  const listColumns = useMemo<GridColDef[]>(
    () => [
      {
        field: 'name',
        headerName: 'Opportunity',
        flex: 1.5,
        minWidth: 200,
        renderCell: (p: GridRenderCellParams) => (
          <Link component="button" variant="body2" onClick={() => navigate(`/opportunities/${p.row.id}`)} sx={{ textAlign: 'left' }}>
            {p.value}
          </Link>
        ),
      },
      { field: 'accountName', headerName: 'Account', flex: 1, minWidth: 150 },
      {
        field: 'amount',
        headerName: 'Amount',
        width: 130,
        valueFormatter: (value: any) => value ? `$${Number(value).toLocaleString()}` : '-',
      },
      {
        field: 'stage',
        headerName: 'Stage',
        width: 150,
        renderCell: (p: GridRenderCellParams) => <StatusChip status={p.value} />,
      },
      { field: 'probability', headerName: 'Probability', width: 110, valueFormatter: (value: any) => `${value ?? 0}%` },
      { field: 'closeDate', headerName: 'Close Date', width: 130 },
      { field: 'leadSource', headerName: 'Source', width: 120 },
      { field: 'forecastCategory', headerName: 'Forecast', width: 120 },
    ],
    [navigate],
  );

  return (
    <>
      <PageHeader
        title="Opportunity Pipeline"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Opportunities' }]}
        action={
          <Stack direction="row" spacing={1}>
            <ToggleButtonGroup value={view} exclusive onChange={(_, v) => v && setView(v)} size="small">
              <ToggleButton value="kanban"><Tooltip title="Kanban"><KanbanIcon /></Tooltip></ToggleButton>
              <ToggleButton value="list"><Tooltip title="List"><ListIcon /></Tooltip></ToggleButton>
            </ToggleButtonGroup>
            <Tooltip title="New Opportunity">
              <IconButton color="primary" onClick={openCreate}><AddIcon /></IconButton>
            </Tooltip>
          </Stack>
        }
      />

      {/* Import / Export bar */}
      <Box sx={{ mb: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
        <Button variant="outlined" size="small" startIcon={<ImportIcon />} onClick={() => importInputRef.current?.click()}>Import CSV</Button>
        <input type="file" accept=".csv" hidden ref={importInputRef} onChange={handleImport} />
        <Button variant="outlined" size="small" startIcon={<ExportIcon />} onClick={handleExport}>Export CSV</Button>
      </Box>

      {/* ── Filter Bar ── */}
      <Stack direction="row" spacing={2} sx={{ mb: 2, flexWrap: 'wrap' }} alignItems="center">
        <TextField
          size="small" label="Search" value={search}
          onChange={(e) => setSearch(e.target.value)}
          sx={{ minWidth: 180 }}
        />
        <TextField
          size="small" select label="Stage" value={filterStage}
          onChange={(e) => setFilterStage(e.target.value)}
          sx={{ minWidth: 150 }}
        >
          <MenuItem value="ALL">All Stages</MenuItem>
          {STAGES.map((s) => <MenuItem key={s} value={s}>{s.replace(/_/g, ' ')}</MenuItem>)}
        </TextField>
        <TextField
          size="small" select label="Lead Source" value={filterSource}
          onChange={(e) => setFilterSource(e.target.value)}
          sx={{ minWidth: 140 }}
        >
          <MenuItem value="ALL">All Sources</MenuItem>
          {['WEB', 'PHONE', 'EMAIL', 'REFERRAL', 'SOCIAL_MEDIA', 'TRADE_SHOW', 'EVENT', 'PARTNER', 'OTHER'].map((s) => (
            <MenuItem key={s} value={s}>{s}</MenuItem>
          ))}
        </TextField>
        <TextField
          size="small" type="date" label="Close From" value={filterDateFrom}
          onChange={(e) => setFilterDateFrom(e.target.value)}
          InputLabelProps={{ shrink: true }} sx={{ minWidth: 145 }}
        />
        <TextField
          size="small" type="date" label="Close To" value={filterDateTo}
          onChange={(e) => setFilterDateTo(e.target.value)}
          InputLabelProps={{ shrink: true }} sx={{ minWidth: 145 }}
        />
      </Stack>

      {view === 'kanban' ? (
        <KanbanBoard columns={kanbanColumns} onDragEnd={handleDragEnd} onItemClick={(itemId) => navigate(`/opportunities/${itemId}`)} />
      ) : (
        <DataTable
          title="Opportunities"
          rows={filtered}
          columns={listColumns}
          loading={loading}
          searchValue={search}
          onSearchChange={setSearch}
          onAdd={openCreate}
          addLabel="New Opportunity"
        />
      )}

      {/* Form Modal */}
      <ModalForm
        open={formOpen}
        title={editingId ? 'Edit Opportunity' : 'New Opportunity'}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
        loading={saving}
      >
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid item xs={12}>
            <TextField fullWidth label="Opportunity Name" value={formData.name} onChange={handleChange('name')} required />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Account ID" value={formData.accountId} onChange={handleChange('accountId')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Amount" type="number" value={formData.amount} onChange={handleChange('amount')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Stage" value={formData.stage} onChange={handleChange('stage')}>
              {STAGES.map((s) => (
                <MenuItem key={s} value={s}>{s.replace(/_/g, ' ')}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Probability (%)" type="number" value={formData.probability} onChange={handleChange('probability')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Close Date" type="date" value={formData.closeDate} onChange={handleChange('closeDate')} InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Currency" value={formData.currency} onChange={handleChange('currency')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Lead Source" value={formData.leadSource} onChange={handleChange('leadSource')}>
              <MenuItem value="">None</MenuItem>
              {['WEB', 'PHONE', 'EMAIL', 'REFERRAL', 'SOCIAL_MEDIA', 'TRADE_SHOW', 'EVENT', 'PARTNER', 'OTHER'].map((s) => (
                <MenuItem key={s} value={s}>{s}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth label="Next Step" value={formData.nextStep} onChange={handleChange('nextStep')} />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth multiline rows={3} label="Description" value={formData.description} onChange={handleChange('description')} />
          </Grid>
        </Grid>
      </ModalForm>
    </>
  );
};

export default OpportunitiesPage;
