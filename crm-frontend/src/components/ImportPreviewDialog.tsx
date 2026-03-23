/* ============================================================
   ImportPreviewDialog – CSV import with AI field detection
   Shows industry-aware field mapping preview before importing
   ============================================================ */
import React, { useState, useEffect, useCallback } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, Box, Typography, Table, TableHead, TableBody,
  TableRow, TableCell, TableContainer, Paper, Chip,
  CircularProgress, Alert, TextField, MenuItem, IconButton,
  Tooltip, Stack, Switch, FormControlLabel,
} from '@mui/material';
import {
  CloudUpload as UploadIcon,
  AutoFixHigh as AiIcon,
  CheckCircle as MappedIcon,
  Warning as UnmappedIcon,
  Business as IndustryIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import { detectCsvFields, getSupportedIndustries } from '../services/zeroConfigService';
import type { CsvFieldDetectionResult, CsvFieldMapping, IndustryFieldInfo } from '../types';

interface ImportPreviewDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirmImport: (file: File) => void;
  entityType: 'lead' | 'account' | 'contact' | 'opportunity';
  title?: string;
}

const INDUSTRY_LABELS: Record<string, string> = {
  real_estate: 'Real Estate',
  healthcare: 'Healthcare',
  technology: 'Technology / SaaS',
  finance: 'Finance / Insurance',
  education: 'Education',
  manufacturing: 'Manufacturing',
};

const confidenceColor = (c: number) => (c >= 0.9 ? 'success' : c >= 0.7 ? 'warning' : 'error');

const ImportPreviewDialog: React.FC<ImportPreviewDialogProps> = ({
  open, onClose, onConfirmImport, entityType, title,
}) => {
  const [file, setFile] = useState<File | null>(null);
  const [csvPreview, setCsvPreview] = useState<string>('');
  const [detecting, setDetecting] = useState(false);
  const [result, setResult] = useState<CsvFieldDetectionResult | null>(null);
  const [industry, setIndustry] = useState<string>('');
  const [industries, setIndustries] = useState<string[]>([]);
  const [enabledMappings, setEnabledMappings] = useState<Record<string, boolean>>({});
  const [error, setError] = useState('');

  // Load supported industries on mount
  useEffect(() => {
    if (open) {
      getSupportedIndustries()
        .then(setIndustries)
        .catch(() => setIndustries(Object.keys(INDUSTRY_LABELS)));
    }
  }, [open]);

  // Reset state when dialog closes
  useEffect(() => {
    if (!open) {
      setFile(null);
      setCsvPreview('');
      setResult(null);
      setIndustry('');
      setEnabledMappings({});
      setError('');
    }
  }, [open]);

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) return;
    setFile(f);
    setResult(null);
    setError('');

    const reader = new FileReader();
    reader.onload = (ev) => {
      const text = ev.target?.result as string;
      // Take first 10 lines for preview
      const lines = text.split('\n').slice(0, 10).join('\n');
      setCsvPreview(lines);
    };
    reader.readAsText(f);
    e.target.value = '';
  }, []);

  const handleDetect = useCallback(async () => {
    if (!file) return;
    setDetecting(true);
    setError('');
    try {
      const reader = new FileReader();
      const csvContent = await new Promise<string>((resolve, reject) => {
        reader.onload = (e) => resolve(e.target?.result as string);
        reader.onerror = reject;
        reader.readAsText(file);
      });

      const detection = await detectCsvFields(
        csvContent, entityType, industry || undefined, undefined
      );
      setResult(detection);

      // Enable all mappings by default
      const map: Record<string, boolean> = {};
      detection.fieldMappings.forEach(m => { map[m.csvHeader] = true; });
      setEnabledMappings(map);
    } catch {
      setError('Field detection failed. The file will be imported with default column mapping.');
    } finally {
      setDetecting(false);
    }
  }, [file, entityType, industry]);

  // Auto-detect when file is selected
  useEffect(() => {
    if (file && csvPreview) {
      handleDetect();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [file]);

  const handleConfirm = () => {
    if (file) {
      onConfirmImport(file);
      onClose();
    }
  };

  const toggleMapping = (csvHeader: string) => {
    setEnabledMappings(prev => ({ ...prev, [csvHeader]: !prev[csvHeader] }));
  };

  const mappedCount = result?.fieldMappings.filter(m => enabledMappings[m.csvHeader]).length ?? 0;
  const industryMappedCount = result?.fieldMappings.filter(
    m => m.isIndustryField && enabledMappings[m.csvHeader]
  ).length ?? 0;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <UploadIcon color="primary" />
          <Typography variant="h6">
            {title || `Import ${entityType.charAt(0).toUpperCase() + entityType.slice(1)}s from CSV`}
          </Typography>
        </Box>
        <IconButton onClick={onClose} size="small"><CloseIcon /></IconButton>
      </DialogTitle>

      <DialogContent dividers>
        {/* ── Industry Selection ── */}
        <Box sx={{ mb: 2 }}>
          <Stack direction="row" spacing={2} alignItems="center">
            <IndustryIcon color="action" />
            <TextField
              select
              size="small"
              label="Industry (optional)"
              value={industry}
              onChange={(e) => { setIndustry(e.target.value); setResult(null); }}
              sx={{ minWidth: 220 }}
              helperText="Select industry to show relevant fields"
            >
              <MenuItem value="">None (default fields)</MenuItem>
              {industries.map(ind => (
                <MenuItem key={ind} value={ind}>
                  {INDUSTRY_LABELS[ind] || ind.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())}
                </MenuItem>
              ))}
            </TextField>
            {industry && (
              <Chip
                icon={<IndustryIcon />}
                label={INDUSTRY_LABELS[industry] || industry}
                color="primary"
                variant="outlined"
                onDelete={() => { setIndustry(''); setResult(null); }}
              />
            )}
          </Stack>
        </Box>

        {/* ── File Selection ── */}
        {!file && (
          <Paper
            sx={{
              p: 4, textAlign: 'center', border: '2px dashed',
              borderColor: 'divider', borderRadius: 2, cursor: 'pointer',
              '&:hover': { borderColor: 'primary.main', bgcolor: 'action.hover' },
            }}
            component="label"
          >
            <UploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
            <Typography variant="h6" color="text.secondary">
              Click to select a CSV file
            </Typography>
            <Typography variant="body2" color="text.secondary">
              AI will auto-detect columns and map them to {entityType} fields
              {industry ? ` (including ${INDUSTRY_LABELS[industry] || industry} fields)` : ''}
            </Typography>
            <input type="file" accept=".csv" hidden onChange={handleFileSelect} />
          </Paper>
        )}

        {/* ── File Selected / Detecting ── */}
        {file && (
          <Box sx={{ mb: 2 }}>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
              <Chip label={file.name} onDelete={() => { setFile(null); setResult(null); setCsvPreview(''); }} />
              <Typography variant="body2" color="text.secondary">
                ({(file.size / 1024).toFixed(1)} KB)
              </Typography>
              <Button
                size="small"
                variant="outlined"
                startIcon={<AiIcon />}
                onClick={handleDetect}
                disabled={detecting}
              >
                Re-detect
              </Button>
            </Stack>
          </Box>
        )}

        {/* ── Loading ── */}
        {detecting && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, my: 3, justifyContent: 'center' }}>
            <CircularProgress size={24} />
            <Typography>AI is detecting fields{industry ? ` with ${INDUSTRY_LABELS[industry] || industry} template` : ''}...</Typography>
          </Box>
        )}

        {/* ── Error ── */}
        {error && <Alert severity="warning" sx={{ mb: 2 }}>{error}</Alert>}

        {/* ── Detection Results ── */}
        {result && !detecting && (
          <>
            {/* Summary chips */}
            <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap">
              <Chip
                icon={<MappedIcon />}
                label={`${mappedCount}/${result.totalColumns} columns mapped`}
                color="success"
                size="small"
              />
              {result.unmappedColumns.length > 0 && (
                <Chip
                  icon={<UnmappedIcon />}
                  label={`${result.unmappedColumns.length} unmapped`}
                  color="warning"
                  variant="outlined"
                  size="small"
                />
              )}
              {industry && (
                <Chip
                  icon={<IndustryIcon />}
                  label={`${industryMappedCount} industry fields matched`}
                  color="info"
                  variant="outlined"
                  size="small"
                />
              )}
              {result.industryFields && result.industryFields.length > 0 && (
                <Chip
                  label={`${result.industryFields.length} ${INDUSTRY_LABELS[industry] || industry} fields available`}
                  color="primary"
                  variant="outlined"
                  size="small"
                />
              )}
            </Stack>

            {/* ── Mapping Table ── */}
            <TableContainer component={Paper} variant="outlined" sx={{ maxHeight: 350 }}>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox" sx={{ fontWeight: 700 }}>Import</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>CSV Column</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>CRM Field</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Type</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Confidence</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Sample</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Source</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {result.fieldMappings.map((m, i) => (
                    <TableRow
                      key={i}
                      sx={{
                        bgcolor: m.isIndustryField ? 'info.50' : m.isCustomField ? 'warning.50' : undefined,
                        opacity: enabledMappings[m.csvHeader] ? 1 : 0.5,
                      }}
                    >
                      <TableCell padding="checkbox">
                        <Switch
                          size="small"
                          checked={!!enabledMappings[m.csvHeader]}
                          onChange={() => toggleMapping(m.csvHeader)}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight={500}>{m.csvHeader}</Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={m.crmField}
                          size="small"
                          color={m.isIndustryField ? 'info' : m.isCustomField ? 'warning' : 'primary'}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption">{m.dataType}</Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={`${Math.round(m.confidence * 100)}%`}
                          size="small"
                          color={confidenceColor(m.confidence) as any}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary">
                          {m.sampleValue?.substring(0, 30)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {m.isIndustryField && (
                          <Tooltip title="Industry-specific field">
                            <Chip label="Industry" size="small" color="info" sx={{ height: 20, fontSize: '0.7rem' }} />
                          </Tooltip>
                        )}
                        {m.isCustomField && (
                          <Tooltip title="Custom field from AI Config">
                            <Chip label="Custom" size="small" color="warning" sx={{ height: 20, fontSize: '0.7rem' }} />
                          </Tooltip>
                        )}
                        {!m.isIndustryField && !m.isCustomField && (
                          <Chip label="Standard" size="small" sx={{ height: 20, fontSize: '0.7rem' }} />
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            {/* ── Unmapped Columns ── */}
            {result.unmappedColumns.length > 0 && (
              <Alert severity="info" sx={{ mt: 2 }}>
                <strong>Unmapped columns (will be skipped):</strong>{' '}
                {result.unmappedColumns.join(', ')}
                {industry
                  ? ' — These columns don\'t match standard or industry-specific fields.'
                  : ' — Try selecting an industry above to match more columns.'}
              </Alert>
            )}

            {/* ── Industry Fields Available ── */}
            {result.industryFields && result.industryFields.length > 0 && (
              <Paper sx={{ mt: 2, p: 1.5 }} variant="outlined">
                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                  <IndustryIcon sx={{ fontSize: 16, mr: 0.5, verticalAlign: 'text-bottom' }} />
                  {INDUSTRY_LABELS[industry] || industry} Fields Available for {entityType}
                </Typography>
                <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                  {result.industryFields.map((f, i) => {
                    const isMapped = result.fieldMappings.some(m => m.crmField === f.fieldName);
                    return (
                      <Chip
                        key={i}
                        label={`${f.label} (${f.fieldType})`}
                        size="small"
                        color={isMapped ? 'success' : 'default'}
                        variant={isMapped ? 'filled' : 'outlined'}
                        sx={{ mb: 0.5 }}
                      />
                    );
                  })}
                </Stack>
              </Paper>
            )}
          </>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          onClick={handleConfirm}
          disabled={!file}
          startIcon={<UploadIcon />}
        >
          {result ? `Import ${mappedCount} Mapped Fields` : 'Import CSV'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ImportPreviewDialog;
