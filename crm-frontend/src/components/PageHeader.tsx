/* ============================================================
   PageHeader – title + breadcrumb + optional action buttons
   ============================================================ */
import React, { ReactNode } from 'react';
import { Box, Typography, Breadcrumbs, Link as MuiLink } from '@mui/material';
import { Link } from 'react-router-dom';
import { NavigateNext } from '@mui/icons-material';

interface Crumb {
  label: string;
  to?: string;
}

interface Props {
  title: string;
  breadcrumbs?: Crumb[];
  action?: ReactNode;
}

const PageHeader: React.FC<Props> = ({ title, breadcrumbs = [], action }) => (
  <Box
    sx={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'flex-start',
      flexWrap: 'wrap',
      gap: 1,
      mb: 3,
    }}
  >
    <Box>
      {breadcrumbs.length > 0 && (
        <Breadcrumbs separator={<NavigateNext fontSize="small" />} sx={{ mb: 0.5 }}>
          {breadcrumbs.map((crumb, idx) =>
            crumb.to ? (
              <MuiLink
                key={idx}
                component={Link}
                to={crumb.to}
                underline="hover"
                color="inherit"
                variant="body2"
              >
                {crumb.label}
              </MuiLink>
            ) : (
              <Typography key={idx} variant="body2" color="text.primary">
                {crumb.label}
              </Typography>
            ),
          )}
        </Breadcrumbs>
      )}
      <Typography variant="h5" fontWeight={700}>
        {title}
      </Typography>
    </Box>
    {action && <Box>{action}</Box>}
  </Box>
);

export default PageHeader;
