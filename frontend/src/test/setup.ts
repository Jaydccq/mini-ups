import '@testing-library/jest-dom'
import { beforeAll, afterEach, afterAll } from 'vitest'
import { cleanup } from '@testing-library/react'

// Simple mock setup that doesn't rely on MSW for CI compatibility
try {
  const { server } = await import('./mocks/server')
  
  // Establish API mocking before all tests
  beforeAll(() => server.listen())
  
  // Reset any request handlers that are declared in a test
  afterEach(() => {
    cleanup()
    server.resetHandlers()
  })
  
  // Clean up after the tests are finished
  afterAll(() => server.close())
} catch (error) {
  console.warn('⚠️ MSW server setup failed, continuing without API mocking:', error)
  
  // Fallback cleanup for tests
  afterEach(() => {
    cleanup()
  })
}

// Mock IntersectionObserver
global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
}

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
}

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => {},
  }),
})