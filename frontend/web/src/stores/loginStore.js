import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLoginStore = defineStore('loginState', () => {
  const showLogin = ref(false)
  const userInfo = ref({})
  const saveUserInfo = (value) => {
    userInfo.value = value
  }
  return { showLogin, userInfo, saveUserInfo }
})
