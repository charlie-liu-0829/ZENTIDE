import '@/assets/icon/iconfont.css'
import '@/assets/base.scss'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import {
  ElAlert,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElButton,
  ElCard,
  ElCol,
  ElConfigProvider,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElForm,
  ElFormItem,
  ElImage,
  ElImageViewer,
  ElInput,
  ElInputNumber,
  ElLoading,
  ElOption,
  ElPagination,
  ElRow,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus'
import 'element-plus/dist/index.css'

import Request from '@/utils/Request'
import Message from '@/utils/Message'
import Utils from '@/utils/Utils'
import { Confirm, Alert } from '@/utils/Confirm.js'
import { Api } from '@/utils/Api.js'
import Verify from '@/utils/Verify.js'

import Table from '@/components/Table.vue'
import Cover from '@/components/Cover.vue'
import Avatar from '@/components/Avatar.vue'

const app = createApp(App)
;[
  ElAlert,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElButton,
  ElCard,
  ElCol,
  ElConfigProvider,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElForm,
  ElFormItem,
  ElImage,
  ElImageViewer,
  ElInput,
  ElInputNumber,
  ElLoading,
  ElOption,
  ElPagination,
  ElRow,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
].forEach((component) => app.use(component))
app.use(createPinia())
app.use(router)

app.component('Cover', Cover)
app.component('Avatar', Avatar)
app.component('Table', Table)

app.config.globalProperties.Request = Request
app.config.globalProperties.Message = Message
app.config.globalProperties.Utils = Utils
app.config.globalProperties.Api = Api
app.config.globalProperties.Confirm = Confirm
app.config.globalProperties.Alert = Alert
app.config.globalProperties.Verify = Verify
app.config.globalProperties.imageThumbnailSuffix = '_thumbnail'
//图片后缀
app.config.globalProperties.imageAccept = '.jpg,.png,.gif,.bmp,.webp'
app.mount('#app')
