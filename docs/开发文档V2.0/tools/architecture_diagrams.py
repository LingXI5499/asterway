"""Reusable inline SVG diagrams for the architecture review chapter.

The three public diagram functions return complete figure markup. Importing this
module performs no file, network, application, database or repository operation.
"""
from __future__ import annotations

import html

__all__ = ["module_diagram", "error_diagram", "database_diagram"]

MODULES = (
    ("app", "启动、HTTP 与应用编排"),
    ("common", "公共机制与基础契约"),
    ("identity", "账户与身份"),
    ("growth", "目标与任务"),
    ("planning", "计划与安排"),
    ("execution", "执行与计时"),
    ("habit", "习惯"),
    ("insights", "统计与洞察"),
    ("notification", "通知"),
    ("administration", "管理与审计"),
)


def esc(value: str) -> str:
    return html.escape(value, quote=True)


def text(x: float, y: float, label: str, *, size: int = 18,
         anchor: str = "middle", weight: str = "normal", fill: str = "#173b39") -> str:
    return (f'<text x="{x}" y="{y}" text-anchor="{anchor}" font-size="{size}" '
            f'font-weight="{weight}" fill="{fill}">{esc(label)}</text>')


def box(x: int, y: int, w: int, h: int, title: str, subtitle: str = "", *,
        fill: str = "#eef6f1", identity: str = "", title_size: int = 21) -> str:
    attr = f' id="{esc(identity)}"' if identity else ""
    result = (f'<g{attr}><rect x="{x}" y="{y}" width="{w}" height="{h}" '
              f'rx="12" fill="{fill}" stroke="#74988c" stroke-width="1.6"/>')
    result += text(x + w / 2, y + (h / 2 if subtitle else h / 2 + 7),
                   title, size=title_size, weight="600")
    if subtitle:
        result += text(x + w / 2, y + h / 2 + 28, subtitle, size=16)
    return result + "</g>"


def path(d: str, diagram: str, *, error: bool = False, dashed: bool = False,
         marker: bool = True) -> str:
    color = "#b66a28" if error else "#387d75"
    marker_attr = f' marker-end="url(#{diagram}-{"error" if error else "arrow"})"' if marker else ""
    dash = ' stroke-dasharray="7 5"' if dashed else ""
    return (f'<path d="{d}" fill="none" stroke="{color}" stroke-width="2.2"'
            f'{marker_attr}{dash}/>')


def svg_start(name: str, width: int, height: int, title: str, description: str) -> str:
    return f'''<svg xmlns="http://www.w3.org/2000/svg" role="img" viewBox="0 0 {width} {height}"
      aria-labelledby="{name}-title {name}-desc" class="diagram-svg">
      <title id="{name}-title">{esc(title)}</title>
      <desc id="{name}-desc">{esc(description)}</desc>
      <defs>
        <marker id="{name}-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#387d75"/></marker>
        <marker id="{name}-error" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#b66a28"/></marker>
      </defs>
      <rect width="100%" height="100%" fill="#fff"/>
      <g font-family="'Microsoft YaHei','PingFang SC','Noto Sans CJK SC',sans-serif">'''


def figure(name: str, svg: str, caption: str) -> str:
    return (f'<figure class="diagram" id="figure-{name}"><div class="diagram-scroll" '
            f'tabindex="0" role="region" aria-label="{esc(caption)}，窄屏可横向滚动">'
            f'{svg}</div><figcaption>{esc(caption)}</figcaption></figure>')


def module_diagram() -> str:
    name = "modules"
    parts = [svg_start(name, 1510, 650, "8 个业务工程 + 2 个支撑工程的依赖方向",
        "app 依赖 common 及 8 个业务工程；每个业务工程仅依赖 common；common 无内部工程依赖。箭头从依赖方指向被依赖方。此图表示当前 POM 结构及建议继续保持的依赖方向。")]
    parts += [text(755, 30, "8 个业务工程 + 2 个支撑工程；保持当前依赖方向", size=23, weight="600"),
              box(525, 58, 460, 83, "app", "运行入口；HTTP / Security / 编排为建议职责", identity="module-app")]
    parts.append(path("M 755 141 V 188 H 128", name, marker=False))
    parts.append(path("M 755 188 H 1375", name, marker=False))
    business = MODULES[2:]
    for index, (module, label) in enumerate(business):
        x = 47 + index * 178
        mid = x + 81
        parts += [path(f"M {mid} 188 V 242", name),
                  box(x, 242, 162, 91, module, label, identity=f"module-{module}", title_size=19),
                  path(f"M {mid} 333 V 405", name, marker=False)]
    parts += [path("M 128 405 H 1375", name, marker=False),
              path("M 755 405 V 476", name),
              box(525, 476, 460, 88, "common", "无内部工程依赖 · 不承载 Web 与领域规则", identity="module-common"),
              path("M 985 100 H 1475 V 520 H 986", name),
              text(1457, 465, "直接依赖", anchor="end", size=17),
              path("M 70 610 H 145", name),
              text(164, 616, "箭头：依赖方 → 被依赖方；横线为共享连接总线", anchor="start", size=18),
              text(1450, 616, "8 个业务工程 + 2 个支撑工程", anchor="end", size=18),
              "</g></svg>"]
    return figure(name, "".join(parts),
                  "工程依赖图：8 个业务工程 + 2 个支撑工程；连同父工程共 11 个 Reactor 项目。依赖箭头为现状，职责按建议方案标注。")


def error_diagram() -> str:
    name = "errors"
    parts = [svg_start(name, 1410, 935, "HTTP 请求与异常响应的分层路径（建议）",
        "请求先经过 traceId 过滤器及 Security 过滤器，再进入 MVC、应用用例、业务服务和 Mapper XML。Security 的未认证入口、拒绝访问处理器及认证失败回调直接使用统一错误工厂和 JSON 写出器。MVC、应用和业务层异常由 Advice 转换；Advice 不捕获所有过滤器异常。")]
    parts += [text(705, 30, "HTTP 与异常响应路径（建议；当前尚无实现）", size=23, weight="600"),
              box(35, 72, 180, 88, "HTTP 请求", "携带 / 分配 traceId"),
              box(260, 72, 225, 88, "TraceIdFilter", "位于 Security 之前"),
              box(535, 72, 235, 88, "Security 过滤器链", "认证 · 授权 · CSRF"),
              box(1010, 72, 320, 88, "HTTP Controller", "Web 请求 / 响应类型与校验"),
              path("M 215 116 H 260", name), path("M 485 116 H 535", name),
              path("M 770 116 H 1010", name), text(895, 104, "正常请求", size=17),
              box(1010, 270, 320, 88, "Application 用例", "跨模块编排与事务边界"),
              box(1010, 470, 320, 88, "业务工程 api / service", "领域规则与数据所有权"),
              box(1010, 670, 320, 88, "Mapper + XML", "参数绑定与本模块持久化"),
              path("M 1170 160 V 270", name), path("M 1170 358 V 470", name),
              path("M 1170 558 V 670", name)]
    # Security failure paths deliberately do not pass through MVC Advice.
    for x, title, subtitle in (
        (30, "AuthenticationEntryPoint", "未认证：401"),
        (335, "AccessDeniedHandler", "拒绝访问 / CSRF：403"),
        (640, "AuthenticationFailureHandler", "认证失败回调：按错误契约"),
    ):
        parts.append(box(x, 270, 290, 88, title, subtitle, fill="#fff4e9", title_size=17))
    parts += [path("M 652 160 V 218 H 175 V 270", name, error=True),
              path("M 652 218 H 480 V 270", name, error=True),
              path("M 652 218 H 785 V 270", name, error=True),
              box(570, 470, 345, 105, "GlobalExceptionHandler", "MVC / 业务异常；未处理异常 → 500", fill="#fff4e9", title_size=20),
              path("M 1010 116 H 960 V 523 H 915", name, error=True, dashed=True),
              path("M 1010 314 H 960", name, error=True, dashed=True, marker=False),
              path("M 1010 514 H 960", name, error=True, dashed=True, marker=False),
              box(30, 470, 430, 118, "过滤器其他异常", "本层显式处理 / 错误派发；已提交则不改写", fill="#fff8f1", title_size=21),
              text(245, 609, "Advice 不兜底捕获全部过滤器异常", size=17, fill="#8b4c1b"),
              box(280, 715, 635, 96, "统一错误工厂 / ApiErrorWriter", "code · message · data · traceId；统一状态码与 JSON", fill="#eaf4f9", title_size=23),
              path("M 175 358 V 395 H 15 V 680 H 400 V 715", name, error=True),
              path("M 480 358 V 642 H 520 V 715", name, error=True),
              path("M 785 358 V 415 H 942 V 685 H 840 V 715", name, error=True),
              path("M 742 575 V 715", name, error=True),
              path("M 245 588 V 687 H 340 V 715", name, error=True, dashed=True),
              path("M 597 811 V 860 H 1170 V 818", name, error=True),
              box(1010, 788, 320, 73, "HTTP 错误响应", "application/json", fill="#eaf4f9"),
              path("M 35 903 H 108", name), text(126, 910, "正常调用", anchor="start", size=17),
              path("M 330 903 H 405", name, error=True), text(423, 910, "错误处理 / 写出", anchor="start", size=17),
              path("M 700 903 H 775", name, error=True, dashed=True),
              text(793, 910, "异常转交；非正常调用", anchor="start", size=17),
              "</g></svg>"]
    return figure(name, "".join(parts),
                  "异常路径图：Security 回调与 MVC Advice 共用错误契约；二者具有不同的拦截边界。")


def database_diagram() -> str:
    name = "database"
    fields = (
        ("id", "UUID", "PK · DEFAULT gen_random_uuid()"),
        ("username", "VARCHAR(30)", "NOT NULL"),
        ("username_key", "VARCHAR(30)", "UQ · GENERATED STORED · 无显式 NOT NULL"),
        ("nickname", "VARCHAR(30)", "NOT NULL"),
        ("password_hash", "VARCHAR(255)", "NOT NULL"),
        ("role", "VARCHAR(16)", "NOT NULL · DEFAULT USER"),
        ("status", "VARCHAR(16)", "NOT NULL · DEFAULT ENABLED"),
        ("created_at", "TIMESTAMPTZ(6)", "NOT NULL · DEFAULT clock_timestamp()"),
        ("updated_at", "TIMESTAMPTZ(6)", "NOT NULL · DEFAULT clock_timestamp()"),
    )
    parts = [svg_start(name, 1320, 770, "V001 工作树 DDL 单表结构：app.app_user",
        "账户表 app.app_user 有九列。id 为主键，username_key 为唯一的存储生成列。此迁移没有物理外键，不绘制关系线。结构来自本地迁移文件，不代表已读取实际数据库。")]
    parts += [text(660, 37, "V001 工作树 DDL：1 表 · 9 列 · 0 物理外键", size=24, weight="600"),
              '<g id="table-app-app-user">',
              '<rect x="55" y="74" width="1210" height="596" rx="13" fill="#fff" stroke="#638d80" stroke-width="2"/>',
              '<path d="M 68 74 H 1252 Q 1265 74 1265 87 V 147 H 55 V 87 Q 55 74 68 74 Z" fill="#e5f0e9"/>',
              text(660, 110, "app.app_user · 用户账户表 · identity 所有", size=25, weight="600"),
              text(90, 142, "字段", anchor="start", size=18, weight="600"),
              text(410, 142, "SQL 类型", anchor="start", size=18, weight="600"),
              text(700, 142, "键 / 约束 / 默认值", anchor="start", size=18, weight="600")]
    for index, (field, sql_type, note) in enumerate(fields):
        y = 150 + index * 52
        if index % 2 == 0:
            parts.append(f'<rect x="57" y="{y}" width="1206" height="52" fill="#f6faf7"/>')
        parts += [text(90, y + 33, field, anchor="start", size=20,
                       weight="600" if field in ("id", "username_key") else "normal"),
                  text(410, y + 33, sql_type, anchor="start", size=20),
                  text(700, y + 33, note, anchor="start", size=17)]
    parts += [text(90, 650, "4 个 CHECK：用户名长度、昵称长度、角色枚举、状态枚举", anchor="start", size=19),
              "</g>", text(660, 706, "图例：PK 主键；UQ 唯一；GENERATED STORED 存储生成列；无 FK，故无关系箭头。", size=19),
              text(660, 743, "证据边界：读取迁移工作树；数据库执行状态由用户说明，未连接数据库核验。", size=18),
              "</g></svg>"]
    return figure(name, "".join(parts),
                  "迁移定义单表图：未伪造跨表关系；实际已应用状态、约束及索引仍待后续独立核验。")
