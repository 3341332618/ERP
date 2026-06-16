package com.erp;

import com.erp.store.ErpStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ErpCoreFlowIntegrationTest {
    @Autowired
    ErpStore store;

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

    private BigDecimal actualQuantity(Long warehouseId, Long productId) {
        return store.stockList().stream()
            .filter(stock -> stock.warehouseId.equals(warehouseId) && stock.productId.equals(productId))
            .map(stock -> stock.actualQuantity)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }
}
