import { chromium } from '@playwright/test'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

// Original canvas animations for the local prototype; no external media is used.
const folder = resolve('public/media')
await mkdir(folder, { recursive: true })
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const page = await browser.newPage()
for (const [name, title, subtitle, palette] of [
  ['focus-demo', '把注意力带回此刻', '吸气 · 停留 · 呼气', ['#173c35', '#dce9cd', '#9dbd8e']],
  ['reading-demo', '给自己一页的时间', '读一点 · 想一点 · 记一点', ['#374654', '#dfebf1', '#90b3c4']],
  ['action-demo', '今天也向前一小步', '从一个小行动开始', ['#524638', '#f2e7d3', '#c0a777']],
]) {
  const result = await page.evaluate(async ({ title, subtitle, palette }) => {
    const canvas = document.createElement('canvas')
    canvas.width = 540; canvas.height = 800
    const context = canvas.getContext('2d')
    const draw = (progress) => {
      context.fillStyle = palette[0]; context.fillRect(0, 0, 540, 800)
      context.textAlign = 'center'
      context.fillStyle = palette[2]; context.font = '13px sans-serif'
      context.fillText('ASTERWAY  /  MOMENTS OF GROWTH', 270, 73)
      const radius = 93 + 32 * Math.sin(progress * Math.PI * 2)
      for (let layer = 4; layer > 0; layer--) {
        context.strokeStyle = `${palette[2]}${['00','65','40','25','12'][layer]}`
        context.lineWidth = 1.5; context.beginPath(); context.arc(270, 315, radius + layer * 29, 0, Math.PI * 2); context.stroke()
      }
      const gradient = context.createRadialGradient(245, 282, 0, 270, 315, radius)
      gradient.addColorStop(0, palette[1]); gradient.addColorStop(1, palette[2])
      context.fillStyle = gradient; context.beginPath(); context.arc(270, 315, radius, 0, Math.PI * 2); context.fill()
      context.fillStyle = palette[1]; context.font = 'bold 30px "Microsoft YaHei", sans-serif'; context.fillText(title, 270, 553)
      context.fillStyle = palette[2]; context.font = '18px "Microsoft YaHei", sans-serif'; context.fillText(subtitle, 270, 606)
      context.font = '12px sans-serif'; context.fillText('原创动画 · 前端原型演示', 270, 750)
      context.fillStyle = `${palette[1]}50`; context.fillRect(150, 664, 240, 2)
      context.fillStyle = palette[1]; context.fillRect(150, 664, 240 * progress, 2)
    }
    draw(.25)
    const poster = canvas.toDataURL('image/png').split(',')[1]
    const stream = canvas.captureStream(20)
    const recorder = new MediaRecorder(stream, { mimeType: 'video/webm;codecs=vp8', videoBitsPerSecond: 280000 })
    const chunks = []
    recorder.ondataavailable = (event) => chunks.push(event.data)
    const recorded = new Promise((resolve) => { recorder.onstop = async () => {
      const reader = new FileReader(); reader.onload = () => resolve(String(reader.result).split(',')[1]); reader.readAsDataURL(new Blob(chunks, { type: 'video/webm' }))
    } })
    const started = performance.now()
    recorder.start()
    const loop = () => { const elapsed = performance.now() - started; draw(Math.min(elapsed / 6000, 1)); if (elapsed < 6000) requestAnimationFrame(loop); else { recorder.stop(); stream.getTracks().forEach(track => track.stop()) } }
    loop()
    return { video: await recorded, poster }
  }, { title, subtitle, palette })
  await writeFile(resolve(folder, `${name}.webm`), Buffer.from(result.video, 'base64'))
  await writeFile(resolve(folder, `${name}.png`), Buffer.from(result.poster, 'base64'))
  console.log(`Created ${name}.webm and poster`)
}
await browser.close()
