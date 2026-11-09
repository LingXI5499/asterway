<!-- 渲染应用Icon界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 import { PhHouseLine as House, PhFlag as Flag, PhStack as Layers, PhListChecks as ListTodo, PhCalend… 提供的组件、状态函数或请求方法。 */
import {
  PhHouseLine as House, PhFlag as Flag, PhStack as Layers, PhListChecks as ListTodo,
  PhCalendarBlank as CalendarDays, PhCalendar as Calendar, PhTimer as Timer,
  PhRepeat as Repeat, PhBell as Bell, PhChartBar as BarChart3, PhGearSix as Settings,
  PhUsers as Users, PhShield as Shield, PhKey as KeyRound, PhList as Menu,
  PhSidebarSimple as PanelLeftClose, PhSidebarSimple as PanelLeftOpen,
  PhMoon as Moon, PhSun as Sun, PhMonitor as Monitor, PhCaretRight as ChevronRight,
  PhSignOut as LogOut, PhUser as User, PhCheck as Check, PhPlus as Plus,
  PhArrowRight as ArrowRight, PhMagnifyingGlass as Search, PhTray as Inbox,
  PhFolderSimple as Folder, PhClock as Clock, PhDotsSixVertical as GripVertical,
  PhSquaresFour as LayoutGrid, PhListBullets as List, PhWarningCircle as AlertCircle,
  PhArrowsClockwise as RefreshCw, PhDotsThree as MoreHorizontal,
  PhClockCounterClockwise as History, PhLaptop as Laptop, PhSparkle as Sparkles,
  PhX as X, PhShieldCheck as ShieldCheck, PhPencilSimple as Pencil,
  PhTrash as Trash2, PhSquare as Square, PhArrowUp, PhArrowLeft,
  PhChatCircleText, PhBookOpen, PhLightbulb, PhNotePencil, PhCopy, PhInfo,
} from "@phosphor-icons/vue";
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 名称（name）字段使用可为空的文字（String）。 */
  name: String,
  /* 每页条数（size）字段使用表达式 { type: [Number, String], default: 18 } 的结果。 */
  size: { /* 类型（type）字段使用表达式 [Number, String] 的结果。 */ type: [Number, String], /* 默认值（default）字段使用固定数值 18。 */ default: 18 },
});
const /* icons保存表达式 { home: House, goal: Flag, stage: Layers, task: ListTodo, week: CalendarDays, day: Calendar, execution: Timer, habit: Re… 的结果。 */ icons = {
  /* home字段使用House。 */
  home: House,
  /* 目标（goal）字段使用开关（Flag）。 */
  goal: Flag,
  /* 阶段（stage）字段使用Layers。 */
  stage: Layers,
  /* 任务（task）字段使用列表Todo（ListTodo）。 */
  task: ListTodo,
  /* 周（week）字段使用Calendar日列表（CalendarDays）。 */
  week: CalendarDays,
  /* 日（day）字段使用Calendar。 */
  day: Calendar,
  /* 执行计时（execution）字段使用定时器（Timer）。 */
  execution: Timer,
  /* 习惯（habit）字段使用Repeat。 */
  habit: Repeat,
  /* 通知（notification）字段使用Bell。 */
  notification: Bell,
  /* 统计（statistics）字段使用BarChart3。 */
  statistics: BarChart3,
  /* 配置（settings）字段使用配置（Settings）。 */
  settings: Settings,
  /* 用户列表（users）字段使用用户列表（Users）。 */
  users: Users,
  /* 角色（role）字段使用Shield。 */
  role: Shield,
  /* 权限（permission）字段使用键轮（KeyRound）。 */
  permission: KeyRound,
  /* 菜单（menu）字段使用菜单（Menu）。 */
  menu: Menu,
  /* 关闭Sidebar（closeSidebar）字段使用面板Left关闭（PanelLeftClose）。 */
  closeSidebar: PanelLeftClose,
  /* 是否展开Sidebar（openSidebar）字段使用面板Left是否展开（PanelLeftOpen）。 */
  openSidebar: PanelLeftOpen,
  /* dark字段使用Moon。 */
  dark: Moon,
  /* light字段使用Sun。 */
  light: Sun,
  /* 系统（system）字段使用Monitor。 */
  system: Monitor,
  /* chevron字段使用ChevronRight。 */
  chevron: ChevronRight,
  /* 退出登录（logout）字段使用日志输出（LogOut）。 */
  logout: LogOut,
  /* 用户（user）字段使用用户（User）。 */
  user: User,
  /* 核对（check）字段使用核对（Check）。 */
  check: Check,
  /* plus字段使用Plus。 */
  plus: Plus,
  /* arrow字段使用ArrowRight。 */
  arrow: ArrowRight,
  /* 搜索（search）字段使用搜索（Search）。 */
  search: Search,
  /* inbox字段使用Inbox。 */
  inbox: Inbox,
  /* folder字段使用Folder。 */
  folder: Folder,
  /* clock字段使用Clock。 */
  clock: Clock,
  /* grip字段使用GripVertical。 */
  grip: GripVertical,
  /* grid字段使用LayoutGrid。 */
  grid: LayoutGrid,
  /* 列表（list）字段使用有顺序的数据列表（List）。 */
  list: List,
  /* 错误（error）字段使用AlertCircle。 */
  error: AlertCircle,
  /* 刷新（refresh）字段使用刷新Cw（RefreshCw）。 */
  refresh: RefreshCw,
  /* more字段使用MoreHorizontal。 */
  more: MoreHorizontal,
  /* 历史（history）字段使用历史（History）。 */
  history: History,
  /* 设备（device）字段使用Laptop。 */
  device: Laptop,
  /* sparkle字段使用Sparkles。 */
  sparkle: Sparkles,
  /* 关闭（close）字段使用X。 */
  close: X,
  /* security字段使用Shield核对（ShieldCheck）。 */
  security: ShieldCheck,
  /* edit字段使用Pencil。 */
  edit: Pencil,
  /* 删除（delete）字段使用Trash2。 */
  delete: Trash2,
  /* 停止（stop）字段使用Square。 */
  stop: Square,
  /* 发送（send）字段使用PhArrowUp。 */
  send: PhArrowUp,
  /* back字段使用PhArrowLeft。 */
  back: PhArrowLeft,
  /* 对话（chat）字段使用PhChatCircle文字（PhChatCircleText）。 */
  chat: PhChatCircleText,
  /* book字段使用PhBook是否展开（PhBookOpen）。 */
  book: PhBookOpen,
  /* 长期记忆（memory）字段使用PhLightbulb。 */
  memory: PhLightbulb,
  /* compose字段使用Ph说明Pencil（PhNotePencil）。 */
  compose: PhNotePencil,
  /* copy字段使用PhCopy。 */
  copy: PhCopy,
  /* 信息（info）字段使用Ph信息（PhInfo）。 */
  info: PhInfo,
};
const /* icon保存随相关状态变化自动重算的值。 */ icon = computed(() => icons[props.name] || Folder);
</script>
<template>
  <!-- 显示component组件；is 取自 icon；size 取自 size；weight 固定为 regular；用样式类 aw-icon 控制外观；aria-hidden 提供无障碍说明 true。 -->
  <component
    :is="icon"
    :size="size"
    weight="regular"
    class="aw-icon"
    aria-hidden="true"
  />
</template>
