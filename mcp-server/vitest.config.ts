/**
 * Vitest Configuration
 * 
 * 配置测试环境和选项
 */

import { defineConfig } from 'vitest/config';
import { resolve } from 'path';

export default defineConfig({
  test: {
    // 测试环境配置
    environment: 'node',
    
    // 全局测试设置
    globals: true,
    
    // 测试文件匹配模式
    include: [
      'src/**/*.{test,spec}.{js,ts}',
      'src/**/__tests__/**/*.{js,ts}'
    ],
    
    // 排除文件
    exclude: [
      'node_modules',
      'dist',
      '.git'
    ],
    
    // 测试超时设置
    testTimeout: 10000,
    hookTimeout: 10000,
    
    // 覆盖率配置
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      reportsDirectory: './coverage',
      include: [
        'src/**/*.{js,ts}'
      ],
      exclude: [
        'src/**/*.{test,spec}.{js,ts}',
        'src/**/__tests__/**',
        'src/index.ts', // Entry point, hard to test in isolation
        'src/**/*.d.ts'
      ],
      thresholds: {
        global: {
          branches: 80,
          functions: 80,
          lines: 80,
          statements: 80
        }
      }
    },
    
    // 设置文件
    setupFiles: ['./src/__tests__/setup.ts'],
    
    // 并行执行
    pool: 'threads',
    poolOptions: {
      threads: {
        singleThread: false
      }
    },
    
    // 监听模式配置
    watch: false,
    
    // 报告配置
    reporter: ['verbose', 'json'],
    outputFile: {
      json: './test-results/results.json'
    }
  },
  
  // 路径解析
  resolve: {
    alias: {
      '@': resolve(__dirname, './src')
    }
  },
  
  // 环境变量
  define: {
    'process.env.NODE_ENV': '"test"'
  }
});