import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Forward API calls to the Payment Router so the browser talks to a single origin (no CORS).
// In Docker, Nginx does the same job.
const apiProxy = {
  '/api': {
    target: 'http://localhost:8080',
    // Keep the browser's Host header (e.g. localhost:5173). The router then sees Origin and Host
    // matching and treats the call as same-origin, so its CORS allow-list is never consulted and
    // the proxy works on any port. With changeOrigin: true, Spring would see Origin localhost:5173
    // but Host localhost:8080, treat it as cross-origin and reject POSTs from unlisted ports (403).
    changeOrigin: false,
  },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: apiProxy, // npm run dev     (http://localhost:5173)
  },
  preview: {
    proxy: apiProxy, // npm run preview (http://localhost:4173)
  },
})
