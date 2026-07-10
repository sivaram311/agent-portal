import { defineConfig, devices } from '@playwright/test';

/**
 * Realme P2 Pro: 1080×2412 physical @ ~3x DPR → ~360×804 CSS viewport.
 */
export const realmeP2Pro = {
  ...devices['Pixel 7'],
  name: 'Realme P2 Pro',
  viewport: { width: 360, height: 800 },
  deviceScaleFactor: 3,
  isMobile: true,
  hasTouch: true,
  userAgent:
    'Mozilla/5.0 (Linux; Android 14; RMX3990) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
};

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: process.env['APP_URL'] || 'http://127.0.0.1:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'realme-p2-pro',
      use: { ...realmeP2Pro },
    },
    {
      name: 'tablet-1024',
      use: {
        ...devices['iPad Pro 11'],
        viewport: { width: 1024, height: 768 },
        isMobile: true,
        hasTouch: true,
      },
    },
    {
      name: 'desktop-1440',
      use: {
        viewport: { width: 1440, height: 900 },
        isMobile: false,
        hasTouch: false,
      },
    },
  ],
  outputDir: 'test-results',
});
