import { test, expect } from '@playwright/test';

test.describe('Responsive shell', () => {
  test('shows Agent Portal brand', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    await expect(page.locator('.brand-inline strong')).toBeVisible();
  });

  test('create dialog opens from New session control', async ({ page }, testInfo) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

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
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    const width = page.viewportSize()?.width ?? 1440;
    if (width < 640) {
      await expect(page.getByTestId('mobile-session-list')).toBeVisible();
    } else {
      await expect(page.locator('app-session-list').first()).toBeVisible();
    }
  });
});
