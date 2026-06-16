# ERP Core Loop Design

## 1. Scope

Build a Spring Boot 3 + Vue 3 ERP management system based on the submitted requirement document:

- Source document: `2--A1-需求说明书(新).doc`
- Extracted text: `2--A1-需求说明书(新).cleaned-requirements.txt`
- Approved approach: Scheme B, core business loop.

The first implementation must make the ERP workflow usable end to end:

1. Maintain master data for products, warehouses, customers, and suppliers.
2. Create purchase inbound and purchase return documents.
3. Create sales outbound and sales return documents.
4. Create warehouse transfer documents.
5. Review inbound and outbound documents through warehouse audit screens.
6. Update actual stock and available stock according to approved documents.
7. Generate income and expense settlement records automatically from approved business documents.
8. Enforce role-based menus, actions, and list data visibility.

The UI should follow mainstream enterprise admin conventions from RuoYi Vue3 / RuoYi-Vue-Pro style systems: left sidebar, top user/message area, tabs, breadcrumbs, dense data tables, Element Plus forms and dialogs, restrained colors, and standard CRUD interaction patterns.

## 2. Users And Roles

The system uses fixed seed roles in the first version.

| Role | Main Permissions |
| --- | --- |
| System Administrator | Master data management: product brands, categories, units, products, warehouses, customers, suppliers |
| Purchase Manager | View and query all purchase inbound and purchase return documents |
| Purchase Specialist | Create, edit, view, delete, submit, and query own purchase inbound and purchase return documents |
| Warehouse Manager | View and query stock distribution, inbound audit, outbound audit |
| Warehouse Specialist | View and audit documents for the warehouse assigned to the current account; receive system messages for pending audit documents |
| Sales Manager | View and query all sales outbound and sales return documents |
| Sales Specialist | Create, edit, view, delete, submit, and query own sales outbound and sales return documents |
| Settlement Manager | View and query income and expense settlement records |

Users are seeded with accounts for each role. Passwords are stored with BCrypt. Authentication uses JWT.

## 3. Backend Architecture

Use a monorepo with `backend/` and `frontend/`.

Backend stack:

- Java 17
- Spring Boot 3
- Spring Security with JWT
- MyBatis-Plus
- MySQL 8 compatible schema
- Flyway for schema and seed data
- Bean Validation for request validation
- JUnit 5 and Spring Boot Test

Backend modules:

| Package | Responsibility |
| --- | --- |
| `common` | API response, pagination, error codes, validation helpers, current user context |
| `security` | Login, JWT filter, password hashing, role/action checks |
| `system` | Users, roles, menus, personal center, password change, messages |
| `masterdata` | Brands, categories, units, products, warehouses, customers, suppliers |
| `document` | Purchase inbound/return, sales outbound/return, stock transfer documents and item rows |
| `inventory` | Stock balance, stock ledger, available stock calculation, stock distribution |
| `audit` | Unified inbound and outbound audit projections and approval/rejection actions |
| `settlement` | Income and expense settlement records generated from approved documents |

Controllers return a consistent response shape:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Validation and business failures return a non-zero code and the exact Chinese message required by the relevant flow where the requirement gives one.

## 4. Frontend Architecture

Frontend stack:

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- UnoCSS or SCSS for small scoped styling

Frontend layout:

| Area | Behavior |
| --- | --- |
| Login page | Username/password login with requirement-specific validation messages |
| App shell | Sidebar menu, top bar, user dropdown, message icon, breadcrumb, tab navigation |
| List pages | Search form, reset button, table, action buttons, pagination |
| Dialog pages | Add/edit/view/audit forms in Element Plus dialogs or drawers |
| Document item grids | Editable line-item tables for product quantities, prices, totals, and validation |

The UI should not use marketing-style cards or decorative layouts. ERP screens should be dense, predictable, and optimized for repeated data entry and review.

## 5. Core Data Model

### 5.1 System Tables

- `sys_user`: login account, name, phone, role, status, password hash, create time.
- `sys_role`: role code and role name.
- `sys_message`: recipient user, title, content, source document type, source document number, read flag, create time.

### 5.2 Master Data Tables

- `md_product_brand`: name, status, create time, update time.
- `md_product_category`: name, status, create time, update time.
- `md_product_unit`: name, status, create time, update time.
- `md_product`: product code, image path, name, category, brand, unit, suggested purchase price, suggested sale price, status, create time, update time.
- `md_warehouse`: warehouse code, name, warehouse specialist user, phone, address, status, create time.
- `md_customer`: customer code, name, contact, phone, address, settlement method, status, create time.
- `md_supplier`: supplier code, name, contact, phone, address, settlement method, status, create time.

### 5.3 Document Tables

Use a header table and item table per business document type:

- `biz_purchase_inbound` and `biz_purchase_inbound_item`
- `biz_purchase_return` and `biz_purchase_return_item`
- `biz_sales_outbound` and `biz_sales_outbound_item`
- `biz_sales_return` and `biz_sales_return_item`
- `biz_stock_transfer` and `biz_stock_transfer_item`
- `biz_audit_record`: source document type, source document id, action, reason, operator, operation time.

Shared document header fields:

- document number
- source document number when a return document references an approved source document
- warehouse, supplier, or customer fields according to document type
- total item kind count
- total amount where money is involved
- status
- creator
- operation time
- auditor
- audit time

### 5.4 Inventory And Settlement Tables

- `inv_stock_balance`: warehouse, product, actual quantity.
- `inv_stock_ledger`: source document, warehouse, product, quantity delta, movement type, operation time.
- `fin_income_settlement`: settlement number, document type, amount, related document number, create time.
- `fin_expense_settlement`: settlement number, document type, amount, related document number, create time.

Available stock is calculated, not stored:

```text
available stock =
  actual stock
  - quantities in not-approved outbound documents
```

The not-approved outbound reservation set follows the requirement exactly:

- Purchase return in `待提交`, `待审核`, or `审核拒绝`
- Sales outbound in `待提交`, `待审核`, or `审核拒绝`
- Stock transfer outbound side in `未调拨`, `待调拨`, or `无法调拨`

Approved outbound documents reduce actual stock through ledger entries, so they are no longer counted as reservations.

## 6. Status Machines

### 6.1 Purchase And Sales Documents

Purchase inbound, purchase return, sales outbound, and sales return share the same status flow:

| UI Status | Internal Code | Meaning |
| --- | --- | --- |
| 待提交 | `DRAFT` | Created or edited by specialist, not submitted |
| 待审核 | `PENDING` | Submitted and waiting for warehouse specialist audit |
| 审核通过 | `APPROVED` | Approved by warehouse specialist |
| 审核拒绝 | `REJECTED` | Rejected by warehouse specialist with reason |

Allowed transitions:

- `DRAFT -> PENDING` by submit.
- `REJECTED -> PENDING` by edit and submit.
- `PENDING -> APPROVED` by audit pass.
- `PENDING -> REJECTED` by audit reject with reason.

### 6.2 Stock Transfer

| UI Status | Internal Code | Meaning |
| --- | --- | --- |
| 未调拨 | `DRAFT` | Created by warehouse manager |
| 待调拨 | `PENDING` | Submitted for warehouse specialist audit |
| 已调拨 | `APPROVED` | Transfer approved |
| 无法调拨 | `REJECTED` | Transfer rejected |

Allowed transitions match the standard document flow.

## 7. Inventory Rules

Inventory changes only when an audit is approved.

| Approved Document | Stock Effect |
| --- | --- |
| Purchase inbound | Increase actual stock in selected warehouse |
| Purchase return | Decrease actual stock in selected warehouse |
| Sales outbound | Decrease actual stock in selected warehouse |
| Sales return | Increase actual stock in selected warehouse |
| Stock transfer | Decrease actual stock in source warehouse and increase actual stock in target warehouse |

Audit approval must run inside a database transaction:

1. Lock affected stock balance rows.
2. Recalculate available stock.
3. Validate outbound or transfer quantities do not exceed available stock.
4. Insert stock ledger rows.
5. Update actual stock balances.
6. Update document status, auditor, and audit time.
7. Insert audit record.
8. Generate settlement record if required.

## 8. Settlement Rules

Income settlement records are generated automatically:

- Sales outbound approved -> income type `销出收入`
- Purchase return approved -> income type `采退收入`

Expense settlement records are generated automatically:

- Purchase inbound approved -> expense type `采入支出`
- Sales return approved -> expense type `销退支出`

Settlement records are read-only in the first implementation. The user can list, query, view, and open the related approved document detail.

## 9. Numbering Rules

Generate business numbers server-side.

| Entity | Format |
| --- | --- |
| Product | `SP` + first-letter abbreviation of first two category chars + first-letter abbreviation of first two brand chars + 4 digit sequence |
| Warehouse | `CK` + 3 digit sequence |
| Customer | `KH` + 3 digit sequence |
| Supplier | `GYS` + 3 digit sequence |
| Purchase inbound | `CR` + `yyyyMMddHHmmss` + 4 digit sequence |
| Purchase return | `CT` + `yyyyMMddHHmmss` + 4 digit sequence |
| Sales outbound | `XC` + `yyyyMMddHHmmss` + 4 digit sequence |
| Sales return | `XT` + `yyyyMMddHHmmss` + 4 digit sequence |
| Stock transfer | `KD` + `yyyyMMddHHmmss` + 4 digit sequence |
| Income settlement | `IM` + `yyyyMMddHHmmss` + 4 digit sequence |
| Expense settlement | `OM` + `yyyyMMddHHmmss` + 4 digit sequence |

For Chinese abbreviation in product codes, use a pinyin first-letter utility. If pinyin conversion fails for a character, use `X` for that character to keep code generation deterministic.

## 10. Role And Data Visibility Rules

Action permissions are enforced on both frontend and backend.

Backend remains authoritative:

- Managers can view all documents in their domain unless the requirement restricts the module.
- Specialists can view only documents created by their own account.
- Warehouse specialists can audit only documents belonging to the warehouse assigned to their account.
- Warehouse managers can view stock and audit lists but cannot approve or reject.
- Settlement managers can view all settlement records.
- System administrators can maintain all master data.

## 11. First Implementation Modules

The first build should implement these modules in this order:

1. Project scaffolding, database migration, seed data, login, current user, password change.
2. App shell: sidebar, route guards, top user menu, message popover, breadcrumbs, tabs.
3. Master data CRUD: brand, category, unit, product, warehouse, customer, supplier.
4. Purchase inbound and purchase return document flows.
5. Sales outbound and sales return document flows.
6. Stock balance, stock distribution, available stock calculation.
7. Inbound audit and outbound audit flows.
8. Stock transfer flow.
9. Income and expense settlement lists and detail views.
10. Integration tests and frontend smoke checks for the core loop.

Product image upload and product batch import are useful but not part of the critical business loop. They can be implemented after the core workflow is working, because they do not affect status, inventory, or settlement correctness.

## 12. Testing Strategy

Backend tests focus on behavior, not only controller happy paths:

- Login succeeds and fails with expected messages.
- Role-based access blocks invalid actions.
- Product code generation updates when category or brand changes.
- Master data cannot be disabled when active stock or in-flow documents reference it.
- Purchase inbound approval increases actual stock and generates expense settlement.
- Purchase return approval decreases actual stock and generates income settlement.
- Sales outbound approval decreases actual stock and generates income settlement.
- Sales return approval increases actual stock and generates expense settlement.
- Transfer approval moves actual stock from source warehouse to target warehouse.
- Rejected or draft outbound documents reduce available stock but do not change actual stock.
- Return documents cannot exceed source document remaining returnable quantity.

Frontend tests and checks:

- Login and route guard smoke test.
- Sidebar menu visibility by role.
- Main list pages render with search, reset, table, and pagination.
- Document create/edit dialogs calculate line totals and total amount.
- Audit dialog approves and rejects documents.

## 13. Design Self-Review

Placeholder scan: no placeholder requirements remain in this design.

Consistency check: modules, roles, document statuses, inventory effects, and settlement triggers align with the extracted requirement text.

Scope check: the approved Scheme B is a core business loop, not a cosmetic prototype. Image upload and batch import are intentionally deferred because they are secondary to inventory and settlement correctness.

Ambiguity decisions:

- The requirement text sometimes says saved draft status is `未提交` and query options say `待提交`. The implementation will use UI label `待提交` and internal code `DRAFT`.
- Available stock reserves `审核拒绝` outbound quantities because the requirement explicitly includes rejected documents in the available-stock deduction set. Users can edit, resubmit, or delete rejected documents to release the reservation.
- Settlement records are generated only on first transition into `APPROVED`. Re-approving an already approved document is not allowed.
