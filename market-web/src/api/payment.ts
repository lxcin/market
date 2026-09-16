import request from '../utils/request'

export interface PaymentInfo {
  orderId: number
  orderStatus?: number
  paymentNo?: string
  amount?: number
  channel?: string
  status?: number
  tradeNo?: string
  sandboxUrl?: string
}

// 统一下单：创建支付单
export const createPayment = (orderId: number) => {
  return request.post(`/payment/create/${orderId}`)
}

// 查询支付状态
export const getPayment = (orderId: number) => {
  return request.get(`/payment/${orderId}`)
}

// 申请退款
export const refundOrder = (orderId: number, reason?: string) => {
  return request.post(`/payment/refund/order/${orderId}`, null, { params: { reason } })
}

// 沙箱：模拟用户完成支付（触发网关回调）
export const sandboxPay = (paymentNo: string) => {
  return request.post(`/payment/sandbox/${paymentNo}/pay`)
}

// 手动对账（管理员）
export const reconcilePayments = (days = 1) => {
  return request.get('/payment/reconcile', { params: { days } })
}
