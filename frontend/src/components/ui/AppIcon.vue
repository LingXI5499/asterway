<script setup>
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
import { computed } from "vue";
const props = defineProps({
  name: String,
  size: { type: [Number, String], default: 18 },
});
const icons = {
  home: House,
  goal: Flag,
  stage: Layers,
  task: ListTodo,
  week: CalendarDays,
  day: Calendar,
  execution: Timer,
  habit: Repeat,
  notification: Bell,
  statistics: BarChart3,
  settings: Settings,
  users: Users,
  role: Shield,
  permission: KeyRound,
  menu: Menu,
  closeSidebar: PanelLeftClose,
  openSidebar: PanelLeftOpen,
  dark: Moon,
  light: Sun,
  system: Monitor,
  chevron: ChevronRight,
  logout: LogOut,
  user: User,
  check: Check,
  plus: Plus,
  arrow: ArrowRight,
  search: Search,
  inbox: Inbox,
  folder: Folder,
  clock: Clock,
  grip: GripVertical,
  grid: LayoutGrid,
  list: List,
  error: AlertCircle,
  refresh: RefreshCw,
  more: MoreHorizontal,
  history: History,
  device: Laptop,
  sparkle: Sparkles,
  close: X,
  security: ShieldCheck,
  edit: Pencil,
  delete: Trash2,
  stop: Square,
  send: PhArrowUp,
  back: PhArrowLeft,
  chat: PhChatCircleText,
  book: PhBookOpen,
  memory: PhLightbulb,
  compose: PhNotePencil,
  copy: PhCopy,
  info: PhInfo,
};
const icon = computed(() => icons[props.name] || Folder);
</script>
<template>
  <component
    :is="icon"
    :size="size"
    weight="regular"
    class="aw-icon"
    aria-hidden="true"
  />
</template>
