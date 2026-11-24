// Parcours navigateur de bout en bout (Playwright) sur l'application servie par le jar.
// Utilisé par la CI (job e2e) : node e2e/ui_smoke.cjs (EZ_BASE_URL, défaut http://localhost:8080).
const { chromium } = require('playwright');

const BASE = process.env.EZ_BASE_URL || 'http://localhost:8080';
const problems = [];

async function login(browser, email) {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  page.on('pageerror', (e) => problems.push(`${email} : ${e.message}`));
  page.on('console', (m) => {
    const t = m.text();
    if (m.type() === 'error' && !/40[13]|429/.test(t)) problems.push(`${email} : ${t}`);
  });
  await page.goto(`${BASE}/login`);
  await page.fill('input[type=email]', email);
  await page.fill('input[type=password]', 'demo1234');
  await page.click('button[type=submit]');
  await page.waitForURL((u) => !u.pathname.startsWith('/login'));
  return page;
}

async function step(name, fn) {
  try {
    await fn();
    console.log(`OK   ${name}`);
  } catch (e) {
    problems.push(`${name} : ${e.message.split('\n')[0]}`);
    console.log(`FAIL ${name}`);
  }
}

(async () => {
  const browser = await chromium.launch();
  const coo = await login(browser, 'camille@ezproject.local');
  await step('portefeuille', async () => {
    await coo.goto(`${BASE}/portfolio`);
    await coo.waitForSelector('text=Risques critiques');
    await coo.waitForSelector('table');
  });
  await step('roadmap', async () => {
    await coo.goto(`${BASE}/roadmap`);
    await coo.waitForSelector('main a:has-text("Portail client")');
  });
  await step('capacité', async () => {
    await coo.goto(`${BASE}/capacity`);
    await coo.waitForSelector('text=Hugo Bernard');
  });
  const projectId = await coo.evaluate(async () => (await (await fetch('/api/projects')).json()).find((p) => p.code === 'PORTAIL').id);
  for (const [tab, marker] of [['', 'Météo du projet'], ['/board', 'Intégration prestataire'], ['/time', 'Jours passés par personne'],
    ['/risks', 'Matrice des risques'], ['/access', 'Visibilité']]) {
    await step(`projet ${tab || 'vue d’ensemble'} (rechargement direct)`, async () => {
      await coo.goto(`${BASE}/projects/${projectId}${tab}`);
      await coo.reload();
      await coo.waitForSelector(`text=${marker}`);
    });
  }
  await step('rapport COPIL', async () => {
    await coo.goto(`${BASE}/projects/${projectId}/copil`);
    await coo.waitForSelector('text=Comité de pilotage');
  });
  await step('organisation : journal d’audit', async () => {
    await coo.goto(`${BASE}/organization/audit`);
    await coo.waitForSelector('text=Journal d’audit');
  });
  await step('mon compte', async () => {
    await coo.goto(`${BASE}/account`);
    await coo.waitForSelector('text=Double authentification');
  });
  const guest = await login(browser, 'julie@ezproject.local');
  await step('invitée : lecture seule', async () => {
    await guest.goto(`${BASE}/projects/${projectId}/board`);
    await guest.waitForSelector('text=Lecture seule');
    if (await guest.locator('button:has-text("Nouvelle tâche")').count()) throw new Error('bouton de création visible');
  });
  await browser.close();
  console.log(problems.length ? `\n${problems.length} problème(s) :\n- ${problems.join('\n- ')}` : '\nTOUT EST VERT');
  process.exit(problems.length ? 1 : 0);
})();
