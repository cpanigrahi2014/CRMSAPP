/* ============================================================
   ObjectManagerPage – browse custom objects, fields, pipelines,
   workflows, dashboards, and roles created via AI Config
   ============================================================ */
import React, { useEffect, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Chip,
  Collapse,
  Grid,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tab,
  Tabs,
  Tooltip,
  Typography,
  CircularProgress,
  Alert,
  Paper,
} from '@mui/material';
import {
  ExpandMore as ExpandIcon,
  ExpandLess as CollapseIcon,
  DataObject as ObjectIcon,
  AccountTree as PipelineIcon,
  AutoFixHigh as WorkflowIcon,
  Dashboard as DashboardIcon,
  AdminPanelSettings as RoleIcon,
  CheckCircle as ActiveIcon,
  Cancel as InactiveIcon,
} from '@mui/icons-material';
import PageHeader from '../components/PageHeader';
import api from '../services/api';

/* ---- Types ---- */
interface CrmField {
  id: string;
  name: string;
  label: string;
  fieldType: string;
  isRequired: boolean;
  isUnique: boolean;
  options: unknown;
  description: string | null;
}

interface CrmObject {
  id: string;
  name: string;
  label: string;
  pluralLabel: string;
  description: string | null;
  icon: string | null;
  isSystem: boolean;
  isActive: boolean;
  createdAt: string;
  fields: CrmField[];
}

interface PipelineStage {
  id: string;
  name: string;
  probability: number;
  sortOrder: number;
  isClosed: boolean;
  isWon: boolean;
  color: string | null;
}

interface Pipeline {
  id: string;
  name: string;
  objectName: string;
  description: string | null;
  isDefault: boolean;
  isActive: boolean;
  createdAt: string;
  stages: PipelineStage[];
}

interface Permission {
  id: string;
  objectName: string;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}

interface Role {
  id: string;
  name: string;
  description: string | null;
  isSystem: boolean;
  isActive: boolean;
  createdAt: string;
  permissions: Permission[];
}

interface Workflow {
  id: string;
  name: string;
  objectName: string;
  triggerType: string;
  actions: unknown;
  description: string | null;
  isActive: boolean;
  createdAt: string;
}

interface DashboardData {
  id: string;
  name: string;
  description: string | null;
  isActive: boolean;
  createdAt: string;
  widgets: { id: string; title: string; widgetType: string }[];
}

/* ---- Tab Panel ---- */
function TabPanel({ children, value, index }: { children: React.ReactNode; value: number; index: number }) {
  return value === index ? <Box sx={{ pt: 2 }}>{children}</Box> : null;
}

/* ---- Field type color helper ---- */
function fieldTypeColor(type: string): 'primary' | 'secondary' | 'success' | 'warning' | 'info' | 'error' | 'default' {
  const map: Record<string, 'primary' | 'secondary' | 'success' | 'warning' | 'info' | 'error'> = {
    text: 'primary', textarea: 'primary', number: 'info', currency: 'success',
    date: 'warning', datetime: 'warning', boolean: 'secondary',
    email: 'info', phone: 'info', url: 'info',
    picklist: 'error', multi_picklist: 'error', lookup: 'secondary',
  };
  return map[type] || 'default';
}

/* ---- Expandable Object Row ---- */
function ObjectRow({ obj }: { obj: CrmObject }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <TableRow hover sx={{ '& > *': { borderBottom: 'unset' } }}>
        <TableCell>
          <IconButton size="small" onClick={() => setOpen(!open)}>
            {open ? <CollapseIcon /> : <ExpandIcon />}
          </IconButton>
        </TableCell>
        <TableCell>
          <Typography fontWeight={600}>{obj.label}</Typography>
          <Typography variant="caption" color="text.secondary">{obj.name}</Typography>
        </TableCell>
        <TableCell>
          <Chip label={obj.isSystem ? 'System' : 'Custom'} size="small" color={obj.isSystem ? 'default' : 'primary'} variant="outlined" />
        </TableCell>
        <TableCell align="center">{obj.fields.length}</TableCell>
        <TableCell>
          {obj.isActive ? <ActiveIcon color="success" fontSize="small" /> : <InactiveIcon color="error" fontSize="small" />}
        </TableCell>
        <TableCell>{new Date(obj.createdAt).toLocaleDateString()}</TableCell>
      </TableRow>
      <TableRow>
        <TableCell colSpan={6} sx={{ py: 0 }}>
          <Collapse in={open} timeout="auto" unmountOnExit>
            <Box sx={{ m: 2 }}>
              {obj.description && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>{obj.description}</Typography>
              )}
              {obj.fields.length > 0 ? (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell><strong>Field Name</strong></TableCell>
                      <TableCell><strong>Label</strong></TableCell>
                      <TableCell><strong>Type</strong></TableCell>
                      <TableCell><strong>Required</strong></TableCell>
                      <TableCell><strong>Unique</strong></TableCell>
                      <TableCell><strong>Options</strong></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {obj.fields.map((f) => (
                      <TableRow key={f.id}>
                        <TableCell><code>{f.name}</code></TableCell>
                        <TableCell>{f.label}</TableCell>
                        <TableCell>
                          <Chip label={f.fieldType} size="small" color={fieldTypeColor(f.fieldType)} variant="outlined" />
                        </TableCell>
                        <TableCell>{f.isRequired ? '✓' : '–'}</TableCell>
                        <TableCell>{f.isUnique ? '✓' : '–'}</TableCell>
                        <TableCell>
                          {Array.isArray(f.options) ? f.options.join(', ') : '–'}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              ) : (
                <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                  No fields defined. Use AI Config to add fields to this object.
                </Typography>
              )}
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}

/* ==== Main Page ==== */
const ObjectManagerPage: React.FC = () => {
  const [tab, setTab] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [objects, setObjects] = useState<CrmObject[]>([]);
  const [pipelines, setPipelines] = useState<Pipeline[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [dashboards, setDashboards] = useState<DashboardData[]>([]);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const [metaRes, wfRes, dashRes] = await Promise.allSettled([
          api.get('/api/ai/metadata'),
          api.get('/api/ai/metadata/workflows'),
          api.get('/api/ai/metadata/dashboards'),
        ]);
        if (metaRes.status === 'fulfilled') {
          const d = metaRes.value.data;
          setObjects(d.objects || []);
          setPipelines(d.pipelines || []);
          setRoles(d.roles || []);
        }
        if (wfRes.status === 'fulfilled') {
          setWorkflows(Array.isArray(wfRes.value.data) ? wfRes.value.data : []);
        }
        if (dashRes.status === 'fulfilled') {
          setDashboards(Array.isArray(dashRes.value.data) ? dashRes.value.data : []);
        }
      } catch (err: any) {
        setError(err?.message || 'Failed to load metadata');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const customObjects = objects.filter((o) => !o.isSystem);
  const systemObjects = objects.filter((o) => o.isSystem);

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title="Object Manager"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Object Manager' }]}
      />

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      )}

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {!loading && (
        <>
          {/* Summary Cards */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            {[
              { label: 'Custom Objects', count: customObjects.length, icon: <ObjectIcon />, color: '#7c3aed' },
              { label: 'System Objects', count: systemObjects.length, icon: <ObjectIcon />, color: '#6b7280' },
              { label: 'Pipelines', count: pipelines.length, icon: <PipelineIcon />, color: '#2563eb' },
              { label: 'Workflows', count: workflows.length, icon: <WorkflowIcon />, color: '#059669' },
              { label: 'Dashboards', count: dashboards.length, icon: <DashboardIcon />, color: '#d97706' },
              { label: 'Roles', count: roles.length, icon: <RoleIcon />, color: '#dc2626' },
            ].map((s) => (
              <Grid item xs={6} sm={4} md={2} key={s.label}>
                <Card variant="outlined">
                  <CardContent sx={{ textAlign: 'center', py: 2, '&:last-child': { pb: 2 } }}>
                    <Box sx={{ color: s.color, mb: 0.5 }}>{s.icon}</Box>
                    <Typography variant="h5" fontWeight={700}>{s.count}</Typography>
                    <Typography variant="caption" color="text.secondary">{s.label}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>

          {/* Tabs */}
          <Paper variant="outlined" sx={{ borderRadius: 2 }}>
            <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ px: 2, borderBottom: 1, borderColor: 'divider' }}>
              <Tab label={`Custom Objects (${customObjects.length})`} />
              <Tab label={`System Objects (${systemObjects.length})`} />
              <Tab label={`Pipelines (${pipelines.length})`} />
              <Tab label={`Workflows (${workflows.length})`} />
              <Tab label={`Dashboards (${dashboards.length})`} />
              <Tab label={`Roles (${roles.length})`} />
            </Tabs>

            {/* Tab 0: Custom Objects */}
            <TabPanel value={tab} index={0}>
              {customObjects.length === 0 ? (
                <Box sx={{ p: 4, textAlign: 'center' }}>
                  <Typography color="text.secondary">No custom objects yet. Go to <strong>AI Config</strong> to create one.</Typography>
                </Box>
              ) : (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell width={50} />
                        <TableCell>Object</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell align="center">Fields</TableCell>
                        <TableCell>Active</TableCell>
                        <TableCell>Created</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {customObjects.map((obj) => <ObjectRow key={obj.id} obj={obj} />)}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </TabPanel>

            {/* Tab 1: System Objects */}
            <TabPanel value={tab} index={1}>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell width={50} />
                      <TableCell>Object</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell align="center">Fields</TableCell>
                      <TableCell>Active</TableCell>
                      <TableCell>Created</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {systemObjects.map((obj) => <ObjectRow key={obj.id} obj={obj} />)}
                  </TableBody>
                </Table>
              </TableContainer>
            </TabPanel>

            {/* Tab 2: Pipelines */}
            <TabPanel value={tab} index={2}>
              {pipelines.length === 0 ? (
                <Box sx={{ p: 4, textAlign: 'center' }}>
                  <Typography color="text.secondary">No pipelines configured.</Typography>
                </Box>
              ) : (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Pipeline Name</TableCell>
                        <TableCell>Object</TableCell>
                        <TableCell>Default</TableCell>
                        <TableCell align="center">Stages</TableCell>
                        <TableCell>Stage Details</TableCell>
                        <TableCell>Created</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {pipelines.map((p) => (
                        <TableRow key={p.id} hover>
                          <TableCell>
                            <Typography fontWeight={600}>{p.name}</Typography>
                            {p.description && <Typography variant="caption" color="text.secondary">{p.description}</Typography>}
                          </TableCell>
                          <TableCell>{p.objectName}</TableCell>
                          <TableCell>{p.isDefault ? <Chip label="Default" size="small" color="primary" /> : '–'}</TableCell>
                          <TableCell align="center">{p.stages.length}</TableCell>
                          <TableCell>
                            {p.stages.length > 0 ? (
                              <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                                {p.stages.map((s) => (
                                  <Tooltip key={s.id} title={`${s.probability}% probability${s.isClosed ? ' (Closed)' : ''}${s.isWon ? ' (Won)' : ''}`}>
                                    <Chip
                                      label={s.name}
                                      size="small"
                                      variant="outlined"
                                      sx={s.color ? { borderColor: s.color, color: s.color } : {}}
                                    />
                                  </Tooltip>
                                ))}
                              </Box>
                            ) : '–'}
                          </TableCell>
                          <TableCell>{new Date(p.createdAt).toLocaleDateString()}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </TabPanel>

            {/* Tab 3: Workflows */}
            <TabPanel value={tab} index={3}>
              {workflows.length === 0 ? (
                <Box sx={{ p: 4, textAlign: 'center' }}>
                  <Typography color="text.secondary">No workflows configured.</Typography>
                </Box>
              ) : (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Workflow Name</TableCell>
                        <TableCell>Object</TableCell>
                        <TableCell>Trigger</TableCell>
                        <TableCell>Active</TableCell>
                        <TableCell>Created</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {workflows.map((w) => (
                        <TableRow key={w.id} hover>
                          <TableCell>
                            <Typography fontWeight={600}>{w.name}</Typography>
                            {w.description && <Typography variant="caption" color="text.secondary">{w.description}</Typography>}
                          </TableCell>
                          <TableCell>{w.objectName}</TableCell>
                          <TableCell>
                            <Chip label={w.triggerType.replace(/_/g, ' ')} size="small" color="info" variant="outlined" />
                          </TableCell>
                          <TableCell>
                            {w.isActive ? <ActiveIcon color="success" fontSize="small" /> : <InactiveIcon color="error" fontSize="small" />}
                          </TableCell>
                          <TableCell>{new Date(w.createdAt).toLocaleDateString()}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </TabPanel>

            {/* Tab 4: Dashboards */}
            <TabPanel value={tab} index={4}>
              {dashboards.length === 0 ? (
                <Box sx={{ p: 4, textAlign: 'center' }}>
                  <Typography color="text.secondary">No dashboards configured.</Typography>
                </Box>
              ) : (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Dashboard Name</TableCell>
                        <TableCell align="center">Widgets</TableCell>
                        <TableCell>Widget Details</TableCell>
                        <TableCell>Created</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {dashboards.map((d) => (
                        <TableRow key={d.id} hover>
                          <TableCell>
                            <Typography fontWeight={600}>{d.name}</Typography>
                            {d.description && <Typography variant="caption" color="text.secondary">{d.description}</Typography>}
                          </TableCell>
                          <TableCell align="center">{d.widgets.length}</TableCell>
                          <TableCell>
                            <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                              {d.widgets.map((w) => (
                                <Chip key={w.id} label={`${w.title} (${w.widgetType})`} size="small" variant="outlined" />
                              ))}
                            </Box>
                          </TableCell>
                          <TableCell>{new Date(d.createdAt).toLocaleDateString()}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </TabPanel>

            {/* Tab 5: Roles */}
            <TabPanel value={tab} index={5}>
              {roles.length === 0 ? (
                <Box sx={{ p: 4, textAlign: 'center' }}>
                  <Typography color="text.secondary">No roles configured.</Typography>
                </Box>
              ) : (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Role Name</TableCell>
                        <TableCell>Type</TableCell>
                        <TableCell>Permissions</TableCell>
                        <TableCell>Created</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {roles.map((r) => (
                        <TableRow key={r.id} hover>
                          <TableCell>
                            <Typography fontWeight={600}>{r.name}</Typography>
                            {r.description && <Typography variant="caption" color="text.secondary">{r.description}</Typography>}
                          </TableCell>
                          <TableCell>
                            <Chip label={r.isSystem ? 'System' : 'Custom'} size="small" color={r.isSystem ? 'default' : 'primary'} variant="outlined" />
                          </TableCell>
                          <TableCell>
                            {r.permissions.length > 0 ? (
                              <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                                {r.permissions.map((p) => {
                                  const perms = [p.canCreate && 'C', p.canRead && 'R', p.canUpdate && 'U', p.canDelete && 'D'].filter(Boolean).join('');
                                  return (
                                    <Tooltip key={p.id} title={`Create: ${p.canCreate ? '✓' : '✗'} | Read: ${p.canRead ? '✓' : '✗'} | Update: ${p.canUpdate ? '✓' : '✗'} | Delete: ${p.canDelete ? '✓' : '✗'}`}>
                                      <Chip label={`${p.objectName}: ${perms}`} size="small" variant="outlined" />
                                    </Tooltip>
                                  );
                                })}
                              </Box>
                            ) : (
                              <Typography variant="body2" color="text.secondary">No object permissions</Typography>
                            )}
                          </TableCell>
                          <TableCell>{new Date(r.createdAt).toLocaleDateString()}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </TabPanel>
          </Paper>
        </>
      )}
    </Box>
  );
};

export default ObjectManagerPage;
