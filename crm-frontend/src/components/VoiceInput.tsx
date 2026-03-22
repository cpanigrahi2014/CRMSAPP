/* ============================================================
   VoiceInput – reusable voice-to-text button using Web Speech API
   Uses browser-native SpeechRecognition for real-time transcription.
   ============================================================ */
import React, { useState, useRef, useCallback, useEffect } from 'react';
import { IconButton, Tooltip, Box, Typography, keyframes } from '@mui/material';
import { Mic as MicIcon, MicOff as MicOffIcon, Stop as StopIcon } from '@mui/icons-material';

interface SpeechRecognitionEvent {
  resultIndex: number;
  results: SpeechRecognitionResultList;
}

interface VoiceInputProps {
  /** Called with the final transcript text */
  onTranscript: (text: string) => void;
  /** Tooltip label when idle */
  tooltip?: string;
  /** BCP-47 language code */
  language?: string;
  /** Size of the icon button */
  size?: 'small' | 'medium' | 'large';
  /** Disable the button */
  disabled?: boolean;
}

const pulse = keyframes`
  0%   { box-shadow: 0 0 0 0 rgba(244,67,54,0.5); }
  70%  { box-shadow: 0 0 0 8px rgba(244,67,54,0); }
  100% { box-shadow: 0 0 0 0 rgba(244,67,54,0); }
`;

const getSpeechRecognition = (): (new () => any) | null => {
  const w = window as any;
  return w.SpeechRecognition || w.webkitSpeechRecognition || null;
};

const VoiceInput: React.FC<VoiceInputProps> = ({
  onTranscript,
  tooltip = 'Voice input',
  language = 'en-US',
  size = 'small',
  disabled = false,
}) => {
  const [listening, setListening] = useState(false);
  const [interim, setInterim] = useState('');
  const [supported, setSupported] = useState(true);
  const recognitionRef = useRef<any>(null);
  const finalTranscriptRef = useRef('');

  useEffect(() => {
    if (!getSpeechRecognition()) setSupported(false);
    return () => { recognitionRef.current?.abort(); };
  }, []);

  const start = useCallback(() => {
    const SR = getSpeechRecognition();
    if (!SR) return;
    const recognition = new SR();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = language;
    finalTranscriptRef.current = '';

    recognition.onresult = (event: SpeechRecognitionEvent) => {
      let finalText = '';
      let interimText = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          finalText += transcript;
        } else {
          interimText += transcript;
        }
      }
      if (finalText) finalTranscriptRef.current += finalText;
      setInterim(interimText);
    };

    recognition.onerror = () => {
      setListening(false);
      setInterim('');
    };

    recognition.onend = () => {
      setListening(false);
      setInterim('');
      if (finalTranscriptRef.current.trim()) {
        onTranscript(finalTranscriptRef.current.trim());
      }
    };

    recognitionRef.current = recognition;
    recognition.start();
    setListening(true);
  }, [language, onTranscript]);

  const stop = useCallback(() => {
    recognitionRef.current?.stop();
  }, []);

  if (!supported) return null;

  return (
    <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}>
      <Tooltip title={listening ? 'Stop recording' : tooltip}>
        <span>
          <IconButton
            size={size}
            color={listening ? 'error' : 'default'}
            onClick={listening ? stop : start}
            disabled={disabled}
            sx={listening ? { animation: `${pulse} 1.2s infinite` } : undefined}
          >
            {listening ? <StopIcon fontSize={size} /> : <MicIcon fontSize={size} />}
          </IconButton>
        </span>
      </Tooltip>
      {listening && interim && (
        <Typography variant="caption" color="text.secondary" sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {interim}…
        </Typography>
      )}
    </Box>
  );
};

export default VoiceInput;
