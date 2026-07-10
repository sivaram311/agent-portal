import { test, expect, type Page } from '@playwright/test';

async function ensureSignedIn(page: Page): Promise<void> {
  await page.goto('/');
  await page.waitForLoadState('domcontentloaded');

  const login = page.getByTestId('login-overlay');
  if (!(await login.isVisible().catch(() => false))) {
    return;
  }

  const user = process.env['CSS_USER'] || 'admin';
  const pass = process.env['CSS_PASSWORD'] || 'admin123';
  await page.getByTestId('login-username').fill(user);
  await page.getByTestId('login-password').fill(pass);
  await page.getByTestId('login-submit').click();
  await expect(page.getByTestId('user-badge')).toBeVisible({ timeout: 20_000 });
}

test.describe('Responsive shell', () => {
  test('shows Agent Portal brand', async ({ page }) => {
    await ensureSignedIn(page);
    await expect(page.locator('.brand-inline strong')).toBeVisible();
  });

  test('create dialog opens from New session control', async ({ page }) => {
    await ensureSignedIn(page);

    const isNarrow = (page.viewportSize()?.width ?? 1440) < 640;
    if (isNarrow) {
      await page.getByTestId('fab-new-session').click();
    } else {
      const desktopNew = page.locator('button.desktop-new');
      if (await desktopNew.isVisible()) {
        await desktopNew.click();
      } else {
        await page.getByRole('button', { name: 'New session' }).first().click();
      }
    }

    await expect(page.getByTestId('create-session-dialog')).toBeVisible();
    await page.getByRole('button', { name: 'Cancel' }).click();
  });

  test('sidebar or mobile list is present', async ({ page }) => {
    await ensureSignedIn(page);
    const width = page.viewportSize()?.width ?? 1440;
    if (width < 640) {
      await expect(page.getByTestId('mobile-session-list')).toBeVisible();
    } else {
      await expect(page.locator('app-session-list').first()).toBeVisible();
    }
  });
});
