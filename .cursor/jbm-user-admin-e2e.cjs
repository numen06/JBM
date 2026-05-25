const fs = require('fs');
const path = require('path');
const { createRequire } = require('module');

const ROOT = path.resolve(__dirname, '..');
const frontendRequire = createRequire(path.join(ROOT, 'jbm-admin-vue', 'package.json'));
const { chromium } = frontendRequire('playwright-core');
const OUT_DIR = path.join(ROOT, '.cursor', 'screenshots');
const RESULT_FILE = path.join(ROOT, '.cursor', 'jbm-user-admin-e2e-result.json');
fs.mkdirSync(OUT_DIR, { recursive: true });

const BASE = process.env.JBM_E2E_BASE || 'http://127.0.0.1:5173';
const CHROME = process.env.CHROME_EXE || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const suffix = `${Date.now()}`.slice(-8);
const user = {
  name: `jbm_ui_${suffix}`,
  password: 'UiTest@123456',
  nick: `UI Test ${suffix}`,
  email: `jbm_ui_${suffix}@example.com`,
  mobile: `139${suffix.slice(-8)}`,
};

const result = {
  base: BASE,
  user: user.name,
  startedAt: new Date().toISOString(),
  steps: [],
  screenshots: [],
};

function step(name, status, extra = {}) {
  result.steps.push({ name, status, at: new Date().toISOString(), ...extra });
}

async function shot(page, name) {
  const file = path.join(OUT_DIR, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  result.screenshots.push(file);
  return file;
}

async function waitReady(page) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
}

async function clickText(page, patterns, opts = {}) {
  const timeout = opts.timeout ?? 15000;
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    const buttons = await page.locator('button,a').elementHandles();
    for (const el of buttons) {
      const text = ((await el.innerText().catch(() => '')) || '').trim();
      if (patterns.some((p) => text.includes(p))) {
        await el.click();
        return text;
      }
    }
    await page.waitForTimeout(250);
  }
  throw new Error(`button/link not found: ${patterns.join('|')}`);
}

async function clickVisibleButton(page, label) {
  const buttons = await page.locator('button:visible').elementHandles();
  for (const el of buttons) {
    const text = ((await el.innerText().catch(() => '')) || '').trim();
    if (text.includes(label)) {
      await el.click();
      return;
    }
  }
  throw new Error(`visible button not found: ${label}`);
}

async function getDialog(page) {
  const dialog = page.locator('body > .fixed').last();
  await dialog.waitFor({ state: 'visible', timeout: 10000 });
  return dialog;
}

async function saveDialog(page) {
  const dialog = await getDialog(page);
  const buttons = await dialog.locator('button').elementHandles();
  const btn = buttons[buttons.length - 1];
  if (!btn) throw new Error('dialog save button not found');
  await btn.click();
}

async function expectDialogClosed(page, actionName) {
  const dialog = page.locator('body > .fixed').last();
  try {
    await dialog.waitFor({ state: 'hidden', timeout: 10000 });
  } catch (e) {
    const text = await dialog.innerText().catch(() => '');
    throw new Error(`${actionName} dialog did not close after save: ${text.slice(0, 500)}`);
  }
}

async function login(page, username, password) {
  await page.goto(`${BASE}/login`);
  await waitReady(page);
  await page.locator('[data-testid="login-username"]').fill(username);
  await page.locator('[data-testid="login-password"]').fill(password);
  await page.locator('[data-testid="login-vcode"]').fill('9999');
  await page.locator('form button[type="submit"]').first().click();
  await page.waitForURL(/\/dashboard/, { timeout: 30000 });
  await waitReady(page);
}

async function register(page) {
  await page.goto(`${BASE}/register`);
  await waitReady(page);
  const inputs = page.locator('form input');
  await inputs.nth(0).fill(user.name);
  await inputs.nth(1).fill(user.password);
  await inputs.nth(2).fill(user.password);
  await inputs.nth(3).fill(user.nick);
  await inputs.nth(4).fill(user.email);
  await inputs.nth(5).fill(user.mobile);
  await inputs.nth(6).fill('9999');
  await page.locator('form button[type="submit"]').click();
  await page.waitForURL(/\/login/, { timeout: 30000 });
  step('user register from UI', 'passed');
}

async function userApplyDeveloper(browser) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 980 } });
  const page = await ctx.newPage();
  await register(page);
  await login(page, user.name, user.password);
  await shot(page, 'e2e-user-dashboard-after-register');
  step('user login after register', 'passed');

  await page.goto(`${BASE}/developer`);
  await waitReady(page);
  await shot(page, 'e2e-user-developer-self-service');
  await clickText(page, ['申请成为开发者', '鐢宠']);
  await page.waitForTimeout(1000);
  await shot(page, 'e2e-user-developer-apply-submitted');
  step('user submit developer application', 'passed');

  await page.goto(`${BASE}/developer/api-keys`);
  await waitReady(page);
  const disabled = await page.locator('button').filter({ hasText: /API Key/i }).last().isDisabled().catch(() => false);
  await shot(page, 'e2e-user-apikey-before-admin-approval');
  if (!disabled) throw new Error('API Key create button should be disabled before developer approval');
  step('api key creation blocked before approval', 'passed');
  await ctx.close();
}

async function approveDeveloper(browser) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 980 } });
  const page = await ctx.newPage();
  await login(page, 'admin', 'Admin@123');
  await page.goto(`${BASE}/developer`);
  await waitReady(page);
  await clickText(page, ['待审批', '寰呭']);
  await page.waitForTimeout(1000);
  const row = page.locator('tr').filter({ hasText: user.name }).first();
  await row.waitFor({ state: 'visible', timeout: 20000 });
  await shot(page, 'e2e-admin-developer-pending-user');
  await row.locator('button').last().click();
  await page.waitForTimeout(1500);
  step('admin approve developer from UI', 'passed');
  await ctx.close();
}

async function userCreateApiKey(browser) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 980 } });
  const page = await ctx.newPage();
  await login(page, user.name, user.password);
  await page.goto(`${BASE}/developer/api-keys`);
  await waitReady(page);
  const create = page.locator('button').filter({ hasText: /API Key/i }).last();
  await create.waitFor({ state: 'visible', timeout: 15000 });
  await page.waitForFunction(() => {
    const buttons = [...document.querySelectorAll('button')];
    const b = buttons.reverse().find((x) => /API Key/i.test(x.textContent || ''));
    return b && !b.disabled;
  }, null, { timeout: 20000 });
  await create.click();
  const dialog = await getDialog(page);
  const inputs = dialog.locator('input');
  await inputs.nth(0).fill(`ui-key-${suffix}`);
  await inputs.nth(1).fill(`client-${suffix}`);
  await inputs.nth(2).fill('created by browser e2e');
  await saveDialog(page);
  await page.waitForTimeout(1500);
  const secretDialog = page.locator('body > .fixed').last();
  if (await secretDialog.isVisible().catch(() => false)) {
    const text = await secretDialog.innerText().catch(() => '');
    if (/Secret/i.test(text)) {
      await secretDialog.locator('button').last().click();
      await expectDialogClosed(page, 'api key secret');
    }
  }
  await shot(page, 'e2e-user-apikey-after-approval-created');
  step('user create api key after approval', 'passed');
  await ctx.close();
}

async function createAndDeleteGatewayItem(page, cfg) {
  await page.goto(`${BASE}${cfg.url}`);
  await waitReady(page);
  const body = await page.locator('body').innerText();
  if (/接口异常|缺少签名|401|403/.test(body)) {
    await shot(page, cfg.debugShot);
    throw new Error(`${cfg.name} page has API/auth error: ${body.slice(0, 300)}`);
  }

  await clickText(page, ['新建', '鏂板缓']);
  let dialog = await getDialog(page);
  let inputs = dialog.locator('input');
  for (let i = 0; i < cfg.create.length; i += 1) {
    await inputs.nth(i).fill(cfg.create[i]);
  }
  if (cfg.selects) {
    const selects = dialog.locator('select');
    for (let i = 0; i < cfg.selects.length; i += 1) {
      await selects.nth(i).selectOption(String(cfg.selects[i]));
    }
  }
  await saveDialog(page);
  await expectDialogClosed(page, `${cfg.name} create`);
  await page.waitForTimeout(1500);

  if (cfg.keyword) {
    await page.locator('input:visible').first().fill(cfg.keyword);
    await clickText(page, ['查询', '鏌ヨ']);
    await page.waitForTimeout(1200);
  }

  let row = page.locator('tr').filter({ hasText: cfg.keyword || cfg.create[0] }).first();
  await row.waitFor({ state: 'visible', timeout: 20000 });
  await shot(page, cfg.createdShot);

  await row.locator('button').first().click();
  dialog = await getDialog(page);
  inputs = dialog.locator('input');
  await inputs.nth(0).fill(`${cfg.create[0]}-edited`);
  if (cfg.editSecond) await inputs.nth(cfg.editSecond.index).fill(cfg.editSecond.value);
  await saveDialog(page);
  await expectDialogClosed(page, `${cfg.name} edit`);
  await page.waitForTimeout(1500);
  step(`${cfg.name} create and edit`, 'passed');

  if (cfg.keyword) {
    await page.locator('input:visible').first().fill(`${cfg.create[0]}-edited`);
    await clickText(page, ['查询', '鏌ヨ']);
    await page.waitForTimeout(1200);
  }
  row = page.locator('tr').filter({ hasText: `${cfg.create[0]}-edited` }).first();
  await row.waitFor({ state: 'visible', timeout: 20000 });
  page.once('dialog', (d) => d.accept());
  await row.locator('button').last().click();
  await page.waitForTimeout(1500);
  step(`${cfg.name} delete`, 'passed');
}

async function adminGatewayAndOnline(browser) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 980 } });
  const page = await ctx.newPage();
  await login(page, 'admin', 'Admin@123');

  await page.goto(`${BASE}/system/online-users`);
  await waitReady(page);
  const onlineText = await page.locator('body').innerText();
  if (/接口异常|缺少签名|401|403/.test(onlineText)) {
    await shot(page, 'e2e-admin-online-users-error');
    throw new Error(`online users page error: ${onlineText.slice(0, 300)}`);
  }
  await shot(page, 'e2e-admin-online-users-loaded');
  step('admin online users page loads', 'passed');

  await createAndDeleteGatewayItem(page, {
    name: 'gateway route',
    url: '/gateway/routes',
    create: [`ui-route-${suffix}`, `/ui-e2e/${suffix}/**`, 'lb://jbm-cluster-platform-center', ''],
    selects: [1],
    keyword: `ui-route-${suffix}`,
    editSecond: { index: 1, value: `/ui-e2e/${suffix}/v2/**` },
    createdShot: 'e2e-admin-gateway-route-created',
    debugShot: 'e2e-admin-gateway-route-error',
  });

  await createAndDeleteGatewayItem(page, {
    name: 'gateway rate limit',
    url: '/gateway/rate-limit',
    create: [`ui-rate-${suffix}`, '77'],
    selects: ['url', 'seconds'],
    keyword: `ui-rate-${suffix}`,
    editSecond: { index: 1, value: '88' },
    createdShot: 'e2e-admin-gateway-rate-created',
    debugShot: 'e2e-admin-gateway-rate-error',
  });

  await createAndDeleteGatewayItem(page, {
    name: 'gateway ip limit',
    url: '/gateway/ip-limit',
    create: [`ui-ip-${suffix}`, '203.0.113.77'],
    selects: [1],
    keyword: `ui-ip-${suffix}`,
    editSecond: { index: 1, value: '203.0.113.78' },
    createdShot: 'e2e-admin-gateway-ip-created',
    debugShot: 'e2e-admin-gateway-ip-error',
  });

  await ctx.close();
}

(async () => {
  const browser = await chromium.launch({ executablePath: CHROME, headless: false });
  try {
    await userApplyDeveloper(browser);
    await approveDeveloper(browser);
    await userCreateApiKey(browser);
    await adminGatewayAndOnline(browser);
    result.status = 'passed';
  } catch (e) {
    result.status = 'failed';
    result.error = e && e.stack ? e.stack : String(e);
    throw e;
  } finally {
    result.finishedAt = new Date().toISOString();
    fs.writeFileSync(RESULT_FILE, JSON.stringify(result, null, 2));
    await browser.close();
  }
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
