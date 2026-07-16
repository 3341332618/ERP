package com.erp;

import com.erp.common.BusinessException;
import com.erp.domain.ErpModels.DocumentRecord;
import com.erp.domain.ErpModels.MasterRecord;
import com.erp.domain.ErpModels.Status;
import com.erp.store.ErpStore;
import com.erp.support.InMemoryBusinessTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@InMemoryBusinessTest
class BusinessAssociationIntegrationTest {
    @Autowired
    ErpStore store;

    @Test
    void productReferencesMustAlreadyExistAndBeEnabled() {
        var admin = store.userByUsername("student01_admin");

        assertThatThrownBy(() -> store.createMaster("product", Map.of(
            "name", "任意引用商品",
            "categoryName", "不存在分类",
            "brandName", "不存在品牌",
            "unitName", "不存在单位",
            "purchasePrice", "10",
            "salePrice", "20"
        ), admin.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("请选择已存在且启用");
    }

    @Test
    void disabledWarehouseCannotBeUsedByNewDocument() {
        var purchase = store.userByUsername("purchase_staff");
        var warehouse = store.createMaster("warehouse", Map.of("name", "禁用关联仓库"));
        store.changeMasterStatus("warehouse", warehouse.id, Status.DISABLED);

        assertThatThrownBy(() -> store.createDocument("purchase-inbound", purchase.id, Map.of(
            "warehouseId", warehouse.id.toString(),
            "partnerId", enabledMaster("supplier", purchase.id).id.toString(),
            "productId", enabledMaster("product", purchase.id).id.toString(),
            "quantity", "1",
            "price", "10"
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("仓库已停用");
    }

    @Test
    void disabledProductCannotBeUsedByNewDocument() {
        var purchase = store.userByUsername("purchase_staff");
        var product = store.createMaster("product", Map.of(
            "name", "禁用关联商品",
            "categoryName", "办公设备",
            "brandName", "连想",
            "unitName", "台",
            "purchasePrice", "10",
            "salePrice", "20"
        ));
        store.changeMasterStatus("product", product.id, Status.DISABLED);

        assertThatThrownBy(() -> store.createDocument("purchase-inbound", purchase.id, Map.of(
            "warehouseId", enabledMaster("warehouse", purchase.id).id.toString(),
            "partnerId", enabledMaster("supplier", purchase.id).id.toString(),
            "productId", product.id.toString(),
            "quantity", "1",
            "price", "10"
        )))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("商品已停用");
    }

    @Test
    void publicReferencesAreMaterializedInsideStudentWorkspace() {
        var student = store.userByUsername("student01");
        var purchase = store.userByUsername("student01_purchase_staff");
        var publicWarehouse = enabledPublicMaster("warehouse");
        var publicSupplier = enabledPublicMaster("supplier");
        var publicProduct = enabledPublicMaster("product");

        var document = store.createDocument("purchase-inbound", purchase.id, Map.of(
            "warehouseId", publicWarehouse.id.toString(),
            "partnerId", publicSupplier.id.toString(),
            "productId", publicProduct.id.toString(),
            "quantity", "1",
            "price", "10"
        ));

        assertThat(document.workspaceOwnerId).isEqualTo(student.id);
        assertThat(store.masterRecord("warehouse", document.warehouseId).workspaceOwnerId).isEqualTo(student.id);
        assertThat(store.masterRecord("supplier", document.partnerId).workspaceOwnerId).isEqualTo(student.id);
        assertThat(store.masterRecord("product", document.items.get(0).productId).workspaceOwnerId).isEqualTo(student.id);
        assertThat(publicWarehouse.workspaceOwnerId).isNull();
        assertThat(publicSupplier.workspaceOwnerId).isNull();
        assertThat(publicProduct.workspaceOwnerId).isNull();
    }

    @Test
    void editingAnAlreadyMaterializedPublicMasterReusesTheLocalRecord() {
        var student = store.userByUsername("student01");
        var admin = store.userByUsername("student01_admin");
        var purchase = store.userByUsername("student01_purchase_staff");
        var publicWarehouse = enabledPublicMaster("warehouse");

        var document = store.createDocument("purchase-inbound", purchase.id, Map.of(
            "warehouseId", publicWarehouse.id.toString(),
            "partnerId", enabledPublicMaster("supplier").id.toString(),
            "productId", enabledPublicMaster("product").id.toString(),
            "quantity", "1",
            "price", "10"
        ));
        var localId = document.warehouseId;
        var publicContact = publicWarehouse.contact;
        var updated = store.updateMaster("warehouse", publicWarehouse.id, Map.of(
            "name", "本地重命名仓库",
            "contact", "本地仓库联系人",
            "phone", "13800009999",
            "address", "本地仓库地址"
        ), admin.id);
        var reusedDocument = store.createDocument("purchase-inbound", purchase.id, Map.of(
            "warehouseId", publicWarehouse.id.toString(),
            "partnerId", enabledPublicMaster("supplier").id.toString(),
            "productId", enabledPublicMaster("product").id.toString(),
            "quantity", "1",
            "price", "10"
        ));

        assertThat(updated.id).isEqualTo(localId);
        assertThat(updated.contact).isEqualTo("本地仓库联系人");
        assertThat(publicWarehouse.contact).isEqualTo(publicContact);
        assertThat(reusedDocument.warehouseId).isEqualTo(localId);
        assertThat(store.masters("warehouse", null, null, admin.id).stream()
            .filter(record -> student.id.equals(record.workspaceOwnerId))
            .filter(record -> updated.code.equals(record.code)))
            .hasSize(1);
    }

    @Test
    void failedPublicMasterUpdateDoesNotLeaveALocalCopy() {
        var student = store.userByUsername("student01");
        var admin = store.userByUsername("student01_admin");
        var publicWarehouse = enabledPublicMaster("warehouse");

        assertThatThrownBy(() -> store.updateMaster("warehouse", publicWarehouse.id, Map.of(
            "name", ""
        ), admin.id)).isInstanceOf(BusinessException.class);

        assertThat(store.masters("warehouse", null, null, admin.id).stream()
            .filter(record -> student.id.equals(record.workspaceOwnerId))
            .filter(record -> publicWarehouse.name.equals(record.name)))
            .isEmpty();
    }

    @Test
    void updatingProductPriceKeepsItsStableCode() {
        var product = store.createMaster("product", Map.of(
            "name", "稳定编号商品",
            "categoryName", "办公设备",
            "brandName", "连想",
            "unitName", "台",
            "purchasePrice", "10",
            "salePrice", "20"
        ));
        var originalCode = product.code;
        store.createMaster("product", Map.of(
            "name", "推进序号商品",
            "categoryName", "办公设备",
            "brandName", "连想",
            "unitName", "台",
            "purchasePrice", "30",
            "salePrice", "40"
        ));

        var updated = store.updateMaster("product", product.id, Map.of(
            "name", product.name,
            "purchasePrice", "11",
            "salePrice", "21"
        ));

        assertThat(updated.code).isEqualTo(originalCode);
    }
    @Test
    void publicBrandCanBeEditedRepeatedlyInsideTheStudentWorkspace() {
        var admin = store.userByUsername("student01_admin");
        var publicBrand = enabledPublicMaster("brand");

        var firstUpdate = store.updateMaster("brand", publicBrand.id, Map.of(
            "name", "本地重命名品牌"
        ), admin.id);
        var secondUpdate = store.updateMaster("brand", publicBrand.id, Map.of(
            "name", "本地重命名品牌"
        ), admin.id);

        assertThat(secondUpdate.id).isEqualTo(firstUpdate.id);
        assertThat(secondUpdate.workspaceOwnerId).isEqualTo(store.userByUsername("student01").id);
        assertThat(store.masters("brand", null, null, admin.id).stream()
            .filter(record -> secondUpdate.code.equals(record.code)))
            .hasSize(1);
    }

    @Test
    void warehouseStaffBindingMustStayInsideTheCurrentWorkspace() {
        var student = store.userByUsername("student01");
        var admin = store.userByUsername("student01_admin");
        var foreignStaff = store.userByUsername("student02_warehouse_staff");
        var publicWarehouse = enabledPublicMaster("warehouse");

        assertThatThrownBy(() -> store.updateMaster("warehouse", publicWarehouse.id, Map.of(
            "name", publicWarehouse.name,
            "warehouseUserId", foreignStaff.id.toString()
        ), admin.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("当前工作区");
        assertThat(store.masters("warehouse", null, null, admin.id).stream()
            .filter(record -> student.id.equals(record.workspaceOwnerId))
            .filter(record -> publicWarehouse.name.equals(record.name)))
            .isEmpty();
    }

    @Test
    void rebindingWarehouseStaffClearsPreviousAssignments() {
        var oldStaff = store.userByUsername("warehouse_staff");
        var newStaff = store.userByUsername("warehouse_staff_south");
        var previousWarehouse = store.createMaster("warehouse", Map.of(
            "name", "华南仓库",
            "warehouseUserId", newStaff.id.toString()
        ));
        var targetWarehouse = store.masterRecord("warehouse", oldStaff.warehouseId);

        var updated = store.updateMaster("warehouse", targetWarehouse.id, Map.of(
            "name", targetWarehouse.name,
            "warehouseUserId", newStaff.id.toString()
        ));

        assertThat(updated.warehouseUserId).isEqualTo(newStaff.id);
        assertThat(oldStaff.warehouseId).isNull();
        assertThat(newStaff.warehouseId).isEqualTo(targetWarehouse.id);
        assertThat(previousWarehouse.warehouseUserId).isNull();
    }

    @Test
    void returnOptionsOnlyExposeApprovedSourceDocumentsCreatedByOperator() {
        var purchase = store.userByUsername("purchase_staff");
        var purchaseManager = store.userByUsername("purchase_manager");
        var approvedOwn = approvedPurchaseInbound("purchase_staff");
        var draftOwn = store.createSimpleDocument("purchase-inbound", purchase.id);
        var approvedOther = approvedPurchaseInbound("purchase_manager");

        var options = store.returnOptions(purchase.id, "purchase-return", null);

        assertThat(options).extracting(option -> option.documentId())
            .contains(approvedOwn.id)
            .doesNotContain(draftOwn.id, approvedOther.id);
        assertThat(purchaseManager.id).isEqualTo(approvedOther.creatorId);
    }

    @Test
    void returnOptionsReportRemainingQuantityAndExcludeTheEditedReturn() {
        var purchase = store.userByUsername("purchase_staff");
        var source = approvedPurchaseInbound("purchase_staff");
        var product = source.items.get(0);
        var draftReturn = store.createDocument("purchase-return", purchase.id, Map.of(
            "relatedDocumentNo", source.documentNo,
            "productId", product.productId.toString(),
            "quantity", "2",
            "price", "0.01"
        ));

        var createOption = store.returnOptions(purchase.id, "purchase-return", null).stream()
            .filter(option -> option.documentId().equals(source.id))
            .findFirst()
            .orElseThrow()
            .items().get(0);
        var editOption = store.returnOptions(purchase.id, "purchase-return", draftReturn.id).stream()
            .filter(option -> option.documentId().equals(source.id))
            .findFirst()
            .orElseThrow()
            .items().get(0);

        assertThat(createOption.originalQuantity()).isEqualByComparingTo("10.00");
        assertThat(createOption.returnedQuantity()).isEqualByComparingTo("2.00");
        assertThat(createOption.remainingQuantity()).isEqualByComparingTo("8.00");
        assertThat(editOption.returnedQuantity()).isEqualByComparingTo("0.00");
        assertThat(editOption.remainingQuantity()).isEqualByComparingTo("10.00");
    }

    @Test
    void returnDocumentAlwaysUsesTheOriginalSourcePrice() {
        var purchase = store.userByUsername("purchase_staff");
        var source = approvedPurchaseInbound("purchase_staff");
        var sourceItem = source.items.get(0);

        var purchaseReturn = store.createDocument("purchase-return", purchase.id, Map.of(
            "relatedDocumentNo", source.documentNo,
            "productId", sourceItem.productId.toString(),
            "quantity", "2",
            "price", "0.01"
        ));

        assertThat(purchaseReturn.items.get(0).price).isEqualByComparingTo(sourceItem.price);
        assertThat(purchaseReturn.totalAmount).isEqualByComparingTo(sourceItem.price.multiply(new java.math.BigDecimal("2.00")));
    }

    @Test
    void failedReturnEditLeavesTheStoredDraftUnchanged() {
        var purchase = store.userByUsername("purchase_staff");
        var source = approvedPurchaseInbound("purchase_staff");
        var sourceItem = source.items.get(0);
        var draftReturn = store.createDocument("purchase-return", purchase.id, Map.of(
            "relatedDocumentNo", source.documentNo,
            "productId", sourceItem.productId.toString(),
            "quantity", "2",
            "price", "0.01"
        ));

        assertThatThrownBy(() -> store.updateDocument("purchase-return", draftReturn.id, purchase.id, Map.of(
            "relatedDocumentNo", source.documentNo,
            "productId", sourceItem.productId.toString(),
            "quantity", "11",
            "price", "0.01"
        ))).isInstanceOf(BusinessException.class);

        var stored = store.getDocument(draftReturn.id);
        assertThat(stored.items).hasSize(1);
        assertThat(stored.items.get(0).quantity).isEqualByComparingTo("2.00");
        assertThat(stored.items.get(0).price).isEqualByComparingTo(sourceItem.price);
        assertThat(stored.relatedDocumentNo).isEqualTo(source.documentNo);
    }

    @Test
    void returnOptionsRejectAnEditingContextOwnedByAnotherOperatorOrType() {
        var purchase = store.userByUsername("purchase_staff");
        var manager = store.userByUsername("purchase_manager");
        var source = approvedPurchaseInbound("purchase_staff");
        var sourceItem = source.items.get(0);
        var draftReturn = store.createDocument("purchase-return", purchase.id, Map.of(
            "relatedDocumentNo", source.documentNo,
            "productId", sourceItem.productId.toString(),
            "quantity", "1",
            "price", "0.01"
        ));

        assertThatThrownBy(() -> store.returnOptions(manager.id, "purchase-return", draftReturn.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("编辑上下文无效");
        assertThatThrownBy(() -> store.returnOptions(purchase.id, "purchase-return", source.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("编辑上下文无效");
    }

    @Test
    void fullyReturnedSourceIsHiddenAndRejectedReturnReleasesItsQuantity() {
        var purchase = store.userByUsername("purchase_staff");
        var source = approvedPurchaseInbound("purchase_staff");
        var sourceItem = source.items.get(0);
        var fullReturn = store.createDocument("purchase-return", purchase.id, Map.of(
            "relatedDocumentNo", source.documentNo,
            "productId", sourceItem.productId.toString(),
            "quantity", "10",
            "price", "0.01"
        ));

        assertThat(store.returnOptions(purchase.id, "purchase-return", null))
            .extracting(option -> option.documentId())
            .doesNotContain(source.id);

        store.submitDocument(fullReturn.id, purchase.id);
        store.reject(fullReturn.id, store.userByUsername("warehouse_staff").id, "不予退货");

        var released = store.returnOptions(purchase.id, "purchase-return", null).stream()
            .filter(option -> option.documentId().equals(source.id))
            .findFirst()
            .orElseThrow()
            .items().get(0);
        assertThat(released.returnedQuantity()).isEqualByComparingTo("0.00");
        assertThat(released.remainingQuantity()).isEqualByComparingTo("10.00");
    }

    @Test
    void approvedReturnCannotBeRejectedToReleaseItsReservedQuantity() {
        var purchase = store.userByUsername("purchase_staff");
        var source = approvedPurchaseInbound("purchase_staff");
        var sourceItem = source.items.get(0);
        var purchaseReturn = store.createDocument("purchase-return", purchase.id, Map.of(
            "relatedDocumentNo", source.documentNo,
            "productId", sourceItem.productId.toString(),
            "quantity", "2",
            "price", "0.01"
        ));
        var warehouseStaff = store.userByUsername("warehouse_staff");
        store.submitDocument(purchaseReturn.id, purchase.id);
        store.approve(purchaseReturn.id, warehouseStaff.id);

        assertThatThrownBy(() -> store.reject(purchaseReturn.id, warehouseStaff.id, "错误驳回"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("当前状态不可审核");

        var option = store.returnOptions(purchase.id, "purchase-return", null).stream()
            .filter(candidate -> candidate.documentId().equals(source.id))
            .findFirst()
            .orElseThrow()
            .items().get(0);
        assertThat(option.returnedQuantity()).isEqualByComparingTo("2.00");
        assertThat(option.remainingQuantity()).isEqualByComparingTo("8.00");
    }

    @Test
    void salesReturnOptionsMirrorTheApprovedSalesSource() {
        var inbound = approvedPurchaseInbound("purchase_staff");
        var sales = store.userByUsername("sales_staff");
        var product = inbound.items.get(0);
        var customer = enabledMaster("customer", sales.id);
        var outbound = store.createDocument("sales-outbound", sales.id, Map.of(
            "warehouseId", inbound.warehouseId.toString(),
            "partnerId", customer.id.toString(),
            "productId", product.productId.toString(),
            "quantity", "3",
            "price", "123.45",
            "remark", "销售出库"
        ));
        store.submitDocument(outbound.id, sales.id);
        store.approve(outbound.id, store.userByUsername("warehouse_staff").id);

        var option = store.returnOptions(sales.id, "sales-return", null).stream()
            .filter(candidate -> candidate.documentId().equals(outbound.id))
            .findFirst()
            .orElseThrow();

        assertThat(option.warehouseId()).isEqualTo(outbound.warehouseId);
        assertThat(option.partnerId()).isEqualTo(outbound.partnerId);
        assertThat(option.items().get(0).price()).isEqualByComparingTo("123.45");
        assertThat(option.items().get(0).remainingQuantity()).isEqualByComparingTo("3.00");
    }

    @Test
    void documentDetailRequiresThePathTypeToMatch() {
        var purchase = store.userByUsername("purchase_staff");
        var inbound = store.createSimpleDocument("purchase-inbound", purchase.id);

        assertThat(store.documentDetail("purchase-inbound", inbound.id, purchase.id)).isSameAs(inbound);
        assertThatThrownBy(() -> store.documentDetail("sales-outbound", inbound.id, purchase.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("单据类型不匹配");
        assertThatThrownBy(() -> store.documentDetail("unknown", inbound.id, purchase.id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("未知单据类型");
        assertThatThrownBy(() -> store.returnOptions(purchase.id, "purchase-inbound", null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("仅退货单");
    }

    @Test
    void unchangedSalesOutboundCanBeEditedWithoutCountingItsOwnReservation() {
        var inbound = approvedPurchaseInbound("purchase_staff");
        var sales = store.userByUsername("sales_staff");
        var customer = enabledMaster("customer", sales.id);
        var product = inbound.items.get(0);
        var payload = Map.<String, Object>of(
            "warehouseId", inbound.warehouseId.toString(),
            "partnerId", customer.id.toString(),
            "productId", product.productId.toString(),
            "quantity", "10",
            "price", "123.45",
            "remark", "unchanged edit"
        );
        var outbound = store.createDocument("sales-outbound", sales.id, payload);

        assertThatCode(() -> store.updateDocument("sales-outbound", outbound.id, sales.id, payload))
            .doesNotThrowAnyException();
    }

    @Test
    void unchangedStockTransferCanBeEditedWithoutCountingItsOwnReservation() {
        var inbound = approvedPurchaseInbound("purchase_staff");
        var manager = store.userByUsername("warehouse_manager");
        var target = store.createMaster("warehouse", Map.of("name", "Editing target warehouse"));
        var product = inbound.items.get(0);
        var payload = Map.<String, Object>of(
            "warehouseId", inbound.warehouseId.toString(),
            "targetWarehouseId", target.id.toString(),
            "productId", product.productId.toString(),
            "quantity", "10",
            "remark", "unchanged edit"
        );
        var transfer = store.createDocument("stock-transfer", manager.id, payload);

        assertThatCode(() -> store.updateDocument("stock-transfer", transfer.id, manager.id, payload))
            .doesNotThrowAnyException();
    }

    @Test
    void unchangedPurchaseReturnCanBeEditedWithoutCountingItsOwnReservation() {
        var purchase = store.userByUsername("purchase_staff");
        var inbound = approvedPurchaseInbound("purchase_staff");
        var product = inbound.items.get(0);
        var payload = Map.<String, Object>of(
            "relatedDocumentNo", inbound.documentNo,
            "productId", product.productId.toString(),
            "quantity", "10",
            "price", "0.01"
        );
        var purchaseReturn = store.createDocument("purchase-return", purchase.id, payload);

        assertThatCode(() -> store.updateDocument("purchase-return", purchaseReturn.id, purchase.id, payload))
            .doesNotThrowAnyException();
    }

    @Test
    void stockViewEditingContextRestoresTheEditedDocumentsReservation() throws Exception {
        var inbound = approvedPurchaseInbound("purchase_staff");
        var sales = store.userByUsername("sales_staff");
        var customer = enabledMaster("customer", sales.id);
        var product = inbound.items.get(0);
        var outbound = store.createDocument("sales-outbound", sales.id, Map.of(
            "warehouseId", inbound.warehouseId.toString(),
            "partnerId", customer.id.toString(),
            "productId", product.productId.toString(),
            "quantity", "4",
            "price", "123.45",
            "remark", "editing stock view"
        ));

        var editingStockMethod = ErpStore.class.getMethod("stockViews", Long.class, Long.class);
        @SuppressWarnings("unchecked")
        var adjustedRows = (java.util.List<Map<String, Object>>) editingStockMethod.invoke(store, sales.id, outbound.id);
        var adjusted = adjustedRows.stream()
            .filter(row -> inbound.warehouseId.equals(row.get("warehouseId")))
            .filter(row -> product.productId.equals(row.get("productId")))
            .findFirst()
            .orElseThrow();

        assertThat((java.math.BigDecimal) adjusted.get("availableQuantity")).isEqualByComparingTo("10.00");
    }

    @Test
    void stockViewEditingContextRejectsOtherCreatorsAndNonEditableDocuments() throws Exception {
        var inbound = approvedPurchaseInbound("purchase_staff");
        var sales = store.userByUsername("sales_staff");
        var customer = enabledMaster("customer", sales.id);
        var product = inbound.items.get(0);
        var outbound = store.createDocument("sales-outbound", sales.id, Map.of(
            "warehouseId", inbound.warehouseId.toString(),
            "partnerId", customer.id.toString(),
            "productId", product.productId.toString(),
            "quantity", "1",
            "price", "123.45",
            "remark", "editing context"
        ));
        var editingStockMethod = ErpStore.class.getMethod("stockViews", Long.class, Long.class);

        assertThatThrownBy(() -> editingStockMethod.invoke(
            store,
            store.userByUsername("sales_manager").id,
            outbound.id
        )).hasRootCauseInstanceOf(BusinessException.class);

        store.submitDocument(outbound.id, sales.id);
        assertThatThrownBy(() -> editingStockMethod.invoke(store, sales.id, outbound.id))
            .hasRootCauseInstanceOf(BusinessException.class);
    }
    @Test
    void disabledLocalizedMasterShadowsItsPublicSourceBeforeStatusFiltering() {
        var admin = store.userByUsername("student01_admin");
        var publicBrand = enabledPublicMaster("brand");
        var local = store.changeMasterStatus("brand", publicBrand.id, Status.DISABLED, admin.id);

        assertThat(store.masters("brand", null, Status.ENABLED.name(), admin.id))
            .extracting(record -> record.id)
            .doesNotContain(publicBrand.id, local.id);
        assertThat(store.masters("brand", null, null, admin.id))
            .filteredOn(record -> record.name.equals(publicBrand.name))
            .extracting(record -> record.id)
            .containsExactly(local.id);
    }

    @Test
    void sameNameLocalMasterShadowsThePublicMasterEvenWithoutLocalizedCode() {
        var admin = store.userByUsername("student01_admin");
        var publicBrand = enabledPublicMaster("brand");
        var local = store.createMaster("brand", Map.of("name", publicBrand.name), admin.id);

        assertThat(store.masters("brand", null, null, admin.id))
            .filteredOn(record -> record.name.equals(publicBrand.name))
            .extracting(record -> record.id)
            .containsExactly(local.id);
    }
    private DocumentRecord approvedPurchaseInbound(String creatorUsername) {
        var creator = store.userByUsername(creatorUsername);
        var inbound = store.createSimpleDocument("purchase-inbound", creator.id);
        store.submitDocument(inbound.id, creator.id);
        store.approve(inbound.id, store.userByUsername("warehouse_staff").id);
        return inbound;
    }

    private MasterRecord enabledMaster(String type, Long userId) {
        return store.masters(type, null, Status.ENABLED.name(), userId).stream()
            .findFirst()
            .orElseThrow();
    }

    private MasterRecord enabledPublicMaster(String type) {
        return store.masters(type, null, Status.ENABLED.name()).stream()
            .filter(record -> record.workspaceOwnerId == null)
            .findFirst()
            .orElseThrow();
    }
}
