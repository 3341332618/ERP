package com.erp;

import com.erp.store.ErpStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

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
}
