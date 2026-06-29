# ERP 管理平台整体操作手册

这份手册按“不会开发也能照着操作”的方式写。你可以把它当成项目使用说明、开发说明、测试说明和 Git 备份说明的合集。

当前项目状态需要先记住一句话：

> 项目已经接入 MySQL 启动配置和 Flyway 建表脚本；但业务服务层还在从内存 `ErpStore` 迁移到数据库 Repository。也就是说：启动后端需要 MySQL，但部分业务运行数据目前仍可能随后端重启而重置。

## 1. 你需要准备什么

### 必装软件

| 软件 | 用途 | 检查命令 |
| --- | --- | --- |
| JDK 17+ | 跑 Spring Boot 后端 | `java -version` |
| Node.js 20+ | 跑 Vue 前端 | `node --version` |
| npm 10+ | 安装和运行前端依赖 | `npm.cmd --version` |
| Docker Desktop | 启动本地 MySQL、跑数据库迁移测试 | `docker --version` |
| Git | 备份、提交、回退代码 | `git --version` |
| PowerShell | 推荐使用的命令行 | Windows 自带 |

在项目根目录 `D:\ai work\ERP` 执行：

```powershell
java -version
node --version
npm.cmd --version
docker --version
docker compose version
git --version
```

如果 Docker 命令报错，先打开 Docker Desktop，等左下角或任务栏显示 Docker 已启动。

## 2. 项目目录怎么看

```text
ERP/
├─ backend/                         # Spring Boot 后端
│  ├─ src/main/java/com/erp/
│  │  ├─ common/                    # 统一响应、业务异常、全局异常处理
│  │  ├─ domain/                    # 领域模型和枚举
│  │  ├─ security/                  # JWT 登录鉴权
│  │  ├─ store/                     # 当前业务内存服务，后续会迁移到数据库
│  │  └─ web/                       # REST 接口控制器
│  ├─ src/main/resources/
│  │  ├─ application.yml            # 通用配置
│  │  ├─ application-local.yml      # 本地开发数据库默认配置
│  │  └─ db/migration/              # Flyway 数据库脚本
│  └─ src/test/java/com/erp/        # 后端测试
├─ frontend/                        # Vue 3 前端
│  ├─ src/api/                      # API 请求封装
│  ├─ src/router/                   # 前端路由
│  ├─ src/views/                    # 页面
│  └─ src/tests/                    # 前端测试
├─ compose.yaml                     # 本地 MySQL Docker Compose
├─ .env.example                     # Docker Compose 环境变量示例
├─ docs/                            # 文档
└─ README.md                        # 项目总览
```

## 3. 第一次启动项目

建议开三个 PowerShell 窗口：

- 窗口 1：MySQL
- 窗口 2：后端
- 窗口 3：前端

所有命令默认从项目根目录开始：

```powershell
cd "D:\ai work\ERP"
```

### 3.1 启动 MySQL

第一次启动先复制环境变量文件：

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
```

启动 MySQL：

```powershell
docker compose up -d mysql
docker compose ps
```

看到 `mysql` 服务状态为 `healthy` 或正在启动中，就可以继续。首次启动 MySQL 可能要等几十秒。

默认数据库连接：

| 项 | 默认值 |
| --- | --- |
| 主机 | `127.0.0.1` |
| 端口 | `3307` |
| 数据库 | `erp` |
| 用户名 | `erp` |
| 密码 | `erp_local_password` |

> 注意：本项目故意使用本机 `3307`，避免和你电脑已有的 MySQL `3306` 冲突。

### 3.2 启动后端

新开一个 PowerShell：

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd spring-boot:run
```

后端地址：

```text
http://127.0.0.1:8080
```

后端启动时会自动连接 MySQL，并执行 Flyway 数据库脚本。

### 3.3 启动前端

新开第三个 PowerShell。

第一次运行前端需要安装依赖：

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd install
```

启动前端：

```powershell
npm.cmd run dev
```

前端地址：

```text
http://127.0.0.1:5173
```

打开浏览器访问这个地址即可。

## 4. 平时怎么启动

如果依赖已经安装过，平时只需要这样：

### 窗口 1：MySQL

```powershell
cd "D:\ai work\ERP"
docker compose up -d mysql
docker compose ps
```

### 窗口 2：后端

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd spring-boot:run
```

### 窗口 3：前端

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd run dev
```

然后浏览器打开：

```text
http://127.0.0.1:5173
```

## 5. 演示账号

所有账号初始密码都是：

```text
123456
```

| 账号 | 角色 | 用途 |
| --- | --- | --- |
| `admin` | 系统管理员 | 看全部 ERP 模块、管理测试竞赛 |
| `superadmin` | 终极管理员 | 评阅测试文件、查看操作轨迹、评分历史 |
| `purchase_manager` | 采购主管 | 查看采购业务 |
| `purchase_staff` | 采购专员 | 创建采购入库、采购退货 |
| `warehouse_manager` | 仓库主管 | 查看库存业务 |
| `warehouse_staff` | 仓库专员 | 审核华东仓库入库/出库 |
| `warehouse_staff_south` | 仓库专员 | 备用仓库专员账号 |
| `sales_manager` | 销售主管 | 查看销售业务 |
| `sales_staff` | 销售专员 | 创建销售出库、销售退货 |
| `settlement_manager` | 结算主管 | 查看收入结算、支出结算 |
| `student01` | 测试学员 | 独立 ERP 工作区、提交缺陷报告和测试文件 |
| `student02` | 测试学员 | 独立 ERP 工作区 |

## 6. 推荐业务演示流程

如果你要给老师、同学或答辩现场演示，按这个顺序最稳。

### 6.1 登录系统

1. 浏览器打开 `http://127.0.0.1:5173`。
2. 使用 `admin / 123456` 登录。
3. 看首页、菜单和基础模块是否出现。

### 6.2 基础资料

用 `admin` 查看：

- 商品品牌
- 商品分类
- 商品单位
- 商品管理
- 仓库信息
- 客户信息
- 供应商信息

系统已经内置了演示数据，例如仓库、客户、供应商和商品。

### 6.3 采购入库闭环

1. 登录 `purchase_staff / 123456`。
2. 进入采购管理。
3. 新建采购入库单。
4. 填仓库、供应商、商品、数量、单价。
5. 保存后提交审核。
6. 退出，登录 `warehouse_staff / 123456`。
7. 进入库存审核。
8. 审核通过采购入库单。
9. 查看库存分布，库存应增加。
10. 登录 `settlement_manager / 123456`，查看支出结算。

### 6.4 销售出库闭环

1. 登录 `sales_staff / 123456`。
2. 进入销售管理。
3. 新建销售出库单。
4. 选择仓库、客户、商品、数量、单价。
5. 提交审核。
6. 登录 `warehouse_staff / 123456`。
7. 审核通过销售出库单。
8. 查看库存分布，库存应减少。
9. 登录 `settlement_manager / 123456`，查看收入结算。

### 6.5 采购退货 / 销售退货

退货单必须关联已经审核通过的原单：

- 采购退货关联采购入库单
- 销售退货关联销售出库单

退货数量不能超过原单剩余可退数量。

### 6.6 库存调拨

1. 登录 `warehouse_manager / 123456`。
2. 进入库存调拨。
3. 选择调出仓库、调入仓库、商品、数量。
4. 提交调拨单。
5. 调出仓和调入仓对应仓库专员会收到审核消息。
6. 仓库专员审核后，调出仓库存减少，调入仓库存增加。

### 6.7 测试竞赛模块

管理员侧：

1. 登录 `admin / 123456`。
2. 进入测试竞赛管理。
3. 在缺陷库中发布缺陷任务。
4. 查看学员列表、缺陷报告和排行榜。

学员侧：

1. 登录 `student01 / 123456`。
2. 查看缺陷测试任务。
3. 使用自己的独立 ERP 工作区操作。
4. 提交缺陷报告。
5. 上传测试文件。
6. 查看个人提交记录和排行榜。

终极管理员侧：

1. 登录 `superadmin / 123456`。
2. 评阅测试文件。
3. 查看操作轨迹、评分历史和排行榜。

## 7. 怎么跑测试

### 7.1 后端业务测试，不需要 Docker

这个命令只跑业务可用性测试，不跑 MySQL/Flyway 迁移验证：

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd "-Dtest=ErpApplicationTests,ErpCoreFlowIntegrationTest,TrainingCompetitionIntegrationTest" test
```

这类测试覆盖：

- Spring 上下文启动
- 菜单和角色
- 采购、销售、库存、结算闭环
- 退货规则
- 库存调拨
- 学员工作区隔离
- 测试竞赛、报告、文件、评分、排行榜

### 7.2 数据库迁移测试，需要 Docker

等 Docker Desktop 启动后再跑：

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd "-Dtest=PersistenceBootstrapTest" test
```

这个测试专门检查：

- MySQL Testcontainers 是否能启动
- Flyway 是否能建表
- 关键表和外键约束是否存在

### 7.3 后端全量测试

如果 Docker 已经启动，可以跑：

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd test
```

如果 Docker 没启动，全量测试里的 `PersistenceBootstrapTest` 会失败。这时先用 7.1 的业务测试命令。

### 7.4 前端测试

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd test -- --run
```

### 7.5 前端打包构建

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd run build
```

构建产物在：

```text
frontend/dist/
```

如果看到 “chunk 超过 500 kB” 警告，一般不影响构建成功。

## 8. 怎么打包交付

### 8.1 后端打包

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd clean package
```

生成文件：

```text
backend/target/erp-backend-0.0.1-SNAPSHOT.jar
```

### 8.2 前端打包

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd run build
```

生成文件夹：

```text
frontend/dist/
```

### 8.3 部署思路

部署时一般分三部分：

1. MySQL 数据库
2. 后端 JAR
3. 前端静态文件

后端启动示例：

```powershell
$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "3307"
$env:DB_NAME = "erp"
$env:DB_USERNAME = "erp"
$env:DB_PASSWORD = "erp_local_password"
$env:ERP_JWT_SECRET = "请换成你自己的长随机密钥"
java -jar .\backend\target\erp-backend-0.0.1-SNAPSHOT.jar
```

前端可以用 Nginx、Caddy 或其他静态服务器托管 `frontend/dist/`，并把 `/api` 反向代理到后端 `8080`。

## 9. Git 备份、提交和回退

### 9.1 查看当前改了什么

```powershell
cd "D:\ai work\ERP"
git status --short
```

### 9.2 做一个本地备份提交

适合在大改之前使用：

```powershell
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
git add -A
git commit -m "backup: before changes $stamp"
git tag "backup/manual-$stamp"
```

### 9.3 查看最近提交

```powershell
git log --oneline --decorate -10
```

### 9.4 查看已有备份 tag

```powershell
git tag --list "backup/*" --sort=-creatordate
```

### 9.5 安全查看某个备份

不要直接覆盖当前代码，先新建分支查看：

```powershell
git switch -c check-backup backup/pre-business-fix-20260629-152532
```

看完后切回当前开发分支：

```powershell
git switch feature/erp-core-loop
```

### 9.6 回到某个备份

如果你只是想参考备份，不要回退，直接看 tag 或新建分支。

如果你确认要让当前分支回到备份点，才使用：

```powershell
git reset --hard backup/pre-business-fix-20260629-152532
```

这会覆盖当前未提交改动。执行前先确认：

```powershell
git status --short
```

## 10. 常见问题处理

### 10.1 前端页面打不开

检查前端是否启动：

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd run dev
```

浏览器打开：

```text
http://127.0.0.1:5173
```

### 10.2 前端提示请求失败

检查后端是否启动：

```powershell
Invoke-WebRequest http://127.0.0.1:8080/api/auth/login -Method POST -ContentType 'application/json' -Body '{"username":"admin","password":"123456"}'
```

如果后端没启动，运行：

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd spring-boot:run
```

### 10.3 后端启动失败，提示数据库连接失败

先看 Docker Desktop 是否打开，再执行：

```powershell
cd "D:\ai work\ERP"
docker compose ps
```

如果 MySQL 没启动：

```powershell
docker compose up -d mysql
```

确认本地端口：

```powershell
Get-NetTCPConnection -LocalPort 3307 -ErrorAction SilentlyContinue
```

### 10.4 端口被占用

查看端口：

```powershell
Get-NetTCPConnection -LocalPort 8080,5173,3307 -ErrorAction SilentlyContinue
```

常用端口：

| 端口 | 用途 |
| --- | --- |
| `8080` | 后端 |
| `5173` | 前端 |
| `3307` | 本地 MySQL |

### 10.5 Maven 报 `AccessDeniedException`

Windows 下有时 `backend/target` 构建产物权限会卡住。处理顺序：

1. 关掉正在运行的后端。
2. 关闭占用项目目录的终端或编辑器任务。
3. 重新运行 Maven。

如果仍然失败，可以删除构建产物后重试：

```powershell
cd "D:\ai work\ERP\backend"
Remove-Item -LiteralPath .\target -Recurse -Force
.\mvnw.cmd test
```

删除前确认你在 `backend` 目录里，不要在项目外误删目录。

### 10.6 Docker 报管道不存在

类似错误：

```text
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine
```

处理方式：

1. 打开 Docker Desktop。
2. 等 Docker 完全启动。
3. 再执行 `docker compose ps`。

### 10.7 修改 `.env` 后数据库账号没变化

MySQL 的初始化账号只在数据卷第一次创建时生效。已经创建过数据卷后，改 `.env` 不会自动改旧账号。

开发环境如果要重建数据库：

```powershell
cd "D:\ai work\ERP"
docker compose down -v
docker compose up -d mysql
```

这会清空本地 Docker 数据卷里的 MySQL 数据。

## 11. 当前限制

- 后端启动已经接入 MySQL 和 Flyway。
- MySQL schema 已经有核心表结构。
- 业务服务层还没全部迁移到数据库 Repository，当前仍有内存业务状态。
- 竞赛上传文件保存在后端运行目录下的 `uploads/competition/<用户名>/`。
- 适合课程设计、演示和开发练习；作为生产系统还需要继续完善持久化、审计、权限、部署和文件存储。

## 12. 以后你怎么让我继续做

你可以直接这样说：

```text
先给我做 Git 备份，然后修复后端测试。
```

```text
启动项目并检查登录、采购入库、库存审核、结算是否能跑通。
```

```text
帮我接着做 MySQL 业务持久化，先写计划再改代码。
```

```text
帮我打包前后端，并把交付步骤写出来。
```

```text
帮我检查这个项目还能优化什么，先列问题再逐个修。
```

我的建议是：每次要大改之前，都先说“先备份”。这样 Git 里会有一个可回退点。

## 13. 你现在最常用的命令速查

```powershell
# 进入项目
cd "D:\ai work\ERP"

# 看当前 Git 状态
git status --short

# 启动 MySQL
docker compose up -d mysql

# 启动后端
cd "D:\ai work\ERP\backend"
.\mvnw.cmd spring-boot:run

# 启动前端
cd "D:\ai work\ERP\frontend"
npm.cmd run dev

# 后端业务测试，不需要 Docker
cd "D:\ai work\ERP\backend"
.\mvnw.cmd "-Dtest=ErpApplicationTests,ErpCoreFlowIntegrationTest,TrainingCompetitionIntegrationTest" test

# 前端测试
cd "D:\ai work\ERP\frontend"
npm.cmd test -- --run

# 前端构建
cd "D:\ai work\ERP\frontend"
npm.cmd run build
```
