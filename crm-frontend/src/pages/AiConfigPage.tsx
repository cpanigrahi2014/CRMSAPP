import React from 'react';
import { Box, Typography } from '@mui/material';
import AiConfigChat from '../components/AiConfigChat';
import PageHeader from '../components/PageHeader';

const AiConfigPage: React.FC = () => {
  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      <PageHeader
        title="AI Configuration Agent"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'AI Config' }]}
      />
      <Box
        sx={{
          flex: 1,
          mx: 3,
          mb: 3,
          borderRadius: 2,
          overflow: 'hidden',
          border: '1px solid',
          borderColor: 'divider',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <AiConfigChat />
      </Box>
    </Box>
  );
};

export default AiConfigPage;
