import { test, expect } from '@playwright/test';
import * as fs from 'node:fs';
import * as path from 'node:path';

const shotDir = path.join(__dirname, '..', 'screenshots', 'realme-p2-pro');

test.beforeAll(() => {
  fs.mkdirSync(shotDir, { recursive: true });
});

test.describe('Realme P2 Pro mobile UI', () => {
  test('viewport matches Realme P2 Pro CSS size and has no horizontal overflow', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForSelector('.brand-inline, [data-testid="mobile-session-list"]', { timeout: 15000 });

    const size = page.viewportSize();
    expect(size?.width).toBe(360);
    expect(size?.height).toBe(800);

    const overflow = await page.evaluate(() => {
      const doc = document.documentElement;
      return {
        scrollWidth: doc.scrollWidth,
        clientWidth: doc.clientWidth,
        bodyScrollWidth: document.body.scrollWidth,
      };
    });

    expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth + 1);
    expect(overflow.bodyScrollWidth).toBeLessThanOrEqual(overflow.clientWidth + 1);

    await page.screenshot({ path: path.join(shotDir, '01-home.png'), fullPage: true });
  });

  test('shows Agent Portal chrome and reachable New Session FAB', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    await expect(page.locator('.brand-inline strong')).toBeVisible();
    await expect(page.getByTestId('fab-new-session')).toBeVisible();

    const box = await page.getByTestId('fab-new-session').boundingBox();
    expect(box).toBeTruthy();
    expect(box!.width).toBeGreaterThanOrEqual(44);
    expect(box!.height).toBeGreaterThanOrEqual(44);

    const vp = page.viewportSize()!;
    expect(box!.y + box!.height).toBeGreaterThan(vp.height * 0.7);
    expect(box!.x).toBeGreaterThan(vp.width * 0.5);

    await page.screenshot({ path: path.join(shotDir, '02-fab.png'), fullPage: true });
  });

  test('create-session dialog is usable on narrow screen', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    await page.getByTestId('fab-new-session').click();
    const dialog = page.getByTestId('create-session-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('heading', { name: 'New agent session' })).toBeVisible();

    const dialogBox = await dialog.boundingBox();
    const vp = page.viewportSize()!;
    expect(dialogBox).toBeTruthy();
    expect(dialogBox!.width).toBeLessThanOrEqual(vp.width);
    expect(dialogBox!.x).toBeGreaterThanOrEqual(0);

    await expect(dialog.getByText('Cursor', { exact: true })).toBeVisible();
    await expect(dialog.getByText('Antigravity', { exact: true })).toBeVisible();

    const workspace = dialog.locator('input[name="workspace"]');
    await expect(workspace).toBeVisible();
    const inputBox = await workspace.boundingBox();
    expect(inputBox!.height).toBeGreaterThanOrEqual(40);

    await page.screenshot({ path: path.join(shotDir, '03-create-dialog.png'), fullPage: true });

    await dialog.getByRole('button', { name: 'Cancel' }).click();
    await expect(dialog).toHaveCount(0);
  });

  test('session list filters and search are touch-friendly', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    const list = page.getByTestId('mobile-session-list');
    await expect(list).toBeVisible();

    const search = page.getByTestId('mobile-search');
    await expect(search).toBeVisible();
    const searchBox = await search.boundingBox();
    expect(searchBox!.height).toBeGreaterThanOrEqual(40);

    for (const label of ['all', 'active', 'failed', 'archived']) {
      const chip = list.getByRole('button', { name: label, exact: true });
      await expect(chip).toBeVisible();
      const chipBox = await chip.boundingBox();
      expect(chipBox!.height).toBeGreaterThanOrEqual(28);
    }

    await search.fill('demo');
    await page.screenshot({ path: path.join(shotDir, '04-search-filters.png'), fullPage: true });
  });

  test('opening a session shows detail with tabs and bottom input', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    const list = page.getByTestId('mobile-session-list');
    await expect(list).toBeVisible();

    const card = list.getByTestId('session-card').first();
    const hasCard = await card.count();

    if (hasCard === 0) {
      await page.getByTestId('fab-new-session').click();
      const dialog = page.getByTestId('create-session-dialog');
      await dialog.locator('input[name="title"]').fill('P2 Pro QA');
      await dialog.locator('input[name="workspace"]').fill('demo');
      await dialog.locator('input[value="antigravity"]').check();
      await dialog.getByRole('button', { name: /Create/i }).click();
      await expect(page.getByTestId('session-detail')).toBeVisible({ timeout: 20000 });
    } else {
      await card.click();
    }

    const detail = page.getByTestId('session-detail');
    await expect(detail).toBeVisible({ timeout: 15000 });
    await expect(detail.getByRole('tab', { name: 'Transcript' })).toBeVisible();
    await expect(detail.getByRole('tab', { name: 'Logs' })).toBeVisible();
    await expect(detail.getByRole('tab', { name: 'Code' })).toBeVisible();
    await expect(detail.getByRole('tab', { name: 'Preview' })).toBeVisible();

    const input = detail.locator('app-agent-input-bar textarea');
    await expect(input).toBeVisible();
    const inputBox = await input.boundingBox();
    const vp = page.viewportSize()!;
    expect(inputBox!.y).toBeGreaterThan(vp.height * 0.45);

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth
    );
    expect(overflow).toBeLessThanOrEqual(1);

    await page.screenshot({ path: path.join(shotDir, '05-session-detail.png'), fullPage: true });

    await detail.getByRole('tab', { name: 'Logs' }).click();
    await expect(detail.getByText(/Tool runs|terminal/i).first()).toBeVisible();
    await page.screenshot({ path: path.join(shotDir, '06-logs-tab.png'), fullPage: true });

    await detail.getByRole('tab', { name: 'Code' }).click();
    await expect(detail.getByTestId('code-viewer')).toBeVisible();
    await expect(detail.getByText(/No files yet|Nothing to preview|workspace/i).first()).toBeVisible();
    await page.screenshot({ path: path.join(shotDir, '07-code-tab.png'), fullPage: true });
  });
});
