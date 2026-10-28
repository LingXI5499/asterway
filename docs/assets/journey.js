'use strict';

/* ===== 错误横幅：任何脚本错误都直接显示在页面上，方便排查 ===== */
function showFatal(msg) {
  var d = document.createElement('div');
  d.style.cssText = 'position:fixed;top:64px;left:50%;transform:translateX(-50%);z-index:9999;'
    + 'background:#3d1418;color:#ff8b8b;border:1px solid #ff5f6b;padding:10px 22px;'
    + 'border-radius:10px;font-size:13px;max-width:80vw;';
  d.textContent = '⚠ 脚本错误：' + msg;
  document.body.appendChild(d);
}

/* ===== SVG 创建辅助（createElementNS，兼容一切浏览器） ===== */
var NS = 'http://www.w3.org/2000/svg';
function el(tag, attrs, parent) {
  var n = document.createElementNS(NS, tag);
  if (attrs) { for (var k in attrs) n.setAttribute(k, attrs[k]); }
  if (parent) parent.appendChild(n);
  return n;
}
function txt(parent, x, y, str, attrs) {
  var a = { x: x, y: y };
  if (attrs) { for (var k in attrs) a[k] = attrs[k]; }
  var t = el('text', a, parent);
  t.textContent = str;
  return t;
}

/* ===== Hero 星空背景 ===== */
(function stars() {
  try {
    var cv = document.getElementById('stars');
    var ctx = cv.getContext('2d');
    var arr = [];
    function resize() {
      cv.width = window.innerWidth;
      var hero = document.querySelector('.hero');
      cv.height = hero ? hero.offsetHeight : 600;
      arr = [];
      for (var i = 0; i < 140; i++) {
        arr.push({
          x: Math.random() * cv.width,
          y: Math.random() * cv.height,
          r: Math.random() * 1.4 + 0.3,
          p: Math.random() * 6.28,
          s: Math.random() * 0.02 + 0.005
        });
      }
    }
    resize();
    window.addEventListener('resize', resize);
    (function draw(t) {
      ctx.clearRect(0, 0, cv.width, cv.height);
      for (var i = 0; i < arr.length; i++) {
        var s = arr[i];
        var a = 0.3 + Math.abs(Math.sin(s.p + t * s.s)) * 0.7;
        ctx.beginPath();
        ctx.arc(s.x, s.y, s.r, 0, 7);
        ctx.fillStyle = 'rgba(160,210,255,' + a + ')';
        ctx.fill();
      }
      requestAnimationFrame(draw);
    })(0);
  } catch (e) { showFatal(e.message); }
})();

/* ===== 舞台装饰星点 ===== */
(function deco() {
  try {
    var groups = document.querySelectorAll('#deco');
    for (var gi = 0; gi < groups.length; gi++) {
      var g = groups[gi];
      for (var i = 0; i < 46; i++) {
        el('circle', {
          cx: (Math.random() * 1000).toFixed(1),
          cy: (Math.random() * 560).toFixed(1),
          r: (Math.random() * 1.6 + 0.4).toFixed(2),
          fill: '#3a4a66'
        }, g);
      }
    }
  } catch (e) { showFatal(e.message); }
})();

/* ===== 站点数据 ===== */
var STATIONS = [
  { t: 0.06, x: 60,  y: 445, c: '#51e08a', no: 1, name: '前端',   en: 'RegisterView' },
  { t: 0.22, x: 250, y: 452, c: '#ff5f6b', no: 2, name: '安检门', en: 'Security Filter' },
  { t: 0.40, x: 430, y: 435, c: '#5ad1ff', no: 3, name: '前台',   en: 'Controller' },
  { t: 0.58, x: 600, y: 430, c: '#b98cff', no: 4, name: '调度室', en: 'UseCase' },
  { t: 0.76, x: 760, y: 395, c: '#ffb454', no: 5, name: '车间',   en: 'Service' },
  { t: 0.94, x: 900, y: 300, c: '#ffd166', no: 6, name: '金库',   en: 'PostgreSQL' }
];

var LOGIN_STATIONS = [
  { t: 0.06, x: 60,  y: 445, c: '#51e08a', no: 1, name: '前端',     en: 'LoginView' },
  { t: 0.24, x: 280, y: 450, c: '#5ad1ff', no: 2, name: '前台',     en: 'Controller' },
  { t: 0.44, x: 480, y: 435, c: '#b98cff', no: 3, name: '核验',     en: 'authenticate' },
  { t: 0.64, x: 680, y: 390, c: '#ffb454', no: 4, name: '发卡仪式', en: 'saveContext' },
  { t: 0.90, x: 905, y: 275, c: '#51e08a', no: 5, name: '卡片入袋', en: 'Cookie' }
];

/* ===== 绘制一个站点（等距小屋），全部 createElementNS ===== */
function drawStation(g, st) {
  var x = st.x, y = st.y, c = st.c;
  var root = el('g', { 'class': 'st-node', style: 'color:' + c }, g);

  // 地面光晕
  el('ellipse', { cx: x, cy: y + 34, rx: 52, ry: 10, fill: c, opacity: 0.10 }, root);
  // 屋体：整体轮廓
  el('path', {
    d: 'M ' + (x - 34) + ' ' + (y - 18) + ' L ' + x + ' ' + (y - 36) +
       ' L ' + (x + 34) + ' ' + (y - 18) + ' L ' + (x + 34) + ' ' + (y + 18) +
       ' L ' + x + ' ' + (y + 36) + ' L ' + (x - 34) + ' ' + (y + 18) + ' Z',
    fill: '#10141f', stroke: c, 'stroke-width': 2
  }, root);
  // 顶面（亮）
  el('path', {
    d: 'M ' + (x - 34) + ' ' + (y - 18) + ' L ' + x + ' ' + (y - 36) +
       ' L ' + (x + 34) + ' ' + (y - 18) + ' L ' + x + ' ' + y + ' Z',
    fill: c, opacity: 0.28
  }, root);
  // 左面（暗）
  el('path', {
    d: 'M ' + (x - 34) + ' ' + (y - 18) + ' L ' + x + ' ' + y +
       ' L ' + x + ' ' + (y + 36) + ' L ' + (x - 34) + ' ' + (y + 18) + ' Z',
    fill: c, opacity: 0.14
  }, root);
  // 站牌号
  el('circle', { cx: x, cy: y - 2, r: 13, fill: c, filter: 'url(#glow)' }, root);
  txt(root, x, y + 2.5, String(st.no), {
    'text-anchor': 'middle', 'font-size': 13,
    'font-weight': 900, fill: '#04121c'
  });
  // 名牌（激活时显示）
  var label = el('g', { 'class': 'st-label' }, root);
  el('rect', {
    x: x - 52, y: y + 44, width: 104, height: 40, rx: 9,
    fill: '#10141f', stroke: c, 'stroke-opacity': 0.6
  }, label);
  txt(label, x, y + 61, st.name, {
    'text-anchor': 'middle', 'font-size': 13, fill: c, 'font-weight': 'bold'
  });
  txt(label, x, y + 77, st.en, {
    'text-anchor': 'middle', 'font-size': 9.5, fill: '#7a8899',
    'font-family': 'Consolas,monospace'
  });
  return { root: root, label: label };
}

/* ===== 播放器工厂 ===== */
function makePlayer(cfg) {
  var stage   = document.getElementById(cfg.stageId);
  var nodesG  = stage.querySelector(cfg.nodesSel);
  var route   = stage.querySelector(cfg.routeSel);
  var parcel  = stage.querySelector(cfg.parcelSel);
  var capNo   = document.getElementById(cfg.capNo);
  var capTxt  = document.getElementById(cfg.capTxt);
  var trk     = document.getElementById(cfg.track);
  var btnPlay = document.getElementById(cfg.btnPlay);
  var btnRe   = document.getElementById(cfg.btnRe);
  var finale  = cfg.finaleSel ? stage.querySelector(cfg.finaleSel) : null;

  // 绘制站点，收集引用
  var nodes = [];
  for (var i = 0; i < cfg.stations.length; i++) {
    nodes.push(drawStation(nodesG, cfg.stations[i]));
  }

  // 拖尾：预创建圆点池（避免每帧改 DOM 结构）
  var trailG = el('g', {}, stage);
  stage.insertBefore(trailG, parcel);
  var TRAIL_N = 26;
  var trailDots = [];
  for (var d = 0; d < TRAIL_N; d++) {
    trailDots.push(el('circle', { cx: -100, cy: -100, r: 1, fill: '#5ad1ff', opacity: 0 }, trailG));
  }
  var trailPts = [];

  var t = 0, playing = false, speed = 1, raf = null, last = 0, done = false;
  var DUR = cfg.duration;

  function pointAt(p) {
    return route.getPointAtLength(route.getTotalLength() * p);
  }
  function setCaption(idx) {
    var c = cfg.captions[idx];
    capNo.textContent = c.no;
    capNo.style.color = c.color || '';
    capTxt.innerHTML = c.html;
  }

  function frame(now) {
    if (!playing) return;
    var dt = (now - last) / 1000;
    last = now;
    t = Math.min(1, t + dt * speed / DUR);
    trk.style.width = (t * 100) + '%';

    // 包裹位置
    var p = pointAt(t);
    parcel.setAttribute('opacity', (t > 0 && t < 0.995) ? 1 : 0);
    parcel.setAttribute('transform', 'translate(' + p.x + ',' + p.y + ')');

    // 拖尾
    trailPts.unshift({ x: p.x, y: p.y });
    if (trailPts.length > TRAIL_N) trailPts.pop();
    for (var i = 0; i < TRAIL_N; i++) {
      var dot = trailDots[i];
      if (i < trailPts.length) {
        var pt = trailPts[i];
        var f = 1 - i / TRAIL_N;
        dot.setAttribute('cx', pt.x);
        dot.setAttribute('cy', pt.y);
        dot.setAttribute('r', f * 6 + 0.5);
        dot.setAttribute('opacity', f * 0.5);
      } else {
        dot.setAttribute('opacity', 0);
      }
    }

    // 站点点亮 / 名牌
    var capIdx = 0;
    for (var s = 0; s < cfg.stations.length; s++) {
      var st = cfg.stations[s];
      var near = Math.abs(t - st.t) < 0.16;
      nodes[s].root.classList.toggle('lit', t >= st.t - 0.04);
      nodes[s].label.classList.toggle('show', near);
      if (t >= st.t) capIdx = s + 1;
    }

    // 字幕
    var ci = 0;
    for (var cc = 0; cc < cfg.captions.length; cc++) {
      if (t >= cfg.captions[cc].t) ci = cc;
    }
    setCaption(ci);

    // 终点
    if (t >= 1) {
      playing = false;
      done = true;
      parcel.setAttribute('opacity', 0);
      if (finale) finale.setAttribute('opacity', 1);
      if (cfg.onFinish) cfg.onFinish(stage, p);
      btnPlay.textContent = '▶ 再看一遍';
      return;
    }
    raf = requestAnimationFrame(frame);
  }

  function play() {
    if (done || t >= 1) {
      t = 0; trailPts.length = 0; done = false;
      if (finale) finale.setAttribute('opacity', 0);
    }
    if (playing) {
      playing = false;
      btnPlay.textContent = '▶ 继续';
      if (raf) cancelAnimationFrame(raf);
      return;
    }
    playing = true;
    btnPlay.textContent = '⏸ 暂停';
    last = performance.now();
    raf = requestAnimationFrame(frame);
  }

  btnPlay.addEventListener('click', play);
  btnRe.addEventListener('click', function () {
    playing = false;
    if (raf) cancelAnimationFrame(raf);
    t = 0; done = false; trailPts.length = 0;
    if (finale) finale.setAttribute('opacity', 0);
    trk.style.width = '0%';
    setCaption(0);
    setTimeout(play, 80);
  });
  if (cfg.btnSpd) {
    var btnSpd = document.getElementById(cfg.btnSpd);
    btnSpd.addEventListener('click', function () {
      speed = speed === 1 ? 1.6 : (speed === 1.6 ? 2.4 : 1);
      btnSpd.textContent = '速度 ' + speed + 'x';
    });
  }
}

/* ===== 启动两台播放器（任何错误都会显示在页面上） ===== */
try {
  makePlayer({
    stageId: 'stage1',
    nodesSel: '#nodes1',
    routeSel: '#route',
    parcelSel: '#parcel',
    finaleSel: '#finale',
    stations: STATIONS,
    duration: 16,
    btnPlay: 'btn1play',
    btnRe: 'btn1re',
    btnSpd: 'btn1spd',
    track: 'trk1',
    capNo: 'cap1no',
    capTxt: 'cap1txt',
    captions: [
      { t: 0, no: '▶', html: '包裹出发！带着你的用户名和密码，从前端启程。<small>此刻它们还是明文 —— 放心，路上很快会被「粉碎」。</small>' },
      { t: 0.06, no: 1, color: 'var(--green)', html: '<b>前端（RegisterView）</b>：把表单打包成 JSON，并自动贴好 CSRF 贴纸。<small>fetch 发出 POST /api/v1/auth/register。</small>' },
      { t: 0.22, no: 2, color: 'var(--red)', html: '<b>安检门（Security Filter）</b>：核对 CSRF 贴纸真伪。<small>假贴纸 / 没贴 → 403 当场拒收，包裹到此为止。</small>' },
      { t: 0.40, no: 3, color: 'var(--accent)', html: '<b>前台（AccountController）</b>：@Valid 核对表格 —— 用户名 1~30 位？密码 8~50 位？<small>不合格退件 422 并说明哪里错；合格则「拆包换箱」，转成业务命令 RegisterCommand。</small>' },
      { t: 0.58, no: 4, color: 'var(--purple)', html: '<b>调度室（AccountUseCase）</b>：注册流程简单，直接转交车间。<small>调度室真正的本领在登录时展示（发员工卡）。</small>' },
      { t: 0.76, no: 5, color: 'var(--orange)', html: '<b>车间（AccountService）</b>流水线：对暗号 → <b>Argon2 粉碎密码</b> → 查重 → 事务内插入。<small>粉碎在事务外（不占仓库通道）；同名包裹靠数据库唯一约束兜底防并发。</small>' },
      { t: 0.94, no: 6, color: 'var(--gold)', html: '<b>金库（PostgreSQL）</b>：包裹成为 app_user 表的一行。<small>id、username_key、时间戳由数据库生成 —— 所以车间还要回读一次，把编号写上回执。</small>' }
    ],
    onFinish: function () {}
  });

  makePlayer({
    stageId: 'stage2',
    nodesSel: '#nodes2',
    routeSel: '#route2',
    parcelSel: '#parcel2',
    stations: LOGIN_STATIONS,
    duration: 14,
    btnPlay: 'btn2play',
    btnRe: 'btn2re',
    track: 'trk2',
    capNo: 'cap2no',
    capTxt: 'cap2txt',
    captions: [
      { t: 0, no: '▶', html: '凭据包裹出发 —— 这次终点不是金库，而是一场<b>发卡仪式</b>。<small>观察最后一站的变化。</small>' },
      { t: 0.06, no: 1, color: 'var(--green)', html: '<b>前端（LoginView）</b>：提交用户名 + 密码。<small>页面加载时已自动领过 CSRF 贴纸。</small>' },
      { t: 0.24, no: 2, color: 'var(--accent)', html: '<b>前台（Controller）</b>：格式核对无误，转成 LoginCommand。<small>错误密码也会走到这里 —— 前台不猜密码对错。</small>' },
      { t: 0.44, no: 3, color: 'var(--purple)', html: '<b>核验（AccountService.authenticate）</b>：查档案 + Argon2 比对。<small>无论「用户名不存在 / 已停用 / 密码错误」，统一报「凭据无效」—— 防止坏人试探出哪些用户名存在。</small>' },
      { t: 0.64, no: 4, color: 'var(--orange)', html: '<b>发卡仪式（AccountUseCase）</b>：换会话 ID → 写安全上下文 → 存入会话表。<small>三步缺一不可 —— 这就是「真正建立登录状态」的含义。</small>' },
      { t: 0.90, no: 5, color: 'var(--green)', html: '<b>员工卡入袋（Set-Cookie）</b>：浏览器收下 SESSION 卡号，之后每次请求自动刷卡。<small>同时 CSRF 贴纸也换了新的 —— 前端要重新领一次再进受保护页面。</small>' }
    ],
    onFinish: function (stage) {
      var badge = stage.querySelector('#badge');
      var route = stage.querySelector('#route2');
      var ok = stage.querySelector('#badge-ok');
      if (!badge || !route) return;
      ok.setAttribute('opacity', 1);
      badge.setAttribute('opacity', 1);
      var L = route.getTotalLength();
      var start = performance.now();
      (function fly(now) {
        var p = Math.min(1, (now - start) / 1800);
        var e = 1 - Math.pow(1 - p, 3);
        var pt = route.getPointAtLength(L * (1 - e));
        badge.setAttribute('transform', 'translate(' + pt.x + ',' + pt.y + ')');
        if (p < 1) requestAnimationFrame(fly);
      })(start);
    }
  });
} catch (e) {
  showFatal(e.message);
}

/* ===== 导航高亮 + 进度条 ===== */
try {
  var links = document.querySelectorAll('.topbar a');
  var secs = document.querySelectorAll('section[id], header.hero');
  var pbar = document.getElementById('pbar');
  var io = new IntersectionObserver(function (es) {
    es.forEach(function (e) {
      if (e.isIntersecting) {
        var id = '#' + e.target.id;
        links.forEach(function (a) {
          a.classList.toggle('on', a.getAttribute('href') === id);
        });
      }
    });
  }, { rootMargin: '-30% 0px -60% 0px' });
  secs.forEach(function (s) { if (s.id) io.observe(s); });
  window.addEventListener('scroll', function () {
    var h = document.documentElement;
    pbar.style.width = (h.scrollTop / (h.scrollHeight - h.clientHeight) * 100) + '%';
  });
} catch (e) { showFatal(e.message); }
