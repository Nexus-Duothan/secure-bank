import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    server: {
      port: Number(env.VITE_PORT || 5173),
      strictPort: true,
      proxy: {
        // Route account operations directly in local development. The reactive gateway's
        // connection-close response on POST is rejected by Node's strict proxy parser.
        '/api/v1/accounts': {
          target: env.VITE_ACCOUNTS_PROXY_TARGET || 'http://localhost:8084',
          changeOrigin: true,
        },
        // Everything goes through the API Gateway, the platform's single entry point. Point
        // VITE_API_PROXY_TARGET at a service port to work on one microservice in isolation.
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  };
});
