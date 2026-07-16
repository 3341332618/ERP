package com.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ErpEntities {
    private ErpEntities() {
    }

    @TableName("erp_workspace")
    public static class WorkspaceEntity {
        @TableId(type = IdType.INPUT)
        public Long id;
        public String code;
        public String name;
        @TableField("workspace_type")
        public String workspaceType;
        @TableField("owner_user_id")
        public Long ownerUserId;
        public String status;
        @TableField("created_at")
        public LocalDateTime createdAt;
        @TableField("updated_at")
        public LocalDateTime updatedAt;
    }

    @TableName("sys_user")
    public static class SysUserEntity {
        @TableId(type = IdType.INPUT)
        public Long id;
        @TableField("workspace_id")
        public Long workspaceId;
        @TableField("warehouse_id")
        public Long warehouseId;
        public String username;
        @TableField("password_hash")
        public String passwordHash;
        public String name;
        public String phone;
        public String avatar;
        @TableField("role_code")
        public String roleCode;
        public String status;
        @TableField("created_at")
        public LocalDateTime createdAt;
        @TableField("updated_at")
        public LocalDateTime updatedAt;
    }

    @TableName("master_product")
    public static class MasterProductEntity {
        @TableId(type = IdType.INPUT)
        public Long id;
        @TableField("workspace_id")
        public Long workspaceId;
        @TableField("category_id")
        public Long categoryId;
        @TableField("brand_id")
        public Long brandId;
        @TableField("unit_id")
        public Long unitId;
        public String code;
        public String name;
        @TableField("purchase_price")
        public BigDecimal purchasePrice;
        @TableField("sale_price")
        public BigDecimal salePrice;
        @TableField("image_data")
        public String imageData;
        public String status;
        @TableField("created_at")
        public LocalDateTime createdAt;
        @TableField("updated_at")
        public LocalDateTime updatedAt;
    }

    @TableName("biz_document")
    public static class BizDocumentEntity {
        @TableId(type = IdType.INPUT)
        public Long id;
        @TableField("workspace_id")
        public Long workspaceId;
        @TableField("document_type")
        public String documentType;
        @TableField("document_no")
        public String documentNo;
        @TableField("related_document_id")
        public Long relatedDocumentId;
        @TableField("warehouse_id")
        public Long warehouseId;
        @TableField("target_warehouse_id")
        public Long targetWarehouseId;
        @TableField("supplier_id")
        public Long supplierId;
        @TableField("customer_id")
        public Long customerId;
        @TableField("total_amount")
        public BigDecimal totalAmount;
        @TableField("document_status")
        public String documentStatus;
        @TableField("creator_id")
        public Long creatorId;
        @TableField("operation_time")
        public LocalDateTime operationTime;
        @TableField("auditor_id")
        public Long auditorId;
        @TableField("audit_time")
        public LocalDateTime auditTime;
        @TableField("reject_reason")
        public String rejectReason;
        public Long version;
    }

    @TableName("test_bug_definition")
    public static class BugDefinitionEntity {
        @TableId(type = IdType.INPUT)
        public String id;
        @TableField("role_name")
        public String roleName;
        @TableField("module_name")
        public String moduleName;
        @TableField("function_name")
        public String functionName;
        public String summary;
        @TableField("reproduce_steps")
        public String reproduceSteps;
        @TableField("expected_result")
        public String expectedResult;
        @TableField("actual_result")
        public String actualResult;
        public String severity;
        public Boolean active;
        @TableField("publisher_id")
        public Long publisherId;
        @TableField("publish_time")
        public LocalDateTime publishTime;
    }

    @TableName("test_bug_report")
    public static class BugReportEntity {
        @TableId(type = IdType.INPUT)
        public Long id;
        @TableField("bug_id")
        public String bugId;
        @TableField("student_id")
        public Long studentId;
        @TableField("workspace_id")
        public Long workspaceId;
        @TableField("bug_summary_snapshot")
        public String bugSummarySnapshot;
        @TableField("module_name")
        public String moduleName;
        public String title;
        @TableField("reproduce_steps")
        public String reproduceSteps;
        @TableField("expected_result")
        public String expectedResult;
        @TableField("actual_result")
        public String actualResult;
        public String evidence;
        @TableField("report_status")
        public String reportStatus;
        public Integer score;
        @TableField("review_comment")
        public String reviewComment;
        @TableField("reviewer_id")
        public Long reviewerId;
        @TableField("submit_time")
        public LocalDateTime submitTime;
        @TableField("review_time")
        public LocalDateTime reviewTime;
    }
}
