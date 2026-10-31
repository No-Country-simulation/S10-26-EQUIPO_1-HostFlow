import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// https://vite.dev/config/
const srcPath = path.resolve(fileURLToPath(new URL('.', import.meta.url)), 'src')

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': srcPath,
      '@components': path.resolve(srcPath, 'components'),
      '@features': path.resolve(srcPath, 'features'),
      '@hooks': path.resolve(srcPath, 'hooks'),
      '@lib': path.resolve(srcPath, 'lib'),
      '@styles': path.resolve(srcPath, 'styles'),
      '@utils': path.resolve(srcPath, 'utils'),
    },
  },
})
