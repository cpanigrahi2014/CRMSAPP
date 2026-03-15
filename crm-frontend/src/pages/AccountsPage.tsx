/* ============================================================
   AccountsPage – account management with DataGrid + CRUD
   ============================================================ */
import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { GridColDef, GridRenderCellParams, GridPaginationModel, GridRowSelectionModel } from '@mui/x-data-grid';
import {
  TextField,
  MenuItem,
  Grid,
  IconButton,
  Tooltip,
  Stack,
  Chip,
  Button,
  Box,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  FileUpload as ImportIcon,
  FileDownload as ExportIcon,
} from '@mui/icons-material';
import { DataTable, PageHeader, ConfirmDialog, ModalForm } from '../components';
import { accountService } from '../services';
import { Account } from '../types';
import { useSnackbar } from 'notistack';

const typeOptions = ['PROSPECT', 'CUSTOMER', 'PARTNER', 'VENDOR', 'OTHER'];
const lifecycleOptions = ['NEW', 'ACTIVE', 'INACTIVE', 'CHURNED'];

const emptyAccount = {
  name: '',
  industry: '',
  website: '',
  phone: '',
  type: 'PROSPECT',
  territory: '',
  lifecycleStage: 'NEW',
  segment: '',
  billingAddress: '',
  shippingAddress: '',
  description: '',
  annualRevenue: 0,
  numberOfEmployees: 0,
};

const AccountsPage: React.FC = () => {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [formData, setFormData] = useState(emptyAccount);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  // Bulk / Import
  const [selectedIds, setSelectedIds] = useState<GridRowSelectionModel>([]);
  const importInputRef = React.useRef<HTMLInputElement>(null);

  const fetchAccounts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await accountService.getAll(paginationModel.page, paginationModel.pageSize);
      setAccounts(Array.isArray(res.data) ? res.data : (res.data as any).content ?? []);
    } catch {
      enqueueSnackbar('Failed to load accounts', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [paginationModel, enqueueSnackbar]);

  useEffect(() => { fetchAccounts(); }, [fetchAccounts]);

  const openCreate = () => { setEditingId(null); setFormData(emptyAccount); setFormOpen(true); };

  const openEdit = (acc: Account) => {
    setEditingId(acc.id);
    setFormData({
      name: acc.name,
      industry: acc.industry ?? '',
      website: acc.website ?? '',
      phone: acc.phone ?? '',
      type: acc.type ?? 'PROSPECT',
      territory: (acc as any).territory ?? '',
      lifecycleStage: (acc as any).lifecycleStage ?? 'NEW',
      segment: (acc as any).segment ?? '',
      billingAddress: (acc as any).billingAddress ?? '',
      shippingAddress: (acc as any).shippingAddress ?? '',
      description: (acc as any).description ?? '',
      annualRevenue: acc.annualRevenue ?? 0,
      numberOfEmployees: acc.numberOfEmployees ?? 0,
    });
    setFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editingId) {
        await accountService.update(editingId, formData);
        enqueueSnackbar('Account updated', { variant: 'success' });
      } else {
        await accountService.create(formData);
        enqueueSnackbar('Account created', { variant: 'success' });
      }
      setFormOpen(false);
      fetchAccounts();
    } catch {
      enqueueSnackbar('Operation failed', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      await accountService.delete(deleteId);
      enqueueSnackbar('Account deleted', { variant: 'success' });
      fetchAccounts();
    } catch {
      enqueueSnackbar('Delete failed', { variant: 'error' });
    }
    setDeleteId(null);
  };

  const handleBulkDelete = async () => {
    if (selectedIds.length === 0) return;
    try {
      await accountService.bulkDelete(selectedIds.map(String));
      enqueueSnackbar(`${selectedIds.length} accounts deleted`, { variant: 'success' });
      setSelectedIds([]);
      fetchAccounts();
    } catch {
      enqueueSnackbar('Bulk delete failed', { variant: 'error' });
    }
  };

  const handleExport = async () => {
    try {
      const csv = await accountService.exportCsv();
      const blob = new Blob([csv as any], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'accounts.csv'; a.click();
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
      const res = await accountService.importCsv(file);
      enqueueSnackbar(`Imported ${(res.data as any)?.imported ?? 0} accounts`, { variant: 'success' });
      fetchAccounts();
    } catch {
      enqueueSnackbar('Import failed', { variant: 'error' });
    }
    if (importInputRef.current) importInputRef.current.value = '';
  };

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFormData((p) => ({ ...p, [field]: e.target.value }));

  const columns = useMemo<GridColDef[]>(
    () => [
      { field: 'name', headerName: 'Account Name', flex: 1.5, minWidth: 180 },
      { field: 'industry', headerName: 'Industry', flex: 1, minWidth: 120 },
      { field: 'phone', headerName: 'Phone', flex: 1, minWidth: 120 },
      {
        field: 'type',
        headerName: 'Type',
        width: 120,
        renderCell: (p: GridRenderCellParams) => (
          <Chip label={p.value ?? 'N/A'} size="small" variant="outlined" color="primary" />
        ),
      },
      {
        field: 'lifecycleStage',
        headerName: 'Stage',
        width: 110,
        renderCell: (p: GridRenderCellParams) => (
          <Chip label={p.value ?? 'NEW'} size="small" variant="outlined" color="info" />
        ),
      },
      {
        field: 'healthScore',
        headerName: 'Health',
        width: 90,
        renderCell: (p: GridRenderCellParams) => {
          const v = p.value ?? 50;
          const color = v >= 70 ? '#4caf50' : v >= 40 ? '#ff9800' : '#f44336';
          return <Chip label={v} size="small" sx={{ bgcolor: color, color: '#fff', fontWeight: 600 }} />;
        },
      },
      { field: 'territory', headerName: 'Territory', width: 110 },
      {
        field: 'annualRevenue',
        headerName: 'Revenue',
        width: 120,
        valueFormatter: (value: any) => value ? `$${Number(value).toLocaleString()}` : '-',
      },
      {
        field: 'tags',
        headerName: 'Tags',
        width: 160,
        sortable: false,
        renderCell: (p: GridRenderCellParams) => {
          const tags = p.value ?? [];
          return (
            <Stack direction="row" spacing={0.3}>
              {(tags as any[]).slice(0, 2).map((t: any) => (
                <Chip key={t.id} label={t.name} size="small" sx={{ bgcolor: t.color, color: '#fff', fontSize: 11 }} />
              ))}
              {tags.length > 2 && <Chip label={`+${tags.length - 2}`} size="small" variant="outlined" />}
            </Stack>
          );
        },
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 130,
        sortable: false,
        filterable: false,
        renderCell: (p: GridRenderCellParams<Account>) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="View"><IconButton size="small" onClick={() => navigate(`/accounts/${p.row.id}`)}><ViewIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(p.row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => setDeleteId(p.row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
          </Stack>
        ),
      },
    ],
    [navigate],
  );

  const filteredRows = useMemo(() => {
    if (!search) return accounts;
    const q = search.toLowerCase();
    return accounts.filter(
      (a) => a.name.toLowerCase().includes(q) || (a.industry ?? '').toLowerCase().includes(q) || (a.type ?? '').toLowerCase().includes(q),
    );
  }, [accounts, search]);

  return (
    <>
      <PageHeader
        title="Account Management"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Accounts' }]}
      />

      {/* Bulk / Import / Export bar */}
      <Box sx={{ mb: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
        {selectedIds.length > 0 && (
          <Button variant="outlined" color="error" size="small" onClick={handleBulkDelete}>
            Delete {selectedIds.length} Selected
          </Button>
        )}
        <Button variant="outlined" size="small" startIcon={<ImportIcon />} onClick={() => importInputRef.current?.click()}>Import CSV</Button>
        <input type="file" accept=".csv" hidden ref={importInputRef} onChange={handleImport} />
        <Button variant="outlined" size="small" startIcon={<ExportIcon />} onClick={handleExport}>Export CSV</Button>
      </Box>

      <DataTable
        title="Accounts"
        rows={filteredRows}
        columns={columns}
        loading={loading}
        searchValue={search}
        onSearchChange={setSearch}
        onAdd={openCreate}
        addLabel="New Account"
        paginationModel={paginationModel}
        onPaginationModelChange={setPaginationModel}
        checkboxSelection
        onRowSelectionModelChange={setSelectedIds}
        rowSelectionModel={selectedIds}
      />

      {/* Create / Edit form */}
      <ModalForm
        open={formOpen}
        title={editingId ? 'Edit Account' : 'New Account'}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
        loading={saving}
        maxWidth="md"
      >
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Account Name" value={formData.name} onChange={handleChange('name')} required />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Type" value={formData.type} onChange={handleChange('type')}>
              {typeOptions.map((t) => (<MenuItem key={t} value={t}>{t}</MenuItem>))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Industry" value={formData.industry} onChange={handleChange('industry')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Phone" value={formData.phone} onChange={handleChange('phone')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Website" value={formData.website} onChange={handleChange('website')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Territory" value={formData.territory} onChange={handleChange('territory')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Lifecycle Stage" value={formData.lifecycleStage} onChange={handleChange('lifecycleStage')}>
              {lifecycleOptions.map((s) => (<MenuItem key={s} value={s}>{s}</MenuItem>))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Segment" value={formData.segment} onChange={handleChange('segment')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Annual Revenue" type="number" value={formData.annualRevenue} onChange={handleChange('annualRevenue')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Employees" type="number" value={formData.numberOfEmployees} onChange={handleChange('numberOfEmployees')} />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth label="Billing Address" value={formData.billingAddress} onChange={handleChange('billingAddress')} />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth multiline rows={2} label="Description" value={formData.description} onChange={handleChange('description')} />
          </Grid>
        </Grid>
      </ModalForm>

      {/* Delete confirm */}
      <ConfirmDialog
        open={!!deleteId}
        title="Delete Account"
        message="Are you sure you want to delete this account? All related data will be affected."
        confirmLabel="Delete"
        confirmColor="error"
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </>
  );
};

export default AccountsPage;
