import { http } from './http'

export interface MenuNode {
  title: string
  path: string
  children: MenuNode[]
}

export interface CurrentUser {
  id: number
  username: string
  name: string
  phone: string
  avatar: string
  role: string
  roleName: string
  createTime: string
}

export function login(data: { username: string; password: string }) {
  return http.post('/auth/login', data) as Promise<{ token: string; user: CurrentUser; menus: MenuNode[] }>
}

export function currentUser() {
  return http.get('/auth/me') as Promise<{ user: CurrentUser; menus: MenuNode[] }>
}

export function changePassword(data: { oldPassword: string; newPassword: string; confirmPassword: string }) {
  return http.post('/auth/change-password', data)
}

export function uploadAvatar(avatar: string) {
  return http.post('/auth/avatar', { avatar }) as Promise<CurrentUser>
}

export function fetchMessages() {
  return http.get('/system/messages') as Promise<any[]>
}

export function listMaster(type: string, params: Record<string, string>) {
  return http.get(`/masterdata/${type}`, { params }) as Promise<any[]>
}

export function createMaster(type: string, data: Record<string, string>) {
  return http.post(`/masterdata/${type}`, data)
}

export function updateMaster(type: string, id: number, data: Record<string, string>) {
  return http.put(`/masterdata/${type}/${id}`, data)
}

export function changeMasterStatus(type: string, id: number, status: string) {
  return http.patch(`/masterdata/${type}/${id}/status`, { status })
}

export function listDocuments(type: string) {
  return http.get(`/documents/${type}`) as Promise<any[]>
}

export function createDocument(type: string) {
  return http.post(`/documents/${type}`) as Promise<any>
}

export function submitDocument(type: string, id: number) {
  return http.post(`/documents/${type}/${id}/submit`) as Promise<any>
}

export function deleteDocument(type: string, id: number) {
  return http.delete(`/documents/${type}/${id}`) as Promise<any>
}

export function listStock() {
  return http.get('/inventory/stock') as Promise<any[]>
}

export function listAudit(direction: string) {
  return http.get(`/inventory/audit/${direction}`) as Promise<any[]>
}

export function approveAudit(id: number) {
  return http.post(`/inventory/audit/${id}/approve`)
}

export function rejectAudit(id: number, reason: string) {
  return http.post(`/inventory/audit/${id}/reject`, { reason })
}

export function listSettlement(direction: string) {
  return http.get(`/settlement/${direction}`) as Promise<any[]>
}
