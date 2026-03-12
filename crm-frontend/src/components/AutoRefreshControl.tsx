/* ============================================================
   AutoRefreshControl – toggle-able auto-refresh with interval
   selector and "last updated" timestamp for real-time analytics
   ============================================================ */
import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Box,
  Switch,
  FormControlLabel,
  Select,
  MenuItem,
  Typography,
  Chip,
  Tooltip,
  IconButton,
} from '@mui/material';
import { Refresh, FiberManualRecord } from '@mui/icons-material';

interface AutoRefreshControlProps {
  onRefresh: () => void | Promise<void>;
  loading?: boolean;
}

const INTERVALS = [
  { value: 15, label: '15s' },
  { value: 30, label: '30s' },
  { value: 60, label: '1m' },
  { value: 300, label: '5m' },
];

const AutoRefreshControl: React.FC<AutoRefreshControlProps> = ({ onRefresh, loading }) => {
  const [enabled, setEnabled] = useState(false);
  const [interval, setInterval_] = useState(30);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
  const [countdown, setCountdown] = useState(30);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const doRefresh = useCallback(async () => {
    await onRefresh();
    setLastUpdated(new Date());
    setCountdown(interval);
  }, [onRefresh, interval]);

  // Auto-refresh timer
  useEffect(() => {
    if (timerRef.current) clearInterval(timerRef.current);
    if (countdownRef.current) clearInterval(countdownRef.current);

    if (enabled) {
      setCountdown(interval);
      timerRef.current = setInterval(() => {
        doRefresh();
      }, interval * 1000);
      countdownRef.current = setInterval(() => {
        setCountdown((prev) => Math.max(0, prev - 1));
      }, 1000);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      if (countdownRef.current) clearInterval(countdownRef.current);
    };
  }, [enabled, interval, doRefresh]);

  const handleManualRefresh = async () => {
    await doRefresh();
  };

  const formatTime = (date: Date) =>
    date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' });

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1.5,
        flexWrap: 'wrap',
      }}
    >
      {/* Live indicator */}
      {enabled && (
        <Chip
          icon={<FiberManualRecord sx={{ fontSize: 10, color: '#059669', animation: 'pulse 2s infinite' }} />}
          label={`Live · ${countdown}s`}
          size="small"
          variant="outlined"
          color="success"
          sx={{
            '@keyframes pulse': {
              '0%, 100%': { opacity: 1 },
              '50%': { opacity: 0.3 },
            },
          }}
        />
      )}

      {/* Auto-refresh toggle */}
      <FormControlLabel
        control={
          <Switch
            size="small"
            checked={enabled}
            onChange={(e) => setEnabled(e.target.checked)}
          />
        }
        label={<Typography variant="caption">Auto-refresh</Typography>}
        sx={{ mr: 0 }}
      />

      {/* Interval selector */}
      {enabled && (
        <Select
          size="small"
          value={interval}
          onChange={(e) => {
            setInterval_(Number(e.target.value));
            setCountdown(Number(e.target.value));
          }}
          sx={{ minWidth: 70, '& .MuiSelect-select': { py: 0.5, fontSize: '0.8rem' } }}
        >
          {INTERVALS.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>
          ))}
        </Select>
      )}

      {/* Manual refresh */}
      <Tooltip title="Refresh now">
        <IconButton
          size="small"
          onClick={handleManualRefresh}
          disabled={loading}
          sx={{ animation: loading ? 'spin 1s linear infinite' : 'none', '@keyframes spin': { '100%': { transform: 'rotate(360deg)' } } }}
        >
          <Refresh fontSize="small" />
        </IconButton>
      </Tooltip>

      {/* Last updated */}
      <Typography variant="caption" color="text.secondary">
        Updated {formatTime(lastUpdated)}
      </Typography>
    </Box>
  );
};

export default AutoRefreshControl;
