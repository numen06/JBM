const fs = require('fs');
const path = require('path');
const { createRequire } = require('module');

const ROOT = path.resolve(__dirname, '..');
const frontendRequire = createRequire(path.join(ROOT, 'jbm-admin-vue', 'package.json'));
const { chromium } = frontendRequire('playwright-core');

const OUT_DIR = path.join(ROOT, '.cursor', 'screenshots');
const RESULT_FILE = path.join(ROOT, '.cursor', 'e2e-org-utf8-result.json');
fs.mkdirSync(OUT_DIR, { recursive: true });

const BASE = process.env.JBM_E2E_BASE || 'http://127.0.0.1:5173';
const CHROME = process.env.CHROME_EXE || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const ORG_NAME = `测试组织UTF8_${`${Date.now()}`.slice(-6)}`;

const result = {
  base: BASE,
  orgName: ORG_NAME,
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
  await dialog.locator('button', { hasText: '保存' }).click();
}

async function login(page) {
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' });
  await page.fill('input[type="text"], input[name="username"], input[autocomplete="username"]', 'admin');
  await page.fill('input[type="password"]', 'Admin@123');
  await clickVisibleButton(page, '登录');
  await page.waitForURL(/\/(dashboard|system)/, { timeout: 20000 });
  step('login', 'passed');
}

async function main() {
  const browser = await chromium.launch({
    executablePath: CHROME,
    headless: true,
    args: ['--disable-dev-shm-usage'],
  });
  const page = await browser.newPage();
  try {
    await login(page);
    await page.goto(`${BASE}/system/orgs`, { waitUntil: 'domcontentloaded' });
    await waitReady(page);
    step('open_org_list', 'passed');

    await clickVisibleButton(page, '新建');
    const dialog = await getDialog(page);
    const nameInput = dialog.locator('input').first();
    await nameInput.fill(ORG_NAME);
    const savePromise = page.waitForResponse(
      (res) => res.url().includes('/baseOrg/save') && res.status() === 200,
      { timeout: 15000 },
    );
    await saveDialog(page);
    const saveResp = await savePromise;
    const saveJson = await saveResp.json();
    const savedName = saveJson?.result?.orgName;
    if (savedName !== ORG_NAME) {
      throw new Error(`保存响应 orgName 乱码: ${JSON.stringify(savedName)} != ${JSON.stringify(ORG_NAME)}`);
    }
    step('create_org', 'passed', { orgName: ORG_NAME, savedName });

    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitReady(page);
    const keywordInput = page.locator('input[placeholder="组织名称"]');
    await keywordInput.fill(ORG_NAME);
    await clickVisibleButton(page, '搜索');
    await waitReady(page);
    await page.waitForTimeout(800);
    const row = page.locator('tbody tr').filter({ hasText: ORG_NAME }).first();
    await row.waitFor({ state: 'visible', timeout: 15000 });
    step('list_after_refresh', 'passed');
    await shot(page, 'e2e-org-create-utf8');

    await page.goto(`${BASE}/system/users`, { waitUntil: 'domcontentloaded' });
    await waitReady(page);
    await clickVisibleButton(page, '新建');
    const userDialog = await getDialog(page);
    const orgSelect = userDialog.locator('select, [role="combobox"]').first();
    await orgSelect.click().catch(() => {});
    await page.waitForTimeout(500);
    const treeText = await userDialog.innerText();
    if (!treeText.includes('默认组织')) {
      throw new Error('组织选择器未显示默认组织中文');
    }
    step('org_tree_selector', 'passed');
    await shot(page, 'e2e-org-tree-utf8');
    await page.keyboard.press('Escape').catch(() => {});

    result.status = 'passed';
    result.finishedAt = new Date().toISOString();
    fs.writeFileSync(RESULT_FILE, JSON.stringify(result, null, 2), 'utf8');
    console.log('PASS', RESULT_FILE);
  } catch (err) {
    result.status = 'failed';
    result.error = String(err && err.message ? err.message : err);
    step('fatal', 'failed', { error: result.error });
    await shot(page, 'e2e-org-create-utf8-failed').catch(() => {});
    fs.writeFileSync(RESULT_FILE, JSON.stringify(result, null, 2), 'utf8');
    console.error(result.error);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
}

main();
