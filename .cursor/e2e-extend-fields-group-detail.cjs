const fs = require('fs');
const path = require('path');
const { createRequire } = require('module');

const ROOT = path.resolve(__dirname, '..');
const frontendRequire = createRequire(path.join(ROOT, 'jbm-admin-vue', 'package.json'));
const { chromium } = frontendRequire('playwright-core');

const BASE = process.env.JBM_E2E_BASE || 'http://127.0.0.1:5173';
const CHROME = process.env.CHROME_EXE || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const OUT_DIR = path.join(ROOT, '.cursor', 'screenshots');
const RESULT_FILE = path.join(ROOT, '.cursor', 'e2e-extend-fields-group-detail-result.json');
fs.mkdirSync(OUT_DIR, { recursive: true });

const result = {
  base: BASE,
  startedAt: new Date().toISOString(),
  steps: [],
  screenshots: [],
};

function step(name, status, extra = {}) {
  result.steps.push({ name, status, at: new Date().toISOString(), ...extra });
}

async function waitReady(page) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
}

async function shot(page, name) {
  const file = path.join(OUT_DIR, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  result.screenshots.push(file);
  return file;
}

async function login(page) {
  await page.goto(`${BASE}/login`);
  await waitReady(page);
  await page.locator('[data-testid="login-username"]').fill('admin');
  await page.locator('[data-testid="login-password"]').fill('Admin@123');
  await page.locator('[data-testid="login-vcode"]').fill('9999');
  await page.locator('form button[type="submit"]').first().click();
  await page.waitForURL(/\/dashboard/, { timeout: 30000 });
  await waitReady(page);
  step('admin login', 'passed');
}

async function ensureSeedForm(page) {
  await page.goto(`${BASE}/system/extend-fields`);
  await waitReady(page);

  const body = await page.locator('body').innerText();
  if (/接口异常|401|403|缺少租户/.test(body)) {
    throw new Error(`extend fields page has API/auth error: ${body.slice(0, 500)}`);
  }

  const listItems = page.locator('[data-testid="field-group-item"]').filter({ hasText: /cen_form_|CEN_FORM|FORM/i });
  if (await listItems.first().isVisible().catch(() => false)) {
    step('field group list loaded', 'passed', { mode: 'existing' });
    return null;
  }

  const suffix = `${Date.now()}`.slice(-8);
  const code = `ui_fields_${suffix}`;
  await page.locator('#formCode').fill(code);
  await page.getByRole('button', { name: /新建/ }).click();
  await page.locator('#formName').fill(`UI_FIELDS_${suffix}`);
  await page.locator('input[placeholder="field_name"]').first().fill('customerLevel');
  await page.locator('input[placeholder="显示标签"]').first().fill('Customer Level');
  await page.locator('input[type="checkbox"]').first().uncheck().catch(() => {});
  await page.getByRole('button', { name: /^保存$/ }).click();
  await page.waitForTimeout(1500);
  step('seed field group created from UI', 'passed', { formCode: code });
  return code;
}

async function clickGroupAndAssertDetail(page, preferredCode) {
  await page.goto(`${BASE}/system/extend-fields`);
  await waitReady(page);

  let groupButton = preferredCode
    ? page.locator('[data-testid="field-group-item"]').filter({ hasText: preferredCode }).first()
    : page.locator('[data-testid="field-group-item"]').filter({ hasText: /cen_form_|CEN_FORM/i }).first();

  await groupButton.waitFor({ state: 'visible', timeout: 20000 });
  const groupText = (await groupButton.innerText()).trim();
  await groupButton.click();
  await page.waitForTimeout(1200);

  const rightText = await page.locator('main').innerText();
  if (!/字段定义/.test(rightText)) {
    throw new Error(`right panel did not switch to field definition view. group=${groupText}; right=${rightText.slice(0, 800)}`);
  }
  if (!/(note|customerLevel|字段名)/.test(rightText)) {
    throw new Error(`right panel has no field rows after group click. group=${groupText}; right=${rightText.slice(0, 800)}`);
  }
  await shot(page, 'e2e-extend-fields-group-detail');
  step('click field group shows right detail', 'passed', { groupText });
}

(async () => {
  const browser = await chromium.launch({
    executablePath: CHROME,
    headless: true,
  });
  try {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 980 } });
    const page = await ctx.newPage();
    await login(page);
    const code = await ensureSeedForm(page);
    await clickGroupAndAssertDetail(page, code);
    await ctx.close();
    result.status = 'passed';
  } catch (err) {
    result.status = 'failed';
    result.error = err && err.stack ? err.stack : String(err);
    throw err;
  } finally {
    result.finishedAt = new Date().toISOString();
    fs.writeFileSync(RESULT_FILE, JSON.stringify(result, null, 2), 'utf8');
    await browser.close();
  }
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
