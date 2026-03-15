/* ============================================================
   LeadsPage – lead management with DataGrid + CRUD modal
   + bulk operations, import/export, scoring, lead detail nav
   ============================================================ */
import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { GridColDef, GridRenderCellParams, GridPaginationModel, GridRowSelectionModel } from '@mui/x-data-grid';
import {
  Box,
  Button,
  TextField,
  MenuItem,
  Grid,
  IconButton,
  Tooltip,
  Stack,
  Chip,
  FormControlLabel,
  Checkbox,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  SwapHoriz as ConvertIcon,
  Upload as UploadIcon,
  Download as DownloadIcon,
  CheckBox as BulkIcon,
} from '@mui/icons-material';
import { DataTable, PageHeader, StatusChip, ConfirmDialog, ModalForm } from '../components';
import { leadService } from '../services';
import { Lead, LeadStatus, LeadSource, OpportunityStage, ConvertLeadRequest } from '../types';
import { useSnackbar } from 'notistack';

const statusOptions: LeadStatus[] = ['NEW', 'CONTACTED', 'QUALIFIED', 'UNQUALIFIED', 'CONVERTED', 'LOST'];
const sourceOptions: LeadSource[] = ['WEB', 'PHONE', 'EMAIL', 'REFERRAL', 'SOCIAL_MEDIA', 'TRADE_SHOW', 'EVENT', 'OTHER'];
const stageOptions: OpportunityStage[] = [
  'PROSPECTING', 'QUALIFICATION', 'NEEDS_ANALYSIS',
  'PROPOSAL', 'NEGOTIATION', 'CLOSED_WON', 'CLOSED_LOST',
];

const emptyLead = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  company: '',
  title: '',
  status: 'NEW' as LeadStatus,
  source: 'WEB' as LeadSource,
  description: '',
};

const LeadsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();

  /* ---- state ---- */
  const [leads, setLeads] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [rowCount, setRowCount] = useState(0);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  // form dialog
  const [formOpen, setFormOpen] = useState(false);
  const [formData, setFormData] = useState(emptyLead);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // confirm dialog
  const [deleteId, setDeleteId] = useState<string | null>(null);

  // convert dialog
  const [convertOpen, setConvertOpen] = useState(false);
  const [convertLeadId, setConvertLeadId] = useState<string | null>(null);
  const [convertLeadName, setConvertLeadName] = useState('');
  const [convertData, setConvertData] = useState<ConvertLeadRequest>({
    opportunityName: '',
    amount: undefined,
    stage: 'PROSPECTING',
    createAccount: false,
    createContact: false,
  });
  const [converting, setConverting] = useState(false);

  // bulk selection
  const [selectedIds, setSelectedIds] = useState<GridRowSelectionModel>([]);

  /* ---- data fetch ---- */
  const fetchLeads = useCallback(async () => {
    setLoading(true);
    try {
      const res = await leadService.getAll(paginationModel.page, paginationModel.pageSize);
      const paged = res.data as any;
      setLeads(Array.isArray(paged) ? paged : paged.content ?? []);
      if (paged?.totalElements !== undefined) setRowCount(paged.totalElements);
    } catch {
      enqueueSnackbar('Failed to load leads', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [paginationModel, enqueueSnackbar]);

  useEffect(() => { fetchLeads(); }, [fetchLeads]);

  /* ---- handlers ---- */
  const openCreate = () => {
    setEditingId(null);
    setFormData(emptyLead);
    setFormOpen(true);
  };

  const openEdit = (lead: Lead) => {
    setEditingId(lead.id);
    setFormData({
      firstName: lead.firstName,
      lastName: lead.lastName,
      email: lead.email,
      phone: lead.phone ?? '',
      company: lead.company,
      title: lead.title ?? '',
      status: lead.status,
      source: lead.source,
      description: lead.description ?? '',
    });
    setFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editingId) {
        await leadService.update(editingId, formData);
        enqueueSnackbar('Lead updated', { variant: 'success' });
      } else {
        await leadService.create(formData);
        enqueueSnackbar('Lead created', { variant: 'success' });
      }
      setFormOpen(false);
      fetchLeads();
    } catch {
      enqueueSnackbar('Operation failed', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      await leadService.delete(deleteId);
      enqueueSnackbar('Lead deleted', { variant: 'success' });
      fetchLeads();
    } catch {
      enqueueSnackbar('Delete failed', { variant: 'error' });
    }
    setDeleteId(null);
  };

  const handleConvert = async (id: string) => {
    const lead = leads.find((l) => l.id === id);
    const defaultName = lead ? `${lead.firstName} ${lead.lastName} - ${lead.company}` : '';
    setConvertLeadId(id);
    setConvertLeadName(lead ? `${lead.firstName} ${lead.lastName}` : '');
    setConvertData({ opportunityName: defaultName, amount: undefined, stage: 'PROSPECTING', createAccount: false, createContact: false });
    setConvertOpen(true);
  };

  const handleConvertSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!convertLeadId) return;
    setConverting(true);
    try {
      await leadService.convert(convertLeadId, convertData);
      enqueueSnackbar('Lead converted to opportunity successfully!', { variant: 'success' });
      setConvertOpen(false);
      fetchLeads();
    } catch {
      enqueueSnackbar('Conversion failed', { variant: 'error' });
    } finally {
      setConverting(false);
    }
  };

  /* ---- Bulk operations ---- */
  const handleBulkDelete = async () => {
    if (selectedIds.length === 0) return;
    try {
      await leadService.bulkUpdate({ leadIds: selectedIds as string[], delete: true });
      enqueueSnackbar(`${selectedIds.length} leads deleted`, { variant: 'success' });
      setSelectedIds([]);
      fetchLeads();
    } catch { enqueueSnackbar('Bulk delete failed', { variant: 'error' }); }
  };

  const handleBulkStatusUpdate = async (status: LeadStatus) => {
    if (selectedIds.length === 0) return;
    try {
      await leadService.bulkUpdate({ leadIds: selectedIds as string[], status });
      enqueueSnackbar(`${selectedIds.length} leads updated to ${status}`, { variant: 'success' });
      setSelectedIds([]);
      fetchLeads();
    } catch { enqueueSnackbar('Bulk update failed', { variant: 'error' }); }
  };

  /* ---- Import / Export ---- */
  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files?.[0]) return;
    try {
      const res = await leadService.importCSV(e.target.files[0]);
      const d = res.data as any;
      enqueueSnackbar(`Imported ${d.imported} leads (${d.errors} errors)`, { variant: 'success' });
      fetchLeads();
    } catch { enqueueSnackbar('Import failed', { variant: 'error' }); }
    e.target.value = '';
  };

  const handleExport = async () => {
    try {
      const data = await leadService.exportCSV();
      const url = window.URL.createObjectURL(new Blob([data], { type: 'text/csv' }));
      const a = document.createElement('a');
      a.href = url; a.download = 'leads_export.csv';
      document.body.appendChild(a); a.click(); a.remove();
      window.URL.revokeObjectURL(url);
    } catch { enqueueSnackbar('Export failed', { variant: 'error' }); }
  };

  /* ---- change helper ---- */
  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFormData((p) => ({ ...p, [field]: e.target.value }));

  /* ---- columns ---- */
  const columns = useMemo<GridColDef[]>(
    () => [
      { field: 'firstName', headerName: 'First Name', flex: 1, minWidth: 110 },
      { field: 'lastName', headerName: 'Last Name', flex: 1, minWidth: 110 },
      { field: 'company', headerName: 'Company', flex: 1, minWidth: 130 },
      { field: 'email', headerName: 'Email', flex: 1.2, minWidth: 170 },
      {
        field: 'leadScore',
        headerName: 'Score',
        width: 80,
        renderCell: (p: GridRenderCellParams) => (
          <Chip label={p.value ?? 0} size="small" color={p.value >= 70 ? 'success' : p.value >= 40 ? 'warning' : 'default'} />
        ),
      },
      {
        field: 'source',
        headerName: 'Source',
        width: 120,
        renderCell: (p: GridRenderCellParams) => <StatusChip status={p.value} />,
      },
      {
        field: 'status',
        headerName: 'Status',
        width: 120,
        renderCell: (p: GridRenderCellParams) => <StatusChip status={p.value} />,
      },
      {
        field: 'tags',
        headerName: 'Tags',
        width: 140,
        sortable: false,
        renderCell: (p: GridRenderCellParams) => (
          <Stack direction="row" spacing={0.3} sx={{ overflow: 'hidden' }}>
            {(p.value ?? []).slice(0, 2).map((t: any) => (
              <Chip key={t.id} label={t.name} size="small" sx={{ bgcolor: t.color, color: '#fff', height: 20, '& .MuiChip-label': { px: 0.5, fontSize: '0.7rem' } }} />
            ))}
            {(p.value ?? []).length > 2 && <Chip label={`+${p.value.length - 2}`} size="small" sx={{ height: 20 }} />}
          </Stack>
        ),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 170,
        sortable: false,
        filterable: false,
        renderCell: (p: GridRenderCellParams<Lead>) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="View">
              <IconButton size="small" onClick={() => navigate(`/leads/${p.row.id}`)}>
                <ViewIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => openEdit(p.row)}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Convert">
              <IconButton size="small" color="success" onClick={() => handleConvert(p.row.id)}>
                <ConvertIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Delete">
              <IconButton size="small" color="error" onClick={() => setDeleteId(p.row.id)}>
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        ),
      },
    ],
    [navigate],
  );

  /* ---- filtered rows ---- */
  const filteredRows = useMemo(() => {
    if (!search) return leads;
    const q = search.toLowerCase();
    return leads.filter(
      (l) =>
        l.firstName?.toLowerCase().includes(q) ||
        l.lastName?.toLowerCase().includes(q) ||
        l.company?.toLowerCase().includes(q) ||
        l.email?.toLowerCase().includes(q),
    );
  }, [leads, search]);

  return (
    <>
      <PageHeader
        title="Lead Management"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Leads' }]}
      />

      {/* Toolbar: Import / Export / Bulk */}
      <Stack direction="row" spacing={1} sx={{ mb: 1 }} flexWrap="wrap">
        <Button variant="outlined" size="small" startIcon={<DownloadIcon />} onClick={handleExport}>
          Export CSV
        </Button>
        <Button variant="outlined" size="small" component="label" startIcon={<UploadIcon />}>
          Import CSV
          <input type="file" accept=".csv" hidden onChange={handleImport} />
        </Button>
        {selectedIds.length > 0 && (
          <>
            <Chip label={`${selectedIds.length} selected`} color="primary" size="small" />
            <Button size="small" color="error" onClick={handleBulkDelete}>Bulk Delete</Button>
            <Button size="small" onClick={() => handleBulkStatusUpdate('QUALIFIED')}>Mark Qualified</Button>
            <Button size="small" onClick={() => handleBulkStatusUpdate('CONTACTED')}>Mark Contacted</Button>
          </>
        )}
      </Stack>

      <DataTable
        title="Leads"
        rows={filteredRows}
        columns={columns}
        loading={loading}
        searchValue={search}
        onSearchChange={setSearch}
        onAdd={openCreate}
        addLabel="New Lead"
        paginationModel={paginationModel}
        onPaginationModelChange={setPaginationModel}
        paginationMode="server"
        rowCount={rowCount}
        checkboxSelection
        onRowSelectionModelChange={setSelectedIds}
        rowSelectionModel={selectedIds}
      />

      {/* Create / Edit Modal */}
      <ModalForm
        open={formOpen}
        title={editingId ? 'Edit Lead' : 'New Lead'}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
        loading={saving}
        maxWidth="md"
      >
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="First Name" value={formData.firstName} onChange={handleChange('firstName')} required />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Last Name" value={formData.lastName} onChange={handleChange('lastName')} required />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Email" type="email" value={formData.email} onChange={handleChange('email')} required />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Phone" value={formData.phone} onChange={handleChange('phone')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Company" value={formData.company} onChange={handleChange('company')} required />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Job Title" value={formData.title} onChange={handleChange('title')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Status" value={formData.status} onChange={handleChange('status')}>
              {statusOptions.map((s) => (
                <MenuItem key={s} value={s}>{s.replace(/_/g, ' ')}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Source" value={formData.source} onChange={handleChange('source')}>
              {sourceOptions.map((s) => (
                <MenuItem key={s} value={s}>{s.replace(/_/g, ' ')}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth multiline rows={3} label="Description" value={formData.description} onChange={handleChange('description')} />
          </Grid>
        </Grid>
      </ModalForm>

      {/* Delete Confirm */}
      <ConfirmDialog
        open={!!deleteId}
        title="Delete Lead"
        message="Are you sure you want to delete this lead? This action cannot be undone."
        confirmLabel="Delete"
        confirmColor="error"
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />

      {/* Convert Lead to Opportunity Dialog */}
      <ModalForm
        open={convertOpen}
        title={`Convert Lead${convertLeadName ? ` — ${convertLeadName}` : ''}`}
        onClose={() => setConvertOpen(false)}
        onSubmit={handleConvertSubmit}
        loading={converting}
        maxWidth="sm"
      >
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Opportunity Name"
              value={convertData.opportunityName}
              onChange={(e) => setConvertData((p) => ({ ...p, opportunityName: e.target.value }))}
              required
              helperText="Name for the new opportunity"
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              label="Amount"
              type="number"
              value={convertData.amount ?? ''}
              onChange={(e) =>
                setConvertData((p) => ({
                  ...p,
                  amount: e.target.value ? Number(e.target.value) : undefined,
                }))
              }
              InputProps={{ inputProps: { min: 0, step: 0.01 } }}
              helperText="Deal value (optional)"
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              select
              label="Stage"
              value={convertData.stage ?? 'PROSPECTING'}
              onChange={(e) =>
                setConvertData((p) => ({ ...p, stage: e.target.value as OpportunityStage }))
              }
              helperText="Initial pipeline stage"
            >
              {stageOptions.map((s) => (
                <MenuItem key={s} value={s}>
                  {s.replace(/_/g, ' ')}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControlLabel
              control={<Checkbox checked={convertData.createAccount ?? false} onChange={(e) => setConvertData((p) => ({ ...p, createAccount: e.target.checked }))} />}
              label="Create Account from Company"
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControlLabel
              control={<Checkbox checked={convertData.createContact ?? false} onChange={(e) => setConvertData((p) => ({ ...p, createContact: e.target.checked }))} />}
              label="Create Contact from Lead"
            />
          </Grid>
        </Grid>
      </ModalForm>
    </>
  );
};

export default LeadsPage;
