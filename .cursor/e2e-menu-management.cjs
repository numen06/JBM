const fs = require('fs');
const path = require('path');
const { createRequire } = require('module');
const { execSync } = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const frontendRequire = createRequire(path.join(ROOT, 'jbm-admin-vue', 'package.json'));
const { chromium } = frontendRequire('playwright-core');

const OUT_DIR = path.join(ROOT, '.cursor', 'screenshots');
const RESULT_FILE = path.join(ROOT, '.cursor', 'e2e-menu-management-result.json');
fs.mkdirSync(OUT_DIR, { recursive: true });

const BASE = process.env.JBM_E2E_BASE || 'http://127.0.0.1:5173';
const GATEWAY = process.env.JBM_GATEWAY || 'http://127.0.0.1:7777';
const CHROME = process.env.CHROME_EXE || 'C:/Program Files/Google/Chrome/Application/chrome.exe';

const result = {
  base: BASE,
  gateway: GATEWAY,
  startedAt: new Date().toISOString(),
  steps: [],
  api: {},
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

function getAdminToken() {
  execSync('python scripts/verify_menu_management.py', {
    cwd: ROOT,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  const json = JSON.parse(fs.readFileSync(RESULT_FILE, 'utf8'));
  if (json.status !== 'passed') throw new Error(json.error || 'verify failed');
  return json;
}

async function loginAdmin(page) {
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' });
  await waitReady(page);
  await page.fill('input[type="text"], input[autocomplete="username"]', 'admin');
  await page.fill('input[type="password"]', 'Admin@123');
  await page.locator('button[type="submit"], button:has-text("登录")').first().click();
  await page.waitForURL(/dashboard|system|\/$/, { timeout: 20000 }).catch(() => {});
  await waitReady(page);
}

async function main() {
  let browser;
  try {
    const apiResult = getAdminToken();
    result.api = apiResult.api;
    step('menu-api-checks', 'passed', apiResult.api);

    browser = await chromium.launch({ executablePath: CHROME, headless: true });
    const page = await browser.newPage();
    await loginAdmin(page);
    step('admin-login', 'passed');

    await page.goto(`${BASE}/system/menus`, { waitUntil: 'domcontentloaded' });
    await waitReady(page);
    await page.waitForSelector('table tbody tr', { timeout: 15000 });
    const prevBtn = page.locator('button:has-text("上一页")');
    await prevBtn.waitFor({ state: 'visible', timeout: 15000 });
    step('menu-page-load', 'passed');

    const paginationText = await page.locator('span').filter({ hasText: /条$/ }).first().textContent().catch(() => '');
    if (!paginationText || !/\d+/.test(paginationText)) {
      throw new Error(`未找到分页栏: ${paginationText || '(empty)'}`);
    }
    result.ui = { paginationText: paginationText.trim() };
    await shot(page, 'e2e-menu-pagination');
    step('menu-pagination-ui', 'passed', { paginationText: paginationText.trim() });

    await page.fill('input[placeholder="编码/名称/路径"]', '用户');
    await page.locator('button:has-text("搜索")').click();
    await waitReady(page);
    await shot(page, 'e2e-menu-app-scope');
    step('menu-search', 'passed');

    await page.locator('select').first().selectOption('platform');
    await page.locator('button:has-text("搜索")').click();
    await waitReady(page);
    await shot(page, 'e2e-menu-platform-protect');
    step('menu-platform-scope', 'passed');

    result.finishedAt = new Date().toISOString();
    result.status = 'passed';
    fs.writeFileSync(RESULT_FILE, JSON.stringify(result, null, 2), 'utf8');
    console.log('E2E passed:', RESULT_FILE);
  } catch (e) {
    result.status = 'failed';
    result.error = e.message;
    result.finishedAt = new Date().toISOString();
    fs.writeFileSync(RESULT_FILE, JSON.stringify(result, null, 2), 'utf8');
    console.error('E2E failed:', e.message);
    process.exitCode = 1;
  } finally {
    if (browser) await browser.close();
  }
}

main();
