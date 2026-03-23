/* ============================================================
   LoginPage – email + password form with MFA support
   ============================================================ */
import React, { useState, useEffect } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  TextField,
  Button,
  Link,
  Typography,
  Box,
  Alert,
  InputAdornment,
  IconButton,
} from '@mui/material';
import {
  Visibility,
  VisibilityOff,
  Email as EmailIcon,
  Lock as LockIcon,
  Business as BusinessIcon,
  PhonelinkLock as MfaIcon,
} from '@mui/icons-material';
import { useAppDispatch, useAppSelector } from '../../hooks/useRedux';
import { login, verifyMfa, clearError, clearMfa } from '../../store/slices/authSlice';

const LoginPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { loading, error, isAuthenticated, mfaRequired, mfaPending } = useAppSelector((s) => s.auth);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [tenantId, setTenantId] = useState('default');
  const [showPassword, setShowPassword] = useState(false);
  const [mfaCode, setMfaCode] = useState('');

  useEffect(() => {
    dispatch(clearError());
    dispatch(clearMfa());
  }, [dispatch]);

  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard', { replace: true });
  }, [isAuthenticated, navigate]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    dispatch(login({ email, password, tenantId }));
  };

  const handleMfaSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (mfaPending) {
      dispatch(verifyMfa({
        userId: mfaPending.userId,
        code: mfaCode,
        tenantId: mfaPending.tenantId,
        mfaToken: mfaPending.mfaToken,
      }));
    }
  };

  // ── MFA Code Entry Step ──
  if (mfaRequired && mfaPending) {
    return (
      <Box component="form" onSubmit={handleMfaSubmit} noValidate>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
          <MfaIcon color="primary" />
          <Typography variant="h6" fontWeight={600}>
            Two-Factor Authentication
          </Typography>
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Enter the 6-digit code from your authenticator app for {mfaPending.email}
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <TextField
          fullWidth
          label="Authentication Code"
          value={mfaCode}
          onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
          required
          autoFocus
          placeholder="000000"
          sx={{ mb: 3 }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <LockIcon fontSize="small" color="action" />
              </InputAdornment>
            ),
          }}
          inputProps={{ maxLength: 6, style: { letterSpacing: '0.5em', textAlign: 'center', fontSize: '1.2rem' } }}
        />

        <Button type="submit" variant="contained" fullWidth size="large" disabled={loading || mfaCode.length !== 6} sx={{ mb: 2 }}>
          {loading ? 'Verifying…' : 'Verify & Sign In'}
        </Button>

        <Button fullWidth variant="text" onClick={() => { dispatch(clearMfa()); setMfaCode(''); }}>
          Back to Login
        </Button>
      </Box>
    );
  }

  // ── Standard Login Step ──
  return (
    <Box component="form" onSubmit={handleSubmit} noValidate>
      <Typography variant="h6" fontWeight={600} gutterBottom>
        Sign In
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Enter your credentials to access the CRM
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <TextField
        fullWidth
        label="Email Address"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
        sx={{ mb: 2 }}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <EmailIcon fontSize="small" color="action" />
            </InputAdornment>
          ),
        }}
      />

      <TextField
        fullWidth
        label="Password"
        type={showPassword ? 'text' : 'password'}
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        required
        sx={{ mb: 1 }}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <LockIcon fontSize="small" color="action" />
            </InputAdornment>
          ),
          endAdornment: (
            <InputAdornment position="end">
              <IconButton size="small" onClick={() => setShowPassword((p) => !p)}>
                {showPassword ? <VisibilityOff fontSize="small" /> : <Visibility fontSize="small" />}
              </IconButton>
            </InputAdornment>
          ),
        }}
      />

      <TextField
        fullWidth
        label="Tenant ID"
        value={tenantId}
        onChange={(e) => setTenantId(e.target.value)}
        required
        sx={{ mb: 2 }}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <BusinessIcon fontSize="small" color="action" />
            </InputAdornment>
          ),
        }}
      />

      <Box sx={{ textAlign: 'right', mb: 2 }}>
        <Link component={RouterLink} to="/auth/forgot-password" variant="body2" underline="hover">
          Forgot password?
        </Link>
      </Box>

      <Button type="submit" variant="contained" fullWidth size="large" disabled={loading} sx={{ mb: 2 }}>
        {loading ? 'Signing in…' : 'Sign In'}
      </Button>

      <Typography variant="body2" textAlign="center">
        Don&apos;t have an account?{' '}
        <Link component={RouterLink} to="/auth/register" underline="hover" fontWeight={600}>
          Sign Up
        </Link>
      </Typography>
    </Box>
  );
};

export default LoginPage;
