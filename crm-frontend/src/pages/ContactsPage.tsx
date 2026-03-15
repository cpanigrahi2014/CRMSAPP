/* ============================================================
   ContactsPage – contact management with DataGrid + CRUD
   Enhanced with segmentation, consent, navigation
   ============================================================ */
import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { GridColDef, GridRenderCellParams, GridPaginationModel } from '@mui/x-data-grid';
import {
  TextField,
  Grid,
  IconButton,
  Tooltip,
  Stack,
  Avatar,
  Box,
  Typography,
  Chip,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Button,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Email as EmailIcon,
  Phone as PhoneIcon,
  Visibility as ViewIcon,
  FileUpload as ImportIcon,
  FileDownload as ExportIcon,
} from '@mui/icons-material';
import { DataTable, PageHeader, ConfirmDialog, ModalForm } from '../components';
import { contactService } from '../services';
import { Contact } from '../types';
import { useSnackbar } from 'notistack';

const lifecycleOptions = ['', 'SUBSCRIBER', 'LEAD', 'MQL', 'SQL', 'OPPORTUNITY', 'CUSTOMER', 'EVANGELIST', 'OTHER'];
const segmentOptions = ['', 'Enterprise', 'Mid-Market', 'SMB', 'Startup', 'Government', 'Non-Profit', 'Other'];
const leadSourceOptions = ['', 'Website', 'Referral', 'Campaign', 'Social Media', 'Trade Show', 'Cold Call', 'Partner', 'Other'];

const emptyContact: Record<string, any> = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  accountId: '',
  title: '',
  department: '',
  mailingAddress: '',
  description: '',
  segment: '',
  lifecycleStage: '',
  leadSource: '',
};

const ContactsPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const navigate = useNavigate();

  const [contacts, setContacts] = useState<Contact[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [formData, setFormData] = useState(emptyContact);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const importInputRef = React.useRef<HTMLInputElement>(null);

  const fetchContacts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await contactService.getAll(paginationModel.page, paginationModel.pageSize);
      setContacts(Array.isArray(res.data) ? res.data : (res.data as any).content ?? []);
    } catch {
      enqueueSnackbar('Failed to load contacts', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [paginationModel, enqueueSnackbar]);

  useEffect(() => { fetchContacts(); }, [fetchContacts]);

  const openCreate = () => { setEditingId(null); setFormData({ ...emptyContact }); setFormOpen(true); };

  const openEdit = (c: Contact) => {
    setEditingId(c.id);
    setFormData({
      firstName: c.firstName,
      lastName: c.lastName,
      email: c.email,
      phone: c.phone ?? '',
      accountId: c.accountId ?? '',
      title: c.title ?? '',
      department: c.department ?? '',
      mailingAddress: c.mailingAddress ?? '',
      description: c.description ?? '',
      segment: c.segment ?? '',
      lifecycleStage: c.lifecycleStage ?? '',
      leadSource: c.leadSource ?? '',
    });
    setFormOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      // Filter out empty strings for optional fields
      const payload: Record<string, any> = {};
      for (const [k, v] of Object.entries(formData)) {
        if (v !== '' && v !== null && v !== undefined) payload[k] = v;
      }
      if (editingId) {
        await contactService.update(editingId, payload);
        enqueueSnackbar('Contact updated', { variant: 'success' });
      } else {
        await contactService.create(payload as any);
        enqueueSnackbar('Contact created', { variant: 'success' });
      }
      setFormOpen(false);
      fetchContacts();
    } catch {
      enqueueSnackbar('Operation failed', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      await contactService.delete(deleteId);
      enqueueSnackbar('Contact deleted', { variant: 'success' });
      fetchContacts();
    } catch {
      enqueueSnackbar('Delete failed', { variant: 'error' });
    }
    setDeleteId(null);
  };

  const handleExport = async () => {
    try {
      const csv = await contactService.exportCsv();
      const blob = new Blob([csv as any], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'contacts.csv'; a.click();
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
      const res = await contactService.importCsv(file);
      enqueueSnackbar(`Imported ${(res.data as any)?.imported ?? 0} contacts`, { variant: 'success' });
      fetchContacts();
    } catch {
      enqueueSnackbar('Import failed', { variant: 'error' });
    }
    if (importInputRef.current) importInputRef.current.value = '';
  };

  const handleChange = (field: string) => (e: any) =>
    setFormData((p) => ({ ...p, [field]: e.target.value }));

  const columns = useMemo<GridColDef[]>(
    () => [
      {
        field: 'fullName',
        headerName: 'Name',
        flex: 1.3,
        minWidth: 200,
        valueGetter: (_value: any, row: any) => `${row.firstName} ${row.lastName}`,
        renderCell: (p: GridRenderCellParams) => (
          <Box
            sx={{ display: 'flex', alignItems: 'center', gap: 1.5, cursor: 'pointer' }}
            onClick={() => navigate(`/contacts/${p.row.id}`)}
          >
            <Avatar sx={{ width: 32, height: 32, fontSize: 13, bgcolor: 'primary.main' }}>
              {(p.row.firstName?.[0] ?? '') + (p.row.lastName?.[0] ?? '')}
            </Avatar>
            <Box>
              <Typography variant="body2" fontWeight={600} sx={{ color: 'primary.main', '&:hover': { textDecoration: 'underline' } }}>
                {p.value}
              </Typography>
              <Typography variant="caption" color="text.secondary">{p.row.title}</Typography>
            </Box>
          </Box>
        ),
      },
      { field: 'email', headerName: 'Email', flex: 1.2, minWidth: 180 },
      { field: 'phone', headerName: 'Phone', flex: 0.8, minWidth: 120 },
      { field: 'department', headerName: 'Department', flex: 0.8, minWidth: 110 },
      {
        field: 'segment',
        headerName: 'Segment',
        flex: 0.7,
        minWidth: 100,
        renderCell: (p: GridRenderCellParams) =>
          p.value ? <Chip label={p.value} size="small" color="primary" variant="outlined" /> : <Typography variant="body2" color="text.secondary">—</Typography>,
      },
      {
        field: 'lifecycleStage',
        headerName: 'Lifecycle',
        flex: 0.7,
        minWidth: 100,
        renderCell: (p: GridRenderCellParams) =>
          p.value ? <Chip label={p.value} size="small" color="secondary" variant="outlined" /> : <Typography variant="body2" color="text.secondary">—</Typography>,
      },
      {
        field: 'consent',
        headerName: 'Consent',
        flex: 0.6,
        minWidth: 90,
        sortable: false,
        renderCell: (p: GridRenderCellParams) => (
          <Stack direction="row" spacing={0.3}>
            {p.row.emailOptIn && <Chip label="E" size="small" color="success" sx={{ minWidth: 24 }} />}
            {p.row.smsOptIn && <Chip label="S" size="small" color="info" sx={{ minWidth: 24 }} />}
            {p.row.doNotCall && <Chip label="DNC" size="small" color="error" sx={{ minWidth: 36 }} />}
          </Stack>
        ),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 170,
        sortable: false,
        filterable: false,
        renderCell: (p: GridRenderCellParams<Contact>) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="View"><IconButton size="small" onClick={() => navigate(`/contacts/${p.row.id}`)}><ViewIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="Email"><IconButton size="small" href={`mailto:${p.row.email}`}><EmailIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="Call"><IconButton size="small" href={`tel:${p.row.phone}`}><PhoneIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(p.row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
            <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => setDeleteId(p.row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
          </Stack>
        ),
      },
    ],
    [navigate],
  );

  const filteredRows = useMemo(() => {
    if (!search) return contacts;
    const q = search.toLowerCase();
    return contacts.filter(
      (c) =>
        c.firstName?.toLowerCase().includes(q) ||
        c.lastName?.toLowerCase().includes(q) ||
        c.email?.toLowerCase().includes(q) ||
        c.segment?.toLowerCase().includes(q) ||
        c.department?.toLowerCase().includes(q),
    );
  }, [contacts, search]);

  return (
    <>
      <PageHeader
        title="Contact Management"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Contacts' }]}
      />

      {/* Import / Export bar */}
      <Box sx={{ mb: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
        <Button variant="outlined" size="small" startIcon={<ImportIcon />} onClick={() => importInputRef.current?.click()}>Import CSV</Button>
        <input type="file" accept=".csv" hidden ref={importInputRef} onChange={handleImport} />
        <Button variant="outlined" size="small" startIcon={<ExportIcon />} onClick={handleExport}>Export CSV</Button>
      </Box>

      <DataTable
        title="Contacts"
        rows={filteredRows}
        columns={columns}
        loading={loading}
        searchValue={search}
        onSearchChange={setSearch}
        onAdd={openCreate}
        addLabel="New Contact"
        paginationModel={paginationModel}
        onPaginationModelChange={setPaginationModel}
      />

      <ModalForm
        open={formOpen}
        title={editingId ? 'Edit Contact' : 'New Contact'}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
        loading={saving}
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
            <TextField fullWidth label="Job Title" value={formData.title} onChange={handleChange('title')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Department" value={formData.department} onChange={handleChange('department')} />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth label="Account ID" value={formData.accountId} onChange={handleChange('accountId')} />
          </Grid>
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth size="small">
              <InputLabel>Segment</InputLabel>
              <Select value={formData.segment} label="Segment" onChange={handleChange('segment')}>
                {segmentOptions.map((o) => <MenuItem key={o} value={o}>{o || '(none)'}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth size="small">
              <InputLabel>Lifecycle</InputLabel>
              <Select value={formData.lifecycleStage} label="Lifecycle" onChange={handleChange('lifecycleStage')}>
                {lifecycleOptions.map((o) => <MenuItem key={o} value={o}>{o || '(none)'}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth size="small">
              <InputLabel>Lead Source</InputLabel>
              <Select value={formData.leadSource} label="Lead Source" onChange={handleChange('leadSource')}>
                {leadSourceOptions.map((o) => <MenuItem key={o} value={o}>{o || '(none)'}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth label="Mailing Address" value={formData.mailingAddress} onChange={handleChange('mailingAddress')} />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth multiline rows={2} label="Notes" value={formData.description} onChange={handleChange('description')} />
          </Grid>
        </Grid>
      </ModalForm>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete Contact"
        message="Are you sure you want to delete this contact?"
        confirmLabel="Delete"
        confirmColor="error"
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </>
  );
};

export default ContactsPage;
