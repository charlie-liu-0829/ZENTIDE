<template>
  <Dialog
    :show="loginStore.showLogin"
    :buttons="dialogConfig.buttons"
    width="780px"
    :showCancel="false"
    @close="closeDialog"
    :padding="0"
    :draggable="false"
    :top="100"
  >
    <div class="dialog-panel">
      <section class="welcome-panel">
        <span class="welcome-mark">知</span>
        <p class="welcome-overline">ZENTIDE</p>
        <h2>回到你的兴趣社区</h2>
        <p>和同好讨论真正感兴趣的事，也不错过与你有关的新变化。</p>
        <ul>
          <li>加入或创建自己的兴趣现场</li>
          <li>分享讨论、经验、图片与视频</li>
          <li>关注值得长期了解的话题</li>
        </ul>
      </section>
      <el-form class="login-register" :model="formData" :rules="rules" ref="formDataRef">
        <div class="tab-panel">
          <span>{{ opType == 0 ? '创建知潮账号' : opType == 2 ? '找回账号密码' : '欢迎回来' }}</span
          ><small>{{ opType == 0 ? '从一个真正感兴趣的话题开始' : opType == 2 ? '通过密保问题重设密码' : '继续你的社区与关注' }}</small>
        </div>
        <!--input输入-->
        <el-form-item prop="email">
          <el-input size="large" clearable placeholder="请输入邮箱" v-model="formData.email" maxLength="150">
            <template #prefix>
              <span class="iconfont icon-email"></span>
            </template>
          </el-input>
        </el-form-item>
        <!--登录密码-->
        <el-form-item prop="password" v-if="opType == 1">
          <el-input show-password size="large" placeholder="请输入密码" v-model="formData.password">
            <template #prefix>
              <span class="iconfont icon-password"></span>
            </template>
          </el-input>
        </el-form-item>
        <template v-if="opType == 2">
          <el-form-item prop="securityAnswer" v-if="securityQuestion">
            <div class="security-question">密保问题：{{ securityQuestion }}</div>
            <el-input size="large" placeholder="请输入密保答案" v-model="formData.securityAnswer" />
          </el-form-item>
          <el-form-item prop="newPassword" v-if="securityQuestion">
            <el-input show-password type="password" size="large" placeholder="请输入新密码" v-model="formData.newPassword" />
          </el-form-item>
        </template>
        <!--注册-->
        <div v-if="opType == 0">
          <el-form-item prop="nickName" v-if="opType == 0">
            <el-input
              size="large"
              clearable
              placeholder="请输入昵称"
              v-model="formData.nickName"
              maxLength="20"
            >
              <template #prefix>
                <span class="iconfont icon-account"></span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="registerPassword">
            <el-input
              show-password
              type="password"
              size="large"
              placeholder="请输入密码"
              v-model="formData.registerPassword"
            >
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="reRegisterPassword">
            <el-input
              show-password
              type="password"
              size="large"
              placeholder="请再次输入密码"
              v-model="formData.reRegisterPassword"
            >
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
            </el-input>
          </el-form-item>
          <div class="security-hint">
            <span>密保问题（可选）</span>
            <small>用于忘记密码时验证身份，答案会加密保存。</small>
          </div>
          <el-form-item prop="securityQuestion">
            <el-input size="large" placeholder="例如：我最喜欢的乐队是？" v-model="formData.securityQuestion" />
          </el-form-item>
          <el-form-item prop="securityAnswer">
            <el-input size="large" placeholder="请输入密保答案" v-model="formData.securityAnswer" />
          </el-form-item>
        </div>
        <el-form-item prop="checkCode" v-if="opType !== 2">
          <div class="check-code-panel">
            <el-input
              size="large"
              placeholder="请输入验证码"
              v-model="formData.checkCode"
              @keyup.enter="doSubmit"
            >
              <template #prefix>
                <span class="iconfont icon-checkcode"></span>
              </template>
            </el-input>
            <img :src="checkCodeInfo.checkCode" class="check-code" @click="changeCheckCode()" />
          </div>
        </el-form-item>
        <el-form-item class="bottom-btn">
          <el-button type="primary" size="large" class="login-btn" @click="doSubmit">
            <span v-if="opType == 0">创建账号</span>
            <span v-if="opType == 1">进入知潮</span><span v-if="opType == 2">重置密码</span>
          </el-button>
          <div class="op-link" @click="showPanel(opType == 0 ? 1 : 0)">
            {{ opType == 0 ? '已有账号？去登录' : '没有账号？去注册' }}
          </div>
          <div v-if="opType == 1" class="op-link secondary" @click="showPanel(2)">忘记密码？使用密保找回</div>
          <div v-if="opType == 2" class="op-link secondary" @click="showPanel(1)">返回登录</div>
        </el-form-item>
      </el-form>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, getCurrentInstance, nextTick, onMounted, watch } from 'vue'
const { proxy } = getCurrentInstance()

import { useLoginStore } from '@/stores/loginStore.js'
const loginStore = useLoginStore()

//验证码
const checkCodeInfo = ref({})
const changeCheckCode = async () => {
  let result = await proxy.Request({
    url: proxy.Api.checkCode,
  })
  if (!result) {
    return
  }
  checkCodeInfo.value = result.data
}

//登录，注册 弹出配置
const dialogConfig = ref({
  show: true,
})

const checkRePassword = (rule, value, callback) => {
  if (value !== formData.value.registerPassword) {
    callback(new Error(rule.message))
  } else {
    callback()
  }
}

// 0:注册 1:登录
const opType = ref(1)
const formData = ref({})
const formDataRef = ref()
const securityQuestion = ref('')
const rules = {
  email: [
    { required: true, message: '请输入邮箱' },
    { validator: proxy.Verify.email, message: '请输入正确的邮箱' },
  ],
  password: [{ required: true, message: '请输入密码' }],
  nickName: [{ required: true, message: '请输入昵称' }],
  registerPassword: [
    { required: true, message: '请输入密码' },
    {
      validator: proxy.Verify.password,
      message: '密码至少1个数字1个字母，允许数字，字母，特殊字符 8-18位',
    },
  ],
  reRegisterPassword: [
    { required: true, message: '请再次输入密码' },
    {
      validator: checkRePassword,
      message: '两次输入的密码不一致',
    },
  ],
  checkCode: [{ required: true, message: '请输入图片验证码' }],
  securityQuestion: [{ max: 200, message: '密保问题不能超过 200 个字符' }],
  securityAnswer: [],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { validator: proxy.Verify.password, message: '密码至少1个数字1个字母，允许数字，字母，特殊字符 8-18位' },
  ],
}

//重置表单
const resetForm = () => {
  changeCheckCode()
  nextTick(() => {
    formDataRef.value?.resetFields()
    formData.value = {}
    securityQuestion.value = ''
  })
}

const loadSecurityQuestion = async () => {
  const email = String(formData.value.email || '').trim()
  if (!email) {
    proxy.Message.warning('请先输入注册邮箱')
    return false
  }
  const result = await proxy.Request({ url: proxy.Api.forgotPasswordQuestion, params: { email }, showError: true })
  if (!result) return false
  securityQuestion.value = result.data
  return true
}

// 登录、注册、重置密码  提交表单
const doSubmit = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    if (opType.value === 0 && ((formData.value.securityQuestion && !formData.value.securityAnswer) || (!formData.value.securityQuestion && formData.value.securityAnswer))) {
      proxy.Message.warning('请同时填写密保问题和答案，或都留空')
      return
    }
    if (opType.value === 2 && !securityQuestion.value) {
      await loadSecurityQuestion()
      return
    }
    let params = {}
    Object.assign(params, formData.value)
    params.checkCodeKey = checkCodeInfo.value.checkCodeKey
    let result = await proxy.Request({
      url: opType.value == 0 ? proxy.Api.register : opType.value === 2 ? proxy.Api.forgotPasswordReset : proxy.Api.login,
      params: params,
      errorCallback: () => {
        changeCheckCode()
      },
    })
    if (!result) {
      return
    }
    //注册返回
    if (opType.value == 0) {
      proxy.Message.success('注册成功,请登录')
      showPanel(1)
    } else if (opType.value === 2) {
      proxy.Message.success('密码已重置，请使用新密码登录')
      showPanel(1)
    } else if (opType.value == 1) {
      proxy.Message.success('登录成功')
      localStorage.setItem('token', result.data.token)
      loginStore.saveUserInfo(result.data)
      loginStore.showLogin = false

      // 登录可能发生在任意页面（社区、邀请页或个人主页）。这些页面
      // 的数据通常是在未登录状态下加载的，因此登录成功后统一刷新，
      // 让 Layout 重新执行 autoLogin 并按登录态重新拉取页面数据。
      window.location.reload()
    }
  })
}

const closeDialog = () => {
  dialogConfig.value.show = false
  loginStore.showLogin = false
}

const showPanel = (type) => {
  opType.value = type
  if (loginStore.showLogin) {
    resetForm()
  }
}

watch(
  () => loginStore.showLogin,
  (visible) => {
    if (visible) showPanel(1)
  },
)

onMounted(() => {
  showPanel(1)
})
</script>

<style lang="scss">
.dialog-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  min-height: 420px;
}
.welcome-panel {
  padding: 54px 48px;
  color: #d8e9e5;
  background: var(--zt-primary);
}
.welcome-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  margin-bottom: 32px;
  border: 1px solid rgba(216, 233, 229, 0.45);
  border-radius: 8px;
  color: #fff;
  font-size: 19px;
  font-weight: 800;
}
.welcome-overline {
  margin: 0 0 10px;
  color: #a8cfca;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.5px;
}
.welcome-panel h2 {
  margin: 0 0 13px;
  color: #fff;
  font-size: 29px;
}
.welcome-panel > p:not(.welcome-overline) {
  max-width: 290px;
  margin: 0;
  color: #c6dfdb;
  font-size: 14px;
  line-height: 1.8;
}
.welcome-panel ul {
  display: grid;
  gap: 10px;
  margin: 30px 0 0;
  padding: 18px 0 0;
  border-top: 1px solid rgba(216, 233, 229, 0.22);
  list-style: none;
}
.welcome-panel li {
  color: #d8e9e5;
  font-size: 12px;
}
.welcome-panel li::before {
  margin-right: 8px;
  color: #92c6bb;
  content: '~';
}
.login-register {
  align-self: center;
  width: auto;
  padding: 38px 34px;
}
.login-register .tab-panel {
  display: grid;
  gap: 6px;
  margin: 0 0 25px;
  color: var(--zt-text);
  font-size: 20px;
  font-weight: 750;
}
.login-register .tab-panel small {
  color: var(--zt-text-3);
  font-size: 12px;
  font-weight: 400;
}
.login-register .login-btn {
  width: 100%;
}
.login-register .bottom-btn {
  margin-bottom: 0;
}
.login-register .bottom-btn .op-link {
  width: 100%;
  margin-top: 4px;
  color: var(--zt-primary);
  cursor: pointer;
  font-size: 13px;
  text-align: right;
}
.login-register .op-link.secondary {
  margin-top: 9px;
  color: var(--zt-text-3);
  font-size: 11px;
}
.security-hint {
  display: grid;
  gap: 3px;
  margin: 8px 0 8px;
  color: var(--zt-text-2);
  font-size: 12px;
  font-weight: 650;
}
.security-hint small {
  color: var(--zt-text-3);
  font-size: 10px;
  font-weight: 400;
}
.security-question {
  margin-bottom: 8px;
  padding: 9px 11px;
  border-left: 3px solid var(--zt-accent);
  color: var(--zt-text-2);
  background: var(--zt-accent-soft);
  font-size: 12px;
  line-height: 1.5;
}

.check-code-panel {
  display: flex;

  .check-code {
    margin-left: 5px;
    cursor: pointer;
  }
}
@media (max-width: 760px) {
  .dialog-panel {
    grid-template-columns: 1fr;
  }
  .welcome-panel {
    display: none;
  }
  .login-register {
    padding: 30px 24px;
  }
}
</style>
