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
