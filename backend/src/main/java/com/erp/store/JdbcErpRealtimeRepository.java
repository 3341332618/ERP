package com.erp.store;

import com.erp.domain.ErpModels.*;
import com.erp.mapper.WorkspaceMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@ConditionalOnProperty(name = "erp.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcErpRealtimeRepository implements ErpRealtimeRepository {
    private final JdbcTemplate jdbc;
    private final WorkspaceMapper workspaceMapper;

    public JdbcErpRealtimeRepository(JdbcTemplate jdbc, WorkspaceMapper workspaceMapper) {
        this.jdbc = jdbc;
        this.workspaceMapper = workspaceMapper;
    }

    @Override
    public boolean hasBusinessData() {
        return workspaceMapper.selectCount(null) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public ErpStoreData loadBusinessData() {
        var users = loadUsers();
        var warehouseUsers = loadWarehouseUserAssignments();
        var masters = loadMasters(warehouseUsers);
        var documents = loadDocuments();
        var bugDefinitions = loadBugDefinitions();
        var messages = loadMessages();
        var stocks = loadStocks();
        var settlements = loadSettlements();
        var bugReports = loadBugReports();
        var competitionFiles = loadCompetitionFiles();
        var rankingHistory = loadRankingHistory();
        var operationLogs = loadOperationLogs();

        return new ErpStoreData(
            maxId(users, masters, documents, messages, stocks, settlements, bugReports, competitionFiles, rankingHistory, operationLogs),
            nextCodeSequence(masters, "warehouse", "CK"),
            nextCodeSequence(masters, "customer", "KH"),
            nextCodeSequence(masters, "supplier", "GYS"),
            nextProductSequence(masters),
            nextTailSequence(documents.stream().map(document -> document.documentNo).toList()),
            nextTailSequence(settlements.stream().map(settlement -> settlement.settlementNo).toList()),
            users,
            masters,
            documents,
            bugDefinitions,
            messages,
            stocks,
            settlements,
            bugReports,
            competitionFiles,
            rankingHistory,
            operationLogs
        );
    }

    @Override
    @Transactional
    public void insertInitialData(ErpStoreData data) {
        if (hasBusinessData()) {
            return;
        }
        insertWorkspace(1L, "SYSTEM", "系统工作区", "SYSTEM", null, LocalDateTime.now());
        data.users().stream()
            .filter(user -> user.role == RoleCode.STUDENT)
            .sorted(Comparator.comparing(user -> user.id))
            .forEach(student -> insertWorkspace(student.id, "STU-" + student.username, student.name + "ERP工作区", "STUDENT", null, student.createTime));

        data.users().stream()
            .sorted(Comparator.comparing(user -> user.id))
            .forEach(user -> insertUser(user, false));
        insertMasters(data.masters());
        updateUserWarehouseAssignments(data.users());
        data.users().stream()
            .filter(user -> user.role == RoleCode.STUDENT)
            .forEach(student -> updateWorkspaceOwner(student.id, student.id));

        data.bugDefinitions().forEach(this::upsertBugDefinition);
        data.documents().forEach(this::insertDocument);
        data.stocks().forEach(this::upsertStock);
        data.settlements().forEach(settlement -> insertSettlement(settlement, documentByNo(workspaceId(settlement.workspaceOwnerId), settlement.relatedDocumentNo)));
        data.messages().forEach(this::insertMessage);
        data.bugReports().forEach(report -> insertBugReport(report, workspaceId(report.studentId)));
        data.competitionFiles().forEach(file -> insertCompetitionFile(file, workspaceId(file.studentId)));
        data.rankingHistoryRecords().forEach(history -> insertRankingHistory(history, workspaceId(history.studentId)));
        data.studentOperationLogs().forEach(log -> insertOperationLog(log, workspaceId(log.studentId)));
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        insertUser(user, true);
    }

    @Override
    @Transactional
    public void updateUserPasswordHashes(Map<Long, String> passwordHashes) {
        passwordHashes.forEach((userId, passwordHash) -> {
            var updated = jdbc.update("""
                update sys_user
                set password_hash = ?, updated_at = current_timestamp(6)
                where id = ?
                """, passwordHash, userId);
            if (updated != 1) {
                throw new IllegalStateException("Password update target does not exist: " + userId);
            }
        });
    }

    @Override
    @Transactional
    public void insertStudentWorkspace(User student, Collection<User> workspaceUsers, Collection<MasterRecord> workspaceMasters) {
        insertWorkspace(student.id, "STU-" + student.username, student.name + "ERP工作区", "STUDENT", null, student.createTime);
        uniqueUsers(workspaceUsers).forEach(user -> insertUser(user, false));
        insertMasters(workspaceMasters);
        updateUserWarehouseAssignments(workspaceUsers);
        updateWorkspaceOwner(student.id, student.id);
    }

    @Override
    @Transactional
    public void deleteStudentWorkspace(Long studentId, Collection<Long> removedUserIds) {
        var workspaceId = workspaceId(studentId);
        jdbc.update("delete from finance_settlement where workspace_id = ?", workspaceId);
        jdbc.update("delete from biz_document_item where workspace_id = ?", workspaceId);
        jdbc.update("delete from biz_document where workspace_id = ?", workspaceId);
        jdbc.update("delete from inventory_balance where workspace_id = ?", workspaceId);
        jdbc.update("delete from test_ranking_history where workspace_id = ?", workspaceId);
        jdbc.update("delete from test_operation_log where workspace_id = ?", workspaceId);
        jdbc.update("delete from test_file_submission where workspace_id = ?", workspaceId);
        jdbc.update("delete from test_bug_report where workspace_id = ?", workspaceId);
        deleteByIds("delete from sys_message where user_id in ", removedUserIds);
        jdbc.update("update erp_workspace set owner_user_id = null where id = ?", workspaceId);
        deleteByIds("delete from sys_user where id in ", removedUserIds);
        jdbc.update("delete from master_product where workspace_id = ?", workspaceId);
        jdbc.update("delete from master_customer where workspace_id = ?", workspaceId);
        jdbc.update("delete from master_supplier where workspace_id = ?", workspaceId);
        jdbc.update("delete from master_warehouse where workspace_id = ?", workspaceId);
        jdbc.update("delete from master_brand where workspace_id = ?", workspaceId);
        jdbc.update("delete from master_category where workspace_id = ?", workspaceId);
        jdbc.update("delete from master_unit where workspace_id = ?", workspaceId);
        jdbc.update("delete from erp_workspace where id = ?", workspaceId);
    }

    @Override
    @Transactional
    public void updateBugDefinition(BugDefinition bug) {
        upsertBugDefinition(bug);
    }

    @Override
    @Transactional
    public void insertBugReport(BugReport report, Long workspaceId) {
        jdbc.update("""
            insert into test_bug_report (
                id, bug_id, student_id, workspace_id, bug_summary_snapshot, module_name, title,
                reproduce_steps, expected_result, actual_result, evidence, report_status, score,
                review_comment, reviewer_id, submit_time, review_time
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                bug_id = values(bug_id),
                bug_summary_snapshot = values(bug_summary_snapshot),
                module_name = values(module_name),
                title = values(title),
                reproduce_steps = values(reproduce_steps),
                expected_result = values(expected_result),
                actual_result = values(actual_result),
                evidence = values(evidence),
                report_status = values(report_status),
                score = values(score),
                review_comment = values(review_comment),
                reviewer_id = values(reviewer_id),
                review_time = values(review_time)
            """,
            report.id, blankToNull(report.bugId), report.studentId, workspaceId,
            text(report.bugSummary), report.moduleName, report.title,
            report.reproduceSteps, report.expectedResult, report.actualResult, report.evidence,
            report.status.name(), report.score == null ? 0 : report.score,
            report.reviewComment, report.reviewerId, ts(report.submitTime), ts(report.reviewTime)
        );
    }

    @Override
    @Transactional
    public void updateBugReport(BugReport report) {
        jdbc.update("""
            update test_bug_report
            set report_status = ?, score = ?, review_comment = ?, reviewer_id = ?, review_time = ?
            where id = ?
            """,
            report.status.name(), report.score == null ? 0 : report.score,
            report.reviewComment, report.reviewerId, ts(report.reviewTime), report.id
        );
    }

    @Override
    @Transactional
    public void insertCompetitionFile(CompetitionFileSubmission file, Long workspaceId) {
        jdbc.update("""
            insert into test_file_submission (
                id, student_id, workspace_id, bug_id, title, module_name, file_name,
                content_type, file_size, storage_path, submission_status, score,
                round_name, review_comment, reviewer_id, submit_time, review_time
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                bug_id = values(bug_id),
                title = values(title),
                module_name = values(module_name),
                file_name = values(file_name),
                content_type = values(content_type),
                file_size = values(file_size),
                storage_path = values(storage_path),
                submission_status = values(submission_status),
                score = values(score),
                round_name = values(round_name),
                review_comment = values(review_comment),
                reviewer_id = values(reviewer_id),
                review_time = values(review_time)
            """,
            file.id, file.studentId, workspaceId, blankToNull(file.bugId), file.title, file.moduleName,
            file.fileName, file.contentType, file.fileSize, file.storagePath, file.status.name(),
            file.score == null ? 0 : file.score, file.roundName, file.reviewComment,
            file.reviewerId, ts(file.submitTime), ts(file.reviewTime)
        );
    }

    @Override
    @Transactional
    public void updateCompetitionFile(CompetitionFileSubmission file) {
        jdbc.update("""
            update test_file_submission
            set submission_status = ?, score = ?, round_name = ?, review_comment = ?, reviewer_id = ?, review_time = ?
            where id = ?
            """,
            file.status.name(), file.score == null ? 0 : file.score, file.roundName,
            file.reviewComment, file.reviewerId, ts(file.reviewTime), file.id
        );
    }

    @Override
    @Transactional
    public void insertRankingHistory(RankingHistory history, Long workspaceId) {
        jdbc.update("""
            insert into test_ranking_history (
                id, round_name, student_id, workspace_id, total_score, approved_reports, approved_files, created_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                round_name = values(round_name),
                total_score = values(total_score),
                approved_reports = values(approved_reports),
                approved_files = values(approved_files)
            """,
            history.id, history.roundName, history.studentId, workspaceId,
            history.totalScore == null ? 0 : history.totalScore,
            history.approvedReports == null ? 0 : history.approvedReports,
            history.approvedFiles == null ? 0 : history.approvedFiles,
            ts(history.createTime)
        );
    }

    @Override
    @Transactional
    public void insertOperationLog(StudentOperationLog log, Long workspaceId) {
        jdbc.update("""
            insert into test_operation_log (
                id, student_id, workspace_id, module_name, action_name, detail, created_at
            ) values (?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                module_name = values(module_name),
                action_name = values(action_name),
                detail = values(detail)
            """,
            log.id, log.studentId, workspaceId, log.moduleName, log.actionName, log.detail, ts(log.createTime)
        );
    }

    @Override
    @Transactional
    public void upsertMasters(Collection<MasterRecord> records) {
        records.forEach(this::upsertMaster);
    }

    @Override
    @Transactional
    public void upsertMaster(MasterRecord record) {
        switch (record.type) {
            case "brand" -> upsertBrand(record);
            case "category" -> upsertCategory(record);
            case "unit" -> upsertUnit(record);
            case "warehouse" -> upsertWarehouse(record);
            case "customer" -> upsertCustomer(record);
            case "supplier" -> upsertSupplier(record);
            case "product" -> upsertProduct(record);
            default -> throw new IllegalArgumentException("Unsupported master type: " + record.type);
        }
    }

    @Override
    @Transactional
    public void deleteMaster(MasterRecord record) {
        var table = masterTable(record.type);
        jdbc.update("delete from " + table + " where workspace_id = ? and id = ?", workspaceId(record.workspaceOwnerId), record.id);
    }

    @Override
    @Transactional
    public void insertDocument(DocumentRecord document) {
        upsertDocumentHeader(document);
        replaceDocumentItems(document);
    }

    @Override
    @Transactional
    public void updateDocument(DocumentRecord document) {
        upsertDocumentHeader(document);
        replaceDocumentItems(document);
    }

    @Override
    @Transactional
    public void deleteDocument(DocumentRecord document) {
        var workspaceId = workspaceId(document.workspaceOwnerId);
        jdbc.update("delete from biz_document_item where workspace_id = ? and document_id = ?", workspaceId, document.id);
        jdbc.update("delete from biz_document where workspace_id = ? and id = ?", workspaceId, document.id);
    }

    @Override
    @Transactional
    public void upsertStock(StockBalance stock) {
        jdbc.update("""
            insert into inventory_balance (workspace_id, warehouse_id, product_id, actual_quantity)
            values (?, ?, ?, ?)
            on duplicate key update actual_quantity = values(actual_quantity), version = version + 1
            """,
            workspaceId(stock.workspaceOwnerId), stock.warehouseId, stock.productId, stock.actualQuantity
        );
    }

    @Override
    @Transactional
    public void insertSettlement(SettlementRecord settlement, DocumentRecord document) {
        var workspaceId = workspaceId(settlement.workspaceOwnerId);
        var documentId = document == null ? documentIdByNo(workspaceId, settlement.relatedDocumentNo) : document.id;
        jdbc.update("""
            insert into finance_settlement (
                id, workspace_id, document_id, direction, settlement_no, document_type_label, amount, created_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                direction = values(direction),
                settlement_no = values(settlement_no),
                document_type_label = values(document_type_label),
                amount = values(amount)
            """,
            settlement.id, workspaceId, documentId, settlement.direction, settlement.settlementNo,
            settlement.documentType, settlement.amount, ts(settlement.createTime)
        );
    }

    @Override
    @Transactional
    public void insertMessage(Message message) {
        jdbc.update("""
            insert into sys_message (id, user_id, title, content, is_read, created_at)
            values (?, ?, ?, ?, ?, ?)
            on duplicate key update
                title = values(title),
                content = values(content),
                is_read = values(is_read)
            """,
            message.id, message.userId, message.title, message.content, message.read, ts(message.createTime)
        );
    }

    private List<User> loadUsers() {
        return jdbc.query("""
            select id, workspace_id, warehouse_id, username, password_hash, name, phone, avatar,
                   role_code, status, created_at
            from sys_user
            order by id
            """, (rs, rowNum) -> {
            var user = new User();
            user.id = rs.getLong("id");
            var workspaceId = rs.getLong("workspace_id");
            user.username = rs.getString("username");
            user.passwordHash = rs.getString("password_hash");
            user.name = rs.getString("name");
            user.phone = rs.getString("phone");
            user.avatar = rs.getString("avatar");
            user.role = RoleCode.valueOf(rs.getString("role_code"));
            user.status = Status.valueOf(rs.getString("status"));
            user.createTime = localDateTime(rs, "created_at");
            user.warehouseId = nullableLong(rs, "warehouse_id");
            user.workspaceOwnerId = user.role == RoleCode.STUDENT ? user.id : ownerFromWorkspace(workspaceId);
            return user;
        });
    }

    private Map<String, Long> loadWarehouseUserAssignments() {
        var assignments = new LinkedHashMap<String, Long>();
        jdbc.query("""
            select workspace_id, warehouse_id, min(id) as user_id
            from sys_user
            where warehouse_id is not null
            group by workspace_id, warehouse_id
            """, rs -> {
            assignments.put(key(rs.getLong("workspace_id"), rs.getLong("warehouse_id")), rs.getLong("user_id"));
        });
        return assignments;
    }

    private List<MasterRecord> loadMasters(Map<String, Long> warehouseUsers) {
        var records = new ArrayList<MasterRecord>();
        records.addAll(loadBrandMasters());
        records.addAll(loadCategoryMasters());
        records.addAll(loadUnitMasters());
        records.addAll(loadWarehouseMasters(warehouseUsers));
        records.addAll(loadCustomerMasters());
        records.addAll(loadSupplierMasters());
        records.addAll(loadProductMasters());
        records.sort(Comparator.comparing(record -> record.id));
        return records;
    }

    private List<MasterRecord> loadBrandMasters() {
        return jdbc.query("""
            select id, workspace_id, code, name, status, created_at, updated_at
            from master_brand
            """, (rs, rowNum) -> simpleMaster(rs, "brand"));
    }

    private List<MasterRecord> loadCategoryMasters() {
        return jdbc.query("""
            select id, workspace_id, code, name, status, created_at, updated_at
            from master_category
            """, (rs, rowNum) -> simpleMaster(rs, "category"));
    }

    private List<MasterRecord> loadUnitMasters() {
        return jdbc.query("""
            select id, workspace_id, code, name, status, created_at, updated_at
            from master_unit
            """, (rs, rowNum) -> simpleMaster(rs, "unit"));
    }

    private List<MasterRecord> loadWarehouseMasters(Map<String, Long> warehouseUsers) {
        return jdbc.query("""
            select id, workspace_id, code, name, phone, address, status, created_at, updated_at
            from master_warehouse
            """, (rs, rowNum) -> {
            var record = simpleMaster(rs, "warehouse");
            record.phone = rs.getString("phone");
            record.address = rs.getString("address");
            record.warehouseUserId = warehouseUsers.get(key(rs.getLong("workspace_id"), record.id));
            return record;
        });
    }

    private List<MasterRecord> loadCustomerMasters() {
        return jdbc.query("""
            select id, workspace_id, code, name, contact, phone, address, settlement_method, status, created_at, updated_at
            from master_customer
            """, (rs, rowNum) -> partnerMaster(rs, "customer"));
    }

    private List<MasterRecord> loadSupplierMasters() {
        return jdbc.query("""
            select id, workspace_id, code, name, contact, phone, address, settlement_method, status, created_at, updated_at
            from master_supplier
            """, (rs, rowNum) -> partnerMaster(rs, "supplier"));
    }

    private List<MasterRecord> loadProductMasters() {
        return jdbc.query("""
            select p.id, p.workspace_id, p.code, p.name, c.name as category_name, b.name as brand_name,
                   u.name as unit_name, p.purchase_price, p.sale_price, p.image_data,
                   p.status, p.created_at, p.updated_at
            from master_product p
            join master_category c on c.workspace_id = p.workspace_id and c.id = p.category_id
            join master_brand b on b.workspace_id = p.workspace_id and b.id = p.brand_id
            join master_unit u on u.workspace_id = p.workspace_id and u.id = p.unit_id
            """, (rs, rowNum) -> {
            var record = simpleMaster(rs, "product");
            record.categoryName = rs.getString("category_name");
            record.brandName = rs.getString("brand_name");
            record.unitName = rs.getString("unit_name");
            record.purchasePrice = rs.getBigDecimal("purchase_price");
            record.salePrice = rs.getBigDecimal("sale_price");
            record.imageData = rs.getString("image_data");
            return record;
        });
    }

    private List<DocumentRecord> loadDocuments() {
        return jdbc.query("""
            select d.*, rd.document_no as related_document_no, creator.name as creator_name, auditor.name as auditor_name
            from biz_document d
            left join biz_document rd on rd.workspace_id = d.workspace_id and rd.id = d.related_document_id
            join sys_user creator on creator.workspace_id = d.workspace_id and creator.id = d.creator_id
            left join sys_user auditor on auditor.workspace_id = d.workspace_id and auditor.id = d.auditor_id
            order by d.id
            """, (rs, rowNum) -> {
            var document = new DocumentRecord();
            document.id = rs.getLong("id");
            var workspaceId = rs.getLong("workspace_id");
            document.workspaceOwnerId = ownerFromWorkspace(workspaceId);
            document.type = DocumentType.valueOf(rs.getString("document_type"));
            document.documentNo = rs.getString("document_no");
            document.relatedDocumentNo = rs.getString("related_document_no");
            document.warehouseId = rs.getLong("warehouse_id");
            document.warehouseCode = rs.getString("warehouse_code_snapshot");
            document.warehouseName = rs.getString("warehouse_name_snapshot");
            document.targetWarehouseId = nullableLong(rs, "target_warehouse_id");
            document.targetWarehouseCode = rs.getString("target_warehouse_code_snapshot");
            document.targetWarehouseName = rs.getString("target_warehouse_name_snapshot");
            var supplierId = nullableLong(rs, "supplier_id");
            var customerId = nullableLong(rs, "customer_id");
            document.partnerId = supplierId == null ? customerId : supplierId;
            document.partnerCode = rs.getString("partner_code_snapshot");
            document.partnerName = rs.getString("partner_name_snapshot");
            document.totalAmount = rs.getBigDecimal("total_amount");
            document.status = DocumentStatus.valueOf(rs.getString("document_status"));
            document.creatorId = rs.getLong("creator_id");
            document.creatorName = rs.getString("creator_name");
            document.operationTime = localDateTime(rs, "operation_time");
            document.auditorId = nullableLong(rs, "auditor_id");
            document.auditorName = rs.getString("auditor_name");
            document.auditTime = localDateTime(rs, "audit_time");
            document.rejectReason = rs.getString("reject_reason");
            document.items = loadDocumentItems(workspaceId, document.id);
            return document;
        });
    }

    private List<DocumentItem> loadDocumentItems(Long workspaceId, Long documentId) {
        return jdbc.query("""
            select product_id, product_code_snapshot, product_name_snapshot, category_name_snapshot,
                   brand_name_snapshot, unit_name_snapshot, quantity, price, amount, remark
            from biz_document_item
            where workspace_id = ? and document_id = ?
            order by id
            """, (rs, rowNum) -> {
            var item = new DocumentItem();
            item.productId = rs.getLong("product_id");
            item.productCode = rs.getString("product_code_snapshot");
            item.productName = rs.getString("product_name_snapshot");
            item.categoryName = rs.getString("category_name_snapshot");
            item.brandName = rs.getString("brand_name_snapshot");
            item.unitName = rs.getString("unit_name_snapshot");
            item.quantity = rs.getBigDecimal("quantity");
            item.price = rs.getBigDecimal("price");
            item.amount = rs.getBigDecimal("amount");
            item.remark = rs.getString("remark");
            return item;
        }, workspaceId, documentId);
    }

    private List<BugDefinition> loadBugDefinitions() {
        return jdbc.query("""
            select b.*, u.name as publisher_name
            from test_bug_definition b
            left join sys_user u on u.id = b.publisher_id
            order by b.id
            """, (rs, rowNum) -> {
            var bug = new BugDefinition();
            bug.id = rs.getString("id");
            bug.roleName = rs.getString("role_name");
            bug.moduleName = rs.getString("module_name");
            bug.functionName = rs.getString("function_name");
            bug.summary = rs.getString("summary");
            bug.reproduceSteps = rs.getString("reproduce_steps");
            bug.expectedResult = rs.getString("expected_result");
            bug.actualResult = rs.getString("actual_result");
            bug.severity = rs.getString("severity");
            bug.active = rs.getBoolean("active");
            bug.publisherId = nullableLong(rs, "publisher_id");
            bug.publisherName = rs.getString("publisher_name");
            bug.publishTime = localDateTime(rs, "publish_time");
            return bug;
        });
    }

    private List<Message> loadMessages() {
        return jdbc.query("""
            select id, user_id, title, content, is_read, created_at
            from sys_message
            order by id
            """, (rs, rowNum) -> {
            var message = new Message();
            message.id = rs.getLong("id");
            message.userId = rs.getLong("user_id");
            message.title = rs.getString("title");
            message.content = rs.getString("content");
            message.read = rs.getBoolean("is_read");
            message.createTime = localDateTime(rs, "created_at");
            return message;
        });
    }

    private List<StockBalance> loadStocks() {
        return jdbc.query("""
            select workspace_id, warehouse_id, product_id, actual_quantity
            from inventory_balance
            order by id
            """, (rs, rowNum) -> {
            var stock = new StockBalance();
            stock.workspaceOwnerId = ownerFromWorkspace(rs.getLong("workspace_id"));
            stock.warehouseId = rs.getLong("warehouse_id");
            stock.productId = rs.getLong("product_id");
            stock.actualQuantity = rs.getBigDecimal("actual_quantity");
            return stock;
        });
    }

    private List<SettlementRecord> loadSettlements() {
        return jdbc.query("""
            select s.id, s.workspace_id, s.direction, s.settlement_no, s.document_type_label,
                   s.amount, d.document_no, s.created_at
            from finance_settlement s
            join biz_document d on d.workspace_id = s.workspace_id and d.id = s.document_id
            order by s.id
            """, (rs, rowNum) -> {
            var settlement = new SettlementRecord();
            settlement.id = rs.getLong("id");
            settlement.workspaceOwnerId = ownerFromWorkspace(rs.getLong("workspace_id"));
            settlement.direction = rs.getString("direction");
            settlement.settlementNo = rs.getString("settlement_no");
            settlement.documentType = rs.getString("document_type_label");
            settlement.amount = rs.getBigDecimal("amount");
            settlement.relatedDocumentNo = rs.getString("document_no");
            settlement.createTime = localDateTime(rs, "created_at");
            return settlement;
        });
    }

    private List<BugReport> loadBugReports() {
        return jdbc.query("""
            select r.*, student.name as student_name, reviewer.name as reviewer_name
            from test_bug_report r
            join sys_user student on student.workspace_id = r.workspace_id and student.id = r.student_id
            left join sys_user reviewer on reviewer.id = r.reviewer_id
            order by r.id
            """, (rs, rowNum) -> {
            var report = new BugReport();
            report.id = rs.getLong("id");
            report.bugId = text(rs.getString("bug_id"));
            report.bugSummary = rs.getString("bug_summary_snapshot");
            report.moduleName = rs.getString("module_name");
            report.title = rs.getString("title");
            report.reproduceSteps = rs.getString("reproduce_steps");
            report.expectedResult = rs.getString("expected_result");
            report.actualResult = rs.getString("actual_result");
            report.evidence = rs.getString("evidence");
            report.studentId = rs.getLong("student_id");
            report.studentName = rs.getString("student_name");
            report.status = BugReportStatus.valueOf(rs.getString("report_status"));
            report.score = rs.getInt("score");
            report.reviewComment = rs.getString("review_comment");
            report.reviewerId = nullableLong(rs, "reviewer_id");
            report.reviewerName = rs.getString("reviewer_name");
            report.submitTime = localDateTime(rs, "submit_time");
            report.reviewTime = localDateTime(rs, "review_time");
            return report;
        });
    }

    private List<CompetitionFileSubmission> loadCompetitionFiles() {
        return jdbc.query("""
            select f.*, student.name as student_name, reviewer.name as reviewer_name
            from test_file_submission f
            join sys_user student on student.workspace_id = f.workspace_id and student.id = f.student_id
            left join sys_user reviewer on reviewer.id = f.reviewer_id
            order by f.id
            """, (rs, rowNum) -> {
            var file = new CompetitionFileSubmission();
            file.id = rs.getLong("id");
            file.studentId = rs.getLong("student_id");
            file.studentName = rs.getString("student_name");
            file.bugId = text(rs.getString("bug_id"));
            file.title = rs.getString("title");
            file.moduleName = rs.getString("module_name");
            file.fileName = rs.getString("file_name");
            file.contentType = rs.getString("content_type");
            file.fileSize = rs.getLong("file_size");
            file.storagePath = rs.getString("storage_path");
            file.status = CompetitionSubmissionStatus.valueOf(rs.getString("submission_status"));
            file.score = rs.getInt("score");
            file.roundName = rs.getString("round_name");
            file.reviewComment = rs.getString("review_comment");
            file.reviewerId = nullableLong(rs, "reviewer_id");
            file.reviewerName = rs.getString("reviewer_name");
            file.submitTime = localDateTime(rs, "submit_time");
            file.reviewTime = localDateTime(rs, "review_time");
            return file;
        });
    }

    private List<RankingHistory> loadRankingHistory() {
        return jdbc.query("""
            select h.*, student.name as student_name
            from test_ranking_history h
            join sys_user student on student.workspace_id = h.workspace_id and student.id = h.student_id
            order by h.id
            """, (rs, rowNum) -> {
            var history = new RankingHistory();
            history.id = rs.getLong("id");
            history.roundName = rs.getString("round_name");
            history.studentId = rs.getLong("student_id");
            history.studentName = rs.getString("student_name");
            history.totalScore = rs.getInt("total_score");
            history.approvedReports = rs.getInt("approved_reports");
            history.approvedFiles = rs.getInt("approved_files");
            history.createTime = localDateTime(rs, "created_at");
            return history;
        });
    }

    private List<StudentOperationLog> loadOperationLogs() {
        return jdbc.query("""
            select l.*, student.name as student_name
            from test_operation_log l
            join sys_user student on student.workspace_id = l.workspace_id and student.id = l.student_id
            order by l.id
            """, (rs, rowNum) -> {
            var log = new StudentOperationLog();
            log.id = rs.getLong("id");
            log.studentId = rs.getLong("student_id");
            log.studentName = rs.getString("student_name");
            log.moduleName = rs.getString("module_name");
            log.actionName = rs.getString("action_name");
            log.detail = rs.getString("detail");
            log.createTime = localDateTime(rs, "created_at");
            return log;
        });
    }

    private void insertWorkspace(Long id, String code, String name, String type, Long ownerUserId, LocalDateTime createdAt) {
        jdbc.update("""
            insert into erp_workspace (id, code, name, workspace_type, owner_user_id, status, created_at, updated_at)
            values (?, ?, ?, ?, ?, 'ENABLED', ?, ?)
            on duplicate key update
                name = values(name),
                workspace_type = values(workspace_type),
                status = values(status),
                updated_at = values(updated_at)
            """,
            id, code, name, type, ownerUserId, ts(createdAt), ts(createdAt)
        );
    }

    private void updateWorkspaceOwner(Long workspaceId, Long ownerUserId) {
        jdbc.update("update erp_workspace set owner_user_id = ?, updated_at = current_timestamp(6) where id = ?", ownerUserId, workspaceId);
    }

    private void insertUser(User user, boolean includeWarehouse) {
        jdbc.update("""
            insert into sys_user (
                id, workspace_id, warehouse_id, username, password_hash, name, phone, avatar,
                role_code, status, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                warehouse_id = values(warehouse_id),
                username = values(username),
                password_hash = values(password_hash),
                name = values(name),
                phone = values(phone),
                avatar = values(avatar),
                role_code = values(role_code),
                status = values(status),
                updated_at = values(updated_at)
            """,
            user.id, workspaceId(user), includeWarehouse ? user.warehouseId : null, user.username,
            user.passwordHash, user.name, user.phone, user.avatar, user.role.name(), user.status.name(),
            ts(user.createTime), ts(LocalDateTime.now())
        );
    }

    private void updateUserWarehouseAssignments(Collection<User> users) {
        uniqueUsers(users).stream()
            .filter(user -> user.warehouseId != null)
            .forEach(user -> jdbc.update(
                "update sys_user set warehouse_id = ?, updated_at = current_timestamp(6) where id = ?",
                user.warehouseId, user.id
            ));
    }

    private void insertMasters(Collection<MasterRecord> records) {
        List.of("brand", "category", "unit", "warehouse", "customer", "supplier", "product")
            .forEach(type -> records.stream()
                .filter(record -> type.equals(record.type))
                .sorted(Comparator.comparing(record -> record.id))
                .forEach(this::upsertMaster));
    }

    private void upsertBrand(MasterRecord record) {
        upsertSimpleMaster("master_brand", record);
    }

    private void upsertCategory(MasterRecord record) {
        upsertSimpleMaster("master_category", record);
    }

    private void upsertUnit(MasterRecord record) {
        upsertSimpleMaster("master_unit", record);
    }

    private void upsertSimpleMaster(String table, MasterRecord record) {
        jdbc.update("""
            insert into %s (id, workspace_id, code, name, status, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                code = values(code),
                name = values(name),
                status = values(status),
                updated_at = values(updated_at)
            """.formatted(table),
            record.id, workspaceId(record.workspaceOwnerId), text(record.code), record.name,
            record.status.name(), ts(record.createTime), ts(timeOrNow(record.updateTime))
        );
    }

    private void upsertWarehouse(MasterRecord record) {
        jdbc.update("""
            insert into master_warehouse (id, workspace_id, code, name, phone, address, status, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                code = values(code),
                name = values(name),
                phone = values(phone),
                address = values(address),
                status = values(status),
                updated_at = values(updated_at)
            """,
            record.id, workspaceId(record.workspaceOwnerId), text(record.code), record.name,
            record.phone, record.address, record.status.name(), ts(record.createTime), ts(timeOrNow(record.updateTime))
        );
        var workspaceId = workspaceId(record.workspaceOwnerId);
        jdbc.update(
            "update sys_user set warehouse_id = null, updated_at = current_timestamp(6) where workspace_id = ? and warehouse_id = ?",
            workspaceId,
            record.id
        );
        if (record.warehouseUserId != null) {
            jdbc.update(
                "update sys_user set warehouse_id = ?, updated_at = current_timestamp(6) where workspace_id = ? and id = ?",
                record.id,
                workspaceId,
                record.warehouseUserId
            );
        }
    }

    private void upsertCustomer(MasterRecord record) {
        upsertPartner("master_customer", record);
    }

    private void upsertSupplier(MasterRecord record) {
        upsertPartner("master_supplier", record);
    }

    private void upsertPartner(String table, MasterRecord record) {
        jdbc.update("""
            insert into %s (
                id, workspace_id, code, name, contact, phone, address, settlement_method, status, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                code = values(code),
                name = values(name),
                contact = values(contact),
                phone = values(phone),
                address = values(address),
                settlement_method = values(settlement_method),
                status = values(status),
                updated_at = values(updated_at)
            """.formatted(table),
            record.id, workspaceId(record.workspaceOwnerId), text(record.code), record.name,
            record.contact, record.phone, record.address, record.settlementMethod,
            record.status.name(), ts(record.createTime), ts(timeOrNow(record.updateTime))
        );
    }

    private void upsertProduct(MasterRecord record) {
        var workspaceId = workspaceId(record.workspaceOwnerId);
        var categoryId = masterIdByName("master_category", workspaceId, record.categoryName);
        var brandId = masterIdByName("master_brand", workspaceId, record.brandName);
        var unitId = masterIdByName("master_unit", workspaceId, record.unitName);
        jdbc.update("""
            insert into master_product (
                id, workspace_id, category_id, brand_id, unit_id, code, name,
                purchase_price, sale_price, image_data, status, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                category_id = values(category_id),
                brand_id = values(brand_id),
                unit_id = values(unit_id),
                code = values(code),
                name = values(name),
                purchase_price = values(purchase_price),
                sale_price = values(sale_price),
                image_data = values(image_data),
                status = values(status),
                updated_at = values(updated_at)
            """,
            record.id, workspaceId, categoryId, brandId, unitId, text(record.code), record.name,
            record.purchasePrice, record.salePrice, record.imageData, record.status.name(),
            ts(record.createTime), ts(timeOrNow(record.updateTime))
        );
    }

    private void upsertBugDefinition(BugDefinition bug) {
        jdbc.update("""
            insert into test_bug_definition (
                id, role_name, module_name, function_name, summary, reproduce_steps,
                expected_result, actual_result, severity, active, publisher_id, publish_time
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                role_name = values(role_name),
                module_name = values(module_name),
                function_name = values(function_name),
                summary = values(summary),
                reproduce_steps = values(reproduce_steps),
                expected_result = values(expected_result),
                actual_result = values(actual_result),
                severity = values(severity),
                active = values(active),
                publisher_id = values(publisher_id),
                publish_time = values(publish_time)
            """,
            bug.id, bug.roleName, bug.moduleName, bug.functionName, bug.summary, bug.reproduceSteps,
            bug.expectedResult, bug.actualResult, bug.severity, bug.active, bug.publisherId, ts(bug.publishTime)
        );
    }

    private void upsertDocumentHeader(DocumentRecord document) {
        var workspaceId = workspaceId(document.workspaceOwnerId);
        var relatedDocumentId = document.relatedDocumentNo == null || document.relatedDocumentNo.isBlank()
            ? null
            : documentIdByNo(workspaceId, document.relatedDocumentNo);
        var supplierId = document.type == DocumentType.PURCHASE_INBOUND || document.type == DocumentType.PURCHASE_RETURN ? document.partnerId : null;
        var customerId = document.type == DocumentType.SALES_OUTBOUND || document.type == DocumentType.SALES_RETURN ? document.partnerId : null;
        jdbc.update("""
            insert into biz_document (
                id, workspace_id, document_type, document_no, related_document_id,
                warehouse_id, target_warehouse_id, supplier_id, customer_id,
                warehouse_code_snapshot, warehouse_name_snapshot,
                target_warehouse_code_snapshot, target_warehouse_name_snapshot,
                partner_code_snapshot, partner_name_snapshot, total_amount,
                document_status, creator_id, operation_time, auditor_id, audit_time, reject_reason
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                related_document_id = values(related_document_id),
                warehouse_id = values(warehouse_id),
                target_warehouse_id = values(target_warehouse_id),
                supplier_id = values(supplier_id),
                customer_id = values(customer_id),
                warehouse_code_snapshot = values(warehouse_code_snapshot),
                warehouse_name_snapshot = values(warehouse_name_snapshot),
                target_warehouse_code_snapshot = values(target_warehouse_code_snapshot),
                target_warehouse_name_snapshot = values(target_warehouse_name_snapshot),
                partner_code_snapshot = values(partner_code_snapshot),
                partner_name_snapshot = values(partner_name_snapshot),
                total_amount = values(total_amount),
                document_status = values(document_status),
                operation_time = values(operation_time),
                auditor_id = values(auditor_id),
                audit_time = values(audit_time),
                reject_reason = values(reject_reason),
                version = version + 1
            """,
            document.id, workspaceId, document.type.name(), document.documentNo, relatedDocumentId,
            document.warehouseId, document.targetWarehouseId, supplierId, customerId,
            document.warehouseCode, document.warehouseName, document.targetWarehouseCode, document.targetWarehouseName,
            document.partnerCode, document.partnerName, document.totalAmount, document.status.name(),
            document.creatorId, ts(document.operationTime), document.auditorId, ts(document.auditTime), document.rejectReason
        );
    }

    private void replaceDocumentItems(DocumentRecord document) {
        var workspaceId = workspaceId(document.workspaceOwnerId);
        jdbc.update("delete from biz_document_item where workspace_id = ? and document_id = ?", workspaceId, document.id);
        for (var item : document.items) {
            jdbc.update("""
                insert into biz_document_item (
                    workspace_id, document_id, product_id, product_code_snapshot, product_name_snapshot,
                    category_name_snapshot, brand_name_snapshot, unit_name_snapshot,
                    quantity, price, amount, remark
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                workspaceId, document.id, item.productId, item.productCode, item.productName,
                item.categoryName, item.brandName, item.unitName, item.quantity, item.price, item.amount, item.remark
            );
        }
    }

    private DocumentRecord documentByNo(Long workspaceId, String documentNo) {
        if (documentNo == null || documentNo.isBlank()) {
            return null;
        }
        try {
            return jdbc.queryForObject("""
                select id, workspace_id, document_type, document_no, total_amount, document_status,
                       creator_id, operation_time
                from biz_document
                where workspace_id = ? and document_no = ?
                """, (rs, rowNum) -> {
                var document = new DocumentRecord();
                document.id = rs.getLong("id");
                document.workspaceOwnerId = ownerFromWorkspace(rs.getLong("workspace_id"));
                document.type = DocumentType.valueOf(rs.getString("document_type"));
                document.documentNo = rs.getString("document_no");
                document.totalAmount = rs.getBigDecimal("total_amount");
                document.status = DocumentStatus.valueOf(rs.getString("document_status"));
                document.creatorId = rs.getLong("creator_id");
                document.operationTime = localDateTime(rs, "operation_time");
                return document;
            }, workspaceId, documentNo);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private MasterRecord simpleMaster(ResultSet rs, String type) throws SQLException {
        var record = new MasterRecord();
        record.id = rs.getLong("id");
        record.type = type;
        record.workspaceOwnerId = ownerFromWorkspace(rs.getLong("workspace_id"));
        record.code = rs.getString("code");
        record.name = rs.getString("name");
        record.status = Status.valueOf(rs.getString("status"));
        record.createTime = localDateTime(rs, "created_at");
        record.updateTime = localDateTime(rs, "updated_at");
        return record;
    }

    private MasterRecord partnerMaster(ResultSet rs, String type) throws SQLException {
        var record = simpleMaster(rs, type);
        record.contact = rs.getString("contact");
        record.phone = rs.getString("phone");
        record.address = rs.getString("address");
        record.settlementMethod = rs.getString("settlement_method");
        return record;
    }

    private Long masterIdByName(String table, Long workspaceId, String name) {
        try {
            return jdbc.queryForObject("select id from " + table + " where workspace_id = ? and name = ? order by id limit 1", Long.class, workspaceId, name);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("MySQL缺少产品引用资料：" + table + "/" + workspaceId + "/" + name);
        }
    }

    private Long documentIdByNo(Long workspaceId, String documentNo) {
        try {
            return jdbc.queryForObject("select id from biz_document where workspace_id = ? and document_no = ?", Long.class, workspaceId, documentNo);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private long count(String sql) {
        var value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private Long workspaceId(Long ownerId) {
        return ownerId == null ? 1L : ownerId;
    }

    private Long workspaceId(User user) {
        return user.role == RoleCode.STUDENT ? user.id : workspaceId(user.workspaceOwnerId);
    }

    private Long ownerFromWorkspace(Long workspaceId) {
        return Objects.equals(workspaceId, 1L) ? null : workspaceId;
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Timestamp ts(LocalDateTime time) {
        return time == null ? null : Timestamp.valueOf(time);
    }

    private LocalDateTime timeOrNow(LocalDateTime value) {
        return value == null ? LocalDateTime.now() : value;
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        var value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String key(Long workspaceId, Long id) {
        return workspaceId + ":" + id;
    }

    private List<User> uniqueUsers(Collection<User> users) {
        var byId = new LinkedHashMap<Long, User>();
        users.forEach(user -> byId.put(user.id, user));
        return new ArrayList<>(byId.values());
    }

    private void deleteByIds(String sqlPrefix, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        var placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        jdbc.update(sqlPrefix + "(" + placeholders + ")", ids.toArray());
    }

    private String masterTable(String type) {
        return switch (type) {
            case "brand" -> "master_brand";
            case "category" -> "master_category";
            case "unit" -> "master_unit";
            case "warehouse" -> "master_warehouse";
            case "customer" -> "master_customer";
            case "supplier" -> "master_supplier";
            case "product" -> "master_product";
            default -> throw new IllegalArgumentException("Unsupported master type: " + type);
        };
    }

    private long maxId(
        List<User> users,
        List<MasterRecord> masters,
        List<DocumentRecord> documents,
        List<Message> messages,
        List<StockBalance> stocks,
        List<SettlementRecord> settlements,
        List<BugReport> bugReports,
        List<CompetitionFileSubmission> competitionFiles,
        List<RankingHistory> rankingHistory,
        List<StudentOperationLog> operationLogs
    ) {
        long max = 1000L;
        for (var user : users) max = Math.max(max, user.id == null ? 0 : user.id);
        for (var master : masters) max = Math.max(max, master.id == null ? 0 : master.id);
        for (var document : documents) max = Math.max(max, document.id == null ? 0 : document.id);
        for (var message : messages) max = Math.max(max, message.id == null ? 0 : message.id);
        for (var settlement : settlements) max = Math.max(max, settlement.id == null ? 0 : settlement.id);
        for (var report : bugReports) max = Math.max(max, report.id == null ? 0 : report.id);
        for (var file : competitionFiles) max = Math.max(max, file.id == null ? 0 : file.id);
        for (var history : rankingHistory) max = Math.max(max, history.id == null ? 0 : history.id);
        for (var log : operationLogs) max = Math.max(max, log.id == null ? 0 : log.id);
        return max;
    }

    private int nextCodeSequence(List<MasterRecord> masters, String type, String prefix) {
        return masters.stream()
            .filter(record -> type.equals(record.type))
            .map(record -> numericSuffix(record.code, prefix.length()))
            .max(Integer::compareTo)
            .orElse(0) + 1;
    }

    private int nextProductSequence(List<MasterRecord> masters) {
        return masters.stream()
            .filter(record -> "product".equals(record.type))
            .map(record -> numericTail(record.code))
            .max(Integer::compareTo)
            .orElse(0) + 1;
    }

    private int nextTailSequence(List<String> values) {
        return values.stream()
            .map(this::numericTail)
            .max(Integer::compareTo)
            .orElse(0) + 1;
    }

    private Integer numericSuffix(String value, int prefixLength) {
        if (value == null || value.length() <= prefixLength) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(prefixLength).replaceAll("\\D.*", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private Integer numericTail(String value) {
        if (value == null || value.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(value.length() - 4));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
