import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElAlert,
  ElAside,
  ElAvatar,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElButton,
  ElCard,
  ElCol,
  ElConfigProvider,
  ElContainer,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElHeader,
  ElIcon,
  ElImage,
  ElInput,
  ElInputNumber,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElPopover,
  ElRow,
  ElSelect,
  ElSubMenu,
  ElTable,
  ElTableColumn,
  ElTag,
  ElUpload
} from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './styles.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

app
  .use(createPinia())
  .use(router)
  .use(ElAlert)
  .use(ElAside)
  .use(ElAvatar)
  .use(ElBreadcrumb)
  .use(ElBreadcrumbItem)
  .use(ElButton)
  .use(ElCard)
  .use(ElCol)
  .use(ElConfigProvider, { locale: zhCn })
  .use(ElContainer)
  .use(ElDatePicker)
  .use(ElDescriptions)
  .use(ElDescriptionsItem)
  .use(ElDialog)
  .use(ElDropdown)
  .use(ElDropdownItem)
  .use(ElDropdownMenu)
  .use(ElEmpty)
  .use(ElForm)
  .use(ElFormItem)
  .use(ElHeader)
  .use(ElIcon)
  .use(ElImage)
  .use(ElInput)
  .use(ElInputNumber)
  .use(ElMain)
  .use(ElMenu)
  .use(ElMenuItem)
  .use(ElOption)
  .use(ElPagination)
  .use(ElPopover)
  .use(ElRow)
  .use(ElSelect)
  .use(ElSubMenu)
  .use(ElTable)
  .use(ElTableColumn)
  .use(ElTag)
  .use(ElUpload)
  .mount('#app')

