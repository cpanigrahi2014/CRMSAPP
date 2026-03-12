/* ============================================================
   ForgotPasswordPage – sends reset link via backend API
   ============================================================ */
import React, { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  TextField, Button, Link, Typography, Box, Alert,
  InputAdornment, CircularProgress,
} from '@mui/material';
import { Email as EmailIcon } from '@mui/icons-material';
import api from '../../services/api';

const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.post('/api/v1/auth/forgot-password', {
        email,
        tenantId: 'default',
      });
      setSubmitted(true);
    } catch (err: any) {
      // Always show success to avoid email enumeration
      setSubmitted(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit} noValidate>
      <Typography variant="h6" fontWeight={600} gutterBottom>
        Reset Password
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Enter your email and we&apos;ll send you a reset link
      </Typography>

      {submitted && (
        <Alert severity="success" sx={{ mb: 2 }}>
          If an account with that email exists, a reset link has been sent. Check your inbox.
        </Alert>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {!submitted && (
        <>
          <TextField
            fullWidth
            label="Email Address"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            sx={{ mb: 3 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <EmailIcon fontSize="small" color="action" />
                </InputAdornment>
              ),
            }}
          />

          <Button
            type="submit"
            variant="contained"
            fullWidth
            size="large"
            disabled={loading || !email}
            sx={{ mb: 2 }}
          >
            {loading ? <CircularProgress size={24} color="inherit" /> : 'Send Reset Link'}
          </Button>
        </>
      )}

      <Typography variant="body2" textAlign="center">
        Remember your password?{' '}
        <Link component={RouterLink} to="/auth/login" underline="hover" fontWeight={600}>
          Sign In
        </Link>
      </Typography>
    </Box>
  );
};

export default ForgotPasswordPage;
