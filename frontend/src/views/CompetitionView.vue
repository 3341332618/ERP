<template>
  <div class="page">
    <div class="table-panel">
      <div class="competition-header">
        <div>
          <h2>{{ pageTitle }}</h2>
          <div class="competition-meta">
            <span>已发布 {{ activeCount }} 条</span>
            <span>报告 {{ reports.length }} 条</span>
            <span>文件 {{ files.length }} 份</span>
            <span>学员 {{ studentCount }} 人</span>
          </div>
        </div>
        <div class="header-actions">
          <el-button v-if="isStudentManagePage" type="primary" @click="openStudentDialog">
            <el-icon><Plus /></el-icon>
            新增学员
          </el-button>
          <el-button v-if="isStudentReportPage" type="primary" @click="openSubmit()">
            <el-icon><EditPen /></el-icon>
            提交缺陷报告
          </el-button>
          <el-button v-if="isStudentFilePage" type="primary" @click="openUploadFile()">
            <el-icon><UploadFilled /></el-icon>
            提交测试文件
          </el-button>
          <el-button @click="load">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <template v-if="isBugPage">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="请输入缺陷编号/摘要查询" clearable style="width: 240px" />
          <el-select v-model="query.moduleName" placeholder="请选择模块名称" clearable style="width: 170px">
            <el-option v-for="item in moduleOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="query.severity" placeholder="请选择严重程度" clearable style="width: 150px">
            <el-option v-for="item in severityOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="query.active" placeholder="请选择发布状态" clearable style="width: 150px">
            <el-option label="已发布" value="true" />
            <el-option label="未发布" value="false" />
          </el-select>
        </div>
        <el-table :data="filteredBugs" border empty-text="暂无缺陷数据">
          <el-table-column prop="id" label="缺陷编号" width="110" />
          <el-table-column prop="roleName" label="角色" width="110" />
          <el-table-column prop="moduleName" label="模块名称" width="120" />
          <el-table-column prop="functionName" label="功能项" width="120" />
          <el-table-column prop="summary" label="摘要描述" min-width="260" show-overflow-tooltip />
          <el-table-column prop="severity" label="严重程度" width="100">
            <template #default="{ row }">
              <el-tag :type="severityType(row.severity)">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '已发布' : '未发布' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="publisherName" label="发布人" width="120" />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button text :type="row.active ? 'warning' : 'primary'" @click="togglePublish(row)">
                {{ row.active ? '取消发布' : '发布缺陷' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-if="isStudentManagePage">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="请输入学员账号/姓名查询" clearable style="width: 260px" />
        </div>
        <el-table :data="filteredStudents" border empty-text="暂无学员数据">
          <el-table-column type="index" label="序号" width="70" />
          <el-table-column prop="username" label="学员账号" min-width="150" />
          <el-table-column prop="name" label="学员姓名" min-width="150" />
          <el-table-column prop="phone" label="联系电话" width="150" />
          <el-table-column label="账号状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="170" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button text type="danger" @click="removeStudent(row)">删除学员</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-if="isTaskPage">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="请输入缺陷编号/摘要查询" clearable style="width: 260px" />
          <el-select v-model="query.moduleName" placeholder="请选择模块名称" clearable style="width: 180px">
            <el-option v-for="item in taskModuleOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </div>
        <el-table :data="filteredTasks" border empty-text="暂无测试任务">
          <el-table-column prop="id" label="缺陷编号" width="110" />
          <el-table-column prop="moduleName" label="模块名称" width="120" />
          <el-table-column prop="functionName" label="功能项" width="120" />
          <el-table-column prop="summary" label="摘要描述" min-width="260" show-overflow-tooltip />
          <el-table-column prop="severity" label="严重程度" width="100">
            <template #default="{ row }">
              <el-tag :type="severityType(row.severity)">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openSubmit(row)">提交报告</el-button>
              <el-button text type="success" @click="openUploadFile(row)">提交文件</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-if="isReportPage">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="请输入报告标题/缺陷编号查询" clearable style="width: 260px" />
          <el-select v-model="query.status" placeholder="请选择评分状态" clearable style="width: 160px">
            <el-option label="待评分" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </div>
        <el-table :data="filteredReports" border empty-text="暂无缺陷报告">
          <el-table-column prop="bugId" label="缺陷编号" width="110" />
          <el-table-column prop="title" label="报告标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="moduleName" label="模块名称" width="120" />
          <el-table-column prop="studentName" label="测试学员" width="120" />
          <el-table-column label="评分状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="90" />
          <el-table-column prop="submitTime" label="提交时间" min-width="170" />
          <el-table-column prop="reviewComment" label="评语" min-width="180" show-overflow-tooltip />
          <el-table-column v-if="isAdminReportPage" label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openReview(row)">评分</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-if="isFilePage">
        <div v-if="isSubmitFilePage" class="upload-panel">
          <div>
            <strong>提交PDF、Word、Excel测试材料</strong>
            <span>用于记录抓包、截图、复现步骤、测试结论和竞赛文档。</span>
          </div>
          <el-button type="primary" @click="openUploadFile">
            <el-icon><UploadFilled /></el-icon>
            选择文件提交
          </el-button>
        </div>
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="请输入标题/文件名/学员查询" clearable style="width: 280px" />
          <el-select v-model="query.status" placeholder="请选择评分状态" clearable style="width: 160px">
            <el-option label="待评分" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </div>
        <el-table :data="filteredFiles" border empty-text="暂无测试文件">
          <el-table-column prop="title" label="提交标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="moduleName" label="测试模块" width="130" />
          <el-table-column prop="studentName" label="测试学员" width="120" />
          <el-table-column prop="fileName" label="文件名称" min-width="200" show-overflow-tooltip />
          <el-table-column label="文件大小" width="110">
            <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column label="评分状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="得分" width="90" />
          <el-table-column prop="roundName" label="评分轮次" width="120" />
          <el-table-column prop="submitTime" label="提交时间" min-width="170" />
          <el-table-column prop="reviewComment" label="评语" min-width="180" show-overflow-tooltip />
          <el-table-column v-if="isFileReviewPage" label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openFileReview(row)">评分</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-if="isLogPage">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="请输入模块/动作/详情查询" clearable style="width: 280px" />
          <el-select v-model="query.studentId" placeholder="请选择学员" clearable style="width: 180px" @change="loadLogs">
            <el-option v-for="item in students" :key="item.id" :label="item.name" :value="String(item.id)" />
          </el-select>
        </div>
        <el-table :data="filteredLogs" border empty-text="暂无操作轨迹">
          <el-table-column prop="studentName" label="测试学员" width="130" />
          <el-table-column prop="moduleName" label="测试模块" width="140" />
          <el-table-column prop="actionName" label="操作动作" width="140" />
          <el-table-column prop="detail" label="操作详情" min-width="260" show-overflow-tooltip />
          <el-table-column prop="createTime" label="记录时间" min-width="170" />
        </el-table>
      </template>

      <template v-if="isHistoryPage">
        <el-table :data="historyRows" border empty-text="暂无评分历史">
          <el-table-column prop="roundName" label="评分轮次" width="130" />
          <el-table-column prop="studentName" label="测试学员" min-width="160" />
          <el-table-column prop="approvedReports" label="通过报告数" width="130" />
          <el-table-column prop="approvedFiles" label="通过文件数" width="130" />
          <el-table-column prop="totalScore" label="历史总分" width="120" />
          <el-table-column prop="createTime" label="生成时间" min-width="170" />
        </el-table>
      </template>

      <template v-if="isRankingPage">
        <el-table :data="rankings" border empty-text="暂无排行榜数据">
          <el-table-column type="index" label="排名" width="80" />
          <el-table-column prop="studentName" label="测试学员" min-width="160" />
          <el-table-column prop="approvedReports" label="通过报告数" width="130" />
          <el-table-column prop="approvedFiles" label="通过文件数" width="130" />
          <el-table-column prop="totalScore" label="总分" width="110" />
        </el-table>
      </template>
    </div>

    <el-dialog v-model="studentDialogVisible" title="新增学员" width="520px">
      <el-form label-width="100px" :model="studentForm">
        <el-form-item label="学员账号"><el-input v-model="studentForm.username" placeholder="请输入学员账号" /></el-form-item>
        <el-form-item label="学员姓名"><el-input v-model="studentForm.name" placeholder="请输入学员姓名" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="studentForm.phone" placeholder="请输入联系电话" /></el-form-item>
        <el-form-item label="登录密码">
          <el-input v-model="studentForm.password" placeholder="为空时使用默认密码123456" show-password />
        </el-form-item>
        <div class="dialog-tip">默认密码为123456，管理员可按教学批次统一发放账号。</div>
      </el-form>
      <template #footer>
        <el-button @click="studentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStudent">保存学员</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDialogVisible" title="提交缺陷报告" width="720px">
      <el-form label-width="110px" :model="reportForm">
        <el-form-item label="缺陷任务">
          <el-select v-model="reportForm.bugId" filterable placeholder="请选择缺陷任务" @change="fillBugForm">
            <el-option v-for="item in tasks" :key="item.id" :label="`${item.id} ${item.summary}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告标题"><el-input v-model="reportForm.title" placeholder="请输入报告标题" /></el-form-item>
        <el-form-item label="模块名称"><el-input v-model="reportForm.moduleName" placeholder="请输入模块名称" /></el-form-item>
        <el-form-item label="复现步骤"><el-input v-model="reportForm.reproduceSteps" type="textarea" :rows="4" placeholder="请输入复现步骤" /></el-form-item>
        <el-form-item label="预期结果"><el-input v-model="reportForm.expectedResult" type="textarea" :rows="2" placeholder="请输入预期结果" /></el-form-item>
        <el-form-item label="实际结果"><el-input v-model="reportForm.actualResult" type="textarea" :rows="2" placeholder="请输入实际结果" /></el-form-item>
        <el-form-item label="证据说明"><el-input v-model="reportForm.evidence" type="textarea" :rows="2" placeholder="请输入截图、抓包或日志说明" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveReport">提交缺陷报告</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadDialogVisible" title="提交测试文件" width="620px">
      <el-form label-width="100px" :model="uploadForm">
        <el-form-item label="提交标题"><el-input v-model="uploadForm.title" placeholder="请输入提交标题" /></el-form-item>
        <el-form-item label="测试模块">
          <el-select v-model="uploadForm.moduleName" filterable allow-create default-first-option placeholder="请选择或输入测试模块">
            <el-option v-for="item in uploadModuleOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联缺陷">
          <el-select v-model="uploadForm.bugId" filterable clearable placeholder="可选">
            <el-option v-for="item in tasks" :key="item.id" :label="`${item.id} ${item.summary}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="测试文件">
          <input class="file-input" type="file" accept=".pdf,.doc,.docx,.xls,.xlsx" @change="selectUploadFile" />
          <div class="dialog-tip">仅支持PDF、Word、Excel文件。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCompetitionFile">提交文件</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" title="学员报告评分" width="560px">
      <el-form label-width="100px" :model="reviewForm">
        <el-form-item label="评分状态">
          <el-select v-model="reviewForm.status">
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="得分">
          <el-input-number v-model="reviewForm.score" :min="0" :max="100" :step="5" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="4" placeholder="请输入评分评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveReview">保存评分</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fileReviewDialogVisible" title="测试文件评分" width="560px">
      <el-form label-width="100px" :model="fileReviewForm">
        <el-form-item label="评分状态">
          <el-select v-model="fileReviewForm.status">
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分轮次"><el-input v-model="fileReviewForm.roundName" placeholder="例如：第1轮" /></el-form-item>
        <el-form-item label="得分">
          <el-input-number v-model="fileReviewForm.score" :min="0" :max="100" :step="5" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="fileReviewForm.reviewComment" type="textarea" :rows="4" placeholder="请输入文件评阅意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fileReviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFileReview">保存评分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Plus, Refresh, UploadFilled } from '@element-plus/icons-vue'
import {
  createStudent,
  deleteStudent,
  listBugDefinitions,
  listBugRankings,
  listBugReports,
  listCompetitionFiles,
  listCompetitionTasks,
  listRankingHistory,
  listStudentOperationLogs,
  listStudents,
  publishBug,
  reviewBugReport,
  reviewCompetitionFile,
  submitBugReport,
  uploadCompetitionFile,
  type BugDefinition,
  type BugReport,
  type CompetitionFileSubmission,
  type RankingHistory,
  type StudentAccount,
  type StudentOperationLog
} from '../api'

const route = useRoute()
const bugs = ref<BugDefinition[]>([])
const tasks = ref<BugDefinition[]>([])
const reports = ref<BugReport[]>([])
const files = ref<CompetitionFileSubmission[]>([])
const logs = ref<StudentOperationLog[]>([])
const historyRows = ref<RankingHistory[]>([])
const rankings = ref<Array<Record<string, any>>>([])
const students = ref<StudentAccount[]>([])
const reportDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const studentDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const fileReviewDialogVisible = ref(false)
const selectedUploadFile = ref<File | null>(null)
const query = reactive({ keyword: '', moduleName: '', severity: '', active: '', status: '', studentId: '' })
const studentForm = reactive({ username: '', name: '', phone: '', password: '' })
const reportForm = reactive<Record<string, string>>({
  bugId: '',
  title: '',
  moduleName: '',
  reproduceSteps: '',
  expectedResult: '',
  actualResult: '',
  evidence: ''
})
const reviewForm = reactive({ id: 0, status: 'APPROVED', score: 80, reviewComment: '' })
const uploadForm = reactive({ title: '', moduleName: '', bugId: '' })
const fileReviewForm = reactive({ id: 0, status: 'APPROVED', score: 80, roundName: '第1轮', reviewComment: '' })

const viewType = computed(() => String(route.params.type || 'bugs'))
const isBugPage = computed(() => viewType.value === 'bugs')
const isStudentManagePage = computed(() => viewType.value === 'students')
const isTaskPage = computed(() => viewType.value === 'tasks')
const isAdminReportPage = computed(() => viewType.value === 'reports')
const isStudentReportPage = computed(() => viewType.value === 'my-reports')
const isReportPage = computed(() => isAdminReportPage.value || isStudentReportPage.value)
const isFileReviewPage = computed(() => viewType.value === 'files')
const isSubmitFilePage = computed(() => viewType.value === 'submit-file')
const isMyFilePage = computed(() => viewType.value === 'my-files')
const isStudentFilePage = computed(() => isSubmitFilePage.value || isMyFilePage.value)
const isFilePage = computed(() => isFileReviewPage.value || isStudentFilePage.value)
const isLogPage = computed(() => viewType.value === 'logs')
const isHistoryPage = computed(() => viewType.value === 'history')
const isRankingPage = computed(() => viewType.value === 'rankings')
const pageTitle = computed(() => ({
  bugs: '缺陷库发布',
  students: '学员管理',
  tasks: '缺陷测试任务',
  reports: '学员报告评分',
  'my-reports': '我的缺陷报告',
  files: '文件评阅',
  'submit-file': '提交测试文件',
  'my-files': '我的提交文件',
  logs: '操作轨迹',
  history: '评分历史',
  rankings: '竞赛排行榜'
} as Record<string, string>)[viewType.value] || '测试竞赛')
const activeCount = computed(() => bugs.value.filter((item) => item.active).length || tasks.value.length)
const studentCount = computed(() => students.value.length || rankings.value.length)
const moduleOptions = computed(() => Array.from(new Set(bugs.value.map((item) => item.moduleName))).filter(Boolean))
const taskModuleOptions = computed(() => Array.from(new Set(tasks.value.map((item) => item.moduleName))).filter(Boolean))
const uploadModuleOptions = computed(() => Array.from(new Set([...tasks.value.map((item) => item.moduleName), ...files.value.map((item) => item.moduleName)])).filter(Boolean))
const severityOptions = computed(() => Array.from(new Set(bugs.value.map((item) => item.severity))).filter(Boolean))
const filteredBugs = computed(() => bugs.value.filter((item) => {
  const keyword = query.keyword.trim()
  const matchKeyword = !keyword || item.id.includes(keyword) || item.summary.includes(keyword)
  const matchModule = !query.moduleName || item.moduleName === query.moduleName
  const matchSeverity = !query.severity || item.severity === query.severity
  const matchActive = !query.active || String(item.active) === query.active
  return matchKeyword && matchModule && matchSeverity && matchActive
}))
const filteredTasks = computed(() => tasks.value.filter((item) => {
  const keyword = query.keyword.trim()
  const matchKeyword = !keyword || item.id.includes(keyword) || item.summary.includes(keyword)
  const matchModule = !query.moduleName || item.moduleName === query.moduleName
  return matchKeyword && matchModule
}))
const filteredReports = computed(() => reports.value.filter((item) => {
  const keyword = query.keyword.trim()
  const matchKeyword = !keyword || item.bugId.includes(keyword) || item.title.includes(keyword)
  const matchStatus = !query.status || item.status === query.status
  return matchKeyword && matchStatus
}))
const filteredStudents = computed(() => students.value.filter((item) => {
  const keyword = query.keyword.trim()
  return !keyword || item.username.includes(keyword) || item.name.includes(keyword)
}))
const filteredFiles = computed(() => files.value.filter((item) => {
  const keyword = query.keyword.trim()
  const matchKeyword = !keyword || item.title.includes(keyword) || item.fileName.includes(keyword) || item.studentName.includes(keyword)
  const matchStatus = !query.status || item.status === query.status
  return matchKeyword && matchStatus
}))
const filteredLogs = computed(() => logs.value.filter((item) => {
  const keyword = query.keyword.trim()
  return !keyword || item.moduleName.includes(keyword) || item.actionName.includes(keyword) || item.detail.includes(keyword)
}))

function severityType(severity: string) {
  return ({ 严重: 'danger', 很高: 'danger', 高: 'warning', 中: 'primary', 低: 'info' } as Record<string, string>)[severity] || 'info'
}

function statusLabel(status: string) {
  return ({ PENDING: '待评分', APPROVED: '已通过', REJECTED: '已驳回' } as Record<string, string>)[status] || '待评分'
}

function statusType(status: string) {
  return ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' } as Record<string, string>)[status] || 'info'
}

function formatFileSize(size: number) {
  if (!size) return '0 KB'
  if (size < 1024 * 1024) return `${Math.ceil(size / 1024)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

async function load() {
  if (isBugPage.value) {
    bugs.value = await listBugDefinitions()
  }
  if (isStudentManagePage.value || isLogPage.value) {
    students.value = await listStudents()
  }
  if (isTaskPage.value || isStudentReportPage.value || isStudentFilePage.value) {
    tasks.value = await listCompetitionTasks()
  }
  if (isReportPage.value) {
    reports.value = await listBugReports()
  }
  if (isFilePage.value) {
    files.value = await listCompetitionFiles()
  }
  if (isLogPage.value) {
    await loadLogs()
  }
  if (isHistoryPage.value) {
    historyRows.value = await listRankingHistory()
  }
  if (isRankingPage.value || isBugPage.value || isReportPage.value || isFilePage.value || isHistoryPage.value) {
    rankings.value = await listBugRankings()
  }
}

async function loadLogs() {
  logs.value = await listStudentOperationLogs(query.studentId ? Number(query.studentId) : undefined)
}

async function togglePublish(row: BugDefinition) {
  await publishBug(row.id, !row.active)
  ElMessage.success(row.active ? '已取消发布' : '发布缺陷成功')
  await load()
}

function openStudentDialog() {
  studentForm.username = ''
  studentForm.name = ''
  studentForm.phone = ''
  studentForm.password = ''
  studentDialogVisible.value = true
}

async function saveStudent() {
  await createStudent(studentForm)
  ElMessage.success('新增学员成功')
  studentDialogVisible.value = false
  await load()
}

async function removeStudent(row: StudentAccount) {
  await ElMessageBox.confirm(`确认删除学员 ${row.name}？删除后该学员账号、报告、文件和轨迹记录将同步移除。`, '删除学员', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteStudent(row.id)
  ElMessage.success('删除学员成功')
  await load()
}

function openSubmit(row?: BugDefinition) {
  const selected = row || tasks.value[0]
  if (!selected) {
    ElMessage.warning('暂无可提交的缺陷测试任务')
    return
  }
  reportForm.bugId = selected.id
  reportForm.title = `发现${selected.moduleName}缺陷`
  reportForm.moduleName = selected.moduleName
  reportForm.reproduceSteps = selected.reproduceSteps
  reportForm.expectedResult = selected.expectedResult
  reportForm.actualResult = selected.actualResult
  reportForm.evidence = ''
  reportDialogVisible.value = true
}

function fillBugForm(id: string) {
  const selected = tasks.value.find((item) => item.id === id)
  if (!selected) return
  reportForm.moduleName = selected.moduleName
  reportForm.title = `发现${selected.moduleName}缺陷`
  reportForm.reproduceSteps = selected.reproduceSteps
  reportForm.expectedResult = selected.expectedResult
  reportForm.actualResult = selected.actualResult
}

async function saveReport() {
  await submitBugReport(reportForm)
  ElMessage.success('缺陷报告提交成功')
  reportDialogVisible.value = false
  await load()
}

function openUploadFile(row?: BugDefinition) {
  selectedUploadFile.value = null
  uploadForm.bugId = row?.id || ''
  uploadForm.moduleName = row?.moduleName || uploadModuleOptions.value[0] || ''
  uploadForm.title = row ? `${row.moduleName}测试材料` : ''
  uploadDialogVisible.value = true
}

function selectUploadFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  selectedUploadFile.value = file || null
  if (file && !uploadForm.title) {
    uploadForm.title = file.name.replace(/\.[^.]+$/, '')
  }
}

async function saveCompetitionFile() {
  if (!selectedUploadFile.value) {
    ElMessage.warning('请先选择PDF、Word或Excel文件')
    return
  }
  await uploadCompetitionFile({
    title: uploadForm.title,
    moduleName: uploadForm.moduleName,
    bugId: uploadForm.bugId,
    file: selectedUploadFile.value
  })
  ElMessage.success('测试文件提交成功')
  uploadDialogVisible.value = false
  await load()
}

function openReview(row: BugReport) {
  reviewForm.id = row.id
  reviewForm.status = row.status === 'REJECTED' ? 'REJECTED' : 'APPROVED'
  reviewForm.score = row.score || 80
  reviewForm.reviewComment = row.reviewComment || ''
  reviewDialogVisible.value = true
}

async function saveReview() {
  await reviewBugReport(reviewForm.id, {
    status: reviewForm.status,
    score: reviewForm.score,
    reviewComment: reviewForm.reviewComment
  })
  ElMessage.success('评分保存成功')
  reviewDialogVisible.value = false
  await load()
}

function openFileReview(row: CompetitionFileSubmission) {
  fileReviewForm.id = row.id
  fileReviewForm.status = row.status === 'REJECTED' ? 'REJECTED' : 'APPROVED'
  fileReviewForm.score = row.score || 80
  fileReviewForm.roundName = row.roundName || '第1轮'
  fileReviewForm.reviewComment = row.reviewComment || ''
  fileReviewDialogVisible.value = true
}

async function saveFileReview() {
  await reviewCompetitionFile(fileReviewForm.id, {
    status: fileReviewForm.status,
    score: fileReviewForm.score,
    roundName: fileReviewForm.roundName,
    reviewComment: fileReviewForm.reviewComment
  })
  ElMessage.success('文件评分保存成功')
  fileReviewDialogVisible.value = false
  await load()
}

watch(viewType, () => {
  query.keyword = ''
  query.moduleName = ''
  query.severity = ''
  query.active = ''
  query.status = ''
  query.studentId = ''
  load()
})
onMounted(load)
</script>

<style scoped>
.competition-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.competition-header h2 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #1f2d3d;
}

.competition-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #606266;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #f7f9fc;
}

.upload-panel strong {
  display: block;
  margin-bottom: 6px;
  color: #1f2d3d;
}

.upload-panel span {
  color: #606266;
  font-size: 13px;
}

.file-input {
  width: 100%;
  padding: 8px 0;
}
</style>
