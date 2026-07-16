import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'

const allowedTechnical = [
  'script',
  'template',
  'style',
  'setup',
  'lang',
  'ts',
  'const',
  'import',
  'from',
  'return',
  'async',
  'await'
]
const legacySalePriceLabel = ['建议', '销售价'].join('')

function files(dir: string): string[] {
  return readdirSync(dir).flatMap((name: string) => {
    const path = join(dir, name)
    return statSync(path).isDirectory() ? files(path) : [path]
  })
}

describe('中文界面文案', () => {
  it('关键页面包含中文业务文案', () => {
    const content = files(join(process.cwd(), 'src'))
      .filter((file) => file.endsWith('.vue') || file.endsWith('.ts'))
      .map((file) => readFileSync(file, 'utf8'))
      .join('\n')

    expect(content).toContain('ERP管理平台')
    expect(content).toContain('采购入库')
    expect(content).toContain('销售出库')
    expect(content).toContain('库存分布')
    expect(content).toContain('收入结算')
    expect(content).toContain('上传头像')
    expect(content).toContain('头像上传成功')
    expect(content).toContain('建议零售价')
    expect(content).toContain('上传文件大小不能超过200KB')
    expect(content).toContain('新增采购入库单')
    expect(content).toContain('新增销售出库单')
    expect(content).toContain('商品明细')
    expect(content).toContain('操作记录')
    expect(content).toContain('批量导入')
    expect(content).toContain('下载模板')
    expect(content).toContain('商品批量导入模板')
    expect(content).toContain('查看仓库')
    expect(content).toContain('仓库明细')
    expect(content).toContain('查看详情')
    expect(content).not.toContain(legacySalePriceLabel)
    expect(allowedTechnical.length).toBeGreaterThan(0)
  })

  it('前端接口覆盖单据修改、商品导入和结算详情', () => {
    const api = readFileSync(join(process.cwd(), 'src/api/index.ts'), 'utf8')

    expect(api).toContain('updateDocument')
    expect(api).toContain('/masterdata/product/import')
    expect(api).toContain('settlementDetail')
    expect(api).toContain('documentDetail')
    expect(api).toContain('listReturnOptions')
    expect(api).toContain('/return-options')
  })

  it('登录态失效时路由返回登录页', () => {
    const router = readFileSync(join(process.cwd(), 'src/router/index.ts'), 'utf8')

    expect(router).toContain('auth.logout()')
    expect(router).toContain("return '/login'")
  })

  it('登录页使用参考登录页的双栏玻璃拟态结构', () => {
    const loginView = readFileSync(join(process.cwd(), 'src/views/LoginView.vue'), 'utf8')

    expect(loginView).toContain('class="login-copy"')
    expect(loginView).toContain('class="login-card__header"')
    expect(loginView).toContain('class="login-field__control"')
    expect(loginView).toContain('class="login-submit"')
  })

  it('登录页使用当前背景图和淡蓝色登录框', () => {
    const loginView = readFileSync(join(process.cwd(), 'src/views/LoginView.vue'), 'utf8')

    expect(loginView).toContain('url("/images/login-current-bg.png")')
    expect(loginView).toContain('--login-panel-bg: rgba(231, 246, 255, 0.96);')
    expect(statSync(join(process.cwd(), 'public/images/login-current-bg.png')).isFile()).toBe(true)
  })

  it('学员 ERP 工作区二次登录通过岗位自动匹配子账号', () => {
    const router = readFileSync(join(process.cwd(), 'src/router/index.ts'), 'utf8')
    const loginView = readFileSync(join(process.cwd(), 'src/views/LoginView.vue'), 'utf8')
    const studentErpLoginView = readFileSync(join(process.cwd(), 'src/views/StudentErpLoginView.vue'), 'utf8')

    expect(router).toContain("path: '/student-erp-login'")
    expect(router).toContain("auth.user?.role !== 'STUDENT'")
    expect(loginView).toContain("auth.user?.role === 'STUDENT'")
    expect(loginView).toContain("router.push('/competition/reports')")
    expect(studentErpLoginView).toContain('我的 ERP 工作区')
    expect(studentErpLoginView).toContain('请选择岗位并输入密码')
    expect(studentErpLoginView).toContain('管理员')
    expect(studentErpLoginView).toContain('采购专员')
    expect(studentErpLoginView).toContain('仓库专员')
    expect(studentErpLoginView).toContain('销售专员')
    expect(studentErpLoginView).toContain('结算主管')
    expect(studentErpLoginView).toContain("suffix: 'admin'")
    expect(studentErpLoginView).toContain("suffix: 'purchase_staff'")
    expect(studentErpLoginView).toContain("suffix: 'warehouse_staff'")
    expect(studentErpLoginView).toContain("suffix: 'sales_staff'")
    expect(studentErpLoginView).toContain("suffix: 'settlement_manager'")
    expect(studentErpLoginView).toContain('targetUsername')
    expect(studentErpLoginView).toContain('`${studentUsername.value}_${form.roleSuffix}`')
    expect(studentErpLoginView).toContain('只能登录当前学员的 ERP 子账号')
    expect(studentErpLoginView).toContain('startsWith(`${studentUsername.value}_`)')
  })

  it('student ERP role logout restores primary student session', () => {
    const authStore = readFileSync(join(process.cwd(), 'src/stores/auth.ts'), 'utf8')
    const appLayout = readFileSync(join(process.cwd(), 'src/layouts/AppLayout.vue'), 'utf8')
    const studentErpLoginView = readFileSync(join(process.cwd(), 'src/views/StudentErpLoginView.vue'), 'utf8')

    expect(authStore).toContain('ERP_PRIMARY_STUDENT_SESSION')
    expect(authStore).toContain('savePrimaryStudentSession')
    expect(authStore).toContain('logoutCurrentIdentity')
    expect(authStore).toContain('restorePrimaryStudentSession')
    expect(studentErpLoginView).toContain('auth.savePrimaryStudentSession()')
    expect(appLayout).toContain('auth.logoutCurrentIdentity()')
    expect(appLayout).toContain("router.push('/competition/my-reports')")
  })

  it('platform admin demo login is removed while student workspace admin role remains', () => {
    const loginView = readFileSync(join(process.cwd(), 'src/views/LoginView.vue'), 'utf8')
    const studentErpLoginView = readFileSync(join(process.cwd(), 'src/views/StudentErpLoginView.vue'), 'utf8')

    expect(loginView).not.toContain("username: 'admin'")
    expect(loginView).toContain("username: 'student01'")
    expect(loginView).toContain("username: 'superadmin'")
    expect(studentErpLoginView).toContain("suffix: 'admin'")
  })

  it('student ERP workspace pages provide multi-field query and Element Plus date filtering', () => {
    const documentView = readFileSync(join(process.cwd(), 'src/views/DocumentView.vue'), 'utf8')
    const masterDataView = readFileSync(join(process.cwd(), 'src/views/MasterDataView.vue'), 'utf8')
    const settlementView = readFileSync(join(process.cwd(), 'src/views/SettlementView.vue'), 'utf8')
    const stockView = readFileSync(join(process.cwd(), 'src/views/StockView.vue'), 'utf8')
    const auditView = readFileSync(join(process.cwd(), 'src/views/AuditView.vue'), 'utf8')

    expect(documentView).toContain('el-date-picker')
    expect(documentView).toContain('operationDateRange')
    expect(documentView).toContain('auditDateRange')
    expect(documentView).toContain('matchRecordByKeyword')
    expect(masterDataView).toContain('el-date-picker')
    expect(masterDataView).toContain('createDateRange')
    expect(masterDataView).toContain('updateDateRange')
    expect(masterDataView).toContain('matchRecordByKeyword')
    expect(settlementView).toContain('el-date-picker')
    expect(settlementView).toContain('createDateRange')
    expect(settlementView).toContain('matchRecordByKeyword')
    expect(stockView).toContain('warehouseName')
    expect(stockView).toContain('matchRecordByKeyword')
    expect(auditView).toContain('el-date-picker')
    expect(auditView).toContain('operationDateRange')
    expect(auditView).toContain('auditDateRange')
    expect(auditView).toContain('matchRecordByKeyword')
  })

  it('student ERP query filters cover every displayed business table field', () => {
    const documentView = readFileSync(join(process.cwd(), 'src/views/DocumentView.vue'), 'utf8')
    const masterDataView = readFileSync(join(process.cwd(), 'src/views/MasterDataView.vue'), 'utf8')
    const settlementView = readFileSync(join(process.cwd(), 'src/views/SettlementView.vue'), 'utf8')
    const stockView = readFileSync(join(process.cwd(), 'src/views/StockView.vue'), 'utf8')
    const auditView = readFileSync(join(process.cwd(), 'src/views/AuditView.vue'), 'utf8')

    for (const field of ['documentNo', 'warehouseCode', 'warehouseName', 'targetWarehouseCode', 'targetWarehouseName', 'partnerCode', 'partnerName', 'itemCount', 'totalAmount', 'relatedDocumentNo', 'creatorName']) {
      expect(documentView).toContain(`query.${field}`)
    }
    for (const field of ['code', 'name', 'categoryName', 'brandName', 'unitName', 'purchasePrice', 'salePrice', 'phone', 'address', 'status']) {
      expect(masterDataView).toContain(`query.${field}`)
    }
    for (const field of ['settlementNo', 'documentType', 'amount', 'relatedDocumentNo']) {
      expect(settlementView).toContain(`query.${field}`)
    }
    for (const field of ['productCode', 'productName', 'categoryName', 'brandName', 'unitName', 'warehouseName']) {
      expect(stockView).toContain(`query.${field}`)
    }
    for (const field of ['documentNo', 'warehouseCode', 'warehouseName', 'businessType', 'itemCount', 'creatorName', 'auditorName', 'status']) {
      expect(auditView).toContain(`query.${field}`)
    }
  })
  it('student ERP query filters are grouped across multiple toolbar rows', () => {
    const styles = readFileSync(join(process.cwd(), 'src/styles.css'), 'utf8')
    const documentView = readFileSync(join(process.cwd(), 'src/views/DocumentView.vue'), 'utf8')
    const masterDataView = readFileSync(join(process.cwd(), 'src/views/MasterDataView.vue'), 'utf8')
    const settlementView = readFileSync(join(process.cwd(), 'src/views/SettlementView.vue'), 'utf8')
    const stockView = readFileSync(join(process.cwd(), 'src/views/StockView.vue'), 'utf8')
    const auditView = readFileSync(join(process.cwd(), 'src/views/AuditView.vue'), 'utf8')

    for (const content of [documentView, masterDataView, settlementView, stockView, auditView]) {
      expect(content).toContain('toolbar query-toolbar')
    }
    expect(documentView.match(/class="query-break"/g)?.length ?? 0).toBeGreaterThanOrEqual(2)
    expect(masterDataView.match(/class="query-break"/g)?.length ?? 0).toBeGreaterThanOrEqual(3)
    expect(settlementView.match(/class="query-break"/g)?.length ?? 0).toBeGreaterThanOrEqual(2)
    expect(auditView.match(/class="query-break"/g)?.length ?? 0).toBeGreaterThanOrEqual(2)
    expect(stockView.match(/class="query-break"/g)?.length ?? 0).toBeGreaterThanOrEqual(1)
    expect(stockView).toContain('toolbar query-actions')
    expect(styles).toContain('.toolbar.query-toolbar')
    expect(styles).toContain('.query-break')
    expect(styles).toContain('flex-basis: 100%')
  })

  it('库存调拨路由使用库存调拨单配置', () => {
    const router = readFileSync(join(process.cwd(), 'src/router/index.ts'), 'utf8')
    const documentView = readFileSync(join(process.cwd(), 'src/views/DocumentView.vue'), 'utf8')

    expect(router).toContain("path: 'inventory/transfer'")
    expect(documentView).toContain("route.path === '/inventory/transfer'")
    expect(documentView).toContain("api: 'stock-transfer'")
    expect(documentView).toContain('请输入库存调拨单号查询')
    expect(documentView).toContain('未调拨')
    expect(documentView).toContain('待调拨')
    expect(documentView).toContain('已调拨')
    expect(documentView).toContain('无法调拨')
  })

  it('主数据新增使用已有业务选项并提供详情查看', () => {
    const masterDataView = readFileSync(join(process.cwd(), 'src/views/MasterDataView.vue'), 'utf8')

    expect(masterDataView).toContain('productReferenceOptions.category')
    expect(masterDataView).toContain('productReferenceOptions.brand')
    expect(masterDataView).toContain('productReferenceOptions.unit')
    expect(masterDataView).toContain('filterable')
    expect(masterDataView).not.toContain('allow-create')
    expect(masterDataView).toContain('el-input-number')
    expect(masterDataView).toContain('formRef.value?.validate()')
    expect(masterDataView).toContain('openDetail')
    expect(masterDataView).toContain('detailVisible')
    expect(masterDataView).toContain('el-descriptions')
  })

  it('单据新增按业务关系加载下拉并支持查看原单', () => {
    const documentView = readFileSync(join(process.cwd(), 'src/views/DocumentView.vue'), 'utf8')

    expect(documentView).toContain('associationOptions.warehouses')
    expect(documentView).toContain('associationOptions.partners')
    expect(documentView).toContain('selectableProducts')
    expect(documentView).toContain('returnOptions')
    expect(documentView).toContain('listReturnOptions')
    expect(documentView).toContain('documentDetail')
    expect(documentView).toContain('stockProductOptions')
    expect(documentView).toContain('transferTargetOptions')
    expect(documentView).toContain('handleWarehouseChange')
    expect(documentView).toContain('handleReturnSourceChange')
    expect(documentView).toContain('formRef.value?.validate()')
    expect(documentView).toContain('el-input-number')
    expect(documentView).toContain('openSourceDetail')
    expect(documentView).toContain('sourceDetailVisible')
    expect(documentView).not.toContain('allow-create')
  })

  it('退货单据表单要求关联原始单据', () => {
    const documentView = readFileSync(join(process.cwd(), 'src/views/DocumentView.vue'), 'utf8')

    expect(documentView).toContain('prop="relatedDocumentNo"')
    expect(documentView).toContain('关联采购入库单必填，请重新输入。')
    expect(documentView).toContain('关联销售出库单必填，请重新输入。')
  })

  it('审核和消息中心覆盖库存调拨入出库要求', () => {
    const auditView = readFileSync(join(process.cwd(), 'src/views/AuditView.vue'), 'utf8')
    const appLayout = readFileSync(join(process.cwd(), 'src/layouts/AppLayout.vue'), 'utf8')

    expect(auditView).toContain('业务类型')
    expect(auditView).toContain('调拨入库')
    expect(auditView).toContain('调拨出库')
    expect(auditView).toContain('审核拒绝原因')
    expect(appLayout).toContain('展开全部消息')
    expect(appLayout).toContain('displayedMessages')
  })

  it('界面结构对齐需求截图的弹窗、分页和上传样式', () => {
    const styles = readFileSync(join(process.cwd(), 'src/styles.css'), 'utf8')
    const masterDataView = readFileSync(join(process.cwd(), 'src/views/MasterDataView.vue'), 'utf8')
    const documentView = readFileSync(join(process.cwd(), 'src/views/DocumentView.vue'), 'utf8')
    const profileView = readFileSync(join(process.cwd(), 'src/views/ProfileView.vue'), 'utf8')

    expect(styles).toContain('.el-form-item.is-required .el-form-item__label::before')
    expect(styles).toContain('.el-message-box__status.el-message-box-icon--warning')
    expect(styles).toContain('.doc-upload-drop')
    expect(styles).toContain('.dialog-section-title')
    expect(masterDataView).toContain('doc-upload-drop')
    expect(masterDataView).toContain('批量导入文件')
    expect(documentView).toContain('class="dialog-section-title"')
    expect(documentView).toContain(':rules="formRules"')
    expect(profileView).toContain('profile-card')
    expect(profileView).toContain('头像预览')
  })

  it('测试竞赛界面覆盖缺陷发布、学员报告和排行榜', () => {
    const api = readFileSync(join(process.cwd(), 'src/api/index.ts'), 'utf8')
    const router = readFileSync(join(process.cwd(), 'src/router/index.ts'), 'utf8')
    const competitionView = readFileSync(join(process.cwd(), 'src/views/CompetitionView.vue'), 'utf8')

    expect(api).toContain('listBugDefinitions')
    expect(api).toContain('publishBug')
    expect(api).toContain('listCompetitionTasks')
    expect(api).toContain('submitBugReport')
    expect(api).toContain('reviewBugReport')
    expect(api).toContain('listBugRankings')
    expect(api).toContain('listStudents')
    expect(api).toContain('createStudent')
    expect(api).toContain('deleteStudent')
    expect(api).toContain('uploadCompetitionFile')
    expect(api).toContain('reviewCompetitionFile')
    expect(api).toContain('listStudentOperationLogs')
    expect(api).toContain('listRankingHistory')
    expect(router).toContain("path: 'competition/:type'")
    expect(competitionView).toContain('缺陷库发布')
    expect(competitionView).toContain('学员管理')
    expect(competitionView).toContain('新增学员')
    expect(competitionView).toContain('删除学员')
    expect(competitionView).toContain('ERP子账号')
    expect(competitionView).toContain('采购专员')
    expect(competitionView).toContain('仓库专员')
    expect(competitionView).toContain('销售专员')
    expect(competitionView).toContain('结算主管')
    expect(competitionView).toContain('默认密码')
    expect(competitionView).toContain('发布缺陷')
    expect(competitionView).not.toContain('缺陷测试任务')
    expect(competitionView).toContain('提交缺陷报告')
    expect(competitionView).toContain('学员报告评分')
    expect(competitionView).toContain('提交测试文件')
    expect(competitionView).toContain('文件评阅')
    expect(competitionView).toContain('操作轨迹')
    expect(competitionView).toContain('评分历史')
    expect(competitionView).toContain('我的提交文件')
    expect(competitionView).toContain('进入我的 ERP 工作区')
    expect(competitionView).toContain('竞赛排行榜')
    expect(competitionView).toContain('测试学员')
  })
  it('学员管理支持将主账号及全部 ERP 子账号密码重置为 123456', () => {
    const api = readFileSync(join(process.cwd(), 'src/api/index.ts'), 'utf8')
    const competitionView = readFileSync(join(process.cwd(), 'src/views/CompetitionView.vue'), 'utf8')

    expect(api).toContain('resetStudentPassword')
    expect(api).toContain('/reset-password')
    expect(competitionView).toContain('<Key />')
    expect(competitionView).toContain('重置密码')
    expect(competitionView).toContain('主账号及全部 ERP 子账号')
    expect(competitionView).toContain('123456')
    expect(competitionView).toContain('resetCount')
    expect(competitionView).toContain("action === 'cancel' || action === 'close'")
  })
})
