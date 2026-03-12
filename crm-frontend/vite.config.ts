import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api/v1/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/v1/leads': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/v1/accounts': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/v1/contacts': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      '/api/v1/opportunities': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/api/v1/activities': {
        target: 'http://localhost:8086',
        changeOrigin: true,
      },
      '/api/v1/ai': {
        target: 'http://localhost:8089',
        changeOrigin: true,
      },
      '/api/ai': {
        target: 'http://localhost:8089',
        changeOrigin: true,
      },
      '/api/v1/email': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
      '/api/v1/workflows': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/api/v1/automation': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/api/v1/integrations': {
        target: 'http://localhost:8091',
        changeOrigin: true,
      },
      '/api/v1/developer': {
        target: 'http://localhost:8091',
        changeOrigin: true,
      },
      '/api/v1/notifications': {
        target: 'http://localhost:8087',
        changeOrigin: true,
      },
      '/api/v1/communications': {
        target: 'http://localhost:8087',
        changeOrigin: true,
      },
      '/api/v1/collaboration': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
    },
  },
});
