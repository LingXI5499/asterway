import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: './',
  server: { port: 5173, strictPort: true },
  test: { include: ['src/tests/**/*.test.ts'] },
})
