import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // During development, forward API calls to the Payment Router so the browser
    // talks to a single origin (no CORS). In Docker, Nginx does the same job.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
