import { test, expect } from '@playwright/test';

/**
 * CSS login overlay appears only when backend has css.enabled=true.
 * When CSS is off, the overlay is absent and the shell loads normally.
 */
test.describe('Auth shell', () => {
  test('either shows CSS login or the main shell', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    const login = page.getByTestId('login-overlay');
    const brand = page.locator('.brand-inline strong');

    await expect(brand.or(login)).toBeVisible({ timeout: 15_000 });

    if (await login.isVisible()) {
      await expect(page.getByTestId('login-username')).toBeVisible();
      await expect(page.getByTestId('login-password')).toBeVisible();
      await expect(page.getByTestId('login-submit')).toBeVisible();

      const cssUrl = process.env['CSS_AUTH_URL'] || 'http://localhost:9000';
      const user = process.env['CSS_USER'] || 'admin';
      const pass = process.env['CSS_PASSWORD'] || 'admin123';

      // Attempt login when CSS is reachable; skip soft-fail if CSS is down.
      const loginRes = await page.request.post(`${cssUrl}/auth/login`, {
        data: { username: user, password: pass, clientId: 'agent-portal' },
        failOnStatusCode: false,
      });
      if (loginRes.ok()) {
        await page.getByTestId('login-username').fill(user);
        await page.getByTestId('login-password').fill(pass);
        await page.getByTestId('login-submit').click();
        await expect(page.getByTestId('user-badge')).toBeVisible({ timeout: 20_000 });
        await expect(page.getByTestId('capability-badges')).toBeVisible();
      }
    } else {
      await expect(brand).toBeVisible();
    }
  });
});
