package com.erp.dto;

import java.math.BigDecimal;
import java.util.List;

public final class DocumentDtos {
    private DocumentDtos() {
    }

    public record ReturnDocumentOption(
        Long documentId,
        String documentNo,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long partnerId,
        String partnerCode,
        String partnerName,
        List<ReturnItemOption> items
    ) {
    }

    public record ReturnItemOption(
        Long productId,
        String productCode,
        String productName,
        String unitName,
        BigDecimal price,
        BigDecimal originalQuantity,
        BigDecimal returnedQuantity,
        BigDecimal remainingQuantity
    ) {
    }
}