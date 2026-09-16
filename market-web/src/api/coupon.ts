import request from '../utils/request'
import { COUPON_MODULE } from './_prefix'

const ADMIN_COUPON_MODULE = '/admin/coupon'

export interface CouponTemplateInfo {
  id?: number
  name: string
  type: number
  thresholdAmount?: number
  discountAmount?: number
  discountRate?: number
  totalCount: number
  perUserLimit?: number
  startTime?: string
  endTime?: string
}

export const getAllCoupons = () => {
  return request.get(`${COUPON_MODULE}/template/list`)
}

export const getPublicCoupons = () => {
  return request.get(`${COUPON_MODULE}/template/list`)
}

export const getUserCouponList = () => {
  return request.get(`${COUPON_MODULE}/my`)
}

export const acquireCoupon = (templateId: number) => {
  return request.post(`${COUPON_MODULE}/claim/${templateId}`)
}

export const previewCoupon = (userCouponId: number, orderAmount: number) => {
  return request.post(`${COUPON_MODULE}/preview`, { userCouponId, orderAmount })
}

export const bestFitCoupons = (orderAmount: number) => {
  return request.get(`${COUPON_MODULE}/best-fit`, { params: { orderAmount } })
}

export const createCoupon = (info: CouponTemplateInfo) => {
  return request.post(`${ADMIN_COUPON_MODULE}`, info)
}

export const getAdminCoupons = (params: { page?: number; size?: number } = {}) => {
  return request.get(`${ADMIN_COUPON_MODULE}/page`, { params })
}

export const updateCouponStatus = (id: number, status: number) => {
  return request.put(`${ADMIN_COUPON_MODULE}/${id}/status`, { status })
}

export const updateCoupon = (id: number, info: CouponTemplateInfo) => {
  return request.put(`${ADMIN_COUPON_MODULE}/${id}`, info)
}

export const deleteCoupon = (id: number) => {
  return request.delete(`${ADMIN_COUPON_MODULE}/${id}`)
}
