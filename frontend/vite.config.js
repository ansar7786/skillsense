import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        headers: {
          origin: 'http://localhost:5173',
          Origin: 'http://localhost:5173'
        },
        configure: (proxy, _options) => {
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            if (req.headers.origin) {
              proxyRes.headers['access-control-allow-origin'] = req.headers.origin;
              proxyRes.headers['access-control-allow-credentials'] = 'true';
            }
          });
        }
      }
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/setupTests.js',
  }
});

