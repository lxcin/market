import request from '../utils/request'
import { CATEGORY_MODULE } from './_prefix'

const ADMIN_CATEGORY_MODULE = '/admin/category'

export interface Category {
  id?: number
  parentId?: number
  name: string
  sort: number
  children?: Category[]
}

export const getCategoryTree = () => {
  return request.get(`${CATEGORY_MODULE}/tree`)
}

export const getAdminCategoryTree = () => {
  return request.get(`${ADMIN_CATEGORY_MODULE}/tree`)
}

export const createCategory = (data: Category) => {
  return request.post(`${ADMIN_CATEGORY_MODULE}`, data)
}

export const updateCategory = (id: number, data: Category) => {
  return request.put(`${ADMIN_CATEGORY_MODULE}/${id}`, data)
}

export const deleteCategory = (id: number) => {
  return request.delete(`${ADMIN_CATEGORY_MODULE}/${id}`)
}
