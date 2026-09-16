import request from '../utils/request'
import { RECOMMEND_MODULE, CATEGORY_MODULE } from './_prefix'

export interface Book {
  id: number
  title: string
  author: string
  price: number
  rate: number
  description?: string
  coverImage?: string
  detail?: string
  categoryId?: number
  sales?: number
}

export const getTopRatedBooks = (limit = 10) => {
  return request.get(`${RECOMMEND_MODULE}/top-rated`, { params: { limit } })
}

export const getHotBooks = (limit = 10) => {
  return request.get(`${RECOMMEND_MODULE}/hot`, { params: { limit } })
}

export const getSubjectList = () => {
  return request.get(`${CATEGORY_MODULE}/tree`)
}

export const getListBySubject = (categoryId: number) => {
  return request.get(`${RECOMMEND_MODULE}/page`, { params: { categoryId, page: 1, size: 10 } })
}
