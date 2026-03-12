/* LoadingSpinner – centred circular progress */
import React from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';

interface Props {
  message?: string;
  fullPage?: boolean;
}

const LoadingSpinner: React.FC<Props> = ({ message = 'Loading…', fullPage = false }) => (
  <Box
    sx={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 2,
      ...(fullPage ? { minHeight: '60vh' } : { py: 6 }),
    }}
  >
    <CircularProgress size={40} />
    <Typography variant="body2" color="text.secondary">
      {message}
    </Typography>
  </Box>
);

export default LoadingSpinner;
