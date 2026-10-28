import {
  ElAlert,
  ElAvatar,
  ElButton,
  ElCard,
  ElCheckbox,
  ElConfigProvider,
  ElDescriptions,
  ElDescriptionsItem,
  ElDivider,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  ElSkeleton,
  ElSlider,
  ElTabPane,
  ElTabs,
  ElTag,
} from 'element-plus'
import 'element-plus/dist/index.css'

// 只注册账户页面使用的组件，避免把整个组件库的 JavaScript 打入首屏。
const components = [
  ElAlert,
  ElAvatar,
  ElButton,
  ElCard,
  ElCheckbox,
  ElConfigProvider,
  ElDescriptions,
  ElDescriptionsItem,
  ElDivider,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  ElSkeleton,
  ElSlider,
  ElTabPane,
  ElTabs,
  ElTag,
]

export function registerElementPlus(app) {
  for (const component of components) {
    app.component(component.name, component)
  }
}
