# ERP MySQL 持久化设计

## 1. 目标

将当前由 `ErpStore` 内存集合承载的用户、基础资料、业务单据、库存、结算、消息和测试竞赛数据迁移到 MySQL，保证后端重启后数据仍然存在，同时保持现有 REST API、前端页面和演示账号行为兼容。

## 2. 范围

本次包含：

- Docker Compose 启动单节点 MySQL。
- Spring Data JPA 实体与 Repository。
- Flyway 建表、索引和演示数据迁移。
- 用户、基础资料、单据、库存、结算、消息和竞赛数据持久化。
- 审核、库存和结算的数据库事务与并发控制。
- Testcontainers MySQL 集成测试。
- README 中的数据库启动、配置和重置说明。

本次不包含：

- Redis、消息队列、分库分表和读写分离。
- 将前后端应用本身容器化。
- 对象存储；竞赛文件仍保存到本地目录，仅将元数据持久化。
- REST API 路径和前端页面重构。

## 3. 方案选择

采用分模块替换方案：先建立数据库和持久化基础设施，再依次迁移认证与基础资料、ERP 核心业务、测试竞赛，最后删除 `ErpStore` 中的内存集合和启动时内存初始化逻辑。

不采用一次性重写，因为 `ErpStore` 同时包含权限、校验、库存、退货、结算和竞赛规则，直接替换难以定位回归问题。也不采用把整个内存状态序列化为 JSON/BLOB 的方式，因为那会失去关系约束、索引和事务能力。

## 4. 架构

```text
Vue 3 前端
    ↓ 保持现有 /api 契约
Spring MVC Controller
    ↓
领域服务（认证、主数据、单据、库存、结算、竞赛）
    ↓ @Transactional
Spring Data JPA Repository
    ↓
MySQL 8.4
    ↑
Flyway 版本迁移 + Testcontainers 集成测试
```

控制器继续返回现有 `ApiResult` 数据形状。JPA Entity 不直接作为 API DTO 返回；服务层负责映射，防止数据库结构泄漏到前端契约。

## 5. 实体分析

### 5.1 工作区与用户

- `erp_workspace`：系统正式数据和每个测试学员的独立数据空间。
- `sys_user`：所有 ERP 角色、管理员和测试学员统一账号。
- 正式演示账号归属系统工作区；每个测试学员归属自己的学员工作区。
- 仓库专员通过 `sys_user.warehouse_id` 绑定负责仓库。
- 删除学员改为将账号状态置为 `DISABLED`，列表和登录均排除禁用账号，保留其历史单据、评分和审计记录。

### 5.2 基础资料

- 品牌、分类、单位、仓库、客户、供应商和商品分别建表。
- 所有基础资料必须关联工作区，编码在工作区内唯一。
- 商品通过外键关联品牌、分类和单位。

### 5.3 ERP 单据与库存

- `biz_document` 保存单据头、状态、仓库、往来单位、创建人和审核人。
- `biz_document_item` 保存商品、数量、价格和商品快照。
- 退货单通过 `related_document_id` 关联原始已审核单据。
- `inventory_balance` 只保存实际库存；可用库存按实际库存减去未完结出库类单据占用量计算。
- `finance_settlement` 与产生它的业务单据一对一，防止重复生成结算。

### 5.4 消息与测试竞赛

- `sys_message` 保存仓库审核通知。
- 缺陷定义为全局数据；报告、文件、评分历史和操作日志关联学员账号。
- 文件二进制不进入 MySQL，数据库只保存文件名、类型、大小和存储路径。

## 6. 表设计与 MySQL DDL

字段的类型、可空性、默认值和中文含义通过以下 DDL 的列定义与 `COMMENT` 明确。实际实现保存为 Flyway 的 `V1__create_schema.sql`，字符集统一使用 `utf8mb4`。

```sql
CREATE TABLE erp_workspace (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '工作区主键',
    code VARCHAR(64) NOT NULL COMMENT '工作区编码',
    name VARCHAR(100) NOT NULL COMMENT '工作区名称',
    workspace_type VARCHAR(20) NOT NULL COMMENT '工作区类型：SYSTEM或STUDENT',
    owner_user_id BIGINT NULL COMMENT '学员工作区所有者',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_workspace_code (code),
    UNIQUE KEY uk_workspace_owner (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP工作区';

CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    workspace_id BIGINT NOT NULL COMMENT '默认工作区',
    warehouse_id BIGINT NULL COMMENT '仓库专员所属仓库',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
    name VARCHAR(64) NOT NULL COMMENT '姓名',
    phone VARCHAR(32) NOT NULL COMMENT '联系电话',
    avatar MEDIUMTEXT NULL COMMENT '头像数据',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_workspace_role (workspace_id, role_code),
    KEY idx_user_warehouse (warehouse_id),
    CONSTRAINT fk_user_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

ALTER TABLE erp_workspace
    ADD CONSTRAINT fk_workspace_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user (id) ON DELETE SET NULL;

CREATE TABLE master_brand (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '品牌主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    code VARCHAR(40) NOT NULL COMMENT '品牌编码',
    name VARCHAR(100) NOT NULL COMMENT '品牌名称',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_brand_workspace_code (workspace_id, code),
    KEY idx_brand_workspace_name (workspace_id, name),
    CONSTRAINT fk_brand_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品品牌';

CREATE TABLE master_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    code VARCHAR(40) NOT NULL COMMENT '分类编码',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_workspace_code (workspace_id, code),
    KEY idx_category_workspace_name (workspace_id, name),
    CONSTRAINT fk_category_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

CREATE TABLE master_unit (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '单位主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    code VARCHAR(40) NOT NULL COMMENT '单位编码',
    name VARCHAR(100) NOT NULL COMMENT '单位名称',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_unit_workspace_code (workspace_id, code),
    KEY idx_unit_workspace_name (workspace_id, name),
    CONSTRAINT fk_unit_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品单位';

CREATE TABLE master_warehouse (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '仓库主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    code VARCHAR(40) NOT NULL COMMENT '仓库编码',
    name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    phone VARCHAR(32) NULL COMMENT '联系电话',
    address VARCHAR(255) NULL COMMENT '仓库地址',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_workspace_code (workspace_id, code),
    KEY idx_warehouse_workspace_name (workspace_id, name),
    CONSTRAINT fk_warehouse_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库';

ALTER TABLE sys_user
    ADD CONSTRAINT fk_user_warehouse FOREIGN KEY (warehouse_id) REFERENCES master_warehouse (id) ON DELETE SET NULL;

CREATE TABLE master_customer (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    code VARCHAR(40) NOT NULL COMMENT '客户编码',
    name VARCHAR(100) NOT NULL COMMENT '客户名称',
    contact VARCHAR(64) NULL COMMENT '联系人',
    phone VARCHAR(32) NULL COMMENT '联系电话',
    address VARCHAR(255) NULL COMMENT '地址',
    settlement_method VARCHAR(64) NULL COMMENT '结算方式',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_workspace_code (workspace_id, code),
    KEY idx_customer_workspace_name (workspace_id, name),
    CONSTRAINT fk_customer_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户';

CREATE TABLE master_supplier (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '供应商主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    code VARCHAR(40) NOT NULL COMMENT '供应商编码',
    name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact VARCHAR(64) NULL COMMENT '联系人',
    phone VARCHAR(32) NULL COMMENT '联系电话',
    address VARCHAR(255) NULL COMMENT '地址',
    settlement_method VARCHAR(64) NULL COMMENT '结算方式',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_workspace_code (workspace_id, code),
    KEY idx_supplier_workspace_name (workspace_id, name),
    CONSTRAINT fk_supplier_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商';

CREATE TABLE master_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    category_id BIGINT NOT NULL COMMENT '商品分类',
    brand_id BIGINT NOT NULL COMMENT '商品品牌',
    unit_id BIGINT NOT NULL COMMENT '商品单位',
    code VARCHAR(40) NOT NULL COMMENT '商品编码',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    purchase_price DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '采购价',
    sale_price DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '销售价',
    image_data MEDIUMTEXT NULL COMMENT '商品图片数据',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_workspace_code (workspace_id, code),
    KEY idx_product_workspace_name (workspace_id, name),
    KEY idx_product_category (category_id),
    KEY idx_product_brand (brand_id),
    CONSTRAINT fk_product_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES master_category (id),
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES master_brand (id),
    CONSTRAINT fk_product_unit FOREIGN KEY (unit_id) REFERENCES master_unit (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

CREATE TABLE biz_document (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '单据主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    document_type VARCHAR(32) NOT NULL COMMENT '单据类型',
    document_no VARCHAR(40) NOT NULL COMMENT '单据编号',
    related_document_id BIGINT NULL COMMENT '退货关联原单',
    warehouse_id BIGINT NOT NULL COMMENT '业务仓库或调出仓库',
    target_warehouse_id BIGINT NULL COMMENT '调拨目标仓库',
    supplier_id BIGINT NULL COMMENT '采购供应商',
    customer_id BIGINT NULL COMMENT '销售客户',
    warehouse_code_snapshot VARCHAR(40) NOT NULL COMMENT '仓库编码快照',
    warehouse_name_snapshot VARCHAR(100) NOT NULL COMMENT '仓库名称快照',
    target_warehouse_code_snapshot VARCHAR(40) NULL COMMENT '目标仓库编码快照',
    target_warehouse_name_snapshot VARCHAR(100) NULL COMMENT '目标仓库名称快照',
    partner_code_snapshot VARCHAR(40) NULL COMMENT '往来单位编码快照',
    partner_name_snapshot VARCHAR(100) NULL COMMENT '往来单位名称快照',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '单据总额',
    document_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    creator_id BIGINT NOT NULL COMMENT '创建人',
    operation_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '业务操作时间',
    auditor_id BIGINT NULL COMMENT '审核人',
    audit_time DATETIME(6) NULL COMMENT '审核时间',
    reject_reason VARCHAR(500) NULL COMMENT '拒绝原因',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_workspace_no (workspace_id, document_no),
    KEY idx_document_workspace_type_status (workspace_id, document_type, document_status),
    KEY idx_document_creator (creator_id, operation_time),
    KEY idx_document_warehouse_status (warehouse_id, document_status),
    KEY idx_document_target_warehouse_status (target_warehouse_id, document_status),
    KEY idx_document_related (related_document_id),
    CONSTRAINT fk_document_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id),
    CONSTRAINT fk_document_related FOREIGN KEY (related_document_id) REFERENCES biz_document (id),
    CONSTRAINT fk_document_warehouse FOREIGN KEY (warehouse_id) REFERENCES master_warehouse (id),
    CONSTRAINT fk_document_target_warehouse FOREIGN KEY (target_warehouse_id) REFERENCES master_warehouse (id),
    CONSTRAINT fk_document_supplier FOREIGN KEY (supplier_id) REFERENCES master_supplier (id),
    CONSTRAINT fk_document_customer FOREIGN KEY (customer_id) REFERENCES master_customer (id),
    CONSTRAINT fk_document_creator FOREIGN KEY (creator_id) REFERENCES sys_user (id),
    CONSTRAINT fk_document_auditor FOREIGN KEY (auditor_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP业务单据';

CREATE TABLE biz_document_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '单据明细主键',
    document_id BIGINT NOT NULL COMMENT '所属单据',
    product_id BIGINT NOT NULL COMMENT '商品',
    product_code_snapshot VARCHAR(40) NOT NULL COMMENT '商品编码快照',
    product_name_snapshot VARCHAR(100) NOT NULL COMMENT '商品名称快照',
    category_name_snapshot VARCHAR(100) NOT NULL COMMENT '分类名称快照',
    brand_name_snapshot VARCHAR(100) NOT NULL COMMENT '品牌名称快照',
    unit_name_snapshot VARCHAR(100) NOT NULL COMMENT '单位名称快照',
    quantity DECIMAL(18,4) NOT NULL COMMENT '数量',
    price DECIMAL(18,2) NOT NULL COMMENT '单价',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    remark VARCHAR(255) NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_document_item_document (document_id),
    KEY idx_document_item_product (product_id),
    CONSTRAINT fk_document_item_document FOREIGN KEY (document_id) REFERENCES biz_document (id) ON DELETE CASCADE,
    CONSTRAINT fk_document_item_product FOREIGN KEY (product_id) REFERENCES master_product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP业务单据明细';

CREATE TABLE inventory_balance (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存余额主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    warehouse_id BIGINT NOT NULL COMMENT '仓库',
    product_id BIGINT NOT NULL COMMENT '商品',
    actual_quantity DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '实际库存',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_scope (workspace_id, warehouse_id, product_id),
    KEY idx_inventory_product (workspace_id, product_id),
    CONSTRAINT fk_inventory_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES master_warehouse (id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES master_product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存余额';

CREATE TABLE finance_settlement (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '结算主键',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区',
    document_id BIGINT NOT NULL COMMENT '来源业务单据',
    direction VARCHAR(16) NOT NULL COMMENT '方向：income或expense',
    settlement_no VARCHAR(40) NOT NULL COMMENT '结算单号',
    document_type_label VARCHAR(40) NOT NULL COMMENT '来源业务类型',
    amount DECIMAL(18,2) NOT NULL COMMENT '结算金额',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlement_document (document_id),
    UNIQUE KEY uk_settlement_workspace_no (workspace_id, settlement_no),
    KEY idx_settlement_workspace_direction (workspace_id, direction, created_at),
    CONSTRAINT fk_settlement_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id),
    CONSTRAINT fk_settlement_document FOREIGN KEY (document_id) REFERENCES biz_document (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支结算';

CREATE TABLE sys_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息主键',
    user_id BIGINT NOT NULL COMMENT '接收用户',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content VARCHAR(1000) NOT NULL COMMENT '消息内容',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已读',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_message_user_read_time (user_id, is_read, created_at),
    CONSTRAINT fk_message_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息';

CREATE TABLE test_bug_definition (
    id VARCHAR(64) NOT NULL COMMENT '缺陷定义编码',
    role_name VARCHAR(64) NOT NULL COMMENT '关联角色名称',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    function_name VARCHAR(100) NOT NULL COMMENT '功能名称',
    summary VARCHAR(500) NOT NULL COMMENT '缺陷摘要',
    reproduce_steps TEXT NOT NULL COMMENT '复现步骤',
    expected_result TEXT NOT NULL COMMENT '预期结果',
    actual_result TEXT NOT NULL COMMENT '实际结果',
    severity VARCHAR(20) NOT NULL COMMENT '严重程度',
    active BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否发布',
    publisher_id BIGINT NULL COMMENT '发布人',
    publish_time DATETIME(6) NULL COMMENT '发布时间',
    PRIMARY KEY (id),
    KEY idx_bug_active_module (active, module_name),
    CONSTRAINT fk_bug_publisher FOREIGN KEY (publisher_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试缺陷定义';

CREATE TABLE test_bug_report (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '缺陷报告主键',
    bug_id VARCHAR(64) NOT NULL COMMENT '缺陷定义编码',
    student_id BIGINT NOT NULL COMMENT '提交学员',
    workspace_id BIGINT NOT NULL COMMENT '学员工作区',
    bug_summary_snapshot VARCHAR(500) NOT NULL COMMENT '缺陷摘要快照',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    title VARCHAR(200) NOT NULL COMMENT '报告标题',
    reproduce_steps TEXT NOT NULL COMMENT '复现步骤',
    expected_result TEXT NOT NULL COMMENT '预期结果',
    actual_result TEXT NOT NULL COMMENT '实际结果',
    evidence MEDIUMTEXT NULL COMMENT '测试证据',
    report_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '评分状态',
    score INT NOT NULL DEFAULT 0 COMMENT '得分',
    review_comment VARCHAR(1000) NULL COMMENT '评语',
    reviewer_id BIGINT NULL COMMENT '评分人',
    submit_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '提交时间',
    review_time DATETIME(6) NULL COMMENT '评分时间',
    PRIMARY KEY (id),
    KEY idx_report_student_time (student_id, submit_time),
    KEY idx_report_status_time (report_status, submit_time),
    CONSTRAINT chk_report_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT fk_report_bug FOREIGN KEY (bug_id) REFERENCES test_bug_definition (id),
    CONSTRAINT fk_report_student FOREIGN KEY (student_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id),
    CONSTRAINT fk_report_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员缺陷报告';

CREATE TABLE test_file_submission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '测试文件提交主键',
    student_id BIGINT NOT NULL COMMENT '提交学员',
    bug_id VARCHAR(64) NULL COMMENT '关联缺陷',
    title VARCHAR(200) NOT NULL COMMENT '提交标题',
    module_name VARCHAR(100) NOT NULL COMMENT '测试模块',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    content_type VARCHAR(100) NOT NULL COMMENT 'MIME类型',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
    storage_path VARCHAR(500) NOT NULL COMMENT '本地存储路径',
    submission_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '评分状态',
    score INT NOT NULL DEFAULT 0 COMMENT '得分',
    round_name VARCHAR(100) NULL COMMENT '竞赛轮次',
    review_comment VARCHAR(1000) NULL COMMENT '评语',
    reviewer_id BIGINT NULL COMMENT '评分人',
    submit_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '提交时间',
    review_time DATETIME(6) NULL COMMENT '评分时间',
    PRIMARY KEY (id),
    KEY idx_file_student_time (student_id, submit_time),
    KEY idx_file_status_time (submission_status, submit_time),
    CONSTRAINT chk_file_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT fk_file_student FOREIGN KEY (student_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_bug FOREIGN KEY (bug_id) REFERENCES test_bug_definition (id),
    CONSTRAINT fk_file_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试文件提交';

CREATE TABLE test_ranking_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评分历史主键',
    round_name VARCHAR(100) NOT NULL COMMENT '竞赛轮次',
    student_id BIGINT NOT NULL COMMENT '学员',
    total_score INT NOT NULL DEFAULT 0 COMMENT '累计得分',
    approved_reports INT NOT NULL DEFAULT 0 COMMENT '通过报告数',
    approved_files INT NOT NULL DEFAULT 0 COMMENT '通过文件数',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '生成时间',
    PRIMARY KEY (id),
    KEY idx_ranking_round_score (round_name, total_score),
    KEY idx_ranking_student_time (student_id, created_at),
    CONSTRAINT fk_ranking_student FOREIGN KEY (student_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛评分历史';

CREATE TABLE test_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '操作日志主键',
    student_id BIGINT NOT NULL COMMENT '学员',
    workspace_id BIGINT NOT NULL COMMENT '学员工作区',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    action_name VARCHAR(100) NOT NULL COMMENT '操作名称',
    detail VARCHAR(1000) NULL COMMENT '操作详情',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_operation_student_time (student_id, created_at),
    KEY idx_operation_module_time (module_name, created_at),
    CONSTRAINT fk_operation_student FOREIGN KEY (student_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_operation_workspace FOREIGN KEY (workspace_id) REFERENCES erp_workspace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员操作日志';
```

## 7. 关系说明

- 一个工作区拥有多个用户和多条基础资料、单据、库存及结算记录。
- 一个学员账号拥有一个独立学员工作区；普通账号共享系统工作区。
- 一个商品属于一个品牌、分类和单位。
- 一张业务单据包含多条明细；明细删除跟随单据级联删除。
- 退货单通过自关联指向采购入库或销售出库原单。
- 一张审核通过的采购/销售单据最多生成一条结算记录。
- 一条库存余额由工作区、仓库和商品三个维度唯一确定。
- 缺陷报告、测试文件、评分历史和操作日志均关联测试学员。

## 8. 索引策略

- 用户名全局唯一，保证登录查询稳定。
- 基础资料使用 `(workspace_id, code)` 唯一索引，支持学员工作区隔离。
- 单据使用 `(workspace_id, document_no)` 唯一索引，并按类型、状态、仓库和创建人建立查询索引。
- 库存使用 `(workspace_id, warehouse_id, product_id)` 唯一索引，防止重复余额行。
- 结算使用 `document_id` 唯一索引，防止一次审核重复创建结算。
- 消息、报告、文件、历史和日志的索引按用户/状态与时间组合，匹配当前列表页面的查询方式。
- 不为低选择性的单列状态字段单独建索引。

## 9. 事务与并发

### 9.1 单据审核

审核通过在一个 `@Transactional` 事务内完成：

1. 以写锁读取单据并确认状态为 `PENDING`。
2. 校验审核角色、工作区和仓库范围。
3. 以写锁读取涉及的库存余额。
4. 校验可用库存和退货剩余数量。
5. 更新库存余额。
6. 更新单据状态与审核信息。
7. 创建唯一结算记录和仓库消息。
8. 提交事务。

任一步失败均回滚，不允许出现“单据已通过但库存或结算未更新”的中间状态。

### 9.2 库存并发

- `inventory_balance` 使用唯一索引防止重复余额行。
- 审核时使用悲观写锁串行化同一库存键的修改。
- `version` 字段作为额外的乐观锁保护。
- 可用库存通过查询未完结的采购退货、销售出库和库存调拨明细动态计算，不单独持久化，避免缓存值失真。

### 9.3 重复提交

- 单据状态写锁阻止同一单据重复审核。
- 结算表的 `document_id` 唯一约束阻止重复结算。
- 学员账号、基础资料编码和单据编号均由唯一约束兜底。

## 10. Flyway 与演示数据

迁移文件按以下顺序执行：

- `V1__create_schema.sql`：全部表、外键和索引。
- `V2__seed_workspaces_users_and_master_data.sql`：系统工作区、内置账号、品牌、分类、单位、仓库、客户、供应商和商品。
- `V3__seed_bug_definitions.sql`：现有缺陷训练定义。

所有演示用户保留当前用户名，初始密码仍为 `123456`，SQL 中只保存预生成的 BCrypt 摘要。学员种子数据同时创建对应工作区并回填 `owner_user_id`。

JPA 配置使用 `ddl-auto: validate`，由 Flyway 唯一负责模式变更，禁止运行时自动改表。

## 11. Docker Compose 与配置

仓库根目录新增 `compose.yaml`，只启动 MySQL：

- 服务名：`mysql`
- 镜像：`mysql:8.4`
- 数据库：`erp`
- 端口：`3306`
- 持久化卷：`erp_mysql_data`
- 健康检查：`mysqladmin ping`
- 字符集：`utf8mb4`

根目录新增 `.env.example`，真实 `.env` 加入 `.gitignore`。后端通过以下环境变量连接：

- `DB_HOST`，默认 `127.0.0.1`
- `DB_PORT`，默认 `3306`
- `DB_NAME`，默认 `erp`
- `DB_USERNAME`，默认 `erp`
- `DB_PASSWORD`，本地开发值由 `.env` 提供
- `ERP_JWT_SECRET`

应用启动时如果数据库不可用或 Flyway 校验失败，应直接启动失败，不回退到内存存储。

## 12. 服务拆分与迁移顺序

### 阶段一：基础设施与认证

- 加入 JPA、MySQL、Flyway 和 Testcontainers 依赖。
- 建立工作区、用户实体与 Repository。
- 登录、当前用户、改密和头像改用数据库。

### 阶段二：基础资料

- 建立七类基础资料实体和服务。
- 保持 `/api/masterdata` 动态类型接口兼容。
- 将商品停用校验迁移为数据库查询。

### 阶段三：单据、库存、结算和消息

- 建立单据头、明细、库存、结算和消息实体。
- 迁移单据状态、退货关联、可用库存、仓库通知和结算规则。
- 用事务和锁替换内存集合的同步假设。

### 阶段四：测试竞赛

- 迁移缺陷、学员、报告、文件、评分历史和操作日志。
- 保留学员工作区隔离和真实缺陷开关逻辑。
- 删除剩余内存集合和种子初始化代码。

每个阶段完成后运行现有测试，避免把所有失败集中到最后处理。

## 13. API 兼容性

- 路径、HTTP 方法、`ApiResult` 包装结构保持不变。
- 角色编码、单据类型编码和状态编码保持不变。
- 前端继续使用当前 `/api` 代理，不要求同步重写页面。
- 数据库实体名称和外键不会直接出现在 API 响应中。
- 原有 `partnerId`、`relatedDocumentNo`、仓库和商品快照字段由 DTO 映射继续提供。

## 14. 错误处理

- 唯一约束冲突转换为明确的中文业务错误。
- 外键约束用于阻止删除仍被业务单据引用的数据。
- 学员删除采用软删除，禁用账号不能再次登录，历史业务和竞赛记录继续保留。
- 数据库连接或 Flyway 迁移失败时拒绝启动。
- 乐观锁或悲观锁冲突返回“数据已被其他操作更新，请刷新后重试”。
- 文件写入失败时不创建文件提交记录；数据库保存失败时删除本次已写入的文件。

## 15. 测试策略

- 使用 Testcontainers 启动与开发环境同类型的 MySQL。
- 测试启动时执行完整 Flyway 迁移，验证 SQL 可执行和 JPA 映射一致。
- 迁移现有 18 个后端测试，并在每个测试前清理业务数据、恢复种子数据。
- 新增数据库重启持久化测试、重复审核测试、并发库存测试、唯一约束测试和 Flyway 校验测试。
- 前端 API 契约不变，现有 8 个前端测试和生产构建继续作为回归门禁。

完成标准：

- `docker compose up -d mysql` 后数据库健康。
- 后端可连接 MySQL 并自动完成 Flyway 迁移。
- 数据在后端重启后仍然存在。
- 现有业务和竞赛测试全部通过。
- 新增事务、持久化和并发测试通过。
- 前端测试与生产构建通过。

## 16. 数据迁移说明

当前数据只存在于运行进程内，没有可稳定读取的历史数据库，因此不迁移运行中的临时内存数据。首次启用 MySQL 时由 Flyway 写入与当前一致的演示账号和基础资料。

旧的竞赛上传文件可能仍在磁盘，但其元数据随旧后端重启已经丢失，不自动扫描恢复。本次实现完成后，新上传文件的元数据和评分会持久化。
