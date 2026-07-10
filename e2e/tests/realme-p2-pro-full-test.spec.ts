import { test } from '@playwright/test';

// Realme P2 Pro viewport (common mobile size)
const REALME_P2_PRO = { width: 360, height: 780 };

test.describe('Realme P2 Pro - Full Screen Testing', () => {

  test.use({
    viewport: REALME_P2_PRO,
    userAgent: 'Mozilla/5.0 (Linux; Android 13; Realme P2 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    isMobile: true,
    hasTouch: true,
  });

  test('Realme P2 Pro - Complete Mobile Flow + Screenshots', async ({ page }) => {
    test.setTimeout(120_000);

    const baseURL = process.env['APP_URL'] || 'http://localhost:4200';
    const username = process.env['CSS_USER'] || 'admin';
    const password = process.env['CSS_PASSWORD'] || 'admin123';

    // ============================================
    // 1. LOGIN SCREEN
    // ============================================
    await page.goto(baseURL);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await page.screenshot({
      path: 'screenshots/realme-p2-pro/01-login-screen.png',
      fullPage: true,
    });

    // Login (CSS overlay when enabled; otherwise shell is already visible)
    const loginOverlay = page.getByTestId('login-overlay');
    const usernameField = page.getByTestId('login-username')
      .or(page.getByRole('textbox', { name: /user/i }))
      .or(page.locator('input[type="text"], input[placeholder*="user" i]'));
    const passwordField = page.getByTestId('login-password')
      .or(page.getByRole('textbox', { name: /pass/i }))
      .or(page.locator('input[type="password"], input[placeholder*="pass" i]'));
    const loginButton = page.getByTestId('login-submit')
      .or(page.locator('button:has-text("Login"), button[type="submit"], button:has-text("Sign in")'));

    if (await loginOverlay.isVisible().catch(() => false) || await usernameField.first().isVisible().catch(() => false)) {
      await usernameField.first().fill(username);
      await passwordField.first().fill(password);
      await loginButton.first().click();
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2500);
    }

    await page.screenshot({
      path: 'screenshots/realme-p2-pro/02-after-login.png',
      fullPage: true,
    });

    // ============================================
    // 2. MAIN SESSION LIST + FAB
    // ============================================
    await page.screenshot({
      path: 'screenshots/realme-p2-pro/03-session-list.png',
      fullPage: true,
    });

    // ============================================
    // 3. CREATE SESSION via FAB (Mobile)
    // ============================================
    const fab = page.getByTestId('fab-new-session')
      .or(page.locator('button[aria-label*="create" i], .fab, button:has-text("+"), [data-testid="fab-create"]'));

    if (await fab.count() > 0) {
      await fab.first().click();
      await page.waitForTimeout(1200);
      await page.screenshot({
        path: 'screenshots/realme-p2-pro/04-create-session-fab.png',
        fullPage: true,
      });

      // Fill session details if form appears
      const promptInput = page.locator('textarea, input[placeholder*="prompt" i], input[name="title"]');
      if (await promptInput.count() > 0) {
        await promptInput.first().fill('Test prompt on Realme P2 Pro');
      }

      // Close dialog
      const cancel = page.getByRole('button', { name: 'Cancel' });
      if (await cancel.count() > 0) {
        await cancel.click();
      } else {
        await page.keyboard.press('Escape');
      }
      await page.waitForTimeout(800);
    }

    // ============================================
    // 4. OPEN A SESSION (create one if list is empty)
    // ============================================
    const mobileList = page.getByTestId('mobile-session-list');
    let firstSession = mobileList.getByTestId('session-card').first();

    if (!(await firstSession.isVisible().catch(() => false))) {
      await fab.first().click();
      const dialog = page.getByTestId('create-session-dialog');
      if (await dialog.isVisible().catch(() => false)) {
        await dialog.locator('input[name="title"]').fill('Realme P2 Pro QA');
        await dialog.locator('input[name="workspace"]').fill('demo');
        const antigravity = dialog.locator('input[value="antigravity"]');
        if (await antigravity.count() > 0) {
          await antigravity.check();
        }
        await dialog.getByRole('button', { name: /Create/i }).click();
        await page.waitForTimeout(2000);
      }
    } else {
      await firstSession.click();
    }

    const sessionDetail = page.getByTestId('session-detail');
    if (await sessionDetail.isVisible({ timeout: 15_000 }).catch(() => false)) {
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);

      await page.screenshot({
        path: 'screenshots/realme-p2-pro/05-session-detail.png',
        fullPage: true,
      });

      // ============================================
      // 5. TEST ALL TABS (Mobile)
      // ============================================
      const tabs = ['Transcript', 'Logs', 'Code', 'Preview', 'Changes', 'History'];

      for (const tabName of tabs) {
        const tab = page.locator(`button:has-text("${tabName}"), [role="tab"]:has-text("${tabName}"), .tab:has-text("${tabName}")`);

        if (await tab.count() > 0) {
          await tab.first().click();
          await page.waitForTimeout(1800);
          await page.screenshot({
            path: `screenshots/realme-p2-pro/06-${tabName.toLowerCase()}-tab.png`,
            fullPage: true,
          });
        }
      }

      // ============================================
      // 6. SUB-AGENT / TASK PANEL (if exists)
      // ============================================
      const subAgentBtn = page.locator('button:has-text("Sub-agent"), button:has-text("Tasks"), [aria-label*="sub"]');
      if (await subAgentBtn.count() > 0) {
        await subAgentBtn.first().click();
        await page.waitForTimeout(1500);
        await page.screenshot({
          path: 'screenshots/realme-p2-pro/07-subagent-panel.png',
          fullPage: true,
        });
      }
    }

    // ============================================
    // 7. DRAWER / MENU (Mobile Navigation)
    // ============================================
    const menuButton = page.locator('button[aria-label*="menu" i], .menu-button, button[aria-label*="drawer" i]');
    if (await menuButton.count() > 0) {
      await menuButton.first().click();
      await page.waitForTimeout(1000);
      await page.screenshot({
        path: 'screenshots/realme-p2-pro/08-drawer-menu.png',
        fullPage: true,
      });
    }

    // ============================================
    // 8. FINAL OVERVIEW
    // ============================================
    await page.screenshot({
      path: 'screenshots/realme-p2-pro/09-final-overview.png',
      fullPage: true,
    });

    console.log('✅ Realme P2 Pro testing completed! All screenshots saved.');
  });
});
