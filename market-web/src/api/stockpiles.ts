import request from '../utils/request'
import { STOCKPILE_MODULE } from './_prefix'

const ADMIN_STOCKPILE_MODULE = '/admin/inventory'

export interface Inventory {
  id?: number
  bookId: number
  stock: number
  lockedStock?: number
  version?: number
}

export const getStockPile = (bookId: number) => {
  return request.get(`${STOCKPILE_MODULE}/${bookId}`)
}

export const getAdminStock = (bookId: number) => {
  return request.get(`${ADMIN_STOCKPILE_MODULE}/${bookId}`)
}

export const updateStockPile = (bookId: number, stock: number) => {
  return request.put(`${ADMIN_STOCKPILE_MODULE}/${bookId}`, { stock })
}
