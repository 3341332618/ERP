# ERP Core Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable Spring Boot 3 + Vue 3 ERP management system implementing the approved core purchase, sales, inventory, audit, and settlement loop.

**Architecture:** Use a monorepo with a Spring Boot backend under `backend/` and a Vue 3 + Element Plus frontend under `frontend/`. The backend owns authentication, role permissions, document status transitions, inventory mutation, and settlement generation; the frontend renders a pure-Chinese enterprise admin UI inspired by RuoYi-style layouts.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security JWT, MyBatis-Plus, Flyway, H2 for local/dev tests, MySQL-compatible SQL, Vue 3, Vite, TypeScript, Pinia, Vue Router, Element Plus, Vitest.

---

## Global Constraints

- All visible frontend interface text must be Chinese: menus, buttons, form labels, placeholders, validation messages, dialog titles, table columns, empty states, tabs, breadcrumbs, notifications, and mock data labels.
- Component names and code identifiers can use English, but no user-facing English strings are allowed in Vue templates, route metadata, frontend constants, or backend API messages.
- The first implementation uses H2 for local startup and tests while keeping SQL and entity design MySQL-compatible.
- Product image upload and Excel batch import are excluded from this first implementation because the approved scope is the core business loop.
- Every document approval operation must be transactional.
- Inventory actual quantity changes only when a document is approved.
- Available stock must reserve non-approved outbound quantities as defined in the design spec.

## File Structure

```text
backend/
  pom.xml
  src/main/java/com/erp/
    ErpApplication.java
    common/
    security/
    system/
    masterdata/
    document/
    inventory/
    audit/
    settlement/
  src/main/resources/
    application.yml
    db/migration/
  src/test/java/com/erp/
frontend/
  package.json
  index.html
  vite.config.ts
  tsconfig.json
  src/
    main.ts
    App.vue
    api/
    stores/
    router/
    layouts/
    components/
    views/
docs/
  superpowers/
    specs/
    plans/
```

## Task 1: Repository And Project Skeleton

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/erp/ErpApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/erp/ErpApplicationTests.java`
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Modify: `.gitignore`

- [ ] **Step 1: Add backend Maven skeleton**

Create a Spring Boot 3 Maven project with dependencies for web, security, validation, JDBC, MyBatis-Plus, Flyway, H2, JWT, Lombok, and tests.

- [ ] **Step 2: Add frontend Vite skeleton**

Create a Vue 3 + TypeScript + Element Plus + Pinia + Vue Router project with Chinese document title `ERP管理平台`.

- [ ] **Step 3: Run baseline backend test**

Run: `cd backend; mvn test`

Expected: Spring context loads and exits with code 0.

- [ ] **Step 4: Run baseline frontend build**

Run: `cd frontend; npm install; npm run build`

Expected: Vite build exits with code 0.

## Task 2: Common Backend Foundation

**Files:**
- Create: `backend/src/main/java/com/erp/common/api/ApiResult.java`
- Create: `backend/src/main/java/com/erp/common/api/PageResult.java`
- Create: `backend/src/main/java/com/erp/common/exception/BusinessException.java`
- Create: `backend/src/main/java/com/erp/common/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/erp/common/security/CurrentUser.java`
- Create: `backend/src/main/java/com/erp/common/security/CurrentUserProvider.java`
- Create: `backend/src/main/java/com/erp/common/util/CodeGenerator.java`
- Create: `backend/src/main/java/com/erp/common/util/MoneyUtils.java`
- Create: `backend/src/test/java/com/erp/common/util/CodeGeneratorTest.java`

- [ ] **Step 1: Write failing tests for code generation**

Test that warehouse, customer, supplier, business document, settlement, and product number formats match the requirement.

- [ ] **Step 2: Implement common response and exception model**

Expose `{ code, message, data }` for normal responses and Chinese business errors.

- [ ] **Step 3: Implement deterministic numbering helpers**

Implement sequence formatting and pinyin-initial fallback for product numbers.

- [ ] **Step 4: Run common tests**

Run: `cd backend; mvn test -Dtest=CodeGeneratorTest`

Expected: all code generation tests pass.

## Task 3: Database Schema And Seed Data

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__schema.sql`
- Create: `backend/src/main/resources/db/migration/V2__seed.sql`
- Create: `backend/src/test/java/com/erp/system/SeedDataTest.java`

- [ ] **Step 1: Write seed data verification test**

Verify fixed roles and seed accounts exist after migrations.

- [ ] **Step 2: Create schema migration**

Create tables for system users, roles, messages, master data, documents, stock balances, stock ledgers, audit records, and settlements.

- [ ] **Step 3: Create seed migration**

Seed roles and accounts:

| Account | Role | Password |
| --- | --- | --- |
| `admin` | 系统管理员 | `123456` |
| `purchase_manager` | 采购主管 | `123456` |
| `purchase_staff` | 采购专员 | `123456` |
| `warehouse_manager` | 仓库主管 | `123456` |
| `warehouse_staff` | 仓库专员 | `123456` |
| `sales_manager` | 销售主管 | `123456` |
| `sales_staff` | 销售专员 | `123456` |
| `settlement_manager` | 结算主管 | `123456` |

- [ ] **Step 4: Run migration tests**

Run: `cd backend; mvn test -Dtest=SeedDataTest`

Expected: seeded roles and accounts are queryable.

## Task 4: Authentication, Personal Center, Menus, And Messages

**Files:**
- Create: `backend/src/main/java/com/erp/security/*`
- Create: `backend/src/main/java/com/erp/system/*`
- Create: `backend/src/test/java/com/erp/security/AuthControllerTest.java`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/api/system.ts`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/layouts/AppLayout.vue`
- Create: `frontend/src/views/login/LoginView.vue`
- Create: `frontend/src/views/system/ProfileView.vue`

- [ ] **Step 1: Write failing login and password-change tests**

Cover missing username, missing password, unknown account, wrong password, valid login, old password mismatch, and password confirmation mismatch.

- [ ] **Step 2: Implement JWT authentication**

Implement `/api/auth/login`, `/api/auth/me`, `/api/auth/change-password`, JWT filter, and role extraction.

- [ ] **Step 3: Implement Chinese menu metadata**

Return menus by role with Chinese names matching the requirement: `基础信息管理`, `采购管理`, `库存管理`, `销售管理`, `结算管理`, `个人中心`.

- [ ] **Step 4: Implement message API**

Expose `/api/system/messages` for the top-right message popover with Chinese empty state `暂无消息内容`.

- [ ] **Step 5: Implement frontend login and app shell**

Use Chinese labels only: `用户名`, `密码`, `登录`, `个人中心`, `退出登录`, `消息`, `暂无消息内容`.

- [ ] **Step 6: Run auth tests and frontend build**

Run: `cd backend; mvn test -Dtest=AuthControllerTest`

Run: `cd frontend; npm run build`

Expected: both commands exit with code 0.

## Task 5: Master Data Management

**Files:**
- Create: `backend/src/main/java/com/erp/masterdata/*`
- Create: `backend/src/test/java/com/erp/masterdata/MasterDataServiceTest.java`
- Create: `frontend/src/api/masterdata.ts`
- Create: `frontend/src/views/masterdata/BrandView.vue`
- Create: `frontend/src/views/masterdata/CategoryView.vue`
- Create: `frontend/src/views/masterdata/UnitView.vue`
- Create: `frontend/src/views/masterdata/ProductView.vue`
- Create: `frontend/src/views/masterdata/WarehouseView.vue`
- Create: `frontend/src/views/masterdata/CustomerView.vue`
- Create: `frontend/src/views/masterdata/SupplierView.vue`
- Create: `frontend/src/components/DataToolbar.vue`
- Create: `frontend/src/components/StatusTag.vue`

- [ ] **Step 1: Write failing master data tests**

Cover create, update, enable, disable, duplicate-name validation, phone validation, product number generation, and disabling restrictions when referenced by stock or active documents.

- [ ] **Step 2: Implement brand, category, and unit CRUD**

Support list, create, update, enable, disable, and query.

- [ ] **Step 3: Implement product CRUD**

Support generated product code, category/brand/unit references, suggested prices, status changes, and queries.

- [ ] **Step 4: Implement warehouse, customer, and supplier CRUD**

Support generated codes, contacts, phone, address, settlement method, status changes, view detail, and queries.

- [ ] **Step 5: Implement frontend master data pages**

All visible strings must be Chinese, including table empty text `暂无数据`, buttons `新增`, `修改`, `查看`, `启用`, `禁用`, `查询`, `重置`, `保存`, `取消`.

- [ ] **Step 6: Run tests and build**

Run: `cd backend; mvn test -Dtest=MasterDataServiceTest`

Run: `cd frontend; npm run build`

Expected: both commands exit with code 0.

## Task 6: Document Services For Purchase And Sales

**Files:**
- Create: `backend/src/main/java/com/erp/document/*`
- Create: `backend/src/test/java/com/erp/document/BusinessDocumentServiceTest.java`
- Create: `frontend/src/api/documents.ts`
- Create: `frontend/src/components/DocumentItemsEditor.vue`
- Create: `frontend/src/views/purchase/PurchaseInboundView.vue`
- Create: `frontend/src/views/purchase/PurchaseReturnView.vue`
- Create: `frontend/src/views/sales/SalesOutboundView.vue`
- Create: `frontend/src/views/sales/SalesReturnView.vue`

- [ ] **Step 1: Write failing document status tests**

Cover draft creation, edit, delete, submit, list visibility by role, rejected resubmit, and manager read-only access.

- [ ] **Step 2: Implement purchase inbound**

Support warehouse/supplier selection, product line items, amount calculation, draft save, submit, view, delete, and query.

- [ ] **Step 3: Implement purchase return**

Support approved purchase inbound association, remaining returnable quantity, available stock limit, draft save, submit, view, delete, and query.

- [ ] **Step 4: Implement sales outbound**

Support warehouse/customer selection, product selection from available stock, amount calculation, draft save, submit, view, delete, and query.

- [ ] **Step 5: Implement sales return**

Support approved sales outbound association, remaining returnable quantity, draft save, submit, view, delete, and query.

- [ ] **Step 6: Implement frontend document pages**

Use Chinese status labels `待提交`, `待审核`, `审核通过`, `审核拒绝`; use Chinese dialog titles such as `新增采购入库单`, `修改销售出库单`, `查看销售退货单`.

- [ ] **Step 7: Run document tests and build**

Run: `cd backend; mvn test -Dtest=BusinessDocumentServiceTest`

Run: `cd frontend; npm run build`

Expected: both commands exit with code 0.

## Task 7: Inventory, Audit, Transfer, And Settlement Loop

**Files:**
- Create: `backend/src/main/java/com/erp/inventory/*`
- Create: `backend/src/main/java/com/erp/audit/*`
- Create: `backend/src/main/java/com/erp/settlement/*`
- Create: `backend/src/test/java/com/erp/inventory/InventoryAuditSettlementTest.java`
- Create: `frontend/src/api/inventory.ts`
- Create: `frontend/src/api/audit.ts`
- Create: `frontend/src/api/settlement.ts`
- Create: `frontend/src/views/inventory/StockDistributionView.vue`
- Create: `frontend/src/views/inventory/InboundAuditView.vue`
- Create: `frontend/src/views/inventory/OutboundAuditView.vue`
- Create: `frontend/src/views/inventory/StockTransferView.vue`
- Create: `frontend/src/views/settlement/IncomeSettlementView.vue`
- Create: `frontend/src/views/settlement/ExpenseSettlementView.vue`

- [ ] **Step 1: Write failing inventory loop tests**

Cover purchase inbound approval increasing actual stock, sales outbound approval decreasing actual stock, purchase return approval decreasing actual stock and generating income, sales return approval increasing actual stock and generating expense, transfer approval moving stock, and rejected outbound reservations affecting available stock.

- [ ] **Step 2: Implement stock balance and available stock calculation**

Use actual stock table plus reservation query for non-approved outbound documents.

- [ ] **Step 3: Implement inbound and outbound audit projections**

Inbound audit includes purchase inbound, sales return, and transfer inbound. Outbound audit includes purchase return, sales outbound, and transfer outbound.

- [ ] **Step 4: Implement approval and rejection**

Approval updates document, stock, ledger, audit record, messages, and settlement in one transaction.

- [ ] **Step 5: Implement stock transfer**

Support warehouse manager create/edit/submit/delete/view/query and warehouse specialist audit.

- [ ] **Step 6: Implement settlement lists and detail views**

Income types: `销出收入`, `采退收入`. Expense types: `采入支出`, `销退支出`.

- [ ] **Step 7: Implement frontend inventory, audit, transfer, and settlement pages**

All labels, status text, operation prompts, rejection reason dialogs, and table columns must be Chinese.

- [ ] **Step 8: Run loop tests and build**

Run: `cd backend; mvn test -Dtest=InventoryAuditSettlementTest`

Run: `cd frontend; npm run build`

Expected: both commands exit with code 0.

## Task 8: Full Verification And Chinese UI Audit

**Files:**
- Create: `backend/src/test/java/com/erp/ErpCoreFlowIntegrationTest.java`
- Create: `frontend/src/tests/chinese-ui.spec.ts`
- Modify: `README.md`

- [ ] **Step 1: Write integration test for the complete core loop**

Seed master data, create purchase inbound, approve it, create sales outbound, approve it, verify stock and settlement records.

- [ ] **Step 2: Add frontend Chinese string audit**

Scan `frontend/src` for visible English UI strings in route metadata, templates, button labels, placeholders, dialogs, messages, and menu constants.

- [ ] **Step 3: Write README**

Document startup commands, seed accounts, role permissions, and core module coverage in Chinese.

- [ ] **Step 4: Run full verification**

Run: `cd backend; mvn test`

Run: `cd frontend; npm run build`

Run: `cd frontend; npm test -- --run`

Expected: all commands exit with code 0.

- [ ] **Step 5: Commit verified work**

Run:

```bash
git add .
git commit -m "Initial ERP core loop"
```

Expected: commit is created with backend, frontend, docs, and README.

## Self-Review

Spec coverage:

- Authentication, personal center, messages, pagination: covered in Tasks 2, 4, and shared frontend components.
- Master data: covered in Task 5.
- Purchase and sales documents: covered in Task 6.
- Stock distribution, inbound audit, outbound audit, transfer, inventory mutation: covered in Task 7.
- Income and expense settlement: covered in Task 7.
- Full Chinese UI requirement: covered by global constraints and Task 8 audit.

Placeholder scan:

- This plan contains no `TBD`, incomplete file names, or unspecified implementation areas.

Type consistency:

- Document statuses use backend enum codes `DRAFT`, `PENDING`, `APPROVED`, `REJECTED` and frontend labels `待提交`, `待审核`, `审核通过`, `审核拒绝`.
- Transfer statuses reuse the same internal enum and map labels to `未调拨`, `待调拨`, `已调拨`, `无法调拨`.
- Income and expense settlement names match the approved design spec.

