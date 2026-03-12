/* ============================================================
   ChartWidget – card wrapper around a Recharts chart
   ============================================================ */
import React, { ReactNode } from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  IconButton,
  Tooltip,
} from '@mui/material';
import { MoreVert as MoreIcon } from '@mui/icons-material';

interface Props {
  title: string;
  subtitle?: string;
  height?: number;
  children: ReactNode;
  action?: ReactNode;
}

const ChartWidget: React.FC<Props> = ({
  title,
  subtitle,
  height = 300,
  children,
  action,
}) => (
  <Card sx={{ height: '100%' }}>
    <CardContent>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
        <Box>
          <Typography variant="h6" fontWeight={600}>
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="body2" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
        {action || (
          <Tooltip title="Options">
            <IconButton size="small">
              <MoreIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
      </Box>
      <Box sx={{ width: '100%', height, overflow: 'visible' }}>{children}</Box>
    </CardContent>
  </Card>
);

export default ChartWidget;
