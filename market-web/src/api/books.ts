import request from '../utils/request'
import { BOOK_MODULE } from './_prefix'

const ADMIN_BOOK_MODULE = '/admin/book'

export interface Book {
  id?: number
  title: string
  author: string
  isbn?: string
  publisher?: string
  publishDate?: string
  categoryId: number
  price: number
  rate?: number
  coverImage?: string
  description?: string
  detail?: string
  sales?: number
  status?: number
}

export interface BookPageParams {
  page?: number
  size?: number
  categoryId?: number
  keyword?: string
}

export const getAllBooks = () => {
  return request.get(`${BOOK_MODULE}/page`, { params: { page: 1, size: 1000 } })
}

export const getBooks = (params: BookPageParams) => {
  return request.get(`${BOOK_MODULE}/page`, { params })
}

export const searchBooks = (keyword: string, limit = 20) => {
  return request.get(`${BOOK_MODULE}/search`, { params: { keyword, limit } })
}

export const searchRich = (params: {
  keyword?: string
  field?: string
  categoryId?: number
  author?: string
  minPrice?: number
  maxPrice?: number
  sort?: string
  page?: number
  size?: number
}) => {
  return request.get(`${BOOK_MODULE}/search/rich`, { params })
}

export const getBook = (id: number) => {
  return request.get(`${BOOK_MODULE}/${id}`)
}

export const getAdminBooks = (params: {
  page?: number
  size?: number
  keyword?: string
  categoryId?: number
  status?: number
}) => {
  return request.get(`${ADMIN_BOOK_MODULE}/page`, { params })
}

export const getHotBooks = (limit = 10) => {
  return request.get(`${BOOK_MODULE}/hot`, { params: { limit } })
}

export const getTopRatedBooks = (limit = 10) => {
  return request.get(`${BOOK_MODULE}/top-rated`, { params: { limit } })
}

export const getAuthorBooks = (name: string, page = 1, size = 12) => {
  return request.get(`${BOOK_MODULE}/author`, { params: { name, page, size } })
}

export const getSuggest = (prefix: string, limit = 10) => {
  return request.get(`${BOOK_MODULE}/suggest`, { params: { prefix, limit } })
}

export const addBook = (book: Book) => {
  return request.post(`${ADMIN_BOOK_MODULE}`, book)
}

export const updateBook = (id: number, book: Book) => {
  return request.put(`${ADMIN_BOOK_MODULE}/${id}`, book)
}

export const deleteBook = (id: number) => {
  return request.delete(`${ADMIN_BOOK_MODULE}/${id}`)
}

export const updateBookStatus = (id: number, status: number) => {
  return request.put(`${ADMIN_BOOK_MODULE}/${id}/status`, { status })
}
