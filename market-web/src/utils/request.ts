import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const body = response.data
    return {
      data: {
        code: String(body.code || 200),
        msg: body.message || 'success',
        data: body.data
      }
    }
  },
  error => {
    const msg = error.response?.data?.message || error.message || 'Request failed'
    ElMessage.error(msg)
    if (error.response?.status === 401) {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('role')
      window.location.hash = '#/login'
    }
    return Promise.reject(error)
  }
)

export default request
