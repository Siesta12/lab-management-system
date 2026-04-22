import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/auth': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/users': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/departments': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/labs': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/reservations': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/dashboard': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/devices': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/consumables': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/violations': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/checkin': { target: 'http://127.0.0.1:8080', changeOrigin: true },
    },
  },
});
