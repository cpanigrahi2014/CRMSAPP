/* ============================================================
   ModalForm – reusable dialog with a form inside
   ============================================================ */
import React, { ReactNode } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  IconButton,
  Box,
} from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';

interface Props {
  open: boolean;
  title: string;
  onClose: () => void;
  onSubmit: (e: React.FormEvent) => void;
  submitLabel?: string;
  loading?: boolean;
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg';
  children: ReactNode;
}

const ModalForm: React.FC<Props> = ({
  open,
  title,
  onClose,
  onSubmit,
  submitLabel = 'Save',
  loading = false,
  maxWidth = 'sm',
  children,
}) => (
  <Dialog open={open} onClose={onClose} maxWidth={maxWidth} fullWidth>
    <Box component="form" onSubmit={onSubmit} noValidate>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        {title}
        <IconButton size="small" onClick={onClose}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers>{children}</DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={loading}>
          Cancel
        </Button>
        <Button type="submit" variant="contained" disabled={loading}>
          {loading ? 'Saving…' : submitLabel}
        </Button>
      </DialogActions>
    </Box>
  </Dialog>
);

export default ModalForm;
