import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler']],
      },
    }),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'mask-icon.svg'],
      manifest: {
        name: 'Paisa - Expense Manager',
        short_name: 'Paisa',
        description: 'Manage your finances with ease',
        theme_color: '#ffffff',
        icons: [
          {
            src: 'logo-dark.png',
            sizes: '192x192',
            type: 'image/png'
          },
          {
            src: 'logo-dark.png',
            sizes: '512x512',
            type: 'image/png'
          }
        ]
      }
    })
  ],
})
