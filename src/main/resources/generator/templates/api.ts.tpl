import request from '@/utils/http'
import type { #(entityName)Form, #(entityName)Query } from './type'
#if(isTree)

/** #(functionName)树 */
export const fetch#(entityName)Tree = () =>
  request.get({ url: '#(apiUrl)/tree' })
#end

/** #(functionName)分页 */
export const fetch#(entityName)Page = (params: #(entityName)Query) =>
  request.get({ url: '#(apiUrl)/page', params })

/** #(functionName)详情 */
export const fetch#(entityName)Detail = (id: number | string) =>
  request.get({ url: '#(apiUrl)/detail', params: { id } })

/** 保存#(functionName)（新增/编辑） */
export const fetchSave#(entityName) = (data: #(entityName)Form) =>
  request.post({ url: '#(apiUrl)/submit', data })

/** 删除#(functionName) */
export const fetchRemove#(entityName) = (ids: (number | string)[]) =>
  request.post({ url: '#(apiUrl)/remove', data: ids })
