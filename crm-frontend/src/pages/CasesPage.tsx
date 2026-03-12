/* ============================================================
   CasesPage – Support / Customer Service case management
   ============================================================ */
import React, { useState, useMemo } from 'react';
import { GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import {
  TextField,
  Grid,
  MenuItem,
  IconButton,
  Tooltip,
  Stack,
} from '@mui/material';
import { Edit as EditIcon, Delete as DeleteIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { DataTable, PageHeader, StatusChip, ConfirmDialog, ModalForm } from '../components';
import { SupportCase } from '../types';
import { useSnackbar } from 'notistack';

const statusOptions = ['OPEN', 'IN_PROGRESS', 'ESCALATED', 'RESOLVED', 'CLOSED'];
const priorityOptions = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

/* ---- mock data ---- */
const mockCases: SupportCase[] = [
  { id: '1', caseNumber: 'CS-1001', subject: 'Login issues with SSO', status: 'OPEN', priority: 'HIGH', contactName: 'Alice Johnson', accountName: 'Acme Corp', createdDate: '2024-01-15', description: '' },
  { id: '2', caseNumber: 'CS-1002', subject: 'Billing discrepancy on invoice', status: 'IN_PROGRESS', priority: 'MEDIUM', contactName: 'Bob Smith', accountName: 'Globex Inc', createdDate: '2024-01-14', description: '' },
  { id: '3', caseNumber: 'CS-1003', subject: 'Feature request: bulk export', status: 'OPEN', priority: 'LOW', contactName: 'Carol Lee', accountName: 'Initech', createdDate: '2024-01-12', description: '' },
  { id: '4', caseNumber: 'CS-1004', subject: 'Performance degradation on reports', status: 'ESCALATED', priority: 'CRITICAL', contactName: 'Dave Miller', accountName: 'Hooli', createdDate: '2024-01-10', description: '' },
  { id: '5', caseNumber: 'CS-1005', subject: 'Unable to attach files', status: 'RESOLVED', priority: 'MEDIUM', contactName: 'Eve Davis', accountName: 'Stark Industries', createdDate: '2024-01-08', description: '' },
];

const emptyCase = {
  subject: '',
  status: 'OPEN',
  priority: 'MEDIUM',
  contactName: '',
  accountName: '',
  description: '',
};

const CasesPage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [cases, setCases] = useState<SupportCase[]>(mockCases);
  const [search, setSearch] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [formData, setFormData] = useState(emptyCase);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const openCreate = () => { setEditingId(null); setFormData(emptyCase); setFormOpen(true); };
  const openEdit = (c: SupportCase) => {
    setEditingId(c.id);
    setFormData({
      subject: c.subject,
      status: c.status,
      priority: c.priority,
      contactName: c.contactName ?? '',
      accountName: c.accountName ?? '',
      description: c.description ?? '',
    });
    setFormOpen(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingId) {
      setCases((prev) => prev.map((c) => (c.id === editingId ? { ...c, ...formData } as SupportCase : c)));
      enqueueSnackbar('Case updated', { variant: 'success' });
    } else {
      const newCase = {
        id: String(Date.now()),
        caseNumber: `CS-${1005 + cases.length}`,
        ...formData,
        createdDate: new Date().toISOString().split('T')[0],
      } as SupportCase;
      setCases((prev) => [...prev, newCase]);
      enqueueSnackbar('Case created', { variant: 'success' });
    }
    setFormOpen(false);
  };

  const handleDelete = () => {
    setCases((prev) => prev.filter((c) => c.id !== deleteId));
    enqueueSnackbar('Case deleted', { variant: 'success' });
    setDeleteId(null);
  };

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFormData((p) => ({ ...p, [field]: e.target.value }));

  const columns = useMemo<GridColDef[]>(() => [
    { field: 'caseNumber', headerName: 'Case #', width: 110 },
    { field: 'subject', headerName: 'Subject', flex: 1.5, minWidth: 200 },
    { field: 'contactName', headerName: 'Contact', flex: 1, minWidth: 130 },
    { field: 'accountName', headerName: 'Account', flex: 1, minWidth: 130 },
    {
      field: 'priority',
      headerName: 'Priority',
      width: 110,
      renderCell: (p: GridRenderCellParams) => <StatusChip status={p.value} />,
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 130,
      renderCell: (p: GridRenderCellParams) => <StatusChip status={p.value} />,
    },
    { field: 'createdDate', headerName: 'Created', width: 120 },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 130,
      sortable: false,
      filterable: false,
      renderCell: (p: GridRenderCellParams<SupportCase>) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="View"><IconButton size="small"><ViewIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(p.row)}><EditIcon fontSize="small" /></IconButton></Tooltip>
          <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => setDeleteId(p.row.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
        </Stack>
      ),
    },
  ], []);

  const filtered = useMemo(() => {
    if (!search) return cases;
    const q = search.toLowerCase();
    return cases.filter(
      (c) =>
        c.subject.toLowerCase().includes(q) ||
        (c.caseNumber ?? '').toLowerCase().includes(q) ||
        (c.contactName ?? '').toLowerCase().includes(q),
    );
  }, [cases, search]);

  return (
    <>
      <PageHeader
        title="Customer Support Cases"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Cases' }]}
      />

      <DataTable
        title="Support Cases"
        rows={filtered}
        columns={columns}
        searchValue={search}
        onSearchChange={setSearch}
        onAdd={openCreate}
        addLabel="New Case"
      />

      <ModalForm
        open={formOpen}
        title={editingId ? 'Edit Case' : 'New Case'}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
      >
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid item xs={12}>
            <TextField fullWidth label="Subject" value={formData.subject} onChange={handleChange('subject')} required />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Status" value={formData.status} onChange={handleChange('status')}>
              {statusOptions.map((s) => <MenuItem key={s} value={s}>{s.replace(/_/g, ' ')}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth select label="Priority" value={formData.priority} onChange={handleChange('priority')}>
              {priorityOptions.map((p) => <MenuItem key={p} value={p}>{p}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Contact Name" value={formData.contactName} onChange={handleChange('contactName')} />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField fullWidth label="Account Name" value={formData.accountName} onChange={handleChange('accountName')} />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth multiline rows={3} label="Description" value={formData.description} onChange={handleChange('description')} />
          </Grid>
        </Grid>
      </ModalForm>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete Case"
        message="Are you sure you want to delete this support case?"
        confirmLabel="Delete"
        confirmColor="error"
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </>
  );
};

export default CasesPage;
