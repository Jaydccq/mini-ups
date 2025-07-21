import { CustomProjectConfig } from 'lost-pixel';

export const config: CustomProjectConfig = {
  pageShots: {
    pages: [
      { path: '/', name: 'homepage' },
      { path: '/login', name: 'login' },
      { path: '/dashboard', name: 'dashboard' },
      { path: '/tracking', name: 'tracking' },
      { path: '/shipments', name: 'shipments' },
    ],
    baseUrl: 'http://localhost:3000',
  },
  lostPixelProjectId: 'mini-ups-frontend',
  apiKey: process.env.LOST_PIXEL_API_KEY,
  threshold: 0.2,
  waitBeforeScreenshot: 1000,
  breakpoints: [1200, 768, 375],
};