package com.erp;

import com.erp.domain.ErpModels.BugDefinition;
import com.erp.domain.ErpModels.BugReport;
import com.erp.domain.ErpModels.CompetitionFileSubmission;
import com.erp.domain.ErpModels.DocumentRecord;
import com.erp.domain.ErpModels.MasterRecord;
import com.erp.domain.ErpModels.Message;
import com.erp.domain.ErpModels.RankingHistory;
import com.erp.domain.ErpModels.SettlementRecord;
import com.erp.domain.ErpModels.StockBalance;
import com.erp.domain.ErpModels.StudentOperationLog;
import com.erp.domain.ErpModels.User;
import com.erp.store.ErpRealtimeRepository;
import com.erp.store.ErpStore;
import com.erp.store.ErpStoreData;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErpRealtimeWriteThroughTest {
    @Test
    void resettingStudentPasswordWritesSixDistinctUsersThroughRealtimeRepository() {
        var repository = new CapturingRealtimeRepository();
        var store = new ErpStore(new BCryptPasswordEncoder(), repository);
        var admin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");

        var resetCount = store.resetStudentPassword(admin.id, student.id);

        assertThat(resetCount).isEqualTo(6);
        var expectedIds = List.of(
            "student01",
            "student01_admin",
            "student01_purchase_staff",
            "student01_warehouse_staff",
            "student01_sales_staff",
            "student01_settlement_manager"
        ).stream().map(username -> store.userByUsername(username).id).toList();
        assertThat(repository.updatedPasswordHashes.keySet())
            .containsExactlyInAnyOrderElementsOf(expectedIds);
        assertThat(repository.updatedPasswordHashes.values())
            .allSatisfy(hash ->
                assertThat(new BCryptPasswordEncoder().matches(ErpStore.DEFAULT_PASSWORD, hash)).isTrue()
            );
    }

    @Test
    void failedPasswordResetLeavesEveryInMemoryHashUnchanged() {
        var repository = new CapturingRealtimeRepository();
        var store = new ErpStore(new BCryptPasswordEncoder(), repository);
        var admin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");
        var usernames = List.of(
            "student01",
            "student01_admin",
            "student01_purchase_staff",
            "student01_warehouse_staff",
            "student01_sales_staff",
            "student01_settlement_manager"
        );
        var originalHashes = usernames.stream().collect(java.util.stream.Collectors.toMap(
            username -> username,
            username -> store.userByUsername(username).passwordHash
        ));
        repository.failPasswordBatch = true;

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            store.resetStudentPassword(admin.id, student.id)
        ).isInstanceOf(IllegalStateException.class);

        assertThat(usernames).allSatisfy(username ->
            assertThat(store.userByUsername(username).passwordHash)
                .isEqualTo(originalHashes.get(username))
        );
    }

    @Test
    void failedPublicMasterMaterializationDoesNotPublishLocalCopy() {
        var repository = new CapturingRealtimeRepository();
        var store = new ErpStore(new BCryptPasswordEncoder(), repository);
        var student = store.userByUsername("student01");
        var purchase = store.userByUsername("student01_purchase_staff");
        var publicWarehouse = store.masters("warehouse", null, "ENABLED").stream()
            .filter(record -> record.workspaceOwnerId == null)
            .findFirst()
            .orElseThrow();
        var publicSupplier = store.masters("supplier", null, "ENABLED").stream()
            .filter(record -> record.workspaceOwnerId == null)
            .findFirst()
            .orElseThrow();
        var publicProduct = store.masters("product", null, "ENABLED").stream()
            .filter(record -> record.workspaceOwnerId == null)
            .findFirst()
            .orElseThrow();
        repository.failMasterUpsert = true;

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> store.createDocument(
            "purchase-inbound",
            purchase.id,
            Map.of(
                "warehouseId", publicWarehouse.id.toString(),
                "partnerId", publicSupplier.id.toString(),
                "productId", publicProduct.id.toString(),
                "quantity", "1",
                "price", "10"
            )
        )).isInstanceOf(IllegalStateException.class);

        assertThat(store.masters("warehouse", null, null, purchase.id).stream()
            .filter(record -> student.id.equals(record.workspaceOwnerId))
            .filter(record -> publicWarehouse.name.equals(record.name)))
            .isEmpty();
    }
    @Test
    void failedMasterCreateDoesNotPublishRecordOrOperationLog() {
        var repository = new CapturingRealtimeRepository();
        var store = new ErpStore(new BCryptPasswordEncoder(), repository);
        var superAdmin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");
        var admin = store.userByUsername("student01_admin");
        var originalLogCount = store.studentOperationLogs(superAdmin.id, student.id).size();
        repository.failMasterUpsert = true;

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> store.createMaster(
            "warehouse",
            Map.of("name", "失败新增仓库"),
            admin.id
        )).isInstanceOf(IllegalStateException.class);

        assertThat(store.masters("warehouse", null, null, admin.id).stream()
            .filter(record -> student.id.equals(record.workspaceOwnerId))
            .filter(record -> "失败新增仓库".equals(record.name)))
            .isEmpty();
        assertThat(store.studentOperationLogs(superAdmin.id, student.id)).hasSize(originalLogCount);
    }

    @Test
    void failedProductReferenceBatchDoesNotPublishPartialLocalMasters() {
        var repository = new CapturingRealtimeRepository();
        var store = new ErpStore(new BCryptPasswordEncoder(), repository);
        var student = store.userByUsername("student01");
        var admin = store.userByUsername("student01_admin");
        store.createMaster("category", Map.of("name", "批量事务分类"));
        store.createMaster("brand", Map.of("name", "批量事务品牌"));
        store.createMaster("unit", Map.of("name", "批量事务单位"));
        repository.upsertedMasters.clear();
        repository.masterUpsertAttempts = 0;
        repository.failMasterUpsertAt = 2;

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> store.createMaster(
            "product",
            Map.of(
                "name", "批量事务商品",
                "categoryName", "批量事务分类",
                "brandName", "批量事务品牌",
                "unitName", "批量事务单位",
                "purchasePrice", "10",
                "salePrice", "20"
            ),
            admin.id
        )).isInstanceOf(IllegalStateException.class);

        assertThat(List.of("category", "brand", "unit", "product")).allSatisfy(type ->
            assertThat(store.masters(type, null, null, admin.id).stream()
                .filter(record -> student.id.equals(record.workspaceOwnerId))
                .filter(record -> record.name.startsWith("批量事务")))
                .isEmpty()
        );
    }

    @Test
    void failedReturnRejectOrDeleteDoesNotReleaseReservedQuantity() {
        var repository = new CapturingRealtimeRepository();
        var store = new ErpStore(new BCryptPasswordEncoder(), repository);
        var purchase = store.userByUsername("purchase_staff");
        var warehouseStaff = store.userByUsername("warehouse_staff");
        var source = store.createSimpleDocument("purchase-inbound", purchase.id);
        store.submitDocument(source.id, purchase.id);
        store.approve(source.id, warehouseStaff.id);
        var sourceItem = source.items.get(0);
        var pendingReturn = store.createDocument("purchase-return", purchase.id, Map.of(
            "relatedDocumentNo", source.documentNo,
            "productId", sourceItem.productId.toString(),
            "quantity", "2",
            "price", "0.01"
        ));
        store.submitDocument(pendingReturn.id, purchase.id);
        repository.failDocumentUpdate = true;

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            store.reject(pendingReturn.id, warehouseStaff.id, "模拟写库失败")
        ).isInstanceOf(IllegalStateException.class);

        assertThat(store.getDocument(pendingReturn.id).status.name()).isEqualTo("PENDING");
        var afterRejectFailure = store.returnOptions(purchase.id, "purchase-return", null).stream()
            .filter(option -> option.documentId().equals(source.id))
            .findFirst()
            .orElseThrow()
            .items().get(0);
        assertThat(afterRejectFailure.returnedQuantity()).isEqualByComparingTo("2.00");

        repository.failDocumentUpdate = false;
        var draftReturn = store.createDocument("purchase-return", purchase.id, Map.of(
            "relatedDocumentNo", source.documentNo,
            "productId", sourceItem.productId.toString(),
            "quantity", "1",
            "price", "0.01"
        ));
        repository.failDocumentDelete = true;

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            store.deleteDocument(draftReturn.id, purchase.id)
        ).isInstanceOf(IllegalStateException.class);

        assertThat(store.getDocument(draftReturn.id)).isSameAs(draftReturn);
        var afterDeleteFailure = store.returnOptions(purchase.id, "purchase-return", null).stream()
            .filter(option -> option.documentId().equals(source.id))
            .findFirst()
            .orElseThrow()
            .items().get(0);
        assertThat(afterDeleteFailure.returnedQuantity()).isEqualByComparingTo("3.00");
    }

    @Test
    void businessChangesCallTargetedRealtimeRepositoryMethods() {
        var repository = new CapturingRealtimeRepository();
        var store = new ErpStore(new BCryptPasswordEncoder(), repository);
        var admin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");

        var brand = store.createMaster("brand", Map.of("name", "实时写库品牌"), student.id);
        var bug = store.publishBug("BUG-0004", true, admin.id);
        var report = store.submitBugReport(student.id, Map.of(
            "title", "实时写库报告",
            "moduleName", "商品品牌",
            "actualResult", "数据库立即有记录",
            "expectedResult", "实时写入 MySQL",
            "reproduceSteps", "新增后直接查表",
            "evidence", "MySQL"
        ));

        assertThat(repository.initialDataInserted).isTrue();
        assertThat(repository.upsertedMasters).extracting(record -> record.id).contains(brand.id);
        assertThat(repository.updatedBugDefinitions).extracting(item -> item.id).contains(bug.id);
        assertThat(repository.insertedBugReports).extracting(item -> item.id).contains(report.id);
    }

    private static final class CapturingRealtimeRepository implements ErpRealtimeRepository {

        boolean initialDataInserted;
        boolean failPasswordBatch;
        boolean failMasterUpsert;
        boolean failDocumentUpdate;
        boolean failDocumentDelete;
        int failMasterUpsertAt;
        int masterUpsertAttempts;
        final List<MasterRecord> upsertedMasters = new ArrayList<>();
        final List<BugDefinition> updatedBugDefinitions = new ArrayList<>();
        final List<BugReport> insertedBugReports = new ArrayList<>();
        final Map<Long, String> updatedPasswordHashes = new java.util.LinkedHashMap<>();

        @Override
        public boolean hasBusinessData() {
            return false;
        }

        @Override
        public ErpStoreData loadBusinessData() {
            throw new AssertionError("empty database should seed defaults");
        }

        @Override
        public void insertInitialData(ErpStoreData data) {
            initialDataInserted = true;
        }

        @Override
        public void updateUser(User user) {
        }

        @Override
        public void updateUserPasswordHashes(Map<Long, String> passwordHashes) {
            if (failPasswordBatch) {
                throw new IllegalStateException("simulated password batch failure");
            }
            updatedPasswordHashes.putAll(passwordHashes);
        }

        @Override
        public void insertStudentWorkspace(User student, Collection<User> workspaceUsers, Collection<MasterRecord> workspaceMasters) {
        }

        @Override
        public void deleteStudentWorkspace(Long studentId, Collection<Long> removedUserIds) {
        }

        @Override
        public void updateBugDefinition(BugDefinition bug) {
            updatedBugDefinitions.add(bug);
        }

        @Override
        public void insertBugReport(BugReport report, Long workspaceId) {
            insertedBugReports.add(report);
        }

        @Override
        public void updateBugReport(BugReport report) {
        }

        @Override
        public void insertCompetitionFile(CompetitionFileSubmission file, Long workspaceId) {
        }

        @Override
        public void updateCompetitionFile(CompetitionFileSubmission file) {
        }

        @Override
        public void insertRankingHistory(RankingHistory history, Long workspaceId) {
        }

        @Override
        public void insertOperationLog(StudentOperationLog log, Long workspaceId) {
        }

        @Override
        public void upsertMaster(MasterRecord record) {
            masterUpsertAttempts++;
            if (failMasterUpsert || (failMasterUpsertAt > 0 && masterUpsertAttempts == failMasterUpsertAt)) {
                throw new IllegalStateException("simulated master upsert failure");
            }
            upsertedMasters.add(record);
        }

        @Override
        public void deleteMaster(MasterRecord record) {
        }

        @Override
        public void insertDocument(DocumentRecord document) {
        }

        @Override
        public void updateDocument(DocumentRecord document) {
            if (failDocumentUpdate) {
                throw new IllegalStateException("simulated document update failure");
            }
        }

        @Override
        public void deleteDocument(DocumentRecord document) {
            if (failDocumentDelete) {
                throw new IllegalStateException("simulated document delete failure");
            }
        }

        @Override
        public void upsertStock(StockBalance stock) {
        }

        @Override
        public void insertSettlement(SettlementRecord settlement, DocumentRecord document) {
        }

        @Override
        public void insertMessage(Message message) {
        }
    }
}
