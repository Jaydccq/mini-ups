import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// Simplified Vite config for CI builds
export default defineConfig({
  plugins: [
    react({
      // Basic React plugin without complex babel config
      fastRefresh: false,
    }),
  ],
  
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  
  // Define minimal global constants
  define: {
    __APP_VERSION__: JSON.stringify('ci-build'),
    __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
    __IS_DEV__: false,
  },
  
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    minify: false,
    chunkSizeWarningLimit: 2000,
    target: 'es2020',
    
    rollupOptions: {
      output: {
        manualChunks: undefined,
        chunkFileNames: 'js/[name]-[hash].js',
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]'
      }
    }
  },
  
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-router-dom',
    ],
  },
  
  envPrefix: ['VITE_'],
})