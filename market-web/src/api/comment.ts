import request from '../utils/request'
import { COMMENT_MODULE } from './_prefix'

export interface Comment {
  id?: number
  bookId: number
  userId?: number
  username?: string
  content: string
  rate?: number
  parentId?: number | null
  createdAt?: string
  replies?: Comment[]
}

export const getCommentsByBook = (bookId: number) => {
  return request.get(`${COMMENT_MODULE}/book/${bookId}`)
}

export const getMyComments = () => {
  return request.get(`${COMMENT_MODULE}/my`)
}

export const getCommentReplies = (parentId: number) => {
  return request.get(`${COMMENT_MODULE}/${parentId}/replies`)
}

export const addComment = (comment: Comment) => {
  return request.post(`${COMMENT_MODULE}`, comment)
}

export const deleteComment = (id: number) => {
  return request.delete(`${COMMENT_MODULE}/${id}`)
}
