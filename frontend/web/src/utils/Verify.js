const regs = {
  email: /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/,
  number: /^\+?[1-9][0-9]*$/,
  password: /^(?=.*\d)(?=.*[a-zA-Z])[\da-zA-Z~!@#$%^&*_]{8,18}$/, //至少1个数字1个字母，允许数字，字母，特殊字符 8-18位
  amount: /^\d+(\.\d{1,2})?$/,
}
const verify = (rule, value, reg, callback) => {
  if (value) {
    if (reg.test(value)) {
      callback()
    } else {
      callback(new Error(rule.message))
    }
  } else {
    callback()
  }
}

const checkPassword = (value) => {
  return regs.password.test(value)
}

const checkEmail = (value) => {
  return regs.email.test(value)
}

const password = (rule, value, callback) => {
  return verify(rule, value, regs.password, callback)
}

const number = (rule, value, callback) => {
  return verify(rule, value, regs.number, callback)
}

const checkAmount = (value) => {
  return regs.amount.test(value)
}

const email = (rule, value, callback) => {
  return verify(rule, value, regs.email, callback)
}

export default {
  checkPassword,
  checkEmail,
  password,
  number,
  email,
  checkAmount,
}
