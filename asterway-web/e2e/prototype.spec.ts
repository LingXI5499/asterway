import { test, expect, type Page } from '@playwright/test';
import { resolve } from 'node:path';
import { mkdir } from 'node:fs/promises';

async function visit(page: Page, route: string) {
  await page.goto(`/#/${route}`);
  await expect(page.locator('h1')).toBeVisible();
}
async function read(page: Page, key = 'asterway:prototype:v2') {
  return page.evaluate((k) => JSON.parse(localStorage.getItem(k)!), key);
}

test('模板计划 → 多任务 → 习惯，伙伴记忆 → 自动创建 → 可撤销', async ({ page }) => {
  await visit(page, 'templates');
  await page.getByLabel('搜索成长模板').fill('雅思');
  await expect(page.locator('.template-card')).toHaveCount(1);
  await page.getByRole('button', { name: '预览并使用' }).click();
  await expect(page.locator('.bundle-counts')).toContainText('6 任务');
  await page.getByRole('button', { name: '加入我的目标', exact: true }).click();
  expect((await read(page, 'asterway:data:v1')).goals).toHaveLength(4);
  await visit(page, 'assistant');
  await page.getByLabel('给成长伙伴的指令').fill('记住：我每天有 20 分钟学习');
  await page.getByRole('button', { name: '发送指令' }).click();
  await expect(page.locator('.memory-preview')).toContainText('20 分钟');
  await page.getByRole('button', { name: '确认批量创建' }).click();
  await page.getByLabel('允许发送后自动执行新增', { exact: false }).check();
  await page.getByLabel('给成长伙伴的指令').fill('帮我制定 30 天日语学习计划');
  await page.getByRole('button', { name: '发送指令' }).click();
  const data = await read(page, 'asterway:data:v1');
  expect(data.goals).toHaveLength(5);
  const goal = data.goals.at(-1);
  expect(
    data.tasks
      .filter((t: { goalId: string; durationMinutes: number }) => t.goalId === goal.id)
      .every((t: { durationMinutes: number }) => t.durationMinutes <= 20),
  ).toBe(true);
  await page.reload();
  await expect(page.locator('.messages')).toContainText('已完成创建');
  await page.getByRole('button', { name: '撤销最近一次创建' }).click();
  expect((await read(page, 'asterway:data:v1')).goals).toHaveLength(4);
  await visit(page, 'knowledge');
  await expect(page.locator('.knowledge-grid')).toContainText('我每天有 20 分钟学习');
});

test('瀑布流互动、评论、知识收藏、真实播放与视频切换', async ({ page }) => {
  await visit(page, 'community');
  expect(
    await page.locator('.community-feed').evaluate((el) => getComputedStyle(el).columnCount),
  ).toBe('3');
  await page.locator('.post-card').first().getByRole('button', { name: /^点赞/ }).click();
  await page.locator('.post-card').first().getByRole('button', { name: /^收藏/ }).click();
  await page.locator('.post-open').first().click();
  await page.getByLabel('评论内容').fill('这个计划让我理解了目标和每日行动的关系');
  await page.getByRole('button', { name: '发布评论' }).click();
  await expect(page.getByRole('dialog')).toContainText('这个计划让我理解');
  await page.getByRole('button', { name: '收入知识库' }).click();
  await page.keyboard.press('Escape');
  await page.getByRole('button', { name: '视频', exact: true }).click();
  await expect(page.locator('.post-card')).toHaveCount(3);
  await page.locator('.post-open').first().click();
  const video = page.locator('video');
  await expect
    .poll(() => video.evaluate((el: HTMLVideoElement) => el.readyState))
    .toBeGreaterThanOrEqual(2);
  await video.evaluate((el: HTMLVideoElement) => el.play());
  await expect
    .poll(() => video.evaluate((el: HTMLVideoElement) => el.currentTime))
    .toBeGreaterThan(0.2);
  const source = await video.getAttribute('src');
  await page.getByRole('button', { name: '下一个', exact: true }).click();
  await expect(video).not.toHaveAttribute('src', source!);
  await page.keyboard.press('Escape');
  await page.reload();
  const post = (await read(page)).posts[0];
  expect(post.liked).toBe(true);
  expect(post.saved).toBe(true);
  expect(post.comments.at(-1).text).toContain('目标和每日行动');
});

test('分享成长、关注、社群加入和本地群聊', async ({ page }) => {
  await visit(page, 'community');
  await page.getByRole('button', { name: '分享成长', exact: true }).click();
  await page.getByLabel('内容标题').fill('我的第一个成长瞬间');
  await page.getByLabel('正文', { exact: true }).fill('今天认真阅读了两页，想把这个习惯持续下去。');
  await page.getByRole('button', { name: '发布到演示社区' }).click();
  await expect(page.getByRole('dialog')).toContainText('今天认真阅读了两页');
  await page.keyboard.press('Escape');
  await page.locator('.follow-button').first().click();
  await page.getByRole('button', { name: '关注', exact: true }).click();
  expect((await read(page)).followedIds).toHaveLength(1);
  await visit(page, 'circles');
  await page.locator('.circle-card').first().getByRole('button', { name: '加入并交流' }).click();
  await expect(page.locator('.message-thread')).toBeVisible();
  await page.getByLabel('消息内容').fill('今天我会完成一个最小行动');
  await page.getByRole('button', { name: '模拟发送' }).click();
  await expect(page.locator('.message-scroll')).toContainText('今天我会完成一个最小行动');
  await expect(page.locator('.message-scroll')).toContainText('【模拟回复】');
  await page.reload();
  await expect(page.locator('.message-scroll')).toContainText('今天我会完成一个最小行动');
});

test('真实任务打卡达成每日赛，领取积分且刷新不重复', async ({ page }) => {
  await visit(page, 'challenges');
  const daily = page.locator('.challenge-card').filter({ hasText: '今日行动赛' });
  await daily.getByRole('button', { name: '报名挑战' }).click();
  await expect(daily.getByRole('button', { name: '领取奖励' })).toBeDisabled();
  await visit(page, 'habits');
  const today = await page.evaluate(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  });
  await page.getByRole('button', { name: `打卡 每天阅读 20 分钟 ${today}`, exact: true }).click();
  await visit(page, 'challenges');
  await daily.getByRole('button', { name: '领取奖励' }).click();
  await page.reload();
  await expect(daily.getByRole('button', { name: '奖励已领取' })).toBeDisabled();
  expect((await read(page)).rewards).toHaveLength(1);
});

for (const width of [1440, 1280, 390]) {
  test(`新增模块布局、导航与截图 ${width}`, async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (error) => errors.push(error.message));
    await page.setViewportSize({ width, height: width === 390 ? 844 : 900 });
    const folder = resolve('../docs/screenshots');
    await mkdir(folder, { recursive: true });
    for (const route of [
      'templates',
      'community',
      'circles',
      'messages',
      'knowledge',
      'challenges',
      'achievements',
      'membership',
      'profile/me',
    ]) {
      await visit(page, route);
      expect(
        await page.evaluate(() => document.documentElement.scrollWidth),
        `${route} 横向溢出`,
      ).toBeLessThanOrEqual(width);
      if (['community', 'messages', 'challenges', 'templates'].includes(route))
        await page.screenshot({
          path: resolve(folder, `${route}-v2-${width}.png`),
          fullPage: true,
        });
    }
    await visit(page, 'community');
    expect(
      await page.locator('.community-feed').evaluate((el) => getComputedStyle(el).columnCount),
    ).toBe(width === 1440 ? '3' : '2');
    if (width === 390) await page.getByRole('button', { name: '打开导航' }).click();
    const links = page.locator('.sidebar .nav-group a');
    const boxes = await links.evaluateAll((elements) =>
      elements.map((el) => el.getBoundingClientRect().top),
    );
    for (let i = 1; i < boxes.length; i++) expect(boxes[i]! - boxes[i - 1]!).toBeGreaterThan(25);
    expect(errors).toEqual([]);
  });
}
