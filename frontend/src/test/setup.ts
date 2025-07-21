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
class MockIntersectionObserver {
  root = null
  rootMargin = ''
  thresholds: number[] = []
  
  constructor(_callback: IntersectionObserverCallback, _options?: IntersectionObserverInit) {}
  disconnect() {}
  observe(_target: Element) {}
  unobserve(_target: Element) {}
  takeRecords(): IntersectionObserverEntry[] { return [] }
}

global.IntersectionObserver = MockIntersectionObserver as any

// Mock ResizeObserver
class MockResizeObserver {
  constructor(_callback: ResizeObserverCallback) {}
  disconnect() {}
  observe(_target: Element, _options?: ResizeObserverOptions) {}
  unobserve(_target: Element) {}
}

global.ResizeObserver = MockResizeObserver as any

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