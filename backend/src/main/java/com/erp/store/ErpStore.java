package com.erp.store;

import com.erp.common.BusinessException;
import com.erp.domain.ErpModels.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Service
public class ErpStore {
    private final PasswordEncoder passwordEncoder;
    private final AtomicLong ids = new AtomicLong(1000);
    private final Map<Long, User> users = new LinkedHashMap<>();
    private final Map<String, MasterRecord> master = new LinkedHashMap<>();
    private final Map<Long, DocumentRecord> documents = new LinkedHashMap<>();
    private final List<Message> messages = new ArrayList<>();
    private final List<StockBalance> stocks = new ArrayList<>();
    private final List<SettlementRecord> settlements = new ArrayList<>();
    private int warehouseSeq = 1;
    private int customerSeq = 1;
    private int supplierSeq = 1;
    private int productSeq = 1;
    private int documentSeq = 1;
    private int settlementSeq = 1;

    public ErpStore(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        seed();
    }

    private void seed() {
        createUser("admin", "系统管理员", "13800000001", RoleCode.ADMIN, null);
        createUser("purchase_manager", "采购主管", "13800000002", RoleCode.PURCHASE_MANAGER, null);
        createUser("purchase_staff", "采购专员", "13800000003", RoleCode.PURCHASE_STAFF, null);
        createUser("warehouse_manager", "仓库主管", "13800000004", RoleCode.WAREHOUSE_MANAGER, null);
        var warehouseStaff = createUser("warehouse_staff", "仓库专员", "13800000005", RoleCode.WAREHOUSE_STAFF, null);
        createUser("sales_manager", "销售主管", "13800000006", RoleCode.SALES_MANAGER, null);
        createUser("sales_staff", "销售专员", "13800000007", RoleCode.SALES_STAFF, null);
        createUser("settlement_manager", "结算主管", "13800000008", RoleCode.SETTLEMENT_MANAGER, null);

        var brand = createMaster("brand", Map.of("name", "连想"));
        var category = createMaster("category", Map.of("name", "办公设备"));
        var unit = createMaster("unit", Map.of("name", "台"));
        var warehouse = createMaster("warehouse", Map.of(
            "name", "华东仓库",
            "phone", "13800000005",
            "address", "上海市浦东新区",
            "warehouseUserId", warehouseStaff.id.toString()
        ));
        warehouseStaff.warehouseId = warehouse.id;
        createMaster("customer", Map.of("name", "上海客户", "contact", "张三", "phone", "13800001000", "address", "上海市徐汇区"));
        createMaster("supplier", Map.of("name", "北京供应商", "contact", "李四", "phone", "13800002000", "address", "北京市朝阳区"));
        createMaster("product", Map.of(
            "name", "笔记本电脑",
            "categoryName", category.name,
            "brandName", brand.name,
            "unitName", unit.name,
            "purchasePrice", "4200",
            "salePrice", "5200"
        ));
    }

    private User createUser(String username, String name, String phone, RoleCode role, Long warehouseId) {
        var user = new User();
        user.id = ids.incrementAndGet();
        user.username = username;
        user.passwordHash = passwordEncoder.encode("123456");
        user.name = name;
        user.phone = phone;
        user.role = role;
        user.warehouseId = warehouseId;
        users.put(user.id, user);
        return user;
    }

    public User userByUsername(String username) {
        return users.values().stream()
            .filter(user -> user.username.equals(username))
            .findFirst()
            .orElseThrow(() -> new BusinessException("登录账号不存在！"));
    }

    public User userById(Long id) {
        var user = users.get(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public User updateAvatar(Long userId, String avatarData) {
        var user = userById(userId);
        user.avatar = validatedImage(avatarData);
        return user;
    }

    public List<MenuNode> menus(RoleCode role) {
        var menus = new ArrayList<MenuNode>();
        if (role == RoleCode.ADMIN) {
            menus.add(baseInfoMenu());
            menus.add(purchaseMenu());
            menus.add(inventoryMenu());
            menus.add(salesMenu());
            menus.add(settlementMenu());
        }
        if (role == RoleCode.PURCHASE_MANAGER || role == RoleCode.PURCHASE_STAFF) {
            menus.add(purchaseMenu());
        }
        if (role == RoleCode.WAREHOUSE_MANAGER || role == RoleCode.WAREHOUSE_STAFF) {
            menus.add(inventoryMenu());
        }
        if (role == RoleCode.SALES_MANAGER || role == RoleCode.SALES_STAFF) {
            menus.add(salesMenu());
        }
        if (role == RoleCode.SETTLEMENT_MANAGER) {
            menus.add(settlementMenu());
        }
        return menus;
    }

    private MenuNode baseInfoMenu() {
        return new MenuNode("基础信息管理", "/master/brand")
            .add(new MenuNode("商品品牌", "/master/brand"))
            .add(new MenuNode("商品分类", "/master/category"))
            .add(new MenuNode("商品单位", "/master/unit"))
            .add(new MenuNode("商品管理", "/master/product"))
            .add(new MenuNode("仓库信息", "/master/warehouse"))
            .add(new MenuNode("客户信息", "/master/customer"))
            .add(new MenuNode("供应商信息", "/master/supplier"));
    }

    private MenuNode purchaseMenu() {
        return new MenuNode("采购管理", "/purchase/inbound")
            .add(new MenuNode("采购入库", "/purchase/inbound"))
            .add(new MenuNode("采购退货", "/purchase/return"));
    }

    private MenuNode inventoryMenu() {
        return new MenuNode("库存管理", "/inventory/stock")
            .add(new MenuNode("库存分布", "/inventory/stock"))
            .add(new MenuNode("入库审核", "/inventory/inbound-audit"))
            .add(new MenuNode("出库审核", "/inventory/outbound-audit"))
            .add(new MenuNode("库存调拨", "/inventory/transfer"));
    }

    private MenuNode salesMenu() {
        return new MenuNode("销售管理", "/sales/outbound")
            .add(new MenuNode("销售出库", "/sales/outbound"))
            .add(new MenuNode("销售退货", "/sales/return"));
    }

    private MenuNode settlementMenu() {
        return new MenuNode("结算管理", "/settlement/income")
            .add(new MenuNode("收入结算", "/settlement/income"))
            .add(new MenuNode("支出结算", "/settlement/expense"));
    }

    public List<Message> messages(Long userId) {
        return messages.stream()
            .filter(message -> message.userId.equals(userId))
            .sorted(Comparator.comparing((Message message) -> message.createTime).reversed())
            .toList();
    }

    public List<MasterRecord> masters(String type, String keyword, String status) {
        return master.values().stream()
            .filter(record -> record.type.equals(type))
            .filter(record -> keyword == null || keyword.isBlank()
                || (record.code != null && record.code.contains(keyword))
                || record.name.contains(keyword))
            .filter(record -> status == null || status.isBlank() || record.status.name().equals(status))
            .sorted(Comparator.comparing((MasterRecord record) -> record.createTime).reversed())
            .toList();
    }

    public MasterRecord createMaster(String type, Map<String, String> payload) {
        require(payload.get("name"), displayName(type) + "名称必填，请重新输入。");
        var record = new MasterRecord();
        record.id = ids.incrementAndGet();
        record.type = type;
        record.name = payload.get("name");
        record.code = nextMasterCode(type, payload);
        record.categoryName = payload.get("categoryName");
        record.brandName = payload.get("brandName");
        record.unitName = payload.get("unitName");
        record.purchasePrice = money(payload.getOrDefault("purchasePrice", "0"));
        record.salePrice = money(payload.getOrDefault("salePrice", "0"));
        record.imageData = validatedImage(payload.get("imageData"));
        record.contact = payload.get("contact");
        record.phone = payload.get("phone");
        record.address = payload.get("address");
        record.settlementMethod = payload.getOrDefault("settlementMethod", defaultSettlement(type));
        if (payload.get("warehouseUserId") != null) {
            record.warehouseUserId = Long.valueOf(payload.get("warehouseUserId"));
        }
        master.put(type + ":" + record.id, record);
        return record;
    }

    public MasterRecord updateMaster(String type, Long id, Map<String, String> payload) {
        var record = masterRecord(type, id);
        require(payload.get("name"), displayName(type) + "名称必填，请重新输入。");
        record.name = payload.get("name");
        record.categoryName = payload.getOrDefault("categoryName", record.categoryName);
        record.brandName = payload.getOrDefault("brandName", record.brandName);
        record.unitName = payload.getOrDefault("unitName", record.unitName);
        record.purchasePrice = money(payload.getOrDefault("purchasePrice", record.purchasePrice == null ? "0" : record.purchasePrice.toString()));
        record.salePrice = money(payload.getOrDefault("salePrice", record.salePrice == null ? "0" : record.salePrice.toString()));
        if (payload.containsKey("imageData")) {
            record.imageData = validatedImage(payload.get("imageData"));
        }
        record.contact = payload.getOrDefault("contact", record.contact);
        record.phone = payload.getOrDefault("phone", record.phone);
        record.address = payload.getOrDefault("address", record.address);
        record.updateTime = LocalDateTime.now();
        if ("product".equals(type)) {
            record.code = productCode(record.categoryName, record.brandName, productSeq - 1);
        }
        return record;
    }

    public List<MasterRecord> importProducts(List<Map<String, String>> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("导入文件内容不能为空。");
        }
        var errors = new ArrayList<String>();
        var normalizedRows = new ArrayList<Map<String, String>>();
        for (int i = 0; i < rows.size(); i++) {
            var rowNumber = i + 2;
            var row = rows.get(i);
            var name = importValue(row, "name", "商品名称");
            var categoryName = importValue(row, "categoryName", "商品分类");
            var brandName = importValue(row, "brandName", "商品品牌");
            var unitName = importValue(row, "unitName", "商品单位");
            var purchasePrice = importValue(row, "purchasePrice", "建议采购价（元）");
            var salePrice = importValue(row, "salePrice", "建议零售价（元）");
            if (name.isBlank()) {
                errors.add("第" + rowNumber + "行：商品名称必填");
            }
            if (categoryName.isBlank()) {
                errors.add("第" + rowNumber + "行：商品分类必填");
            }
            if (brandName.isBlank()) {
                errors.add("第" + rowNumber + "行：商品品牌必填");
            }
            if (unitName.isBlank()) {
                errors.add("第" + rowNumber + "行：商品单位必填");
            }
            if (!validMoney(purchasePrice)) {
                errors.add("第" + rowNumber + "行：建议采购价（元）输入有误");
            }
            if (!validMoney(salePrice)) {
                errors.add("第" + rowNumber + "行：建议零售价（元）输入有误");
            }
            normalizedRows.add(Map.of(
                "name", name,
                "categoryName", categoryName,
                "brandName", brandName,
                "unitName", unitName,
                "purchasePrice", purchasePrice,
                "salePrice", salePrice
            ));
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("；", errors));
        }
        return normalizedRows.stream()
            .map(row -> createMaster("product", row))
            .toList();
    }

    public MasterRecord changeMasterStatus(String type, Long id, Status status) {
        var record = masterRecord(type, id);
        if (status == Status.DISABLED) {
            assertCanDisable(record);
        }
        record.status = status;
        record.updateTime = LocalDateTime.now();
        return record;
    }

    public MasterRecord masterRecord(String type, Long id) {
        var record = master.get(type + ":" + id);
        if (record == null) {
            throw new BusinessException(displayName(type) + "不存在");
        }
        return record;
    }

    private void assertCanDisable(MasterRecord record) {
        if ("product".equals(record.type)) {
            var hasStock = stocks.stream()
                .anyMatch(stock -> stock.productId.equals(record.id) && stock.actualQuantity.compareTo(BigDecimal.ZERO) > 0);
            if (hasStock) {
                throw new BusinessException("商品存在实际库存数量，无法禁用。");
            }
            if (hasOpenDocumentForProduct(record.id)) {
                throw new BusinessException("商品关联单据流转中，无法禁用。");
            }
        }
        if ("warehouse".equals(record.type)) {
            var hasStock = stocks.stream()
                .anyMatch(stock -> stock.warehouseId.equals(record.id) && stock.actualQuantity.compareTo(BigDecimal.ZERO) > 0);
            if (hasStock) {
                throw new BusinessException("仓库存在商品，无法禁用。");
            }
            if (hasOpenDocumentForWarehouse(record.id)) {
                throw new BusinessException("仓库关联单据流转中，无法禁用。");
            }
        }
        if ("customer".equals(record.type) || "supplier".equals(record.type)) {
            if (hasOpenDocumentForPartner(record.id)) {
                throw new BusinessException(displayName(record.type) + "关联单据流转中，无法禁用。");
            }
        }
    }

    private boolean hasOpenDocumentForProduct(Long productId) {
        return documents.values().stream()
            .filter(document -> document.status != DocumentStatus.APPROVED)
            .flatMap(document -> document.items.stream())
            .anyMatch(item -> item.productId.equals(productId));
    }

    private boolean hasOpenDocumentForWarehouse(Long warehouseId) {
        return documents.values().stream()
            .filter(document -> document.status != DocumentStatus.APPROVED)
            .anyMatch(document -> warehouseId.equals(document.warehouseId) || warehouseId.equals(document.targetWarehouseId));
    }

    private boolean hasOpenDocumentForPartner(Long partnerId) {
        return documents.values().stream()
            .filter(document -> document.status != DocumentStatus.APPROVED)
            .anyMatch(document -> partnerId.equals(document.partnerId));
    }

    public List<DocumentRecord> documents(String typeCode, Long userId) {
        var user = userById(userId);
        var type = DocumentType.byCode(typeCode);
        return documents.values().stream()
            .filter(document -> document.type == type)
            .filter(document -> canSeeDocument(user, document))
            .sorted(Comparator.comparing((DocumentRecord document) -> document.operationTime).reversed())
            .toList();
    }

    public DocumentRecord createSimpleDocument(String typeCode, Long userId) {
        return createDocument(typeCode, userId, Map.of());
    }

    public DocumentRecord createDocument(String typeCode, Long userId, Map<String, ?> payload) {
        var type = DocumentType.byCode(typeCode);
        var document = new DocumentRecord();
        document.id = ids.incrementAndGet();
        document.type = type;
        document.documentNo = type.prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "%04d".formatted(documentSeq++);
        var user = userById(userId);
        document.creatorId = user.id;
        document.creatorName = user.name;
        fillDocumentParties(document);
        var data = payload == null ? Map.<String, Object>of() : payload;
        applyDocumentPayload(document, data);
        document.items.add(data.isEmpty() ? defaultItem(document) : documentItem(document, data));
        recalc(document);
        documents.put(document.id, document);
        return document;
    }

    public DocumentRecord updateDocument(String typeCode, Long id, Long userId, Map<String, ?> payload) {
        var document = getDocument(id);
        if (document.type != DocumentType.byCode(typeCode)) {
            throw new BusinessException("单据类型不匹配");
        }
        if (!document.creatorId.equals(userId)) {
            throw new BusinessException("只能修改本人发起的单据");
        }
        if (document.status != DocumentStatus.DRAFT && document.status != DocumentStatus.REJECTED) {
            throw new BusinessException("当前状态不可修改");
        }
        var data = payload == null ? Map.<String, Object>of() : payload;
        fillDocumentParties(document);
        applyDocumentPayload(document, data);
        document.items.clear();
        document.items.add(data.isEmpty() ? defaultItem(document) : documentItem(document, data));
        document.operationTime = LocalDateTime.now();
        recalc(document);
        return document;
    }

    public DocumentRecord getDocument(Long id) {
        var document = documents.get(id);
        if (document == null) {
            throw new BusinessException("单据不存在");
        }
        return document;
    }

    public DocumentRecord submitDocument(Long id, Long userId) {
        var document = getDocument(id);
        if (!document.creatorId.equals(userId)) {
            throw new BusinessException("只能提交本人发起的单据");
        }
        if (document.status != DocumentStatus.DRAFT && document.status != DocumentStatus.REJECTED) {
            throw new BusinessException("当前状态不可提交");
        }
        document.status = DocumentStatus.PENDING;
        document.operationTime = LocalDateTime.now();
        notifyWarehouse(document);
        return document;
    }

    public DocumentRecord deleteDocument(Long id, Long userId) {
        var document = getDocument(id);
        if (!document.creatorId.equals(userId)) {
            throw new BusinessException("只能删除本人发起的单据");
        }
        if (document.status != DocumentStatus.DRAFT && document.status != DocumentStatus.REJECTED) {
            throw new BusinessException("当前状态不可删除");
        }
        documents.remove(id);
        return document;
    }

    public List<DocumentRecord> auditList(String direction, Long userId) {
        var user = userById(userId);
        Predicate<DocumentRecord> directionFilter = "inbound".equals(direction)
            ? document -> document.type.inbound || document.type == DocumentType.STOCK_TRANSFER
            : document -> document.type.outbound;
        return documents.values().stream()
            .filter(directionFilter)
            .filter(document -> document.status != DocumentStatus.DRAFT)
            .filter(document -> user.role == RoleCode.WAREHOUSE_MANAGER
                || (user.role == RoleCode.WAREHOUSE_STAFF && documentWarehouseMatches(user, document)))
            .sorted(Comparator.comparing((DocumentRecord document) -> document.operationTime).reversed())
            .toList();
    }

    public synchronized DocumentRecord approve(Long id, Long auditorId) {
        var auditor = userById(auditorId);
        var document = getDocument(id);
        if (auditor.role != RoleCode.WAREHOUSE_STAFF) {
            throw new BusinessException("当前角色无审核权限");
        }
        if (document.status != DocumentStatus.PENDING) {
            throw new BusinessException("当前状态不可审核");
        }
        if (!documentWarehouseMatches(auditor, document)) {
            throw new BusinessException("只能审核所属仓库单据");
        }
        applyStock(document);
        document.status = DocumentStatus.APPROVED;
        document.auditorId = auditor.id;
        document.auditorName = auditor.name;
        document.auditTime = LocalDateTime.now();
        recalcAvailable();
        createSettlement(document);
        return document;
    }

    public DocumentRecord reject(Long id, Long auditorId, String reason) {
        require(reason, "拒绝原因必填，请重新输入。");
        var auditor = userById(auditorId);
        var document = getDocument(id);
        if (auditor.role != RoleCode.WAREHOUSE_STAFF) {
            throw new BusinessException("当前角色无审核权限");
        }
        document.status = DocumentStatus.REJECTED;
        document.auditorId = auditor.id;
        document.auditorName = auditor.name;
        document.auditTime = LocalDateTime.now();
        document.rejectReason = reason;
        return document;
    }

    public List<StockBalance> stockList() {
        recalcAvailable();
        return stocks.stream()
            .filter(stock -> stock.actualQuantity.compareTo(BigDecimal.ZERO) > 0)
            .toList();
    }

    public List<Map<String, Object>> stockViews() {
        recalcAvailable();
        return stockList().stream()
            .<Map<String, Object>>map(stock -> {
                var warehouse = masterRecord("warehouse", stock.warehouseId);
                var product = masterRecord("product", stock.productId);
                var row = new LinkedHashMap<String, Object>();
                row.put("warehouseId", stock.warehouseId);
                row.put("warehouseCode", warehouse.code);
                row.put("warehouseName", warehouse.name);
                row.put("productId", stock.productId);
                row.put("productCode", product.code);
                row.put("productName", product.name);
                row.put("categoryName", product.categoryName);
                row.put("brandName", product.brandName);
                row.put("unitName", product.unitName);
                row.put("imageData", product.imageData);
                row.put("actualQuantity", stock.actualQuantity);
                row.put("availableQuantity", stock.availableQuantity);
                return row;
            })
            .toList();
    }

    public List<SettlementRecord> settlements(String direction) {
        return settlements.stream()
            .filter(record -> record.direction.equals(direction))
            .sorted(Comparator.comparing((SettlementRecord record) -> record.createTime).reversed())
            .toList();
    }

    public Map<String, Object> settlementDetail(String direction, Long id) {
        var settlement = settlements.stream()
            .filter(record -> record.id.equals(id) && record.direction.equals(direction))
            .findFirst()
            .orElseThrow(() -> new BusinessException("结算单不存在"));
        var document = documents.values().stream()
            .filter(item -> item.documentNo.equals(settlement.relatedDocumentNo))
            .findFirst()
            .orElseThrow(() -> new BusinessException("关联单据不存在"));
        var detail = new LinkedHashMap<String, Object>();
        detail.put("settlement", settlement);
        detail.put("document", document);
        return detail;
    }

    public List<MasterRecord> usersByRole(RoleCode role) {
        return users.values().stream()
            .filter(user -> user.role == role)
            .map(user -> {
                var record = new MasterRecord();
                record.id = user.id;
                record.name = user.name;
                record.phone = user.phone;
                record.status = user.status;
                return record;
            })
            .toList();
    }

    private void fillDocumentParties(DocumentRecord document) {
        var warehouse = first("warehouse");
        document.warehouseId = warehouse.id;
        document.warehouseCode = warehouse.code;
        document.warehouseName = warehouse.name;
        if (document.type == DocumentType.STOCK_TRANSFER) {
            document.targetWarehouseId = warehouse.id;
            document.targetWarehouseCode = warehouse.code;
            document.targetWarehouseName = warehouse.name;
            return;
        }
        var partnerType = document.type == DocumentType.PURCHASE_INBOUND || document.type == DocumentType.PURCHASE_RETURN ? "supplier" : "customer";
        var partner = first(partnerType);
        document.partnerId = partner.id;
        document.partnerCode = partner.code;
        document.partnerName = partner.name;
    }

    private DocumentItem defaultItem(DocumentRecord document) {
        var product = first("product");
        var item = new DocumentItem();
        item.productId = product.id;
        item.productCode = product.code;
        item.productName = product.name;
        item.categoryName = product.categoryName;
        item.brandName = product.brandName;
        item.unitName = product.unitName;
        item.quantity = BigDecimal.TEN;
        item.price = defaultPrice(document.type, product);
        item.amount = item.quantity.multiply(item.price).setScale(2, RoundingMode.HALF_UP);
        item.availableQuantity = available(document.warehouseId, product.id);
        item.remark = document.type == DocumentType.STOCK_TRANSFER ? "正常调拨" : "";
        return item;
    }

    private void applyDocumentPayload(DocumentRecord document, Map<String, ?> payload) {
        if (payload.isEmpty()) {
            return;
        }
        var sourceWarehouse = payload.containsKey("warehouseId")
            ? masterRecord("warehouse", longRequired(payload, "warehouseId", document.type == DocumentType.STOCK_TRANSFER ? "调出仓库必填，请重新输入。" : "仓库必填，请重新输入。"))
            : masterRecord("warehouse", document.warehouseId);
        setWarehouse(document, sourceWarehouse);
        if (document.type == DocumentType.STOCK_TRANSFER) {
            var targetWarehouse = masterRecord("warehouse", longRequired(payload, "targetWarehouseId", "调入仓库必填，请重新输入。"));
            if (sourceWarehouse.id.equals(targetWarehouse.id)) {
                throw new BusinessException("调入仓库不能与调出仓库相同，请重新选择。");
            }
            document.targetWarehouseId = targetWarehouse.id;
            document.targetWarehouseCode = targetWarehouse.code;
            document.targetWarehouseName = targetWarehouse.name;
            return;
        }
        if (payload.containsKey("relatedDocumentNo")) {
            document.relatedDocumentNo = text(payload, "relatedDocumentNo");
        }
        if (payload.containsKey("partnerId")) {
            var partnerType = document.type == DocumentType.PURCHASE_INBOUND || document.type == DocumentType.PURCHASE_RETURN ? "supplier" : "customer";
            setPartner(document, masterRecord(partnerType, longRequired(payload, "partnerId", partnerLabel(document.type) + "必填，请重新输入。")));
        }
    }

    private DocumentItem documentItem(DocumentRecord document, Map<String, ?> payload) {
        var product = masterRecord("product", longRequired(payload, "productId", "商品必填，请重新输入。"));
        var item = new DocumentItem();
        item.productId = product.id;
        item.productCode = product.code;
        item.productName = product.name;
        item.categoryName = product.categoryName;
        item.brandName = product.brandName;
        item.unitName = product.unitName;
        item.quantity = quantityRequired(text(payload, "quantity"), quantityMessage(document.type));
        item.price = document.type == DocumentType.STOCK_TRANSFER
            ? BigDecimal.ZERO
            : money(payload.containsKey("price") ? text(payload, "price") : defaultPrice(document.type, product).toString());
        item.availableQuantity = available(document.warehouseId, product.id);
        item.remark = text(payload, "remark");
        if (document.type == DocumentType.STOCK_TRANSFER) {
            require(item.remark, "备注必填，请重新输入。");
        }
        if (requiresAvailableStock(document.type) && item.quantity.compareTo(item.availableQuantity) > 0) {
            throw new BusinessException("可用库存不足，请重新输入" + stockQuantityLabel(document.type) + "。");
        }
        return item;
    }

    private void setWarehouse(DocumentRecord document, MasterRecord warehouse) {
        document.warehouseId = warehouse.id;
        document.warehouseCode = warehouse.code;
        document.warehouseName = warehouse.name;
    }

    private void setPartner(DocumentRecord document, MasterRecord partner) {
        document.partnerId = partner.id;
        document.partnerCode = partner.code;
        document.partnerName = partner.name;
    }

    private BigDecimal defaultPrice(DocumentType type, MasterRecord product) {
        var price = type == DocumentType.SALES_OUTBOUND || type == DocumentType.SALES_RETURN
            ? product.salePrice
            : product.purchasePrice;
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return type == DocumentType.SALES_OUTBOUND || type == DocumentType.SALES_RETURN
                ? new BigDecimal("5200")
                : new BigDecimal("4200");
        }
        return price;
    }

    private boolean requiresAvailableStock(DocumentType type) {
        return type == DocumentType.PURCHASE_RETURN || type == DocumentType.SALES_OUTBOUND || type == DocumentType.STOCK_TRANSFER;
    }

    private String stockQuantityLabel(DocumentType type) {
        return switch (type) {
            case PURCHASE_RETURN -> "采退数量";
            case SALES_OUTBOUND -> "销售出库数量";
            case STOCK_TRANSFER -> "调出数量";
            default -> "数量";
        };
    }

    private String quantityMessage(DocumentType type) {
        return stockQuantityLabel(type) + "必填，请重新输入。";
    }

    private String partnerLabel(DocumentType type) {
        return type == DocumentType.PURCHASE_INBOUND || type == DocumentType.PURCHASE_RETURN ? "供应商" : "客户";
    }

    private void recalc(DocumentRecord document) {
        document.totalAmount = document.items.stream()
            .peek(item -> item.amount = item.quantity.multiply(item.price).setScale(2, RoundingMode.HALF_UP))
            .map(item -> item.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean canSeeDocument(User user, DocumentRecord document) {
        return switch (user.role) {
            case PURCHASE_MANAGER, SALES_MANAGER, WAREHOUSE_MANAGER, SETTLEMENT_MANAGER, ADMIN -> true;
            case PURCHASE_STAFF, SALES_STAFF -> document.creatorId.equals(user.id);
            case WAREHOUSE_STAFF -> documentWarehouseMatches(user, document);
        };
    }

    private boolean documentWarehouseMatches(User user, DocumentRecord document) {
        return user.warehouseId == null
            || user.warehouseId.equals(document.warehouseId)
            || user.warehouseId.equals(document.targetWarehouseId);
    }

    private void notifyWarehouse(DocumentRecord document) {
        users.values().stream()
            .filter(user -> user.role == RoleCode.WAREHOUSE_STAFF)
            .filter(user -> documentWarehouseMatches(user, document))
            .forEach(user -> {
                var message = new Message();
                message.id = ids.incrementAndGet();
                message.userId = user.id;
                message.title = document.type.inbound ? "入库审核" : "出库审核";
                message.content = "【" + message.title + "】单据号：" + document.documentNo + " 待审核 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                messages.add(message);
            });
    }

    private void applyStock(DocumentRecord document) {
        if (document.type == DocumentType.PURCHASE_INBOUND || document.type == DocumentType.SALES_RETURN) {
            document.items.forEach(item -> changeStock(document.warehouseId, item.productId, item.quantity));
        }
        if (document.type == DocumentType.PURCHASE_RETURN || document.type == DocumentType.SALES_OUTBOUND) {
            document.items.forEach(item -> changeStock(document.warehouseId, item.productId, item.quantity.negate()));
        }
        if (document.type == DocumentType.STOCK_TRANSFER) {
            document.items.forEach(item -> {
                changeStock(document.warehouseId, item.productId, item.quantity.negate());
                changeStock(document.targetWarehouseId, item.productId, item.quantity);
            });
        }
    }

    private void changeStock(Long warehouseId, Long productId, BigDecimal delta) {
        var stock = stock(warehouseId, productId);
        var next = stock.actualQuantity.add(delta);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("可用库存不足，无法审核通过。");
        }
        stock.actualQuantity = next;
        stock.availableQuantity = available(warehouseId, productId);
    }

    private StockBalance stock(Long warehouseId, Long productId) {
        return stocks.stream()
            .filter(item -> item.warehouseId.equals(warehouseId) && item.productId.equals(productId))
            .findFirst()
            .orElseGet(() -> {
                var stock = new StockBalance();
                stock.warehouseId = warehouseId;
                stock.productId = productId;
                stocks.add(stock);
                return stock;
            });
    }

    private BigDecimal available(Long warehouseId, Long productId) {
        var actual = stocks.stream()
            .filter(stock -> stock.warehouseId.equals(warehouseId) && stock.productId.equals(productId))
            .map(stock -> stock.actualQuantity)
            .findFirst()
            .orElse(BigDecimal.ZERO);
        var reserved = documents.values().stream()
            .filter(document -> EnumSet.of(DocumentStatus.DRAFT, DocumentStatus.PENDING, DocumentStatus.REJECTED).contains(document.status))
            .filter(document -> document.type == DocumentType.PURCHASE_RETURN || document.type == DocumentType.SALES_OUTBOUND || document.type == DocumentType.STOCK_TRANSFER)
            .filter(document -> document.warehouseId.equals(warehouseId))
            .flatMap(document -> document.items.stream())
            .filter(item -> item.productId.equals(productId))
            .map(item -> item.quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return actual.subtract(reserved);
    }

    private void recalcAvailable() {
        stocks.forEach(stock -> stock.availableQuantity = available(stock.warehouseId, stock.productId));
    }

    private void createSettlement(DocumentRecord document) {
        var record = new SettlementRecord();
        record.id = ids.incrementAndGet();
        record.amount = document.totalAmount;
        record.relatedDocumentNo = document.documentNo;
        if (document.type == DocumentType.SALES_OUTBOUND) {
            record.direction = "income";
            record.documentType = "销出收入";
            record.settlementNo = settlementNo("IM");
        } else if (document.type == DocumentType.PURCHASE_RETURN) {
            record.direction = "income";
            record.documentType = "采退收入";
            record.settlementNo = settlementNo("IM");
        } else if (document.type == DocumentType.PURCHASE_INBOUND) {
            record.direction = "expense";
            record.documentType = "采入支出";
            record.settlementNo = settlementNo("OM");
        } else if (document.type == DocumentType.SALES_RETURN) {
            record.direction = "expense";
            record.documentType = "销退支出";
            record.settlementNo = settlementNo("OM");
        } else {
            return;
        }
        settlements.add(record);
    }

    private MasterRecord first(String type) {
        return master.values().stream()
            .filter(record -> record.type.equals(type))
            .findFirst()
            .orElseThrow(() -> new BusinessException(displayName(type) + "不存在"));
    }

    private String nextMasterCode(String type, Map<String, String> payload) {
        return switch (type) {
            case "warehouse" -> "CK%03d".formatted(warehouseSeq++);
            case "customer" -> "KH%03d".formatted(customerSeq++);
            case "supplier" -> "GYS%03d".formatted(supplierSeq++);
            case "product" -> productCode(payload.get("categoryName"), payload.get("brandName"), productSeq++);
            default -> null;
        };
    }

    private String productCode(String categoryName, String brandName, int seq) {
        return "SP" + initials(categoryName) + initials(brandName) + "%04d".formatted(seq);
    }

    private String initials(String value) {
        if (value == null || value.isBlank()) {
            return "XX";
        }
        var result = new StringBuilder();
        for (int i = 0; i < value.length() && result.length() < 2; i++) {
            char ch = value.charAt(i);
            if (ch <= 127 && Character.isLetterOrDigit(ch)) {
                result.append(Character.toUpperCase(ch));
            } else {
                result.append("X");
            }
        }
        while (result.length() < 2) {
            result.append("X");
        }
        return result.toString();
    }

    private String settlementNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "%04d".formatted(settlementSeq++);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean validMoney(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return money(value).compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String importValue(Map<String, String> row, String field, String... aliases) {
        var value = row.get(field);
        if (value != null) {
            return value.trim();
        }
        for (var alias : aliases) {
            value = row.get(alias);
            if (value != null) {
                return value.trim();
            }
        }
        return "";
    }

    private Long longRequired(Map<String, ?> payload, String key, String message) {
        var value = text(payload, key);
        require(value, message);
        return Long.valueOf(value);
    }

    private String text(Map<String, ?> payload, String key) {
        var value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private BigDecimal quantityRequired(String value, String message) {
        require(value, message);
        var quantity = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(message);
        }
        return quantity;
    }

    private String validatedImage(String imageData) {
        if (imageData == null || imageData.isBlank()) {
            return null;
        }
        var pngPrefix = "data:image/png;base64,";
        var jpegPrefix = "data:image/jpeg;base64,";
        var jpgPrefix = "data:image/jpg;base64,";
        String base64;
        if (imageData.startsWith(pngPrefix)) {
            base64 = imageData.substring(pngPrefix.length());
        } else if (imageData.startsWith(jpegPrefix)) {
            base64 = imageData.substring(jpegPrefix.length());
        } else if (imageData.startsWith(jpgPrefix)) {
            base64 = imageData.substring(jpgPrefix.length());
        } else {
            throw new BusinessException("请上传JPG/PNG类型格式文件");
        }
        try {
            if (Base64.getDecoder().decode(base64).length > 200 * 1024) {
                throw new BusinessException("上传文件大小不能超过200KB");
            }
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("请上传JPG/PNG类型格式文件");
        }
        return imageData;
    }

    private String defaultSettlement(String type) {
        return "supplier".equals(type) ? "货到付款" : "付款发货";
    }

    private String displayName(String type) {
        return switch (type) {
            case "brand" -> "商品品牌";
            case "category" -> "商品分类";
            case "unit" -> "商品单位";
            case "product" -> "商品";
            case "warehouse" -> "仓库";
            case "customer" -> "客户";
            case "supplier" -> "供应商";
            default -> "数据";
        };
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
    }
}
