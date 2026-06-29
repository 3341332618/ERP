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

export function createDocument(type: string, data: Record<string, string> = {}) {
  return http.post(`/documents/${type}`, data) as Promise<any>
}

export function updateDocument(type: string, id: number, data: Record<string, string> = {}) {
  return http.put(`/documents/${type}/${id}`, data) as Promise<any>
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

export function settlementDetail(direction: string, id: number) {
  return http.get(`/settlement/${direction}/${id}`) as Promise<any>
}

export function importProducts(rows: Record<string, string>[]) {
  return http.post('/masterdata/product/import', rows) as Promise<any[]>
}

export interface BugDefinition {
  id: string
  roleName: string
  moduleName: string
  functionName: string
  summary: string
  reproduceSteps: string
  expectedResult: string
  actualResult: string
  severity: string
  active: boolean
  publisherName?: string
  publishTime?: string
}

export interface BugReport {
  id: number
  bugId: string
  bugSummary: string
  moduleName: string
  title: string
  reproduceSteps: string
  expectedResult: string
  actualResult: string
  evidence: string
  studentName: string
  status: string
  score: number
  reviewComment: string
  submitTime: string
  reviewTime?: string
}

export interface StudentAccount {
  id: number
  username: string
  name: string
  phone: string
  status: string
  createTime: string
}

export interface CompetitionFileSubmission {
  id: number
  title: string
  moduleName: string
  bugId: string
  fileName: string
  contentType: string
  fileSize: number
  storagePath: string
  studentId: number
  studentName: string
  status: string
  score: number
  roundName: string
  reviewComment: string
  reviewerName?: string
  submitTime: string
  reviewTime?: string
}

export interface RankingHistory {
  id: number
  roundName: string
  studentId: number
  studentName: string
  totalScore: number
  approvedReports: number
  approvedFiles: number
  createTime: string
}

export interface StudentOperationLog {
  id: number
  studentId: number
  studentName: string
  moduleName: string
  actionName: string
  detail: string
  createTime: string
}

export function listBugDefinitions() {
  return http.get('/competition/bugs') as Promise<BugDefinition[]>
}

export function listStudents() {
  return http.get('/competition/students') as Promise<StudentAccount[]>
}

export function createStudent(data: { username: string; name: string; phone: string; password: string }) {
  return http.post('/competition/students', data) as Promise<StudentAccount>
}

export function deleteStudent(id: number) {
  return http.delete(`/competition/students/${id}`)
}

export function publishBug(id: string, active: boolean) {
  return http.patch(`/competition/bugs/${id}/publish`, { active }) as Promise<BugDefinition>
}

export function listCompetitionTasks() {
  return http.get('/competition/tasks') as Promise<BugDefinition[]>
}

export function listBugReports() {
  return http.get('/competition/reports') as Promise<BugReport[]>
}

export function submitBugReport(data: Record<string, string>) {
  return http.post('/competition/reports', data) as Promise<BugReport>
}

export function reviewBugReport(id: number, data: { status: string; score: number; reviewComment: string }) {
  return http.patch(`/competition/reports/${id}/review`, data) as Promise<BugReport>
}

export function listBugRankings() {
  return http.get('/competition/rankings') as Promise<Array<Record<string, any>>>
}

export function listCompetitionFiles() {
  return http.get('/competition/files') as Promise<CompetitionFileSubmission[]>
}

export function uploadCompetitionFile(data: { title: string; moduleName: string; bugId?: string; file: File }) {
  const formData = new FormData()
  formData.append('title', data.title)
  formData.append('moduleName', data.moduleName)
  formData.append('bugId', data.bugId || '')
  formData.append('file', data.file)
  return http.post('/competition/files', formData) as Promise<CompetitionFileSubmission>
}

export function reviewCompetitionFile(id: number, data: { status: string; score: number; roundName: string; reviewComment: string }) {
  return http.patch(`/competition/files/${id}/review`, data) as Promise<CompetitionFileSubmission>
}

export function listStudentOperationLogs(studentId?: number) {
  return http.get('/competition/logs', { params: { studentId } }) as Promise<StudentOperationLog[]>
}

export function listRankingHistory() {
  return http.get('/competition/history') as Promise<RankingHistory[]>
}
