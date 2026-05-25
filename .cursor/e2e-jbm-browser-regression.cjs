const fs = require('fs');
const path = require('path');
const { createRequire } = require('module');

const ROOT = path.resolve(__dirname, '..');
const frontendRequire = createRequire(path.join(ROOT, 'jbm-admin-vue', 'package.json'));
const { chromium } = frontendRequire('playwright-core');

const BASE = process.env.JBM_E2E_BASE || 'http://127.0.0.1:5173';
const CHROME = process.env.CHROME_EXE || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const HEADLESS = process.env.JBM_E2E_HEADLESS !== 'false';
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');
const OUT_DIR = path.join(ROOT, '.cursor', 'runs', `browser-regression-${RUN_ID}`);
const SCREEN_DIR = path.join(OUT_DIR, 'screenshots');
const RESULT_FILE = path.join(OUT_DIR, 'result.json');

fs.mkdirSync(SCREEN_DIR, { recursive: true });

const suffix = `${Date.now()}`.slice(-8);
const user = {
  username: `jbm_e2e_${suffix}`,
  password: 'UiTest@123456',
  nick: `JBM E2E ${suffix}`,
  email: `jbm_e2e_${suffix}@example.com`,
  mobile: `139${suffix.slice(-8)}`,
};

const result = {
  base: BASE,
  runId: RUN_ID,
  startedAt: new Date().toISOString(),
  user: user.username,
  steps: [],
  screenshots: [],
  networkErrors: [],
  checks: {},
};

function addStep(name, status, details = {}) {
  const step = { name, status, at: new Date().toISOString(), ...details };
  result.steps.push(step);
  return step;
}

function failStep(name, error, details = {}) {
  return addStep(name, 'failed', {
    error: error && error.message ? error.message : String(error),
    ...details,
  });
}

async function shot(page, name) {
  const file = path.join(SCREEN_DIR, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  result.screenshots.push(file);
  return file;
}

async function waitReady(page) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
}

async function clickSubmit(page) {
  const submit = page.locator('form button[type="submit"]').first();
  await submit.waitFor({ state: 'visible', timeout: 15000 });
  await submit.click();
}

async function pageText(page) {
  return (await page.locator('body').innerText().catch(() => '')).trim();
}

async function assertProtectedPage(page, name, options = {}) {
  await waitReady(page);
  const url = page.url();
  const text = await pageText(page);
  if (/\/login(?:\?|$)/.test(url)) {
    throw new Error(`${name} redirected to login`);
  }
  if (!options.allowStatusText && /\b(404|401|403|500)\b|Network Error|Request failed/i.test(text)) {
    throw new Error(`${name} shows an error: ${text.slice(0, 500)}`);
  }
  if (text.length < 20) {
    throw new Error(`${name} rendered almost no text`);
  }
  return { url, textLength: text.length };
}

async function fillIfVisible(page, selector, value) {
  const locator = page.locator(selector).first();
  if (await locator.isVisible().catch(() => false)) {
    await locator.fill(value);
  }
}

async function tryLogin(page, username, password, clientId, clientSecret) {
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' });
  await waitReady(page);
  await page.locator('[data-testid="login-username"]').fill(username);
  await page.locator('[data-testid="login-password"]').fill(password);
  await page.locator('[data-testid="login-vcode"]').fill('9999');
  await fillIfVisible(page, '[data-testid="login-client-id"]', clientId);
  await fillIfVisible(page, '[data-testid="login-client-secret"]', clientSecret);
  await clickSubmit(page);
  await page.waitForURL(/\/dashboard/, { timeout: 25000 });
  await waitReady(page);
}

async function loginAny(page, candidates, name) {
  let lastError;
  for (const candidate of candidates) {
    try {
      await tryLogin(page, candidate.username, candidate.password, candidate.clientId, candidate.clientSecret);
      addStep(name, 'passed', { username: candidate.username, clientId: candidate.clientId });
      return candidate;
    } catch (error) {
      lastError = error;
      await shot(page, `${name}-failed-${candidate.username}-${candidate.clientId}`).catch(() => {});
    }
  }
  throw lastError || new Error(`${name} failed`);
}

async function registerFromUi(page) {
  await page.goto(`${BASE}/register`, { waitUntil: 'domcontentloaded' });
  await waitReady(page);
  const inputs = page.locator('form input');
  await inputs.nth(0).fill(user.username);
  await inputs.nth(1).fill(user.password);
  await inputs.nth(2).fill(user.password);
  await inputs.nth(3).fill(user.nick);
  await inputs.nth(4).fill(user.email);
  await inputs.nth(5).fill(user.mobile);
  await inputs.nth(6).fill('9999');
  await clickSubmit(page);
  await page.waitForURL(/\/login/, { timeout: 45000 });
  await waitReady(page);
  await shot(page, '01-register-success');
  addStep('register-from-ui', 'passed');
}

async function api(page, method, url, body) {
  return page.evaluate(
    async ({ method, url, body }) => {
      const token = localStorage.getItem('jbm_access_token') || '';
      const response = await fetch(url, {
        method,
        headers: {
          Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}`,
          tenantId: '0',
          'Content-Type': 'application/json;charset=UTF-8',
        },
        body: body == null ? undefined : JSON.stringify(body),
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch {
        json = null;
      }
      return { status: response.status, text, json };
    },
    { method, url, body },
  );
}

function unwrapResult(resp, name) {
  if (resp.status < 200 || resp.status >= 300) {
    throw new Error(`${name} http ${resp.status}: ${resp.text.slice(0, 500)}`);
  }
  const body = resp.json;
  if (!body || (body.success !== true && body.code !== 200)) {
    throw new Error(`${name} api failed: ${resp.text.slice(0, 500)}`);
  }
  return body.result;
}

async function checkOnlineConsistency(page) {
  const stats = unwrapResult(await api(page, 'GET', '/user/statistics'), 'user statistics');
  const online = unwrapResult(
    await api(page, 'POST', '/online/pageList', {
      pageForm: { currPage: 1, pageSize: 20 },
    }),
    'online pageList',
  );
  const statsOnline = Number(stats && stats.onlineUser);
  const total = Number(online && online.total);
  const list = Array.isArray(online && online.contents)
    ? online.contents
    : Array.isArray(online && online.list)
      ? online.list
      : [];
  result.checks.online = { statsOnline, total, rowCount: list.length };
  if (!Number.isFinite(statsOnline) || !Number.isFinite(total)) {
    throw new Error(`online numbers are not numeric: ${JSON.stringify(result.checks.online)}`);
  }
  if (total < 1 || list.length < 1) {
    throw new Error(`online list did not include current session: ${JSON.stringify(result.checks.online)}`);
  }
  if (statsOnline !== total) {
    throw new Error(`online statistics mismatch: stats=${statsOnline}, listTotal=${total}`);
  }
  addStep('online-statistics-match-list', 'passed', result.checks.online);
}

async function openPage(page, url, name, options = {}) {
  await page.goto(`${BASE}${url}`, { waitUntil: 'domcontentloaded' });
  const info = await assertProtectedPage(page, name, options);
  if (options.selector) {
    await page.locator(options.selector).first().waitFor({ state: 'visible', timeout: 20000 });
  }
  if (options.screenshotBeforeAssert) {
    await shot(page, options.screenshot || name);
  }
  if (options.expectPagination) {
    const paginationText = await page.locator('body').innerText();
    if (!/\d+\s*\/\s*\d+/.test(paginationText)) {
      throw new Error(`${name} has no visible pagination marker`);
    }
  }
  if (!options.screenshotBeforeAssert) {
    await shot(page, options.screenshot || name);
  }
  addStep(name, 'passed', info);
}

async function checkOrgManagement(page) {
  await page.goto(`${BASE}/system/orgs`, { waitUntil: 'domcontentloaded' });
  const info = await assertProtectedPage(page, 'org-management-tree-page');
  await page.locator('aside').first().waitFor({ state: 'visible', timeout: 20000 });
  await page.locator('main').first().waitFor({ state: 'visible', timeout: 20000 });
  await shot(page, '07-org-management-tree');
  addStep('org-management-tree-page', 'passed', info);

  const toggleButtons = page.locator('div.inline-flex.rounded-md button');
  await toggleButtons.nth(1).click();
  await waitReady(page);
  await page.locator('table').first().waitFor({ state: 'visible', timeout: 20000 });
  const text = await page.locator('body').innerText();
  if (!/\d+\s*\/\s*\d+/.test(text)) {
    throw new Error('org-management-list-page has no visible pagination marker');
  }
  await shot(page, '08-org-management-list');
  addStep('org-management-list-page', 'passed');
}

async function checkExtendFieldGroup(page) {
  await page.goto(`${BASE}/system/extend-fields`, { waitUntil: 'domcontentloaded' });
  await assertProtectedPage(page, 'extend-fields');
  const groups = page.locator('[data-testid="field-group-item"]');
  await groups.first().waitFor({ state: 'visible', timeout: 25000 });
  const before = await page.locator('main').innerText();
  await groups.first().click();
  await page.waitForTimeout(1000);
  const after = await page.locator('main').innerText();
  await shot(page, '06-extend-fields-group-detail');
  if (after.length <= before.length * 0.5) {
    throw new Error('field group click did not keep a meaningful right-side detail area');
  }
  addStep('extend-field-group-click', 'passed', { beforeLength: before.length, afterLength: after.length });
}

async function main() {
  let browser;
  try {
    browser = await chromium.launch({
      executablePath: CHROME,
      headless: HEADLESS,
      slowMo: HEADLESS ? 0 : 80,
      args: ['--disable-dev-shm-usage'],
    });

    const context = await browser.newContext({ viewport: { width: 1440, height: 980 } });
    const page = await context.newPage();
    page.on('response', (response) => {
      const url = response.url();
      if (!url.startsWith(BASE) && !url.startsWith('http://127.0.0.1:5173')) return;
      const status = response.status();
      if (status >= 400 && !/\.(png|jpg|jpeg|svg|ico|map)(\?|$)/i.test(url)) {
        result.networkErrors.push({ status, url });
      }
    });

    await registerFromUi(page);
    await loginAny(
      page,
      [
        { username: user.username, password: user.password, clientId: 'demo', clientSecret: 'demo123' },
        {
          username: user.username,
          password: user.password,
          clientId: 'jbmSeedDevAppKey00000001',
          clientSecret: 'jbmSeedDevSecret0000000001',
        },
      ],
      'new-user-login',
    );
    await shot(page, '02-new-user-dashboard');
    await openPage(page, '/docs', 'api-wiki-public', {
      allowStatusText: true,
      screenshot: '03-api-wiki',
    });

    await page.evaluate(() => localStorage.clear());
    await loginAny(
      page,
      [
        { username: 'admin', password: 'Admin@123', clientId: 'demo', clientSecret: 'demo123' },
        {
          username: 'admin',
          password: 'admin',
          clientId: 'jbmSeedDevAppKey00000001',
          clientSecret: 'jbmSeedDevSecret0000000001',
        },
        { username: 'admin', password: 'admin', clientId: 'demo', clientSecret: 'demo123' },
      ],
      'admin-login',
    );
    await shot(page, '04-admin-dashboard');

    await checkOnlineConsistency(page);
    await openPage(page, '/system/online-users', 'online-users-page', {
      selector: 'table',
      expectPagination: true,
      screenshot: '05-online-users',
    });
    await checkExtendFieldGroup(page);
    await checkOrgManagement(page);
    await openPage(page, '/system/menus', 'menu-management-page', {
      selector: 'table',
      expectPagination: true,
      screenshot: '09-menu-management',
    });
    await openPage(page, '/gateway/routes', 'gateway-routes-page', {
      selector: 'table',
      expectPagination: true,
      screenshot: '10-gateway-routes',
    });
    await openPage(page, '/gateway/rate-limit', 'gateway-rate-limit-page', {
      selector: 'main',
      screenshot: '11-gateway-rate-limit',
    });
    await openPage(page, '/gateway/ip-limit', 'gateway-ip-limit-page', {
      selector: 'main',
      screenshot: '12-gateway-ip-limit',
    });

    result.status = result.networkErrors.length ? 'failed' : 'passed';
    if (result.networkErrors.length) {
      result.error = `network errors: ${JSON.stringify(result.networkErrors.slice(0, 10))}`;
    }
  } catch (error) {
    result.status = 'failed';
    result.error = error && error.stack ? error.stack : String(error);
    failStep('fatal', error);
  } finally {
    result.finishedAt = new Date().toISOString();
    fs.writeFileSync(RESULT_FILE, JSON.stringify(result, null, 2), 'utf8');
    if (browser) await browser.close();
    console.log(RESULT_FILE);
    if (result.status !== 'passed') process.exitCode = 1;
  }
}

main();
