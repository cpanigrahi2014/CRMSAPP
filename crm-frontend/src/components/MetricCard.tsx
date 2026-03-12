/* ============================================================
   MetricCard – colourful KPI card for the dashboard
   ============================================================ */
import React, { ReactNode } from 'react';
import { Card, CardContent, Typography, Box, Avatar } from '@mui/material';
import { TrendingUp, TrendingDown } from '@mui/icons-material';

interface Props {
  title: string;
  value: string | number;
  change?: number;          // percentage change (positive = good)
  icon: ReactNode;
  color?: string;
}

const MetricCard: React.FC<Props> = ({
  title,
  value,
  change,
  icon,
  color = '#1976d2',
}) => (
  <Card sx={{ height: '100%' }}>
    <CardContent sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
      <Avatar sx={{ bgcolor: `${color}20`, color, width: 52, height: 52 }}>{icon}</Avatar>
      <Box sx={{ flex: 1 }}>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {title}
        </Typography>
        <Typography variant="h5" fontWeight={700}>
          {value}
        </Typography>
        {change !== undefined && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 0.5 }}>
            {change >= 0 ? (
              <TrendingUp sx={{ fontSize: 16, color: 'success.main' }} />
            ) : (
              <TrendingDown sx={{ fontSize: 16, color: 'error.main' }} />
            )}
            <Typography variant="caption" color={change >= 0 ? 'success.main' : 'error.main'}>
              {change >= 0 ? '+' : ''}
              {change}%
            </Typography>
            <Typography variant="caption" color="text.secondary">
              vs last month
            </Typography>
          </Box>
        )}
      </Box>
    </CardContent>
  </Card>
);

export default MetricCard;
