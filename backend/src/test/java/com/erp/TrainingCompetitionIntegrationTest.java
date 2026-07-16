package com.erp;

import com.erp.common.BusinessException;
import com.erp.domain.ErpModels.BugReportStatus;
import com.erp.domain.ErpModels.CompetitionSubmissionStatus;
import com.erp.domain.ErpModels.RoleCode;
import com.erp.store.ErpStore;
import com.erp.support.InMemoryBusinessTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@InMemoryBusinessTest
class TrainingCompetitionIntegrationTest {
    @Autowired
    ErpStore store;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void superAdminResetsStudentMainAndErpAccountPasswordsToDefault() {
        var admin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");
        var studentTwo = store.userByUsername("student02");
        var resetUsernames = List.of(
            "student01",
            "student01_admin",
            "student01_purchase_staff",
            "student01_warehouse_staff",
            "student01_sales_staff",
            "student01_settlement_manager"
        );
        resetUsernames.forEach(username -> store.updatePasswordHash(
            store.userByUsername(username).id,
            passwordEncoder.encode("654321")
        ));
        var studentTwoPasswordHash = studentTwo.passwordHash;

        var resetCount = store.resetStudentPassword(admin.id, student.id);

        assertThat(resetCount).isEqualTo(6);
        assertThat(resetUsernames).allSatisfy(username ->
            assertThat(passwordEncoder.matches(
                ErpStore.DEFAULT_PASSWORD,
                store.userByUsername(username).passwordHash
            )).isTrue()
        );
        assertThat(store.userByUsername("student02").passwordHash).isEqualTo(studentTwoPasswordHash);
    }

    @Test
    void studentCannotResetAnotherStudentPassword() {
        var student = store.userByUsername("student01");
        var studentTwo = store.userByUsername("student02");

        assertThatThrownBy(() -> store.resetStudentPassword(student.id, studentTwo.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("终极管理员");
    }

    @Test
    void resetStudentPasswordRejectsNonStudentTarget() {
        var admin = store.userByUsername("superadmin");

        assertThatThrownBy(() -> store.resetStudentPassword(admin.id, admin.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("学员");
    }

    @Test
    void bugLibraryCoversImportedTrainingListAndSupportsPublishReportReviewRanking() {
        var admin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");

        assertThat(store.bugDefinitions()).hasSize(79);
        assertThat(store.bugDefinitions(admin.id)).hasSize(79);
        assertThatThrownBy(() -> store.bugDefinitions(student.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("权限");
        assertThat(store.bugDefinitions())
            .anySatisfy(bug -> {
                assertThat(bug.id).isEqualTo("BUG-0004");
                assertThat(bug.moduleName).isEqualTo("商品品牌");
                assertThat(bug.summary).isEqualTo("商品品牌名称与系统内已有的商品品牌重复也可保存。");
                assertThat(bug.severity).isEqualTo("高");
            })
            .anySatisfy(bug -> {
                assertThat(bug.id).isEqualTo("BUG-0042");
                assertThat(bug.moduleName).isEqualTo("入库审核");
                assertThat(bug.summary).isEqualTo("审核拒绝原因窗口—拒绝原因超过160位字符可保存成功。");
            });

        store.publishBug("BUG-0004", true, admin.id);

        assertThat(store.activeBugTasks(student.id)).isEmpty();

        var report = store.submitBugReport(student.id, Map.of(
            "title", "商品品牌重复校验缺失",
            "moduleName", "商品品牌",
            "actualResult", "重复品牌保存成功",
            "expectedResult", "提示商品品牌名称不唯一",
            "reproduceSteps", "进入商品品牌新增，输入已有品牌名称并保存",
            "evidence", "接口返回保存成功"
        ));

        assertThat(report.status).isEqualTo(BugReportStatus.PENDING);
        assertThat(report.studentName).isEqualTo("测试学员一");
        assertThat(report.bugId).isBlank();
        assertThat(report.bugSummary).isBlank();

        var reviewed = store.reviewBugReport(admin.id, report.id, Map.of(
            "status", "APPROVED",
            "score", "85",
            "reviewComment", "定位准确，步骤清晰"
        ));

        assertThat(reviewed.status).isEqualTo(BugReportStatus.APPROVED);
        assertThat(reviewed.score).isEqualTo(85);
        assertThat(store.bugRankings())
            .singleElement()
            .satisfies(row -> {
                assertThat(row.get("studentName")).isEqualTo("测试学员一");
                assertThat(row.get("totalScore")).isEqualTo(85);
                assertThat(row.get("approvedReports")).isEqualTo(1);
            });
    }

    @Test
    void publishedBugsStayHiddenFromStudentsButEnableDefectiveBehavior() {
        var admin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");

        assertThat(store.activeBugTasks(student.id)).isEmpty();
        var studentMenuPaths = store.menus(RoleCode.STUDENT).stream()
            .flatMap(menu -> menu.children.stream())
            .map(menu -> menu.path)
            .toList();
        assertThat(studentMenuPaths)
            .doesNotContain("/competition/tasks")
            .contains("/competition/my-reports", "/competition/submit-file", "/competition/my-files");

        store.createMaster("brand", Map.of("name", "学生端重复品牌"), student.id);
        assertThatThrownBy(() -> store.createMaster("brand", Map.of("name", "学生端重复品牌"), student.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("商品品牌名称不唯一，请重新输入。");

        var longCategoryName = "学生端12345678901234567";
        assertThat(longCategoryName).hasSizeGreaterThan(16);
        assertThatThrownBy(() -> store.createMaster("category", Map.of("name", longCategoryName), student.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("商品分类名称输入有误，请重新输入。");

        store.publishBug("BUG-0002", true, admin.id);
        store.publishBug("BUG-0004", true, admin.id);

        assertThat(store.activeBugTasks(student.id)).isEmpty();

        var duplicatedBrand = store.createMaster("brand", Map.of("name", "学生端重复品牌"), student.id);
        var oversizedCategory = store.createMaster("category", Map.of("name", longCategoryName), student.id);

        assertThat(duplicatedBrand.name).isEqualTo("学生端重复品牌");
        assertThat(duplicatedBrand.workspaceOwnerId).isEqualTo(student.id);
        assertThat(oversizedCategory.name).isEqualTo(longCategoryName);
        assertThat(oversizedCategory.workspaceOwnerId).isEqualTo(student.id);

        var brandReport = store.submitBugReport(student.id, Map.of(
            "title", "学员端商品品牌重复仍可保存",
            "moduleName", "商品品牌",
            "actualResult", "重复品牌保存成功",
            "expectedResult", "提示商品品牌名称不唯一",
            "reproduceSteps", "学员登录后进入商品品牌，新增已有品牌名称并保存",
            "evidence", "学生端工作区返回保存成功"
        ));
        var guessedReport = store.submitBugReport(student.id, Map.of(
            "bugId", "BUG-0004",
            "title", "手工填写编号不应绑定官方缺陷",
            "moduleName", "商品品牌",
            "actualResult", "重复品牌保存成功",
            "expectedResult", "提示商品品牌名称不唯一",
            "reproduceSteps", "即使手工带入缺陷编号，也只保存学员自己填写的报告内容",
            "evidence", "学生端请求体中的编号不作为官方缺陷绑定依据"
        ));
        var categoryReport = store.submitBugReport(student.id, Map.of(
            "title", "学员端商品分类超长名称仍可保存",
            "moduleName", "商品分类",
            "actualResult", "超长分类名称保存成功",
            "expectedResult", "提示商品分类名称输入有误",
            "reproduceSteps", "学员登录后进入商品分类，输入超过16位名称并保存",
            "evidence", "学生端工作区返回保存成功"
        ));

        assertThat(store.bugReports(student.id))
            .extracting(report -> report.bugId)
            .containsOnly("");
        assertThat(brandReport.status).isEqualTo(BugReportStatus.PENDING);
        assertThat(guessedReport.status).isEqualTo(BugReportStatus.PENDING);
        assertThat(categoryReport.status).isEqualTo(BugReportStatus.PENDING);
        assertThat(brandReport.bugSummary).isBlank();
        assertThat(guessedReport.bugSummary).isBlank();
        assertThat(categoryReport.bugSummary).isBlank();
        assertThat(brandReport.studentName).isEqualTo("测试学员一");
        assertThat(guessedReport.studentName).isEqualTo("测试学员一");
        assertThat(categoryReport.studentName).isEqualTo("测试学员一");

        var guessedFile = store.submitCompetitionFile(student.id, Map.of(
            "title", "手工编号文件提交",
            "moduleName", "商品品牌",
            "bugId", "BUG-0004",
            "fileName", "evidence.pdf",
            "contentType", "application/pdf",
            "fileSize", "1024",
            "storagePath", "uploads/competition/student01/evidence.pdf"
        ));
        assertThat(guessedFile.bugId).isBlank();
    }

    @Test
    void adminCanCreateAndDeleteStudentAccounts() {
        var admin = store.userByUsername("superadmin");

        assertThat(store.students(admin.id))
            .extracting(student -> student.username)
            .contains("student01", "student02");

        var created = store.createStudent(admin.id, Map.of(
            "username", "student03",
            "name", "测试学员三",
            "phone", "13900000003",
            "password", "654321"
        ));

        assertThat(created.username).isEqualTo("student03");
        assertThat(store.userByUsername("student03").role).isEqualTo(RoleCode.STUDENT);
        assertThat(store.students(admin.id))
            .extracting(student -> student.name)
            .contains("测试学员三");

        store.publishBug("BUG-0004", true, admin.id);
        var report = store.submitBugReport(store.userByUsername("student03").id, Map.of(
            "title", "新增学员提交报告",
            "moduleName", "商品品牌",
            "actualResult", "重复品牌保存成功",
            "expectedResult", "提示商品品牌名称不唯一",
            "reproduceSteps", "新增重复品牌",
            "evidence", "接口返回保存成功"
        ));
        store.reviewBugReport(admin.id, report.id, Map.of(
            "status", "APPROVED",
            "score", "90",
            "reviewComment", "有效报告"
        ));

        assertThat(store.bugRankings())
            .anySatisfy(row -> assertThat(row.get("studentName")).isEqualTo("测试学员三"));

        store.deleteStudent(admin.id, created.id);

        assertThatThrownBy(() -> store.userByUsername("student03"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("登录账号不存在");
        assertThat(store.students(admin.id))
            .extracting(student -> student.username)
            .doesNotContain("student03");
        assertThat(store.bugRankings())
            .extracting(row -> row.get("studentName"))
            .doesNotContain("测试学员三");
        assertThatThrownBy(() -> store.deleteStudent(admin.id, admin.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("只能删除测试学员账号");
    }

    @Test
    void creatingStudentAutomaticallyCreatesRoleSpecificErpAccounts() {
        var admin = store.userByUsername("superadmin");

        var created = store.createStudent(admin.id, Map.of(
            "username", "student_auto",
            "name", "自动学员",
            "phone", "13900000999",
            "password", "654321"
        ));

        assertThat(created.erpAccounts)
            .extracting(account -> account.username)
            .containsExactlyInAnyOrder(
                "student_auto_admin",
                "student_auto_purchase_staff",
                "student_auto_warehouse_staff",
                "student_auto_sales_staff",
                "student_auto_settlement_manager"
            );
        assertThat(store.userByUsername("student_auto_admin").role).isEqualTo(RoleCode.ADMIN);
        assertThat(store.userByUsername("student_auto_purchase_staff").role).isEqualTo(RoleCode.PURCHASE_STAFF);
        assertThat(store.userByUsername("student_auto_warehouse_staff").role).isEqualTo(RoleCode.WAREHOUSE_STAFF);
        assertThat(store.userByUsername("student_auto_sales_staff").role).isEqualTo(RoleCode.SALES_STAFF);
        assertThat(store.userByUsername("student_auto_settlement_manager").role).isEqualTo(RoleCode.SETTLEMENT_MANAGER);
        assertThat(store.userByUsername("student_auto_admin").workspaceOwnerId).isEqualTo(created.id);
        assertThat(store.userByUsername("student_auto_purchase_staff").workspaceOwnerId).isEqualTo(created.id);
        assertThat(store.userByUsername("student_auto_warehouse_staff").workspaceOwnerId).isEqualTo(created.id);
        assertThat(store.userByUsername("student_auto_sales_staff").workspaceOwnerId).isEqualTo(created.id);
        assertThat(store.userByUsername("student_auto_settlement_manager").workspaceOwnerId).isEqualTo(created.id);
    }

    @Test
    void studentWorkspaceAdminUsesOwnedErpMenusWithoutPlatformCompetitionPermissions() {
        var platformAdmin = store.userByUsername("superadmin");
        var workspaceAdmin = store.userByUsername("student01_admin");

        assertThatThrownBy(() -> store.userByUsername("admin"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("登录账号不存在");
        assertThat(store.menus(platformAdmin))
            .extracting(menu -> menu.title)
            .containsExactly("测试竞赛后台");
        assertThat(store.menus(workspaceAdmin))
            .extracting(menu -> menu.title)
            .containsExactly("基础信息管理", "采购管理", "库存管理", "销售管理", "结算管理");
        assertThatThrownBy(() -> store.bugDefinitions(workspaceAdmin.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("当前角色无缺陷发布权限");
        assertThat(store.bugDefinitions(platformAdmin.id)).isNotEmpty();
    }

    @Test
    void studentMainAccountUsesCompetitionOnlyAndErpSubaccountsUseOwnedWorkspace() {
        var student = store.userByUsername("student01");
        var studentTwo = store.userByUsername("student02");
        var purchase = store.userByUsername("student01_purchase_staff");
        var warehouse = store.userByUsername("student01_warehouse_staff");
        var settlement = store.userByUsername("student01_settlement_manager");
        var studentTwoPurchase = store.userByUsername("student02_purchase_staff");
        var studentTwoWarehouse = store.userByUsername("student02_warehouse_staff");
        var studentTwoSettlement = store.userByUsername("student02_settlement_manager");

        assertThat(store.menus(student.role))
            .extracting(menu -> menu.title)
            .containsExactly("测试竞赛");
        assertThat(store.menus(purchase.role))
            .extracting(menu -> menu.title)
            .containsExactly("采购管理");
        assertThat(store.menus(warehouse.role))
            .extracting(menu -> menu.title)
            .containsExactly("库存管理");
        assertThat(store.menus(settlement.role))
            .extracting(menu -> menu.title)
            .containsExactly("结算管理");

        var inbound = store.createSimpleDocument("purchase-inbound", purchase.id);
        assertThat(inbound.workspaceOwnerId).isEqualTo(student.id);
        assertThat(inbound.workspaceOwnerId).isNotEqualTo(studentTwo.id);
        store.submitDocument(inbound.id, purchase.id);

        assertThat(store.documents("purchase-inbound", studentTwoPurchase.id))
            .extracting(document -> document.documentNo)
            .doesNotContain(inbound.documentNo);
        assertThat(store.auditList("inbound", store.userByUsername("warehouse_staff").id))
            .extracting(document -> document.documentNo)
            .doesNotContain(inbound.documentNo);
        assertThat(store.auditList("inbound", studentTwoWarehouse.id))
            .extracting(document -> document.documentNo)
            .doesNotContain(inbound.documentNo);
        assertThat(store.auditList("inbound", warehouse.id))
            .extracting(document -> document.documentNo)
            .contains(inbound.documentNo);

        store.approve(inbound.id, warehouse.id);

        assertThat(store.stockViews(warehouse.id)).isNotEmpty();
        assertThat(store.stockViews(studentTwoWarehouse.id)).isEmpty();
        assertThat(store.settlements("expense", settlement.id))
            .extracting(record -> record.relatedDocumentNo)
            .contains(inbound.documentNo);
        assertThat(store.settlements("expense", studentTwoSettlement.id))
            .extracting(record -> record.relatedDocumentNo)
            .doesNotContain(inbound.documentNo);
        assertThat(store.studentOperationLogs(store.userByUsername("superadmin").id, student.id))
            .extracting(log -> log.moduleName)
            .contains("采购入库");
    }

    @Test
    void deletingStudentRemovesSubaccountsAndWorkspaceData() {
        var admin = store.userByUsername("superadmin");
        var created = store.createStudent(admin.id, Map.of(
            "username", "student_delete",
            "name", "删除学员",
            "phone", "13900000888"
        ));
        var purchase = store.userByUsername("student_delete_purchase_staff");
        var warehouse = store.userByUsername("student_delete_warehouse_staff");

        var inbound = store.createSimpleDocument("purchase-inbound", purchase.id);
        store.submitDocument(inbound.id, purchase.id);
        store.approve(inbound.id, warehouse.id);
        assertThat(store.stockViews(warehouse.id)).isNotEmpty();

        store.deleteStudent(admin.id, created.id);

        assertThatThrownBy(() -> store.userByUsername("student_delete"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("登录账号不存在");
        assertThatThrownBy(() -> store.userByUsername("student_delete_purchase_staff"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("登录账号不存在");
        assertThat(store.bugRankings())
            .extracting(row -> row.get("studentName"))
            .doesNotContain("删除学员");
    }

    @Test
    void superAdminCanReceiveReviewCompetitionFilesAndCreateRankingHistory() {
        var superAdmin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");

        assertThat(superAdmin.role).isEqualTo(RoleCode.SUPER_ADMIN);
        assertThat(store.menus(superAdmin.role))
            .extracting(menu -> menu.title)
            .containsExactly("测试竞赛后台");

        var submission = store.submitCompetitionFile(student.id, Map.of(
            "title", "库存调拨测试报告",
            "moduleName", "库存调拨",
            "fileName", "库存调拨测试报告.pdf",
            "contentType", "application/pdf",
            "fileSize", "2048",
            "storagePath", "uploads/competition/student01/库存调拨测试报告.pdf"
        ));

        assertThat(submission.status).isEqualTo(CompetitionSubmissionStatus.PENDING);
        assertThat(submission.studentName).isEqualTo("测试学员一");
        assertThat(store.competitionFiles(superAdmin.id))
            .singleElement()
            .satisfies(file -> {
                assertThat(file.title).isEqualTo("库存调拨测试报告");
                assertThat(file.fileName).endsWith(".pdf");
            });

        var reviewed = store.reviewCompetitionFile(superAdmin.id, submission.id, Map.of(
            "status", "APPROVED",
            "score", "96",
            "roundName", "第1轮",
            "reviewComment", "证据链完整，覆盖库存调拨关联校验"
        ));

        assertThat(reviewed.status).isEqualTo(CompetitionSubmissionStatus.APPROVED);
        assertThat(reviewed.score).isEqualTo(96);
        assertThat(store.rankingHistory(superAdmin.id))
            .singleElement()
            .satisfies(history -> {
                assertThat(history.roundName).isEqualTo("第1轮");
                assertThat(history.studentName).isEqualTo("测试学员一");
                assertThat(history.totalScore).isEqualTo(96);
                assertThat(history.approvedFiles).isEqualTo(1);
            });

        assertThatThrownBy(() -> store.submitCompetitionFile(student.id, Map.of(
            "title", "非法附件",
            "moduleName", "库存调拨",
            "fileName", "bad.exe",
            "contentType", "application/octet-stream",
            "fileSize", "1024",
            "storagePath", "uploads/competition/student01/bad.exe"
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("仅支持PDF、Word、Excel文件");
    }

    @Test
    void superAdminCanSeeStudentOperationLogs() {
        var superAdmin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");

        store.recordStudentOperation(student.id, "库存调拨", "新建调拨单", "从华东仓调拨到华南仓");
        store.recordStudentOperation(student.id, "采购退货", "提交审核", "关联采购入库单RK202606200001");

        assertThat(store.studentOperationLogs(superAdmin.id, student.id))
            .extracting(log -> log.moduleName)
            .containsExactly("采购退货", "库存调拨");
        assertThat(store.studentOperationLogs(superAdmin.id, null))
            .extracting(log -> log.studentName)
            .contains("测试学员一");
        assertThatThrownBy(() -> store.studentOperationLogs(student.id, null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("终极管理员");
    }

    @Test
    void studentBusinessWorkspaceIsIsolatedAcrossMasterDocumentsStockAndSettlement() {
        var studentOne = store.userByUsername("student01");
        var studentTwo = store.userByUsername("student02");
        var studentOnePurchase = store.userByUsername("student01_purchase_staff");
        var studentOneWarehouse = store.userByUsername("student01_warehouse_staff");
        var studentOneSettlement = store.userByUsername("student01_settlement_manager");
        var studentTwoPurchase = store.userByUsername("student02_purchase_staff");
        var studentTwoWarehouse = store.userByUsername("student02_warehouse_staff");
        var studentTwoSettlement = store.userByUsername("student02_settlement_manager");

        assertThat(store.menus(RoleCode.STUDENT))
            .extracting(menu -> menu.title)
            .containsExactly("测试竞赛");
        assertThat(store.menus(RoleCode.PURCHASE_STAFF))
            .extracting(menu -> menu.title)
            .containsExactly("采购管理");

        var privateBrand = store.createMaster("brand", Map.of("name", "学生一私有品牌"), studentOnePurchase.id);
        assertThat(privateBrand.workspaceOwnerId).isEqualTo(studentOne.id);
        assertThat(store.masters("brand", null, null, studentOnePurchase.id))
            .extracting(record -> record.name)
            .contains("学生一私有品牌");
        assertThat(store.masters("brand", null, null, studentTwoPurchase.id))
            .extracting(record -> record.name)
            .doesNotContain("学生一私有品牌");
        assertThat(store.masters("brand", null, null))
            .extracting(record -> record.name)
            .doesNotContain("学生一私有品牌");

        var inbound = store.createSimpleDocument("purchase-inbound", studentOnePurchase.id);
        assertThat(inbound.workspaceOwnerId).isEqualTo(studentOne.id);
        assertThat(store.documents("purchase-inbound", studentTwoPurchase.id))
            .extracting(document -> document.documentNo)
            .doesNotContain(inbound.documentNo);

        store.submitDocument(inbound.id, studentOnePurchase.id);
        store.approve(inbound.id, studentOneWarehouse.id);

        assertThat(store.stockViews(studentOneWarehouse.id)).isNotEmpty();
        assertThat(store.stockViews(studentTwoWarehouse.id)).isEmpty();
        assertThat(store.settlements("expense", studentOneSettlement.id))
            .anySatisfy(settlement -> assertThat(settlement.relatedDocumentNo).isEqualTo(inbound.documentNo));
        assertThat(store.settlements("expense", studentTwoSettlement.id))
            .extracting(settlement -> settlement.relatedDocumentNo)
            .doesNotContain(inbound.documentNo);
        assertThat(store.studentOperationLogs(store.userByUsername("superadmin").id, studentOne.id))
            .extracting(log -> log.moduleName)
            .contains("采购入库");
    }

    @Test
    void cancelPublishClearsPublisherMetadata() {
        var admin = store.userByUsername("superadmin");

        var published = store.publishBug("BUG-0004", true, admin.id);
        assertThat(published.active).isTrue();
        assertThat(published.publisherId).isEqualTo(admin.id);
        assertThat(published.publisherName).isEqualTo(admin.name);
        assertThat(published.publishTime).isNotNull();

        var unpublished = store.publishBug("BUG-0004", false, admin.id);

        assertThat(unpublished.active).isFalse();
        assertThat(unpublished.publisherId).isNull();
        assertThat(unpublished.publisherName).isBlank();
        assertThat(unpublished.publishTime).isNull();
    }

    @Test
    void selectedBugDefinitionsToggleRealDefectiveBehavior() {
        var admin = store.userByUsername("superadmin");

        store.createMaster("brand", Map.of("name", "竞赛重复品牌"));
        assertThatThrownBy(() -> store.createMaster("brand", Map.of("name", "竞赛重复品牌")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("商品品牌名称不唯一，请重新输入。");

        store.publishBug("BUG-0004", true, admin.id);
        var duplicate = store.createMaster("brand", Map.of("name", "竞赛重复品牌"));
        assertThat(duplicate.name).isEqualTo("竞赛重复品牌");

        var longCategoryName = "12345678901234567";
        assertThat(longCategoryName).hasSizeGreaterThan(16);
        store.publishBug("BUG-0002", false, admin.id);
        assertThatThrownBy(() -> store.createMaster("category", Map.of("name", longCategoryName)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("商品分类名称输入有误，请重新输入。");

        store.publishBug("BUG-0002", true, admin.id);
        assertThat(store.createMaster("category", Map.of("name", longCategoryName)).name).isEqualTo(longCategoryName);
    }
}
