import axios from 'axios'
import { ElLoading } from 'element-plus'
import Message from '../utils/Message'
const contentTypeForm = 'application/x-www-form-urlencoded;charset=UTF-8'
const contentTypeJson = 'application/json'
const responseTypeJson = 'json'

import { useLoginStore } from '@/stores/loginStore.js'

let loading = null
const instance = axios.create({
  withCredentials: true,
  baseURL: (import.meta.env.PROD ? import.meta.env.VITE_DOMAIN : '') + '/api',
  timeout: 10 * 1000,
})

//请求前拦截器
instance.interceptors.request.use(
  (config) => {
    if (config.showLoading) {
      loading = ElLoading.service({
        lock: true,
        text: '加载中......',
        background: 'rgba(0, 0, 0, 0.7)',
      })
    }
    return config
  },
  (error) => {
    if (error?.config?.showLoading && loading) {
      loading.close()
    }
    Message.error('请求发送失败')
    return Promise.reject('请求发送失败')
  },
)
//请求后拦截器
instance.interceptors.response.use(
  async (response) => {
    const {
      showLoading,
      errorCallback,
      showError = true,
      responseType,
      suppressLogin = false,
    } = response.config
    if (showLoading && loading) {
      loading.close()
    }
    const responseData = response.data
    if (responseType == 'arraybuffer' || responseType == 'blob') {
      return responseData
    }
    //正常请求
    if (responseData.code == 200) {
      return responseData
    } else if (responseData.code == 901) {
      const loginStore = useLoginStore()
      if (!suppressLogin) loginStore.showLogin = true
      loginStore.saveUserInfo({})
      return Promise.reject({ showError: false })
    } else {
      //其他错误
      if (errorCallback) {
        errorCallback(responseData)
      }
      return Promise.reject({ showError: showError, msg: responseData.info })
    }
  },
  (error) => {
    if (error?.config?.showLoading && loading) {
      loading.close()
    }
    return Promise.reject({ showError: true, msg: '网络异常' })
  },
)

const request = (config) => {
  const {
    url,
    params,
    dataType,
    showLoading = true,
    responseType = responseTypeJson,
    showError = true,
    suppressLogin = false,
    timeout = 10 * 1000,
    method = 'POST',
  } = config
  let contentType = contentTypeForm
  if (dataType != null && dataType == 'json') {
    contentType = contentTypeJson
  }
  let formData = params
  if (contentType === contentTypeForm) {
    formData = new FormData() // 创建form对象
    for (let key in params) {
      if (params[key] instanceof Array) {
        params[key].forEach((element) => {
          formData.append(key, element)
        })
      } else {
        formData.append(key, params[key] == undefined ? '' : params[key])
      }
    }
  }
  let headers = {
    'Content-Type': contentType,
    'X-Requested-With': 'XMLHttpRequest',
    token: localStorage.getItem('token') || '',
  }
  const axiosConfig = {
      onUploadProgress: (event) => {
        if (config.uploadProgressCallback) {
          config.uploadProgressCallback(event)
        }
      },
      responseType: responseType,
      headers: headers,
      showLoading: showLoading,
      errorCallback: config.errorCallback,
      showError: showError,
      suppressLogin,
      timeout,
      method: method.toLowerCase(),
    }
  if (method.toUpperCase() === 'GET') {
    axiosConfig.params = params || {}
    delete axiosConfig.data
  } else if (method.toUpperCase() === 'DELETE') {
    axiosConfig.data = formData
  } else {
    axiosConfig.data = formData
  }
  return instance
    .request({ url, ...axiosConfig })
    .catch((error) => {
      if (error.showError) {
        Message.error(error.msg)
      }
      return null
    })
}

export default request
