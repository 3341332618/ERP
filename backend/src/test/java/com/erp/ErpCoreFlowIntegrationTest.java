package com.erp;

import com.erp.store.ErpStore;
import com.erp.common.BusinessException;
import com.erp.domain.ErpModels.RoleCode;
import com.erp.domain.ErpModels.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ErpCoreFlowIntegrationTest {
    @Autowired
    ErpStore store;

    @Test
    void adminMenuIncludesAllBusinessModules() {
        var adminMenus = store.menus(RoleCode.ADMIN);

        assertThat(adminMenus)
            .extracting(menu -> menu.title)
            .containsExactly("基础信息管理", "采购管理", "库存管理", "销售管理", "结算管理", "个人中心");
        assertThat(adminMenus.stream()
            .filter(menu -> "库存管理".equals(menu.title))
            .findFirst()
            .orElseThrow()
            .children)
            .extracting(menu -> menu.title)
            .contains("库存分布", "库存调拨");
    }

    @Test
    void purchaseApprovalCreatesStockAndExpenseSettlement() {
        var user = store.userByUsername("purchase_staff");
        var document = store.createSimpleDocument("purchase-inbound", user.id);
        store.submitDocument(document.id, user.id);

        var auditor = store.userByUsername("warehouse_staff");
        store.approve(document.id, auditor.id);

        assertThat(store.getDocument(document.id).status.label).isEqualTo("审核通过");
        assertThat(store.stockList()).anySatisfy(stock -> assertThat(stock.actualQuantity).isGreaterThan(BigDecimal.ZERO));
        assertThat(store.settlements("expense")).anySatisfy(record -> assertThat(record.documentType).isEqualTo("采入支出"));
    }

    @Test
    void profileAvatarCanBeUploadedAndProductImageFollowsDocumentUploadRule() {
        var admin = store.userByUsername("admin");
        var pngData = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";

        store.updateAvatar(admin.id, pngData);
        var product = store.createMaster("product", java.util.Map.of(
            "name", "上传图片商品",
            "categoryName", "办公设备",
            "brandName", "连想",
            "unitName", "台",
            "purchasePrice", "100",
            "salePrice", "120",
            "imageData", pngData
        ));

        assertThat(store.userById(admin.id).avatar).isEqualTo(pngData);
        assertThat(product.imageData).isEqualTo(pngData);
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> store.updateAvatar(admin.id, "data:image/gif;base64,AAAA"));
    }

    @Test
    void stockTransferMovesInventoryBetweenDifferentWarehouses() {
        var purchaseUser = store.userByUsername("purchase_staff");
        var inbound = store.createSimpleDocument("purchase-inbound", purchaseUser.id);
        store.submitDocument(inbound.id, purchaseUser.id);
        var auditor = store.userByUsername("warehouse_staff");
        store.approve(inbound.id, auditor.id);

        var sourceWarehouse = store.masterRecord("warehouse", inbound.warehouseId);
        var targetWarehouse = store.createMaster("warehouse", Map.of(
            "name", "华南仓库",
            "phone", "13800000005",
            "address", "深圳市南山区"
        ));
        var product = store.masterRecord("product", inbound.items.get(0).productId);
        var sourceBeforeTransfer = actualQuantity(sourceWarehouse.id, product.id);
        var manager = store.userByUsername("warehouse_manager");

        var transfer = store.createDocument("stock-transfer", manager.id, Map.of(
            "warehouseId", sourceWarehouse.id.toString(),
            "targetWarehouseId", targetWarehouse.id.toString(),
            "productId", product.id.toString(),
            "quantity", "4",
            "remark", "门店补货"
        ));

        assertThat(transfer.documentNo).startsWith("KD");
        assertThat(transfer.warehouseId).isEqualTo(sourceWarehouse.id);
        assertThat(transfer.targetWarehouseId).isEqualTo(targetWarehouse.id);
        assertThat(transfer.warehouseId).isNotEqualTo(transfer.targetWarehouseId);

        store.submitDocument(transfer.id, manager.id);
        store.approve(transfer.id, auditor.id);

        assertThat(actualQuantity(sourceWarehouse.id, product.id)).isEqualByComparingTo(sourceBeforeTransfer.subtract(new BigDecimal("4")));
        assertThat(actualQuantity(targetWarehouse.id, product.id)).isEqualByComparingTo(new BigDecimal("4"));
    }

    @Test
    void salesOutboundUsesSelectedDetailsAndCreatesIncomeSettlement() {
        var purchaseUser = store.userByUsername("purchase_staff");
        var inbound = store.createSimpleDocument("purchase-inbound", purchaseUser.id);
        store.submitDocument(inbound.id, purchaseUser.id);
        var auditor = store.userByUsername("warehouse_staff");
        store.approve(inbound.id, auditor.id);

        var warehouse = store.masterRecord("warehouse", inbound.warehouseId);
        var product = store.masterRecord("product", inbound.items.get(0).productId);
        var customer = store.masters("customer", null, null).get(0);
        var salesUser = store.userByUsername("sales_staff");
        var afterInbound = actualQuantity(warehouse.id, product.id);

        var outbound = store.createDocument("sales-outbound", salesUser.id, Map.of(
            "warehouseId", warehouse.id.toString(),
            "partnerId", customer.id.toString(),
            "productId", product.id.toString(),
            "quantity", "3",
            "price", "5300",
            "remark", "客户发货"
        ));
        store.submitDocument(outbound.id, salesUser.id);
        store.approve(outbound.id, auditor.id);

        var saved = store.getDocument(outbound.id);
        assertThat(saved.partnerName).isEqualTo(customer.name);
        assertThat(saved.items).singleElement().satisfies(item -> {
            assertThat(item.quantity).isEqualByComparingTo("3.00");
            assertThat(item.price).isEqualByComparingTo("5300.00");
            assertThat(item.amount).isEqualByComparingTo("15900.00");
            assertThat(item.remark).isEqualTo("客户发货");
        });
        assertThat(actualQuantity(warehouse.id, product.id)).isEqualByComparingTo(afterInbound.subtract(new BigDecimal("3.00")));
        assertThat(store.settlements("income"))
            .anySatisfy(record -> {
                assertThat(record.documentType).isEqualTo("销出收入");
                assertThat(record.relatedDocumentNo).isEqualTo(outbound.documentNo);
                assertThat(record.amount).isEqualByComparingTo("15900.00");
            });
    }

    @Test
    void purchaseReturnAndSalesReturnUpdateStockAndSettlementsFromRelatedDocuments() {
        var purchaseUser = store.userByUsername("purchase_staff");
        var inbound = store.createSimpleDocument("purchase-inbound", purchaseUser.id);
        store.submitDocument(inbound.id, purchaseUser.id);
        var auditor = store.userByUsername("warehouse_staff");
        store.approve(inbound.id, auditor.id);
        var product = store.masterRecord("product", inbound.items.get(0).productId);
        var afterInbound = actualQuantity(inbound.warehouseId, product.id);

        var purchaseReturn = store.createDocument("purchase-return", purchaseUser.id, Map.of(
            "warehouseId", inbound.warehouseId.toString(),
            "partnerId", inbound.partnerId.toString(),
            "productId", product.id.toString(),
            "quantity", "2",
            "price", "4200",
            "relatedDocumentNo", inbound.documentNo,
            "remark", "采购退货"
        ));
        store.submitDocument(purchaseReturn.id, purchaseUser.id);
        store.approve(purchaseReturn.id, auditor.id);
        var afterPurchaseReturn = actualQuantity(inbound.warehouseId, product.id);
        assertThat(afterPurchaseReturn).isEqualByComparingTo(afterInbound.subtract(new BigDecimal("2.00")));
        assertThat(store.settlements("income")).anySatisfy(record -> {
            assertThat(record.documentType).isEqualTo("采退收入");
            assertThat(record.relatedDocumentNo).isEqualTo(purchaseReturn.documentNo);
            assertThat(record.amount).isEqualByComparingTo("8400.00");
        });

        var salesUser = store.userByUsername("sales_staff");
        var customer = store.masters("customer", null, null).get(0);
        var outbound = store.createDocument("sales-outbound", salesUser.id, Map.of(
            "warehouseId", inbound.warehouseId.toString(),
            "partnerId", customer.id.toString(),
            "productId", product.id.toString(),
            "quantity", "3",
            "price", "5200",
            "remark", "销售出库"
        ));
        store.submitDocument(outbound.id, salesUser.id);
        store.approve(outbound.id, auditor.id);
        var afterSalesOutbound = actualQuantity(inbound.warehouseId, product.id);

        var salesReturn = store.createDocument("sales-return", salesUser.id, Map.of(
            "warehouseId", inbound.warehouseId.toString(),
            "partnerId", customer.id.toString(),
            "productId", product.id.toString(),
            "quantity", "1",
            "price", "5200",
            "relatedDocumentNo", outbound.documentNo,
            "remark", "销售退货"
        ));
        store.submitDocument(salesReturn.id, salesUser.id);
        store.approve(salesReturn.id, auditor.id);

        assertThat(store.getDocument(salesReturn.id).relatedDocumentNo).isEqualTo(outbound.documentNo);
        assertThat(afterSalesOutbound).isEqualByComparingTo(afterPurchaseReturn.subtract(new BigDecimal("3.00")));
        assertThat(actualQuantity(inbound.warehouseId, product.id)).isEqualByComparingTo(afterSalesOutbound.add(new BigDecimal("1.00")));
        assertThat(store.settlements("expense")).anySatisfy(record -> {
            assertThat(record.documentType).isEqualTo("销退支出");
            assertThat(record.relatedDocumentNo).isEqualTo(salesReturn.documentNo);
            assertThat(record.amount).isEqualByComparingTo("5200.00");
        });
    }

    @Test
    void disablingProductIsRejectedWhenItHasStockOrOpenDocuments() {
        var purchaseUser = store.userByUsername("purchase_staff");
        var inbound = store.createSimpleDocument("purchase-inbound", purchaseUser.id);
        store.submitDocument(inbound.id, purchaseUser.id);
        store.approve(inbound.id, store.userByUsername("warehouse_staff").id);
        var stockedProduct = store.masterRecord("product", inbound.items.get(0).productId);

        assertThatThrownBy(() -> store.changeMasterStatus("product", stockedProduct.id, Status.DISABLED))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("商品存在实际库存数量，无法禁用。");

        var pendingProduct = store.importProducts(List.of(Map.of(
            "name", "流转中商品",
            "categoryName", "办公设备",
            "brandName", "连想",
            "unitName", "台",
            "purchasePrice", "100",
            "salePrice", "150"
        ))).get(0);
        store.createDocument("purchase-inbound", purchaseUser.id, Map.of(
            "warehouseId", inbound.warehouseId.toString(),
            "partnerId", inbound.partnerId.toString(),
            "productId", pendingProduct.id.toString(),
            "quantity", "1",
            "price", "100"
        ));

        assertThatThrownBy(() -> store.changeMasterStatus("product", pendingProduct.id, Status.DISABLED))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("商品关联单据流转中，无法禁用。");
    }

    @Test
    void stockDistributionIncludesWarehouseAndProductDetails() {
        var purchaseUser = store.userByUsername("purchase_staff");
        var inbound = store.createSimpleDocument("purchase-inbound", purchaseUser.id);
        store.submitDocument(inbound.id, purchaseUser.id);
        store.approve(inbound.id, store.userByUsername("warehouse_staff").id);

        assertThat(store.stockViews()).anySatisfy(row -> {
            assertThat(row.get("warehouseCode")).isEqualTo(inbound.warehouseCode);
            assertThat(row.get("warehouseName")).isEqualTo(inbound.warehouseName);
            assertThat(row.get("productCode")).isEqualTo(inbound.items.get(0).productCode);
            assertThat(row.get("productName")).isEqualTo(inbound.items.get(0).productName);
            assertThat(row.get("categoryName")).isEqualTo(inbound.items.get(0).categoryName);
            assertThat(row.get("brandName")).isEqualTo(inbound.items.get(0).brandName);
            assertThat(row.get("unitName")).isEqualTo(inbound.items.get(0).unitName);
        });
    }

    @Test
    void settlementDetailIncludesRelatedApprovedDocument() {
        var purchaseUser = store.userByUsername("purchase_staff");
        var inbound = store.createSimpleDocument("purchase-inbound", purchaseUser.id);
        store.submitDocument(inbound.id, purchaseUser.id);
        store.approve(inbound.id, store.userByUsername("warehouse_staff").id);

        var settlement = store.settlements("expense").stream()
            .filter(record -> record.relatedDocumentNo.equals(inbound.documentNo))
            .findFirst()
            .orElseThrow();
        var detail = store.settlementDetail("expense", settlement.id);

        assertThat(detail.get("settlement")).isEqualTo(settlement);
        assertThat(detail.get("document")).isEqualTo(inbound);
    }

    private BigDecimal actualQuantity(Long warehouseId, Long productId) {
        return store.stockList().stream()
            .filter(stock -> stock.warehouseId.equals(warehouseId) && stock.productId.equals(productId))
            .map(stock -> stock.actualQuantity)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }
}
