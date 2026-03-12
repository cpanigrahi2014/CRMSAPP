import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  IconButton,
  Avatar,
  Chip,
  Button,
  CircularProgress,
  Divider,
  Tooltip,
  Fade,
  useTheme,
  alpha,
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import PersonIcon from '@mui/icons-material/Person';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import SettingsIcon from '@mui/icons-material/Settings';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import api from '../services/api';

// ─── Types ───────────────────────────────────────────────────────────────────

interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
  metadata?: {
    action?: string;
    status?: 'pending_confirmation' | 'created' | 'failed';
    auditLogId?: string;
    details?: Record<string, unknown>;
  };
}

// ─── Component ───────────────────────────────────────────────────────────────

const AiConfigChat: React.FC = () => {
  const theme = useTheme();
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 'welcome',
      role: 'assistant',
      content:
        'Hello! I\'m your CRM Configuration Agent. I can help you set up objects, fields, workflows, pipelines, dashboards, roles, and automation rules using natural language.\n\nTry something like:\n• "Create a field called Budget in Leads"\n• "Add pipeline stage Technical Review"\n• "Create a workflow that sends email when lead becomes qualified"\n• "Create dashboard showing monthly revenue"',
      timestamp: new Date(),
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  // ── Send Instruction ─────────────────────────────────────────────────────

  const sendInstruction = async () => {
    const text = input.trim();
    if (!text || loading) return;

    const userMsg: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: text,
      timestamp: new Date(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const res = await api.post('/api/ai/configure', {
        instruction: text,
        sessionId,
      });

      const data = res.data;
      if (data.sessionId && !sessionId) setSessionId(data.sessionId);

      const assistantMsg: Message = {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: data.message,
        timestamp: new Date(),
        metadata: {
          action: data.action,
          status: data.status,
          auditLogId: data.auditLogId,
          details: data.details,
        },
      };
      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err: any) {
      const errMsg: Message = {
        id: `error-${Date.now()}`,
        role: 'assistant',
        content: err.response?.data?.message || err.response?.data?.error || 'Something went wrong. Please try again.',
        timestamp: new Date(),
        metadata: { status: 'failed' },
      };
      setMessages((prev) => [...prev, errMsg]);
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  };

  // ── Confirm / Reject ──────────────────────────────────────────────────────

  const handleConfirm = async (auditLogId: string) => {
    setLoading(true);
    try {
      const res = await api.post('/api/ai/confirm', { auditLogId, sessionId });
      const data = res.data;
      const msg: Message = {
        id: `confirm-${Date.now()}`,
        role: 'assistant',
        content: data.message,
        timestamp: new Date(),
        metadata: { action: data.action, status: data.status },
      };
      setMessages((prev) => [...prev, msg]);
    } catch (err: any) {
      setMessages((prev) => [
        ...prev,
        {
          id: `cerr-${Date.now()}`,
          role: 'assistant',
          content: err.response?.data?.message || 'Confirmation failed.',
          timestamp: new Date(),
          metadata: { status: 'failed' },
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async (auditLogId: string) => {
    try {
      await api.post('/api/ai/reject', { auditLogId });
      setMessages((prev) => [
        ...prev,
        {
          id: `reject-${Date.now()}`,
          role: 'assistant',
          content: 'Action cancelled.',
          timestamp: new Date(),
        },
      ]);
    } catch {
      // silent
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendInstruction();
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  // ── Status Chip ───────────────────────────────────────────────────────────

  const statusChip = (status?: string) => {
    if (!status) return null;
    const map: Record<string, { color: 'success' | 'warning' | 'error'; label: string }> = {
      created: { color: 'success', label: 'Created' },
      pending_confirmation: { color: 'warning', label: 'Pending Confirmation' },
      failed: { color: 'error', label: 'Failed' },
    };
    const cfg = map[status];
    if (!cfg) return null;
    return <Chip size="small" color={cfg.color} label={cfg.label} sx={{ mt: 1 }} />;
  };

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Paper
        elevation={0}
        sx={{
          p: 2,
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          borderBottom: `1px solid ${theme.palette.divider}`,
          borderRadius: 0,
        }}
      >
        <Avatar sx={{ bgcolor: theme.palette.primary.main, width: 40, height: 40 }}>
          <AutoFixHighIcon />
        </Avatar>
        <Box>
          <Typography variant="subtitle1" fontWeight={700}>
            CRM Configuration Agent
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Configure your CRM using natural language
          </Typography>
        </Box>
        <Box sx={{ ml: 'auto' }}>
          <Tooltip title="Settings">
            <IconButton size="small">
              <SettingsIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      </Paper>

      {/* Messages */}
      <Box
        sx={{
          flex: 1,
          overflowY: 'auto',
          p: 2,
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
          bgcolor: alpha(theme.palette.background.default, 0.5),
        }}
      >
        {messages.map((msg) => (
          <Fade in key={msg.id}>
            <Box
              sx={{
                display: 'flex',
                gap: 1.5,
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
                maxWidth: '85%',
                alignSelf: msg.role === 'user' ? 'flex-end' : 'flex-start',
              }}
            >
              <Avatar
                sx={{
                  width: 32,
                  height: 32,
                  bgcolor: msg.role === 'user' ? 'secondary.main' : 'primary.main',
                  mt: 0.5,
                }}
              >
                {msg.role === 'user' ? <PersonIcon fontSize="small" /> : <SmartToyIcon fontSize="small" />}
              </Avatar>
              <Paper
                elevation={0}
                sx={{
                  p: 2,
                  borderRadius: 2,
                  bgcolor:
                    msg.role === 'user'
                      ? alpha(theme.palette.primary.main, 0.08)
                      : theme.palette.background.paper,
                  border: `1px solid ${
                    msg.role === 'user'
                      ? alpha(theme.palette.primary.main, 0.2)
                      : theme.palette.divider
                  }`,
                  position: 'relative',
                }}
              >
                <Typography
                  variant="body2"
                  sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.7 }}
                >
                  {msg.content}
                </Typography>

                {msg.metadata?.status && statusChip(msg.metadata.status)}

                {/* Config details */}
                {msg.metadata?.details && msg.metadata.status !== 'failed' && (
                  <Box sx={{ mt: 1.5 }}>
                    <Paper
                      variant="outlined"
                      sx={{
                        p: 1.5,
                        bgcolor: alpha(theme.palette.info.main, 0.04),
                        borderRadius: 1,
                        fontFamily: 'monospace',
                        fontSize: 12,
                        position: 'relative',
                      }}
                    >
                      <Tooltip title="Copy JSON">
                        <IconButton
                          size="small"
                          sx={{ position: 'absolute', top: 4, right: 4 }}
                          onClick={() => copyToClipboard(JSON.stringify(msg.metadata!.details, null, 2))}
                        >
                          <ContentCopyIcon sx={{ fontSize: 14 }} />
                        </IconButton>
                      </Tooltip>
                      <pre style={{ margin: 0, overflow: 'auto', maxHeight: 200 }}>
                        {JSON.stringify(msg.metadata.details, null, 2)}
                      </pre>
                    </Paper>
                  </Box>
                )}

                {/* Confirm / Reject buttons */}
                {msg.metadata?.status === 'pending_confirmation' && msg.metadata?.auditLogId && (
                  <Box sx={{ mt: 1.5, display: 'flex', gap: 1 }}>
                    <Button
                      size="small"
                      variant="contained"
                      color="success"
                      startIcon={<CheckCircleIcon />}
                      onClick={() => handleConfirm(msg.metadata!.auditLogId!)}
                      disabled={loading}
                    >
                      Confirm
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      color="error"
                      startIcon={<CancelIcon />}
                      onClick={() => handleReject(msg.metadata!.auditLogId!)}
                      disabled={loading}
                    >
                      Cancel
                    </Button>
                  </Box>
                )}

                <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mt: 1 }}>
                  {msg.timestamp.toLocaleTimeString()}
                </Typography>
              </Paper>
            </Box>
          </Fade>
        ))}

        {loading && (
          <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
              <SmartToyIcon fontSize="small" />
            </Avatar>
            <Paper elevation={0} sx={{ p: 2, borderRadius: 2, border: `1px solid ${theme.palette.divider}` }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <CircularProgress size={16} />
                <Typography variant="body2" color="text.secondary">
                  Analyzing instruction...
                </Typography>
              </Box>
            </Paper>
          </Box>
        )}

        <div ref={messagesEndRef} />
      </Box>

      <Divider />

      {/* Input */}
      <Box sx={{ p: 2, display: 'flex', gap: 1, alignItems: 'flex-end' }}>
        <TextField
          inputRef={inputRef}
          fullWidth
          multiline
          maxRows={4}
          placeholder="Describe what you want to configure..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={loading}
          variant="outlined"
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              borderRadius: 2,
            },
          }}
        />
        <IconButton
          color="primary"
          onClick={sendInstruction}
          disabled={loading || !input.trim()}
          sx={{
            bgcolor: 'primary.main',
            color: 'white',
            '&:hover': { bgcolor: 'primary.dark' },
            '&.Mui-disabled': { bgcolor: 'action.disabledBackground' },
            width: 40,
            height: 40,
          }}
        >
          <SendIcon fontSize="small" />
        </IconButton>
      </Box>
    </Box>
  );
};

export default AiConfigChat;
