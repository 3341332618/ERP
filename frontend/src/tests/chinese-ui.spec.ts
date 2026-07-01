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
    expect(competitionView).toContain('竞赛排行榜')
    expect(competitionView).toContain('测试学员')
  })
})
