import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { visualizer } from 'rollup-plugin-visualizer'
import { splitVendorChunkPlugin } from 'vite'

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  // Load env file based on `mode` in the current working directory.
  // Set the third parameter to '' to load all env regardless of the `VITE_` prefix.
  const env = loadEnv(mode, process.cwd(), '')
  
  const isDev = command === 'serve'
  const isProd = mode === 'production'
  
  return {
    plugins: [
      react({
        // Enable React Fast Refresh for development
        fastRefresh: isDev,
        // React production optimizations
        babel: isProd ? {
          plugins: [
            // Remove PropTypes in production
            ['transform-react-remove-prop-types', { removeImport: true }],
            // Remove development-only code
            ['transform-remove-console', { exclude: ['error', 'warn'] }]
          ]
        } : undefined
      }),
      
      // Vendor chunk splitting for better caching
      splitVendorChunkPlugin(),
      
      // Bundle analyzer for production builds
      isProd && visualizer({
        filename: 'dist/stats.html',
        open: false,
        gzipSize: true,
        brotliSize: true,
      }),
    ].filter(Boolean),
    
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    
    // Define global constants
    define: {
      __APP_VERSION__: JSON.stringify(process.env.npm_package_version || '1.0.0'),
      __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
      __IS_DEV__: isDev,
    },
    
    server: {
      port: 3001,
      host: true,
      strictPort: true,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8081',
          changeOrigin: true,
          secure: false,
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('proxy error', err);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              console.log('Sending Request to the Target:', req.method, req.url);
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
            });
          },
        },
        '/ws': {
          target: env.VITE_WS_BASE_URL || 'ws://localhost:8081',
          ws: true,
          changeOrigin: true,
        }
      }
    },
    
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      
      // Generate sourcemaps for production debugging
      sourcemap: isProd ? 'hidden' : true,
      
      // Build optimizations  
      minify: isProd ? 'terser' : false,
      cssMinify: isProd,
      
      // Increase chunk size warning limit
      chunkSizeWarningLimit: 1000,
      
      // Target modern browsers for smaller bundles
      target: ['es2020', 'edge88', 'firefox78', 'chrome87', 'safari13.1'],
      
      rollupOptions: {
        output: {
          // Optimize chunk splitting for better caching
          manualChunks: (id) => {
            // Vendor chunks
            if (id.includes('node_modules')) {
              // React ecosystem
              if (id.includes('react') || id.includes('react-dom') || id.includes('react-router')) {
                return 'react-vendor'
              }
              
              // TanStack Query
              if (id.includes('@tanstack/react-query')) {
                return 'query-vendor'
              }
              
              // Radix UI components
              if (id.includes('@radix-ui')) {
                return 'ui-vendor'
              }
              
              // Lucide icons
              if (id.includes('lucide-react')) {
                return 'icons-vendor'
              }
              
              // Socket.io and real-time libraries
              if (id.includes('socket.io') || id.includes('ws')) {
                return 'realtime-vendor'
              }
              
              // Other vendor libraries
              return 'vendor'
            }
            
            // App chunks
            if (id.includes('/src/components/')) {
              return 'components'
            }
            
            if (id.includes('/src/pages/')) {
              return 'pages'
            }
            
            if (id.includes('/src/services/') || id.includes('/src/hooks/')) {
              return 'services'
            }
          },
          
          // File naming patterns
          chunkFileNames: (chunkInfo) => {
            const facadeModuleId = chunkInfo.facadeModuleId
              ? chunkInfo.facadeModuleId.split('/').pop()
              : 'chunk'
            return `js/${chunkInfo.name}-[hash].js`
          },
          entryFileNames: 'js/[name]-[hash].js',
          assetFileNames: (assetInfo) => {
            const info = assetInfo.name?.split('.') || []
            let extType = info[info.length - 1]
            
            if (/\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/i.test(assetInfo.name || '')) {
              extType = 'media'
            } else if (/\.(png|jpe?g|gif|svg|ico|webp)(\?.*)?$/i.test(assetInfo.name || '')) {
              extType = 'images'
            } else if (/\.(woff2?|eot|ttf|otf)(\?.*)?$/i.test(assetInfo.name || '')) {
              extType = 'fonts'
            }
            
            return `${extType}/[name]-[hash].[ext]`
          }
        },
        
        // External dependencies (for CDN usage if needed)
        external: isProd ? [] : [],
      },
      
      // Terser options for additional minification
      terserOptions: isProd ? {
        compress: {
          drop_console: true,
          drop_debugger: true,
          pure_funcs: ['console.log', 'console.info'],
        },
        mangle: {
          safari10: true,
        },
        format: {
          safari10: true,
        },
      } : {},
    },
    
    // Dependency optimization
    optimizeDeps: {
      include: [
        'react',
        'react-dom',
        'react-router-dom',
        '@tanstack/react-query',
        'zustand',
        'socket.io-client',
      ],
      exclude: ['@vite/client', '@vite/env'],
    },
    
    // Preview server config (for testing production builds locally)
    preview: {
      port: 4173,
      host: true,
    },
    
    // Environment variable validation
    envPrefix: ['VITE_', 'REACT_APP_'],
  }
})