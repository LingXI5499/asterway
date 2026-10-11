import { test, expect } from '@playwright/test'
import { readFile, mkdir } from 'node:fs/promises'
import { resolve } from 'node:path'
const key = 'asterway:data:v1'
async function state(page: import('@playwright/test').Page) {
  return page.evaluate((k) => JSON.parse(localStorage.getItem(k)!), key)
}
async function visit(page: import('@playwright/test').Page, path: string) {
  await page.goto(`/#/${path}`)
  await expect(page.locator('main')).toBeVisible()
}
test('目标 → 行动 → 习惯 → 刷新保存的产品闭环', async ({ page }) => {
  await visit(page, 'goals')
  await page.getByRole('button', { name: '新建目标', exact: true }).click()
  await page.getByLabel('目标名称').fill('合作伙伴 MVP 演示')
  await page.getByRole('button', { name: '创建', exact: true }).click()
  await expect(page.getByRole('dialog')).toContainText('待规划')
  await page.getByRole('button', { name: '+ 新建', exact: true }).click()
  await page.getByLabel('任务名称').fill('整理产品演示资料')
  await page.getByRole('button', { name: '创建', exact: true }).click()
  await page.getByRole('link', { name: '今日总览', exact: true }).click()
  await expect(
    page.getByRole('button', { name: '完成：整理产品演示资料', exact: true }),
  ).toBeVisible()
  const before = await state(page),
    initialXp = before.xpEvents.reduce((n: number, e: { amount: number }) => n + e.amount, 0)
  await page.getByRole('button', { name: '完成：整理产品演示资料', exact: true }).click()
  await page.reload()
  await expect(
    page.getByRole('button', { name: '取消完成：整理产品演示资料', exact: true }),
  ).toBeVisible()
  const after = await state(page)
  expect(after.xpEvents.reduce((n: number, e: { amount: number }) => n + e.amount, 0)).toBe(
    initialXp + 20,
  )
  await page.getByRole('link', { name: '习惯追踪', exact: true }).click()
  const today = await page.evaluate(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  })
  await page.getByRole('button', { name: `打卡 每天阅读 20 分钟 ${today}`, exact: true }).click()
  await page.reload()
  await expect(
    page.getByRole('button', { name: `取消 每天阅读 20 分钟 ${today}`, exact: true }),
  ).toBeVisible()
  await page.getByRole('link', { name: '我的目标', exact: true }).click()
  await page.locator('.goal-card').filter({ hasText: '合作伙伴 MVP 演示' }).click()
  await expect(page.getByRole('dialog')).toContainText('100%')
})
test('伙伴草案取消、确认、撤销和刷新保留', async ({ page }) => {
  await visit(page, 'assistant')
  const original = (await state(page)).tasks
  await page.getByRole('button', { name: '帮我调整', exact: true }).click()
  await expect(page.locator('.proposal-change')).toHaveCount(3)
  expect((await state(page)).tasks).toEqual(original)
  await page.getByRole('button', { name: '取消方案' }).click()
  expect((await state(page)).tasks).toEqual(original)
  await page.getByRole('button', { name: '帮我调整', exact: true }).click()
  await page.getByRole('button', { name: '确认应用' }).click()
  expect(
    (await state(page)).tasks.find((t: { id: string }) => t.id === 'task-1').scheduledDate,
  ).not.toBe(original[1].scheduledDate)
  await page.reload()
  await page.getByRole('button', { name: '撤销最近一次方案' }).click()
  expect((await state(page)).tasks).toEqual(original)
  await page.getByLabel('给成长伙伴的指令').fill('你觉得未来会怎样？')
  await page.getByRole('button', { name: '发送指令' }).click()
  await expect(page.locator('.messages')).toContainText('暂不支持开放问答')
})
test('数据导出、无效导入、有效覆盖、清空与重置', async ({ page }, testInfo) => {
  await visit(page, 'settings')
  const original = await state(page)
  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出', exact: true }).click()
  const download = await downloadPromise,
    file = testInfo.outputPath('asterway-backup.json')
  await download.saveAs(file)
  const exported = JSON.parse(await readFile(file, 'utf8'))
  expect(exported.tasks).toEqual(original.tasks)
  await page.locator('input[type=file]').setInputFiles({
    name: 'invalid.json',
    mimeType: 'application/json',
    buffer: Buffer.from('{"schemaVersion":3}'),
  })
  await expect(page.locator('.toast.error')).toContainText('数据格式不正确')
  expect((await state(page)).tasks).toEqual(original.tasks)
  await page.getByRole('button', { name: '清空', exact: true }).click()
  await page.getByRole('button', { name: '确认操作', exact: true }).click()
  await page.reload()
  expect((await state(page)).tasks).toHaveLength(0)
  await page.locator('input[type=file]').setInputFiles(file)
  await expect(page.getByRole('dialog')).toContainText('文件校验通过')
  await page.getByRole('button', { name: '确认覆盖', exact: true }).click()
  expect((await state(page)).tasks).toEqual(original.tasks)
  await page.getByRole('button', { name: '重置', exact: true }).click()
  await page.getByRole('button', { name: '确认操作', exact: true }).click()
  expect((await state(page)).tasks).toHaveLength(13)
})
test('周切换、复盘保存、无效路由以及页面无 JS 错误', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  await visit(page, 'planner')
  const current = await page.locator('.week-control strong').innerText()
  await page.getByRole('button', { name: '上一周', exact: true }).click()
  await expect(page.locator('.week-control strong')).not.toHaveText(current)
  await page.getByRole('button', { name: '本周', exact: true }).click()
  await expect(page.locator('.week-control strong')).toHaveText(current)
  await visit(page, 'insights')
  await page.getByLabel('这周做得不错的事').fill('让合作伙伴看到了产品的基本方向')
  await page.getByRole('button', { name: '保存复盘' }).click()
  await page.reload()
  await expect(page.getByLabel('这周做得不错的事')).toHaveValue('让合作伙伴看到了产品的基本方向')
  await page.getByRole('button', { name: '最近 30 天' }).click()
  await expect(page.locator('.bar-column')).toHaveCount(30)
  await visit(page, 'unknown')
  await expect(page.getByRole('link', { name: '返回今日' })).toBeVisible()
  expect(errors).toEqual([])
})
for (const size of [
  { width: 1440, height: 900 },
  { width: 1280, height: 800 },
  { width: 390, height: 844 },
]) {
  test(`布局与演示截图 ${size.width}×${size.height}`, async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.setViewportSize(size)
    await visit(page, 'today')
    await expect(page.getByRole('heading', { name: /好，星行者/ })).toBeVisible()
    await page.getByRole('button', { name: '开始体验' }).click()
    const folder = resolve('../docs/screenshots')
    await mkdir(folder, { recursive: true })
    await page.screenshot({
      path: resolve(folder, `today-${size.width}x${size.height}.png`),
      fullPage: true,
    })
    for (const path of [
      'today',
      'goals',
      'planner',
      'habits',
      'insights',
      'assistant',
      'settings',
    ]) {
      await visit(page, path)
      await expect(page.locator('h1')).toBeVisible()
      const dimensions = await page.evaluate(() => ({
        width: document.documentElement.scrollWidth,
        viewport: window.innerWidth,
      }))
      expect(dimensions.width, `${path} 水平溢出`).toBeLessThanOrEqual(dimensions.viewport)
    }
    await visit(page, 'today')
    if (size.width < 768) {
      await page.getByRole('button', { name: '打开导航' }).click()
      await page.getByRole('link', { name: '我的目标', exact: true }).click()
    }
    await page.getByRole('button', { name: '快速添加' }).click()
    await page.getByRole('button', { name: '新建任务', exact: true }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await page.getByLabel('任务名称').fill('小屏也能创建的行动')
    await page.getByRole('button', { name: '创建', exact: true }).click()
    expect(
      (await state(page)).tasks.some((t: { title: string }) => t.title === '小屏也能创建的行动'),
    ).toBe(true)
    expect(errors).toEqual([])
  })
}
