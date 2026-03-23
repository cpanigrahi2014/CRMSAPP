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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  Chip,
} from '@mui/material';
import {
  ViewKanban as KanbanIcon,
  ViewList as ListIcon,
  Add as AddIcon,
  FileUpload as ImportIcon,
  FileDownload as ExportIcon,
  Settings as SettingsIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import { PageHeader, StatusChip, DataTable, ModalForm, ImportPreviewDialog } from '../components';
import KanbanBoard, { KanbanColumn } from '../components/KanbanBoard';
import { opportunityService } from '../services';
import { Opportunity, OpportunityStage, PipelineStage } from '../types';

/* Fallback stages used while loading or if API fails */
const DEFAULT_STAGES: PipelineStage[] = [
  { id: '1', name: 'PROSPECTING', displayName: 'Prospecting', displayOrder: 0, color: '#1976d2', defaultProbability: 10, forecastCategory: 'PIPELINE', closedWon: false, closedLost: false, active: true, createdAt: '', updatedAt: '' },
  { id: '2', name: 'QUALIFICATION', displayName: 'Qualification', displayOrder: 1, color: '#7c3aed', defaultProbability: 25, forecastCategory: 'PIPELINE', closedWon: false, closedLost: false, active: true, createdAt: '', updatedAt: '' },
  { id: '3', name: 'NEEDS_ANALYSIS', displayName: 'Needs Analysis', displayOrder: 2, color: '#8b5cf6', defaultProbability: 40, forecastCategory: 'PIPELINE', closedWon: false, closedLost: false, active: true, createdAt: '', updatedAt: '' },
  { id: '4', name: 'PROPOSAL', displayName: 'Proposal', displayOrder: 3, color: '#d97706', defaultProbability: 60, forecastCategory: 'BEST_CASE', closedWon: false, closedLost: false, active: true, createdAt: '', updatedAt: '' },
  { id: '5', name: 'NEGOTIATION', displayName: 'Negotiation', displayOrder: 4, color: '#0891b2', defaultProbability: 80, forecastCategory: 'COMMIT', closedWon: false, closedLost: false, active: true, createdAt: '', updatedAt: '' },
  { id: '6', name: 'CLOSED_WON', displayName: 'Closed Won', displayOrder: 5, color: '#059669', defaultProbability: 100, forecastCategory: 'CLOSED', closedWon: true, closedLost: false, active: true, createdAt: '', updatedAt: '' },
  { id: '7', name: 'CLOSED_LOST', displayName: 'Closed Lost', displayOrder: 6, color: '#dc2626', defaultProbability: 0, forecastCategory: 'CLOSED', closedWon: false, closedLost: true, active: true, createdAt: '', updatedAt: '' },
];

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
  const [pipelineStages, setPipelineStages] = useState<PipelineStage[]>(DEFAULT_STAGES);
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
  const [importDialogOpen, setImportDialogOpen] = useState(false);

  // stage management
  const [stageDialogOpen, setStageDialogOpen] = useState(false);
  const [stageFormOpen, setStageFormOpen] = useState(false);
  const [editingStageId, setEditingStageId] = useState<string | null>(null);
  const [stageForm, setStageForm] = useState({
    name: '', displayName: '', displayOrder: 0, color: '#1976d2',
    defaultProbability: 0, forecastCategory: 'PIPELINE',
    closedWon: false, closedLost: false, active: true,
  });
  const [stageSaving, setStageSaving] = useState(false);

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

  /* ---- load pipeline stages ---- */
  useEffect(() => {
    opportunityService.getPipelineStages()
      .then((res) => {
        const stages = res.data;
        if (Array.isArray(stages) && stages.length > 0) {
          setPipelineStages(stages);
        }
      })
      .catch(() => { /* keep defaults */ });
  }, []);

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
    return pipelineStages.map((stage) => ({
      id: stage.name,
      title: stage.displayName,
      color: stage.color,
      items: filtered
        .filter((o) => o.stage === stage.name)
        .map((o) => ({
          id: o.id,
          title: o.name,
          subtitle: o.accountName ?? o.accountId ?? undefined,
          value: `$${(o.amount ?? 0).toLocaleString()}`,
          tag: `${o.probability ?? 0}%`,
          tagColor: o.probability && o.probability >= 70 ? ('success' as const) : ('info' as const),
        })),
    }));
  }, [filtered, pipelineStages]);

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

  const handleImportConfirm = async (file: File) => {
    try {
      const res = await opportunityService.importCsv(file);
      enqueueSnackbar(`Imported ${(res.data as any)?.imported ?? 0} opportunities`, { variant: 'success' });
      fetchOpps();
    } catch {
      enqueueSnackbar('Import failed', { variant: 'error' });
    }
  };

  /* ---- stage management ---- */
  const fetchStages = async () => {
    try {
      const res = await opportunityService.getAllPipelineStages();
      const stages = res.data;
      if (Array.isArray(stages) && stages.length > 0) {
        setPipelineStages(stages.filter((s) => s.active));
      }
    } catch { /* ignore */ }
  };

  const openStageCreate = () => {
    setEditingStageId(null);
    setStageForm({
      name: '', displayName: '', displayOrder: pipelineStages.length, color: '#1976d2',
      defaultProbability: 0, forecastCategory: 'PIPELINE',
      closedWon: false, closedLost: false, active: true,
    });
    setStageFormOpen(true);
  };

  const openStageEdit = (stage: PipelineStage) => {
    setEditingStageId(stage.id);
    setStageForm({
      name: stage.name, displayName: stage.displayName,
      displayOrder: stage.displayOrder, color: stage.color,
      defaultProbability: stage.defaultProbability,
      forecastCategory: stage.forecastCategory,
      closedWon: stage.closedWon, closedLost: stage.closedLost,
      active: stage.active,
    });
    setStageFormOpen(true);
  };

  const handleStageSubmit = async () => {
    setStageSaving(true);
    try {
      if (editingStageId) {
        await opportunityService.updatePipelineStage(editingStageId, stageForm);
        enqueueSnackbar('Stage updated', { variant: 'success' });
      } else {
        await opportunityService.createPipelineStage(stageForm);
        enqueueSnackbar('Stage created', { variant: 'success' });
      }
      setStageFormOpen(false);
      fetchStages();
    } catch {
      enqueueSnackbar('Stage operation failed', { variant: 'error' });
    } finally {
      setStageSaving(false);
    }
  };

  const handleStageDelete = async (id: string) => {
    try {
      await opportunityService.deletePipelineStage(id);
      enqueueSnackbar('Stage deleted', { variant: 'success' });
      fetchStages();
    } catch {
      enqueueSnackbar('Failed to delete stage', { variant: 'error' });
    }
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
            <Tooltip title="Manage Stages">
              <IconButton onClick={() => { fetchStages(); setStageDialogOpen(true); }}><SettingsIcon /></IconButton>
            </Tooltip>
          </Stack>
        }
      />

      {/* Import / Export bar */}
      <Box sx={{ mb: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
        <Button variant="outlined" size="small" startIcon={<ImportIcon />} onClick={() => setImportDialogOpen(true)}>Import CSV</Button>
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
          {pipelineStages.map((s) => <MenuItem key={s.name} value={s.name}>{s.displayName}</MenuItem>)}
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
              {pipelineStages.map((s) => (
                <MenuItem key={s.name} value={s.name}>{s.displayName}</MenuItem>
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

      {/* Stage Management Dialog */}
      <Dialog open={stageDialogOpen} onClose={() => setStageDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          Manage Pipeline Stages
          <Button variant="contained" size="small" startIcon={<AddIcon />} onClick={openStageCreate}>Add Stage</Button>
        </DialogTitle>
        <DialogContent>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Order</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Display Name</TableCell>
                <TableCell>Color</TableCell>
                <TableCell>Probability</TableCell>
                <TableCell>Forecast</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {pipelineStages.map((stage) => (
                <TableRow key={stage.id}>
                  <TableCell>{stage.displayOrder}</TableCell>
                  <TableCell>{stage.name}</TableCell>
                  <TableCell>{stage.displayName}</TableCell>
                  <TableCell><Chip size="small" sx={{ bgcolor: stage.color, color: '#fff' }} label={stage.color} /></TableCell>
                  <TableCell>{stage.defaultProbability}%</TableCell>
                  <TableCell>{stage.forecastCategory}</TableCell>
                  <TableCell>
                    {stage.closedWon ? <Chip size="small" label="Won" color="success" /> :
                     stage.closedLost ? <Chip size="small" label="Lost" color="error" /> :
                     <Chip size="small" label="Open" variant="outlined" />}
                  </TableCell>
                  <TableCell>
                    <IconButton size="small" onClick={() => openStageEdit(stage)}><EditIcon fontSize="small" /></IconButton>
                    <IconButton size="small" color="error" onClick={() => handleStageDelete(stage.id)}><DeleteIcon fontSize="small" /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStageDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Stage Form Dialog */}
      <Dialog open={stageFormOpen} onClose={() => setStageFormOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingStageId ? 'Edit Stage' : 'New Stage'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0 }}>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Name (key)" value={stageForm.name}
                onChange={(e) => setStageForm((p) => ({ ...p, name: e.target.value.toUpperCase().replace(/\s+/g, '_') }))} required />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Display Name" value={stageForm.displayName}
                onChange={(e) => setStageForm((p) => ({ ...p, displayName: e.target.value }))} required />
            </Grid>
            <Grid item xs={6} sm={3}>
              <TextField fullWidth label="Order" type="number" value={stageForm.displayOrder}
                onChange={(e) => setStageForm((p) => ({ ...p, displayOrder: Number(e.target.value) }))} />
            </Grid>
            <Grid item xs={6} sm={3}>
              <TextField fullWidth label="Color" type="color" value={stageForm.color}
                onChange={(e) => setStageForm((p) => ({ ...p, color: e.target.value }))} />
            </Grid>
            <Grid item xs={6} sm={3}>
              <TextField fullWidth label="Probability %" type="number" value={stageForm.defaultProbability}
                onChange={(e) => setStageForm((p) => ({ ...p, defaultProbability: Number(e.target.value) }))} />
            </Grid>
            <Grid item xs={6} sm={3}>
              <TextField fullWidth select label="Forecast" value={stageForm.forecastCategory}
                onChange={(e) => setStageForm((p) => ({ ...p, forecastCategory: e.target.value }))}>
                {['PIPELINE', 'BEST_CASE', 'COMMIT', 'CLOSED'].map((f) => <MenuItem key={f} value={f}>{f}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth select label="Closed Won?" value={stageForm.closedWon ? 'yes' : 'no'}
                onChange={(e) => setStageForm((p) => ({ ...p, closedWon: e.target.value === 'yes', closedLost: e.target.value === 'yes' ? false : p.closedLost }))}>
                <MenuItem value="no">No</MenuItem>
                <MenuItem value="yes">Yes</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth select label="Closed Lost?" value={stageForm.closedLost ? 'yes' : 'no'}
                onChange={(e) => setStageForm((p) => ({ ...p, closedLost: e.target.value === 'yes', closedWon: e.target.value === 'yes' ? false : p.closedWon }))}>
                <MenuItem value="no">No</MenuItem>
                <MenuItem value="yes">Yes</MenuItem>
              </TextField>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStageFormOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleStageSubmit} disabled={stageSaving}>
            {stageSaving ? 'Saving...' : editingStageId ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Import Preview Dialog with industry-aware field detection */}
      <ImportPreviewDialog
        open={importDialogOpen}
        onClose={() => setImportDialogOpen(false)}
        onConfirmImport={handleImportConfirm}
        entityType="opportunity"
      />
    </>
  );
};

export default OpportunitiesPage;
