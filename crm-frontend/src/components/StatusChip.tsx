/* ============================================================
   StatusChip – coloured chip for entity statuses
   ============================================================ */
import React from 'react';
import { Chip, ChipProps } from '@mui/material';

type StatusColor = 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';

const statusColorMap: Record<string, StatusColor> = {
  // Lead statuses
  NEW: 'info',
  CONTACTED: 'primary',
  QUALIFIED: 'success',
  UNQUALIFIED: 'warning',
  CONVERTED: 'secondary',
  // Opportunity stages
  PROSPECTING: 'info',
  QUALIFICATION: 'primary',
  PROPOSAL: 'warning',
  NEGOTIATION: 'secondary',
  CLOSED_WON: 'success',
  CLOSED_LOST: 'error',
  // Case statuses
  OPEN: 'info',
  IN_PROGRESS: 'primary',
  ESCALATED: 'error',
  RESOLVED: 'success',
  CLOSED: 'default',
  // Campaign status
  PLANNED: 'info',
  ACTIVE: 'success',
  COMPLETED: 'default',
  ABORTED: 'error',
  // General
  HIGH: 'error',
  MEDIUM: 'warning',
  LOW: 'success',
};

interface Props extends Omit<ChipProps, 'color'> {
  status: string;
}

const StatusChip: React.FC<Props> = ({ status, ...rest }) => {
  const safeStatus = status ?? '';
  const color = statusColorMap[safeStatus] ?? 'default';
  const label = safeStatus.replace(/_/g, ' ') || '—';
  return <Chip label={label} color={color} size="small" variant="outlined" {...rest} />;
};

export default StatusChip;
