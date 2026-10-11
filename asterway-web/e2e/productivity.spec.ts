import { test, expect, type Page } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';

const key = 'asterway:data:v1';
async function read(page: Page) {
  return page.evaluate((k) => JSON.parse(localStorage.getItem(k)!), key);
}
async function visit(page: Page, route: string) {
  await page.goto(`/#/${route}`);
  await expect(page.locator('h1')).toBeVisible();
}

test('四象限重新判断与任务编辑持久化，不改变安排日期', async ({ page }) => {
  await visit(page, 'methods');
  const before = await read(page),
    task = before.tasks.find((t: any) => t.status === 'todo');
  await page.getByLabel(`移动象限：${task.title}`, { exact: true }).selectOption('batch');
  await page.reload();
  let after = (await read(page)).tasks.find((t: any) => t.id === task.id);
  expect(after.important).toBe(false);
  expect(after.urgent).toBe(true);
  expect(after.scheduledDate).toBe(task.scheduledDate);
  await page.getByRole('button', { name: task.title, exact: true }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.locator('.task-planning-details summary').click();
  await page.getByLabel('是否重要').selectOption('yes');
  await page.getByLabel('是否紧急').selectOption('no');
  await page.getByLabel('任务截止日期').fill('2026-12-01');
  await page.getByLabel('需要的精力').selectOption('low');
  await page.getByRole('button', { name: '保存修改' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  after = (await read(page)).tasks.find((t: any) => t.id === task.id);
  expect(after).toMatchObject({
    important: true,
    urgent: false,
    energy: 'low',
    dueDate: '2026-12-01',
  });
});

test('每日三件事先预览后确认，时段不重叠，刷新后可撤销', async ({ page }) => {
  await visit(page, 'methods?tab=top3');
  const before = await read(page);
  await page.getByLabel('留白比例').selectOption('0');
  await page.getByRole('button', { name: '手动选择', exact: true }).click();
  const choices = page.locator('.selection-check');
  for (let i = 0; i < 3; i++) await choices.nth(i).click();
  await page.getByRole('button', { name: '生成可执行草案' }).click();
  await expect(page.getByRole('dialog')).toContainText('确认你的行动计划');
  expect((await read(page)).tasks).toEqual(before.tasks);
  await expect(page.locator('.plan-timeline article')).toHaveCount(3);
  await page.getByRole('button', { name: '确认排入日程' }).click();
  await expect(page).toHaveURL(/planner/);
  const after = await read(page);
  const focused = after.tasks.filter((t: any) => t.isTodayFocus && t.status === 'todo');
  expect(focused).toHaveLength(3);
  const times = focused
    .map((t: any) => {
      const [h, m] = t.startTime.split(':').map(Number);
      return [h * 60 + m, h * 60 + m + t.durationMinutes];
    })
    .sort((a: number[], b: number[]) => a[0] - b[0]);
  expect(times[1][0]).toBeGreaterThanOrEqual(times[0][1]);
  expect(times[2][0]).toBeGreaterThanOrEqual(times[1][1]);
  await visit(page, 'methods?tab=top3');
  await page.getByRole('button', { name: '撤销最近一次安排' }).click();
  expect((await read(page)).tasks.map(({ updatedAt, ...t }: any) => t)).toEqual(
    before.tasks.map(({ updatedAt, ...t }: any) => t),
  );
  await page.getByRole('button', { name: '手动选择', exact: true }).click();
  await page.locator('.selection-check').nth(0).click();
  await page.locator('.selection-check').nth(1).click();
  await page.getByRole('button', { name: '生成可执行草案' }).click();
  await page.getByRole('button', { name: '确认排入日程' }).click();
  await visit(page, 'today');
  await expect(page.locator('.focus-card')).toHaveCount(2);
});

test('专注跨页面与刷新恢复，实际分钟只记录一次，休息不记为专注', async ({ page }) => {
  await page.clock.install();
  await visit(page, 'focus');
  const before = await read(page),
    task = before.tasks.find((t: any) => t.status === 'todo');
  await page.getByLabel('选择专注任务').selectOption(task.id);
  await page.getByRole('button', { name: '开始专注', exact: true }).click();
  await page.clock.fastForward(65000);
  await expect(page.locator('.focus-countdown')).toHaveText('23:55');
  await page.getByRole('link', { name: '效率计划', exact: true }).click();
  await expect(page.locator('.active-focus-pill')).toBeVisible();
  await page.reload();
  await page.locator('.active-focus-pill').click();
  await expect(page.getByRole('button', { name: '暂停', exact: true })).toBeVisible();
  await page.getByRole('button', { name: /记录已完成的 1 分钟/ }).click();
  let data = await read(page);
  expect(data.focusSessions).toHaveLength(before.focusSessions.length + 1);
  expect(data.focusSessions.at(-1)).toMatchObject({ minutes: 1, taskId: task.id });
  expect(data.tasks.find((t: any) => t.id === task.id).status).toBe('todo');
  await page.reload();
  await expect(page.getByRole('button', { name: /已记录这段专注/ })).toBeDisabled();
  await page.getByRole('button', { name: '给自己 5 分钟休息' }).click();
  await page.getByRole('button', { name: '开始休息', exact: true }).click();
  await page.clock.fastForward(65000);
  await expect(page.locator('.focus-record-button')).toHaveCount(0);
  data = await read(page);
  expect(data.focusSessions).toHaveLength(before.focusSessions.length + 1);
});

test('全局搜索可直达计划方法与具体成长模板', async ({ page }) => {
  await visit(page, 'knowledge');
  await page.keyboard.press('Control+k');
  await page.getByLabel('搜索关键词').fill('四象限');
  await page.locator('.search-results button').click();
  await expect(page).toHaveURL(/methods/);
  await page.keyboard.press('Control+k');
  await page.getByLabel('搜索关键词').fill('雅思');
  await page.locator('.search-results button').click();
  await expect(page).toHaveURL(/templates\?search=/);
  await expect(page.locator('.template-card')).toHaveCount(1);
  await visit(page, 'knowledge');
  const note = await page.evaluate(
    () => JSON.parse(localStorage.getItem('asterway:prototype:v2')!).notes[0],
  );
  await page.keyboard.press('Control+k');
  await page.getByLabel('搜索关键词').fill(note.title);
  await page.locator('.search-results button').filter({ hasText: '知识' }).click();
  await expect(page).toHaveURL(/knowledge\?note=/);
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.getByLabel('记录标题', { exact: true })).toHaveValue(note.title);
});

test('伙伴按轻重缓急排程，即使开启新增自动执行也需确认，支持撤销', async ({ page }) => {
  await visit(page, 'assistant');
  const before = await read(page);
  await page.getByLabel('允许发送后自动执行新增', { exact: false }).check();
  await page.getByLabel('给成长伙伴的指令').fill('按四象限帮我安排今天 120 分钟的三件事');
  await page.getByRole('button', { name: '发送指令' }).click();
  await expect(page.getByRole('button', { name: '确认应用', exact: true })).toBeVisible();
  expect((await read(page)).tasks).toEqual(before.tasks);
  await page.getByRole('button', { name: '确认应用', exact: true }).click();
  expect((await read(page)).tasks.some((t: any) => t.startTime)).toBe(true);
  await page.getByRole('button', { name: '撤销最近一次方案', exact: true }).click();
  expect((await read(page)).tasks.map(({ updatedAt, ...t }: any) => t)).toEqual(
    before.tasks.map(({ updatedAt, ...t }: any) => t),
  );
});

for (const width of [1440, 1280, 390]) {
  test(`效率工作区 ${width}px 布局、入口与截图`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    const errors: string[] = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await mkdir(resolve('../docs/screenshots'), { recursive: true });
    for (const route of [
      'methods',
      'methods?tab=top3',
      'methods?tab=blocks',
      'methods?tab=library',
      'focus',
    ]) {
      await visit(page, route);
      await expect
        .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1))
        .toBe(true);
      if (route === 'methods') {
        expect(
          await page.locator('.matrix-board').evaluate((el) => getComputedStyle(el).display),
        ).toBe('grid');
        await expect(page.locator('.quadrant')).toHaveCount(4);
      }
      if (width !== 1280) {
        const name = route.replace('?tab=', '-');
        await page.screenshot({
          path: resolve(`../docs/screenshots/${name}-v3-${width}.png`),
          fullPage: true,
        });
      }
    }
    expect(errors).toEqual([]);
  });
}
