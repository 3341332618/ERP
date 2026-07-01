package com.erp.store;

import com.erp.common.BusinessException;
import com.erp.domain.ErpModels.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Service
public class ErpStore {
    private final PasswordEncoder passwordEncoder;
    private final AtomicLong ids = new AtomicLong(1000);
    private final Map<Long, User> users = new LinkedHashMap<>();
    private final Map<String, MasterRecord> master = new LinkedHashMap<>();
    private final Map<Long, DocumentRecord> documents = new LinkedHashMap<>();
    private final Map<String, BugDefinition> bugDefinitions = new LinkedHashMap<>();
    private final List<Message> messages = new ArrayList<>();
    private final List<StockBalance> stocks = new ArrayList<>();
    private final List<SettlementRecord> settlements = new ArrayList<>();
    private final List<BugReport> bugReports = new ArrayList<>();
    private final List<CompetitionFileSubmission> competitionFiles = new ArrayList<>();
    private final List<RankingHistory> rankingHistoryRecords = new ArrayList<>();
    private final List<StudentOperationLog> studentOperationLogs = new ArrayList<>();
    private int warehouseSeq = 1;
    private int customerSeq = 1;
    private int supplierSeq = 1;
    private int productSeq = 1;
    private int documentSeq = 1;
    private int settlementSeq = 1;

    public ErpStore(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        seed();
    }

    private void seed() {
        createUser("admin", "系统管理员", "13800000001", RoleCode.ADMIN, null);
        createUser("superadmin", "终极管理员", "13800000000", RoleCode.SUPER_ADMIN, null);
        createUser("purchase_manager", "采购主管", "13800000002", RoleCode.PURCHASE_MANAGER, null);
        createUser("purchase_staff", "采购专员", "13800000003", RoleCode.PURCHASE_STAFF, null);
        createUser("warehouse_manager", "仓库主管", "13800000004", RoleCode.WAREHOUSE_MANAGER, null);
        var warehouseStaff = createUser("warehouse_staff", "仓库专员", "13800000005", RoleCode.WAREHOUSE_STAFF, null);
        createUser("warehouse_staff_south", "华南仓库专员", "13800000009", RoleCode.WAREHOUSE_STAFF, null);
        createUser("sales_manager", "销售主管", "13800000006", RoleCode.SALES_MANAGER, null);
        createUser("sales_staff", "销售专员", "13800000007", RoleCode.SALES_STAFF, null);
        createUser("settlement_manager", "结算主管", "13800000008", RoleCode.SETTLEMENT_MANAGER, null);
        createStudentWorkspace("student01", "测试学员一", "13900000001", "123456");
        createStudentWorkspace("student02", "测试学员二", "13900000002", "123456");

        var brand = createMaster("brand", Map.of("name", "连想"));
        var category = createMaster("category", Map.of("name", "办公设备"));
        var unit = createMaster("unit", Map.of("name", "台"));
        var warehouse = createMaster("warehouse", Map.of(
            "name", "华东仓库",
            "phone", "13800000005",
            "address", "上海市浦东新区",
            "warehouseUserId", warehouseStaff.id.toString()
        ));
        warehouseStaff.warehouseId = warehouse.id;
        createMaster("customer", Map.of("name", "上海客户", "contact", "张三", "phone", "13800001000", "address", "上海市徐汇区"));
        createMaster("supplier", Map.of("name", "北京供应商", "contact", "李四", "phone", "13800002000", "address", "北京市朝阳区"));
        createMaster("product", Map.of(
            "name", "笔记本电脑",
            "categoryName", category.name,
            "brandName", brand.name,
            "unitName", unit.name,
            "purchasePrice", "4200",
            "salePrice", "5200"
        ));
        seedBugDefinitions();
    }

    private void seedBugDefinitions() {
        var mapper = new ObjectMapper();
        try (var stream = ErpStore.class.getResourceAsStream("/bug-training-definitions.json")) {
            if (stream == null) {
                throw new BusinessException("缺陷训练清单不存在。");
            }
            var definitions = mapper.readValue(stream, new TypeReference<List<BugDefinition>>() {
            });
            definitions.forEach(definition -> bugDefinitions.put(definition.id, definition));
        } catch (IOException ex) {
            throw new BusinessException("缺陷训练清单读取失败。");
        }
    }

    private User createUser(String username, String name, String phone, RoleCode role, Long warehouseId) {
        return createUser(username, name, phone, role, warehouseId, "123456");
    }

    private User createUser(String username, String name, String phone, RoleCode role, Long warehouseId, String password) {
        return createUser(username, name, phone, role, warehouseId, password, null);
    }

    private User createUser(String username, String name, String phone, RoleCode role, Long warehouseId, String password, Long workspaceOwnerId) {
        var user = new User();
        user.id = ids.incrementAndGet();
        user.username = username;
        user.passwordHash = passwordEncoder.encode(password);
        user.name = name;
        user.phone = phone;
        user.role = role;
        user.warehouseId = warehouseId;
        user.workspaceOwnerId = workspaceOwnerId;
        users.put(user.id, user);
        return user;
    }

    private User createStudentWorkspace(String username, String name, String phone, String password) {
        var student = createUser(username, name, phone, RoleCode.STUDENT, null, password);
        student.workspaceOwnerId = student.id;
        createStudentErpAccount(student, "_purchase_staff", "采购专员", RoleCode.PURCHASE_STAFF, password);
        var warehouseStaff = createStudentErpAccount(student, "_warehouse_staff", "仓库专员", RoleCode.WAREHOUSE_STAFF, password);
        createStudentErpAccount(student, "_sales_staff", "销售专员", RoleCode.SALES_STAFF, password);
        createStudentErpAccount(student, "_settlement_manager", "结算主管", RoleCode.SETTLEMENT_MANAGER, password);
        createMaster("warehouse", Map.of(
            "name", name + "ERP仓库",
            "phone", phone,
            "address", "学员独立ERP工作区",
            "warehouseUserId", warehouseStaff.id.toString()
        ), student.id, false);
        return student;
    }

    private User createStudentErpAccount(User student, String suffix, String roleLabel, RoleCode role, String password) {
        return createUser(student.username + suffix, student.name + roleLabel, student.phone, role, null, password, student.id);
    }

    public User userByUsername(String username) {
        return users.values().stream()
            .filter(user -> user.username.equals(username))
            .findFirst()
            .orElseThrow(() -> new BusinessException("登录账号不存在！"));
    }

    public User userById(Long id) {
        var user = users.get(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public User updateAvatar(Long userId, String avatarData) {
        var user = userById(userId);
        user.avatar = validatedImage(avatarData);
        return user;
    }

    public List<MenuNode> menus(RoleCode role) {
        var menus = new ArrayList<MenuNode>();
        if (role == RoleCode.ADMIN) {
            menus.add(baseInfoMenu());
            menus.add(purchaseMenu());
            menus.add(inventoryMenu());
            menus.add(salesMenu());
            menus.add(settlementMenu());
            menus.add(competitionAdminMenu());
        }
        if (role == RoleCode.SUPER_ADMIN) {
            menus.add(competitionSuperAdminMenu());
        }
        if (role == RoleCode.PURCHASE_MANAGER || role == RoleCode.PURCHASE_STAFF) {
            menus.add(purchaseMenu());
        }
        if (role == RoleCode.WAREHOUSE_MANAGER || role == RoleCode.WAREHOUSE_STAFF) {
            menus.add(inventoryMenu());
        }
        if (role == RoleCode.SALES_MANAGER || role == RoleCode.SALES_STAFF) {
            menus.add(salesMenu());
        }
        if (role == RoleCode.SETTLEMENT_MANAGER) {
            menus.add(settlementMenu());
        }
        if (role == RoleCode.STUDENT) {
            menus.add(competitionStudentMenu());
        }
        return menus;
    }

    private MenuNode baseInfoMenu() {
        return new MenuNode("基础信息管理", "/master/brand")
            .add(new MenuNode("商品品牌", "/master/brand"))
            .add(new MenuNode("商品分类", "/master/category"))
            .add(new MenuNode("商品单位", "/master/unit"))
            .add(new MenuNode("商品管理", "/master/product"))
            .add(new MenuNode("仓库信息", "/master/warehouse"))
            .add(new MenuNode("客户信息", "/master/customer"))
            .add(new MenuNode("供应商信息", "/master/supplier"));
    }

    private MenuNode purchaseMenu() {
        return new MenuNode("采购管理", "/purchase/inbound")
            .add(new MenuNode("采购入库", "/purchase/inbound"))
            .add(new MenuNode("采购退货", "/purchase/return"));
    }

    private MenuNode inventoryMenu() {
        return new MenuNode("库存管理", "/inventory/stock")
            .add(new MenuNode("库存分布", "/inventory/stock"))
            .add(new MenuNode("入库审核", "/inventory/inbound-audit"))
            .add(new MenuNode("出库审核", "/inventory/outbound-audit"))
            .add(new MenuNode("库存调拨", "/inventory/transfer"));
    }

    private MenuNode salesMenu() {
        return new MenuNode("销售管理", "/sales/outbound")
            .add(new MenuNode("销售出库", "/sales/outbound"))
            .add(new MenuNode("销售退货", "/sales/return"));
    }

    private MenuNode settlementMenu() {
        return new MenuNode("结算管理", "/settlement/income")
            .add(new MenuNode("收入结算", "/settlement/income"))
            .add(new MenuNode("支出结算", "/settlement/expense"));
    }

    private MenuNode competitionAdminMenu() {
        return new MenuNode("测试竞赛管理", "/competition/bugs")
            .add(new MenuNode("缺陷库发布", "/competition/bugs"))
            .add(new MenuNode("学员管理", "/competition/students"))
            .add(new MenuNode("学员报告评分", "/competition/reports"))
            .add(new MenuNode("竞赛排行榜", "/competition/rankings"));
    }

    private MenuNode competitionSuperAdminMenu() {
        return new MenuNode("测试竞赛后台", "/competition/students")
            .add(new MenuNode("学员管理", "/competition/students"))
            .add(new MenuNode("缺陷库发布", "/competition/bugs"))
            .add(new MenuNode("文件评阅", "/competition/files"))
            .add(new MenuNode("操作轨迹", "/competition/logs"))
            .add(new MenuNode("评分历史", "/competition/history"))
            .add(new MenuNode("竞赛排行榜", "/competition/rankings"));
    }

    private MenuNode competitionStudentMenu() {
        return new MenuNode("测试竞赛", "/competition/my-reports")
            .add(new MenuNode("我的缺陷报告", "/competition/my-reports"))
            .add(new MenuNode("提交测试文件", "/competition/submit-file"))
            .add(new MenuNode("我的提交文件", "/competition/my-files"))
            .add(new MenuNode("竞赛排行榜", "/competition/rankings"));
    }

    public List<Message> messages(Long userId) {
        return messages.stream()
            .filter(message -> message.userId.equals(userId))
            .sorted(Comparator.comparing((Message message) -> message.createTime).reversed())
            .toList();
    }

    public List<BugDefinition> bugDefinitions() {
        return new ArrayList<>(bugDefinitions.values());
    }

    public List<BugDefinition> bugDefinitions(Long operatorId) {
        requireAdmin(userById(operatorId));
        return bugDefinitions();
    }

    public List<StudentAccount> students(Long operatorId) {
        requireAdmin(userById(operatorId));
        return users.values().stream()
            .filter(user -> user.role == RoleCode.STUDENT)
            .sorted(Comparator.comparing((User user) -> user.createTime).reversed())
            .map(this::studentView)
            .toList();
    }

    public StudentAccount createStudent(Long operatorId, Map<String, String> payload) {
        requireAdmin(userById(operatorId));
        var username = requiredPayload(payload, "username", "学员账号必填，请重新输入。");
        var name = requiredPayload(payload, "name", "学员姓名必填，请重新输入。");
        var phone = requiredPayload(payload, "phone", "联系电话必填，请重新输入。");
        if (users.values().stream().anyMatch(user -> user.username.equals(username))) {
            throw new BusinessException("学员账号已存在，请重新输入。");
        }
        var password = textOrDefault(payload, "password", "123456");
        return studentView(createStudentWorkspace(username, name, phone, password));
    }

    public void deleteStudent(Long operatorId, Long studentId) {
        requireAdmin(userById(operatorId));
        var student = userById(studentId);
        if (student.role != RoleCode.STUDENT) {
            throw new BusinessException("只能删除测试学员账号。");
        }
        var removedUserIds = users.values().stream()
            .filter(user -> user.id.equals(student.id) || Objects.equals(user.workspaceOwnerId, student.id))
            .map(user -> user.id)
            .toList();
        users.keySet().removeIf(removedUserIds::contains);
        master.entrySet().removeIf(entry -> Objects.equals(entry.getValue().workspaceOwnerId, student.id));
        documents.entrySet().removeIf(entry -> Objects.equals(entry.getValue().workspaceOwnerId, student.id));
        stocks.removeIf(stock -> Objects.equals(stock.workspaceOwnerId, student.id));
        settlements.removeIf(settlement -> Objects.equals(settlement.workspaceOwnerId, student.id));
        messages.removeIf(message -> removedUserIds.contains(message.userId));
        bugReports.removeIf(report -> report.studentId.equals(student.id));
        competitionFiles.removeIf(file -> file.studentId.equals(student.id));
        studentOperationLogs.removeIf(log -> log.studentId.equals(student.id));
        rankingHistoryRecords.removeIf(history -> history.studentId.equals(student.id));
    }

    public BugDefinition publishBug(String bugId, boolean active, Long operatorId) {
        var operator = userById(operatorId);
        requireAdmin(operator);
        var bug = bugDefinition(bugId);
        bug.active = active;
        if (active) {
            bug.publisherId = operator.id;
            bug.publisherName = operator.name;
            bug.publishTime = LocalDateTime.now();
        } else {
            bug.publisherId = null;
            bug.publisherName = "";
            bug.publishTime = null;
        }
        return bug;
    }

    public List<BugDefinition> activeBugTasks(Long studentId) {
        var student = userById(studentId);
        requireStudent(student);
        return List.of();
    }

    public List<BugReport> bugReports(Long userId) {
        var user = userById(userId);
        return bugReports.stream()
            .filter(report -> user.role != RoleCode.STUDENT || report.studentId.equals(user.id))
            .sorted(Comparator.comparing((BugReport report) -> report.submitTime).reversed())
            .toList();
    }

    public BugReport submitBugReport(Long studentId, Map<String, String> payload) {
        var student = userById(studentId);
        requireStudent(student);
        var report = new BugReport();
        report.id = ids.incrementAndGet();
        report.bugId = "";
        report.bugSummary = "";
        report.moduleName = requiredPayload(payload, "moduleName", "测试模块必填，请重新输入。");
        report.title = requiredPayload(payload, "title", "缺陷标题必填，请重新输入。");
        report.reproduceSteps = requiredPayload(payload, "reproduceSteps", "复现步骤必填，请重新输入。");
        report.expectedResult = requiredPayload(payload, "expectedResult", "预期结果必填，请重新输入。");
        report.actualResult = requiredPayload(payload, "actualResult", "实际结果必填，请重新输入。");
        report.evidence = textOrDefault(payload, "evidence", "");
        report.studentId = student.id;
        report.studentName = student.name;
        bugReports.add(report);
        return report;
    }

    public BugReport reviewBugReport(Long reviewerId, Long reportId, Map<String, String> payload) {
        var reviewer = userById(reviewerId);
        requireAdmin(reviewer);
        var report = bugReport(reportId);
        var status = BugReportStatus.valueOf(payload.getOrDefault("status", BugReportStatus.APPROVED.name()));
        var score = Integer.parseInt(payload.getOrDefault("score", "0"));
        if (score < 0 || score > 100) {
            throw new BusinessException("评分范围应为0到100。");
        }
        report.status = status;
        report.score = status == BugReportStatus.APPROVED ? score : 0;
        report.reviewComment = payload.getOrDefault("reviewComment", "");
        report.reviewerId = reviewer.id;
        report.reviewerName = reviewer.name;
        report.reviewTime = LocalDateTime.now();
        return report;
    }

    public CompetitionFileSubmission submitCompetitionFile(Long studentId, Map<String, String> payload) {
        var student = userById(studentId);
        requireStudent(student);
        var fileName = requiredPayload(payload, "fileName", "测试附件名称必填，请重新上传。");
        if (!validCompetitionFile(fileName)) {
            throw new BusinessException("仅支持PDF、Word、Excel文件，请重新上传。");
        }
        var submission = new CompetitionFileSubmission();
        submission.id = ids.incrementAndGet();
        submission.title = requiredPayload(payload, "title", "提交标题必填，请重新输入。");
        submission.moduleName = requiredPayload(payload, "moduleName", "测试模块必填，请重新输入。");
        submission.bugId = "";
        submission.fileName = fileName;
        submission.contentType = textOrDefault(payload, "contentType", "application/octet-stream");
        submission.fileSize = Long.parseLong(textOrDefault(payload, "fileSize", "0"));
        submission.storagePath = requiredPayload(payload, "storagePath", "测试附件存储路径必填，请重新上传。");
        submission.studentId = student.id;
        submission.studentName = student.name;
        competitionFiles.add(submission);
        recordStudentOperation(student.id, submission.moduleName, "提交测试文件", submission.title + "：" + submission.fileName);
        return submission;
    }

    public List<CompetitionFileSubmission> competitionFiles(Long userId) {
        var user = userById(userId);
        return competitionFiles.stream()
            .filter(file -> user.role != RoleCode.STUDENT || file.studentId.equals(user.id))
            .sorted(Comparator
                .comparing((CompetitionFileSubmission file) -> file.submitTime).reversed()
                .thenComparing(file -> file.id, Comparator.reverseOrder()))
            .toList();
    }

    public CompetitionFileSubmission reviewCompetitionFile(Long reviewerId, Long submissionId, Map<String, String> payload) {
        var reviewer = userById(reviewerId);
        requireSuperAdmin(reviewer);
        var submission = competitionFile(submissionId);
        var status = CompetitionSubmissionStatus.valueOf(payload.getOrDefault("status", CompetitionSubmissionStatus.APPROVED.name()));
        var score = Integer.parseInt(payload.getOrDefault("score", "0"));
        if (score < 0 || score > 100) {
            throw new BusinessException("评分范围应为0到100。");
        }
        submission.status = status;
        submission.score = status == CompetitionSubmissionStatus.APPROVED ? score : 0;
        submission.roundName = textOrDefault(payload, "roundName", "默认轮次");
        submission.reviewComment = textOrDefault(payload, "reviewComment", "");
        submission.reviewerId = reviewer.id;
        submission.reviewerName = reviewer.name;
        submission.reviewTime = LocalDateTime.now();
        appendRankingHistory(submission.roundName, submission.studentId);
        return submission;
    }

    public List<RankingHistory> rankingHistory(Long operatorId) {
        requireSuperAdmin(userById(operatorId));
        return rankingHistoryRecords.stream()
            .sorted(Comparator
                .comparing((RankingHistory history) -> history.createTime).reversed()
                .thenComparing(history -> history.id, Comparator.reverseOrder()))
            .toList();
    }

    public StudentOperationLog recordStudentOperation(Long studentId, String moduleName, String actionName, String detail) {
        var student = userById(studentId);
        requireStudent(student);
        var log = new StudentOperationLog();
        log.id = ids.incrementAndGet();
        log.studentId = student.id;
        log.studentName = student.name;
        log.moduleName = moduleName;
        log.actionName = actionName;
        log.detail = detail;
        studentOperationLogs.add(log);
        return log;
    }

    public List<StudentOperationLog> studentOperationLogs(Long operatorId, Long studentId) {
        requireSuperAdmin(userById(operatorId));
        return studentOperationLogs.stream()
            .filter(log -> studentId == null || log.studentId.equals(studentId))
            .sorted(Comparator
                .comparing((StudentOperationLog log) -> log.createTime).reversed()
                .thenComparing(log -> log.id, Comparator.reverseOrder()))
            .toList();
    }

    public List<Map<String, Object>> bugRankings() {
        return users.values().stream()
            .filter(user -> user.role == RoleCode.STUDENT)
            .map(user -> {
                var approved = bugReports.stream()
                    .filter(report -> report.studentId.equals(user.id))
                    .filter(report -> report.status == BugReportStatus.APPROVED)
                    .toList();
                var approvedFiles = competitionFiles.stream()
                    .filter(file -> file.studentId.equals(user.id))
                    .filter(file -> file.status == CompetitionSubmissionStatus.APPROVED)
                    .toList();
                var totalScore = approved.stream()
                    .mapToInt(report -> report.score == null ? 0 : report.score)
                    .sum()
                    + approvedFiles.stream()
                    .mapToInt(file -> file.score == null ? 0 : file.score)
                    .sum();
                return Map.<String, Object>of(
                    "studentId", user.id,
                    "studentName", user.name,
                    "approvedReports", approved.size(),
                    "approvedFiles", approvedFiles.size(),
                    "totalScore", totalScore
                );
            })
            .filter(row -> (Integer) row.get("totalScore") > 0)
            .sorted(Comparator
                .comparing((Map<String, Object> row) -> (Integer) row.get("totalScore")).reversed()
                .thenComparing(row -> String.valueOf(row.get("studentName"))))
            .toList();
    }

    public List<MasterRecord> masters(String type, String keyword, String status) {
        return masters(type, keyword, status, null);
    }

    public List<MasterRecord> masters(String type, String keyword, String status, Long userId) {
        var ownerId = workspaceOwnerId(userId);
        return master.values().stream()
            .filter(record -> workspaceVisible(record.workspaceOwnerId, ownerId))
            .filter(record -> record.type.equals(type))
            .filter(record -> keyword == null || keyword.isBlank()
                || (record.code != null && record.code.contains(keyword))
                || record.name.contains(keyword))
            .filter(record -> status == null || status.isBlank() || record.status.name().equals(status))
            .sorted(Comparator.comparing((MasterRecord record) -> record.createTime).reversed())
            .toList();
    }

    public MasterRecord createMaster(String type, Map<String, String> payload) {
        return createMaster(type, payload, null);
    }

    public MasterRecord createMaster(String type, Map<String, String> payload, Long userId) {
        return createMaster(type, payload, userId, true);
    }

    private MasterRecord createMaster(String type, Map<String, String> payload, Long userId, boolean recordOperation) {
        var ownerId = workspaceOwnerId(userId);
        var name = validatedMasterName(type, null, payload.get("name"), ownerId);
        var record = new MasterRecord();
        record.id = ids.incrementAndGet();
        record.type = type;
        record.name = name;
        record.code = nextMasterCode(type, payload);
        record.categoryName = payload.get("categoryName");
        record.brandName = payload.get("brandName");
        record.unitName = payload.get("unitName");
        record.purchasePrice = money(payload.getOrDefault("purchasePrice", "0"));
        record.salePrice = money(payload.getOrDefault("salePrice", "0"));
        record.imageData = validatedImage(payload.get("imageData"));
        record.contact = payload.get("contact");
        record.phone = payload.get("phone");
        record.address = payload.get("address");
        record.settlementMethod = payload.getOrDefault("settlementMethod", defaultSettlement(type));
        if (payload.get("warehouseUserId") != null) {
            record.warehouseUserId = Long.valueOf(payload.get("warehouseUserId"));
        }
        record.workspaceOwnerId = ownerId;
        master.put(type + ":" + record.id, record);
        bindWarehouseStaff(record);
        if (recordOperation && ownerId != null) {
            recordStudentOperation(ownerId, displayName(type), "新增资料", record.name);
        }
        return record;
    }

    public MasterRecord updateMaster(String type, Long id, Map<String, String> payload) {
        return updateMaster(type, id, payload, null);
    }

    public MasterRecord updateMaster(String type, Long id, Map<String, String> payload, Long userId) {
        var ownerId = workspaceOwnerId(userId);
        var record = writableMasterRecord(type, id, ownerId);
        record.name = validatedMasterName(type, record.id, payload.get("name"), ownerId);
        record.categoryName = payload.getOrDefault("categoryName", record.categoryName);
        record.brandName = payload.getOrDefault("brandName", record.brandName);
        record.unitName = payload.getOrDefault("unitName", record.unitName);
        record.purchasePrice = money(payload.getOrDefault("purchasePrice", record.purchasePrice == null ? "0" : record.purchasePrice.toString()));
        record.salePrice = money(payload.getOrDefault("salePrice", record.salePrice == null ? "0" : record.salePrice.toString()));
        if (payload.containsKey("imageData")) {
            record.imageData = validatedImage(payload.get("imageData"));
        }
        record.contact = payload.getOrDefault("contact", record.contact);
        record.phone = payload.getOrDefault("phone", record.phone);
        record.address = payload.getOrDefault("address", record.address);
        record.updateTime = LocalDateTime.now();
        if ("warehouse".equals(type) && payload.containsKey("warehouseUserId")) {
            record.warehouseUserId = Long.valueOf(payload.get("warehouseUserId"));
            bindWarehouseStaff(record);
        }
        if ("product".equals(type)) {
            record.code = productCode(record.categoryName, record.brandName, productSeq - 1);
        }
        if (ownerId != null) {
            recordStudentOperation(ownerId, displayName(type), "修改资料", record.name);
        }
        return record;
    }

    public List<MasterRecord> importProducts(List<Map<String, String>> rows) {
        return importProducts(rows, null);
    }

    public List<MasterRecord> importProducts(List<Map<String, String>> rows, Long userId) {
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("导入文件内容不能为空。");
        }
        var errors = new ArrayList<String>();
        var normalizedRows = new ArrayList<Map<String, String>>();
        for (int i = 0; i < rows.size(); i++) {
            var rowNumber = i + 2;
            var row = rows.get(i);
            var name = importValue(row, "name", "商品名称");
            var categoryName = importValue(row, "categoryName", "商品分类");
            var brandName = importValue(row, "brandName", "商品品牌");
            var unitName = importValue(row, "unitName", "商品单位");
            var purchasePrice = importValue(row, "purchasePrice", "建议采购价（元）");
            var salePrice = importValue(row, "salePrice", "建议零售价（元）");
            if (name.isBlank()) {
                errors.add("第" + rowNumber + "行：商品名称必填");
            }
            if (categoryName.isBlank()) {
                errors.add("第" + rowNumber + "行：商品分类必填");
            }
            if (brandName.isBlank()) {
                errors.add("第" + rowNumber + "行：商品品牌必填");
            }
            if (unitName.isBlank()) {
                errors.add("第" + rowNumber + "行：商品单位必填");
            }
            if (!validMoney(purchasePrice)) {
                errors.add("第" + rowNumber + "行：建议采购价（元）输入有误");
            }
            if (!validMoney(salePrice)) {
                errors.add("第" + rowNumber + "行：建议零售价（元）输入有误");
            }
            normalizedRows.add(Map.of(
                "name", name,
                "categoryName", categoryName,
                "brandName", brandName,
                "unitName", unitName,
                "purchasePrice", purchasePrice,
                "salePrice", salePrice
            ));
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("；", errors));
        }
        return normalizedRows.stream()
            .map(row -> createMaster("product", row, userId))
            .toList();
    }

    public MasterRecord changeMasterStatus(String type, Long id, Status status) {
        return changeMasterStatus(type, id, status, null);
    }

    public MasterRecord changeMasterStatus(String type, Long id, Status status, Long userId) {
        var ownerId = workspaceOwnerId(userId);
        var record = writableMasterRecord(type, id, ownerId);
        if (status == Status.DISABLED) {
            assertCanDisable(record);
        }
        record.status = status;
        record.updateTime = LocalDateTime.now();
        if (ownerId != null) {
            recordStudentOperation(ownerId, displayName(type), "调整状态", record.name + "：" + status.label);
        }
        return record;
    }

    public MasterRecord masterRecord(String type, Long id) {
        var record = master.get(type + ":" + id);
        if (record == null) {
            throw new BusinessException(displayName(type) + "不存在");
        }
        return record;
    }

    public MasterRecord masterRecord(String type, Long id, Long userId) {
        var record = masterRecord(type, id);
        var ownerId = workspaceOwnerId(userId);
        if (!workspaceVisible(record.workspaceOwnerId, ownerId)) {
            throw new BusinessException(displayName(type) + "不存在");
        }
        return record;
    }

    private MasterRecord masterRecordForWorkspace(String type, Long id, Long ownerId) {
        var record = masterRecord(type, id);
        if (!workspaceVisible(record.workspaceOwnerId, ownerId)) {
            throw new BusinessException(displayName(type) + "不存在");
        }
        return record;
    }

    private MasterRecord writableMasterRecord(String type, Long id, Long ownerId) {
        var record = masterRecordForWorkspace(type, id, ownerId);
        if (ownerId == null || Objects.equals(record.workspaceOwnerId, ownerId)) {
            return record;
        }
        var copy = copyMasterRecord(record, ownerId);
        master.put(type + ":" + copy.id, copy);
        return copy;
    }

    private MasterRecord copyMasterRecord(MasterRecord source, Long ownerId) {
        var copy = new MasterRecord();
        copy.id = ids.incrementAndGet();
        copy.type = source.type;
        copy.code = source.code == null ? null : source.code + "-S" + ownerId;
        copy.name = source.name;
        copy.categoryName = source.categoryName;
        copy.brandName = source.brandName;
        copy.unitName = source.unitName;
        copy.purchasePrice = source.purchasePrice;
        copy.salePrice = source.salePrice;
        copy.imageData = source.imageData;
        copy.contact = source.contact;
        copy.phone = source.phone;
        copy.address = source.address;
        copy.settlementMethod = source.settlementMethod;
        copy.warehouseUserId = source.warehouseUserId;
        copy.workspaceOwnerId = ownerId;
        copy.status = source.status;
        copy.createTime = LocalDateTime.now();
        return copy;
    }

    private void assertCanDisable(MasterRecord record) {
        if ("product".equals(record.type)) {
            var hasStock = stocks.stream()
                .anyMatch(stock -> stock.productId.equals(record.id) && stock.actualQuantity.compareTo(BigDecimal.ZERO) > 0);
            if (hasStock) {
                throw new BusinessException("商品存在实际库存数量，无法禁用。");
            }
            if (hasOpenDocumentForProduct(record.id)) {
                throw new BusinessException("商品关联单据流转中，无法禁用。");
            }
        }
        if ("warehouse".equals(record.type)) {
            var hasStock = stocks.stream()
                .anyMatch(stock -> stock.warehouseId.equals(record.id) && stock.actualQuantity.compareTo(BigDecimal.ZERO) > 0);
            if (hasStock) {
                throw new BusinessException("仓库存在商品，无法禁用。");
            }
            if (hasOpenDocumentForWarehouse(record.id)) {
                throw new BusinessException("仓库关联单据流转中，无法禁用。");
            }
        }
        if ("customer".equals(record.type) || "supplier".equals(record.type)) {
            if (hasOpenDocumentForPartner(record.id)) {
                throw new BusinessException(displayName(record.type) + "关联单据流转中，无法禁用。");
            }
        }
    }

    private boolean hasOpenDocumentForProduct(Long productId) {
        return documents.values().stream()
            .filter(document -> document.status != DocumentStatus.APPROVED)
            .flatMap(document -> document.items.stream())
            .anyMatch(item -> item.productId.equals(productId));
    }

    private boolean hasOpenDocumentForWarehouse(Long warehouseId) {
        return documents.values().stream()
            .filter(document -> document.status != DocumentStatus.APPROVED)
            .anyMatch(document -> warehouseId.equals(document.warehouseId) || warehouseId.equals(document.targetWarehouseId));
    }

    private boolean hasOpenDocumentForPartner(Long partnerId) {
        return documents.values().stream()
            .filter(document -> document.status != DocumentStatus.APPROVED)
            .anyMatch(document -> partnerId.equals(document.partnerId));
    }

    public List<DocumentRecord> documents(String typeCode, Long userId) {
        var user = userById(userId);
        var type = DocumentType.byCode(typeCode);
        return documents.values().stream()
            .filter(document -> document.type == type)
            .filter(document -> canSeeDocument(user, document))
            .sorted(Comparator.comparing((DocumentRecord document) -> document.operationTime).reversed())
            .toList();
    }

    public DocumentRecord createSimpleDocument(String typeCode, Long userId) {
        return createDocument(typeCode, userId, Map.of());
    }

    public DocumentRecord createDocument(String typeCode, Long userId, Map<String, ?> payload) {
        var type = DocumentType.byCode(typeCode);
        var document = new DocumentRecord();
        document.id = ids.incrementAndGet();
        document.type = type;
        document.documentNo = type.prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "%04d".formatted(documentSeq++);
        var user = userById(userId);
        document.creatorId = user.id;
        document.creatorName = user.name;
        document.workspaceOwnerId = workspaceOwnerId(user.id);
        fillDocumentParties(document);
        var data = payload == null ? Map.<String, Object>of() : payload;
        applyDocumentPayload(document, data);
        document.items.add(data.isEmpty() ? defaultItem(document) : documentItem(document, data));
        recalc(document);
        documents.put(document.id, document);
        if (document.workspaceOwnerId != null) {
            recordStudentOperation(document.workspaceOwnerId, document.type.label, "新建单据", document.documentNo);
        }
        return document;
    }

    public DocumentRecord updateDocument(String typeCode, Long id, Long userId, Map<String, ?> payload) {
        var document = getDocument(id);
        if (document.type != DocumentType.byCode(typeCode)) {
            throw new BusinessException("单据类型不匹配");
        }
        if (!document.creatorId.equals(userId)) {
            throw new BusinessException("只能修改本人发起的单据");
        }
        if (document.status != DocumentStatus.DRAFT && document.status != DocumentStatus.REJECTED) {
            throw new BusinessException("当前状态不可修改");
        }
        var data = payload == null ? Map.<String, Object>of() : payload;
        fillDocumentParties(document);
        applyDocumentPayload(document, data);
        document.items.clear();
        document.items.add(data.isEmpty() ? defaultItem(document) : documentItem(document, data));
        document.operationTime = LocalDateTime.now();
        recalc(document);
        return document;
    }

    public DocumentRecord getDocument(Long id) {
        var document = documents.get(id);
        if (document == null) {
            throw new BusinessException("单据不存在");
        }
        return document;
    }

    public DocumentRecord getDocument(Long id, Long userId) {
        var document = getDocument(id);
        var user = userById(userId);
        if (!canSeeDocument(user, document)) {
            throw new BusinessException("单据不存在");
        }
        return document;
    }

    public DocumentRecord submitDocument(Long id, Long userId) {
        var document = getDocument(id);
        if (!document.creatorId.equals(userId)) {
            throw new BusinessException("只能提交本人发起的单据");
        }
        if (document.status != DocumentStatus.DRAFT && document.status != DocumentStatus.REJECTED) {
            throw new BusinessException("当前状态不可提交");
        }
        document.status = DocumentStatus.PENDING;
        document.operationTime = LocalDateTime.now();
        notifyWarehouse(document);
        if (document.workspaceOwnerId != null) {
            recordStudentOperation(document.workspaceOwnerId, document.type.label, "提交审核", document.documentNo);
        }
        return document;
    }

    public DocumentRecord deleteDocument(Long id, Long userId) {
        var document = getDocument(id);
        if (!document.creatorId.equals(userId)) {
            throw new BusinessException("只能删除本人发起的单据");
        }
        if (document.status != DocumentStatus.DRAFT && document.status != DocumentStatus.REJECTED) {
            throw new BusinessException("当前状态不可删除");
        }
        documents.remove(id);
        if (document.workspaceOwnerId != null) {
            recordStudentOperation(document.workspaceOwnerId, document.type.label, "删除单据", document.documentNo);
        }
        return document;
    }

    public List<DocumentRecord> auditList(String direction, Long userId) {
        var user = userById(userId);
        var ownerId = workspaceOwnerId(userId);
        Predicate<DocumentRecord> directionFilter = "inbound".equals(direction)
            ? document -> document.type.inbound || document.type == DocumentType.STOCK_TRANSFER
            : document -> document.type.outbound;
        return documents.values().stream()
            .filter(directionFilter)
            .filter(document -> document.status != DocumentStatus.DRAFT)
            .filter(document -> Objects.equals(document.workspaceOwnerId, ownerId))
            .filter(document -> user.role == RoleCode.WAREHOUSE_MANAGER
                || (user.role == RoleCode.WAREHOUSE_STAFF && documentAuditWarehouseMatches(user, document, direction)))
            .sorted(Comparator.comparing((DocumentRecord document) -> document.operationTime).reversed())
            .toList();
    }

    public synchronized DocumentRecord approve(Long id, Long auditorId) {
        var auditor = userById(auditorId);
        var document = getDocument(id);
        if (!Objects.equals(document.workspaceOwnerId, workspaceOwnerId(auditor.id))) {
            throw new BusinessException("只能审核所属工作区单据");
        }
        if (auditor.role != RoleCode.WAREHOUSE_STAFF) {
            throw new BusinessException("当前角色无审核权限");
        }
        if (document.status != DocumentStatus.PENDING) {
            throw new BusinessException("当前状态不可审核");
        }
        if (!documentWarehouseMatches(auditor, document)) {
            throw new BusinessException("只能审核所属仓库单据");
        }
        applyStock(document);
        document.status = DocumentStatus.APPROVED;
        document.auditorId = auditor.id;
        document.auditorName = auditor.name;
        document.auditTime = LocalDateTime.now();
        recalcAvailable(document.workspaceOwnerId);
        createSettlement(document);
        if (document.workspaceOwnerId != null) {
            recordStudentOperation(document.workspaceOwnerId, document.type.label, "审核通过", document.documentNo);
        }
        return document;
    }

    public DocumentRecord reject(Long id, Long auditorId, String reason) {
        require(reason, "拒绝原因必填，请重新输入。");
        if (reason.length() > 160 && !activeBug("BUG-0042")) {
            throw new BusinessException("拒绝原因不能超过160位字符，请重新输入。");
        }
        var auditor = userById(auditorId);
        var document = getDocument(id);
        if (!Objects.equals(document.workspaceOwnerId, workspaceOwnerId(auditor.id))) {
            throw new BusinessException("只能审核所属工作区单据");
        }
        if (auditor.role != RoleCode.WAREHOUSE_STAFF) {
            throw new BusinessException("当前角色无审核权限");
        }
        document.status = DocumentStatus.REJECTED;
        document.auditorId = auditor.id;
        document.auditorName = auditor.name;
        document.auditTime = LocalDateTime.now();
        document.rejectReason = reason;
        if (document.workspaceOwnerId != null) {
            recordStudentOperation(document.workspaceOwnerId, document.type.label, "审核拒绝", document.documentNo);
        }
        return document;
    }

    public List<StockBalance> stockList() {
        return stockList(null);
    }

    public List<StockBalance> stockList(Long userId) {
        var ownerId = workspaceOwnerId(userId);
        recalcAvailable(ownerId);
        return stocks.stream()
            .filter(stock -> Objects.equals(stock.workspaceOwnerId, ownerId))
            .filter(stock -> stock.actualQuantity.compareTo(BigDecimal.ZERO) > 0)
            .toList();
    }

    public List<Map<String, Object>> stockViews() {
        return stockViews(null);
    }

    public List<Map<String, Object>> stockViews(Long userId) {
        var ownerId = workspaceOwnerId(userId);
        recalcAvailable(ownerId);
        return stockList(userId).stream()
            .<Map<String, Object>>map(stock -> {
                var warehouse = masterRecordForWorkspace("warehouse", stock.warehouseId, ownerId);
                var product = masterRecordForWorkspace("product", stock.productId, ownerId);
                var row = new LinkedHashMap<String, Object>();
                row.put("warehouseId", stock.warehouseId);
                row.put("warehouseCode", warehouse.code);
                row.put("warehouseName", warehouse.name);
                row.put("productId", stock.productId);
                row.put("productCode", product.code);
                row.put("productName", product.name);
                row.put("categoryName", product.categoryName);
                row.put("brandName", product.brandName);
                row.put("unitName", product.unitName);
                row.put("imageData", product.imageData);
                row.put("actualQuantity", stock.actualQuantity);
                row.put("availableQuantity", stock.availableQuantity);
                return row;
            })
            .toList();
    }

    public List<SettlementRecord> settlements(String direction) {
        return settlements(direction, null);
    }

    public List<SettlementRecord> settlements(String direction, Long userId) {
        var ownerId = workspaceOwnerId(userId);
        return settlements.stream()
            .filter(record -> Objects.equals(record.workspaceOwnerId, ownerId))
            .filter(record -> record.direction.equals(direction))
            .sorted(Comparator.comparing((SettlementRecord record) -> record.createTime).reversed())
            .toList();
    }

    public Map<String, Object> settlementDetail(String direction, Long id) {
        return settlementDetail(direction, id, null);
    }

    public Map<String, Object> settlementDetail(String direction, Long id, Long userId) {
        var ownerId = workspaceOwnerId(userId);
        var settlement = settlements.stream()
            .filter(record -> Objects.equals(record.workspaceOwnerId, ownerId))
            .filter(record -> record.id.equals(id) && record.direction.equals(direction))
            .findFirst()
            .orElseThrow(() -> new BusinessException("结算单不存在"));
        var document = documents.values().stream()
            .filter(item -> Objects.equals(item.workspaceOwnerId, ownerId))
            .filter(item -> item.documentNo.equals(settlement.relatedDocumentNo))
            .findFirst()
            .orElseThrow(() -> new BusinessException("关联单据不存在"));
        var detail = new LinkedHashMap<String, Object>();
        detail.put("settlement", settlement);
        detail.put("document", document);
        return detail;
    }

    public List<MasterRecord> usersByRole(RoleCode role) {
        return usersByRole(role, null);
    }

    public List<MasterRecord> usersByRole(RoleCode role, Long userId) {
        var ownerId = workspaceOwnerId(userId);
        return users.values().stream()
            .filter(user -> user.role == role)
            .filter(user -> Objects.equals(workspaceOwnerId(user.id), ownerId))
            .map(user -> {
                var record = new MasterRecord();
                record.id = user.id;
                record.name = user.name;
                record.phone = user.phone;
                record.status = user.status;
                return record;
            })
            .toList();
    }

    private void fillDocumentParties(DocumentRecord document) {
        var warehouse = first("warehouse", document.workspaceOwnerId);
        document.warehouseId = warehouse.id;
        document.warehouseCode = warehouse.code;
        document.warehouseName = warehouse.name;
        if (document.type == DocumentType.STOCK_TRANSFER) {
            document.targetWarehouseId = warehouse.id;
            document.targetWarehouseCode = warehouse.code;
            document.targetWarehouseName = warehouse.name;
            return;
        }
        var partnerType = document.type == DocumentType.PURCHASE_INBOUND || document.type == DocumentType.PURCHASE_RETURN ? "supplier" : "customer";
        var partner = first(partnerType, document.workspaceOwnerId);
        document.partnerId = partner.id;
        document.partnerCode = partner.code;
        document.partnerName = partner.name;
    }

    private DocumentItem defaultItem(DocumentRecord document) {
        var product = first("product", document.workspaceOwnerId);
        var item = new DocumentItem();
        item.productId = product.id;
        item.productCode = product.code;
        item.productName = product.name;
        item.categoryName = product.categoryName;
        item.brandName = product.brandName;
        item.unitName = product.unitName;
        item.quantity = BigDecimal.TEN;
        item.price = defaultPrice(document.type, product);
        item.amount = item.quantity.multiply(item.price).setScale(2, RoundingMode.HALF_UP);
        item.availableQuantity = available(document.warehouseId, product.id, document.workspaceOwnerId);
        item.remark = document.type == DocumentType.STOCK_TRANSFER ? "正常调拨" : "";
        return item;
    }

    private void applyDocumentPayload(DocumentRecord document, Map<String, ?> payload) {
        if (isReturnType(document.type)) {
            applyRelatedDocument(document, text(payload, "relatedDocumentNo"));
            return;
        }
        if (payload.isEmpty()) {
            return;
        }
        var sourceWarehouse = payload.containsKey("warehouseId")
            ? masterRecordForWorkspace("warehouse", longRequired(payload, "warehouseId", document.type == DocumentType.STOCK_TRANSFER ? "调出仓库必填，请重新输入。" : "仓库必填，请重新输入。"), document.workspaceOwnerId)
            : masterRecordForWorkspace("warehouse", document.warehouseId, document.workspaceOwnerId);
        setWarehouse(document, sourceWarehouse);
        if (document.type == DocumentType.STOCK_TRANSFER) {
            var targetWarehouse = masterRecordForWorkspace("warehouse", longRequired(payload, "targetWarehouseId", "调入仓库必填，请重新输入。"), document.workspaceOwnerId);
            if (sourceWarehouse.id.equals(targetWarehouse.id)) {
                throw new BusinessException("调入仓库不能与调出仓库相同，请重新选择。");
            }
            document.targetWarehouseId = targetWarehouse.id;
            document.targetWarehouseCode = targetWarehouse.code;
            document.targetWarehouseName = targetWarehouse.name;
            return;
        }
        if (payload.containsKey("relatedDocumentNo")) {
            document.relatedDocumentNo = text(payload, "relatedDocumentNo");
        }
        if (payload.containsKey("partnerId")) {
            var partnerType = document.type == DocumentType.PURCHASE_INBOUND || document.type == DocumentType.PURCHASE_RETURN ? "supplier" : "customer";
            setPartner(document, masterRecordForWorkspace(partnerType, longRequired(payload, "partnerId", partnerLabel(document.type) + "必填，请重新输入。"), document.workspaceOwnerId));
        }
    }

    private DocumentItem documentItem(DocumentRecord document, Map<String, ?> payload) {
        var product = masterRecordForWorkspace("product", longRequired(payload, "productId", "商品必填，请重新输入。"), document.workspaceOwnerId);
        var item = new DocumentItem();
        item.productId = product.id;
        item.productCode = product.code;
        item.productName = product.name;
        item.categoryName = product.categoryName;
        item.brandName = product.brandName;
        item.unitName = product.unitName;
        item.quantity = quantityRequired(text(payload, "quantity"), quantityMessage(document.type));
        item.price = document.type == DocumentType.STOCK_TRANSFER
            ? BigDecimal.ZERO
            : money(payload.containsKey("price") ? text(payload, "price") : defaultPrice(document.type, product).toString());
        item.availableQuantity = available(document.warehouseId, product.id, document.workspaceOwnerId);
        item.remark = text(payload, "remark");
        if (document.type == DocumentType.STOCK_TRANSFER) {
            require(item.remark, "备注必填，请重新输入。");
        }
        validateReturnQuantity(document, item);
        if (requiresAvailableStock(document.type) && item.quantity.compareTo(item.availableQuantity) > 0) {
            throw new BusinessException("可用库存不足，请重新输入" + stockQuantityLabel(document.type) + "。");
        }
        return item;
    }

    private void setWarehouse(DocumentRecord document, MasterRecord warehouse) {
        document.warehouseId = warehouse.id;
        document.warehouseCode = warehouse.code;
        document.warehouseName = warehouse.name;
    }

    private void setPartner(DocumentRecord document, MasterRecord partner) {
        document.partnerId = partner.id;
        document.partnerCode = partner.code;
        document.partnerName = partner.name;
    }

    private BigDecimal defaultPrice(DocumentType type, MasterRecord product) {
        var price = type == DocumentType.SALES_OUTBOUND || type == DocumentType.SALES_RETURN
            ? product.salePrice
            : product.purchasePrice;
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return type == DocumentType.SALES_OUTBOUND || type == DocumentType.SALES_RETURN
                ? new BigDecimal("5200")
                : new BigDecimal("4200");
        }
        return price;
    }

    private boolean requiresAvailableStock(DocumentType type) {
        return type == DocumentType.PURCHASE_RETURN || type == DocumentType.SALES_OUTBOUND || type == DocumentType.STOCK_TRANSFER;
    }

    private String stockQuantityLabel(DocumentType type) {
        return switch (type) {
            case PURCHASE_RETURN -> "采退数量";
            case SALES_OUTBOUND -> "销售出库数量";
            case SALES_RETURN -> "销退数量";
            case STOCK_TRANSFER -> "调出数量";
            default -> "数量";
        };
    }

    private String quantityMessage(DocumentType type) {
        return stockQuantityLabel(type) + "必填，请重新输入。";
    }

    private String partnerLabel(DocumentType type) {
        return type == DocumentType.PURCHASE_INBOUND || type == DocumentType.PURCHASE_RETURN ? "供应商" : "客户";
    }

    private boolean isReturnType(DocumentType type) {
        return type == DocumentType.PURCHASE_RETURN || type == DocumentType.SALES_RETURN;
    }

    private void applyRelatedDocument(DocumentRecord document, String relatedDocumentNo) {
        require(relatedDocumentNo, relatedRequiredMessage(document.type));
        var related = findDocumentByNo(relatedDocumentNo, document.workspaceOwnerId);
        var requiredType = document.type == DocumentType.PURCHASE_RETURN ? DocumentType.PURCHASE_INBOUND : DocumentType.SALES_OUTBOUND;
        if (related == null
            || related.type != requiredType
            || related.status != DocumentStatus.APPROVED
            || !related.creatorId.equals(document.creatorId)) {
            throw new BusinessException(relatedExceptionMessage(document.type));
        }
        document.relatedDocumentNo = related.documentNo;
        document.warehouseId = related.warehouseId;
        document.warehouseCode = related.warehouseCode;
        document.warehouseName = related.warehouseName;
        document.partnerId = related.partnerId;
        document.partnerCode = related.partnerCode;
        document.partnerName = related.partnerName;
    }

    private DocumentRecord findDocumentByNo(String documentNo) {
        return findDocumentByNo(documentNo, null);
    }

    private DocumentRecord findDocumentByNo(String documentNo, Long ownerId) {
        return documents.values().stream()
            .filter(document -> document.documentNo.equals(documentNo))
            .filter(document -> Objects.equals(document.workspaceOwnerId, ownerId))
            .findFirst()
            .orElse(null);
    }

    private void validateReturnQuantity(DocumentRecord document, DocumentItem item) {
        if (!isReturnType(document.type)) {
            return;
        }
        var related = findDocumentByNo(document.relatedDocumentNo, document.workspaceOwnerId);
        var originalQuantity = related.items.stream()
            .filter(relatedItem -> relatedItem.productId.equals(item.productId))
            .map(relatedItem -> relatedItem.quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        var returnedQuantity = documents.values().stream()
            .filter(existing -> !existing.id.equals(document.id))
            .filter(existing -> existing.type == document.type)
            .filter(existing -> Objects.equals(existing.workspaceOwnerId, document.workspaceOwnerId))
            .filter(existing -> document.relatedDocumentNo.equals(existing.relatedDocumentNo))
            .filter(existing -> existing.status != DocumentStatus.REJECTED)
            .flatMap(existing -> existing.items.stream())
            .filter(existingItem -> existingItem.productId.equals(item.productId))
            .map(existingItem -> existingItem.quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        var remainingQuantity = originalQuantity.subtract(returnedQuantity);
        if (item.quantity.compareTo(remainingQuantity) > 0) {
            throw new BusinessException(stockQuantityLabel(document.type) + "输入有误，请重新输入。");
        }
    }

    private String relatedRequiredMessage(DocumentType type) {
        return type == DocumentType.PURCHASE_RETURN
            ? "关联采购入库单必填，请重新输入。"
            : "关联销售出库单必填，请重新输入。";
    }

    private String relatedExceptionMessage(DocumentType type) {
        return type == DocumentType.PURCHASE_RETURN
            ? "关联采购入库单号异常，请重新输入。"
            : "关联销售出库单号异常，请重新输入。";
    }

    private void recalc(DocumentRecord document) {
        document.totalAmount = document.items.stream()
            .peek(item -> item.amount = item.quantity.multiply(item.price).setScale(2, RoundingMode.HALF_UP))
            .map(item -> item.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean canSeeDocument(User user, DocumentRecord document) {
        var ownerId = workspaceOwnerId(user.id);
        if (!Objects.equals(document.workspaceOwnerId, ownerId)) {
            return false;
        }
        return switch (user.role) {
            case PURCHASE_MANAGER, SALES_MANAGER, WAREHOUSE_MANAGER, SETTLEMENT_MANAGER, ADMIN, SUPER_ADMIN -> true;
            case PURCHASE_STAFF, SALES_STAFF -> document.creatorId.equals(user.id);
            case WAREHOUSE_STAFF -> documentWarehouseMatches(user, document);
            case STUDENT -> Objects.equals(document.workspaceOwnerId, user.id);
        };
    }

    private boolean documentWarehouseMatches(User user, DocumentRecord document) {
        if (!Objects.equals(document.workspaceOwnerId, workspaceOwnerId(user.id))) {
            return false;
        }
        return user.warehouseId == null
            || user.warehouseId.equals(document.warehouseId)
            || user.warehouseId.equals(document.targetWarehouseId);
    }

    private boolean documentAuditWarehouseMatches(User user, DocumentRecord document, String direction) {
        if (!Objects.equals(document.workspaceOwnerId, workspaceOwnerId(user.id))) {
            return false;
        }
        if (user.warehouseId == null) {
            return true;
        }
        if (document.type == DocumentType.STOCK_TRANSFER) {
            return "inbound".equals(direction)
                ? user.warehouseId.equals(document.targetWarehouseId)
                : user.warehouseId.equals(document.warehouseId);
        }
        return user.warehouseId.equals(document.warehouseId);
    }

    private void notifyWarehouse(DocumentRecord document) {
        if (document.type == DocumentType.STOCK_TRANSFER) {
            notifyWarehouse(document, document.warehouseId, "出库审核");
            notifyWarehouse(document, document.targetWarehouseId, "入库审核");
            return;
        }
        notifyWarehouse(document, document.warehouseId, document.type.inbound ? "入库审核" : "出库审核");
    }

    private void notifyWarehouse(DocumentRecord document, Long warehouseId, String title) {
        users.values().stream()
            .filter(user -> user.role == RoleCode.WAREHOUSE_STAFF)
            .filter(user -> Objects.equals(workspaceOwnerId(user.id), document.workspaceOwnerId))
            .filter(user -> user.warehouseId == null || user.warehouseId.equals(warehouseId))
            .forEach(user -> {
                var message = new Message();
                message.id = ids.incrementAndGet();
                message.userId = user.id;
                message.title = title;
                message.content = "【" + message.title + "】单据号：" + document.documentNo + " 待审核 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                messages.add(message);
            });
    }

    private void applyStock(DocumentRecord document) {
        if (document.type == DocumentType.PURCHASE_INBOUND || document.type == DocumentType.SALES_RETURN) {
            document.items.forEach(item -> changeStock(document.warehouseId, item.productId, item.quantity, document.workspaceOwnerId));
        }
        if (document.type == DocumentType.PURCHASE_RETURN || document.type == DocumentType.SALES_OUTBOUND) {
            document.items.forEach(item -> changeStock(document.warehouseId, item.productId, item.quantity.negate(), document.workspaceOwnerId));
        }
        if (document.type == DocumentType.STOCK_TRANSFER) {
            document.items.forEach(item -> {
                changeStock(document.warehouseId, item.productId, item.quantity.negate(), document.workspaceOwnerId);
                changeStock(document.targetWarehouseId, item.productId, item.quantity, document.workspaceOwnerId);
            });
        }
    }

    private void changeStock(Long warehouseId, Long productId, BigDecimal delta) {
        changeStock(warehouseId, productId, delta, null);
    }

    private void changeStock(Long warehouseId, Long productId, BigDecimal delta, Long ownerId) {
        var stock = stock(warehouseId, productId, ownerId);
        var next = stock.actualQuantity.add(delta);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("可用库存不足，无法审核通过。");
        }
        stock.actualQuantity = next;
        stock.availableQuantity = available(warehouseId, productId, ownerId);
    }

    private StockBalance stock(Long warehouseId, Long productId) {
        return stock(warehouseId, productId, null);
    }

    private StockBalance stock(Long warehouseId, Long productId, Long ownerId) {
        return stocks.stream()
            .filter(item -> item.warehouseId.equals(warehouseId) && item.productId.equals(productId))
            .filter(item -> Objects.equals(item.workspaceOwnerId, ownerId))
            .findFirst()
            .orElseGet(() -> {
                var stock = new StockBalance();
                stock.warehouseId = warehouseId;
                stock.productId = productId;
                stock.workspaceOwnerId = ownerId;
                stocks.add(stock);
                return stock;
            });
    }

    private BigDecimal available(Long warehouseId, Long productId) {
        return available(warehouseId, productId, null);
    }

    private BigDecimal available(Long warehouseId, Long productId, Long ownerId) {
        var actual = stocks.stream()
            .filter(stock -> stock.warehouseId.equals(warehouseId) && stock.productId.equals(productId))
            .filter(stock -> Objects.equals(stock.workspaceOwnerId, ownerId))
            .map(stock -> stock.actualQuantity)
            .findFirst()
            .orElse(BigDecimal.ZERO);
        var reserved = documents.values().stream()
            .filter(document -> Objects.equals(document.workspaceOwnerId, ownerId))
            .filter(document -> EnumSet.of(DocumentStatus.DRAFT, DocumentStatus.PENDING, DocumentStatus.REJECTED).contains(document.status))
            .filter(document -> document.type == DocumentType.PURCHASE_RETURN || document.type == DocumentType.SALES_OUTBOUND || document.type == DocumentType.STOCK_TRANSFER)
            .filter(document -> document.warehouseId.equals(warehouseId))
            .flatMap(document -> document.items.stream())
            .filter(item -> item.productId.equals(productId))
            .map(item -> item.quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return actual.subtract(reserved);
    }

    private void recalcAvailable() {
        recalcAvailable(null);
    }

    private void recalcAvailable(Long ownerId) {
        stocks.stream()
            .filter(stock -> Objects.equals(stock.workspaceOwnerId, ownerId))
            .forEach(stock -> stock.availableQuantity = available(stock.warehouseId, stock.productId, ownerId));
    }

    private void createSettlement(DocumentRecord document) {
        var record = new SettlementRecord();
        record.id = ids.incrementAndGet();
        record.amount = document.totalAmount;
        record.relatedDocumentNo = document.documentNo;
        record.workspaceOwnerId = document.workspaceOwnerId;
        if (document.type == DocumentType.SALES_OUTBOUND) {
            record.direction = "income";
            record.documentType = "销出收入";
            record.settlementNo = settlementNo("IM");
        } else if (document.type == DocumentType.PURCHASE_RETURN) {
            record.direction = "income";
            record.documentType = "采退收入";
            record.settlementNo = settlementNo("IM");
        } else if (document.type == DocumentType.PURCHASE_INBOUND) {
            record.direction = "expense";
            record.documentType = "采入支出";
            record.settlementNo = settlementNo("OM");
        } else if (document.type == DocumentType.SALES_RETURN) {
            record.direction = "expense";
            record.documentType = "销退支出";
            record.settlementNo = settlementNo("OM");
        } else {
            return;
        }
        settlements.add(record);
    }

    private MasterRecord first(String type) {
        return first(type, null);
    }

    private MasterRecord first(String type, Long ownerId) {
        var exact = master.values().stream()
            .filter(record -> Objects.equals(record.workspaceOwnerId, ownerId))
            .filter(record -> record.type.equals(type))
            .findFirst()
            .orElse(null);
        if (exact != null) {
            return exact;
        }
        return master.values().stream()
            .filter(record -> record.workspaceOwnerId == null)
            .filter(record -> record.type.equals(type))
            .findFirst()
            .orElseThrow(() -> new BusinessException(displayName(type) + "不存在"));
    }

    private void bindWarehouseStaff(MasterRecord record) {
        if (!"warehouse".equals(record.type) || record.warehouseUserId == null) {
            return;
        }
        userById(record.warehouseUserId).warehouseId = record.id;
    }

    private String nextMasterCode(String type, Map<String, String> payload) {
        return switch (type) {
            case "warehouse" -> "CK%03d".formatted(warehouseSeq++);
            case "customer" -> "KH%03d".formatted(customerSeq++);
            case "supplier" -> "GYS%03d".formatted(supplierSeq++);
            case "product" -> productCode(payload.get("categoryName"), payload.get("brandName"), productSeq++);
            default -> null;
        };
    }

    private String productCode(String categoryName, String brandName, int seq) {
        return "SP" + initials(categoryName) + initials(brandName) + "%04d".formatted(seq);
    }

    private String initials(String value) {
        if (value == null || value.isBlank()) {
            return "XX";
        }
        var result = new StringBuilder();
        for (int i = 0; i < value.length() && result.length() < 2; i++) {
            char ch = value.charAt(i);
            if (ch <= 127 && Character.isLetterOrDigit(ch)) {
                result.append(Character.toUpperCase(ch));
            } else {
                result.append("X");
            }
        }
        while (result.length() < 2) {
            result.append("X");
        }
        return result.toString();
    }

    private String settlementNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "%04d".formatted(settlementSeq++);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean validMoney(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return money(value).compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String importValue(Map<String, String> row, String field, String... aliases) {
        var value = row.get(field);
        if (value != null) {
            return value.trim();
        }
        for (var alias : aliases) {
            value = row.get(alias);
            if (value != null) {
                return value.trim();
            }
        }
        return "";
    }

    private Long longRequired(Map<String, ?> payload, String key, String message) {
        var value = text(payload, key);
        require(value, message);
        return Long.valueOf(value);
    }

    private String text(Map<String, ?> payload, String key) {
        var value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private BigDecimal quantityRequired(String value, String message) {
        require(value, message);
        var quantity = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(message);
        }
        return quantity;
    }

    private String validatedImage(String imageData) {
        if (imageData == null || imageData.isBlank()) {
            return null;
        }
        var pngPrefix = "data:image/png;base64,";
        var jpegPrefix = "data:image/jpeg;base64,";
        var jpgPrefix = "data:image/jpg;base64,";
        String base64;
        if (imageData.startsWith(pngPrefix)) {
            base64 = imageData.substring(pngPrefix.length());
        } else if (imageData.startsWith(jpegPrefix)) {
            base64 = imageData.substring(jpegPrefix.length());
        } else if (imageData.startsWith(jpgPrefix)) {
            base64 = imageData.substring(jpgPrefix.length());
        } else {
            throw new BusinessException("请上传JPG/PNG类型格式文件");
        }
        try {
            if (Base64.getDecoder().decode(base64).length > 200 * 1024) {
                throw new BusinessException("上传文件大小不能超过200KB");
            }
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("请上传JPG/PNG类型格式文件");
        }
        return imageData;
    }

    private String defaultSettlement(String type) {
        return "supplier".equals(type) ? "货到付款" : "付款发货";
    }

    private BugDefinition bugDefinition(String bugId) {
        var bug = bugDefinitions.get(bugId);
        if (bug == null) {
            throw new BusinessException("缺陷不存在。");
        }
        return bug;
    }

    private BugReport bugReport(Long reportId) {
        return bugReports.stream()
            .filter(report -> report.id.equals(reportId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("缺陷报告不存在。"));
    }

    private CompetitionFileSubmission competitionFile(Long submissionId) {
        return competitionFiles.stream()
            .filter(file -> file.id.equals(submissionId))
            .findFirst()
            .orElseThrow(() -> new BusinessException("测试文件提交记录不存在。"));
    }

    private boolean validCompetitionFile(String fileName) {
        var lower = fileName == null ? "" : fileName.toLowerCase();
        return lower.endsWith(".pdf")
            || lower.endsWith(".doc")
            || lower.endsWith(".docx")
            || lower.endsWith(".xls")
            || lower.endsWith(".xlsx");
    }

    private void appendRankingHistory(String roundName, Long studentId) {
        var student = userById(studentId);
        var approvedReports = bugReports.stream()
            .filter(report -> report.studentId.equals(studentId))
            .filter(report -> report.status == BugReportStatus.APPROVED)
            .toList();
        var approvedFiles = competitionFiles.stream()
            .filter(file -> file.studentId.equals(studentId))
            .filter(file -> file.status == CompetitionSubmissionStatus.APPROVED)
            .toList();
        var history = new RankingHistory();
        history.id = ids.incrementAndGet();
        history.roundName = roundName;
        history.studentId = student.id;
        history.studentName = student.name;
        history.approvedReports = approvedReports.size();
        history.approvedFiles = approvedFiles.size();
        history.totalScore = approvedReports.stream()
            .mapToInt(report -> report.score == null ? 0 : report.score)
            .sum()
            + approvedFiles.stream()
            .mapToInt(file -> file.score == null ? 0 : file.score)
            .sum();
        rankingHistoryRecords.add(history);
    }

    private boolean activeBug(String bugId) {
        var bug = bugDefinitions.get(bugId);
        return bug != null && bug.active;
    }

    private Long workspaceOwnerId(Long userId) {
        if (userId == null) {
            return null;
        }
        var user = userById(userId);
        if (user.role == RoleCode.STUDENT) {
            return user.id;
        }
        return user.workspaceOwnerId;
    }

    private boolean workspaceVisible(Long recordOwnerId, Long currentOwnerId) {
        return currentOwnerId == null
            ? recordOwnerId == null
            : recordOwnerId == null || Objects.equals(recordOwnerId, currentOwnerId);
    }

    private void requireAdmin(User user) {
        if (user.role != RoleCode.ADMIN && user.role != RoleCode.SUPER_ADMIN) {
            throw new BusinessException("当前角色无缺陷发布权限。");
        }
    }

    private void requireSuperAdmin(User user) {
        if (user.role != RoleCode.SUPER_ADMIN) {
            throw new BusinessException("当前角色不是终极管理员。");
        }
    }

    private void requireStudent(User user) {
        if (user.role != RoleCode.STUDENT) {
            throw new BusinessException("当前角色不是测试学员。");
        }
    }

    private String requiredPayload(Map<String, String> payload, String key, String message) {
        var value = payload == null ? null : payload.get(key);
        require(value, message);
        return value.trim();
    }

    private String textOrDefault(Map<String, String> payload, String key, String fallback) {
        if (payload == null) {
            return fallback;
        }
        var value = payload.get(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private StudentAccount studentView(User user) {
        var student = new StudentAccount();
        student.id = user.id;
        student.username = user.username;
        student.name = user.name;
        student.phone = user.phone;
        student.status = user.status;
        student.createTime = user.createTime;
        student.erpAccounts = users.values().stream()
            .filter(account -> Objects.equals(account.workspaceOwnerId, user.id))
            .filter(account -> account.id != null && !account.id.equals(user.id))
            .sorted(Comparator.comparing(account -> account.username))
            .map(account -> {
                var workspaceAccount = new StudentWorkspaceAccount();
                workspaceAccount.id = account.id;
                workspaceAccount.username = account.username;
                workspaceAccount.name = account.name;
                workspaceAccount.role = account.role;
                workspaceAccount.roleName = account.role.label;
                return workspaceAccount;
            })
            .toList();
        return student;
    }

    private String validatedMasterName(String type, Long currentId, String value, Long ownerId) {
        require(value, displayName(type) + "名称必填，请重新输入。");
        var name = value.trim();
        if ("category".equals(type) && name.length() > 16 && !activeBug("BUG-0002")) {
            throw new BusinessException("商品分类名称输入有误，请重新输入。");
        }
        if ("product".equals(type) && name.length() > 16 && !activeBug("BUG-0011")) {
            throw new BusinessException("商品名称输入有误，请重新输入。");
        }
        if ("brand".equals(type) && !activeBug("BUG-0004")) {
            var duplicate = master.values().stream()
                .filter(record -> record.type.equals(type))
                .filter(record -> workspaceVisible(record.workspaceOwnerId, ownerId))
                .filter(record -> currentId == null || !record.id.equals(currentId))
                .anyMatch(record -> record.name.equals(name));
            if (duplicate) {
                throw new BusinessException("商品品牌名称不唯一，请重新输入。");
            }
        }
        return name;
    }

    private String displayName(String type) {
        return switch (type) {
            case "brand" -> "商品品牌";
            case "category" -> "商品分类";
            case "unit" -> "商品单位";
            case "product" -> "商品";
            case "warehouse" -> "仓库";
            case "customer" -> "客户";
            case "supplier" -> "供应商";
            default -> "数据";
        };
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
    }
}
