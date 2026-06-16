package com.erp.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ErpModels {
    private ErpModels() {
    }

    public enum Status {
        ENABLED("启用"),
        DISABLED("禁用");

        public final String label;

        Status(String label) {
            this.label = label;
        }
    }

    public enum DocumentStatus {
        DRAFT("待提交"),
        PENDING("待审核"),
        APPROVED("审核通过"),
        REJECTED("审核拒绝");

        public final String label;

        DocumentStatus(String label) {
            this.label = label;
        }
    }

    public enum DocumentType {
        PURCHASE_INBOUND("purchase-inbound", "采购入库", "CR", true, false),
        PURCHASE_RETURN("purchase-return", "采购退货", "CT", false, true),
        SALES_OUTBOUND("sales-outbound", "销售出库", "XC", false, true),
        SALES_RETURN("sales-return", "销售退货", "XT", true, false),
        STOCK_TRANSFER("stock-transfer", "库存调拨", "KD", false, true);

        public final String code;
        public final String label;
        public final String prefix;
        public final boolean inbound;
        public final boolean outbound;

        DocumentType(String code, String label, String prefix, boolean inbound, boolean outbound) {
            this.code = code;
            this.label = label;
            this.prefix = prefix;
            this.inbound = inbound;
            this.outbound = outbound;
        }

        public static DocumentType byCode(String code) {
            for (DocumentType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知单据类型");
        }
    }

    public enum RoleCode {
        ADMIN("系统管理员"),
        PURCHASE_MANAGER("采购主管"),
        PURCHASE_STAFF("采购专员"),
        WAREHOUSE_MANAGER("仓库主管"),
        WAREHOUSE_STAFF("仓库专员"),
        SALES_MANAGER("销售主管"),
        SALES_STAFF("销售专员"),
        SETTLEMENT_MANAGER("结算主管");

        public final String label;

        RoleCode(String label) {
            this.label = label;
        }
    }

    public static class User {
        public Long id;
        public String username;
        public String passwordHash;
        public String name;
        public String phone;
        public String avatar;
        public RoleCode role;
        public Status status = Status.ENABLED;
        public LocalDateTime createTime = LocalDateTime.now();
        public Long warehouseId;
    }

    public static class MenuNode {
        public String title;
        public String path;
        public List<MenuNode> children = new ArrayList<>();

        public MenuNode(String title, String path) {
            this.title = title;
            this.path = path;
        }

        public MenuNode add(MenuNode child) {
            this.children.add(child);
            return this;
        }
    }

    public static class Message {
        public Long id;
        public Long userId;
        public String title;
        public String content;
        public boolean read;
        public LocalDateTime createTime = LocalDateTime.now();
    }

    public static class MasterRecord {
        public Long id;
        public String type;
        public String code;
        public String name;
        public String categoryName;
        public String brandName;
        public String unitName;
        public BigDecimal purchasePrice;
        public BigDecimal salePrice;
        public String imageData;
        public String contact;
        public String phone;
        public String address;
        public String settlementMethod;
        public Long warehouseUserId;
        public Status status = Status.ENABLED;
        public LocalDateTime createTime = LocalDateTime.now();
        public LocalDateTime updateTime;
    }

    public static class DocumentItem {
        public Long productId;
        public String productCode;
        public String productName;
        public String categoryName;
        public String brandName;
        public String unitName;
        public BigDecimal quantity = BigDecimal.ZERO;
        public BigDecimal price = BigDecimal.ZERO;
        public BigDecimal amount = BigDecimal.ZERO;
        public BigDecimal availableQuantity = BigDecimal.ZERO;
        public String remark;
    }

    public static class DocumentRecord {
        public Long id;
        public DocumentType type;
        public String documentNo;
        public String relatedDocumentNo;
        public Long warehouseId;
        public String warehouseCode;
        public String warehouseName;
        public Long targetWarehouseId;
        public String targetWarehouseCode;
        public String targetWarehouseName;
        public Long partnerId;
        public String partnerCode;
        public String partnerName;
        public List<DocumentItem> items = new ArrayList<>();
        public BigDecimal totalAmount = BigDecimal.ZERO;
        public DocumentStatus status = DocumentStatus.DRAFT;
        public Long creatorId;
        public String creatorName;
        public LocalDateTime operationTime = LocalDateTime.now();
        public Long auditorId;
        public String auditorName;
        public LocalDateTime auditTime;
        public String rejectReason;
    }

    public static class StockBalance {
        public Long warehouseId;
        public Long productId;
        public BigDecimal actualQuantity = BigDecimal.ZERO;
        public BigDecimal availableQuantity = BigDecimal.ZERO;
    }

    public static class SettlementRecord {
        public Long id;
        public String direction;
        public String settlementNo;
        public String documentType;
        public BigDecimal amount;
        public String relatedDocumentNo;
        public LocalDateTime createTime = LocalDateTime.now();
    }
}
