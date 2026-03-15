/* ============================================================
   ScreenCapturePage – Capture screenshots of any CRM screen
   ============================================================
   How it works:
   ─────────────
   1. html2canvas renders the visible DOM (or a selected area) into
      an off-screen <canvas> element purely in the browser – no
      server round-trip or browser extension required.
   2. The canvas is converted to a PNG data-URL via toDataURL().
   3. The user can preview, copy to clipboard, or download the image.

   Supported capture modes:
     • Full Page  – captures the entire CRM content area
     • Visible Area – captures only the current viewport
     • Select Element – lets the user click on any element to capture it

   NOTE: html2canvas recreates the page by reading DOM/CSS; some
   advanced CSS (e.g. backdrop-filter, mix-blend-mode) or cross-origin
   images may render with minor differences.
   ============================================================ */
import React, { useState, useCallback, useRef, useEffect } from 'react';
import {
  Box,
  Button,
  ButtonGroup,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Collapse,
  IconButton,
  Paper,
  Stack,
  Tooltip,
  Typography,
  Alert,
  AlertTitle,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  Screenshot as ScreenshotIcon,
  Fullscreen as FullPageIcon,
  CropFree as VisibleIcon,
  TouchApp as SelectIcon,
  Download as DownloadIcon,
  ContentCopy as CopyIcon,
  Delete as ClearIcon,
  Info as InfoIcon,
  CheckCircle as SuccessIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
  CameraAlt as CameraIcon,
  Layers as LayersIcon,
  Visibility as ViewIcon,
  Timer as TimerIcon,
} from '@mui/icons-material';
import html2canvas from 'html2canvas';
import { PageHeader } from '../components';
import { useSnackbar } from 'notistack';

type CaptureMode = 'full' | 'visible' | 'element';

const ScreenCapturePage: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();

  /* ---------- state ---------- */
  const [mode, setMode] = useState<CaptureMode>('full');
  const [capturing, setCapturing] = useState(false);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [captureTime, setCaptureTime] = useState<number | null>(null);
  const [selecting, setSelecting] = useState(false);
  const [showExplanation, setShowExplanation] = useState(true);

  const previewRef = useRef<HTMLImageElement | null>(null);

  /* ---------- element-selection overlay ---------- */
  useEffect(() => {
    if (!selecting) return;

    const handleClick = (e: MouseEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setSelecting(false);
      captureElement(e.target as HTMLElement);
    };

    const handleHover = (e: MouseEvent) => {
      (e.target as HTMLElement).style.outline = '3px solid #1976d2';
    };

    const handleLeave = (e: MouseEvent) => {
      (e.target as HTMLElement).style.outline = '';
    };

    document.addEventListener('click', handleClick, true);
    document.addEventListener('mouseover', handleHover, true);
    document.addEventListener('mouseout', handleLeave, true);

    return () => {
      document.removeEventListener('click', handleClick, true);
      document.removeEventListener('mouseover', handleHover, true);
      document.removeEventListener('mouseout', handleLeave, true);
      // Clean up any lingering outlines
      document.querySelectorAll('[style*="outline"]').forEach((el) => {
        (el as HTMLElement).style.outline = '';
      });
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selecting]);

  /* ---------- capture logic ---------- */
  const captureElement = useCallback(
    async (target: HTMLElement) => {
      setCapturing(true);
      const start = performance.now();
      try {
        const canvas = await html2canvas(target, {
          useCORS: true,
          allowTaint: false,
          backgroundColor: '#ffffff',
          scale: window.devicePixelRatio || 2,
          logging: false,
        });
        const url = canvas.toDataURL('image/png');
        setImageUrl(url);
        setCaptureTime(Math.round(performance.now() - start));
        enqueueSnackbar('Screenshot captured successfully!', { variant: 'success' });
      } catch (err) {
        console.error('Screen capture failed:', err);
        enqueueSnackbar('Capture failed – see console for details.', { variant: 'error' });
      } finally {
        setCapturing(false);
      }
    },
    [enqueueSnackbar],
  );

  const handleCapture = useCallback(async () => {
    if (mode === 'element') {
      setSelecting(true);
      enqueueSnackbar('Click any element on the page to capture it.', { variant: 'info' });
      return;
    }

    /* For 'full' and 'visible' we capture the main content area */
    const target =
      mode === 'full'
        ? (document.querySelector('main') as HTMLElement) ?? document.body
        : document.documentElement;

    setCapturing(true);
    const start = performance.now();
    try {
      const canvas = await html2canvas(target, {
        useCORS: true,
        allowTaint: false,
        backgroundColor: '#ffffff',
        scale: window.devicePixelRatio || 2,
        logging: false,
        ...(mode === 'visible' && {
          windowWidth: document.documentElement.clientWidth,
          windowHeight: document.documentElement.clientHeight,
          scrollX: 0,
          scrollY: 0,
          width: document.documentElement.clientWidth,
          height: document.documentElement.clientHeight,
        }),
      });
      const url = canvas.toDataURL('image/png');
      setImageUrl(url);
      setCaptureTime(Math.round(performance.now() - start));
      enqueueSnackbar('Screenshot captured successfully!', { variant: 'success' });
    } catch (err) {
      console.error('Screen capture failed:', err);
      enqueueSnackbar('Capture failed – see console for details.', { variant: 'error' });
    } finally {
      setCapturing(false);
    }
  }, [mode, enqueueSnackbar]);

  /* ---------- download ---------- */
  const handleDownload = useCallback(() => {
    if (!imageUrl) return;
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const a = document.createElement('a');
    a.href = imageUrl;
    a.download = `crm-screenshot-${timestamp}.png`;
    a.click();
    enqueueSnackbar('Image downloaded.', { variant: 'success' });
  }, [imageUrl, enqueueSnackbar]);

  /* ---------- copy to clipboard ---------- */
  const handleCopy = useCallback(async () => {
    if (!imageUrl) return;
    try {
      const res = await fetch(imageUrl);
      const blob = await res.blob();
      await navigator.clipboard.write([new ClipboardItem({ 'image/png': blob })]);
      enqueueSnackbar('Copied to clipboard!', { variant: 'success' });
    } catch {
      enqueueSnackbar('Copy failed – your browser may not support clipboard images.', {
        variant: 'warning',
      });
    }
  }, [imageUrl, enqueueSnackbar]);

  /* ---------- clear ---------- */
  const handleClear = () => {
    setImageUrl(null);
    setCaptureTime(null);
  };

  /* ---------- render ---------- */
  return (
    <>
      <PageHeader
        title="Screen Capture"
        breadcrumbs={[
          { label: 'Home', to: '/dashboard' },
          { label: 'Screen Capture' },
        ]}
        action={
          <Button
            variant="contained"
            startIcon={capturing ? <CircularProgress size={18} color="inherit" /> : <CameraIcon />}
            onClick={handleCapture}
            disabled={capturing || selecting}
            size="large"
          >
            {capturing ? 'Capturing…' : selecting ? 'Click an element…' : 'Take Screenshot'}
          </Button>
        }
      />

      {/* ────── Element-selection banner ────── */}
      {selecting && (
        <Alert severity="info" sx={{ mb: 2 }} onClose={() => setSelecting(false)}>
          <AlertTitle>Element Selection Mode</AlertTitle>
          Hover over any element — it will be outlined in blue. Click to capture it. Press&nbsp;
          <strong>Esc</strong> or close this banner to cancel.
        </Alert>
      )}

      {/* ────── How-It-Works explanation section ────── */}
      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ pb: '16px !important' }}>
          <Stack
            direction="row"
            alignItems="center"
            justifyContent="space-between"
            onClick={() => setShowExplanation((v) => !v)}
            sx={{ cursor: 'pointer' }}
          >
            <Stack direction="row" alignItems="center" spacing={1}>
              <InfoIcon color="primary" />
              <Typography variant="h6" fontWeight={600}>
                How Screen Capture Works
              </Typography>
            </Stack>
            <IconButton size="small">
              {showExplanation ? <ExpandLessIcon /> : <ExpandMoreIcon />}
            </IconButton>
          </Stack>

          <Collapse in={showExplanation}>
            <Divider sx={{ my: 1.5 }} />

            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              The Screen Capture tool uses <strong>html2canvas</strong> to convert live DOM elements
              into a raster image — entirely in your browser with zero server calls or extensions.
            </Typography>

            <List dense disablePadding>
              <ListItem>
                <ListItemIcon>
                  <LayersIcon color="primary" fontSize="small" />
                </ListItemIcon>
                <ListItemText
                  primary="Step 1 – DOM Traversal"
                  secondary="html2canvas walks the DOM tree, reads computed CSS styles, and reconstructs each element on an off-screen <canvas>."
                />
              </ListItem>
              <ListItem>
                <ListItemIcon>
                  <CameraIcon color="primary" fontSize="small" />
                </ListItemIcon>
                <ListItemText
                  primary="Step 2 – Canvas Rendering"
                  secondary="Each element (text, borders, backgrounds, images) is painted onto the canvas using the Canvas 2D API, preserving layout and styling."
                />
              </ListItem>
              <ListItem>
                <ListItemIcon>
                  <DownloadIcon color="primary" fontSize="small" />
                </ListItemIcon>
                <ListItemText
                  primary="Step 3 – Image Export"
                  secondary="The canvas is converted to a PNG data-URL via canvas.toDataURL(). You can then preview, copy, or download the result."
                />
              </ListItem>
            </List>

            <Alert severity="info" variant="outlined" sx={{ mt: 1.5 }}>
              <strong>Privacy:</strong> All processing happens client-side. Your screenshot never
              leaves the browser — nothing is uploaded to any server.
            </Alert>
          </Collapse>
        </CardContent>
      </Card>

      {/* ────── Capture mode selector ────── */}
      <Paper sx={{ p: 2.5, mb: 3 }}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          Capture Mode
        </Typography>
        <ButtonGroup variant="outlined" fullWidth>
          <Button
            variant={mode === 'full' ? 'contained' : 'outlined'}
            startIcon={<FullPageIcon />}
            onClick={() => setMode('full')}
          >
            Full Page
          </Button>
          <Button
            variant={mode === 'visible' ? 'contained' : 'outlined'}
            startIcon={<VisibleIcon />}
            onClick={() => setMode('visible')}
          >
            Visible Area
          </Button>
          <Button
            variant={mode === 'element' ? 'contained' : 'outlined'}
            startIcon={<SelectIcon />}
            onClick={() => setMode('element')}
          >
            Select Element
          </Button>
        </ButtonGroup>

        <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
          {mode === 'full' && (
            <Chip icon={<ViewIcon />} label="Captures the entire main content area" size="small" />
          )}
          {mode === 'visible' && (
            <Chip
              icon={<ViewIcon />}
              label="Captures only what's currently visible in the viewport"
              size="small"
            />
          )}
          {mode === 'element' && (
            <Chip
              icon={<SelectIcon />}
              label="Click any element to capture just that component"
              size="small"
            />
          )}
        </Stack>
      </Paper>

      {/* ────── Preview & actions ────── */}
      {imageUrl && (
        <Paper sx={{ p: 2.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
            <Stack direction="row" alignItems="center" spacing={1}>
              <SuccessIcon color="success" />
              <Typography variant="subtitle1" fontWeight={600}>
                Captured Screenshot
              </Typography>
              {captureTime != null && (
                <Chip
                  icon={<TimerIcon />}
                  label={`${captureTime} ms`}
                  size="small"
                  color="default"
                />
              )}
            </Stack>

            <Stack direction="row" spacing={1}>
              <Tooltip title="Download PNG">
                <IconButton color="primary" onClick={handleDownload}>
                  <DownloadIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Copy to clipboard">
                <IconButton color="primary" onClick={handleCopy}>
                  <CopyIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Clear">
                <IconButton color="error" onClick={handleClear}>
                  <ClearIcon />
                </IconButton>
              </Tooltip>
            </Stack>
          </Stack>

          <Box
            sx={{
              maxHeight: 600,
              overflow: 'auto',
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 2,
              backgroundColor: '#f5f5f5',
              textAlign: 'center',
            }}
          >
            <img
              ref={previewRef}
              src={imageUrl}
              alt="Captured screenshot"
              style={{ maxWidth: '100%', display: 'block', margin: '0 auto' }}
            />
          </Box>
        </Paper>
      )}

      {/* ────── Empty state ────── */}
      {!imageUrl && !capturing && (
        <Paper
          sx={{
            p: 6,
            textAlign: 'center',
            border: '2px dashed',
            borderColor: 'divider',
            borderRadius: 3,
          }}
        >
          <ScreenshotIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No screenshot yet
          </Typography>
          <Typography variant="body2" color="text.disabled" sx={{ mb: 3 }}>
            Choose a capture mode above, then click <strong>Take Screenshot</strong> to capture the
            current screen.
          </Typography>
          <Button variant="outlined" startIcon={<CameraIcon />} onClick={handleCapture}>
            Take Screenshot Now
          </Button>
        </Paper>
      )}
    </>
  );
};

export default ScreenCapturePage;
