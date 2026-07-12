import { test, expect } from '@playwright/test';
import * as fs from 'node:fs';
import * as path from 'node:path';

/** Realme P2 Pro CSS viewport (from device audit / user template). */
const REALME_P2_PRO = { width: 360, height: 780 };

const shotDir = path.join(__dirname, '..', 'screenshots', 'realme');

function apiBase(appUrl: string): string {
  const u = new URL(appUrl);
  if (u.hostname.includes('delena.buzz') || u.port === '' || u.port === '80' || u.port === '443') {
    return `${u.origin}/api`;
  }
  if (u.port === '4200') {
    return `${u.protocol}//${u.hostname}:8080/api`;
  }
  return `${u.origin}/api`;
}

test.describe('Realme P2 Pro - Full E2E with Multi-Agent & Changes', () => {
  test.use({
    viewport: REALME_P2_PRO,
    isMobile: true,
    hasTouch: true,
    deviceScaleFactor: 3,
    userAgent:
      'Mozilla/5.0 (Linux; Android 14; RMX3990) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
  });

  const BASE_URL = process.env['APP_URL'] || 'https://delena.buzz';
  const USERNAME = process.env['CSS_USER'] || 'admin';
  const PASSWORD = process.env['CSS_PASSWORD'] || 'admin123';

  test.beforeAll(() => {
    fs.mkdirSync(shotDir, { recursive: true });
  });

  test('Realme P2 Pro - Complete Flow with Assertions & Changes Tab', async ({ page, request }) => {
    test.setTimeout(180_000);

    // =====================
    // 1. LOGIN
    // =====================
    await page.goto(BASE_URL);
    await page.waitForLoadState('domcontentloaded');

    const loginOverlay = page.getByTestId('login-overlay');
    const usernameField = page
      .getByTestId('login-username')
      .or(page.locator('input[type="text"], input[placeholder*="user" i]'));
    const passwordField = page
      .getByTestId('login-password')
      .or(page.locator('input[type="password"], input[placeholder*="pass" i]'));
    const loginButton = page
      .getByTestId('login-submit')
      .or(page.locator('button:has-text("Sign in"), button:has-text("Login"), button[type="submit"]'));

    if (
      (await loginOverlay.isVisible().catch(() => false)) ||
      (await usernameField.first().isVisible().catch(() => false))
    ) {
      await usernameField.first().fill(USERNAME);
      await passwordField.first().fill(PASSWORD);
      await loginButton.first().click();
      await page.waitForLoadState('networkidle');
    }

    await expect(
      page.locator('.brand-inline strong').or(page.getByText('Agent Portal', { exact: false })).first()
    ).toBeVisible({ timeout: 20_000 });
    await expect(
      page
        .getByTestId('user-badge')
        .or(page.getByTestId('overflow-menu-btn'))
        .or(page.locator('button:has-text("Sign out"), button:has-text("Log out")'))
        .first()
    ).toBeVisible({ timeout: 15_000 });

    await page.screenshot({ path: path.join(shotDir, '01-after-login.png'), fullPage: true });

    // =====================
    // 2. CREATE SESSION (Mobile FAB)
    // =====================
    const fab = page
      .getByTestId('fab-new-session')
      .or(page.locator('button[aria-label*="create" i], .fab, button:has-text("+")'));
    await expect(fab.first()).toBeVisible();
    await fab.first().click();

    const dialog = page.getByTestId('create-session-dialog');
    await expect(dialog).toBeVisible({ timeout: 10_000 });

    await dialog.locator('input[name="title"]').fill('Realme P2 Pro E2E Test');
    await dialog.locator('input[name="workspace"]').fill('demo');

    const cursorRadio = dialog.locator('input[value="cursor"]');
    if (await cursorRadio.count()) {
      await cursorRadio.check();
    } else {
      const cursorLabel = dialog.getByText('Cursor', { exact: true });
      if (await cursorLabel.count()) {
        await cursorLabel.click();
      }
    }

    await dialog.getByRole('button', { name: /Create/i }).click();
    const detail = page.getByTestId('session-detail');
    await expect(detail).toBeVisible({ timeout: 30_000 });
    await expect(detail.getByText('Realme P2 Pro E2E Test').first()).toBeVisible({ timeout: 15_000 });

    await page.screenshot({ path: path.join(shotDir, '02-session-created.png'), fullPage: true });

    // =====================
    // 3. MULTI-AGENT FLOW - Agent API Actions
    // =====================
    const actionsUrl = `${apiBase(BASE_URL)}/agent/actions`;
    const actionsResponse = await request.get(actionsUrl);
    expect(actionsResponse.ok()).toBeTruthy();

    const payload = await actionsResponse.json();
    const actions = Array.isArray(payload) ? payload : payload.actions;
    expect(Array.isArray(actions)).toBeTruthy();
    expect(actions.length).toBeGreaterThan(5);

    // =====================
    // 4. SESSION DETAIL - Tab Navigation
    // =====================
    const tabs = ['Transcript', 'Logs', 'Code', 'Preview', 'Changes', 'History', 'Activity'];

    for (const tabName of tabs) {
      const tab = detail.getByRole('tab', { name: tabName });
      if ((await tab.count()) === 0) {
        continue;
      }
      await tab.click();
      await expect(tab).toHaveAttribute('aria-selected', 'true');
    }

    // =====================
    // 5. CHANGES TAB
    // =====================
    const changesTab = detail.getByRole('tab', { name: 'Changes' });
    await changesTab.click();
    await expect(changesTab).toHaveAttribute('aria-selected', 'true');

    const changesPanel = page.getByTestId('changes-panel');
    const refreshBtn = changesPanel.locator('button:has-text("Refresh")').or(
      page.locator('button:has-text("Refresh")')
    );
    if (await refreshBtn.count()) {
      await refreshBtn.first().click();
    }

    const keepButton = page.locator('button:has-text("Keep")');
    const restoreButton = page.locator('button:has-text("Restore")');
    const noChangesText = page.locator(
      'text=/No file changes|No changes|nothing to review/i'
    );

    const hasChangesUI =
      (await keepButton.count()) > 0 ||
      (await restoreButton.count()) > 0 ||
      (await noChangesText.count()) > 0 ||
      (await changesPanel.count()) > 0;

    expect(hasChangesUI).toBeTruthy();

    await page.screenshot({ path: path.join(shotDir, '05-changes-tab.png'), fullPage: true });

    // =====================
    // 6. SEND PROMPT (optional — do not wait for full agent completion)
    // =====================
    const promptInput = detail.locator('app-agent-input-bar textarea').or(
      page.locator('textarea, input[placeholder*="Ask" i], input[placeholder*="prompt" i]')
    );
    if (await promptInput.count()) {
      await promptInput.first().fill('Test prompt from Realme P2 Pro E2E');
      await page.keyboard.press('Enter');
      await page.waitForTimeout(2500);
    }

    // App Home smoke (platform Org / Apps) — on mobile Apps lives in ⋯ menu
    const overflow = page.getByTestId('overflow-menu-btn');
    if (await overflow.isVisible().catch(() => false)) {
      await overflow.click();
      await page.waitForTimeout(200);
    }
    const appsBtn = page.getByTestId('app-home-btn');
    if (await appsBtn.isVisible().catch(() => false)) {
      await appsBtn.click({ force: true });
      await expect(page.getByTestId('app-home')).toBeVisible();
      await page.screenshot({ path: path.join(shotDir, '07-app-home.png'), fullPage: true });
      await page.locator('[data-testid="app-home"] button:has-text("Close")').click();
    }

    await page.screenshot({ path: path.join(shotDir, '06-final-state.png'), fullPage: true });
  });
});
