# 超级管理员测试竞赛流程测试文档

## 1. 测试目标

验证 ERP 测试竞赛系统的超级管理员端到端流程是否完整、前后端是否接通，重点覆盖：

- 终极管理员登录与菜单加载。
- 学员管理：查询、新增、删除学员。
- 缺陷发布：缺陷库读取、动态发布/取消发布。
- 学员文件提交：PDF、Word、Excel 上传入口与后端保存。
- 超级管理员文件评阅：评分、评语、轮次。
- 排行榜：文件评分进入总分。
- 评分历史：每次评分生成历史快照。
- 操作轨迹：学员提交测试文件等关键动作可追踪。
- 学员测试工作区隔离：学员产生的基础资料、单据、库存、结算按 `workspaceOwnerId` 隔离。

## 2. 测试环境

| 项目 | 内容 |
| --- | --- |
| 前端地址 | `http://127.0.0.1:5173` |
| 后端地址 | `http://127.0.0.1:8080` |
| 前端代理 | `/api -> http://127.0.0.1:8080` |
| 超级管理员 | `superadmin / 123456` |
| 学员账号 | `student01 / 123456`、`student02 / 123456` |
| 文件保存目录 | `backend/uploads/competition/{学员账号}/` |

## 3. 前后端接口映射

| 页面功能 | 前端路径 | 前端 API | 后端接口 |
| --- | --- | --- | --- |
| 超级管理员登录 | `/login` | `login` | `POST /api/auth/login` |
| 学员管理 | `/competition/students` | `listStudents` | `GET /api/competition/students` |
| 新增学员 | `/competition/students` | `createStudent` | `POST /api/competition/students` |
| 删除学员 | `/competition/students` | `deleteStudent` | `DELETE /api/competition/students/{id}` |
| 缺陷库发布 | `/competition/bugs` | `listBugDefinitions`、`publishBug` | `GET /api/competition/bugs`、`PATCH /api/competition/bugs/{id}/publish` |
| 文件评阅 | `/competition/files` | `listCompetitionFiles`、`reviewCompetitionFile` | `GET /api/competition/files`、`PATCH /api/competition/files/{id}/review` |
| 学员提交文件 | `/competition/submit-file` | `uploadCompetitionFile` | `POST /api/competition/files` |
| 操作轨迹 | `/competition/logs` | `listStudentOperationLogs` | `GET /api/competition/logs` |
| 评分历史 | `/competition/history` | `listRankingHistory` | `GET /api/competition/history` |
| 竞赛排行榜 | `/competition/rankings` | `listBugRankings` | `GET /api/competition/rankings` |

## 4. 端到端主流程

1. 超级管理员登录系统。
2. 系统返回 `SUPER_ADMIN` 用户信息和 `测试竞赛后台` 菜单。
3. 超级管理员进入 `学员管理`，系统加载已有学员。
4. 超级管理员新增一个临时学员，再删除该学员，验证增删闭环。
5. 超级管理员进入 `缺陷库发布`，读取 79 条缺陷定义。
6. 超级管理员发布一个缺陷，例如 `BUG-0004`。发布后只开启系统错误行为，不向学员展示缺陷清单。
7. 学员 `student01` 登录后在独立 ERP 工作区自行测试，提交缺陷报告或 PDF 测试文件。
8. 超级管理员进入 `文件评阅`，看到该文件并评分。
9. 系统生成评分历史。
10. 系统更新排行榜总分。
11. 超级管理员进入 `操作轨迹`，看到学员提交文件记录。

## 5. 关键测试用例

| 编号 | 用例 | 操作 | 期望结果 |
| --- | --- | --- | --- |
| TC-SA-001 | 超级管理员登录 | 使用 `superadmin / 123456` 登录 | 返回角色 `SUPER_ADMIN`，菜单包含 `学员管理`、`文件评阅`、`操作轨迹`、`评分历史` |
| TC-SA-002 | 学员列表加载 | 打开 `/competition/students` | 表格显示 `student01`、`student02` |
| TC-SA-003 | 新增学员 | 输入账号、姓名、手机号、密码保存 | 学员列表数量加 1 |
| TC-SA-004 | 删除学员 | 删除刚创建的临时学员 | 学员列表数量恢复 |
| TC-SA-005 | 缺陷库加载 | 打开 `/competition/bugs` | 返回 79 条缺陷定义 |
| TC-SA-006 | 缺陷动态发布 | 发布 `BUG-0004` | 返回 `active=true` |
| TC-SA-007 | 学员缺陷清单隐藏 | 学员打开竞赛入口或请求任务列表 | 不显示已发布缺陷编号、摘要和任务清单 |
| TC-SA-008 | 学员文件提交 | 学员提交 `.pdf` 文件 | 返回提交记录，状态为 `PENDING` |
| TC-SA-009 | 文件评阅 | 超级管理员评分 88 分并通过 | 返回 `APPROVED`，分数为 88 |
| TC-SA-010 | 评分历史 | 查看 `/competition/history` | 存在本次评分轮次历史 |
| TC-SA-011 | 排行榜 | 查看 `/competition/rankings` | `student01` 总分包含文件评分 |
| TC-SA-011 | 操作轨迹 | 查看 `/competition/logs?studentId=...` | 存在 `提交测试文件` 记录 |
| TC-SA-012 | 学员隔离 | 学员一新增资料、单据、库存、结算 | 学员二看不到学员一数据，全局管理员也不串数据 |

## 6. 已执行联调结果

执行方式：使用真实浏览器上下文从 `http://127.0.0.1:5173` 发起同源 `fetch` 请求，所有请求都经过前端代理 `/api` 到后端。

| 检查项 | 实际结果 |
| --- | --- |
| 超级管理员登录 | `SUPER_ADMIN` |
| 超级管理员菜单 | `学员管理`、`缺陷库发布`、`文件评阅`、`操作轨迹`、`评分历史`、`竞赛排行榜` |
| 学员初始数量 | 2 |
| 新增临时学员 | 成功，列表数量从 2 变 3 |
| 删除临时学员 | 成功，列表数量从 3 变 2 |
| 缺陷定义数量 | 79 |
| 发布 `BUG-0004` | 成功，`active=true` |
| 学员端缺陷清单 | 不显示已发布缺陷 |
| 学员上传测试文件 | 成功，返回文件提交 ID |
| 中文标题/模块/文件名 | 保存正常 |
| 文件评阅 | 成功，`APPROVED`，得分 88 |
| 评分历史 | 已生成本次轮次记录 |
| 排行榜 | `student01` 总分包含文件评分 |
| 操作轨迹 | 已记录 `提交测试文件` |

浏览器联调关键返回：

```json
{
  "uploadTitleOk": true,
  "uploadModuleOk": true,
  "uploadFileNameOk": true,
  "reviewRoundOk": true,
  "historyHasReviewedFile": true,
  "rankingHasStudent01": true,
  "logsHasUpload": true
}
```

## 7. 自动化测试覆盖

| 命令 | 覆盖内容 |
| --- | --- |
| `backend/.\\mvnw.cmd test` | 后端业务流程、库存调拨、退货关联、竞赛、文件评分、评分历史、学员工作区隔离 |
| `frontend/npm.cmd test -- --run` | 中文界面关键文案、竞赛页面 API 和入口覆盖 |
| `frontend/npm.cmd run build` | Vue 模板和 TypeScript 编译 |
| `git diff --check` | 空白和格式风险检查 |

## 8. 当前风险

- 当前学员隔离是内存版逻辑工作区，通过 `workspaceOwnerId` 隔离，不是真实每学员一个数据库文件。
- 后端重启后，内存中的学员提交文件记录、评分历史、排行榜会重置；上传文件本体会留在 `uploads/competition/`。
- 如果要用于长期竞赛，需要接入持久化数据库，并按学员生成独立数据源或独立 schema。
- 文件下载接口尚未完善；当前后台能接收、列表展示、评分，但还没有“下载原文件”按钮。
- 浏览器缓存或旧 dev server 进程可能导致菜单/页面显示旧状态，测试前需要确认 8080 和 5173 都是当前进程。

## 9. 通过标准

本阶段通过标准：

- 超级管理员能登录并看到后台菜单。
- 学员列表不是空表，能新增/删除学员。
- 学员能提交测试文件。
- 超级管理员能看到文件并评分。
- 评分能进入排行榜和历史记录。
- 学员关键动作能在操作轨迹里查看。
- 后端测试、前端测试、前端构建均通过。

下一阶段通过标准：

- 每个学员使用真实独立数据库文件或 schema。
- 文件可下载、预览或打包导出。
- 排行榜支持按竞赛批次/轮次筛选。
- 操作轨迹覆盖更多 ERP 操作，例如基础资料修改、采购入库、库存调拨、退货、结算查询。
