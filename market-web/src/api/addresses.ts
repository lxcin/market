import request from '../utils/request'
import { ADDRESS_MODULE } from './_prefix'

export interface Address {
  id?: number
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: number
}

export const getAddressList = () => {
  return request.get(`${ADDRESS_MODULE}/list`)
}

export const addAddress = (address: Address) => {
  return request.post(`${ADDRESS_MODULE}`, address)
}

export const updateAddress = (id: number, address: Address) => {
  return request.put(`${ADDRESS_MODULE}/${id}`, address)
}

export const deleteAddress = (id: number) => {
  return request.delete(`${ADDRESS_MODULE}/${id}`)
}

export const setDefaultAddress = (id: number) => {
  return request.put(`${ADDRESS_MODULE}/${id}/default`)
}
