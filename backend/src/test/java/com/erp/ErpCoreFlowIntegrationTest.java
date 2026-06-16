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
}
