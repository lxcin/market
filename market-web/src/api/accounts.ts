import request from '../utils/request'

export const getCaptcha = () => {
  return request.get('/user/captcha')
}

export const loginAccount = (data: {
  username: string
  password: string
  captchaId: string
  captchaCode: string
}) => {
  return request.post('/user/login', data)
}

export const registerAccount = (data: {
  username: string
  password: string
  email: string
  phone: string
}) => {
  return request.post('/user/register', data)
}

export const sendSmsCode = (phone: string) => {
  return request.post('/user/sms/send', { phone })
}

export const resetPassword = (data: { phone: string; code: string; newPassword: string }) => {
  return request.post('/user/password/reset', data)
}

export const getAccount = () => {
  return request.get('/user/info')
}

export const logout = () => {
  return request.post('/user/logout')
}
