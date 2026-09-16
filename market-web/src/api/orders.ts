import request from '../utils/request'
import { ORDER_MODULE } from './_prefix'

const ADMIN_ORDER_MODULE = '/admin/order'

export interface OrderCreateInfo {
  idempotentKey: string
  addressId: number
  userCouponId?: number | null
}

export interface DirectBuyInfo {
  idempotentKey: string
  bookId: number
  quantity: number
  addressId: number
  userCouponId?: number | null
}

export const getOrders = (params: { page?: number; size?: number } = {}) => {
  return request.get(`${ORDER_MODULE}/page`, { params })
}

export const getOrderDetail = (orderId: number) => {
  return request.get(`${ORDER_MODULE}/${orderId}`)
}

export const createOrder = (info: OrderCreateInfo) => {
  return request.post(`${ORDER_MODULE}/create`, info)
}

export const directBuy = (info: DirectBuyInfo) => {
  return request.post(`${ORDER_MODULE}/direct-buy`, info)
}

export const cancelOrder = (orderId: number) => {
  return request.put(`${ORDER_MODULE}/${orderId}/cancel`)
}

export const getAllOrdersByAdmin = (params: { page?: number; size?: number; status?: number } = {}) => {
  return request.get(`${ADMIN_ORDER_MODULE}/page`, { params })
}

export const updateOrderStatusByAdmin = (orderId: number, status: number) => {
  return request.put(`${ADMIN_ORDER_MODULE}/${orderId}/status`, { status })
}

export const getAdminOrderDetail = (orderId: number) => {
  return request.get(`${ADMIN_ORDER_MODULE}/${orderId}`)
}
