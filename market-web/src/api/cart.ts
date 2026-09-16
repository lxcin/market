import request from '../utils/request'
import { CART_MODULE } from './_prefix'

export interface AddToCartInfo {
  bookId: number
  quantity?: number
}

export const getAllCartItems = () => {
  return request.get(`${CART_MODULE}/list`)
}

export const addProductToCart = (addInfo: AddToCartInfo) => {
  return request.post(`${CART_MODULE}/add`, {
    bookId: addInfo.bookId,
    quantity: addInfo.quantity || 1
  })
}

export const deleteProductFromCart = (bookId: number) => {
  return request.delete(`${CART_MODULE}/${bookId}`)
}

export const updateProductQuantity = (bookId: number, quantity: number) => {
  return request.put(`${CART_MODULE}/quantity`, { bookId, quantity })
}

export const toggleCartItemCheck = (bookId: number) => {
  return request.put(`${CART_MODULE}/check/${bookId}`)
}

export const checkAllCartItems = (checked: boolean) => {
  return request.put(`${CART_MODULE}/check-all`, { checked })
}

export const removeCheckedCartItems = () => {
  return request.delete(`${CART_MODULE}/checked`)
}

export const getCartSummary = () => {
  return request.get(`${CART_MODULE}/summary`)
}
