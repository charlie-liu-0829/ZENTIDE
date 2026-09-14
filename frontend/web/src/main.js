import '@/assets/icon/iconfont.css'
import {
  ElAlert,
  ElButton,
  ElConfigProvider,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElImage,
  ElImageViewer,
  ElInput,
  ElOption,
  ElRadio,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElUpload,
} from 'element-plus'
import 'element-plus/dist/index.css'
import '@/assets/base.scss'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import Request from '@/utils/Request'
import Message from '@/utils/Message'
import Utils from '@/utils/Utils'
import { Confirm, Alert } from '@/utils/Confirm.js'
import { Api } from '@/utils/Api.js'
import Verify from '@/utils/Verify.js'

import Cover from '@/components/Cover.vue'
import Avatar from '@/components/Avatar.vue'
import Dialog from '@/components/Dialog.vue'

const app = createApp(App)
;[
  ElAlert,
  ElButton,
  ElConfigProvider,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElImage,
  ElImageViewer,
  ElInput,
  ElOption,
  ElRadio,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElUpload,
].forEach((component) => app.use(component))
app.use(createPinia())
app.use(router)

app.component('Dialog', Dialog)
app.component('Cover', Cover)
app.component('Avatar', Avatar)

app.config.globalProperties.Request = Request
app.config.globalProperties.Message = Message
app.config.globalProperties.Utils = Utils
app.config.globalProperties.Api = Api
app.config.globalProperties.Confirm = Confirm
app.config.globalProperties.Alert = Alert
app.config.globalProperties.Verify = Verify
app.config.globalProperties.bodyWidth = 1300
app.config.globalProperties.imageThumbnailSuffix = '_thumbnail'
app.config.globalProperties.imageAccept = '.jpg,.png,.gif,.bmp,.webp'
app.mount('#app')
