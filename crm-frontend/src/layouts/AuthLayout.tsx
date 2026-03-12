/* ============================================================
   AuthLayout – centred card for login / register / forgot-pw
   ============================================================ */
import React from 'react';
import { Outlet, Navigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  useTheme,
} from '@mui/material';
import { useAppSelector } from '../hooks/useRedux';

const AuthLayout: React.FC = () => {
  const theme = useTheme();
  const { isAuthenticated } = useAppSelector((s) => s.auth);

  // If already logged-in, skip straight to dashboard
  if (isAuthenticated) return <Navigate to="/dashboard" replace />;

  return (
    <Box
      sx={{
        display: 'flex',
        minHeight: '100vh',
        alignItems: 'center',
        justifyContent: 'center',
        background:
          theme.palette.mode === 'dark'
            ? 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)'
            : 'linear-gradient(135deg, #e3f2fd 0%, #f5f5f5 100%)',
        p: 2,
      }}
    >
      <Card
        elevation={8}
        sx={{
          width: '100%',
          maxWidth: 460,
          borderRadius: 3,
          overflow: 'visible',
        }}
      >
        <CardContent sx={{ p: 4 }}>
          {/* Brand */}
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Typography
              variant="h4"
              fontWeight={800}
              color="primary"
              gutterBottom
            >
              CRM Platform
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Enterprise Sales Suite
            </Typography>
          </Box>

          {/* Nested route content (Login / Register / ForgotPassword) */}
          <Outlet />
        </CardContent>
      </Card>
    </Box>
  );
};

export default AuthLayout;
