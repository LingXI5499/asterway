import { test, expect, type Page } from '@playwright/test';
import { resolve } from 'node:path';
import { mkdir } from 'node:fs/promises';
async function visit(page: Page, route: string) {
  await page.goto(`/#/${route}`);
  await expect(page.locator('h1')).toBeVisible();
}
async function read(page: Page, key = 'asterway:data:v1') {
  return page.evaluate((k) => JSON.parse(localStorage.getItem(k)!), key);
}
for (const [method, label] of [
  ['matrix', '四象限'],
  ['top3', '每日三件事'],
  ['blocks', '时间分块'],
  ['focus', '番茄专注'],
  ['frog', '先做关键难事'],
  ['gtd', '收集与下一步'],
]) {
  test(`${label}新建流程产生对应方法的目标和行动`, async ({ page }) => {
    await visit(page, 'methods');
    const before = await read(page);
    await page.getByRole('button', { name: '新建方法计划', exact: true }).click();
    await page
      .locator('.builder-methods button')
      .filter({ has: page.getByText(label!, { exact: true }) })
      .click();
    await page.getByRole('button', { name: `使用${label}继续` }).click();
    await page.getByLabel('计划名称', { exact: true }).fill(`测试${label}成长`);
    await page.getByLabel('行动1', { exact: true }).fill('完成第一项成长行动');
    await page.getByRole('button', { name: /添加行动/ }).click();
    await page.getByLabel('行动2', { exact: true }).fill('完成第二项成长行动');
    if (method === 'matrix') await page.getByLabel('所属象限').first().selectOption('batch');
    if (method === 'focus') await page.getByLabel('每段专注时长').selectOption('45');
    await page.getByRole('button', { name: `预览${label}计划` }).click();
    await expect(page.locator('.builder-preview-times')).toBeVisible();
    expect((await read(page)).goals).toHaveLength(before.goals.length);
    await page.getByRole('button', { name: '确认建立方法计划' }).click();
    const data = await read(page),
      goal = data.goals.at(-1),
      tasks = data.tasks.filter((t: any) => t.goalId === goal.id);
    expect(goal.planningMethod).toBe(method);
    expect(tasks).toHaveLength(2);
    expect(tasks.every((t: any) => t.planningMethod === method)).toBe(true);
    if (method === 'matrix') expect(tasks[0]).toMatchObject({ important: false, urgent: true });
    if (method === 'gtd') expect(tasks.every((t: any) => !t.scheduledDate)).toBe(true);
    if (method === 'blocks') expect(tasks.every((t: any) => t.startTime)).toBe(true);
    if (method === 'top3') expect(tasks.every((t: any) => t.isTodayFocus)).toBe(true);
    if (method === 'frog') expect(tasks[0]).toMatchObject({ energy: 'high', isTodayFocus: true });
    if (method === 'focus') {
      await expect(page.locator('.focus-countdown')).toHaveText('45:00');
      await expect(page.getByLabel('选择专注任务')).toHaveValue(tasks[0].id);
    }
    await page.reload();
    expect((await read(page)).goals.at(-1).planningMethod).toBe(method);
  });
}
test('任意页面的全局伙伴创建、知识记忆、草案和会话共享', async ({ page }) => {
  await visit(page, 'knowledge');
  const before = await read(page);
  await page.getByRole('button', { name: '打开全局成长伙伴' }).click();
  await page.getByRole('button', { name: '帮我理解当前页面' }).click();
  await expect(page.locator('.partner-history')).toContainText('你正在「个人知识库」');
  await page.getByRole('button', { name: '养成习惯', exact: true }).click();
  expect((await read(page)).habits).toHaveLength(before.habits.length);
  await page.getByRole('button', { name: '确认批量创建', exact: true }).click();
  expect((await read(page)).habits).toHaveLength(before.habits.length + 1);
  await page.getByLabel('全局伙伴指令').fill('记住：我每天有 30 分钟学习');
  await page.getByRole('button', { name: '发送全局指令' }).click();
  await page.getByRole('button', { name: '确认批量创建' }).click();
  await page.getByRole('button', { name: '回顾记忆', exact: true }).click();
  await expect(page.locator('.partner-history')).toContainText('30 分钟学习');
  await page.getByLabel('全局伙伴指令').fill('按四象限帮我安排今天 120 分钟的三件事');
  await page.getByRole('button', { name: '发送全局指令' }).click();
  await page.getByRole('link', { name: '完整工作台' }).click();
  await expect(page.getByRole('button', { name: '确认应用', exact: true })).toBeVisible();
  await expect(page.locator('.messages')).toContainText('30 分钟学习');
  await page.getByRole('button', { name: '取消方案' }).click();
  await visit(page, 'community');
  await page.keyboard.press('Control+j');
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.locator('.partner-context')).toContainText('成长社区');
});
test('伙伴每日提醒需确认，刷新保存，仅到点显示一次，暂停可保存', async ({ page }) => {
  await page.clock.install({ time: new Date('2026-10-12T12:29:40Z') });
  await visit(page, 'knowledge');
  await page.getByRole('button', { name: '打开全局成长伙伴' }).click();
  await page.getByLabel('全局伙伴指令').fill('每天 20:30 提醒我复盘');
  await page.getByRole('button', { name: '发送全局指令' }).click();
  expect((await read(page, 'asterway:prototype:v2')).reminders || []).toHaveLength(0);
  await page.getByRole('button', { name: '确认添加提醒' }).click();
  await page.reload();
  await expect(page.getByRole('button', { name: '打开全局成长伙伴' })).toBeVisible();
  await page.clock.fastForward(30000);
  await expect(page.locator('.toast')).toContainText('成长提醒：复盘');
  const state = await read(page, 'asterway:prototype:v2');
  expect(state.reminders[0].lastFiredDate).toBe('2026-10-12');
  await page.getByRole('button', { name: '打开全局成长伙伴' }).click();
  await page.locator('.partner-reminders summary').click();
  await page.getByLabel(/20:30 · 复盘/).uncheck();
  await page.reload();
  expect((await read(page, 'asterway:prototype:v2')).reminders[0].enabled).toBe(false);
});
test('图文多图轮播、左右详情布局、独立视频频道与键盘切换', async ({ page }) => {
  await visit(page, 'community');
  await page.getByRole('button', { name: '图文', exact: true }).click();
  await expect
    .poll(() =>
      page
        .locator('.post-cover img')
        .evaluateAll((elements) =>
          elements.every(
            (el) => (el as HTMLImageElement).complete && (el as HTMLImageElement).naturalWidth > 0,
          ),
        ),
    )
    .toBe(true);
  expect(
    (await page.locator('.post-type').allTextContents()).every((t) => t.includes('图文笔记')),
  ).toBe(true);
  await page.getByRole('button', { name: '分享成长', exact: true }).click();
  await page.getByLabel('内容标题').fill('今天的阅读和笔记');
  await page.getByLabel('正文', { exact: true }).fill('认真记录两张图片，回顾今天的一个小行动。');
  await page.locator('input[type=file]').setInputFiles([
    {
      name: 'one.svg',
      mimeType: 'image/svg+xml',
      buffer: Buffer.from(
        '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="500"><rect width="400" height="500" fill="#b8cfa2"/></svg>',
      ),
    },
    {
      name: 'two.svg',
      mimeType: 'image/svg+xml',
      buffer: Buffer.from(
        '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="500"><rect width="400" height="500" fill="#dbc5b4"/></svg>',
      ),
    },
  ]);
  await expect(page.locator('.publish-image-preview img')).toHaveCount(2);
  await page.getByRole('button', { name: '发布到演示社区' }).click();
  expect(
    await page.locator('.note-detail-layout').evaluate((el) => getComputedStyle(el).display),
  ).toBe('grid');
  const first = await page.locator('.note-carousel img').getAttribute('src');
  await page.getByRole('button', { name: '下一张图片' }).click();
  expect(await page.locator('.note-carousel img').getAttribute('src')).not.toBe(first);
  await page.keyboard.press('Escape');
  await page.getByRole('button', { name: '视频', exact: true }).click();
  await expect(page.locator('.video-room-banner')).toBeVisible();
  await expect(page.locator('.post-card')).toHaveCount(3);
  await page.locator('.post-open').first().click();
  await expect
    .poll(() => page.locator('video').evaluate((el: HTMLVideoElement) => el.currentTime))
    .toBeGreaterThan(0.2);
  const source = await page.locator('video').getAttribute('src');
  await page.locator('.video-modal h2').first().click();
  await page.keyboard.press('ArrowDown');
  expect(await page.locator('video').getAttribute('src')).not.toBe(source);
});
for (const width of [1440, 1280, 390])
  test(`全局伙伴、方法新建和社区详情 ${width}px 适配`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    await mkdir(resolve('../docs/screenshots'), { recursive: true });
    async function capture(name: string) {
      await expect
        .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1))
        .toBe(true);
      const dialog = page.getByRole('dialog');
      const modal = await dialog.count();
      if (modal)
        expect(await dialog.evaluate((el) => el.scrollWidth <= el.clientWidth + 1)).toBe(true);
      if (width !== 1280)
        await page.screenshot({
          path: resolve(`../docs/screenshots/${name}-v4-${width}.png`),
          fullPage: !modal,
        });
    }
    await visit(page, 'methods');
    await page.getByRole('button', { name: '新建方法计划' }).click();
    await capture('method-builder');
    await page.keyboard.press('Escape');
    await visit(page, 'community');
    await capture('community');
    await page.getByRole('button', { name: '图文', exact: true }).click();
    await page.locator('.post-open').first().click();
    await capture('note-detail');
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: '视频', exact: true }).click();
    await page.locator('.post-open').first().click();
    await capture('video-detail');
    if (width === 390) {
      const source = await page.locator('video').getAttribute('src');
      await page.locator('.note-media-stage').evaluate((el) => {
        const first = new Touch({ identifier: 1, target: el, clientX: 160, clientY: 450 });
        el.dispatchEvent(new TouchEvent('touchstart', { bubbles: true, touches: [first] }));
        const last = new Touch({ identifier: 1, target: el, clientX: 161, clientY: 240 });
        el.dispatchEvent(new TouchEvent('touchend', { bubbles: true, changedTouches: [last] }));
      });
      expect(await page.locator('video').getAttribute('src')).not.toBe(source);
    }
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: '打开全局成长伙伴' }).click();
    await capture('global-partner');
    expect(errors).toEqual([]);
  });
